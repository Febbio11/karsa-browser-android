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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import androidx.compose.runtime.key
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.karsa.browser.browser.AppUpdater
import com.karsa.browser.browser.BrowserProfile
import com.karsa.browser.browser.BrowserTab
import com.karsa.browser.browser.BookmarkItem
import com.karsa.browser.browser.DEFAULT_SEARCH_ENGINES
import com.karsa.browser.browser.DownloadItem
import com.karsa.browser.browser.HistoryItem
import com.karsa.browser.browser.HomeShortcut
import com.karsa.browser.browser.KarsaWebViewCallbacks
import com.karsa.browser.browser.NavigationGuard
import com.karsa.browser.browser.OrientationLock
import com.karsa.browser.browser.RenderSettings
import com.karsa.browser.browser.SitePermissions
import com.karsa.browser.browser.activeSearchEngine
import com.karsa.browser.browser.createKarsaWebView
import com.karsa.browser.browser.isUrlLike
import com.karsa.browser.browser.resolveNavigationInput
import com.karsa.browser.browser.shortcutBadgeFor
import com.karsa.browser.browser.titleFromUrl
import com.karsa.browser.ui.components.AddressOverlay
import com.karsa.browser.ui.components.ExtensionsPopover
import com.karsa.browser.ui.components.FlexiblePopup
import com.karsa.browser.ui.components.KarsaHomePage
import com.karsa.browser.ui.components.MenuSheet
import com.karsa.browser.ui.components.SearchEngineBadge
import com.karsa.browser.ui.components.SearchEngineSheet
import com.karsa.browser.ui.components.SiteInfoPopover
import com.karsa.browser.ui.components.TabSwitcherSheet
import com.karsa.browser.ui.components.WalletConfirmDialog
import com.karsa.browser.ui.theme.KarsaSerif
import com.karsa.browser.userscript.UserScript
import com.karsa.browser.userscript.UserScriptManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Layar utama v1.0.2 (porting prototipe commit 36cd752):
 * - Top bar 3 elemen: [pill URL lebar] -> [Ekstensi] -> [Full Layar], statusBarsPadding().
 * - Bottom bar 5 menu: Home | Bookmark 1-tap | Tambah Tab + | Menu Tab berangka | ⋮,
 *   navigationBarsPadding(). Keduanya disembunyikan saat Full Screen.
 * - Home orisinal Karsa (karsa://home) sebagai tampilan awal; state 100% fresh.
 * - Mode Mobile default (UA mobile bersih); toggle Mode Desktop per tab di menu ⋮.
 * - Multi mesin pencari + autocomplete lokal.
 * Semua ikon vektor monokrom (Material Rounded) — nol emoji sebagai ikon.
 */
@Composable
fun BrowserScreen(
    settings: RenderSettings,
    darkTheme: Boolean,
    onToggleDarkTheme: () -> Unit,
    onUpdateSettings: ((RenderSettings) -> RenderSettings) -> Unit,
    shortcuts: List<HomeShortcut> = emptyList(),
    onUpdateShortcuts: (List<HomeShortcut>) -> Unit = {},
    rememberDesktopHosts: Set<String> = emptySet(),
    onToggleRememberDesktop: (String, Boolean) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    fun notice(msg: String) { scope.launch { snackbar.showSnackbar(msg) } }

    val engine = activeSearchEngine(settings)
    val guard = remember { NavigationGuard() }
    // Sinkronkan whitelist dari settings (toggle di ikon gembok).
    LaunchedEffect(settings.walletWhitelistEnabled, settings.antiRedirectEnabled) {
        guard.walletPolicy = guard.walletPolicy.copy(enabled = settings.walletWhitelistEnabled)
    }

    val scriptManager = remember { UserScriptManager(context) }
    var userScripts by remember { mutableStateOf<List<UserScript>>(emptyList()) }
    // v1.0.2: tidak ada userscript contoh — daftar 100% kosong saat fresh.
    LaunchedEffect(Unit) { userScripts = scriptManager.list() }

    // v1.0.3: cek update otomatis sekali saat aplikasi dibuka (diam-diam).
    var updateInfo by remember { mutableStateOf<AppUpdater.UpdateInfo?>(null) }
    LaunchedEffect(Unit) {
        delay(1500)
        val info = AppUpdater.checkForUpdate(context)
        if (info != null) updateInfo = info
    }

    // --- State tab & navigasi: v1.0.2 hanya 1 tab Home, tanpa seed dummy. ---
    val tabs = remember { mutableStateListOf(BrowserTab(title = "Beranda", url = BrowserTab.HOME_URL)) }
    var activeTabId by remember { mutableStateOf(tabs[0].id) }
    val activeTab: BrowserTab = tabs.firstOrNull { it.id == activeTabId } ?: tabs[0]
    var progress by remember { mutableStateOf(0) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var lastLoadedUrl by remember { mutableStateOf<String?>(null) }

    fun updateTab(id: String, title: String? = null, url: String? = null, desktopMode: Boolean? = null) {
        val i = tabs.indexOfFirst { it.id == id }
        if (i < 0) return
        val old = tabs[i]
        tabs[i] = old.copy(
            title = title ?: old.title,
            url = url ?: old.url,
            desktopMode = desktopMode ?: old.desktopMode
        )
    }

    fun navigate(raw: String) {
        val url = resolveNavigationInput(raw, engine)
        if (url.isEmpty()) return
        val isSearch = !isUrlLike(raw)
        val title = if (url == BrowserTab.HOME_URL) "Beranda"
            else if (isSearch) "${raw.trim()} — ${engine.name}"
            else titleFromUrl(url)
        // Ke Home: selalu kembali Mode Mobile. Host yang diingat -> Desktop.
        val host = runCatching { java.net.URL(url).host }.getOrNull().orEmpty()
        val desktop = if (url == BrowserTab.HOME_URL) false
            else if (rememberDesktopHosts.contains(host)) true
            else activeTab.desktopMode
        updateTab(activeTabId, url = url, title = title, desktopMode = desktop)
        if (webViewRef?.url != url && url != BrowserTab.HOME_URL) {
            webViewRef?.loadUrl(url)
            lastLoadedUrl = url
        }
    }

    // --- State overlay ---
    var urlFocusMode by remember { mutableStateOf(false) }
    var searchDraft by remember { mutableStateOf("") }
    var enginePickerOpen by remember { mutableStateOf(false) }
    var clipboardUrl by remember { mutableStateOf<String?>(null) }
    var siteInfoOpen by remember { mutableStateOf(false) }
    var extensionsOpen by remember { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }
    var searchEngineSheetOpen by remember { mutableStateOf(false) }
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
    val profiles = remember { listOf(BrowserProfile()) }
    // v1.0.2: bookmark / riwayat / unduhan mulai KOSONG (fresh).
    val bookmarks = remember { mutableStateListOf<BookmarkItem>() }
    val history = remember { mutableStateListOf<HistoryItem>() }
    val downloads = remember { mutableStateListOf<DownloadItem>() }
    // Toast Bookmark mengambang (auto-hide 2,5 detik).
    var bookmarkToast by remember { mutableStateOf<String?>(null) }
    var bookmarkToastHasArrow by remember { mutableStateOf(false) }
    LaunchedEffect(bookmarkToast) {
        if (bookmarkToast != null) { delay(2500); bookmarkToast = null }
    }

    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(urlFocusMode) {
        if (urlFocusMode) {
            focusRequester.requestFocus()
            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = cm.primaryClip?.getItemAt(0)?.text?.toString().orEmpty()
            clipboardUrl = if (clip.startsWith("http://") || clip.startsWith("https://")) clip else null
        }
    }

    // File picker userscript (.user.js / .js) — ikon Ekstensi -> Pasang.
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
            notice("Dibuka di aplikasi wallet — kembali ke halaman tanpa reload.")
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
        if (enabled) notice("Full Screen ${if (lock == OrientationLock.PORTRAIT) "Vertikal" else if (lock == OrientationLock.LANDSCAPE) "Horizontal" else "Auto"} — tanpa reload halaman.")
    }

    fun closeAllOverlays() {
        siteInfoOpen = false; extensionsOpen = false; tabSwitcherOpen = false
        menuOpen = false; searchEngineSheetOpen = false; urlFocusMode = false
        enginePickerOpen = false
    }

    fun toggleDesktopMode() {
        val next = !activeTab.desktopMode
        updateTab(activeTabId, desktopMode = next)
        notice(
            if (next) "Mode Desktop aktif · UA Chrome Desktop + Wide Viewport dimuat ulang."
            else "Mode Mobile aktif · UA Chrome Mobile bersih (tanpa \"; wv\")."
        )
    }

    fun toggleCurrentBookmark() {
        if (activeTab.isHome) return // di Home: nonaktif (buka situs dulu).
        val existing = bookmarks.firstOrNull { it.url == activeTab.url }
        if (existing != null) {
            bookmarks.remove(existing)
            bookmarkToast = "Bookmark dihapus"; bookmarkToastHasArrow = false
        } else {
            bookmarks.add(0, BookmarkItem(title = activeTab.title, url = activeTab.url))
            bookmarkToast = "Bookmark disimpan"; bookmarkToastHasArrow = true
        }
    }

    val isBookmarked = bookmarks.any { it.url == activeTab.url }
    val barIconTint = MaterialTheme.colorScheme.onSurface

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(modifier = Modifier.fillMaxSize()) {
            if (!fullScreen) {
                // ===== BAR ATAS (56.dp, 3 elemen) — statusBarsPadding() wajib. =====
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .height(56.dp)
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (urlFocusMode) {
                        IconButton(onClick = { urlFocusMode = false; searchDraft = ""; enginePickerOpen = false }) {
                            Icon(Icons.Rounded.ArrowBack, contentDescription = "Batal tanpa reload")
                        }
                        Surface(
                            modifier = Modifier.weight(1f).height(40.dp),
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f))
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 4.dp, end = 8.dp)) {
                                IconButton(
                                    onClick = { enginePickerOpen = !enginePickerOpen },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        SearchEngineBadge(engine = engine)
                                        Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = "Ganti mesin pencari", modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                                OutlinedTextField(
                                    value = searchDraft,
                                    onValueChange = { searchDraft = it; enginePickerOpen = false },
                                    placeholder = { Text("Telusuri ${engine.name} atau ketik URL", fontSize = 13.sp) },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f).focusRequester(focusRequester),
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                                    keyboardActions = KeyboardActions(onGo = {
                                        navigate(searchDraft); urlFocusMode = false; searchDraft = ""; enginePickerOpen = false
                                    }),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color.Transparent,
                                        unfocusedBorderColor = Color.Transparent,
                                        focusedContainerColor = Color.Transparent,
                                        unfocusedContainerColor = Color.Transparent
                                    )
                                )
                                if (searchDraft.isNotEmpty()) {
                                    IconButton(onClick = { searchDraft = "" }, modifier = Modifier.size(32.dp)) {
                                        Icon(Icons.Rounded.Close, contentDescription = "Hapus", modifier = Modifier.size(16.dp))
                                    }
                                    TextButton(onClick = {
                                        navigate(searchDraft); urlFocusMode = false; searchDraft = ""; enginePickerOpen = false
                                    }) { Text("Buka", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary) }
                                } else {
                                    TextButton(onClick = {
                                        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        val clip = cm.primaryClip?.getItemAt(0)?.text?.toString().orEmpty()
                                        if (clip.isNotEmpty()) { searchDraft = clip; navigate(clip); urlFocusMode = false; searchDraft = "" }
                                        else notice("Papan klip kosong")
                                    }) { Text("Tempel") }
                                }
                            }
                        }
                    } else {
                        // [Pill URL lebar] — ketuk = focus mode (otomatis kosong).
                        Surface(
                            modifier = Modifier.weight(1f).height(42.dp)
                                .clickable {
                                    closeAllOverlays()
                                    searchDraft = ""; urlFocusMode = true
                                },
                            shape = RoundedCornerShape(21.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                            tonalElevation = 1.dp
                        ) {
                            Row(modifier = Modifier.padding(start = 4.dp, end = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { siteInfoOpen = true }, modifier = Modifier.size(36.dp)) {
                                    Icon(Icons.Rounded.Lock, contentDescription = "Info situs", modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                                }
                                Text(
                                    text = if (activeTab.isHome) "Telusuri ${engine.name} atau ketik URL" else activeTab.url.removePrefix("https://").removePrefix("http://"),
                                    maxLines = 1,
                                    fontSize = 13.5.sp,
                                    color = if (activeTab.isHome) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                        // [Ekstensi] — ikon vektor, toggle.
                        IconButton(onClick = { closeAllOverlays(); extensionsOpen = !extensionsOpen }) {
                            Icon(Icons.Rounded.Extension, contentDescription = "Ekstensi", modifier = Modifier.size(22.dp), tint = barIconTint)
                        }
                        // [Full Layar] — ikon vektor.
                        IconButton(onClick = { closeAllOverlays(); applyFullScreen(true, orientationLock) }) {
                            Icon(Icons.Rounded.Fullscreen, contentDescription = "Full Layar", modifier = Modifier.size(22.dp), tint = barIconTint)
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
                            Icon(Icons.Rounded.Close, contentDescription = "Tutup cari")
                        }
                    }
                }
            }

            // ===== KONTEN: Home orisinal / WebView =====
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                if (activeTab.isHome) {
                    KarsaHomePage(
                        shortcuts = shortcuts,
                        activeEngine = engine,
                        onOpenSearchFocus = { openPicker -> enginePickerOpen = openPicker; urlFocusMode = true; searchDraft = "" },
                        onNavigateShortcut = { url, label -> navigate(url) },
                        onAddShortcut = { label, url ->
                            val badge = shortcutBadgeFor(label)
                            onUpdateShortcuts(shortcuts + HomeShortcut(label = label, url = url, badge = badge))
                            notice("Pintasan \"$label\" ditambahkan ke Beranda.")
                        },
                        onRemoveShortcut = { id ->
                            onUpdateShortcuts(shortcuts.filter { it.id != id })
                        }
                    )
                } else {
                    // WebView dibuat ulang saat Mode Desktop berubah (UA + viewport + reload).
                    key(activeTab.desktopMode) {
                        AndroidView(
                            factory = { ctx ->
                                createKarsaWebView(
                                    context = ctx,
                                    guard = guard,
                                    settings = settings,
                                    userScripts = userScripts,
                                    scriptManager = scriptManager,
                                    desktopMode = activeTab.desktopMode,
                                    callbacks = KarsaWebViewCallbacks(
                                        onPageStarted = { url -> updateTab(activeTabId, url = url) },
                                        onPageFinished = { url, title ->
                                            updateTab(activeTabId, url = url, title = title.ifEmpty { titleFromUrl(url) })
                                            if (!activeTab.isHome) {
                                                history.add(0, HistoryItem(title = title.ifEmpty { titleFromUrl(url) }, url = url, visitedAt = "baru saja"))
                                            }
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
                    }
                }

                // Overlay alamat (focus mode): picker engine + kartu + saran.
                if (urlFocusMode) {
                    AddressOverlay(
                        searchDraft = searchDraft,
                        onDraftChange = { searchDraft = it },
                        activeTab = activeTab,
                        bookmarks = bookmarks,
                        history = history,
                        engine = engine,
                        suggestionsEnabled = settings.searchSuggestionsEnabled,
                        enginePickerOpen = enginePickerOpen,
                        onToggleEnginePicker = { enginePickerOpen = !enginePickerOpen },
                        onSelectEngine = { id ->
                            onUpdateSettings { it.copy(searchEngineId = id.id) }
                            enginePickerOpen = false
                            notice("Mesin pencari aktif: ${DEFAULT_SEARCH_ENGINES.first { it.id == id }.name}")
                        },
                        onOpenSearchEngineSheet = { urlFocusMode = false; enginePickerOpen = false; searchEngineSheetOpen = true },
                        clipboardUrl = clipboardUrl,
                        onCancel = { urlFocusMode = false; searchDraft = ""; enginePickerOpen = false },
                        onNavigate = { navigate(it); urlFocusMode = false; searchDraft = ""; enginePickerOpen = false },
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

            // ===== BOTTOM BAR (56.dp, 5 menu) — navigationBarsPadding() wajib. =====
            if (!fullScreen) {
                Surface(
                    modifier = Modifier.fillMaxWidth().navigationBarsPadding(),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 1.dp
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 1. Home
                        IconButton(
                            onClick = { closeAllOverlays(); navigate(BrowserTab.HOME_URL) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                Icons.Rounded.Home, contentDescription = "Home",
                                modifier = Modifier.size(22.dp),
                                tint = if (activeTab.isHome) MaterialTheme.colorScheme.onSurface else barIconTint
                            )
                        }
                        // 2. Bookmark 1-tap (nonaktif di Home).
                        IconButton(
                            onClick = { if (!activeTab.isHome) toggleCurrentBookmark() },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                if (isBookmarked) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                                contentDescription = if (activeTab.isHome) "Buka situs terlebih dahulu untuk menyimpan Bookmark"
                                    else if (isBookmarked) "Hapus halaman ini dari Bookmark" else "Simpan halaman ini ke Bookmark (1-ketuk)",
                                modifier = Modifier.size(22.dp),
                                tint = when {
                                    activeTab.isHome -> Color(0xFFA09A8E)
                                    isBookmarked -> Color(0xFF141413)
                                    else -> barIconTint
                                }
                            )
                        }
                        // 3. Tambah Tab +
                        IconButton(
                            onClick = {
                                closeAllOverlays()
                                val t = BrowserTab(title = "Beranda", url = BrowserTab.HOME_URL)
                                tabs.add(t); activeTabId = t.id
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = "Tambah Tab", modifier = Modifier.size(24.dp), tint = barIconTint)
                        }
                        // 4. Menu Tab (kotak berangka).
                        Box(
                            modifier = Modifier.weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .border(1.75.dp, MaterialTheme.colorScheme.onSurface, RoundedCornerShape(4.dp))
                                    .background(
                                        if (tabSwitcherOpen) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent,
                                        RoundedCornerShape(4.dp)
                                    )
                                    .clickable { closeAllOverlays(); tabSwitcherOpen = !tabSwitcherOpen },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    if (tabs.size > 99) ":D" else "${tabs.size}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = if (tabSwitcherOpen) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                        // 5. Menu Titik Tiga.
                        IconButton(
                            onClick = { closeAllOverlays(); menuOpen = !menuOpen },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Rounded.MoreVert, contentDescription = "Menu", modifier = Modifier.size(22.dp), tint = barIconTint)
                        }
                    }
                }
            }
        }

        // Toast Bookmark mengambang (di atas bottom bar, auto-hide 2,5 detik).
        if (bookmarkToast != null && !fullScreen) {
            Surface(
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 76.dp),
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.inverseSurface,
                tonalElevation = 6.dp,
                shadowElevation = 8.dp,
                onClick = {
                    if (bookmarkToastHasArrow) { bookmarkToast = null; menuOpen = true }
                }
            ) {
                Row(modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(28.dp).background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(6.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(activeTab.faviconLabel, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(bookmarkToast ?: "", color = MaterialTheme.colorScheme.inverseOnSurface, fontSize = 13.sp)
                    if (bookmarkToastHasArrow) {
                        Icon(Icons.Rounded.ChevronRight, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.inverseOnSurface)
                    }
                }
            }
        }

        // ===== POPOVER & SHEET (di atas WebView) =====
        if (siteInfoOpen) {
            val siteHost = activeTab.host
            SiteInfoPopover(
                activeTab = activeTab,
                permissions = sitePermissions,
                settings = settings,
                profile = activeProfile,
                rememberDesktopForSite = rememberDesktopHosts.contains(siteHost),
                onToggleRememberDesktopForSite = {
                    val next = !rememberDesktopHosts.contains(siteHost)
                    onToggleRememberDesktop(siteHost, next)
                    if (next) {
                        updateTab(activeTabId, desktopMode = true)
                        notice("Mode Desktop diingat otomatis untuk $siteHost.")
                    } else {
                        notice("Preferensi Mode Desktop untuk $siteHost dihapus (kembali Mode Mobile).")
                    }
                },
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
                    val t = BrowserTab(title = "Beranda", url = BrowserTab.HOME_URL, incognito = incognito)
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
                desktopMode = activeTab.desktopMode,
                onToggleDesktopMode = { toggleDesktopMode() },
                onOpenSearchEngine = { menuOpen = false; searchEngineSheetOpen = true },
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
                    val t = BrowserTab(title = "Beranda", url = BrowserTab.HOME_URL, incognito = true)
                    tabs.add(t); activeTabId = t.id
                },
                onCheckUpdate = {
                    // v1.0.3: cek update via update.json (bukan buka browser).
                    menuOpen = false
                    scope.launch {
                        val info = AppUpdater.checkForUpdate(context)
                        if (info != null) updateInfo = info
                        else notice("Karsa sudah versi terbaru, atau update belum tersedia")
                    }
                }
            )
        }
        if (searchEngineSheetOpen) {
            SearchEngineSheet(
                settings = settings,
                onUpdateSettings = onUpdateSettings,
                onSelectEngine = { id ->
                    onUpdateSettings { it.copy(searchEngineId = id.id) }
                    notice("Mesin pencari aktif: ${DEFAULT_SEARCH_ENGINES.first { it.id == id }.name}")
                },
                onClose = { searchEngineSheetOpen = false }
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
        // Dialog update otomatis (v1.0.3).
        updateInfo?.let { info ->
            AlertDialog(
                onDismissRequest = { updateInfo = null },
                title = { Text("Update tersedia — Karsa v${info.versionName}", fontFamily = KarsaSerif) },
                text = {
                    Text("Build ${info.versionCode} sudah tersedia di GitHub Releases. Unduh dan pasang sekarang? Aplikasi akan restart setelah update.")
                },
                confirmButton = {
                    Button(onClick = {
                        val started = AppUpdater.downloadAndInstall(context, info) {
                            updateInfo = null
                            notice("Mengunduh update Karsa v${info.versionName}...")
                        }
                        if (!started) {
                            updateInfo = null
                            notice("Izinkan \"Install unknown apps\" untuk Karsa, lalu ketuk Update lagi.")
                        }
                    }) { Text("Update Sekarang") }
                },
                dismissButton = {
                    TextButton(onClick = { updateInfo = null }) { Text("Nanti") }
                }
            )
        }
        SnackbarHost(hostState = snackbar, modifier = Modifier.align(Alignment.BottomCenter))
    }
}
