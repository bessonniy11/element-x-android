/*
 * Copyright (c) 2026 Element Creations Ltd.
 * Copyright 2026 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.login.impl.screens.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.element.android.compound.theme.ElementTheme
import io.element.android.features.login.impl.customauth.defaultCustomAuthLegalLinks
import io.element.android.features.login.impl.R
import io.element.android.libraries.designsystem.theme.components.Text

@Composable
fun LegalLinksSection(
    privacyPolicyUrl: String = defaultCustomAuthLegalLinks().privacyPolicyUrl,
    termsUrl: String = defaultCustomAuthLegalLinks().termsUrl,
    modifier: Modifier = Modifier,
) {
    val uriHandler = LocalUriHandler.current

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        LegalLink(
            modifier = Modifier.weight(1f),
            text = stringResource(id = R.string.screen_legal_privacy_policy),
            onClick = {
                runCatching {
                    uriHandler.openUri(privacyPolicyUrl)
                }
            },
        )
        LegalLink(
            modifier = Modifier.weight(1f),
            text = stringResource(id = R.string.screen_legal_terms),
            onClick = {
                runCatching {
                    uriHandler.openUri(termsUrl)
                }
            },
        )
    }
}

@Composable
private fun LegalLink(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier,
    ) {
        Text(
            text = text,
            color = ElementTheme.colors.textSecondary,
            style = MaterialTheme.typography.labelMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() }
                .padding(vertical = 4.dp),
        )
    }
}
