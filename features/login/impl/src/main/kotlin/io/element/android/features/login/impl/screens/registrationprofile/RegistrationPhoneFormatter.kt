/*
 * Copyright (c) 2026 Element Creations Ltd.
 * Copyright 2026 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.login.impl.screens.registrationprofile

private const val RU_PHONE_DIGITS_LENGTH = 10

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

private fun extractRuPhoneDigits(input: String): String {
    var digits = input.filter(Char::isDigit)

    if (digits.startsWith("8")) {
        digits = "7${digits.drop(1)}"
    }
    if (digits.startsWith("7")) {
        digits = digits.drop(1)
    }

    return digits.take(RU_PHONE_DIGITS_LENGTH)
}
