/*
 * Copyright (c) 2026 Element Creations Ltd.
 * Copyright 2026 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.login.impl.screens.registrationprofile

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.bumble.appyx.core.modality.BuildContext
import com.bumble.appyx.core.node.Node
import com.bumble.appyx.core.plugin.Plugin
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedInject
import io.element.android.annotations.ContributesNode
import io.element.android.libraries.architecture.NodeInputs
import io.element.android.libraries.architecture.inputs

@ContributesNode(AppScope::class)
@AssistedInject
class RegistrationProfileNode(
    @Assisted buildContext: BuildContext,
    @Assisted plugins: List<Plugin>,
    presenterFactory: RegistrationProfilePresenter.Factory,
) : Node(buildContext = buildContext, plugins = plugins) {
    data class Inputs(
        val verifiedToken: String,
        val email: String,
    ) : NodeInputs

    private val inputs = inputs<Inputs>()
    private val presenter = presenterFactory.create(
        verifiedToken = inputs.verifiedToken,
        email = inputs.email,
    )

    @Composable
    override fun View(modifier: Modifier) {
        val state = presenter.present()
        RegistrationProfileView(
            state = state,
            onBackClick = ::navigateUp,
            modifier = modifier,
        )
    }
}

