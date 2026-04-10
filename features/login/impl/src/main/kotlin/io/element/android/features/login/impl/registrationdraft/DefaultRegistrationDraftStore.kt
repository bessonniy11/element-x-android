/*
 * Copyright (c) 2026 Element Creations Ltd.
 * Copyright 2026 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.login.impl.registrationdraft

import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import io.element.android.libraries.preferences.api.store.PreferenceDataStoreFactory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val homeserverUrlKey = stringPreferencesKey("homeserverUrl")
private val loginKey = stringPreferencesKey("login")
private val emailKey = stringPreferencesKey("email")
private val registrationSessionIdKey = stringPreferencesKey("registrationSessionId")
private val stepKey = stringPreferencesKey("step")
private val verifiedTokenKey = stringPreferencesKey("verifiedToken")
private val displayNameKey = stringPreferencesKey("displayName")
private val phoneKey = stringPreferencesKey("phone")
private val avatarUploadRefKey = stringPreferencesKey("avatarUploadRef")

@ContributesBinding(AppScope::class)
class DefaultRegistrationDraftStore(
    preferenceDataStoreFactory: PreferenceDataStoreFactory,
) : RegistrationDraftStore {
    private val store = preferenceDataStoreFactory.create("elementx_registration_draft")

    override fun draftFlow(): Flow<RegistrationDraft?> {
        return store.data.map { prefs ->
            val homeserverUrl = prefs[homeserverUrlKey]
            val login = prefs[loginKey]
            val email = prefs[emailKey]
            val registrationSessionId = prefs[registrationSessionIdKey]
            val step = prefs[stepKey]?.let { stepValue ->
                RegistrationDraftStep.entries.firstOrNull { it.name == stepValue }
            }

            if (homeserverUrl == null || login == null || email == null || registrationSessionId == null || step == null) {
                null
            } else {
                RegistrationDraft(
                    homeserverUrl = homeserverUrl,
                    login = login,
                    email = email,
                    registrationSessionId = registrationSessionId,
                    step = step,
                    verifiedToken = prefs[verifiedTokenKey],
                    displayName = prefs[displayNameKey],
                    phone = prefs[phoneKey],
                    avatarUploadRef = prefs[avatarUploadRefKey],
                )
            }
        }
    }

    override suspend fun saveDraft(draft: RegistrationDraft) {
        store.edit { prefs ->
            prefs[homeserverUrlKey] = draft.homeserverUrl
            prefs[loginKey] = draft.login
            prefs[emailKey] = draft.email
            prefs[registrationSessionIdKey] = draft.registrationSessionId
            prefs[stepKey] = draft.step.name

            setOrRemove(prefs = prefs, key = verifiedTokenKey, value = draft.verifiedToken)
            setOrRemove(prefs = prefs, key = displayNameKey, value = draft.displayName)
            setOrRemove(prefs = prefs, key = phoneKey, value = draft.phone)
            setOrRemove(prefs = prefs, key = avatarUploadRefKey, value = draft.avatarUploadRef)
        }
    }

    override suspend fun clear() {
        store.edit { it.clear() }
    }
}

private fun setOrRemove(
    prefs: androidx.datastore.preferences.core.MutablePreferences,
    key: androidx.datastore.preferences.core.Preferences.Key<String>,
    value: String?,
) {
    val normalizedValue = value?.trim().orEmpty()
    if (normalizedValue.isEmpty()) {
        prefs.remove(key)
    } else {
        prefs[key] = normalizedValue
    }
}
