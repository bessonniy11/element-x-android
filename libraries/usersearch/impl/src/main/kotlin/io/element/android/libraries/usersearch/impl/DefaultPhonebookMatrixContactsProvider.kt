/*
 * Copyright (c) 2026 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.usersearch.impl

import android.content.Context
import android.provider.ContactsContract
import dev.zacsweers.metro.ContributesBinding
import io.element.android.appconfig.AuthenticationConfig
import io.element.android.libraries.core.coroutine.CoroutineDispatchers
import io.element.android.libraries.core.uri.ensureProtocol
import io.element.android.libraries.di.SessionScope
import io.element.android.libraries.di.annotations.ApplicationContext
import io.element.android.libraries.matrix.api.MatrixClient
import io.element.android.libraries.matrix.api.core.UserId
import io.element.android.libraries.matrix.api.user.MatrixUser
import io.element.android.libraries.network.RetrofitFactory
import io.element.android.libraries.sessionstorage.api.SessionStore
import io.element.android.libraries.usersearch.api.PhonebookContact
import io.element.android.libraries.usersearch.api.PhonebookMatrixContactsProvider
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import java.net.URI

private const val PHONEBOOK_GATEWAY_BATCH_LIMIT = 500
private const val RU_LOCAL_PHONE_DIGITS = 10
private const val RU_FULL_PHONE_DIGITS = 11
private const val MIN_PHONE_DIGITS = RU_LOCAL_PHONE_DIGITS

@ContributesBinding(SessionScope::class)
class DefaultPhonebookMatrixContactsProvider(
    @ApplicationContext private val context: Context,
    private val matrixClient: MatrixClient,
    private val sessionStore: SessionStore,
    retrofitFactory: RetrofitFactory,
    private val coroutineDispatchers: CoroutineDispatchers,
) : PhonebookMatrixContactsProvider {
    private val gatewayApi: PhonebookLookupApi = retrofitFactory
        .create(AuthenticationConfig.CUSTOM_AUTH_GATEWAY_URL)
        .create(PhonebookLookupApi::class.java)

    override suspend fun getMappedMatrixContacts(): List<MatrixUser> {
        return getPhonebookContacts()
            .mapNotNull { it.matrixUser }
            .distinctBy { it.userId.value }
    }

    override suspend fun getPhonebookContacts(): List<PhonebookContact> = withContext(coroutineDispatchers.io) {
        val rawContacts = readDevicePhonebookContacts()
        if (rawContacts.isEmpty()) return@withContext emptyList()

        val matchesByPhone = lookupMatrixUsersByContact(rawContacts)

        rawContacts
            .map { contact ->
                PhonebookContact(
                    displayName = contact.displayName,
                    phoneNumber = contact.phoneNumber,
                    matrixUser = matchesByPhone[contact.phoneNumber],
                )
            }
            .sortedWith(
                compareByDescending<PhonebookContact> { it.matrixUser != null }
                    .thenBy { it.displayName.lowercase() }
                    .thenBy { it.phoneNumber }
            )
    }

    private suspend fun lookupMatrixUsersByContact(rawContacts: List<RawPhonebookContact>): Map<String, MatrixUser?> {
        if (rawContacts.isEmpty()) return emptyMap()
        val contactsByPhone = rawContacts.distinctBy { it.phoneNumber }
        val gatewayMatches = lookupMatrixUsersViaGateway(contactsByPhone)
        if (gatewayMatches == null) {
            // Strict mode: if backend lookup is unavailable, return contacts quickly without unreliable name matching.
            return contactsByPhone.associate { it.phoneNumber to null }
        }
        return contactsByPhone.associate { it.phoneNumber to gatewayMatches[it.phoneNumber] }
    }

    private suspend fun lookupMatrixUsersViaGateway(rawContacts: List<RawPhonebookContact>): Map<String, MatrixUser?>? {
        val sessionData = runCatching { sessionStore.getSession(matrixClient.sessionId.value) }.getOrNull() ?: return null
        val accessToken = sessionData.accessToken.trim()
        val homeserverUrl = sessionData.homeserverUrl.trim()
        if (accessToken.isEmpty() || homeserverUrl.isEmpty()) return emptyMap()
        if (!isManagedHomeserver(homeserverUrl)) return emptyMap()

        val phones = rawContacts
            .asSequence()
            .map { it.phoneNumber }
            .distinct()
            .toList()
        if (phones.isEmpty()) return emptyMap()

        val matchedByPhone = LinkedHashMap<String, MatrixUser?>()
        phones.chunked(PHONEBOOK_GATEWAY_BATCH_LIMIT).forEach { batch ->
            val batchPhonesByKey = LinkedHashMap<String, String>(batch.size)
            batch.forEach { phone ->
                val key = normalizePhoneKey(phone) ?: return@forEach
                batchPhonesByKey.putIfAbsent(key, phone)
            }
            val response = runCatching {
                gatewayApi.lookup(
                    authorization = "Bearer $accessToken",
                    body = PhoneLookupRequest(
                        homeserver = homeserverUrl.ensureProtocol(),
                        phones = batch,
                    ),
                )
            }.getOrNull() ?: return null

            if (!response.isSuccessful) return null
            val payload = response.body() ?: return null
            payload.items.forEach { item ->
                val key = normalizePhoneKey(item.phone) ?: return@forEach
                val requestedPhone = batchPhonesByKey[key] ?: return@forEach
                val userId = runCatching { UserId(item.user.userId.trim()) }.getOrNull() ?: return@forEach
                matchedByPhone[requestedPhone] = MatrixUser(
                    userId = userId,
                    displayName = item.user.displayName?.trim()?.takeIf { it.isNotEmpty() },
                    avatarUrl = item.user.avatarUrl?.trim()?.takeIf { it.isNotEmpty() },
                )
            }
        }
        return buildMap(phones.size) {
            phones.forEach { phone ->
                put(phone, matchedByPhone[phone])
            }
        }
    }

    private fun isManagedHomeserver(homeserverUrl: String): Boolean {
        val host = runCatching { URI(homeserverUrl.ensureProtocol()).host }.getOrNull()
            ?.trim()
            ?.lowercase()
            ?: return false
        return AuthenticationConfig.CUSTOM_AUTH_MANAGED_HOMESERVERS.contains(host)
    }

    private fun readDevicePhonebookContacts(): List<RawPhonebookContact> {
        return runCatching {
            val projection = arrayOf(
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NORMALIZED_NUMBER,
                ContactsContract.CommonDataKinds.Phone.NUMBER,
            )
            val contacts = LinkedHashMap<String, RawPhonebookContact>()
            context.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                projection,
                null,
                null,
                null,
            )?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val normalizedIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NORMALIZED_NUMBER)
                val rawIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                while (cursor.moveToNext()) {
                    val normalized = cursor.getStringOrNull(normalizedIndex)
                        ?: cursor.getStringOrNull(rawIndex)
                    val phoneNumber = normalized?.let(::normalizePhoneNumber) ?: continue
                    val name = cursor.getStringOrNull(nameIndex).orEmpty().ifBlank { phoneNumber }
                    contacts.putIfAbsent(
                        phoneNumber,
                        RawPhonebookContact(
                            displayName = name,
                            phoneNumber = phoneNumber,
                        )
                    )
                }
            }
            contacts.values.toList()
        }.getOrDefault(emptyList())
    }

    private fun normalizePhoneNumber(rawValue: String): String? {
        val digitsOnly = rawValue.filter(Char::isDigit)
        if (digitsOnly.length < MIN_PHONE_DIGITS) return null
        return when {
            rawValue.trim().startsWith("+") -> "+$digitsOnly"
            digitsOnly.length == RU_LOCAL_PHONE_DIGITS -> "+7$digitsOnly"
            digitsOnly.length == RU_FULL_PHONE_DIGITS && digitsOnly.startsWith("8") -> "+7${digitsOnly.drop(1)}"
            digitsOnly.length == RU_FULL_PHONE_DIGITS && digitsOnly.startsWith("7") -> "+$digitsOnly"
            else -> "+$digitsOnly"
        }
    }

    private fun normalizePhoneKey(rawValue: String): String? {
        val digitsOnly = rawValue.filter(Char::isDigit)
        return when {
            digitsOnly.length == RU_LOCAL_PHONE_DIGITS -> digitsOnly
            digitsOnly.length == RU_FULL_PHONE_DIGITS && digitsOnly.startsWith("8") -> digitsOnly.drop(1)
            digitsOnly.length == RU_FULL_PHONE_DIGITS && digitsOnly.startsWith("7") -> digitsOnly.drop(1)
            else -> null
        }
    }

    private fun android.database.Cursor.getStringOrNull(index: Int): String? {
        if (index < 0 || isNull(index)) return null
        return getString(index)
    }
}

private data class RawPhonebookContact(
    val displayName: String,
    val phoneNumber: String,
)

private interface PhonebookLookupApi {
    @POST("/auth/contacts/lookup")
    suspend fun lookup(
        @Header("X-Client-Auth-Mode")
        authMode: String = AuthenticationConfig.CUSTOM_AUTH_MODE,
        @Header("X-Client-App")
        clientApp: String = AuthenticationConfig.CUSTOM_AUTH_CLIENT_APP,
        @Header("Authorization")
        authorization: String,
        @Body
        body: PhoneLookupRequest,
    ): Response<PhoneLookupResponse>
}

@Serializable
private data class PhoneLookupRequest(
    val homeserver: String,
    val phones: List<String>,
)

@Serializable
private data class PhoneLookupResponse(
    val items: List<PhoneLookupItem> = emptyList(),
)

@Serializable
private data class PhoneLookupItem(
    val phone: String,
    val user: PhoneLookupUser,
)

@Serializable
private data class PhoneLookupUser(
    @SerialName("user_id")
    val userId: String,
    @SerialName("display_name")
    val displayName: String? = null,
    @SerialName("avatar_url")
    val avatarUrl: String? = null,
)
