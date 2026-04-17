/*
 * Copyright (c) 2026 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.home.impl.contacts

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import io.element.android.libraries.matrix.api.core.UserId
import io.element.android.libraries.matrix.api.user.MatrixUser
import kotlinx.collections.immutable.toImmutableList

internal class HomeContactsStateProvider : PreviewParameterProvider<HomeContactsState> {
    override val values: Sequence<HomeContactsState>
        get() = sequenceOf(
            aHomeContactsState(
                contacts = listOf(
                    HomeContact(
                        displayName = "Alice",
                        phoneNumber = "+79000000001",
                        matrixUser = MatrixUser(UserId("@alice:matrix.org"), "Alice"),
                    ),
                    HomeContact(
                        displayName = "Bob",
                        phoneNumber = "+79000000002",
                        matrixUser = null,
                    ),
                ),
                hasContactsPermission = true,
            ),
            aHomeContactsState(
                contacts = emptyList(),
                hasContactsPermission = true,
            ),
            aHomeContactsState(
                contacts = emptyList(),
                hasContactsPermission = false,
            ),
            aHomeContactsState(
                isLoading = true,
                hasContactsPermission = true,
            ),
        )
}

internal fun aHomeContactsState(
    hasContactsPermission: Boolean = true,
    isLoading: Boolean = false,
    contacts: List<HomeContact> = emptyList(),
    eventSink: (HomeContactsEvents) -> Unit = {},
): HomeContactsState {
    return HomeContactsState(
        hasContactsPermission = hasContactsPermission,
        isLoading = isLoading,
        contacts = contacts.toImmutableList(),
        eventSink = eventSink,
    )
}
