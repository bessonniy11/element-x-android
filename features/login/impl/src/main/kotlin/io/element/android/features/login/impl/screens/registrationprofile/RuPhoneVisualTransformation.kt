/*
 * Copyright (c) 2026 Element Creations Ltd.
 * Copyright 2026 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.login.impl.screens.registrationprofile

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

object RuPhoneVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val digits = extractRuPhoneDigits(text.text)
        val masked = if (digits.isEmpty()) {
            RU_PHONE_PREFIX
        } else {
            formatRuPhoneInput(digits)
        }

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                val safeOffset = offset.coerceIn(0, digits.length)
                return indexAfterRuDigits(masked = masked, digitsCount = safeOffset)
            }

            override fun transformedToOriginal(offset: Int): Int {
                return countRuDigitsBeforeMaskCursor(masked = masked, cursor = offset)
            }
        }

        return TransformedText(
            text = AnnotatedString(masked),
            offsetMapping = offsetMapping,
        )
    }
}

