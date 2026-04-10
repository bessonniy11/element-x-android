/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2023-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.login.impl

import android.app.Activity
import android.os.Parcelable
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.bumble.appyx.core.lifecycle.subscribe
import com.bumble.appyx.core.modality.BuildContext
import com.bumble.appyx.core.node.Node
import com.bumble.appyx.core.plugin.Plugin
import com.bumble.appyx.navmodel.backstack.BackStack
import com.bumble.appyx.navmodel.backstack.operation.pop
import com.bumble.appyx.navmodel.backstack.operation.push
import com.bumble.appyx.navmodel.backstack.operation.singleTop
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedInject
import io.element.android.annotations.ContributesNode
import io.element.android.compound.theme.ElementTheme
import io.element.android.features.login.api.LoginEntryPoint
import io.element.android.features.login.impl.accountprovider.AccountProviderDataSource
import io.element.android.features.login.impl.customauth.CustomAuthService
import io.element.android.features.login.impl.customauth.RegistrationStartAcceptance
import io.element.android.features.login.impl.customauth.RegistrationStatus
import io.element.android.features.login.impl.customauth.RegistrationVerifyAcceptance
import io.element.android.features.login.impl.registrationdraft.RegistrationDraft
import io.element.android.features.login.impl.registrationdraft.RegistrationDraftStep
import io.element.android.features.login.impl.registrationdraft.RegistrationDraftStore
import io.element.android.features.login.impl.qrcode.QrCodeLoginFlowNode
import io.element.android.features.login.impl.screens.authchoice.AuthChoiceNode
import io.element.android.features.login.impl.screens.changeaccountprovider.ChangeAccountProviderNode
import io.element.android.features.login.impl.screens.chooseaccountprovider.ChooseAccountProviderNode
import io.element.android.features.login.impl.screens.confirmaccountprovider.ConfirmAccountProviderNode
import io.element.android.features.login.impl.screens.createaccount.CreateAccountNode
import io.element.android.features.login.impl.screens.loginpassword.LoginPasswordNode
import io.element.android.features.login.impl.screens.onboarding.OnBoardingNode
import io.element.android.features.login.impl.screens.registrationprofile.RegistrationProfileNode
import io.element.android.features.login.impl.screens.registrationresume.RegistrationResumeNode
import io.element.android.features.login.impl.screens.registrationstart.RegistrationStartNode
import io.element.android.features.login.impl.screens.registrationverify.RegistrationVerifyNode
import io.element.android.features.login.impl.screens.serverselection.ServerSelectionNode
import io.element.android.features.login.impl.screens.searchaccountprovider.SearchAccountProviderNode
import io.element.android.libraries.androidutils.browser.openUrlInChromeCustomTab
import io.element.android.libraries.architecture.BackstackView
import io.element.android.libraries.architecture.BaseFlowNode
import io.element.android.libraries.architecture.NodeInputs
import io.element.android.libraries.architecture.callback
import io.element.android.libraries.architecture.createNode
import io.element.android.libraries.architecture.inputs
import io.element.android.libraries.di.annotations.AppCoroutineScope
import io.element.android.libraries.matrix.api.auth.OidcDetails
import io.element.android.libraries.oidc.api.OidcAction
import io.element.android.libraries.oidc.api.OidcActionFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize

