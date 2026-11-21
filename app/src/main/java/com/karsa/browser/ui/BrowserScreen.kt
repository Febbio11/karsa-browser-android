package com.karsa.browser.ui

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.net.Uri
import android.webkit.CookieManager
import android.webkit.WebView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.karsa.browser.browser.BrowserProfile
import com.karsa.browser.browser.BrowserTab
import com.karsa.browser.browser.KarsaWebViewCallbacks
import com.karsa.browser.browser.NavigationDecision
import com.karsa.browser.browser.NavigationGuard
import com.karsa.browser.browser.RenderSettings
import com.karsa.browser.browser.SitePermissions
import com.karsa.browser.browser.BookmarkItem
import com.karsa.browser.browser.DownloadItem
import com.karsa.browser.browser.HistoryItem
import com.karsa.browser.browser.OrientationLock
import com.karsa.browser.browser.createKarsaWebView
import com.karsa.browser.browser.resolveNavigationInput
import com.karsa.browser.browser.titleFromUrl
import com.karsa.browser.ui.components.AddressOverlay
import com.karsa.browser.ui.components.ExtensionsPopover
import com.karsa.browser.ui.components.FlexiblePopup
import com.karsa.browser.ui.components.MenuSheet
import com.karsa.browser.ui.components.SiteInfoPopover
import com.karsa.browser.ui.components.TabSwitcherSheet
import com.karsa.browser.ui.components.WalletConfirmDialog
import com.karsa.browser.userscript.UserScript
import com.karsa.browser.userscript.UserScriptManager
import kotlinx.coroutines.launch

/**
 * Layar utama v1 — 1 bar atas tunggal (prototipe BrowserSimulator.tsx):
 * [🔒 URL/Search] -> [🧩 Ekstensi] -> [⛶ Full Layar] -> [Tab berangka] -> [⋮ Menu]
 * Full Layar & Ekstensi HANYA di bar atas, tidak ada di menu ⋮.
 */
