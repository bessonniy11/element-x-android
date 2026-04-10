/*
 * Copyright (c) 2026 Element Creations Ltd.
 * Copyright 2026 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.login.impl.screens.registrationstart

import io.element.android.features.login.impl.accountprovider.AccountProvider
import io.element.android.features.login.impl.customauth.RegistrationStartAcceptance
import io.element.android.libraries.architecture.AsyncData

data class RegistrationStartState(
    val accountProvider: AccountProvider,
    val login: String,
    val email: String,
    val isLoginValid: Boolean,
    val isEmailValid: Boolean,
    val startAction: AsyncData<RegistrationStartAcceptance>,
    val eventSink: (RegistrationStartEvents) -> Unit,
) {
    val submitEnabled: Boolean
        get() = login.isNotBlank() &&
            email.isNotBlank() &&
            isLoginValid &&
            isEmailValid &&
            startAction !is AsyncData.Loading
}

