/*
 * Copyright (c) 2026 Element Creations Ltd.
 * Copyright 2026 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.login.impl.screens.registrationresume

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.element.android.compound.tokens.generated.CompoundIcons
import io.element.android.features.login.impl.R
import io.element.android.features.login.impl.registrationdraft.RegistrationDraft
import io.element.android.features.login.impl.registrationdraft.RegistrationDraftStep
import io.element.android.libraries.designsystem.atomic.molecules.ButtonColumnMolecule
import io.element.android.libraries.designsystem.atomic.molecules.IconTitleSubtitleMolecule
import io.element.android.libraries.designsystem.components.BigIcon
import io.element.android.libraries.designsystem.theme.components.Button
import io.element.android.libraries.designsystem.theme.components.Scaffold
import io.element.android.libraries.designsystem.theme.components.Text
import io.element.android.libraries.designsystem.theme.components.TopAppBar
import io.element.android.libraries.ui.strings.CommonStrings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegistrationResumeView(
    draft: RegistrationDraft,
    onContinueClick: () -> Unit,
    onStartOverClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(title = {})
        }
    ) { padding ->
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
                iconStyle = BigIcon.Style.Default(CompoundIcons.History()),
                title = stringResource(id = R.string.screen_registration_resume_title),
                subTitle = stringResource(id = R.string.screen_registration_resume_subtitle),
            )
            Spacer(Modifier.height(24.dp))
            Text(
                text = stringResource(id = R.string.screen_registration_resume_details_login, draft.login),
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(id = R.string.screen_registration_resume_details_email, draft.email),
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(
                    id = if (draft.step == RegistrationDraftStep.Profile) {
                        R.string.screen_registration_resume_details_step_profile
                    } else {
                        R.string.screen_registration_resume_details_step_verify
                    }
                ),
            )
            Spacer(Modifier.height(24.dp))
            Spacer(modifier = Modifier.weight(1f))
            ButtonColumnMolecule {
                Button(
                    text = stringResource(id = R.string.screen_registration_resume_continue),
                    onClick = onContinueClick,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    text = stringResource(CommonStrings.action_start_over),
                    onClick = onStartOverClick,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(48.dp))
            }
        }
    }
}
