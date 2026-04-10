/*
 * Copyright (c) 2026 Element Creations Ltd.
 * Copyright 2026 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.login.impl.screens.registrationverify

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
import androidx.compose.ui.platform.LocalAutofillManager
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import io.element.android.compound.tokens.generated.CompoundIcons
import io.element.android.features.login.impl.R
import io.element.android.features.login.impl.customauth.RegistrationVerifyAcceptance
import io.element.android.libraries.architecture.AsyncData
import io.element.android.libraries.designsystem.atomic.molecules.ButtonColumnMolecule
import io.element.android.libraries.designsystem.atomic.molecules.IconTitleSubtitleMolecule
import io.element.android.libraries.designsystem.components.BigIcon
import io.element.android.libraries.designsystem.components.button.BackButton
import io.element.android.libraries.designsystem.components.dialogs.ErrorDialog
import io.element.android.libraries.designsystem.components.form.textFieldState
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
fun RegistrationVerifyView(
    state: RegistrationVerifyState,
    onBackClick: () -> Unit,
    onSubmitSuccess: (RegistrationVerifyAcceptance) -> Unit,
    modifier: Modifier = Modifier,
) {
    val autofillManager = LocalAutofillManager.current
    val eventSink = state.eventSink
    val isLoading by remember(state.verifyAction) {
        derivedStateOf { state.verifyAction is AsyncData.Loading }
    }

    LaunchedEffect(state.verifyAction) {
        val action = state.verifyAction
        if (action is AsyncData.Success) {
            onSubmitSuccess(action.data)
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
                .verticalScroll(state = rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, bottom = 20.dp),
        ) {
            IconTitleSubtitleMolecule(
                modifier = Modifier.padding(top = 20.dp, start = 16.dp, end = 16.dp),
                iconStyle = BigIcon.Style.Default(CompoundIcons.KeySolid()),
                title = stringResource(id = R.string.screen_registration_verify_title),
                subTitle = stringResource(id = R.string.screen_registration_verify_subtitle, state.email),
            )
            Spacer(Modifier.height(40.dp))

            var passwordFieldState by textFieldState(stateValue = state.password)
            var passwordVisible by remember { mutableStateOf(false) }
            TextField(
                label = stringResource(id = R.string.screen_registration_verify_password_label),
                value = passwordFieldState,
                enabled = !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(TestTags.loginPassword)
                    .semantics {
                        contentType = ContentType.Password
                    },
                onValueChange = {
                    val sanitized = it.sanitize()
                    passwordFieldState = sanitized
                    eventSink(RegistrationVerifyEvents.SetPassword(sanitized))
                },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    val image = if (passwordVisible) CompoundIcons.VisibilityOn() else CompoundIcons.VisibilityOff()
                    val description = if (passwordVisible) {
                        stringResource(CommonStrings.a11y_hide_password)
                    } else {
                        stringResource(CommonStrings.a11y_show_password)
                    }
                    Box(Modifier.clickable { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = image,
                            contentDescription = description,
                        )
                    }
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(onDone = {
                    focusManager.clearFocus(force = true)
                    eventSink(RegistrationVerifyEvents.Submit)
                }),
                singleLine = true,
            )

            Spacer(Modifier.height(24.dp))
            Spacer(modifier = Modifier.weight(1f))

            ButtonColumnMolecule {
                Button(
                    text = stringResource(CommonStrings.action_continue),
                    showProgress = isLoading,
                    onClick = { eventSink(RegistrationVerifyEvents.Submit) },
                    enabled = state.submitEnabled,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(TestTags.loginContinue)
                )
                Spacer(modifier = Modifier.height(48.dp))
            }
        }

        if (state.verifyAction is AsyncData.Failure) {
            ErrorDialog(
                title = stringResource(CommonStrings.dialog_title_error),
                content = stringResource(id = R.string.screen_registration_error_generic),
                onSubmit = { eventSink(RegistrationVerifyEvents.ClearError) },
            )
        }
    }
}

private fun String.sanitize(): String = replace("\n", "")
