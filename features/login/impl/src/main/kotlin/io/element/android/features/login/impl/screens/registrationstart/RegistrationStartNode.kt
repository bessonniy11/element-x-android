/*
 * Copyright (c) 2026 Element Creations Ltd.
 * Copyright 2026 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.login.impl.screens.registrationstart

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.bumble.appyx.core.modality.BuildContext
import com.bumble.appyx.core.node.Node
import com.bumble.appyx.core.plugin.Plugin
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedInject
import io.element.android.annotations.ContributesNode
import io.element.android.features.login.impl.customauth.CustomAuthService
import io.element.android.features.login.impl.customauth.RegistrationStartAcceptance
import io.element.android.features.login.impl.screens.common.rememberRuntimeLegalLinks
import io.element.android.libraries.architecture.callback

@ContributesNode(AppScope::class)
@AssistedInject
class RegistrationStartNode(
    @Assisted buildContext: BuildContext,
    @Assisted plugins: List<Plugin>,
    private val presenter: RegistrationStartPresenter,
    private val customAuthService: CustomAuthService,
) : Node(buildContext = buildContext, plugins = plugins) {
    interface Callback : Plugin {
        fun onBack()
        fun onRegistrationStarted(
            acceptance: RegistrationStartAcceptance,
            login: String,
            email: String,
        )
    }

    private val callback: Callback = callback()

    @Composable
    override fun View(modifier: Modifier) {
        val state = presenter.present()
        val legalLinks = rememberRuntimeLegalLinks(
            homeserverUrl = state.accountProvider.url,
            customAuthService = customAuthService,
        )
        RegistrationStartView(
            state = state,
            privacyPolicyUrl = legalLinks.privacyPolicyUrl,
            termsUrl = legalLinks.termsUrl,
            onBackClick = callback::onBack,
            onSubmitSuccess = { acceptance, login, email ->
                callback.onRegistrationStarted(
                    acceptance = acceptance,
                    login = login,
                    email = email,
                )
            },
            modifier = modifier,
        )
    }
}
