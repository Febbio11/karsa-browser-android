package com.karsa.browser.browser

import java.util.UUID

// ---------- Mesin pencari (porting prototipe commit 36cd752) ----------

enum class SearchEngineId(val id: String) {
    GOOGLE("google"),
    BRAVE("brave"),
    DUCKDUCKGO("duckduckgo"),
    BING("bing"),
    YANDEX("yandex"),
    CUSTOM("custom");

    companion object {
        fun fromId(id: String): SearchEngineId =
            entries.firstOrNull { it.id == id } ?: GOOGLE
    }
}

data class SearchEngineConfig(
    val id: SearchEngineId,
    val name: String,
    val shortLabel: String,
    val urlTemplate: String,
    val description: String,
    val domain: String
)

val DEFAULT_SEARCH_ENGINES: List<SearchEngineConfig> = listOf(
    SearchEngineConfig(SearchEngineId.GOOGLE, "Google", "G", "https://www.google.com/search?q=%s", "Pencarian cepat dan hasil paling luas (Default)", "google.com"),
    SearchEngineConfig(SearchEngineId.BRAVE, "Brave Search", "Br", "https://search.brave.com/search?q=%s", "Indeks independen, privat, tanpa pelacakan iklan", "search.brave.com"),
    SearchEngineConfig(SearchEngineId.DUCKDUCKGO, "DuckDuckGo", "DDG", "https://duckduckgo.com/?q=%s", "Fokus privasi tanpa riwayat pencarian terpersonalisasi", "duckduckgo.com"),
    SearchEngineConfig(SearchEngineId.BING, "Bing", "B", "https://www.bing.com/search?q=%s", "Pencarian visual dan jawaban terstruktur Microsoft", "bing.com"),
    SearchEngineConfig(SearchEngineId.YANDEX, "Yandex", "Ya", "https://yandex.com/search/?text=%s", "Kuat untuk pencarian lintas wilayah & gambar", "yandex.com"),
    SearchEngineConfig(SearchEngineId.CUSTOM, "Kustom (Custom URL)", "URL", "https://example.com/?q=%s", "Gunakan template URL mesin pencari Anda sendiri (%s)", "custom")
)

/** Engine aktif dari settings (Kustom memakai nama/template yang disimpan pengguna). */
fun activeSearchEngine(settings: RenderSettings): SearchEngineConfig {
    val id = SearchEngineId.fromId(settings.searchEngineId)
    val base = DEFAULT_SEARCH_ENGINES.first { it.id == id }
    if (id != SearchEngineId.CUSTOM) return base
    val name = settings.customSearchName.trim().ifEmpty { base.name }
    val template = settings.customSearchTemplate.trim().ifEmpty { base.urlTemplate }
    val domain = runCatching { java.net.URL(template).host.removePrefix("www.") }.getOrElse { "custom" }
    return base.copy(name = name, urlTemplate = template, domain = domain)
}

/** Aturan bangun URL persis prototipe: %s diganti semua; tanpa %s pakai ?q= / &. */
fun buildSearchUrl(engine: SearchEngineConfig, query: String): String {
    val encoded = java.net.URLEncoder.encode(query.trim(), "UTF-8")
    val tpl = engine.urlTemplate.trim().ifEmpty { "https://www.google.com/search?q=%s" }
    if (tpl.contains("%s")) return tpl.replace("%s", encoded)
    val separator = if (tpl.contains("?")) {
        if (tpl.endsWith("?") || tpl.endsWith("&")) "" else "&"
    } else "?q="
    return "$tpl$separator$encoded"
}

// ---------- Tab & halaman Home ----------

data class BrowserTab(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "Beranda",
    val url: String = HOME_URL,
    val incognito: Boolean = false,
    val desktopMode: Boolean = false
) {
    companion object {
        /** Halaman awal v2: Home orisinal Karsa (bukan lagi arena game). */
        const val HOME_URL = "karsa://home"
    }
    val host: String get() = if (url == HOME_URL) "Karsa Home" else runCatching {
        java.net.URL(url).host ?: url
    }.getOrElse { url }
    val isSecure: Boolean get() = url.startsWith("https://") || url.startsWith("karsa://")
    val isHome: Boolean get() = url == HOME_URL
    val faviconLabel: String get() = if (isHome) "K" else host.firstOrNull()?.uppercase() ?: "W"
}

