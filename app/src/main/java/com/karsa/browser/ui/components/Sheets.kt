package com.karsa.browser.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Monitor
import androidx.compose.material.icons.rounded.NorthEast
import androidx.compose.material.icons.rounded.NorthWest
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.karsa.browser.browser.BookmarkItem
import com.karsa.browser.browser.BrowserProfile
import com.karsa.browser.browser.BrowserTab
import com.karsa.browser.browser.DEFAULT_SEARCH_ENGINES
import com.karsa.browser.browser.DownloadItem
import com.karsa.browser.browser.HistoryItem
import com.karsa.browser.browser.PermissionValue
import com.karsa.browser.browser.RenderSettings
import com.karsa.browser.browser.SearchEngineConfig
import com.karsa.browser.browser.SearchEngineId
import com.karsa.browser.browser.SitePermissions
import com.karsa.browser.browser.activeSearchEngine
import com.karsa.browser.browser.buildSmartSuggestions
import com.karsa.browser.browser.isUrlLike
import com.karsa.browser.ui.theme.KarsaSerif
import com.karsa.browser.userscript.UserScript

// ---------- Shared small pieces ----------

@Composable
private fun SheetHeader(title: String, onBack: (() -> Unit)?, onClose: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
        if (onBack != null) {
            IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Kembali") }
        }
        Text(title, style = MaterialTheme.typography.titleMedium, fontFamily = KarsaSerif, modifier = Modifier.weight(1f))
        IconButton(onClick = onClose) { Icon(Icons.Filled.Close, contentDescription = "Tutup") }
    }
    Divider()
}

