/*
 * Copyright (c) 2026 Element Creations Ltd.
 * Copyright 2026 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.login.impl.screens.registrationprofile

private const val RU_PHONE_DIGITS_LENGTH = 10
const val RU_PHONE_PREFIX = "+7 ("

fun formatRuPhoneInput(rawInput: String): String {
    val digits = extractRuPhoneDigits(rawInput)
    if (digits.isEmpty()) return ""

    val part1 = digits.take(3)
    val part2 = digits.drop(3).take(3)
    val part3 = digits.drop(6).take(2)
    val part4 = digits.drop(8).take(2)

    return buildString {
        append("+7")
        append(" (")
        append(part1)
        if (part1.length == 3) {
            append(")")
        }
        if (part2.isNotEmpty()) {
            append(" ")
            append(part2)
        }
        if (part3.isNotEmpty()) {
            append(" ")
            append(part3)
        }
        if (part4.isNotEmpty()) {
            append(" ")
            append(part4)
        }
    }
}

fun normalizeRuPhoneForBackend(input: String): String? {
    val digits = extractRuPhoneDigits(input)
    if (digits.length != RU_PHONE_DIGITS_LENGTH) return null
    return "+7$digits"
}

fun extractRuPhoneDigits(input: String): String {
    var digits = input.filter(Char::isDigit)
    val hasRuPrefixMarkers = input.contains("+7") || input.contains("7 (")

    // If user types before the fixed +7 prefix, preserve typed digit and remove the prefix digit.
    if (digits.length >= 2 && digits[1] == '7' && digits[0] != '7') {
        digits = buildString(digits.length - 1) {
            append(digits[0])
            append(digits.drop(2))
        }
    }

    if (digits.startsWith("8")) {
        digits = "7${digits.drop(1)}"
    }
    if (digits.startsWith("7") && (digits.length > 1 || hasRuPrefixMarkers)) {
        digits = digits.drop(1)
    }

    return digits.take(RU_PHONE_DIGITS_LENGTH)
}

fun indexAfterRuDigits(masked: String, digitsCount: Int): Int {
    if (digitsCount <= 0) return if (masked.startsWith(RU_PHONE_PREFIX)) RU_PHONE_PREFIX.length else 0

    var seenDigits = 0
    var skippedPrefixDigit = false
    for (index in masked.indices) {
        val char = masked[index]
        if (!char.isDigit()) continue

        if (!skippedPrefixDigit && masked.startsWith("+7") && char == '7') {
            skippedPrefixDigit = true
            continue
        }

        seenDigits++
        if (seenDigits >= digitsCount) {
            return index + 1
        }
    }
    return masked.length
}

fun countRuDigitsBeforeMaskCursor(masked: String, cursor: Int): Int {
    val safeCursor = cursor.coerceIn(0, masked.length)
    var digits = 0
    var skippedPrefixDigit = false

    for (index in 0 until safeCursor) {
        val char = masked[index]
        if (!char.isDigit()) continue

        if (!skippedPrefixDigit && masked.startsWith("+7") && char == '7') {
            skippedPrefixDigit = true
            continue
        }
        digits++
    }

    return digits.coerceIn(0, RU_PHONE_DIGITS_LENGTH)
}
