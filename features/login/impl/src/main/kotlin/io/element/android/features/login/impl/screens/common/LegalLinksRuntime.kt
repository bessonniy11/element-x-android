/*
 * Copyright (c) 2026 Element Creations Ltd.
 * Copyright 2026 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.login.impl.screens.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import io.element.android.features.login.impl.customauth.CustomAuthLegalLinks
import io.element.android.features.login.impl.customauth.CustomAuthService
import io.element.android.features.login.impl.customauth.defaultCustomAuthLegalLinks

@Composable
fun rememberRuntimeLegalLinks(
    homeserverUrl: String,
    customAuthService: CustomAuthService,
): CustomAuthLegalLinks {
    var links by remember(homeserverUrl) { mutableStateOf(defaultCustomAuthLegalLinks()) }

    LaunchedEffect(homeserverUrl) {
        links = customAuthService.getLegalLinks(homeserverUrl)
    }

    return links
}

