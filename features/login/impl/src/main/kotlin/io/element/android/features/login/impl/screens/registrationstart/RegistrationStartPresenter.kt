/*
 * Copyright (c) 2026 Element Creations Ltd.
 * Copyright 2026 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.login.impl.screens.registrationstart

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import dev.zacsweers.metro.Inject
import io.element.android.features.login.impl.accountprovider.AccountProviderDataSource
import io.element.android.features.login.impl.customauth.CustomAuthService
import io.element.android.features.login.impl.customauth.RegistrationStartAcceptance
import io.element.android.libraries.architecture.AsyncData
import io.element.android.libraries.architecture.Presenter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Inject
class RegistrationStartPresenter(
    private val accountProviderDataSource: AccountProviderDataSource,
    private val customAuthService: CustomAuthService,
) : Presenter<RegistrationStartState> {
    @Composable
    override fun present(): RegistrationStartState {
        val localCoroutineScope = rememberCoroutineScope()
        val startAction: MutableState<AsyncData<RegistrationStartAcceptance>> = remember {
            mutableStateOf(AsyncData.Uninitialized)
        }
        var login by rememberSaveable { mutableStateOf("") }
        var email by rememberSaveable { mutableStateOf("") }
        val accountProvider by accountProviderDataSource.flow.collectAsState()

        fun handleEvent(event: RegistrationStartEvents) {
            when (event) {
                is RegistrationStartEvents.SetLogin -> {
                    login = event.login
                }
                is RegistrationStartEvents.SetEmail -> {
                    email = event.email
                }
                RegistrationStartEvents.Submit -> {
                    localCoroutineScope.startRegistration(
                        homeserverUrl = accountProvider.url,
                        login = login.trim(),
                        email = email.trim(),
                        startAction = startAction,
                    )
                }
                RegistrationStartEvents.ClearError -> {
                    startAction.value = AsyncData.Uninitialized
                }
            }
        }

        val normalizedLogin = login.trim()
        val normalizedEmail = email.trim()

        return RegistrationStartState(
            accountProvider = accountProvider,
            login = login,
            email = email,
            isLoginValid = normalizedLogin.matches(LOGIN_REGEX),
            isEmailValid = EMAIL_REGEX.matches(normalizedEmail),
            startAction = startAction.value,
            eventSink = ::handleEvent,
        )
    }

    private fun CoroutineScope.startRegistration(
        homeserverUrl: String,
        login: String,
        email: String,
        startAction: MutableState<AsyncData<RegistrationStartAcceptance>>,
    ) = launch {
        if (!login.matches(LOGIN_REGEX) || !EMAIL_REGEX.matches(email)) return@launch
        startAction.value = AsyncData.Loading()
        customAuthService.startRegistration(
            homeserverUrl = homeserverUrl,
            login = login,
            email = email,
        ).onSuccess { acceptance ->
            startAction.value = AsyncData.Success(acceptance)
        }.onFailure { failure ->
            startAction.value = AsyncData.Failure(failure)
        }
    }

    companion object {
        private val LOGIN_REGEX = Regex("^[a-z]+$")
        private val EMAIL_REGEX = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")
    }
}
