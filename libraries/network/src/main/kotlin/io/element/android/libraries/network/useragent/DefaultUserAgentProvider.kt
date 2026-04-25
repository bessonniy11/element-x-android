/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2023-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.network.useragent

import android.os.Build
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.SingleIn
import io.element.android.libraries.core.meta.BuildMeta
import io.element.android.libraries.matrix.api.SdkMetadata

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class DefaultUserAgentProvider(
    private val buildMeta: BuildMeta,
    private val sdkMeta: SdkMetadata,
) : UserAgentProvider {
    companion object {
        private val WhitespaceRegex = "\\s+".toRegex()
    }

    private val userAgent: String by lazy { buildUserAgent() }

    override fun provide(): String = userAgent

    /**
     * Create an user agent with the application version.
     * Ex: Element X/1.5.0 (Xiaomi Mi 9T; Android 11; RKQ1.200826.002; Sdk c344b155c)
     */
    private fun buildUserAgent(): String {
        val appName = sanitizeForHeader(buildMeta.applicationName, fallback = buildMeta.applicationId)
        val appVersion = sanitizeForHeader(buildMeta.versionName, fallback = "0")
        val deviceManufacturer = sanitizeForHeader(Build.MANUFACTURER, fallback = "unknown")
        val deviceModel = sanitizeForHeader(Build.MODEL, fallback = "unknown")
        val androidVersion = sanitizeForHeader(Build.VERSION.RELEASE, fallback = "unknown")
        val deviceBuildId = sanitizeForHeader(Build.DISPLAY, fallback = "unknown")
        val matrixSdkVersion = sanitizeForHeader(sdkMeta.sdkGitSha, fallback = "unknown")

        return buildString {
            append(appName)
            append("/")
            append(appVersion)
            append(" (")
            append(deviceManufacturer)
            append(" ")
            append(deviceModel)
            append("; ")
            append("Android ")
            append(androidVersion)
            append("; ")
            append(deviceBuildId)
            append("; ")
            append("Sdk ")
            append(matrixSdkVersion)
            append(")")
        }
    }

    private fun sanitizeForHeader(value: String, fallback: String): String {
        val sanitized = value
            .map { char ->
                when (char.code) {
                    in 0x20..0x7E -> char
                    else -> '-'
                }
            }
            .joinToString("")
            .trim()
            .replace(WhitespaceRegex, " ")
        return sanitized.ifEmpty { fallback }
    }
}
