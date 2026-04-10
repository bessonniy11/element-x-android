/*
 * Copyright (c) 2026 Element Creations Ltd.
 * Copyright 2026 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.login.impl.screens.registrationprofile

sealed interface RegistrationProfileEvents {
    data class SetDisplayName(val displayName: String) : RegistrationProfileEvents
    data class SetPhone(val phone: String) : RegistrationProfileEvents
    data object PickAvatar : RegistrationProfileEvents
    data object RemoveAvatar : RegistrationProfileEvents
    data class Submit(
        val displayName: String,
        val phone: String,
    ) : RegistrationProfileEvents
    data object ClearAvatarError : RegistrationProfileEvents
    data object ClearError : RegistrationProfileEvents
}
