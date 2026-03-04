/*
 * Copyright (c) 2026 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

@file:OptIn(ExperimentalCoroutinesApi::class)

package io.element.android.features.login.impl.screens.checkclassic

import com.google.common.truth.Truth.assertThat
import io.element.android.features.login.impl.classic.ElementClassicConnection
import io.element.android.features.login.impl.classic.ElementClassicConnectionState
import io.element.android.features.login.impl.classic.FakeElementClassicConnection
import io.element.android.features.login.impl.classic.anElementClassicReady
import io.element.android.libraries.matrix.test.A_FAILURE_REASON
import io.element.android.libraries.matrix.test.A_USER_ID
import io.element.android.libraries.matrix.test.A_USER_ID_2
import io.element.android.libraries.sessionstorage.api.SessionStore
import io.element.android.libraries.sessionstorage.test.InMemorySessionStore
import io.element.android.libraries.sessionstorage.test.aSessionData
import io.element.android.tests.testutils.WarmUpRule
import io.element.android.tests.testutils.lambda.lambdaRecorder
import io.element.android.tests.testutils.test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class CheckClassicPresenterTest {
    @get:Rule
    val warmUpRule = WarmUpRule()

    @Test
    fun `present - initial state`() = runTest {
        val presenter = createPresenter()
        presenter.test {
            val initialState = awaitItem()
            assertThat(initialState.elementClassicSession.isLoading()).isTrue()
        }
    }

    @Test
    fun `present - emit request data invokes the expected method`() = runTest {
        val requestVerifiedUserIdResult = lambdaRecorder<Unit> {}
        val presenter = createPresenter(
            elementClassicConnection = FakeElementClassicConnection(
                startResult = {},
                requestVerifiedUserIdResult = requestVerifiedUserIdResult,
            ),
        )
        presenter.test {
            val initialState = awaitItem()
            assertThat(initialState.elementClassicSession.isLoading()).isTrue()
            initialState.eventSink(CheckClassicEvent.RefreshData)
        }
        requestVerifiedUserIdResult.assertions().isCalledOnce()
    }

    @Test
    fun `present - cannot sign in if a session with the same account already exists`() = runTest {
        val elementClassicConnection = FakeElementClassicConnection(
            startResult = {},
        )
        val presenter = createPresenter(
            elementClassicConnection = elementClassicConnection,
            sessionStore = InMemorySessionStore(
                initialList = listOf(
                    aSessionData(
                        sessionId = A_USER_ID.value,
                    )
                )
            ),
        )
        presenter.test {
            val initialState = awaitItem()
            assertThat(initialState.elementClassicSession.isLoading()).isTrue()
            elementClassicConnection.emitState(
                anElementClassicReady()
            )
            val finalState = awaitItem()
            assertThat(finalState.elementClassicSession.isFailure()).isTrue()
        }
    }

    @Test
    fun `present - cannot sign in if Element Classic is not found`() = runTest {
        val elementClassicConnection = FakeElementClassicConnection(
            startResult = {},
        )
        val presenter = createPresenter(
            elementClassicConnection = elementClassicConnection,
        )
        presenter.test {
            val initialState = awaitItem()
            assertThat(initialState.elementClassicSession.isLoading()).isTrue()
            elementClassicConnection.emitState(
                ElementClassicConnectionState.ElementClassicNotFound
            )
            val finalState = awaitItem()
            assertThat(finalState.elementClassicSession.isFailure()).isTrue()
        }
    }

    @Test
    fun `present - cannot sign in if Element Classic has no session`() = runTest {
        val elementClassicConnection = FakeElementClassicConnection(
            startResult = {},
        )
        val presenter = createPresenter(
            elementClassicConnection = elementClassicConnection,
        )
        presenter.test {
            val initialState = awaitItem()
            assertThat(initialState.elementClassicSession.isLoading()).isTrue()
            elementClassicConnection.emitState(
                ElementClassicConnectionState.ElementClassicReadyNoSession
            )
            val finalState = awaitItem()
            assertThat(finalState.elementClassicSession.isFailure()).isTrue()
        }
    }

    @Test
    fun `present - cannot sign in if there has been an error`() = runTest {
        val elementClassicConnection = FakeElementClassicConnection(
            startResult = {},
        )
        val presenter = createPresenter(
            elementClassicConnection = elementClassicConnection,
        )
        presenter.test {
            val initialState = awaitItem()
            assertThat(initialState.elementClassicSession.isLoading()).isTrue()
            elementClassicConnection.emitState(
                ElementClassicConnectionState.Error(A_FAILURE_REASON)
            )
            val finalState = awaitItem()
            assertThat(finalState.elementClassicSession.isFailure()).isTrue()
        }
    }

    @Test
    fun `present - can sign in when the session can be retrieved`() = runTest {
        val elementClassicConnection = FakeElementClassicConnection(
            startResult = {},
        )
        val presenter = createPresenter(
            elementClassicConnection = elementClassicConnection,
        )
        presenter.test {
            val initialState = awaitItem()
            assertThat(initialState.elementClassicSession.isLoading()).isTrue()
            elementClassicConnection.emitState(
                anElementClassicReady()
            )
            val finalState = awaitItem()
            assertThat(finalState.elementClassicSession.isSuccess()).isTrue()
            assertThat(finalState.elementClassicSession.dataOrNull()).isEqualTo(A_USER_ID)
        }
    }

    @Test
    fun `present - can sign in if a session with another account already exists`() = runTest {
        val elementClassicConnection = FakeElementClassicConnection(
            startResult = {},
        )
        val presenter = createPresenter(
            elementClassicConnection = elementClassicConnection,
            sessionStore = InMemorySessionStore(
                initialList = listOf(
                    aSessionData(
                        sessionId = A_USER_ID_2.value,
                    )
                )
            ),
        )
        presenter.test {
            val initialState = awaitItem()
            assertThat(initialState.elementClassicSession.isLoading()).isTrue()
            elementClassicConnection.emitState(
                anElementClassicReady()
            )
            val finalState = awaitItem()
            assertThat(finalState.elementClassicSession.isSuccess()).isTrue()
            assertThat(finalState.elementClassicSession.dataOrNull()).isEqualTo(A_USER_ID)
        }
    }
}

private fun createPresenter(
    elementClassicConnection: ElementClassicConnection = FakeElementClassicConnection(),
    sessionStore: SessionStore = InMemorySessionStore(),
) = CheckClassicPresenter(
    elementClassicConnection = elementClassicConnection,
    sessionStore = sessionStore,
)
