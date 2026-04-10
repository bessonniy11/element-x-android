/*
 * Copyright (c) 2026 Element Creations Ltd.
 * Copyright 2026 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.login.impl.screens.serverselection

import io.element.android.features.login.impl.changeserver.ChangeServerState
import io.element.android.libraries.architecture.AsyncData

data class ServerSelectionState(
    val serverInput: String,
    val changeServerState: ChangeServerState,
    val eventSink: (ServerSelectionEvents) -> Unit,
) {
    val submitEnabled: Boolean
        get() = serverInput.isNotBlank() && changeServerState.changeServerAction !is AsyncData.Loading
}

