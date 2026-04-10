/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2023-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.appconfig

object AuthenticationConfig {
    const val MATRIX_ORG_URL = "https://matrix.org"
    const val DEFAULT_HOMESERVER_URL = "https://matrix.ibessonniy.ru"
    const val CUSTOM_AUTH_GATEWAY_URL = "https://auth.matrix.ibessonniy.ru"
    const val CUSTOM_AUTH_MODE = "custom_auth_v1"
    const val CUSTOM_AUTH_CLIENT_APP = "element-x-android-bessonniy"
    const val CUSTOM_AUTH_PRIVACY_POLICY_URL = "https://matrix.ibessonniy.ru/privacy"
    const val CUSTOM_AUTH_TERMS_URL = "https://matrix.ibessonniy.ru/terms"
    val CUSTOM_AUTH_MANAGED_HOMESERVERS: Set<String> = setOf("matrix.ibessonniy.ru")

    /**
     * URL with some docs that explain what's sliding sync and how to add it to your home server.
     */
    const val SLIDING_SYNC_READ_MORE_URL = "https://github.com/matrix-org/sliding-sync/blob/main/docs/Landing.md"

    /**
     * Force a sliding sync proxy url, if not null, the proxy url in the .well-known file will be ignored.
     */
    val SLIDING_SYNC_PROXY_URL: String? = null
}
