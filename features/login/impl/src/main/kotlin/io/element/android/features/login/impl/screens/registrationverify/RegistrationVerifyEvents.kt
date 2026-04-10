/*
 * Copyright (c) 2026 Element Creations Ltd.
 * Copyright 2026 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.login.impl.screens.registrationverify

sealed interface RegistrationVerifyEvents {
    data class SetPassword(val password: String) : RegistrationVerifyEvents
    data object Submit : RegistrationVerifyEvents
    data object ClearError : RegistrationVerifyEvents
}

