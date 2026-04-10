/*
 * Copyright (c) 2026 Element Creations Ltd.
 * Copyright 2026 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.login.impl.screens.registrationresume

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import dev.zacsweers.metro.Inject
import io.element.android.features.login.impl.registrationdraft.RegistrationDraftStore
import io.element.android.libraries.architecture.Presenter

@Inject
class RegistrationResumePresenter(
    private val registrationDraftStore: RegistrationDraftStore,
) : Presenter<RegistrationResumeState> {
    @Composable
    override fun present(): RegistrationResumeState {
        val draft by registrationDraftStore.draftFlow().collectAsState(initial = null)
        return RegistrationResumeState(
            draft = draft,
        )
    }
}
