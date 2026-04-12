/*
 * Copyright (c) 2026 Element Creations Ltd.
 * Copyright 2026 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.login.impl.screens.authchoice

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.element.android.compound.tokens.generated.CompoundIcons
import io.element.android.features.login.impl.customauth.defaultCustomAuthLegalLinks
import io.element.android.features.login.impl.R
import io.element.android.features.login.impl.screens.common.LegalLinksSection
import io.element.android.libraries.designsystem.atomic.molecules.ButtonColumnMolecule
import io.element.android.libraries.designsystem.atomic.molecules.IconTitleSubtitleMolecule
import io.element.android.libraries.designsystem.components.BigIcon
import io.element.android.libraries.designsystem.components.button.BackButton
import io.element.android.libraries.designsystem.theme.components.Button
import io.element.android.libraries.designsystem.theme.components.Scaffold
import io.element.android.libraries.designsystem.theme.components.TopAppBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthChoiceView(
    homeserverTitle: String,
    canRegister: Boolean,
    privacyPolicyUrl: String = defaultCustomAuthLegalLinks().privacyPolicyUrl,
    termsUrl: String = defaultCustomAuthLegalLinks().termsUrl,
    onBackClick: () -> Unit,
    onLoginClick: () -> Unit,
    onRegisterClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = { BackButton(onClick = onBackClick) },
            )
        }
    ) { padding ->
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
                    title = stringResource(
                        id = R.string.screen_account_provider_signin_title,
                        homeserverTitle,
                    ),
                    subTitle = stringResource(id = R.string.screen_auth_choice_subtitle),
                )
            }

            Box(
                modifier = Modifier.padding(start = 36.dp, end = 36.dp, bottom = 20.dp)
            ) {
                ButtonColumnMolecule {
                    Button(
                        text = stringResource(id = R.string.screen_auth_choice_login),
                        onClick = onLoginClick,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    if (canRegister) {
                        Button(
                            text = stringResource(id = R.string.screen_auth_choice_register),
                            onClick = onRegisterClick,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    LegalLinksSection(
                        privacyPolicyUrl = privacyPolicyUrl,
                        termsUrl = termsUrl,
                    )
                }
            }
        }
    }
}
