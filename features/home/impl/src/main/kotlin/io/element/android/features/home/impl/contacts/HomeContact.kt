/*
 * Copyright (c) 2026 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.home.impl.contacts

import io.element.android.libraries.matrix.api.user.MatrixUser

data class HomeContact(
    val displayName: String,
    val phoneNumber: String,
    val matrixUser: MatrixUser?,
) {
    val key = "$displayName|$phoneNumber"
    val isInMatrix = matrixUser != null
}
