/*
 * Copyright (c) 2026 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.usersearch.api

import io.element.android.libraries.matrix.api.user.MatrixUser

data class PhonebookContact(
    val displayName: String,
    val phoneNumber: String,
    val matrixUser: MatrixUser?,
)

interface PhonebookMatrixContactsProvider {
    suspend fun getMappedMatrixContacts(): List<MatrixUser>

    suspend fun getPhonebookContacts(): List<PhonebookContact> {
        return getMappedMatrixContacts().map { matrixUser ->
            PhonebookContact(
                displayName = matrixUser.displayName ?: matrixUser.userId.value,
                phoneNumber = "",
                matrixUser = matrixUser,
            )
        }
    }
}
