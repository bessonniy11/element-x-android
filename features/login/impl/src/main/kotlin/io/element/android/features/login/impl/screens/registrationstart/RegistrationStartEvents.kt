/*
 * Copyright (c) 2026 Element Creations Ltd.
 * Copyright 2026 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.login.impl.screens.registrationstart

sealed interface RegistrationStartEvents {
    data class SetLogin(val login: String) : RegistrationStartEvents
    data class SetEmail(val email: String) : RegistrationStartEvents
    data object Submit : RegistrationStartEvents
    data object ClearError : RegistrationStartEvents
}

