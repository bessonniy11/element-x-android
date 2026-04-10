/*
 * Copyright (c) 2026 Element Creations Ltd.
 * Copyright 2026 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.login.impl.screens.registrationprofile

import io.element.android.features.login.impl.accountprovider.AccountProvider
import io.element.android.libraries.architecture.AsyncData
import io.element.android.libraries.matrix.api.core.SessionId

data class RegistrationProfileState(
    val accountProvider: AccountProvider,
    val email: String,
    val displayName: String,
    val phone: String,
    val hasAvatar: Boolean,
    val avatarUploadAction: AsyncData<Unit>,
    val completeAction: AsyncData<SessionId>,
    val eventSink: (RegistrationProfileEvents) -> Unit,
) {
    val isPhoneValid: Boolean
        get() = normalizeRuPhoneForBackend(phone) != null

    val submitEnabled: Boolean
        get() = displayName.isNotBlank() &&
            isPhoneValid &&
            completeAction !is AsyncData.Loading
}