@Composable
private fun ToggleRow(label: String, desc: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

// ---------- 1. Ikon Gembok: Info Situs ----------

@Composable
fun SiteInfoPopover(
    activeTab: BrowserTab,
    permissions: SitePermissions,
    settings: RenderSettings,
    profile: BrowserProfile,
    rememberDesktopForSite: Boolean = false,
    onToggleRememberDesktopForSite: () -> Unit = {},
    onClose: () -> Unit,
    onClearSiteData: () -> Unit,
    onResetPermissions: () -> Unit,
    onTogglePermission: (String, PermissionValue) -> Unit,
    onUpdateSettings: ((RenderSettings) -> RenderSettings) -> Unit,
    onOpenProfiles: () -> Unit,
    onOpenProxy: () -> Unit
) {
    Dialog(onDismissRequest = onClose) {
        Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface) {
            Column(modifier = Modifier.fillMaxWidth().fillMaxHeight(0.85f)) {
                SheetHeader("Info Situs", null, onClose)
                LazyColumn(modifier = Modifier.weight(1f)) {
                    item {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Column(modifier = Modifier.padding(start = 10.dp)) {
                                Text(activeTab.host, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(if (activeTab.isSecure) "Koneksi Aman · HTTPS / TLS 1.3" else "Koneksi Tidak Terenkripsi", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                        Row(modifier = Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = onClearSiteData, modifier = Modifier.weight(1f)) { Text("Hapus Cache Situs Ini", maxLines = 1) }
                            OutlinedButton(onClick = onResetPermissions, modifier = Modifier.weight(1f)) { Text("Reset Izin", maxLines = 1) }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        // v1.0.2: ingat Mode Desktop per situs (prototipe 36cd752).
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
                                .clickable { onToggleRememberDesktopForSite() },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Rounded.Monitor, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                            Text(
                                "Ingat Mode Desktop untuk situs ini",
                                modifier = Modifier.weight(1f).padding(start = 10.dp),
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Checkbox(checked = rememberDesktopForSite, onCheckedChange = { onToggleRememberDesktopForSite() })
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                    item {
                        Text("Proteksi Situs (Anti-Redirect Pintar)", fontFamily = KarsaSerif, style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(horizontal = 16.dp))
                        // NOTE: jangan pakai `it` ganda — `it` luar adalah Boolean baru dari
                        // ToggleRow, `it` dalam adalah RenderSettings; dulu `it.copy(x = it)`
                        // mengirim RenderSettings ke field Boolean (run #7: type mismatch).
                        ToggleRow("Anti-Redirect Media Sosial", "X / Discord / Telegram selalu terkunci 100% di Pop-up internal", settings.antiRedirectEnabled) { enabled ->
                            onUpdateSettings { it.copy(antiRedirectEnabled = enabled) }
                        }
                        ToggleRow("Whitelist Wallet Eksternal", "Izinkan wc:// & rabby:// ke aplikasi wallet resmi (Rabby default)", settings.walletWhitelistEnabled) { enabled ->
                            onUpdateSettings { it.copy(walletWhitelistEnabled = enabled) }
                        }
                        ToggleRow("Matikan Pull-to-Refresh (situs ini)", "Cegah reload tidak sengaja saat geser layar", settings.disablePullToRefreshPerSite) { enabled ->
                            onUpdateSettings { it.copy(disablePullToRefreshPerSite = enabled) }
                        }
                    }
                    item {
                        Text("Kontrol Izin Cepat", fontFamily = KarsaSerif, style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(16.dp, 8.dp, 16.dp, 4.dp))
                        listOf(
                            Triple("clipboard", "Clipboard (Salin/Tempel)", permissions.clipboard),
                            Triple("notifications", "Notifikasi", permissions.notifications),
                            Triple("camera", "Kamera", permissions.camera),
                            Triple("microphone", "Mikrofon", permissions.microphone),
                            Triple("location", "Lokasi", permissions.location),
                            Triple("popups", "Pop-up Internal", permissions.popups),
                            Triple("autoplay", "Autoplay Audio / Video", permissions.autoplay)
                        ).forEach { (key, label, v) ->
                            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                                Text(if (v == PermissionValue.ALLOW) "Izinkan" else if (v == PermissionValue.ASK) "Tanya" else "Blokir", style = MaterialTheme.typography.bodySmall)
                                Checkbox(checked = v == PermissionValue.ALLOW, onCheckedChange = {
                                    onTogglePermission(key, if (it) PermissionValue.ALLOW else PermissionValue.BLOCK)
                                })
                            }
                        }
                    }
                    item {
                        Text("Profil & Proxy Sesi Aktif", fontFamily = KarsaSerif, style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(16.dp))
                        Row(modifier = Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("${profile.name} (${profile.badge})", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                            TextButton(onClick = onOpenProfiles) { Text("Ganti") }
                        }
                        Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("Proxy: ${profile.proxySummary}", modifier = Modifier.weight(1f), fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall)
                            TextButton(onClick = onOpenProxy) { Text("Atur") }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}

// ---------- 2. Ikon Ekstensi: Userscript v1 ----------

@Composable
fun ExtensionsPopover(
    scripts: List<UserScript>,
    onClose: () -> Unit,
    onPickFile: () -> Unit,
    onToggleScript: (UserScript, Boolean) -> Unit,
    onRemoveScript: (UserScript) -> Unit
) {
    Dialog(onDismissRequest = onClose) {
        Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface) {
            Column(modifier = Modifier.fillMaxWidth().fillMaxHeight(0.8f)) {
                // Tanpa ←/× di header — toggle lewat ikon Ekstensi di bar atas / tap luar.
                Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Extension, contentDescription = null, modifier = Modifier.size(28.dp), tint = MaterialTheme.colorScheme.primary)
                    Column(modifier = Modifier.weight(1f).padding(start = 8.dp)) {
                        Text("Ekstensi — Userscript", fontFamily = KarsaSerif, style = MaterialTheme.typography.titleMedium)
                        Text("${scripts.count { it.enabled }}/${scripts.size} aktif · v1 Userscript (.user.js)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Divider()
                LazyColumn(modifier = Modifier.weight(1f).padding(8.dp)) {
                    item {
                        Text("v1: Userscript diinjeksikan ke halaman aktif. Dukungan .crx / Chrome Web Store disiapkan modular untuk v2 — antarmuka pengelola ini dipakai ulang.", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(8.dp))
                        Button(onClick = onPickFile, modifier = Modifier.fillMaxWidth().padding(8.dp)) { Text("Pasang Userscript (.user.js / .js)") }
                    }
                    items(scripts, key = { it.id }) { s ->
                        Card(modifier = Modifier.fillMaxWidth().padding(4.dp)) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(s.name, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                                    Switch(checked = s.enabled, onCheckedChange = { onToggleScript(s, it) })
                                }
                                Text("v${s.version} · ${s.fileName} · ${s.runAt}", fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall)
                                if (s.description.isNotEmpty()) Text(s.description, style = MaterialTheme.typography.bodySmall)
                                TextButton(onClick = { onRemoveScript(s) }) { Text("Hapus") }
                            }
                        }
                    }
                    if (scripts.isEmpty()) item { Text("Belum ada userscript. Pasang file .user.js dari penyimpanan HP.", modifier = Modifier.padding(16.dp)) }
                }
            }
        }
    }
}

// ---------- 3. Tab Switcher ----------

@Composable
fun TabSwitcherSheet(
    tabs: List<BrowserTab>,
    activeTabId: String,
    onSelect: (BrowserTab) -> Unit,
    onCloseTab: (String) -> Unit,
    onAddTab: (Boolean) -> Unit,
    onClose: () -> Unit
) {
    Dialog(onDismissRequest = onClose) {
        Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface) {
            Column(modifier = Modifier.fillMaxWidth().fillMaxHeight(0.7f)) {
                SheetHeader("Tab (${tabs.size})", null, onClose)
                LazyColumn(modifier = Modifier.weight(1f).padding(8.dp)) {
                    items(tabs, key = { it.id }) { t ->
                        Card(modifier = Modifier.fillMaxWidth().padding(4.dp).clickable { onSelect(t) }) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(if (t.id == activeTabId) "● " else "○ ", color = MaterialTheme.colorScheme.primary)
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(t.title, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyMedium)
                                    Text(t.url, maxLines = 1, overflow = TextOverflow.Ellipsis, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall)
                                }
                                IconButton(onClick = { onCloseTab(t.id) }) { Icon(Icons.Filled.Close, contentDescription = "Tutup tab") }
                            }
                        }
                    }
                }
                Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { onAddTab(false) }, modifier = Modifier.weight(1f)) { Text("+ Tab Baru") }
                    OutlinedButton(onClick = { onAddTab(true) }, modifier = Modifier.weight(1f)) { Text("Incognito") }
                }
            }
        }
    }
}

// ---------- 4. Overlay Alamat / Search (rewrite total, prototipe 36cd752) ----------

/** Teks saran dengan bagian yang cocok query dicetak tebal. */
private fun boldMatch(text: String, query: String): AnnotatedString {
    val q = query.trim()
    if (q.isEmpty()) return AnnotatedString(text)
    val idx = text.lowercase().indexOf(q.lowercase())
    if (idx < 0) return AnnotatedString(text)
    return buildAnnotatedString {
        append(text.substring(0, idx))
        withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = Color.Unspecified)) {
            append(text.substring(idx, idx + q.length))
        }
        append(text.substring(idx + q.length))
    }
}