data class HomeShortcut(
    val id: String = UUID.randomUUID().toString(),
    val label: String,
    val url: String,
    val badge: String
)

val DEFAULT_SHORTCUTS: List<HomeShortcut> = listOf(
    HomeShortcut(id = "sc-google", label = "Google", url = "https://www.google.com", badge = "G"),
    HomeShortcut(id = "sc-x", label = "X", url = "https://x.com", badge = "X"),
    HomeShortcut(id = "sc-youtube", label = "YouTube", url = "https://m.youtube.com", badge = "YT")
)

/** Badge pintasan: 2 huruf alfanumerik pertama label, huruf besar; kosong -> "WB". */
fun shortcutBadgeFor(label: String): String {
    val letters = label.filter { it.isLetterOrDigit() }.take(2).uppercase()
    return letters.ifEmpty { "WB" }
}

fun shortcutLabelFor(url: String): String = runCatching {
    val host = java.net.URL(url).host.removePrefix("www.").split(".").firstOrNull().orEmpty()
    host.replaceFirstChar { it.uppercase() }.ifEmpty { "Pintasan" }
}.getOrElse { "Pintasan" }

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

/** Profil awal v2: hanya 1 — "Profil Utama" (P1), tanpa wallet/seed. */
data class BrowserProfile(
    val id: String = "prof-main",
    val name: String = "Profil Utama",
    val badge: String = "P1",
    val walletLabel: String? = null,
    val proxySummary: String = "DIRECT"
)

data class BookmarkItem(val id: String = UUID.randomUUID().toString(), val title: String, val url: String, val folder: String = "Tersimpan")
data class HistoryItem(val id: String = UUID.randomUUID().toString(), val title: String, val url: String, val visitedAt: String)
data class DownloadItem(val id: String = UUID.randomUUID().toString(), val filename: String, val size: String, val status: String = "Selesai")

enum class OrientationLock { AUTO, PORTRAIT, LANDSCAPE }

data class RenderSettings(
    val zoomPercent: Int = 100,          // 50..200
    val forceZoom: Boolean = true,
    val forceDarkMode: Boolean = false,
    val adBlockEnabled: Boolean = true,
    val antiRedirectEnabled: Boolean = true,
    val walletWhitelistEnabled: Boolean = true,
    val disablePullToRefreshPerSite: Boolean = true,
    val translateEnabled: Boolean = false,
    val translateTarget: String = "id",  // default Auto-Deteksi -> Bahasa Indonesia
    val searchEngineId: String = "google",
    val customSearchName: String = "",
    val customSearchTemplate: String = "",
    val searchSuggestionsEnabled: Boolean = true
)

/**
 * Aturan navigasi kolom search v2:
 * - "karsa://home" -> Home.
 * - URL lengkap / domain (ada "." tanpa spasi) -> buka langsung sebagai URL.
 * - Kata kunci biasa -> cari via mesin pencari aktif (default Google).
 */
fun resolveNavigationInput(raw: String, engine: SearchEngineConfig): String {
    val t = raw.trim()
    if (t.isEmpty()) return ""
    if (t.startsWith("http://") || t.startsWith("https://") || t.startsWith("karsa://")) return t
    if (t.contains(".") && !t.contains(" ")) return "https://$t"
    return buildSearchUrl(engine, t)
}

fun isUrlLike(raw: String): Boolean {
    val t = raw.trim()
    return t.startsWith("http://") || t.startsWith("https://") || t.startsWith("karsa://") ||
        (t.contains(".") && !t.contains(" "))
}

fun titleFromUrl(url: String): String {
    if (url == BrowserTab.HOME_URL) return "Beranda"
    return runCatching {
        java.net.URL(url).host.removePrefix("www.")
    }.getOrElse { url }
}

// ---------- Saran Pencarian (Autocomplete) — 100% lokal, tanpa API ----------

