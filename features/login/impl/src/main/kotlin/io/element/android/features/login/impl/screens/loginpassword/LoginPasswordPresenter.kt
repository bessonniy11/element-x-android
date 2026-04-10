/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2023-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.login.impl.screens.loginpassword

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import dev.zacsweers.metro.Inject
import io.element.android.features.login.impl.accountprovider.AccountProviderDataSource
import io.element.android.features.login.impl.customauth.CustomAuthService
import io.element.android.libraries.architecture.AsyncData
import io.element.android.libraries.architecture.Presenter
import io.element.android.libraries.matrix.api.auth.MatrixAuthenticationService
import io.element.android.libraries.matrix.api.core.SessionId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Inject
class LoginPasswordPresenter(
    private val authenticationService: MatrixAuthenticationService,
    private val accountProviderDataSource: AccountProviderDataSource,
    private val customAuthService: CustomAuthService,
) : Presenter<LoginPasswordState> {
    @Composable
    override fun present(): LoginPasswordState {
        val localCoroutineScope = rememberCoroutineScope()
        val loginAction: MutableState<AsyncData<SessionId>> = remember {
            mutableStateOf(AsyncData.Uninitialized)
        }
        val passwordResetAction: MutableState<AsyncData<Unit>> = remember {
            mutableStateOf(AsyncData.Uninitialized)
        }

        val formState = rememberSaveable {
            mutableStateOf(LoginFormState.Default)
        }
        val accountProvider by accountProviderDataSource.flow.collectAsState()
        val canUseCustomPasswordReset = remember(accountProvider.url) {
            customAuthService.isManagedHomeserver(accountProvider.url)
        }

        fun handleEvent(event: LoginPasswordEvents) {
            when (event) {
                is LoginPasswordEvents.SetLogin -> updateFormState(formState) {
                    copy(login = event.login)
                }
                is LoginPasswordEvents.SetPassword -> updateFormState(formState) {
                    copy(password = event.password)
                }
                LoginPasswordEvents.Submit -> {
                    localCoroutineScope.submit(
                        accountProviderUrl = accountProvider.url,
                        formState = formState.value,
                        loggedInState = loginAction,
                    )
                }
                LoginPasswordEvents.RequestPasswordReset -> {
                    localCoroutineScope.requestPasswordReset(
                        accountProviderUrl = accountProvider.url,
                        identifier = formState.value.login.trim(),
                        passwordResetState = passwordResetAction,
                    )
                }
                LoginPasswordEvents.ClearError -> loginAction.value = AsyncData.Uninitialized
                LoginPasswordEvents.ClearPasswordResetNotice -> passwordResetAction.value = AsyncData.Uninitialized
            }
        }

        return LoginPasswordState(
            accountProvider = accountProvider,
            canUseCustomPasswordReset = canUseCustomPasswordReset,
            formState = formState.value,
            loginAction = loginAction.value,
            passwordResetAction = passwordResetAction.value,
            eventSink = ::handleEvent,
        )
    }

    private fun CoroutineScope.submit(
        accountProviderUrl: String,
        formState: LoginFormState,
        loggedInState: MutableState<AsyncData<SessionId>>
    ) = launch {
        loggedInState.value = AsyncData.Loading()
        val result = if (customAuthService.isManagedHomeserver(accountProviderUrl)) {
            customAuthService.loginByIdentifier(
                homeserverUrl = accountProviderUrl,
                identifier = formState.login.trim(),
                password = formState.password,
            ).fold(
                onSuccess = { externalSession ->
                    authenticationService.importCreatedSession(externalSession)
                },
                onFailure = {
                    // Compatibility fallback: allow legacy login if gateway is unavailable.
                    authenticationService.login(formState.login.trim(), formState.password)
                }
            )
        } else {
            authenticationService.login(formState.login.trim(), formState.password)
        }

        result
            .onSuccess { sessionId ->
                loggedInState.value = AsyncData.Success(sessionId)
            }
            .onFailure { failure ->
                loggedInState.value = AsyncData.Failure(failure)
            }
    }

    private fun CoroutineScope.requestPasswordReset(
        accountProviderUrl: String,
        identifier: String,
        passwordResetState: MutableState<AsyncData<Unit>>,
    ) = launch {
        if (identifier.isBlank()) return@launch
        passwordResetState.value = AsyncData.Loading()
        if (customAuthService.isManagedHomeserver(accountProviderUrl)) {
            customAuthService.requestPasswordReset(
                homeserverUrl = accountProviderUrl,
                identifier = identifier,
            )
        }
        // Always present the same UX result to avoid account enumeration.
        passwordResetState.value = AsyncData.Success(Unit)
    }

    private fun updateFormState(formState: MutableState<LoginFormState>, updateLambda: LoginFormState.() -> LoginFormState) {
        formState.value = updateLambda(formState.value)
    }
}
