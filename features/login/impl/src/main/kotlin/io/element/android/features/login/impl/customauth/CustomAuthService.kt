/*
 * Copyright (c) 2026 Element Creations Ltd.
 * Copyright 2026 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.login.impl.customauth

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import io.element.android.appconfig.AuthenticationConfig
import io.element.android.libraries.core.uri.ensureProtocol
import io.element.android.libraries.matrix.api.auth.external.ExternalSession
import io.element.android.libraries.network.RetrofitFactory
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query
import java.net.URI

interface CustomAuthService {
    fun isManagedHomeserver(homeserverUrl: String): Boolean

    suspend fun loginByIdentifier(
        homeserverUrl: String,
        identifier: String,
        password: String,
    ): Result<ExternalSession>

    suspend fun requestPasswordReset(
        homeserverUrl: String,
        identifier: String,
    ): Result<PasswordResetAcceptance>

    suspend fun startRegistration(
        homeserverUrl: String,
        login: String,
        email: String,
    ): Result<RegistrationStartAcceptance>

    suspend fun verifyRegistrationPassword(
        registrationSessionId: String,
        password: String,
    ): Result<RegistrationVerifyAcceptance>

    suspend fun getRegistrationStatus(
        registrationSessionId: String,
    ): Result<RegistrationStatus>

    suspend fun uploadRegistrationAvatar(
        registrationSessionId: String,
        contentType: String,
        contentBase64: String,
    ): Result<AvatarUploadAcceptance>

    suspend fun completeRegistration(
        homeserverUrl: String,
        verifiedToken: String,
        displayName: String,
        phone: String,
        avatarUploadRef: String? = null,
    ): Result<ExternalSession>
}

@Serializable
data class PasswordResetAcceptance(
    val accepted: Boolean,
    @SerialName("retry_after_seconds")
    val retryAfterSeconds: Int,
)

@Serializable
data class RegistrationStartAcceptance(
    @SerialName("registration_session_id")
    val registrationSessionId: String,
    @SerialName("expires_at")
    val expiresAt: String? = null,
)

@Serializable
data class RegistrationVerifyAcceptance(
    @SerialName("verified_token")
    val verifiedToken: String,
    @SerialName("expires_at")
    val expiresAt: String? = null,
)

@Serializable
data class RegistrationStatus(
    val status: String,
    @SerialName("expires_at")
    val expiresAt: String? = null,
)

@Serializable
data class AvatarUploadAcceptance(
    @SerialName("avatar_upload_ref")
    val avatarUploadRef: String,
)

@ContributesBinding(AppScope::class)
@Inject
class DefaultCustomAuthService(
    retrofitFactory: RetrofitFactory,
) : CustomAuthService {
    private val api: CustomAuthApi = retrofitFactory.create(AuthenticationConfig.CUSTOM_AUTH_GATEWAY_URL)
        .create(CustomAuthApi::class.java)

    override fun isManagedHomeserver(homeserverUrl: String): Boolean {
        val host = normalizeHost(homeserverUrl) ?: return false
        return AuthenticationConfig.CUSTOM_AUTH_MANAGED_HOMESERVERS.contains(host)
    }

    override suspend fun loginByIdentifier(
        homeserverUrl: String,
        identifier: String,
        password: String,
    ): Result<ExternalSession> = runCatching {
        val normalizedHomeserverUrl = homeserverUrl.ensureProtocol()
        val response = api.loginPassword(
            body = LoginPasswordRequest(
                identifier = identifier.trim(),
                password = password,
                homeserver = normalizedHomeserverUrl,
            )
        )
        val payload = response.body()
        if (!response.isSuccessful || payload == null) {
            throw CustomAuthException.LoginFailed(response.code())
        }
        ExternalSession(
            userId = payload.userId,
            deviceId = payload.deviceId,
            accessToken = payload.accessToken,
            refreshToken = payload.refreshToken,
            homeserverUrl = normalizedHomeserverUrl,
        )
    }

    override suspend fun requestPasswordReset(
        homeserverUrl: String,
        identifier: String,
    ): Result<PasswordResetAcceptance> = runCatching {
        val response = api.passwordResetRequest(
            body = PasswordResetRequest(
                identifier = identifier.trim(),
                homeserver = homeserverUrl.ensureProtocol(),
            )
        )
        val payload = response.body()
        if (!response.isSuccessful || payload == null) {
            throw CustomAuthException.ResetFailed(response.code())
        }
        PasswordResetAcceptance(
            accepted = payload.accepted,
            retryAfterSeconds = payload.retryAfterSeconds,
        )
    }

    override suspend fun startRegistration(
        homeserverUrl: String,
        login: String,
        email: String,
    ): Result<RegistrationStartAcceptance> = runCatching {
        val response = api.registrationStart(
            body = RegistrationStartRequest(
                login = login.trim(),
                email = email.trim(),
                homeserver = homeserverUrl.ensureProtocol(),
            )
        )
        val payload = response.requireBodyOrThrow(CustomAuthException.StartRegistrationFailed(response.code()))
        RegistrationStartAcceptance(
            registrationSessionId = payload.registrationSessionId,
            expiresAt = payload.expiresAt,
        )
    }

    override suspend fun verifyRegistrationPassword(
        registrationSessionId: String,
        password: String,
    ): Result<RegistrationVerifyAcceptance> = runCatching {
        val response = api.registrationVerifyPassword(
            body = RegistrationVerifyPasswordRequest(
                registrationSessionId = registrationSessionId.trim(),
                password = password,
            )
        )
        val payload = response.requireBodyOrThrow(CustomAuthException.VerifyRegistrationFailed(response.code()))
        RegistrationVerifyAcceptance(
            verifiedToken = payload.verifiedToken,
            expiresAt = payload.expiresAt,
        )
    }

    override suspend fun getRegistrationStatus(
        registrationSessionId: String,
    ): Result<RegistrationStatus> = runCatching {
        val response = api.registrationStatus(
            registrationSessionId = registrationSessionId.trim(),
        )
        val payload = response.requireBodyOrThrow(CustomAuthException.RegistrationStatusFailed(response.code()))
        RegistrationStatus(
            status = payload.status,
            expiresAt = payload.expiresAt,
        )
    }

    override suspend fun completeRegistration(
        homeserverUrl: String,
        verifiedToken: String,
        displayName: String,
        phone: String,
        avatarUploadRef: String?,
    ): Result<ExternalSession> = runCatching {
        val response = api.registrationComplete(
            body = RegistrationCompleteRequest(
                verifiedToken = verifiedToken.trim(),
                displayName = displayName.trim(),
                phone = phone.trim(),
                avatarUploadRef = avatarUploadRef?.trim()?.takeIf { it.isNotEmpty() },
            )
        )
        val payload = response.requireBodyOrThrow(CustomAuthException.CompleteRegistrationFailed(response.code()))
        ExternalSession(
            userId = payload.userId,
            deviceId = payload.deviceId,
            accessToken = payload.accessToken,
            refreshToken = payload.refreshToken,
            homeserverUrl = homeserverUrl.ensureProtocol(),
        )
    }

    override suspend fun uploadRegistrationAvatar(
        registrationSessionId: String,
        contentType: String,
        contentBase64: String,
    ): Result<AvatarUploadAcceptance> = runCatching {
        val response = api.registrationAvatarUpload(
            body = RegistrationAvatarUploadRequest(
                registrationSessionId = registrationSessionId.trim(),
                contentType = contentType.trim(),
                contentBase64 = contentBase64.trim(),
            )
        )
        val payload = response.requireBodyOrThrow(CustomAuthException.RegistrationAvatarUploadFailed(response.code()))
        AvatarUploadAcceptance(
            avatarUploadRef = payload.avatarUploadRef,
        )
    }

    private fun normalizeHost(homeserverUrl: String): String? {
        val raw = homeserverUrl.trim()
        if (raw.isEmpty()) return null
        return runCatching {
            URI(raw.ensureProtocol()).host?.trim()?.lowercase()
        }.getOrNull()?.takeIf { it.isNotEmpty() }
    }

    private fun <T> Response<T>.requireBodyOrThrow(error: Throwable): T {
        val payload = body()
        if (!isSuccessful || payload == null) {
            throw error
        }
        return payload
    }
}

private interface CustomAuthApi {
    @POST("/auth/login/password")
    suspend fun loginPassword(
        @Header("X-Client-Auth-Mode")
        authMode: String = AuthenticationConfig.CUSTOM_AUTH_MODE,
        @Header("X-Client-App")
        clientApp: String = AuthenticationConfig.CUSTOM_AUTH_CLIENT_APP,
        @Body
        body: LoginPasswordRequest,
    ): retrofit2.Response<LoginPasswordResponse>

    @POST("/auth/password/reset/request")
    suspend fun passwordResetRequest(
        @Header("X-Client-Auth-Mode")
        authMode: String = AuthenticationConfig.CUSTOM_AUTH_MODE,
        @Header("X-Client-App")
        clientApp: String = AuthenticationConfig.CUSTOM_AUTH_CLIENT_APP,
        @Body
        body: PasswordResetRequest,
    ): retrofit2.Response<PasswordResetResponse>

    @POST("/auth/registration/start")
    suspend fun registrationStart(
        @Header("X-Client-Auth-Mode")
        authMode: String = AuthenticationConfig.CUSTOM_AUTH_MODE,
        @Header("X-Client-App")
        clientApp: String = AuthenticationConfig.CUSTOM_AUTH_CLIENT_APP,
        @Body
        body: RegistrationStartRequest,
    ): retrofit2.Response<RegistrationStartResponse>

    @POST("/auth/registration/verify-password")
    suspend fun registrationVerifyPassword(
        @Header("X-Client-Auth-Mode")
        authMode: String = AuthenticationConfig.CUSTOM_AUTH_MODE,
        @Header("X-Client-App")
        clientApp: String = AuthenticationConfig.CUSTOM_AUTH_CLIENT_APP,
        @Body
        body: RegistrationVerifyPasswordRequest,
    ): retrofit2.Response<RegistrationVerifyPasswordResponse>

    @GET("/auth/registration/status")
    suspend fun registrationStatus(
        @Header("X-Client-Auth-Mode")
        authMode: String = AuthenticationConfig.CUSTOM_AUTH_MODE,
        @Header("X-Client-App")
        clientApp: String = AuthenticationConfig.CUSTOM_AUTH_CLIENT_APP,
        @Query("registration_session_id")
        registrationSessionId: String,
    ): retrofit2.Response<RegistrationStatusResponse>

    @POST("/auth/registration/complete")
    suspend fun registrationComplete(
        @Header("X-Client-Auth-Mode")
        authMode: String = AuthenticationConfig.CUSTOM_AUTH_MODE,
        @Header("X-Client-App")
        clientApp: String = AuthenticationConfig.CUSTOM_AUTH_CLIENT_APP,
        @Body
        body: RegistrationCompleteRequest,
    ): retrofit2.Response<RegistrationCompleteResponse>

    @POST("/auth/registration/avatar/upload")
    suspend fun registrationAvatarUpload(
        @Header("X-Client-Auth-Mode")
        authMode: String = AuthenticationConfig.CUSTOM_AUTH_MODE,
        @Header("X-Client-App")
        clientApp: String = AuthenticationConfig.CUSTOM_AUTH_CLIENT_APP,
        @Body
        body: RegistrationAvatarUploadRequest,
    ): retrofit2.Response<RegistrationAvatarUploadResponse>
}

@Serializable
private data class LoginPasswordRequest(
    val identifier: String,
    val password: String,
    val homeserver: String,
)

@Serializable
private data class LoginPasswordResponse(
    @SerialName("access_token")
    val accessToken: String,
    @SerialName("refresh_token")
    val refreshToken: String? = null,
    @SerialName("user_id")
    val userId: String,
    @SerialName("device_id")
    val deviceId: String,
)

@Serializable
private data class PasswordResetRequest(
    val identifier: String,
    val homeserver: String,
)

@Serializable
private data class PasswordResetResponse(
    val accepted: Boolean,
    @SerialName("retry_after_seconds")
    val retryAfterSeconds: Int,
)

@Serializable
private data class RegistrationStartRequest(
    val login: String,
    val email: String,
    val homeserver: String,
)

@Serializable
private data class RegistrationStartResponse(
    @SerialName("registration_session_id")
    val registrationSessionId: String,
    @SerialName("expires_at")
    val expiresAt: String? = null,
)

@Serializable
private data class RegistrationVerifyPasswordRequest(
    @SerialName("registration_session_id")
    val registrationSessionId: String,
    val password: String,
)

@Serializable
private data class RegistrationVerifyPasswordResponse(
    @SerialName("verified_token")
    val verifiedToken: String,
    @SerialName("expires_at")
    val expiresAt: String? = null,
)

@Serializable
private data class RegistrationStatusResponse(
    val status: String,
    @SerialName("expires_at")
    val expiresAt: String? = null,
)

@Serializable
private data class RegistrationCompleteRequest(
    @SerialName("verified_token")
    val verifiedToken: String,
    @SerialName("display_name")
    val displayName: String,
    val phone: String,
    @SerialName("avatar_upload_ref")
    val avatarUploadRef: String? = null,
)

@Serializable
private data class RegistrationAvatarUploadRequest(
    @SerialName("registration_session_id")
    val registrationSessionId: String,
    @SerialName("content_type")
    val contentType: String,
    @SerialName("content_base64")
    val contentBase64: String,
)

@Serializable
private data class RegistrationAvatarUploadResponse(
    @SerialName("avatar_upload_ref")
    val avatarUploadRef: String,
)

@Serializable
private data class RegistrationCompleteResponse(
    @SerialName("access_token")
    val accessToken: String,
    @SerialName("refresh_token")
    val refreshToken: String? = null,
    @SerialName("user_id")
    val userId: String,
    @SerialName("device_id")
    val deviceId: String,
)

sealed class CustomAuthException(
    message: String,
) : Exception(message) {
    data class LoginFailed(val statusCode: Int) : CustomAuthException("Custom auth login failed: $statusCode")
    data class ResetFailed(val statusCode: Int) : CustomAuthException("Custom auth reset failed: $statusCode")
    data class StartRegistrationFailed(val statusCode: Int) : CustomAuthException("Custom auth registration start failed: $statusCode")
    data class VerifyRegistrationFailed(val statusCode: Int) : CustomAuthException("Custom auth registration verify failed: $statusCode")
    data class RegistrationStatusFailed(val statusCode: Int) : CustomAuthException("Custom auth registration status failed: $statusCode")
    data class RegistrationAvatarUploadFailed(val statusCode: Int) : CustomAuthException("Custom auth registration avatar upload failed: $statusCode")
    data class CompleteRegistrationFailed(val statusCode: Int) : CustomAuthException("Custom auth registration complete failed: $statusCode")
}