data class KnownDomain(val domain: String, val label: String, val keywords: List<String>)

val KNOWN_DOMAINS: List<KnownDomain> = listOf(
    KnownDomain("google.com", "Google", listOf("google", "goo", "search")),
    KnownDomain("x.com", "X (Twitter)", listOf("x.com", "twitter", "twi", "x")),
    KnownDomain("youtube.com", "YouTube", listOf("youtube", "you", "yt", "video")),
    KnownDomain("github.com", "GitHub", listOf("github", "git", "repo", "code")),
    KnownDomain("rabby.io", "Rabby Wallet", listOf("rabby", "wallet", "web3")),
    KnownDomain("coingecko.com", "CoinGecko", listOf("coin", "coingecko", "crypto", "harga")),
    KnownDomain("defillama.com", "DefiLlama", listOf("defi", "llama", "tvl", "crypto")),
    KnownDomain("play.aetherfrontier.gg", "Aether Frontier — Web3 Arena", listOf("aether", "game", "play", "web3", "arena")),
    KnownDomain("discord.com", "Discord", listOf("discord", "disc", "chat")),
    KnownDomain("wikipedia.org", "Wikipedia", listOf("wiki", "wikipedia"))
)

val TOPIC_SUGGESTIONS: List<Pair<List<String>, List<String>>> = listOf(
    listOf("web3", "wallet", "rabby", "metamask", "crypto", "eth", "btc", "sol", "defi") to listOf(
        "walletconnect mobile android", "rabby wallet extension chrome", "harga ethereum hari ini",
        "airdrop web3 terbaru 2026", "gas fee arbitrum tracker"
    ),
    listOf("game", "aether", "fps", "webgl", "play", "steam") to listOf(
        "game web3 browser ringan android", "webgl 60fps optimization mobile",
        "aether frontier guide pemula", "setting dpi browser gaming android"
    ),
    listOf("karsa", "browser", "android", "chrome", "brave", "duck") to listOf(
        "karsa browser android apk", "cara pasang ekstensi chrome di android",
        "mode desktop browser android tanpa reload", "perbandingan brave search vs duckduckgo"
    ),
    listOf("berita", "cuaca", "kurs", "translate", "terjemah", "belajar", "cara") to listOf(
        "berita teknologi hari ini", "kurs dollar rupiah terbaru", "cuaca hari ini",
        "cara membersihkan cache situs tertentu"
    )
)

data class SearchSuggestion(
    val text: String,       // yang diisi ke kolom / dinavigasi
    val label: String,      // subtitle baris
    val isDomain: Boolean
)

/**
 * Urutan prototipe: (1) domain cocok maks 2; (2) saran topik sampai total 5;
 * (3) sufiks dinamis sampai total 5. Bookmark/riwayat ditangani pemanggil
 * (ditampilkan di seksi terpisah, bukan di daftar ini).
 */
fun buildSmartSuggestions(query: String): List<SearchSuggestion> {
    val q = query.trim().lowercase()
    if (q.isEmpty()) return emptyList()
    val out = mutableListOf<SearchSuggestion>()
    for (d in KNOWN_DOMAINS) {
        if (out.size >= 2) break
        if (d.keywords.any { q.contains(it) } || d.domain.contains(q)) {
            out += SearchSuggestion(d.domain, "${d.label} · Kunjungi situs", true)
        }
    }
    for ((keys, items) in TOPIC_SUGGESTIONS) {
        if (out.size >= 5) break
        if (keys.any { q.contains(it) }) {
            for (s in items) {
                if (out.size >= 5) break
                if (out.none { it.text == s }) out += SearchSuggestion(s, "Saran pencarian", false)
            }
        }
    }
    if (out.size < 5) {
        val suffixes = if (q.contains(".") && !q.contains(" ")) {
            listOf("$q/login", "$q alternatif", "$q review")
        } else {
            listOf("$q terbaru", "$q tutorial lengkap", "$q resmi", "$q download")
        }
        for (s in suffixes) {
            if (out.size >= 5) break
            if (out.none { it.text == s }) out += SearchSuggestion(s, "Saran pencarian", false)
        }
    }
    return out
}
