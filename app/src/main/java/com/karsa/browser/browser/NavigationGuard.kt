package com.karsa.browser.browser

import android.net.Uri
import java.net.URLDecoder

/**
 * Port Kotlin dari prototipe: InternalNavigationDelegate.kt
 * (architectureData.ts -> STARTER_CODE_FILES[id=nav-guard])
 *
 * Aturan v1 (spesifikasi Febbio):
 * - Blokir TOTAL redirect ke aplikasi media sosial eksternal (X, Discord,
 *   Telegram) -> tetap di Pop-up Fleksibel internal.
 * - WHITELIST khusus: WalletConnect (wc:) & Rabby Wallet Mobile (rabby:)
 *   -> lewat pop-up konfirmasi, baru diteruskan ke aplikasi resmi.
 *   Setelah tanda tangan, kembali ke game tanpa reload (sesi WC v2 hidup
 *   di tab, WebView tidak di-reload oleh guard ini).
 */
data class WalletWhitelistPolicy(
    val enabled: Boolean = true,
    val allowedSchemes: Set<String> = setOf("rabby", "wc"),
    val allowedPackages: Map<String, String> = mapOf(
        "rabby" to "com.debank.rabbymobile"
    )
)

sealed class NavigationDecision {
    data object AllowInCurrentTab : NavigationDecision()
    data class OpenInFlexiblePopup(val resolvedUrl: String, val blockedScheme: String?, val reason: String) : NavigationDecision()
    data class AskExternalWallet(val deepLinkUri: String, val targetPackage: String?, val scheme: String) : NavigationDecision()
    data class BlockSilently(val rawUrl: String) : NavigationDecision()
}

class NavigationGuard(
    var walletPolicy: WalletWhitelistPolicy = WalletWhitelistPolicy()
) {
    // Media sosial & store: 100% terkunci, tidak pernah keluar dari Karsa.
    private val lockedSocialSchemes = setOf("intent", "twitter", "x", "discord", "tg", "telegram", "market")
    private val walletSchemes = setOf("rabby", "wc")
    private val oauthPopupHosts = setOf(
        "x.com", "twitter.com", "api.twitter.com",
        "discord.com", "oauth.telegram.org", "accounts.google.com"
    )

    fun intercept(rawUrl: String, isPopupRequest: Boolean = false): NavigationDecision {
        val uri = runCatching { Uri.parse(rawUrl) }.getOrNull()
            ?: return NavigationDecision.BlockSilently(rawUrl)
        val scheme = uri.scheme?.lowercase().orEmpty()

        // 1. Whitelist wallet: hanya wc: & rabby:, hanya jika policy aktif.
        if (scheme in walletSchemes) {
            return if (walletPolicy.enabled && scheme in walletPolicy.allowedSchemes) {
                NavigationDecision.AskExternalWallet(
                    deepLinkUri = rawUrl,
                    targetPackage = walletPolicy.allowedPackages[scheme],
                    scheme = "$scheme://"
                )
            } else {
                NavigationDecision.OpenInFlexiblePopup(
                    resolvedUrl = rawUrl,
                    blockedScheme = "$scheme://",
                    reason = "Wallet eksternal tidak di Whitelist — ditahan di Pop-up Fleksibel"
                )
            }
        }

        // 2. Blokir 100% intent:// & skema aplikasi media sosial.
        if (scheme in lockedSocialSchemes) {
            val fallback = extractFallbackWebUrl(rawUrl, uri, scheme)
            return if (fallback != null) {
                NavigationDecision.OpenInFlexiblePopup(
                    resolvedUrl = fallback,
                    blockedScheme = "$scheme://",
                    reason = "Anti-Redirect 100%: aplikasi media sosial diblokir — dialihkan ke Pop-up Fleksibel"
                )
            } else {
                NavigationDecision.BlockSilently(rawUrl)
            }
        }

        // 3. OAuth sosmed tetap di Pop-up Fleksibel internal.
        val host = uri.host?.lowercase().orEmpty()
        val isAuthPath = uri.path?.contains("oauth", ignoreCase = true) == true ||
            uri.path?.contains("authorize", ignoreCase = true) == true
        if (isPopupRequest || (host in oauthPopupHosts && isAuthPath)) {
            return NavigationDecision.OpenInFlexiblePopup(
                resolvedUrl = rawUrl,
                blockedScheme = null,
                reason = "Otorisasi ditangani di Pop-up Fleksibel internal"
            )
        }
        return NavigationDecision.AllowInCurrentTab
    }

    private fun extractFallbackWebUrl(rawUrl: String, uri: Uri, scheme: String): String? {
        if (scheme == "intent") {
            val encoded = Regex("S\\.browser_fallback_url=([^;]+)").find(rawUrl)?.groupValues?.getOrNull(1)
            if (encoded != null) return runCatching { URLDecoder.decode(encoded, "UTF-8") }.getOrNull()
            val host = uri.host ?: return null
            val path = uri.encodedPath.orEmpty()
            val query = uri.encodedQuery?.let { "?$it" }.orEmpty()
            return "https://$host$path$query"
        }
        return when (scheme) {
            "twitter", "x" -> "https://x.com/i/flow/login"
            "discord" -> "https://discord.com/login"
            "tg", "telegram" -> "https://web.telegram.org/"
            else -> null
        }
    }
}
