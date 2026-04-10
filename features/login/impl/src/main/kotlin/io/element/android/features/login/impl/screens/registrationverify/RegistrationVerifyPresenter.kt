/*
 * Copyright (c) 2026 Element Creations Ltd.
 * Copyright 2026 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.login.impl.screens.registrationverify

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import io.element.android.features.login.impl.customauth.CustomAuthService
import io.element.android.features.login.impl.customauth.RegistrationVerifyAcceptance
import io.element.android.libraries.architecture.AsyncData
import io.element.android.libraries.architecture.Presenter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@AssistedInject
class RegistrationVerifyPresenter(
    @Assisted private val registrationSessionId: String,
    @Assisted private val login: String,
    @Assisted private val email: String,
    private val customAuthService: CustomAuthService,
) : Presenter<RegistrationVerifyState> {
    @AssistedFactory
    interface Factory {
        fun create(
            registrationSessionId: String,
            login: String,
            email: String,
        ): RegistrationVerifyPresenter
    }

    @Composable
    override fun present(): RegistrationVerifyState {
        val localCoroutineScope = rememberCoroutineScope()
        val verifyAction: MutableState<AsyncData<RegistrationVerifyAcceptance>> = remember {
            mutableStateOf(AsyncData.Uninitialized)
        }
        var password by rememberSaveable { mutableStateOf("") }

        fun handleEvent(event: RegistrationVerifyEvents) {
            when (event) {
                is RegistrationVerifyEvents.SetPassword -> {
                    password = event.password
                }
                RegistrationVerifyEvents.Submit -> {
                    localCoroutineScope.verifyPassword(
                        registrationSessionId = registrationSessionId,
                        password = password,
                        verifyAction = verifyAction,
                    )
                }
                RegistrationVerifyEvents.ClearError -> {
                    verifyAction.value = AsyncData.Uninitialized
                }
            }
        }

        return RegistrationVerifyState(
            login = login,
            email = email,
            registrationSessionId = registrationSessionId,
            password = password,
            verifyAction = verifyAction.value,
            eventSink = ::handleEvent,
        )
    }

    private fun CoroutineScope.verifyPassword(
        registrationSessionId: String,
        password: String,
        verifyAction: MutableState<AsyncData<RegistrationVerifyAcceptance>>,
    ) = launch {
        if (password.isBlank()) return@launch
        verifyAction.value = AsyncData.Loading()
        customAuthService.verifyRegistrationPassword(
            registrationSessionId = registrationSessionId,
            password = password,
        ).onSuccess { acceptance ->
            verifyAction.value = AsyncData.Success(acceptance)
        }.onFailure { failure ->
            verifyAction.value = AsyncData.Failure(failure)
        }
    }
}

