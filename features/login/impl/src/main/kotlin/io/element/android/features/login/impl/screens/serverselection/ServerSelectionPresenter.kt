/*
 * Copyright (c) 2026 Element Creations Ltd.
 * Copyright 2026 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.login.impl.screens.serverselection

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import dev.zacsweers.metro.Inject
import io.element.android.features.login.impl.accountprovider.AccountProvider
import io.element.android.features.login.impl.accountprovider.AccountProviderDataSource
import io.element.android.features.login.impl.changeserver.ChangeServerEvents
import io.element.android.features.login.impl.changeserver.ChangeServerState
import io.element.android.libraries.architecture.Presenter
import io.element.android.libraries.core.uri.ensureProtocol

@Inject
class ServerSelectionPresenter(
    private val accountProviderDataSource: AccountProviderDataSource,
    private val changeServerPresenter: Presenter<ChangeServerState>,
) : Presenter<ServerSelectionState> {
    @Composable
    override fun present(): ServerSelectionState {
        val accountProvider by accountProviderDataSource.flow.collectAsState()
        var serverInput by rememberSaveable {
            mutableStateOf(accountProvider.title)
        }
        val changeServerState = changeServerPresenter.present()

        fun handleEvent(event: ServerSelectionEvents) {
            when (event) {
                is ServerSelectionEvents.SetServer -> {
                    serverInput = event.server
                }
                ServerSelectionEvents.Submit -> {
                    val normalizedServer = serverInput.trim().ensureProtocol()
                    changeServerState.eventSink(
                        ChangeServerEvents.ChangeServer(
                            AccountProvider(url = normalizedServer)
                        )
                    )
                }
            }
        }

        return ServerSelectionState(
            serverInput = serverInput,
            changeServerState = changeServerState,
            eventSink = ::handleEvent,
        )
    }
}
