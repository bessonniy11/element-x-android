/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2023-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.preferences.impl.about

import android.app.Activity
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import com.bumble.appyx.core.modality.BuildContext
import com.bumble.appyx.core.node.Node
import com.bumble.appyx.core.plugin.Plugin
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedInject
import io.element.android.annotations.ContributesNode
import io.element.android.appconfig.AuthenticationConfig
import io.element.android.compound.theme.ElementTheme
import io.element.android.libraries.androidutils.browser.openUrlInChromeCustomTab
import io.element.android.libraries.architecture.callback
import io.element.android.libraries.core.uri.ensureProtocol
import io.element.android.libraries.di.SessionScope
import io.element.android.libraries.matrix.api.MatrixClient
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@ContributesNode(SessionScope::class)
@AssistedInject
class AboutNode(
    @Assisted buildContext: BuildContext,
    @Assisted plugins: List<Plugin>,
    private val presenter: AboutPresenter,
    private val matrixClient: MatrixClient,
) : Node(buildContext, plugins = plugins) {
    private val okHttpClient = OkHttpClient()

    interface Callback : Plugin {
        fun navigateToOssLicenses()
    }

    private val callback: Callback = callback()

    private fun onElementLegalClick(
        activity: Activity,
        darkTheme: Boolean,
        elementLegal: ElementLegal,
    ) {
        activity.openUrlInChromeCustomTab(null, darkTheme, elementLegal.url)
    }

    @Composable
    override fun View(modifier: Modifier) {
        val activity = requireNotNull(LocalActivity.current)
        val isDark = ElementTheme.isLightTheme.not()
        val state = presenter.present()
        val runtimeLegals by produceState(
            initialValue = state.elementLegals,
            key1 = matrixClient.sessionId.value,
        ) {
            value = loadRuntimeLegals(state.elementLegals)
        }
        AboutView(
            state = state.copy(elementLegals = runtimeLegals),
            onBackClick = ::navigateUp,
            onElementLegalClick = { elementLegal ->
                onElementLegalClick(activity, isDark, elementLegal)
            },
            onOpenSourceLicensesClick = callback::navigateToOssLicenses,
            modifier = modifier
        )
    }

    private suspend fun loadRuntimeLegals(
        fallback: ImmutableList<ElementLegal>,
    ): ImmutableList<ElementLegal> = withContext(Dispatchers.IO) {
        val homeserverUrl = "https://${matrixClient.userIdServerName()}".ensureProtocol()
        if (!isManagedHomeserver(homeserverUrl)) return@withContext fallback

        val request = Request.Builder()
            .url(buildPublicConfigUrl(homeserverUrl))
            .get()
            .header("X-Client-Auth-Mode", AuthenticationConfig.CUSTOM_AUTH_MODE)
            .header("X-Client-App", AuthenticationConfig.CUSTOM_AUTH_CLIENT_APP)
            .build()

        return@withContext runCatching {
            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@use fallback
                val body = response.body?.string().orEmpty()
                if (body.isBlank()) return@use fallback

                val payload = JSONObject(body)
                val privacyPolicyUrl = payload.optString("privacy_policy_url").sanitizeUrl()
                val termsUrl = payload.optString("terms_url").sanitizeUrl()

                getAllLegals(
                    termsUrl = termsUrl ?: AuthenticationConfig.CUSTOM_AUTH_TERMS_URL,
                    privacyPolicyUrl = privacyPolicyUrl ?: AuthenticationConfig.CUSTOM_AUTH_PRIVACY_POLICY_URL,
                )
            }
        }.getOrElse { fallback }
    }

    private fun buildPublicConfigUrl(homeserverUrl: String): String {
        val baseUrl = AuthenticationConfig.CUSTOM_AUTH_GATEWAY_URL.trim().trimEnd('/')
        val encodedHomeserver = URLEncoder.encode(homeserverUrl, StandardCharsets.UTF_8)
        return "$baseUrl/auth/public/config?homeserver=$encodedHomeserver"
    }

    private fun isManagedHomeserver(homeserverUrl: String): Boolean {
        val host = normalizeHost(homeserverUrl) ?: return false
        return AuthenticationConfig.CUSTOM_AUTH_MANAGED_HOMESERVERS.contains(host)
    }

    private fun normalizeHost(homeserverUrl: String): String? {
        val raw = homeserverUrl.trim()
        if (raw.isEmpty()) return null
        return runCatching {
            URI(raw.ensureProtocol()).host?.trim()?.lowercase()
        }.getOrNull()?.takeIf { it.isNotEmpty() }
    }

    private fun String?.sanitizeUrl(): String? {
        val raw = this?.trim().orEmpty()
        if (raw.isEmpty()) return null
        val withScheme = raw.ensureProtocol()
        val parsedHost = runCatching { URI(withScheme).host }.getOrNull()
        return if (parsedHost.isNullOrBlank()) null else withScheme
    }
}
