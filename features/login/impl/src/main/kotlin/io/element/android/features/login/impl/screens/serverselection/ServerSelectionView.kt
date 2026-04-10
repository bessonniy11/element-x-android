/*
 * Copyright (c) 2026 Element Creations Ltd.
 * Copyright 2026 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.login.impl.screens.serverselection

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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import io.element.android.compound.tokens.generated.CompoundIcons
import io.element.android.features.login.impl.R
import io.element.android.features.login.impl.changeserver.ChangeServerView
import io.element.android.libraries.architecture.AsyncData
import io.element.android.libraries.designsystem.atomic.molecules.ButtonColumnMolecule
import io.element.android.libraries.designsystem.atomic.molecules.IconTitleSubtitleMolecule
import io.element.android.libraries.designsystem.components.BigIcon
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
fun ServerSelectionView(
    state: ServerSelectionState,
    onBackClick: () -> Unit,
    onLearnMoreClick: () -> Unit,
    onSuccess: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val eventSink = state.eventSink
    val isLoading by androidx.compose.runtime.remember(state.changeServerState.changeServerAction) {
        derivedStateOf { state.changeServerState.changeServerAction is AsyncData.Loading }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    io.element.android.libraries.designsystem.components.button.BackButton(onClick = onBackClick)
                }
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
                iconStyle = BigIcon.Style.Default(CompoundIcons.HomeSolid()),
                title = stringResource(id = R.string.screen_change_server_title),
                subTitle = stringResource(id = R.string.screen_change_server_subtitle),
            )
            Spacer(Modifier.height(40.dp))

            var serverInputState by textFieldState(stateValue = state.serverInput)
            TextField(
                label = stringResource(id = R.string.screen_change_server_form_header),
                value = serverInputState,
                enabled = !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .onTabOrEnterKeyFocusNext(focusManager)
                    .testTag(TestTags.changeServerServer),
                onValueChange = {
                    val sanitized = it.replace("\n", "")
                    serverInputState = sanitized
                    eventSink(ServerSelectionEvents.SetServer(sanitized))
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Uri,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(onDone = {
                    focusManager.moveFocus(FocusDirection.Down)
                    eventSink(ServerSelectionEvents.Submit)
                }),
                singleLine = true,
                trailingIcon = if (serverInputState.isNotEmpty()) {
                    {
                        Box(Modifier.clickable {
                            serverInputState = ""
                            eventSink(ServerSelectionEvents.SetServer(""))
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
                supportingText = stringResource(id = R.string.screen_change_server_form_notice),
            )

            Spacer(Modifier.height(24.dp))
            Spacer(modifier = Modifier.weight(1f))
            ButtonColumnMolecule {
                Button(
                    text = stringResource(CommonStrings.action_continue),
                    showProgress = isLoading,
                    onClick = { eventSink(ServerSelectionEvents.Submit) },
                    enabled = state.submitEnabled,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(TestTags.loginContinue)
                )
                Spacer(modifier = Modifier.height(48.dp))
            }
        }
        ChangeServerView(
            state = state.changeServerState,
            onLearnMoreClick = onLearnMoreClick,
            onSuccess = onSuccess,
        )
    }
}

