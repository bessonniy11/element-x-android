/*
 * Copyright (c) 2026 Element Creations Ltd.
 * Copyright 2026 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.login.impl.registrationdraft

enum class RegistrationDraftStep {
    VerifyPassword,
    Profile,
}

data class RegistrationDraft(
    val homeserverUrl: String,
    val login: String,
    val email: String,
    val registrationSessionId: String,
    val step: RegistrationDraftStep,
    val verifiedToken: String? = null,
    val displayName: String? = null,
    val phone: String? = null,
    val avatarUploadRef: String? = null,
)