@ContributesNode(AppScope::class)
@AssistedInject
class LoginFlowNode(
    @Assisted buildContext: BuildContext,
    @Assisted plugins: List<Plugin>,
    private val accountProviderDataSource: AccountProviderDataSource,
    private val customAuthService: CustomAuthService,
    private val registrationDraftStore: RegistrationDraftStore,
    private val oidcActionFlow: OidcActionFlow,
    @AppCoroutineScope
    private val appCoroutineScope: CoroutineScope,
) : BaseFlowNode<LoginFlowNode.NavTarget>(
    backstack = BackStack(
        initialElement = NavTarget.RegistrationResumeChoice,
        savedStateMap = buildContext.savedStateMap,
    ),
    buildContext = buildContext,
    plugins = plugins,
) {
    data class Params(
        val accountProvider: String?,
        val loginHint: String?,
    ) : NodeInputs

    private val callback: LoginEntryPoint.Callback = callback()
    private var activity: Activity? = null
    private var darkTheme: Boolean = false

    private var externalAppStarted = false

    override fun onBuilt() {
        super.onBuilt()
        lifecycle.subscribe(
            onResume = {
                if (externalAppStarted) {
                    externalAppStarted = false
                    // Workaround to detect that the Custom Chrome Tab has been closed
                    // If there is no coming OidcAction (that would end this Node),
                    // consider that the user has cancelled the login
                    // by pressing back or by closing the Custom Chrome Tab.
                    lifecycleScope.launch {
                        delay(5000)
                        oidcActionFlow.post(OidcAction.GoBack(toUnblock = true))
                    }
                }
            }
        )
    }

    sealed interface NavTarget : Parcelable {
        @Parcelize
        data object RegistrationResumeChoice : NavTarget

        @Parcelize
        data object ServerSelection : NavTarget

        @Parcelize
        data object AuthChoice : NavTarget

        @Parcelize
        data object OnBoarding : NavTarget

        @Parcelize
        data object QrCode : NavTarget

        @Parcelize
        data class ConfirmAccountProvider(
            val isAccountCreation: Boolean,
        ) : NavTarget

        @Parcelize
        data object ChooseAccountProvider : NavTarget

        @Parcelize
        data object ChangeAccountProvider : NavTarget

        @Parcelize
        data object SearchAccountProvider : NavTarget

        @Parcelize
        data object LoginPassword : NavTarget

        @Parcelize
        data object RegistrationStart : NavTarget

        @Parcelize
        data class RegistrationVerify(
            val registrationSessionId: String,
            val login: String,
            val email: String,
        ) : NavTarget

        @Parcelize
        data class RegistrationProfile(
            val verifiedToken: String,
            val email: String,
        ) : NavTarget

        @Parcelize
        data class CreateAccount(val url: String) : NavTarget
    }

    override fun resolve(navTarget: NavTarget, buildContext: BuildContext): Node {
        return when (navTarget) {
            NavTarget.RegistrationResumeChoice -> {
                val callback = object : RegistrationResumeNode.Callback {
                    override fun onNoDraftFound() {
                        backstack.singleTop(NavTarget.ServerSelection)
                    }

                    override fun onContinueDraft(draft: RegistrationDraft) {
                        lifecycleScope.launch {
                            accountProviderDataSource.setUrl(draft.homeserverUrl)
                            val status = customAuthService
                                .getRegistrationStatus(draft.registrationSessionId)
                                .getOrNull()
                            val target = resolveResumeTarget(draft = draft, status = status)
                            if (target == null) {
                                registrationDraftStore.clear()
                                backstack.singleTop(NavTarget.ServerSelection)
                            } else {
                                backstack.singleTop(target)
                            }
                        }
                    }

                    override fun onStartOver() {
                        appCoroutineScope.launch {
                            registrationDraftStore.clear()
                        }
                        backstack.singleTop(NavTarget.ServerSelection)
                    }
                }
                createNode<RegistrationResumeNode>(buildContext, plugins = listOf(callback))
            }
            NavTarget.ServerSelection -> {
                val callback = object : ServerSelectionNode.Callback {
                    override fun onServerConfirmed() {
                        backstack.push(NavTarget.AuthChoice)
                    }

                    override fun onBack() {
                        callback.onDone()
                    }
                }
                createNode<ServerSelectionNode>(buildContext, plugins = listOf(callback))
            }
            NavTarget.AuthChoice -> {
                val callback = object : AuthChoiceNode.Callback {
                    override fun onBack() {
                        backstack.pop()
                    }

                    override fun onLoginSelected(isManagedHomeserver: Boolean) {
                        backstack.push(
                            if (isManagedHomeserver) {
                                NavTarget.LoginPassword
                            } else {
                                NavTarget.ConfirmAccountProvider(isAccountCreation = false)
                            }
                        )
                    }

                    override fun onRegisterSelected() {
                        backstack.push(NavTarget.RegistrationStart)
                    }
                }
                createNode<AuthChoiceNode>(buildContext, plugins = listOf(callback))
            }
            NavTarget.RegistrationStart -> {
                val callback = object : RegistrationStartNode.Callback {
                    override fun onBack() {
                        backstack.pop()
                    }

                    override fun onRegistrationStarted(
                        acceptance: RegistrationStartAcceptance,
                        login: String,
                        email: String,
                    ) {
                        appCoroutineScope.launch {
                            registrationDraftStore.saveDraft(
                                RegistrationDraft(
                                    homeserverUrl = accountProviderDataSource.flow.value.url,
                                    login = login,
                                    email = email,
                                    registrationSessionId = acceptance.registrationSessionId,
                                    step = RegistrationDraftStep.VerifyPassword,
                                )
                            )
                        }
                        backstack.push(
                            NavTarget.RegistrationVerify(
                                registrationSessionId = acceptance.registrationSessionId,
                                login = login,
                                email = email,
                            )
                        )
                    }
                }
                createNode<RegistrationStartNode>(buildContext, plugins = listOf(callback))
            }
            is NavTarget.RegistrationVerify -> {
                val inputs = RegistrationVerifyNode.Inputs(
                    registrationSessionId = navTarget.registrationSessionId,
                    login = navTarget.login,
                    email = navTarget.email,
                )
                val callback = object : RegistrationVerifyNode.Callback {
                    override fun onBack() {
                        backstack.pop()
                    }

                    override fun onRegistrationVerified(
                        acceptance: RegistrationVerifyAcceptance,
                        email: String,
                    ) {
                        appCoroutineScope.launch {
                            val existingDraft = registrationDraftStore.draftFlow().firstOrNull()
                            registrationDraftStore.saveDraft(
                                if (existingDraft != null) {
                                    existingDraft.copy(
                                        step = RegistrationDraftStep.Profile,
                                        verifiedToken = acceptance.verifiedToken,
                                    )
                                } else {
                                    RegistrationDraft(
                                        homeserverUrl = accountProviderDataSource.flow.value.url,
                                        login = navTarget.login,
                                        email = navTarget.email,
                                        registrationSessionId = navTarget.registrationSessionId,
                                        step = RegistrationDraftStep.Profile,
                                        verifiedToken = acceptance.verifiedToken,
                                    )
                                }
                            )
                        }
                        backstack.push(
                            NavTarget.RegistrationProfile(
                                verifiedToken = acceptance.verifiedToken,
                                email = email,
                            )
                        )
                    }
                }
                createNode<RegistrationVerifyNode>(buildContext, plugins = listOf(inputs, callback))
            }
            is NavTarget.RegistrationProfile -> {
                val inputs = RegistrationProfileNode.Inputs(
                    verifiedToken = navTarget.verifiedToken,
                    email = navTarget.email,
                )
                createNode<RegistrationProfileNode>(buildContext, plugins = listOf(inputs))
            }
            NavTarget.OnBoarding -> {
                val callback = object : OnBoardingNode.Callback {
                    override fun navigateToSignUpFlow() {
                        backstack.push(
                            NavTarget.ConfirmAccountProvider(isAccountCreation = true)
                        )
                    }

                    override fun navigateToSignInFlow(mustChooseAccountProvider: Boolean) {
                        backstack.push(
                            if (mustChooseAccountProvider) {
                                NavTarget.ChooseAccountProvider
                            } else {
                                NavTarget.ConfirmAccountProvider(isAccountCreation = false)
                            }
                        )
                    }

                    override fun navigateToQrCode() {
                        backstack.push(NavTarget.QrCode)
                    }

                    override fun navigateToBugReport() {
                        callback.navigateToBugReport()
                    }

                    override fun navigateToOidc(oidcDetails: OidcDetails) {
                        navigateToMas(oidcDetails)
                    }

                    override fun navigateToCreateAccount(url: String) {
                        backstack.push(NavTarget.CreateAccount(url))
                    }

                    override fun navigateToLoginPassword() {
                        backstack.push(NavTarget.LoginPassword)
                    }

                    override fun onDone() {
                        callback.onDone()
                    }
                }
                val params = inputs<Params>()
                val inputs = OnBoardingNode.Params(
                    accountProvider = params.accountProvider,
                    loginHint = params.loginHint,
                )
                createNode<OnBoardingNode>(buildContext, listOf(callback, inputs))
            }
            NavTarget.ChooseAccountProvider -> {
                val callback = object : ChooseAccountProviderNode.Callback {
                    override fun navigateToOidc(oidcDetails: OidcDetails) {
                        navigateToMas(oidcDetails)
                    }

                    override fun navigateToCreateAccount(url: String) {
                        backstack.push(NavTarget.CreateAccount(url))
                    }

                    override fun navigateToLoginPassword() {
                        backstack.push(NavTarget.LoginPassword)
                    }
                }
                createNode<ChooseAccountProviderNode>(buildContext, listOf(callback))
            }
            NavTarget.QrCode -> {
                val callback = object : QrCodeLoginFlowNode.Callback {
                    override fun navigateBack() {
                        backstack.pop()
                    }
                }
                createNode<QrCodeLoginFlowNode>(buildContext, listOf(callback))
            }
            is NavTarget.ConfirmAccountProvider -> {
                val inputs = ConfirmAccountProviderNode.Inputs(
                    isAccountCreation = navTarget.isAccountCreation,
                )
                val callback = object : ConfirmAccountProviderNode.Callback {
                    override fun navigateToOidc(oidcDetails: OidcDetails) {
                        navigateToMas(oidcDetails)
                    }

                    override fun navigateToCreateAccount(url: String) {
                        backstack.push(NavTarget.CreateAccount(url))
                    }

                    override fun navigateToLoginPassword() {
                        backstack.push(NavTarget.LoginPassword)
                    }

                    override fun navigateToChangeAccountProvider() {
                        backstack.push(NavTarget.ChangeAccountProvider)
                    }
                }
                createNode<ConfirmAccountProviderNode>(buildContext, plugins = listOf(inputs, callback))
            }
            NavTarget.ChangeAccountProvider -> {
                val callback = object : ChangeAccountProviderNode.Callback {
                    override fun onDone() {
                        // Go back to the Account Provider screen
                        val confirmAccountProvider = backstack.elements.value.firstOrNull {
                            it.key.navTarget is NavTarget.ConfirmAccountProvider
                        }?.key?.navTarget ?: NavTarget.ConfirmAccountProvider(isAccountCreation = false)
                        backstack.singleTop(confirmAccountProvider)
                    }

                    override fun navigateToSearchAccountProvider() {
                        backstack.push(NavTarget.SearchAccountProvider)
                    }
                }

                createNode<ChangeAccountProviderNode>(buildContext, plugins = listOf(callback))
            }
            NavTarget.SearchAccountProvider -> {
                val callback = object : SearchAccountProviderNode.Callback {
                    override fun onDone() {
                        // Go back to the Account Provider screen
                        val confirmAccountProvider = backstack.elements.value.firstOrNull {
                            it.key.navTarget is NavTarget.ConfirmAccountProvider
                        }?.key?.navTarget ?: NavTarget.ConfirmAccountProvider(isAccountCreation = false)
                        backstack.singleTop(confirmAccountProvider)
                    }
                }

                createNode<SearchAccountProviderNode>(buildContext, plugins = listOf(callback))
            }
            NavTarget.LoginPassword -> {
                createNode<LoginPasswordNode>(buildContext)
            }
            is NavTarget.CreateAccount -> {
                val inputs = CreateAccountNode.Inputs(
                    url = navTarget.url,
                )
                createNode<CreateAccountNode>(buildContext, listOf(inputs))
            }
        }
    }

    private fun navigateToMas(oidcDetails: OidcDetails) {
        activity?.let {
            externalAppStarted = true
            it.openUrlInChromeCustomTab(null, darkTheme, oidcDetails.url)
        }
    }

    @Composable
    override fun View(modifier: Modifier) {
        activity = requireNotNull(LocalActivity.current)
        darkTheme = !ElementTheme.isLightTheme
        DisposableEffect(Unit) {
            onDispose {
                activity = null
                appCoroutineScope.launch {
                    accountProviderDataSource.reset()
                }
            }
        }
        BackstackView()
    }

    private fun resolveResumeTarget(draft: RegistrationDraft, status: RegistrationStatus?): NavTarget? {
        val statusValue = status?.status?.trim()?.lowercase()
        if (statusValue == "expired" || statusValue == "completed") {
            return null
        }
        return when {
            draft.step == RegistrationDraftStep.Profile && !draft.verifiedToken.isNullOrBlank() -> {
                NavTarget.RegistrationProfile(
                    verifiedToken = draft.verifiedToken,
                    email = draft.email,
                )
            }
            else -> {
                NavTarget.RegistrationVerify(
                    registrationSessionId = draft.registrationSessionId,
                    login = draft.login,
                    email = draft.email,
                )
            }
        }
    }
}
