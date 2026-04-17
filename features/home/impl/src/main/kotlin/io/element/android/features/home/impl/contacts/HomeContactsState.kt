/*
 * Copyright (c) 2026 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.home.impl.contacts

import kotlinx.collections.immutable.ImmutableList

data class HomeContactsState(
    val hasContactsPermission: Boolean,
    val isLoading: Boolean,
    val contacts: ImmutableList<HomeContact>,
    val eventSink: (HomeContactsEvents) -> Unit,
)
