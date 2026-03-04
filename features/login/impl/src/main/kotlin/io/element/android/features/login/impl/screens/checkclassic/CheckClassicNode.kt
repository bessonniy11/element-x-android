/*
 * Copyright (c) 2026 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.login.impl.screens.checkclassic

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.bumble.appyx.core.modality.BuildContext
import com.bumble.appyx.core.node.Node
import com.bumble.appyx.core.plugin.Plugin
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedInject
import io.element.android.annotations.ContributesNode
import io.element.android.libraries.architecture.AsyncData
import io.element.android.libraries.architecture.callback
import io.element.android.libraries.matrix.api.core.UserId

@ContributesNode(AppScope::class)
@AssistedInject
class CheckClassicNode(
    @Assisted buildContext: BuildContext,
    @Assisted plugins: List<Plugin>,
    private val presenter: CheckClassicPresenter,
) : Node(
    buildContext = buildContext,
    plugins = plugins
) {
    interface Callback : Plugin {
        fun navigateToClassic(userId: UserId)
        fun navigateToOnBoarding()
    }

    private val callback: Callback = callback()

    @Composable
    override fun View(modifier: Modifier) {
        val state = presenter.present()
        LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
            state.eventSink(CheckClassicEvent.RefreshData)
        }
        LaunchedEffect(state.elementClassicSession) {
            when (state.elementClassicSession) {
                AsyncData.Uninitialized,
                is AsyncData.Loading -> Unit
                is AsyncData.Failure -> callback.navigateToOnBoarding()
                is AsyncData.Success -> callback.navigateToClassic(state.elementClassicSession.data)
            }
        }
        CheckClassicView()
    }
}
