/*
 * Copyright (c) 2026 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.login.impl.screens.checkclassic

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.element.android.libraries.designsystem.preview.ElementPreview
import io.element.android.libraries.designsystem.preview.PreviewsDayNight

@Composable
fun CheckClassicView(
    modifier: Modifier = Modifier,
) = Box(
    modifier = modifier.fillMaxSize(),
) {
    // Nothing to render
}

@PreviewsDayNight
@Composable
internal fun CheckClassicViewPreview() = ElementPreview {
    CheckClassicView()
}