@Composable
fun BrowserScreen(
    settings: RenderSettings,
    darkTheme: Boolean,
    onToggleDarkTheme: () -> Unit,
    onUpdateSettings: ((RenderSettings) -> RenderSettings) -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    fun notice(msg: String) { scope.launch { snackbar.showSnackbar(msg) } }

    val guard = remember { NavigationGuard() }
    // Sinkronkan whitelist dari settings (toggle di ikon gembok).
    LaunchedEffect(settings.walletWhitelistEnabled, settings.antiRedirectEnabled) {
        guard.walletPolicy = guard.walletPolicy.copy(enabled = settings.walletWhitelistEnabled)
    }

    val scriptManager = remember { UserScriptManager(context) }
    var userScripts by remember { mutableStateOf<List<UserScript>>(emptyList()) }
    LaunchedEffect(Unit) {
        scriptManager.ensureSample()
        userScripts = scriptManager.list()
    }

    // --- State tab & navigasi (seed dari prototipe) ---
    val tabs = remember {
        mutableStateListOf(
            BrowserTab(title = "Aether Frontier — Desktop Web3 Tactics", url = BrowserTab.HOME_URL),
            BrowserTab(title = "Aether Marketplace — Arbitrum", url = "https://market.aetherfrontier.gg/inventory"),
            BrowserTab(title = "DeBank Portfolio Dashboard", url = "https://debank.com/profile/0x71c")
        )
    }
    var activeTabId by remember { mutableStateOf(tabs[0].id) }
    // NOTE: val lokal TIDAK boleh punya custom getter (dulu: `val activeTab ... get() = ...`
    // -> "Syntax error: Unexpected tokens" + cascade "must be initialized" di run #7).
    // Val biasa sudah reaktif: tabs & activeTabId dibaca saat komposisi.
    val activeTab: BrowserTab = tabs.firstOrNull { it.id == activeTabId } ?: tabs[0]
    var progress by remember { mutableStateOf(0) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var lastLoadedUrl by remember { mutableStateOf<String?>(null) }

    fun updateTab(id: String, title: String? = null, url: String? = null) {
        val i = tabs.indexOfFirst { it.id == id }
        if (i < 0) return
        tabs[i] = tabs[i].copy(title = title ?: tabs[i].title, url = url ?: tabs[i].url)
    }

    fun navigate(raw: String) {
        val url = resolveNavigationInput(raw)
        if (url.isEmpty()) return
        updateTab(activeTabId, url = url, title = titleFromUrl(url))
        if (webViewRef?.url != url) {
            webViewRef?.loadUrl(url)
            lastLoadedUrl = url
        }
    }

    // --- State overlay ---
    var urlFocusMode by remember { mutableStateOf(false) }
    var searchDraft by remember { mutableStateOf("") }
    var siteInfoOpen by remember { mutableStateOf(false) }
    var extensionsOpen by remember { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }
    var tabSwitcherOpen by remember { mutableStateOf(false) }
    var fullScreen by remember { mutableStateOf(false) }
    var orientationLock by remember { mutableStateOf(OrientationLock.AUTO) }
    var popupUrl by remember { mutableStateOf<String?>(null) }
    var popupTitle by remember { mutableStateOf("") }
    var popupReason by remember { mutableStateOf("") }
    var walletDeepLink by remember { mutableStateOf<Pair<String, String?>>(Pair("", null)) }
    var walletDialogOpen by remember { mutableStateOf(false) }
    var sitePermissions by remember { mutableStateOf(SitePermissions()) }
    var findBarOpen by remember { mutableStateOf(false) }
    var findQuery by remember { mutableStateOf("") }
    var activeProfile by remember { mutableStateOf(BrowserProfile()) }
    val profiles = remember {
        listOf(
            BrowserProfile("prof-main", "Utama · Akun Guild", "P1", "Rabby · 0x71C...94A2", "DIRECT"),
            BrowserProfile("prof-alt1", "Farming Alt #1 (Terisolasi)", "P2", "MetaMask · 0x9F2...18D4", "SOCKS5 sg1:1080"),
            BrowserProfile("prof-alt2", "Airdrop Alt #2 (Terisolasi)", "P3", "Phantom · 8xK...3pQ", "HTTPS jp2:8443")
        )
    }
    val bookmarks = remember {
        mutableStateListOf(
            BookmarkItem(title = "Aether Frontier Arena", url = "https://play.aetherfrontier.gg/arena?mode=desktop", folder = "Game Web3"),
            BookmarkItem(title = "Pixels Online (Desktop)", url = "https://play.pixels.xyz/", folder = "Game Web3"),
            BookmarkItem(title = "Chrome Web Store", url = "https://chromewebstore.google.com/", folder = "Ekstensi"),
            BookmarkItem(title = "Arbitrum Bridge", url = "https://bridge.arbitrum.io/", folder = "Wallet")
        )
    }
    val history = remember {
        mutableStateListOf(
            HistoryItem(title = "Aether Frontier — Desktop Web3 Tactics", url = "https://play.aetherfrontier.gg/arena", visitedAt = "23:41"),
            HistoryItem(title = "OAuth X (Diblokir ke Pop-up Internal)", url = "https://x.com/i/oauth2/authorize", visitedAt = "23:38")
        )
    }
    val downloads = remember {
        mutableStateListOf(
            DownloadItem(filename = "aether-auto-harvest.user.js", size = "14 KB"),
            DownloadItem(filename = "rabby-wallet-0.92.84.crx", size = "6,4 MB")
        )
    }

    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(urlFocusMode) { if (urlFocusMode) focusRequester.requestFocus() }

    // File picker userscript (.user.js / .js) — ikon 🧩 -> Pasang.
    val pickScript = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            runCatching {
                val name = uri.lastPathSegment?.substringAfterLast("/") ?: "script.user.js"
                val code = context.contentResolver.openInputStream(uri)?.bufferedReader()?.readText().orEmpty()
                val installed = scriptManager.install(name, code)
                userScripts = scriptManager.list()
                notice(if (installed != null) "Userscript dipasang: ${installed.name}" else "Gagal memasang userscript")
            }.onFailure { notice("Gagal membaca file: ${it.message}") }
        }
    }

    fun openWalletApp(uri: String, pkg: String?) {
        runCatching {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uri)).apply {
                pkg?.let { setPackage(it) }
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            notice("Dibuka di aplikasi wallet — kembali ke game tanpa reload.")
        }.onFailure {
            // Fallback: salin URI WalletConnect agar bisa ditempel manual di Rabby.
            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            cm.setPrimaryClip(ClipData.newPlainText("WalletConnect", uri))
            notice("Aplikasi wallet tidak ditemukan — URI WalletConnect disalin.")
        }
    }

    fun applyFullScreen(enabled: Boolean, lock: OrientationLock) {
        fullScreen = enabled
        orientationLock = lock
        activity?.let { act ->
            val controller = WindowCompat.getInsetsController(act.window, act.window.decorView)
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            if (enabled) controller.hide(WindowInsetsCompat.Type.systemBars())
            else controller.show(WindowInsetsCompat.Type.systemBars())
            act.requestedOrientation = when (lock) {
                OrientationLock.AUTO -> ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                OrientationLock.PORTRAIT -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
                OrientationLock.LANDSCAPE -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            }
        }
        // Tanpa reload: configChanges di Manifest menjaga WebView tetap hidup.
        if (enabled) notice("Full Screen ${if (lock == OrientationLock.PORTRAIT) "Vertikal" else if (lock == OrientationLock.LANDSCAPE) "Horizontal" else "Auto"} — tanpa reload game.")
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(modifier = Modifier.fillMaxSize()) {
            if (!fullScreen) {
                // ===== BAR ATAS TUNGGAL (1 baris) =====
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (urlFocusMode) {
                        IconButton(onClick = { urlFocusMode = false; searchDraft = "" }) {
                            Icon(Icons.Filled.ArrowBack, contentDescription = "Batal tanpa reload")
                        }
                        OutlinedTextField(
                            value = searchDraft,
                            onValueChange = { searchDraft = it },
                            placeholder = { Text("Telusuri atau ketik URL", fontFamily = FontFamily.Monospace) },
                            singleLine = true,
                            modifier = Modifier.weight(1f).focusRequester(focusRequester),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                            keyboardActions = KeyboardActions(onGo = {
                                navigate(searchDraft); urlFocusMode = false; searchDraft = ""
                            }),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            )
                        )
                        TextButton(onClick = {
                            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = cm.primaryClip?.getItemAt(0)?.text?.toString().orEmpty()
                            if (clip.isNotEmpty()) { searchDraft = clip; navigate(clip); urlFocusMode = false; searchDraft = "" }
                            else notice("Papan klip kosong")
                        }) { Text("Tempel") }
                    } else {
                        // [🔒 Kolom URL/Search] — ketuk = masuk focus mode (otomatis kosong)
                        Surface(
                            modifier = Modifier.weight(1f).height(40.dp)
                                .clickable {
                                    siteInfoOpen = false; extensionsOpen = false
                                    tabSwitcherOpen = false; menuOpen = false
                                    searchDraft = ""; urlFocusMode = true
                                },
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            tonalElevation = 0.dp
                        ) {
                            Row(modifier = Modifier.padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { siteInfoOpen = true }, modifier = Modifier.size(28.dp)) {
                                    Icon(Icons.Filled.Lock, contentDescription = "Info situs", tint = MaterialTheme.colorScheme.primary)
                                }
                                Icon(Icons.Filled.Search, contentDescription = null, modifier = Modifier.size(16.dp))
                                Text(
                                    text = activeTab.url,
                                    maxLines = 1,
                                    fontFamily = FontFamily.Monospace,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.weight(1f).padding(start = 6.dp)
                                )
                            }
                        }
                        // [🧩 Ekstensi] — toggle langsung, tanpa ←/×
                        IconButton(onClick = { extensionsOpen = !extensionsOpen; siteInfoOpen = false; menuOpen = false }) {
                            Text("🧩", style = MaterialTheme.typography.titleMedium)
                        }
                        // [⛶ Full Layar] — hanya di bar atas
                        IconButton(onClick = { applyFullScreen(true, orientationLock) }) {
                            Text("⛶", style = MaterialTheme.typography.titleMedium)
                        }
                        // [Tab Switcher berangka]
                        IconButton(onClick = { tabSwitcherOpen = true }) {
                            Text("${tabs.size}", style = MaterialTheme.typography.titleMedium, fontFamily = FontFamily.Monospace)
                        }
                        // [⋮ Menu]
                        IconButton(onClick = { menuOpen = true; siteInfoOpen = false; extensionsOpen = false }) {
                            Icon(Icons.Filled.MoreVert, contentDescription = "Menu")
                        }
                    }
                }
                if (progress in 1..99) {
                    LinearProgressIndicator(progress = { progress / 100f }, modifier = Modifier.fillMaxWidth().height(2.dp))
                }
                if (findBarOpen) {
                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = findQuery, onValueChange = {
                                findQuery = it
                                webViewRef?.findAllAsync(it)
                            },
                            placeholder = { Text("Cari di halaman") }, singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { findBarOpen = false; findQuery = ""; webViewRef?.clearMatches() }) {
                            Icon(Icons.Filled.Close, contentDescription = "Tutup cari")
                        }
                    }
                }
            }

            // ===== WEBVIEW =====
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                AndroidView(
                    factory = { ctx ->
                        createKarsaWebView(
                            context = ctx,
                            guard = guard,
                            settings = settings,
                            userScripts = userScripts,
                            scriptManager = scriptManager,
                            callbacks = KarsaWebViewCallbacks(
                                onPageStarted = { url -> updateTab(activeTabId, url = url) },
                                onPageFinished = { url, title ->
                                    updateTab(activeTabId, url = url, title = title.ifEmpty { titleFromUrl(url) })
                                    history.add(0, HistoryItem(title = title.ifEmpty { titleFromUrl(url) }, url = url, visitedAt = "baru saja"))
                                },
                                onProgress = { progress = it },
                                onPopupRequest = { url ->
                                    popupUrl = url; popupTitle = "Pop-up"; popupReason = "window.open() dari halaman"
                                },
                                onWalletDeepLink = { uri, pkg ->
                                    walletDeepLink = Pair(uri, pkg); walletDialogOpen = true
                                },
                                onBlockedSocial = { url, reason ->
                                    popupUrl = url; popupTitle = "Pop-up Fleksibel"; popupReason = reason
                                    notice("Anti-Redirect: $reason")
                                }
                            )
                        ).also { webViewRef = it; it.loadUrl(activeTab.url); lastLoadedUrl = activeTab.url }
                    },
                    update = { view ->
                        // Terapkan zoom tanpa reload saat settings berubah.
                        view.settings.textZoom = settings.zoomPercent
                        // Ganti tab: load hanya jika URL memang beda.
                        if (activeTab.url != lastLoadedUrl && activeTab.url != view.url) {
                            view.loadUrl(activeTab.url); lastLoadedUrl = activeTab.url
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // Overlay alamat (focus mode): kartu halaman aktif + bookmark + batal tanpa reload.
                if (urlFocusMode) {
                    AddressOverlay(
                        searchDraft = searchDraft,
                        activeTab = activeTab,
                        bookmarks = bookmarks,
                        onCancel = { urlFocusMode = false; searchDraft = "" },
                        onNavigate = { navigate(it); urlFocusMode = false; searchDraft = "" },
                        onPaste = {
                            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = cm.primaryClip?.getItemAt(0)?.text?.toString().orEmpty()
                            if (clip.isNotEmpty()) searchDraft = clip else notice("Papan klip kosong")
                        },
                        onShare = {
                            val send = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"; putExtra(Intent.EXTRA_TEXT, activeTab.url)
                            }
                            context.startActivity(Intent.createChooser(send, "Bagikan"))
                        },
                        onCopy = {
                            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            cm.setPrimaryClip(ClipData.newPlainText("URL", activeTab.url))
                            notice("URL disalin")
                        },
                        onEdit = { searchDraft = activeTab.url }
                    )
                }

                // Tombol keluar Full Layar (melayang, tanpa reload).
                if (fullScreen) {
                    TextButton(
                        onClick = { applyFullScreen(false, OrientationLock.AUTO) },
                        modifier = Modifier.align(Alignment.TopEnd).padding(12.dp)
                            .background(Color.Black.copy(alpha = 0.45f), RoundedCornerShape(20.dp))
                    ) { Text("✕ Keluar Full Layar", color = Color.White) }
                }
            }
        }

        // ===== POPOVER & SHEET (di atas WebView) =====
        if (siteInfoOpen) {
            SiteInfoPopover(
                activeTab = activeTab,
                permissions = sitePermissions,
                settings = settings,
                profile = activeProfile,
                onClose = { siteInfoOpen = false },
                onClearSiteData = {
                    CookieManager.getInstance().removeAllCookies(null)
                    webViewRef?.clearCache(true)
                    notice("Cache & cookie situs ini dihapus")
                },
                onResetPermissions = { sitePermissions = SitePermissions(); notice("Izin situs direset") },
                onTogglePermission = { key, v ->
                    sitePermissions = when (key) {
                        "clipboard" -> sitePermissions.copy(clipboard = v)
                        "notifications" -> sitePermissions.copy(notifications = v)
                        "camera" -> sitePermissions.copy(camera = v)
                        "microphone" -> sitePermissions.copy(microphone = v)
                        "location" -> sitePermissions.copy(location = v)
                        "popups" -> sitePermissions.copy(popups = v)
                        "autoplay" -> sitePermissions.copy(autoplay = v)
                        else -> sitePermissions
                    }
                },
                onUpdateSettings = onUpdateSettings,
                onOpenProfiles = { siteInfoOpen = false; menuOpen = true },
                onOpenProxy = { siteInfoOpen = false; menuOpen = true }
            )
        }
        if (extensionsOpen) {
            ExtensionsPopover(
                scripts = userScripts,
                onClose = { extensionsOpen = false },
                onPickFile = { pickScript.launch("*/*") },
                onToggleScript = { script, enabled ->
                    scope.launch {
                        scriptManager.setEnabled(script.id, enabled)
                        userScripts = scriptManager.list()
                    }
                },
                onRemoveScript = { script ->
                    scope.launch {
                        scriptManager.remove(script.id); userScripts = scriptManager.list()
                        notice("Userscript dihapus: ${script.name}")
                    }
                }
            )
        }
        if (tabSwitcherOpen) {
            TabSwitcherSheet(
                tabs = tabs,
                activeTabId = activeTabId,
                onSelect = { t -> activeTabId = t.id; tabSwitcherOpen = false },
                onCloseTab = { id ->
                    if (tabs.size > 1) {
                        tabs.removeAll { it.id == id }
                        if (activeTabId == id) activeTabId = tabs[0].id
                    }
                },
                onAddTab = { incognito ->
                    val t = BrowserTab(title = if (incognito) "Tab Penyamaran (Desktop)" else "Tab Desktop Baru", url = BrowserTab.HOME_URL, incognito = incognito)
                    tabs.add(t); activeTabId = t.id; tabSwitcherOpen = false
                },
                onClose = { tabSwitcherOpen = false }
            )
        }
        if (menuOpen) {
            MenuSheet(
                settings = settings,
                darkTheme = darkTheme,
                profiles = profiles,
                activeProfile = activeProfile,
                bookmarks = bookmarks,
                history = history,
                downloads = downloads,
                onClose = { menuOpen = false },
                onToggleDarkTheme = onToggleDarkTheme,
                onUpdateSettings = onUpdateSettings,
                onSelectProfile = { activeProfile = it; notice("Profil: ${it.name}"); menuOpen = false },
                onAddBookmark = {
                    bookmarks.add(0, BookmarkItem(title = activeTab.title, url = activeTab.url))
                    notice("Bookmark ditambahkan")
                },
                onNavigate = { navigate(it); menuOpen = false },
                onFindInPage = { findBarOpen = true; menuOpen = false },
                onSavePdf = {
                    // Simpan PDF via PrintManager (Android native, tanpa server).
                    runCatching {
                        val printManager = context.getSystemService(Context.PRINT_SERVICE) as android.print.PrintManager
                        // createPrintDocumentAdapter via safe-call menghasilkan nullable -> batalkan runCatching bila null.
                        val adapter = webViewRef?.createPrintDocumentAdapter(activeTab.title) ?: return@runCatching
                        printManager.print(activeTab.title, adapter, null)
                    }.onFailure { notice("Gagal simpan PDF: ${it.message}") }
                },
                onClearCookies = {
                    CookieManager.getInstance().removeAllCookies(null)
                    notice("Semua cookie dihapus")
                },
                onRunJs = { code -> webViewRef?.evaluateJavascript(code, null) },
                onZoom = { pct -> onUpdateSettings { it.copy(zoomPercent = pct.coerceIn(50, 200)) } },
                onOpenIncognito = {
                    val t = BrowserTab(title = "Tab Penyamaran (Desktop)", url = BrowserTab.HOME_URL, incognito = true)
                    tabs.add(t); activeTabId = t.id
                },
                onCheckUpdate = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Febbio11/karsa-browser-android/releases"))
                    context.startActivity(intent)
                }
            )
        }
        popupUrl?.let { url ->
            FlexiblePopup(
                title = popupTitle.ifEmpty { "Pop-up Fleksibel" },
                url = url,
                reason = popupReason,
                onPromoteToTab = {
                    val t = BrowserTab(title = "$popupTitle (Sesi OAuth Aktif)", url = url)
                    tabs.add(t); activeTabId = t.id; popupUrl = null
                    notice("Pop-up dipindah ke Tab Baru tanpa reload")
                },
                onKeepInPopup = { /* tetap tampil sebagai overlay WebView mini */ },
                onClose = { popupUrl = null }
            )
        }
        if (walletDialogOpen) {
            WalletConfirmDialog(
                deepLink = walletDeepLink.first,
                targetPackage = walletDeepLink.second,
                onOpenApp = { openWalletApp(walletDeepLink.first, walletDeepLink.second); walletDialogOpen = false },
                onCopyUri = {
                    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    cm.setPrimaryClip(ClipData.newPlainText("WalletConnect", walletDeepLink.first))
                    notice("URI WalletConnect disalin — tempel di Rabby Wallet")
                    walletDialogOpen = false
                },
                onCancel = { walletDialogOpen = false }
            )
        }
        SnackbarHost(hostState = snackbar, modifier = Modifier.align(Alignment.BottomCenter))
    }
}
