/*
 * Copyright (c) 2026 Element Creations Ltd.
 * Copyright 2026 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.login.impl.screens.registrationresume

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import com.bumble.appyx.core.modality.BuildContext
import com.bumble.appyx.core.node.Node
import com.bumble.appyx.core.plugin.Plugin
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedInject
import io.element.android.annotations.ContributesNode
import io.element.android.features.login.impl.registrationdraft.RegistrationDraft
import io.element.android.libraries.architecture.callback

@ContributesNode(AppScope::class)
@AssistedInject
class RegistrationResumeNode(
    @Assisted buildContext: BuildContext,
    @Assisted plugins: List<Plugin>,
    private val presenter: RegistrationResumePresenter,
) : Node(buildContext = buildContext, plugins = plugins) {
    interface Callback : Plugin {
        fun onNoDraftFound()
        fun onContinueDraft(draft: RegistrationDraft)
        fun onStartOver()
    }

    private val callback: Callback = callback()

    @Composable
    override fun View(modifier: Modifier) {
        val state = presenter.present()
        val draft = state.draft

        if (draft == null) {
            LaunchedEffect(Unit) {
                callback.onNoDraftFound()
            }
            return
        }

        RegistrationResumeView(
            draft = draft,
            onContinueClick = { callback.onContinueDraft(draft) },
            onStartOverClick = callback::onStartOver,
            modifier = modifier,
        )
    }
}

