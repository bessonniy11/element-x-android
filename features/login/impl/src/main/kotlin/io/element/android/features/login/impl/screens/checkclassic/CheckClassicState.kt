/*
 * Copyright (c) 2026 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.login.impl.screens.checkclassic

import io.element.android.libraries.architecture.AsyncData
import io.element.android.libraries.matrix.api.core.UserId

data class CheckClassicState(
    val elementClassicSession: AsyncData<UserId>,
    val eventSink: (CheckClassicEvent) -> Unit,
)
