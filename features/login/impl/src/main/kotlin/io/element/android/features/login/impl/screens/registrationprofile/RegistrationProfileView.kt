/*
 * Copyright (c) 2026 Element Creations Ltd.
 * Copyright 2026 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.login.impl.screens.registrationprofile

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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.unit.dp
import io.element.android.compound.tokens.generated.CompoundIcons
import io.element.android.features.login.impl.R
import io.element.android.libraries.architecture.AsyncData
import io.element.android.libraries.designsystem.atomic.molecules.ButtonColumnMolecule
import io.element.android.libraries.designsystem.atomic.molecules.IconTitleSubtitleMolecule
import io.element.android.libraries.designsystem.components.BigIcon
import io.element.android.libraries.designsystem.components.button.BackButton
import io.element.android.libraries.designsystem.components.dialogs.ErrorDialog
import io.element.android.libraries.designsystem.components.form.textFieldState
import io.element.android.libraries.designsystem.modifiers.onTabOrEnterKeyFocusNext
import io.element.android.libraries.designsystem.theme.components.Button
import io.element.android.libraries.designsystem.theme.components.Scaffold
import io.element.android.libraries.designsystem.theme.components.TextField
import io.element.android.libraries.designsystem.theme.components.TopAppBar
import io.element.android.libraries.testtags.TestTags
import io.element.android.libraries.testtags.testTag
import io.element.android.libraries.ui.strings.CommonStrings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegistrationProfileView(
    state: RegistrationProfileState,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val eventSink = state.eventSink
    val isLoading by androidx.compose.runtime.remember(state.completeAction) {
        derivedStateOf { state.completeAction is AsyncData.Loading }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    BackButton(onClick = onBackClick)
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
                iconStyle = BigIcon.Style.Default(CompoundIcons.UserProfileSolid()),
                title = stringResource(id = R.string.screen_registration_profile_title),
                subTitle = stringResource(id = R.string.screen_registration_profile_subtitle, state.accountProvider.title),
            )
            Spacer(Modifier.height(40.dp))

            var displayNameFieldState by textFieldState(stateValue = state.displayName)
            TextField(
                label = stringResource(id = R.string.screen_registration_profile_display_name_label),
                value = displayNameFieldState,
                enabled = !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .onTabOrEnterKeyFocusNext(focusManager),
                onValueChange = {
                    val sanitized = it.sanitize()
                    displayNameFieldState = sanitized
                    eventSink(RegistrationProfileEvents.SetDisplayName(sanitized))
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Next,
                ),
                keyboardActions = KeyboardActions(onNext = {
                    focusManager.moveFocus(FocusDirection.Down)
                }),
                singleLine = true,
            )

            Spacer(Modifier.height(20.dp))

            var phoneFieldState by remember {
                val initialDigits = extractRuPhoneDigits(state.phone)
                mutableStateOf(
                    TextFieldValue(
                        text = initialDigits,
                        selection = TextRange(initialDigits.length),
                    )
                )
            }
            LaunchedEffect(state.phone) {
                val nextDigits = extractRuPhoneDigits(state.phone)
                if (phoneFieldState.text != nextDigits) {
                    phoneFieldState = TextFieldValue(
                        text = nextDigits,
                        selection = TextRange(nextDigits.length),
                    )
                }
            }
            TextField(
                label = stringResource(id = R.string.screen_registration_profile_phone_label),
                value = phoneFieldState,
                enabled = !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .onTabOrEnterKeyFocusNext(focusManager),
                onValueChange = {
                    val sanitized = it.text.sanitize()
                    val normalizedDigits = extractRuPhoneDigits(sanitized)
                    val digitsBeforeCursor = countRuDigitsBeforeCursor(
                        text = sanitized,
                        cursor = it.selection.start,
                    )
                    val nextCursor = digitsBeforeCursor.coerceIn(0, normalizedDigits.length)
                    phoneFieldState = TextFieldValue(
                        text = normalizedDigits,
                        selection = TextRange(nextCursor),
                    )
                    eventSink(RegistrationProfileEvents.SetPhone(formatRuPhoneInput(normalizedDigits)))
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Phone,
                    imeAction = ImeAction.Next,
                ),
                keyboardActions = KeyboardActions(onNext = {
                    focusManager.moveFocus(FocusDirection.Down)
                }),
                singleLine = true,
                visualTransformation = RuPhoneVisualTransformation,
            )

            Spacer(Modifier.height(20.dp))

            TextField(
                label = stringResource(id = R.string.screen_registration_profile_email_label),
                value = state.email,
                enabled = false,
                modifier = Modifier.fillMaxWidth(),
                onValueChange = {},
                singleLine = true,
            )

            Spacer(Modifier.height(20.dp))
            Spacer(modifier = Modifier.weight(1f))

            val formattedPhoneForSubmit = formatRuPhoneInput(phoneFieldState.text)
            val normalizedPhoneForSubmit = normalizeRuPhoneForBackend(formattedPhoneForSubmit)
            val uiSubmitEnabled = displayNameFieldState.trim().isNotBlank() &&
                normalizedPhoneForSubmit != null &&
                !isLoading

            ButtonColumnMolecule {
                Button(
                    text = stringResource(id = R.string.screen_registration_profile_submit),
                    showProgress = isLoading,
                    onClick = {
                        val currentDisplayName = displayNameFieldState.sanitize()
                        val currentPhone = formatRuPhoneInput(phoneFieldState.text)
                        eventSink(
                            RegistrationProfileEvents.Submit(
                                displayName = currentDisplayName,
                                phone = currentPhone,
                            )
                        )
                    },
                    enabled = uiSubmitEnabled,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(TestTags.loginContinue)
                )
                Spacer(modifier = Modifier.height(48.dp))
            }
        }

        if (state.completeAction is AsyncData.Failure) {
            ErrorDialog(
                title = stringResource(CommonStrings.dialog_title_error),
                content = stringResource(id = R.string.screen_registration_error_generic),
                onSubmit = { eventSink(RegistrationProfileEvents.ClearError) },
            )
        }
    }
}

private fun String.sanitize(): String = replace("\n", "")

private fun countRuDigitsBeforeCursor(text: String, cursor: Int): Int {
    val safeCursor = cursor.coerceIn(0, text.length)
    return extractRuPhoneDigits(text.take(safeCursor)).length
}
