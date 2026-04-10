/*
 * Copyright (c) 2026 Element Creations Ltd.
 * Copyright 2026 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.login.impl.screens.registrationprofile

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class RegistrationPhoneFormatterTest {
    @Test
    fun `format - supports local ten digits`() {
        assertThat(formatRuPhoneInput("9636294882")).isEqualTo("+7 (963) 629 48 82")
    }

    @Test
    fun `format - supports 7-prefixed number`() {
        assertThat(formatRuPhoneInput("79636294882")).isEqualTo("+7 (963) 629 48 82")
    }

    @Test
    fun `format - supports 8-prefixed number`() {
        assertThat(formatRuPhoneInput("89636294882")).isEqualTo("+7 (963) 629 48 82")
    }

    @Test
    fun `format - handles pasted formatted value`() {
        assertThat(formatRuPhoneInput("+7 (963) 629 48 82")).isEqualTo("+7 (963) 629 48 82")
    }

    @Test
    fun `format - keeps user digit when typed before prefix`() {
        assertThat(formatRuPhoneInput("6+7 (9")).isEqualTo("+7 (69")
    }

    @Test
    fun `normalize - returns backend format`() {
        assertThat(normalizeRuPhoneForBackend("+7 (963) 629 48 82")).isEqualTo("+79636294882")
    }

    @Test
    fun `normalize - returns null for incomplete value`() {
        assertThat(normalizeRuPhoneForBackend("+7 (963) 629")).isNull()
    }

    @Test
    fun `normalize - returns null for prefix only`() {
        assertThat(normalizeRuPhoneForBackend(RU_PHONE_PREFIX)).isNull()
    }
}

