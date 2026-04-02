/*
 * Copyright (c) 2026 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.login.impl.screens.classic.missingkeybackup

import io.element.android.tests.testutils.lambda.lambdaError

class FakeMissingKeyBackupNavigator(
    private val startOverResult: () -> Unit = { lambdaError() },
) : MissingKeyBackupNavigator {
    override fun startOver() = startOverResult()
}
