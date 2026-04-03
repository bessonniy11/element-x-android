/*
 * Copyright (c) 2026 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.login.impl.screens.classic

import dev.zacsweers.metro.Inject
import io.element.android.features.login.impl.classic.ElementClassicConnection
import io.element.android.features.login.impl.classic.ElementClassicConnectionState
import io.element.android.libraries.sessionstorage.api.SessionStore
import io.element.android.libraries.sessionstorage.api.toUserListFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.take

@Inject
class ClassicFlowNodeHelper(
    private val elementClassicConnection: ElementClassicConnection,
    private val sessionStore: SessionStore,
) {
    fun navigationEventFlow(): Flow<NavigationEvent> {
        return combine(
            elementClassicConnection.stateFlow
                .distinctUntilChangedBy {
                    // Ignore change on ElementClassicConnectionState.ElementClassicReady.avatar
                    if (it is ElementClassicConnectionState.ElementClassicReady) {
                        it.copy(avatar = null)
                    } else {
                        it
                    }
                },
            sessionStore.sessionsFlow().toUserListFlow()
                // Take only 1 emission of the sessions, else when the user actually logged in it will trigger a navigation to OnBoarding.
                .take(1),
        ) { elementClassicConnectionState, existingSessions ->
            when (elementClassicConnectionState) {
                ElementClassicConnectionState.Idle -> {
                    NavigationEvent.Idle
                }
                ElementClassicConnectionState.ElementClassicNotFound,
                ElementClassicConnectionState.ElementClassicReadyNoSession,
                is ElementClassicConnectionState.Error -> {
                    NavigationEvent.NavigateToOnBoarding
                }
                is ElementClassicConnectionState.ElementClassicReady -> {
                    if (elementClassicConnectionState.elementClassicSession.userId.value in existingSessions) {
                        NavigationEvent.NavigateToOnBoarding
                    } else {
                        // 2 cases when this can be run:
                        // First time this screen will be displayed
                        // Missing key backup screen was displayed, but the data has changed (user set up the key backup on Classic),
                        // and the app is resuming.
                        NavigationEvent.NavigateToLoginWithClassic(elementClassicConnectionState.elementClassicSession.userId)
                    }
                }
            }
        }
    }
}
