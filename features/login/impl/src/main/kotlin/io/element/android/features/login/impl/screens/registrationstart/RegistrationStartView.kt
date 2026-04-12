/*
 * Copyright (c) 2026 Element Creations Ltd.
 * Copyright 2026 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.login.impl.screens.registrationstart

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalAutofillManager
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import io.element.android.compound.tokens.generated.CompoundIcons
import io.element.android.features.login.impl.R
import io.element.android.features.login.impl.customauth.defaultCustomAuthLegalLinks
import io.element.android.features.login.impl.customauth.RegistrationStartAcceptance
import io.element.android.features.login.impl.screens.common.LegalLinksSection
import io.element.android.libraries.architecture.AsyncData
import io.element.android.libraries.designsystem.atomic.molecules.ButtonColumnMolecule
import io.element.android.libraries.designsystem.atomic.molecules.IconTitleSubtitleMolecule
import io.element.android.libraries.designsystem.components.BigIcon
import io.element.android.libraries.designsystem.components.button.BackButton
import io.element.android.libraries.designsystem.components.dialogs.ErrorDialog
import io.element.android.libraries.designsystem.components.form.textFieldState
import io.element.android.libraries.designsystem.modifiers.onTabOrEnterKeyFocusNext
import io.element.android.libraries.designsystem.theme.components.Button
import io.element.android.libraries.designsystem.theme.components.Icon
import io.element.android.libraries.designsystem.theme.components.Scaffold
import io.element.android.libraries.designsystem.theme.components.TextField
import io.element.android.libraries.designsystem.theme.components.TopAppBar
import io.element.android.libraries.testtags.TestTags
import io.element.android.libraries.testtags.testTag
import io.element.android.libraries.ui.strings.CommonStrings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegistrationStartView(
    state: RegistrationStartState,
    onBackClick: () -> Unit,
    onSubmitSuccess: (RegistrationStartAcceptance, String, String) -> Unit,
    privacyPolicyUrl: String = defaultCustomAuthLegalLinks().privacyPolicyUrl,
    termsUrl: String = defaultCustomAuthLegalLinks().termsUrl,
    modifier: Modifier = Modifier,
) {
    val autofillManager = LocalAutofillManager.current
    val eventSink = state.eventSink
    val isLoading by remember(state.startAction) {
        derivedStateOf { state.startAction is AsyncData.Loading }
    }

    LaunchedEffect(state.startAction) {
        val action = state.startAction
        if (action is AsyncData.Success) {
            onSubmitSuccess(action.data, state.login.trim(), state.email.trim())
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    BackButton(
                        onClick = {
                            autofillManager?.cancel()
                            onBackClick()
                        }
                    )
                },
            )
        }
    ) { padding ->
        val focusManager = LocalFocusManager.current
        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .padding(padding)
                .consumeWindowInsets(padding)
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(state = rememberScrollState())
                    .padding(start = 20.dp, end = 20.dp),
            ) {
                IconTitleSubtitleMolecule(
                    modifier = Modifier.padding(top = 20.dp, start = 16.dp, end = 16.dp),
                    iconStyle = BigIcon.Style.Default(CompoundIcons.UserProfileSolid()),
                    title = stringResource(id = R.string.screen_registration_start_title),
                    subTitle = stringResource(id = R.string.screen_registration_start_subtitle, state.accountProvider.title),
                )
                Spacer(Modifier.height(40.dp))

                var loginFieldState by textFieldState(stateValue = state.login)
                TextField(
                    label = stringResource(id = R.string.screen_registration_start_login_label),
                    value = loginFieldState,
                    enabled = !isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .onTabOrEnterKeyFocusNext(focusManager)
                        .testTag(TestTags.loginEmailUsername)
                        .semantics {
                            contentType = ContentType.Username
                        },
                    onValueChange = {
                        val sanitized = it.sanitize()
                        loginFieldState = sanitized
                        eventSink(RegistrationStartEvents.SetLogin(sanitized))
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Next,
                    ),
                    keyboardActions = KeyboardActions(onNext = {
                        focusManager.moveFocus(FocusDirection.Down)
                    }),
                    supportingText = if (state.login.isNotBlank() && !state.isLoginValid) {
                        stringResource(id = R.string.screen_registration_start_login_error)
                    } else {
                        null
                    },
                    singleLine = true,
                    trailingIcon = if (loginFieldState.isNotEmpty()) {
                        {
                            Box(Modifier.clickable {
                                loginFieldState = ""
                                eventSink(RegistrationStartEvents.SetLogin(""))
                            }) {
                                Icon(
                                    imageVector = CompoundIcons.Close(),
                                    contentDescription = stringResource(CommonStrings.action_clear)
                                )
                            }
                        }
                    } else {
                        null
                    },
                )

                Spacer(Modifier.height(20.dp))

                var emailFieldState by textFieldState(stateValue = state.email)
                TextField(
                    label = stringResource(id = R.string.screen_registration_start_email_label),
                    value = emailFieldState,
                    enabled = !isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .onTabOrEnterKeyFocusNext(focusManager)
                        .semantics {
                            contentType = ContentType.EmailAddress
                        },
                    onValueChange = {
                        val sanitized = it.sanitize()
                        emailFieldState = sanitized
                        eventSink(RegistrationStartEvents.SetEmail(sanitized))
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(onDone = {
                        focusManager.clearFocus(force = true)
                        eventSink(RegistrationStartEvents.Submit)
                    }),
                    supportingText = if (state.email.isNotBlank() && !state.isEmailValid) {
                        stringResource(id = R.string.screen_registration_start_email_error)
                    } else {
                        null
                    },
                    singleLine = true,
                    trailingIcon = if (emailFieldState.isNotEmpty()) {
                        {
                            Box(Modifier.clickable {
                                emailFieldState = ""
                                eventSink(RegistrationStartEvents.SetEmail(""))
                            }) {
                                Icon(
                                    imageVector = CompoundIcons.Close(),
                                    contentDescription = stringResource(CommonStrings.action_clear)
                                )
                            }
                        }
                    } else {
                        null
                    },
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 36.dp, end = 36.dp, bottom = 20.dp),
            ) {
                ButtonColumnMolecule {
                    Button(
                        text = stringResource(id = R.string.screen_registration_start_submit),
                        showProgress = isLoading,
                        onClick = { eventSink(RegistrationStartEvents.Submit) },
                        enabled = state.submitEnabled,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag(TestTags.loginContinue)
                    )
                }
                Spacer(Modifier.height(12.dp))
                LegalLinksSection(
                    privacyPolicyUrl = privacyPolicyUrl,
                    termsUrl = termsUrl,
                )
            }
        }

        if (state.startAction is AsyncData.Failure) {
            ErrorDialog(
                title = stringResource(CommonStrings.dialog_title_error),
                content = stringResource(id = R.string.screen_registration_error_generic),
                onSubmit = { eventSink(RegistrationStartEvents.ClearError) },
            )
        }
    }
}

private fun String.sanitize(): String = replace("\n", "")
