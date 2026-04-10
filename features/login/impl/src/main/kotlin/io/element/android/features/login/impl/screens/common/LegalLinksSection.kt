/*
 * Copyright (c) 2026 Element Creations Ltd.
 * Copyright 2026 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.login.impl.screens.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import io.element.android.appconfig.AuthenticationConfig
import io.element.android.features.login.impl.R
import io.element.android.libraries.designsystem.theme.components.TextButton

@Composable
fun LegalLinksSection(
    modifier: Modifier = Modifier,
) {
    val uriHandler = LocalUriHandler.current

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        TextButton(
            text = stringResource(id = R.string.screen_legal_privacy_policy),
            onClick = {
                runCatching {
                    uriHandler.openUri(AuthenticationConfig.CUSTOM_AUTH_PRIVACY_POLICY_URL)
                }
            },
        )
        TextButton(
            text = stringResource(id = R.string.screen_legal_terms),
            onClick = {
                runCatching {
                    uriHandler.openUri(AuthenticationConfig.CUSTOM_AUTH_TERMS_URL)
                }
            },
        )
    }
}
