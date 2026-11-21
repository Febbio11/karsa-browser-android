package com.karsa.browser.browser

import java.util.UUID

data class BrowserTab(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "Tab Baru",
    val url: String = HOME_URL,
    val incognito: Boolean = false
) {
    companion object {
        // Halaman awal v1: sama seperti prototipe — arena desktop Web3.
        // Di WebView v1 ini hanya start page, bukan game bawaan.
        const val HOME_URL = "https://play.aetherfrontier.gg/arena?mode=desktop"
        // Mesin pencari default v1: Google Search (tambahan 22 Nov 2026).
        // Kata kunci biasa (bukan URL) di kolom search -> Google, bukan
        // mesin lain. Alias SEARCH_URL dipertahankan untuk kompatibilitas.
        const val SEARCH_ENGINE_NAME = "Google Search"
        const val GOOGLE_SEARCH_URL = "https://www.google.com/search?q="
        const val SEARCH_URL = GOOGLE_SEARCH_URL
    }
    val host: String get() = runCatching {
        java.net.URL(url).host ?: url
    }.getOrElse { url }
    val isSecure: Boolean get() = url.startsWith("https://") || url.startsWith("karsa://")
}

enum class PermissionValue { ALLOW, ASK, BLOCK }

data class SitePermissions(
    val clipboard: PermissionValue = PermissionValue.ALLOW,
    val notifications: PermissionValue = PermissionValue.ASK,
    val camera: PermissionValue = PermissionValue.BLOCK,
    val microphone: PermissionValue = PermissionValue.BLOCK,
    val location: PermissionValue = PermissionValue.BLOCK,
    val popups: PermissionValue = PermissionValue.ALLOW,
    val autoplay: PermissionValue = PermissionValue.ALLOW
)

data class BrowserProfile(
    val id: String = "prof-main",
    val name: String = "Utama · Akun Guild",
    val badge: String = "P1",
    val walletLabel: String? = "Rabby · 0x71C...94A2",
    val proxySummary: String = "DIRECT"
)

data class BookmarkItem(val id: String = UUID.randomUUID().toString(), val title: String, val url: String, val folder: String = "Umum")
data class HistoryItem(val id: String = UUID.randomUUID().toString(), val title: String, val url: String, val visitedAt: String)
data class DownloadItem(val id: String = UUID.randomUUID().toString(), val filename: String, val size: String, val status: String = "Selesai")

enum class OrientationLock { AUTO, PORTRAIT, LANDSCAPE }

data class RenderSettings(
    val zoomPercent: Int = 100,          // 50..200, per situs di v1 disimpan global + per-host di SettingsStore
    val forceZoom: Boolean = true,
    val forceDarkMode: Boolean = false,
    val adBlockEnabled: Boolean = true,
    val antiRedirectEnabled: Boolean = true,
    val walletWhitelistEnabled: Boolean = true,
    val disablePullToRefreshPerSite: Boolean = true,
    val translateEnabled: Boolean = false,
    val translateTarget: String = "id"   // default Auto-Deteksi -> Bahasa Indonesia
)

/**
 * Aturan navigasi kolom search v1:
 * - URL lengkap / domain (ada "." tanpa spasi) -> buka langsung sebagai URL.
 * - Kata kunci biasa (mis. "aether frontier arena") -> Google Search
 *   default: https://www.google.com/search?q=<kata kunci ter-encode>.
 * Dipakai BrowserScreen.navigate() untuk input Go, Tempel & Buka, dan
 * saran bookmark — jadi tidak ada jalur search lain di v1.
 */
fun resolveNavigationInput(raw: String): String {
    val t = raw.trim()
    if (t.isEmpty()) return ""
    if (t.startsWith("http://") || t.startsWith("https://") || t.startsWith("karsa://")) return t
    if (t.contains(".") && !t.contains(" ")) return "https://$t"
    return BrowserTab.GOOGLE_SEARCH_URL + java.net.URLEncoder.encode(t, "UTF-8")
}

fun titleFromUrl(url: String): String = runCatching {
    val u = java.net.URL(url)
    u.host.removePrefix("www.")
}.getOrElse { url }
