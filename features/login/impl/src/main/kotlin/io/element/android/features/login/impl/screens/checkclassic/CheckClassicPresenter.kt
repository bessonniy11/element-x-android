/*
 * Copyright (c) 2026 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.login.impl.screens.checkclassic

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import dev.zacsweers.metro.Inject
import io.element.android.features.login.impl.classic.ElementClassicConnection
import io.element.android.features.login.impl.classic.ElementClassicConnectionState
import io.element.android.libraries.architecture.AsyncData
import io.element.android.libraries.architecture.Presenter
import io.element.android.libraries.sessionstorage.api.SessionStore
import io.element.android.libraries.sessionstorage.api.toUserListFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn

@Inject
class CheckClassicPresenter(
    private val elementClassicConnection: ElementClassicConnection,
    private val sessionStore: SessionStore,
) : Presenter<CheckClassicState> {
    @Composable
    override fun present(): CheckClassicState {
        val elementClassicSession by produceState(initialValue = AsyncData.Loading()) {
            combine(
                elementClassicConnection.stateFlow,
                sessionStore.sessionsFlow().toUserListFlow(),
            ) { elementClassicConnectionState, existingSessions ->
                value = when (elementClassicConnectionState) {
                    ElementClassicConnectionState.Idle -> AsyncData.Loading()
                    is ElementClassicConnectionState.ElementClassicReady -> {
                        if (elementClassicConnectionState.elementClassicSession.userId.value in existingSessions) {
                            AsyncData.Failure(Exception("Already logged in with this session"))
                        } else {
                            AsyncData.Success(elementClassicConnectionState.elementClassicSession.userId)
                        }
                    }
                    ElementClassicConnectionState.ElementClassicNotFound,
                    ElementClassicConnectionState.ElementClassicReadyNoSession,
                    is ElementClassicConnectionState.Error -> {
                        AsyncData.Failure(Exception("No session"))
                    }
                }
            }
                .launchIn(this)
        }

        fun handleEvent(event: CheckClassicEvent) {
            when (event) {
                CheckClassicEvent.RefreshData -> {
                    elementClassicConnection.requestSession()
                }
            }
        }

        return CheckClassicState(
            elementClassicSession = elementClassicSession,
            eventSink = ::handleEvent,
        )
    }
}