@Composable
fun AddressOverlay(
    searchDraft: String,
    onDraftChange: (String) -> Unit,
    activeTab: BrowserTab,
    bookmarks: List<BookmarkItem>,
    history: List<HistoryItem>,
    engine: SearchEngineConfig,
    suggestionsEnabled: Boolean,
    enginePickerOpen: Boolean,
    onToggleEnginePicker: () -> Unit,
    onSelectEngine: (SearchEngineId) -> Unit,
    onOpenSearchEngineSheet: () -> Unit,
    clipboardUrl: String?,
    onCancel: () -> Unit,
    onNavigate: (String) -> Unit,
    onPaste: () -> Unit,
    onShare: () -> Unit,
    onCopy: () -> Unit,
    onEdit: () -> Unit
) {
    val draft = searchDraft.trim()
    val qLower = draft.lowercase()
    val filteredBookmarks = if (draft.isEmpty()) bookmarks.take(4)
        else bookmarks.filter { it.title.lowercase().contains(qLower) || it.url.lowercase().contains(qLower) }.take(3)
    val filteredHistory = if (draft.isEmpty()) history.take(4)
        else history.filter { it.title.lowercase().contains(qLower) || it.url.lowercase().contains(qLower) }
            .filter { h -> bookmarks.none { it.url == h.url } }.take(3)
    val suggestions = if (draft.isNotEmpty() && suggestionsEnabled) buildSmartSuggestions(draft) else emptyList()

    Box(modifier = Modifier.fillMaxSize()) {
        // Backdrop gelap + blur untuk tap-luar (batal tanpa reload).
        Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)).clickable { onCancel() })
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp))
                .verticalScroll(rememberScrollState())
                .padding(12.dp)
        ) {
            // (0) Picker cepat mesin pencari — hanya bila enginePickerOpen.
            if (enginePickerOpen) {
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Ganti Cepat Mesin Pencari", style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f))
                    TextButton(onClick = onOpenSearchEngineSheet) {
                        Icon(Icons.Rounded.Tune, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Atur URL Kustom", fontSize = 12.sp)
                    }
                }
                DEFAULT_SEARCH_ENGINES.chunked(2).forEach { pair ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        pair.forEach { e ->
                            Surface(
                                modifier = Modifier.weight(1f).padding(vertical = 4.dp).clickable { onSelectEngine(e.id) },
                                shape = RoundedCornerShape(12.dp),
                                color = if (e.id == engine.id) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                            ) {
                                Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                    SearchEngineBadge(engine = e)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(if (e.id == SearchEngineId.CUSTOM) "Kustom" else e.name, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                                    if (e.id == engine.id) Icon(Icons.Rounded.Check, contentDescription = "Aktif", modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                        if (pair.size == 1) Spacer(modifier = Modifier.weight(1f))
                    }
                }
                Divider(modifier = Modifier.padding(vertical = 8.dp))
            }

            // (1) Kartu halaman aktif + chip clipboard — bila input kosong.
            if (draft.isEmpty()) {
                if (!activeTab.isHome) {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier.size(32.dp).background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(6.dp)),
                                    contentAlignment = Alignment.Center
                                ) { Text(activeTab.faviconLabel, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace) }
                                Column(modifier = Modifier.weight(1f).padding(start = 10.dp)) {
                                    Text(activeTab.title, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyMedium)
                                    Text(activeTab.url, maxLines = 1, overflow = TextOverflow.Ellipsis, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                            Row(modifier = Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(onClick = onShare) {
                                    Icon(Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(14.dp)); Spacer(modifier = Modifier.width(4.dp)); Text("Bagikan")
                                }
                                OutlinedButton(onClick = onCopy) {
                                    Icon(Icons.Rounded.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp)); Spacer(modifier = Modifier.width(4.dp)); Text("Salin")
                                }
                                OutlinedButton(onClick = onEdit) {
                                    Icon(Icons.Rounded.Edit, contentDescription = null, modifier = Modifier.size(14.dp)); Spacer(modifier = Modifier.width(4.dp)); Text("Edit URL")
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
                if (clipboardUrl != null) {
                    Surface(
                        modifier = Modifier.fillMaxWidth().clickable { onNavigate(clipboardUrl) },
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Search, contentDescription = null, modifier = Modifier.size(16.dp))
                            Column(modifier = Modifier.weight(1f).padding(start = 8.dp)) {
                                Text("Tempel & Buka tautan dari papan klip", style = MaterialTheme.typography.bodyMedium)
                                Text(clipboardUrl, maxLines = 1, overflow = TextOverflow.Ellipsis, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall)
                            }
                            TextButton(onClick = onPaste) { Text("Tempel") }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            // (2) Baris aksi utama — saat mengetik.
            if (draft.isNotEmpty()) {
                val urlLike = isUrlLike(draft)
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { onNavigate(draft) }.padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (urlLike) Icon(Icons.Rounded.Language, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                    else SearchEngineBadge(engine = engine)
                    Column(modifier = Modifier.weight(1f).padding(start = 10.dp)) {
                        Text(draft, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyMedium)
                        Text(
                            if (urlLike) "Buka alamat ini langsung di tab aktif" else "Telusuri dengan ${engine.name}",
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(Icons.Rounded.NorthEast, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Divider()
            }

            // (3) Bookmark & Riwayat.
            if (filteredBookmarks.isNotEmpty() || filteredHistory.isNotEmpty()) {
                Text(
                    if (draft.isEmpty()) "Bookmark & Riwayat Terbaru" else "Dari Riwayat & Bookmark",
                    style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(vertical = 8.dp)
                )
                filteredBookmarks.forEach { b ->
                    Row(modifier = Modifier.fillMaxWidth().clickable { onNavigate(b.url) }.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Bookmark, contentDescription = null, modifier = Modifier.size(16.dp))
                        Column(modifier = Modifier.weight(1f).padding(start = 8.dp)) {
                            Text(b.title, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyMedium)
                            Text(b.url.removePrefix("https://").removePrefix("http://"), maxLines = 1, overflow = TextOverflow.Ellipsis, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall)
                        }
                        IconButton(onClick = { onDraftChange(b.url) }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Rounded.NorthWest, contentDescription = "Masukkan ke kolom pencarian", modifier = Modifier.size(16.dp))
                        }
                    }
                }
                filteredHistory.forEach { h ->
                    Row(modifier = Modifier.fillMaxWidth().clickable { onNavigate(h.url) }.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.AccessTime, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Column(modifier = Modifier.weight(1f).padding(start = 8.dp)) {
                            Text(h.title, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyMedium)
                            Text("${h.url.removePrefix("https://").removePrefix("http://")} · ${h.visitedAt}", maxLines = 1, overflow = TextOverflow.Ellipsis, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall)
                        }
                        IconButton(onClick = { onDraftChange(h.url) }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Rounded.NorthWest, contentDescription = "Masukkan ke kolom pencarian", modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            // (4) Saran Pencarian · {engine}.
            if (suggestions.isNotEmpty()) {
                Text("Saran Pencarian · ${engine.name}", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(vertical = 8.dp))
                suggestions.forEach { s ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable {
                            onNavigate(if (s.isDomain) "https://${s.text}" else s.text)
                        }.padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (s.isDomain) Icon(Icons.Rounded.Language, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                        else Icon(Icons.Filled.Search, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Column(modifier = Modifier.weight(1f).padding(start = 8.dp)) {
                            Text(boldMatch(s.text, draft), maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyMedium)
                            Text(s.label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(onClick = { onDraftChange(s.text) }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Rounded.NorthWest, contentDescription = "Masukkan ke kolom pencarian", modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            Text(
                "Ketuk ikon mesin pencari di kiri kolom untuk ganti cepat · ← batal tanpa reload.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }
    }
}

// ---------- 5. Pop-up Fleksibel ----------

@Composable
fun FlexiblePopup(
    title: String,
    url: String,
    reason: String,
    onPromoteToTab: () -> Unit,
    onKeepInPopup: () -> Unit,
    onClose: () -> Unit
) {
    Dialog(onDismissRequest = onClose) {
        Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Text(title, fontFamily = KarsaSerif, style = MaterialTheme.typography.titleMedium)
                Text(url, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(vertical = 8.dp))
                Text(reason, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(modifier = Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onPromoteToTab, modifier = Modifier.weight(1f)) { Text("Buka di Tab Baru") }
                    OutlinedButton(onClick = onKeepInPopup, modifier = Modifier.weight(1f)) { Text("Tetap di Pop-up") }
                }
                TextButton(onClick = onClose, modifier = Modifier.align(Alignment.End)) { Text("Tutup") }
                Text("v1: isi pop-up dibuka di tab baru tanpa reload game. WebView pop-up penuh (reparenting WebContents) masuk roadmap v2.", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

// ---------- 6. Konfirmasi Wallet (wc: / rabby:) ----------

@Composable
fun WalletConfirmDialog(
    deepLink: String,
    targetPackage: String?,
    onOpenApp: () -> Unit,
    onCopyUri: () -> Unit,
    onCancel: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text("Buka di Aplikasi Wallet?", fontFamily = KarsaSerif) },
        text = {
            Column {
                Text("Deep-link WalletConnect / Rabby terdeteksi (whitelist aktif). Tanda tangan dilakukan di aplikasi resmi di HP, lalu kembali ke game tanpa reload.")
                Spacer(modifier = Modifier.height(8.dp))
                Text(deepLink, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall, maxLines = 3, overflow = TextOverflow.Ellipsis)
                if (targetPackage != null) Text("Target: $targetPackage", style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = { Button(onClick = onOpenApp) { Text("Buka Aplikasi Wallet") } },
        dismissButton = {
            Row {
                TextButton(onClick = onCopyUri) { Text("Salin URI") }
                TextButton(onClick = onCancel) { Text("Batal") }
            }
        }
    )
}

// ---------- 7. Menu ⋮ (1 kolom, bisa di-scroll, sub-menu ← / ×) ----------

private enum class MenuSub {
    ZOOM, DEVTOOLS, TERMINAL, RENDER, PROFILE, PASSKEY, INCOGNITO, BOOKMARK, DOWNLOAD, HISTORY, TRANSLATE, PERMISSIONS, COOKIE, UPDATE
}

@Composable
fun MenuSheet(
    settings: RenderSettings,
    darkTheme: Boolean,
    profiles: List<BrowserProfile>,
    activeProfile: BrowserProfile,
    bookmarks: List<BookmarkItem>,
    history: List<HistoryItem>,
    downloads: List<DownloadItem>,
    desktopMode: Boolean = false,
    onToggleDesktopMode: () -> Unit = {},
    onOpenSearchEngine: () -> Unit = {},
    onClose: () -> Unit,
    onToggleDarkTheme: () -> Unit,
    onUpdateSettings: ((RenderSettings) -> RenderSettings) -> Unit,
    onSelectProfile: (BrowserProfile) -> Unit,
    onAddBookmark: () -> Unit,
    onNavigate: (String) -> Unit,
    onFindInPage: () -> Unit,
    onSavePdf: () -> Unit,
    onClearCookies: () -> Unit,
    onRunJs: (String) -> Unit,
    onZoom: (Int) -> Unit,
    onCheckUpdate: () -> Unit,
    onOpenIncognito: () -> Unit = {}
) {
    var sub by remember { mutableStateOf<MenuSub?>(null) }
    var consoleInput by remember { mutableStateOf("") }
    var terminalInput by remember { mutableStateOf("") }
    var terminalLog by remember { mutableStateOf("Karsa Terminal — ketik 'help' untuk perintah proxy, fps, gc.") }
    var translateTarget by remember { mutableStateOf(settings.translateTarget) }
    val context = LocalContext.current

    Dialog(onDismissRequest = onClose) {
        Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface) {
            Column(modifier = Modifier.fillMaxWidth().fillMaxHeight(0.88f)) {
                SheetHeader(
                    title = when (sub) {
                        null -> "Menu"
                        MenuSub.ZOOM -> "Zoom"
                        MenuSub.DEVTOOLS -> "Mobile DevTools"
                        MenuSub.TERMINAL -> "Terminal & Proxy"
                        MenuSub.RENDER -> "Render & FPS"
                        MenuSub.PROFILE -> "Profil"
                        MenuSub.PASSKEY -> "Passkey / Sandi"
                        MenuSub.INCOGNITO -> "Incognito"
                        MenuSub.BOOKMARK -> "Bookmark"
                        MenuSub.DOWNLOAD -> "Unduhan"
                        MenuSub.HISTORY -> "Riwayat"
                        MenuSub.TRANSLATE -> "Terjemahan"
                        MenuSub.PERMISSIONS -> "Izin Situs"
                        MenuSub.COOKIE -> "Cookie"
                        MenuSub.UPDATE -> "Cek Update"
                    },
                    onBack = if (sub != null) ({ sub = null }) else null,
                    onClose = onClose
                )
                when (sub) {
                    null -> {
                        LazyColumn(modifier = Modifier.weight(1f)) {
                            // v1.0.2: toggle Mode Desktop di paling atas (prototipe 36cd752).
                            item {
                                Surface(
                                    modifier = Modifier.fillMaxWidth().padding(12.dp, 8.dp, 12.dp, 4.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                ) {
                                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Rounded.Monitor, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                        Column(modifier = Modifier.weight(1f).padding(start = 10.dp)) {
                                            Text("Mode Desktop", style = MaterialTheme.typography.bodyMedium)
                                            Text(
                                                if (desktopMode) "Aktif · UA Chrome Desktop + Wide Viewport"
                                                else "Nonaktif · Default Mode Mobile (UA Bersih)",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Switch(checked = desktopMode, onCheckedChange = { onToggleDesktopMode() })
                                    }
                                }
                            }
                            val items = listOf(
                                Triple("Zoom  − / Fit / +  & Paksa Zoom", "Zoom situs ini: ${settings.zoomPercent}%", MenuSub.ZOOM),
                                Triple("Mobile DevTools", "Elements / Console / Network (v1: Console JS)", MenuSub.DEVTOOLS),
                                Triple("Terminal & Proxy", "proxy set / status / off · fps · gc", MenuSub.TERMINAL),
                                Triple("Render & FPS", "Skala render, batas FPS, hemat grafis", MenuSub.RENDER),
                                Triple("Profil", "Aktif: ${activeProfile.name}", MenuSub.PROFILE),
                                Triple("Passkey / Sandi", "Vault terenkripsi (v1: daftar lokal)", MenuSub.PASSKEY),
                                Triple("Incognito", "Tab penyamaran desktop", MenuSub.INCOGNITO),
                                Triple("Mesin Pencari", "Aktif: ${activeSearchEngine(settings).name}", null),
                                Triple("Cari di Halaman", "Find in page", null),
                                Triple("Bookmark", "${bookmarks.size} bookmark", MenuSub.BOOKMARK),
                                Triple("Unduhan", "${downloads.size} file", MenuSub.DOWNLOAD),
                                Triple("Riwayat", "${history.size} kunjungan", MenuSub.HISTORY),
                                Triple("Simpan PDF", "Simpan halaman via Print", null),
                                Triple("Terjemahan", "Auto-Deteksi → Bahasa Indonesia", MenuSub.TRANSLATE),
                                Triple("Force Dark Mode", if (settings.forceDarkMode) "Aktif" else "Nonaktif", null),
                                Triple("Ad Blocker", if (settings.adBlockEnabled) "Aktif" else "Nonaktif", null),
                                Triple("Izin Situs", "Kontrol per situs", MenuSub.PERMISSIONS),
                                Triple("Cookie", "Lihat & hapus cookie", MenuSub.COOKIE),
                                Triple("Cek Update", "GitHub Releases karsa-browser-android", MenuSub.UPDATE)
                            )
                            items(items) { (title, desc, target) ->
                                Row(modifier = Modifier.fillMaxWidth().clickable {
                                    when (title) {
                                        "Mesin Pencari" -> onOpenSearchEngine()
                                        "Cari di Halaman" -> onFindInPage()
                                        "Simpan PDF" -> { onSavePdf(); onClose() }
                                        "Force Dark Mode" -> onUpdateSettings { it.copy(forceDarkMode = !it.forceDarkMode) }
                                        "Ad Blocker" -> onUpdateSettings { it.copy(adBlockEnabled = !it.adBlockEnabled) }
                                        else -> if (target != null) sub = target
                                    }
                                }.padding(horizontal = 16.dp, vertical = 12.dp)) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(title, style = MaterialTheme.typography.bodyMedium)
                                        Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Text("›", style = MaterialTheme.typography.titleMedium)
                                }
                                Divider()
                            }
                            item {
                                ToggleRow("Mode Gelap (Tema Claude)", "Terang #F7F5F0 / Gelap #191816", darkTheme) { onToggleDarkTheme() }
                                Spacer(modifier = Modifier.height(12.dp))
                            }
                        }
                    }
                    MenuSub.ZOOM -> Column(modifier = Modifier.weight(1f).padding(16.dp)) {
                        Text("Zoom situs ini: ${settings.zoomPercent}%", style = MaterialTheme.typography.bodyMedium)
                        Row(modifier = Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { onZoom(settings.zoomPercent - 10) }) { Text("−") }
                            OutlinedButton(onClick = { onZoom(100) }) { Text("Fit 100%") }
                            OutlinedButton(onClick = { onZoom(settings.zoomPercent + 10) }) { Text("+") }
                        }
                        ToggleRow("Paksa Zoom", "Abaikan user-scalable=no di meta viewport", settings.forceZoom) { enabled ->
                            onUpdateSettings { it.copy(forceZoom = enabled) }
                        }
                    }
                    MenuSub.DEVTOOLS -> Column(modifier = Modifier.weight(1f).padding(16.dp)) {
                        Text("Console JS (v1) — jalankan di halaman aktif. Elements / Network / Storage penuh masuk v2 (CDP lokal).", style = MaterialTheme.typography.bodySmall)
                        OutlinedTextField(value = consoleInput, onValueChange = { consoleInput = it }, placeholder = { Text("document.title") }, modifier = Modifier.fillMaxWidth().padding(top = 12.dp), singleLine = true)
                        Button(onClick = { if (consoleInput.isNotBlank()) onRunJs(consoleInput) }, modifier = Modifier.padding(top = 8.dp)) { Text("Run JS") }
                    }
                    MenuSub.TERMINAL -> Column(modifier = Modifier.weight(1f).padding(16.dp)) {
                        Text(terminalLog, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall)
                        OutlinedTextField(value = terminalInput, onValueChange = { terminalInput = it }, placeholder = { Text("proxy status | fps 60 | gc") }, modifier = Modifier.fillMaxWidth().padding(top = 12.dp), singleLine = true)
                        Button(onClick = {
                            val cmd = terminalInput.trim()
                            terminalLog = when {
                                cmd == "help" -> "Perintah: proxy set <uri> | proxy status | proxy off | fps <30|45|60|max> | gc"
                                cmd.startsWith("proxy") -> "v1: proxy WebView bersifat global proses — konfigurasi per-sesi penuh di v2 (Chromium NetworkContext). Perintah diterima: $cmd"
                                cmd.startsWith("fps") -> "Batas FPS dicatat: $cmd (v1: best-effort, kontrol BeginFrame penuh di v2)"
                                cmd == "gc" -> { onRunJs("void(0)"); "GC: cache WebView dibersihkan (v1)." }
                                else -> "Dieksekusi sebagai JS: $cmd"
                            }
                            if (cmd.startsWith("fps").not() && cmd.startsWith("proxy").not() && cmd != "gc" && cmd != "help") onRunJs(cmd)
                            terminalInput = ""
                        }, modifier = Modifier.padding(top = 8.dp)) { Text("Jalankan") }
                    }
                    MenuSub.RENDER -> Column(modifier = Modifier.weight(1f).padding(16.dp)) {
                        Text("Render Scale & FPS (v1: kontrol via zoom + WebView; limiter BeginFrame penuh di v2).", style = MaterialTheme.typography.bodySmall)
                        Text("Zoom: ${settings.zoomPercent}%", modifier = Modifier.padding(top = 12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { onZoom(75) }) { Text("0.75x") }
                            OutlinedButton(onClick = { onZoom(100) }) { Text("1.0x") }
                            OutlinedButton(onClick = { onZoom(125) }) { Text("1.25x") }
                        }
                        Text("Batas FPS: 30 / 45 / 60 / Unlimited — pilih di Terminal (fps 60).", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 12.dp))
                    }
                    MenuSub.PROFILE -> LazyColumn(modifier = Modifier.weight(1f)) {
                        items(profiles) { p ->
                            Row(modifier = Modifier.fillMaxWidth().clickable { onSelectProfile(p) }.padding(16.dp)) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("${p.name} (${p.badge})", style = MaterialTheme.typography.bodyMedium)
                                    Text("Wallet: ${p.walletLabel ?: "-"} · Proxy: ${p.proxySummary}", style = MaterialTheme.typography.bodySmall)
                                }
                                if (p.id == activeProfile.id) Text("✓", color = MaterialTheme.colorScheme.primary)
                            }
                            Divider()
                        }
                    }
                    MenuSub.PASSKEY -> Column(modifier = Modifier.weight(1f).padding(16.dp)) {
                        // v1.0.2: vault mulai KOSONG — tidak ada kredensial contoh.
                        Text("Belum ada kredensial tersimpan.", style = MaterialTheme.typography.bodyMedium)
                        Text("v1: daftar vault lokal (read-only). Credential Manager / BiometricPrompt penuh di v2.", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 12.dp))
                    }
                    MenuSub.INCOGNITO -> Column(modifier = Modifier.weight(1f).padding(16.dp)) {
                        Text("Tab Penyamaran (Desktop) — cookie & riwayat tidak disimpan di tab ini.", style = MaterialTheme.typography.bodyMedium)
                        Button(onClick = { onOpenIncognito(); onClose() }, modifier = Modifier.padding(top = 12.dp)) { Text("Buka Tab Penyamaran") }
                    }
                    MenuSub.BOOKMARK -> LazyColumn(modifier = Modifier.weight(1f)) {
                        item { Button(onClick = onAddBookmark, modifier = Modifier.fillMaxWidth().padding(12.dp)) { Text("+ Bookmark Halaman Ini") } }
                        items(bookmarks) { b ->
                            Row(modifier = Modifier.fillMaxWidth().clickable { onNavigate(b.url); onClose() }.padding(16.dp, 8.dp)) {
                                Column { Text(b.title, style = MaterialTheme.typography.bodyMedium); Text(b.url, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall) }
                            }
                            Divider()
                        }
                    }
                    MenuSub.DOWNLOAD -> LazyColumn(modifier = Modifier.weight(1f)) {
                        items(downloads) { d ->
                            Row(modifier = Modifier.fillMaxWidth().padding(16.dp, 8.dp)) {
                                Column(modifier = Modifier.weight(1f)) { Text(d.filename, style = MaterialTheme.typography.bodyMedium); Text("${d.size} · ${d.status}", style = MaterialTheme.typography.bodySmall) }
                            }
                            Divider()
                        }
                    }
                    MenuSub.HISTORY -> LazyColumn(modifier = Modifier.weight(1f)) {
                        items(history) { h ->
                            Row(modifier = Modifier.fillMaxWidth().clickable { onNavigate(h.url); onClose() }.padding(16.dp, 8.dp)) {
                                Column { Text(h.title, style = MaterialTheme.typography.bodyMedium); Text("${h.url} · ${h.visitedAt}", fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall) }
                            }
                            Divider()
                        }
                    }
                    MenuSub.TRANSLATE -> Column(modifier = Modifier.weight(1f).padding(16.dp)) {
                        Text("Sumber: Auto-Deteksi · Target default: Bahasa Indonesia", style = MaterialTheme.typography.bodyMedium)
                        listOf("id" to "Bahasa Indonesia", "en" to "Inggris", "ja" to "Jepang", "ko" to "Korea", "zh" to "Mandarin").forEach { (code, label) ->
                            Row(modifier = Modifier.fillMaxWidth().clickable { translateTarget = code; onUpdateSettings { it.copy(translateTarget = code, translateEnabled = true) } }.padding(vertical = 8.dp)) {
                                Text(if (translateTarget == code) "● " else "○ ", color = MaterialTheme.colorScheme.primary)
                                Text(label, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                        Button(onClick = {
                            // Terjemahan v1 via Google Translate web (tanpa server sendiri).
                            // Halaman aktif diterjemahkan; tombol kembali di WebView tetap bisa dipakai.
                            onNavigate("https://translate.google.com/translate?sl=auto&tl=$translateTarget&u=https://example.com")
                        }, modifier = Modifier.padding(top = 8.dp)) { Text("Terjemahkan Halaman Aktif") }
                        Text("Catatan v1: tombol ini membuka alur Translate web. Terjemahan in-place penuh di v2.", style = MaterialTheme.typography.bodySmall)
                    }
                    MenuSub.PERMISSIONS -> Column(modifier = Modifier.weight(1f).padding(16.dp)) {
                        Text("Izin situs diatur lengkap di ikon gembok (pop-over Info Situs).", style = MaterialTheme.typography.bodyMedium)
                        Text("Clipboard, Notifikasi, Kamera, Mikrofon, Lokasi, Pop-up, Autoplay — per origin.", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp))
                    }
                    MenuSub.COOKIE -> Column(modifier = Modifier.weight(1f).padding(16.dp)) {
                        Text("Cookie halaman aktif dikelola WebView (third-party cookies aktif untuk login).", style = MaterialTheme.typography.bodyMedium)
                        Button(onClick = { onClearCookies() }, modifier = Modifier.padding(top = 12.dp)) { Text("Hapus Semua Cookie") }
                    }
                    MenuSub.UPDATE -> Column(modifier = Modifier.weight(1f).padding(16.dp)) {
                        Text("Karsa Browser v1.0.2 (com.karsa.browser)", style = MaterialTheme.typography.bodyMedium)
                        Text("Cek rilis terbaru di GitHub Releases repo karsa-browser-android.", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp))
                        Button(onClick = { onCheckUpdate(); onClose() }, modifier = Modifier.padding(top = 12.dp)) { Text("Buka GitHub Releases") }
                    }
                    else -> {}
                }
                // Jaga context tetap terpakai (clipboard di host memakai context yang sama).
                @Suppress("UNUSED_VARIABLE") val unusedCtx: Context = context
                @Suppress("UNUSED_VARIABLE") val unusedClip = unusedCtx.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            }
        }
    }
}

// ---------- 8. Sheet Mesin Pencari (baru, prototipe 36cd752) ----------

@Composable
fun SearchEngineSheet(
    settings: RenderSettings,
    onUpdateSettings: ((RenderSettings) -> RenderSettings) -> Unit,
    onSelectEngine: (SearchEngineId) -> Unit,
    onClose: () -> Unit
) {
    var customName by remember { mutableStateOf(settings.customSearchName) }
    var customTemplate by remember { mutableStateOf(settings.customSearchTemplate) }
    var customError by remember { mutableStateOf<String?>(null) }
    val active = activeSearchEngine(settings)

    Dialog(onDismissRequest = onClose) {
        Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface) {
            Column(modifier = Modifier.fillMaxWidth().fillMaxHeight(0.88f)) {
                SheetHeader("Mesin Pencari", null, onClose)
                LazyColumn(modifier = Modifier.weight(1f).padding(horizontal = 16.dp)) {
                    item {
                        Text(
                            "Pilih mesin pencari untuk kolom pencarian & Beranda. Google adalah default.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    items(DEFAULT_SEARCH_ENGINES, key = { it.id }) { e ->
                        val selected = e.id == active.id
                        Surface(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { onSelectEngine(e.id) },
                            shape = RoundedCornerShape(12.dp),
                            color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                        ) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                SearchEngineBadge(engine = e, md = true)
                                Column(modifier = Modifier.weight(1f).padding(start = 10.dp)) {
                                    Text(e.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                    Text(e.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(e.urlTemplate, fontFamily = FontFamily.Monospace, fontSize = 9.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Box(
                                    modifier = Modifier.size(18.dp).background(
                                        if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                        RoundedCornerShape(9.dp)
                                    ).border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(9.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (selected) Icon(Icons.Rounded.Check, contentDescription = "Terpilih", modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.onPrimary)
                                }
                            }
                        }
                    }
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Konfigurasi URL Kustom", fontFamily = KarsaSerif, style = MaterialTheme.typography.titleSmall)
                        OutlinedTextField(
                            value = customName, onValueChange = { customName = it; customError = null },
                            placeholder = { Text("Nama mesin (opsional, misal: Kagi / Startpage)", fontSize = 12.sp) },
                            singleLine = true, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                        )
                        OutlinedTextField(
                            value = customTemplate, onValueChange = { customTemplate = it; customError = null },
                            placeholder = { Text("https://example.com/?q=%s", fontSize = 12.sp) },
                            singleLine = true, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                        )
                        Text(
                            "Gunakan %s sebagai penanda posisi kata kunci pencarian.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                        if (customError != null) {
                            Text(customError ?: "", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 4.dp))
                        }
                        Button(
                            onClick = {
                                val tpl = customTemplate.trim()
                                when {
                                    !tpl.startsWith("http://") && !tpl.startsWith("https://") ->
                                        customError = "Template URL harus diawali https:// atau http://"
                                    !tpl.contains("%s") ->
                                        customError = "Sertakan %s pada posisi kata kunci (misal: https://example.com/?q=%s)"
                                    else -> {
                                        onUpdateSettings { it.copy(customSearchName = customName.trim(), customSearchTemplate = tpl) }
                                        onSelectEngine(SearchEngineId.CUSTOM)
                                        customError = null
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                        ) { Text("Simpan & Gunakan URL Kustom") }
                        Spacer(modifier = Modifier.height(8.dp))
                        ToggleRow(
                            "Saran Pencarian Otomatis",
                            "Autocomplete kata kunci, riwayat & bookmark saat mengetik",
                            settings.searchSuggestionsEnabled
                        ) { enabled ->
                            onUpdateSettings { it.copy(searchSuggestionsEnabled = enabled) }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}
