/*
 * Copyright (c) 2026 Element Creations Ltd.
 * Copyright 2026 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.login.impl.screens.authchoice

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.bumble.appyx.core.modality.BuildContext
import com.bumble.appyx.core.node.Node
import com.bumble.appyx.core.plugin.Plugin
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedInject
import io.element.android.annotations.ContributesNode
import io.element.android.features.login.impl.accountprovider.AccountProviderDataSource
import io.element.android.features.login.impl.customauth.CustomAuthService
import io.element.android.features.login.impl.screens.common.rememberRuntimeLegalLinks
import io.element.android.libraries.architecture.callback

@ContributesNode(AppScope::class)
@AssistedInject
class AuthChoiceNode(
    @Assisted buildContext: BuildContext,
    @Assisted plugins: List<Plugin>,
    private val accountProviderDataSource: AccountProviderDataSource,
    private val customAuthService: CustomAuthService,
) : Node(buildContext = buildContext, plugins = plugins) {
    interface Callback : Plugin {
        fun onBack()
        fun onLoginSelected(isManagedHomeserver: Boolean)
        fun onRegisterSelected()
    }

    private val callback: Callback = callback()

    @Composable
    override fun View(modifier: Modifier) {
        val accountProvider by accountProviderDataSource.flow.collectAsState()
        val isManagedHomeserver = customAuthService.isManagedHomeserver(accountProvider.url)
        val legalLinks = rememberRuntimeLegalLinks(
            homeserverUrl = accountProvider.url,
            customAuthService = customAuthService,
        )

        AuthChoiceView(
            homeserverTitle = accountProvider.title,
            canRegister = isManagedHomeserver,
            privacyPolicyUrl = legalLinks.privacyPolicyUrl,
            termsUrl = legalLinks.termsUrl,
            onBackClick = callback::onBack,
            onLoginClick = { callback.onLoginSelected(isManagedHomeserver) },
            onRegisterClick = callback::onRegisterSelected,
            modifier = modifier,
        )
    }
}
