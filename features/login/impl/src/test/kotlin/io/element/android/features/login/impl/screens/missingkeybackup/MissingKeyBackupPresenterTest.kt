/*
 * Copyright (c) 2026 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.login.impl.screens.missingkeybackup

import com.google.common.truth.Truth.assertThat
import io.element.android.features.login.impl.classic.ElementClassicConnection
import io.element.android.features.login.impl.classic.FakeElementClassicConnection
import io.element.android.libraries.core.meta.BuildMeta
import io.element.android.libraries.matrix.test.AN_APPLICATION_NAME
import io.element.android.libraries.matrix.test.core.aBuildMeta
import io.element.android.tests.testutils.lambda.lambdaRecorder
import io.element.android.tests.testutils.test
import kotlinx.coroutines.test.runTest
import org.junit.Test

class MissingKeyBackupPresenterTest {
    @Test
    fun `present - initial state`() = runTest {
        val presenter = createPresenter()
        presenter.test {
            val initialState = awaitItem()
            assertThat(initialState.appName).isEqualTo(AN_APPLICATION_NAME)
        }
    }

    @Test
    fun `present - when the screen is resumed twice, the start over method is called`() = runTest {
        val resetResult = lambdaRecorder<Unit> { }
        val startOverResult = lambdaRecorder<Unit> { }
        val presenter = createPresenter(
            elementClassicConnection = FakeElementClassicConnection(
                resetResult = resetResult,
            ),
            navigator = FakeMissingKeyBackupNavigator(
                startOverResult = startOverResult,
            ),
        )
        presenter.test {
            val initialState = awaitItem()
            initialState.eventSink(MissingKeyBackupEvent.OnResume)
            expectNoEvents()
            initialState.eventSink(MissingKeyBackupEvent.OnResume)
            resetResult.assertions().isCalledOnce()
            startOverResult.assertions().isCalledOnce()
        }
    }
}

private fun createPresenter(
    navigator: MissingKeyBackupNavigator = FakeMissingKeyBackupNavigator(),
    buildMeta: BuildMeta = aBuildMeta(applicationName = AN_APPLICATION_NAME),
    elementClassicConnection: ElementClassicConnection = FakeElementClassicConnection(),
) = MissingKeyBackupPresenter(
    navigator = navigator,
    buildMeta = buildMeta,
    elementClassicConnection = elementClassicConnection,
)
