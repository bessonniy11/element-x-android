/*
 * Copyright (c) 2026 Element Creations Ltd.
 * Copyright 2026 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.login.impl.screens.registrationverify

import io.element.android.features.login.impl.customauth.RegistrationVerifyAcceptance
import io.element.android.libraries.architecture.AsyncData

data class RegistrationVerifyState(
    val login: String,
    val email: String,
    val registrationSessionId: String,
    val password: String,
    val verifyAction: AsyncData<RegistrationVerifyAcceptance>,
    val eventSink: (RegistrationVerifyEvents) -> Unit,
) {
    val submitEnabled: Boolean
        get() = password.isNotBlank() && verifyAction !is AsyncData.Loading
}

