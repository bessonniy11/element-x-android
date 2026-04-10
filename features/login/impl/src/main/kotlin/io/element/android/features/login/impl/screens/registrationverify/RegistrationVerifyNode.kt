/*
 * Copyright (c) 2026 Element Creations Ltd.
 * Copyright 2026 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.login.impl.screens.registrationverify

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.bumble.appyx.core.modality.BuildContext
import com.bumble.appyx.core.node.Node
import com.bumble.appyx.core.plugin.Plugin
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedInject
import io.element.android.annotations.ContributesNode
import io.element.android.features.login.impl.customauth.RegistrationVerifyAcceptance
import io.element.android.libraries.architecture.NodeInputs
import io.element.android.libraries.architecture.callback
import io.element.android.libraries.architecture.inputs

@ContributesNode(AppScope::class)
@AssistedInject
class RegistrationVerifyNode(
    @Assisted buildContext: BuildContext,
    @Assisted plugins: List<Plugin>,
    presenterFactory: RegistrationVerifyPresenter.Factory,
) : Node(buildContext = buildContext, plugins = plugins) {
    data class Inputs(
        val registrationSessionId: String,
        val login: String,
        val email: String,
    ) : NodeInputs

    interface Callback : Plugin {
        fun onBack()
        fun onRegistrationVerified(
            acceptance: RegistrationVerifyAcceptance,
            email: String,
        )
    }

    private val callback: Callback = callback()
    private val inputs = inputs<Inputs>()
    private val presenter = presenterFactory.create(
        registrationSessionId = inputs.registrationSessionId,
        login = inputs.login,
        email = inputs.email,
    )

    @Composable
    override fun View(modifier: Modifier) {
        val state = presenter.present()
        RegistrationVerifyView(
            state = state,
            onBackClick = callback::onBack,
            onSubmitSuccess = { acceptance ->
                callback.onRegistrationVerified(
                    acceptance = acceptance,
                    email = state.email,
                )
            },
            modifier = modifier,
        )
    }
}

