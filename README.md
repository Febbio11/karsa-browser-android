# Karsa Browser v1.0.2 — MVP Android Native

**`com.karsa.browser` · Kotlin + Jetpack Compose · Min SDK 26 (Android 8.0+) · versionCode 3**

Browser Android dari prototipe
[`Febbio11/karsa-browser-prototype`](https://github.com/Febbio11/karsa-browser-prototype)
(gaya Claude AI: terang `#F7F5F0` / gelap `#191816`, aksen `#C86442`, serif editorial +
Plus Jakarta Sans + JetBrains Mono).

> **v1.0.2 (22 Nov 2026, porting prototipe commit `36cd752`):** Bar atas jadi 3 elemen
> + Bottom Bar 5 menu, semua ikon vektor monokrom, Home orisinal Karsa sebagai
> tampilan awal (state 100% fresh, tanpa data dummy), **Mode Mobile default**,
> multi mesin pencari + autocomplete lokal.

> **Engine v1: Android System WebView yang dimodifikasi maksimal — BUKAN kompilasi
> Chromium dari nol.** Full Chromium fork (Cromite/Ungoogled + patch ekstensi)
> adalah roadmap Fase 3 di prototipe, dan tidak realistis di VM 6 GB / 1,5 CPU
> (cek 21 Nov 2026: RAM available 620 MB, VM bisa di-replace tiap jam).
> Build berat dilakukan di **GitHub Actions**, bukan di VM ini.

## Yang ada di v1.0.2

| Area | Isi |
|---|---|
| **Bar atas (1 baris, 3 elemen)** | `[Pill URL lebar]` → `[Ekstensi]` → `[Full Layar]` — `statusBarsPadding()` agar tidak ketutup status bar Android. Semua ikon vektor monokrom (Material Rounded), tanpa emoji |
| **Bottom Bar (5 menu)** | `[Home]` → `[Bookmark 1-tap]` → `[+ Tambah Tab]` → `[Kotak Tab berangka]` → `[⋮ Menu]` — `navigationBarsPadding()` agar duduk di atas gesture bar. Bookmark 1-tap: hitam solid saat tersimpan + toast mengambang (auto-hide 2,5 dtk); nonaktif di Home |
| **Home orisinal Karsa** | `karsa://home` sebagai tampilan awal: wordmark serif "Karsa", subtitle Mode Mobile, pill pencarian (badge engine), grid Pintasan Cepat 4 kolom (Google / X / YouTube, bisa tambah/hapus, tersimpan di DataStore). State awal 100% fresh — 0 tab dummy, 0 bookmark/riwayat/unduhan/ekstensi/userscript |
| **Search/URL** | Ketuk = otomatis kosong, badge mesin pencari + picker cepat di overlay, tombol Tempel cepat, kartu halaman aktif (Bagikan / Salin / Edit URL), chip clipboard, `←` / tap luar = batal **tanpa reload** |
| **Multi Mesin Pencari** | Google (default), Brave, DuckDuckGo, Bing, Yandex, Kustom (template URL `%s` + validasi). Dipilih dari badge di kolom search, picker overlay, atau sheet di menu ⋮ |
| **Saran Pencarian (Autocomplete)** | 100% lokal, tanpa API: domain dikenal (10), topik (4 grup), sufiks dinamis, bookmark & riwayat. Maks 5 saran, bagian cocok dicetak tebal, tombol isi-kolom per baris. Bisa dimatikan di sheet Mesin Pencari |
| **Mode Mobile (default)** | UA Chrome Mobile bersih Pixel 8 (tanpa `; wv`) agar Google & YouTube tampil normal. Toggle **Mode Desktop** per tab di menu ⋮ (paling atas) — UA Chrome Desktop + wide viewport + reload. Ingat-per-situs via checkbox di Info Situs |
| **Info Situs (gembok)** | SSL, Hapus Cache situs ini, Reset Izin, toggle izin cepat, toggle Anti-Redirect & Matikan Pull-to-Refresh per situs, checkbox "Ingat Mode Desktop untuk situs ini", info Profil/Proxy |
| **Ekstensi** | Hanya di bar atas (toggle langsung, tanpa `←`/`×`). v1 = pengelola **Userscript (`.user.js`/`.js`)** + file picker Android. Arsitektur modular untuk v2 (`.crx` / Web Store) |
| **Full Layar** | Hanya di bar atas. Vertikal & Horizontal + kunci orientasi manual + tombol keluar melayang tanpa reload (`configChanges` di Manifest) |
| **⋮ Menu (1 kolom, scroll)** | Mode Desktop (paling atas), Zoom `-`/`Fit`/`+` & Paksa Zoom, Mobile DevTools (Console JS v1), Terminal & Proxy, Render & FPS, Profil, Passkey/Sandi, Incognito, Mesin Pencari, Cari di Halaman, Bookmark, Unduhan, Riwayat, Simpan PDF, Terjemahan Auto → Bahasa Indonesia, Force Dark Mode, Ad Blocker, Izin Situs, Cookie, Cek Update GitHub Releases. Tiap sub-menu punya `←` Kembali dan `×` Tutup |
| **WebView v1.0.2** | **Mode Mobile default** (UA Pixel 8 Chrome 131, hapus `; wv`), Mode Desktop = UA Chrome 131 Windows + wide viewport, `setSupportMultipleWindows(true)`, `javaScriptCanOpenWindowsAutomatically=true`, `domStorageEnabled=true`, `setAcceptThirdPartyCookies(webView, true)` |
| **Google (22 Nov 2026)** | Mesin pencari default = **Google Search** (`https://www.google.com/search?q=...`) untuk kata kunci biasa di kolom search (`Models.kt: resolveNavigationInput`). Login dengan Google: selain UA bersih, header `X-Requested-With` dihapus via `WebSettingsCompat.setRequestedWithHeaderOriginAllowList(settings, emptySet())` di WebView utama + pop-up (`WebViewFactory.kt: GoogleOAuthCompat`, dijaga `WebViewFeature.isFeatureSupported`) agar tidak kena `403 disallowed_useragent` |
| **Anti-Redirect Pintar** | Blokir total `twitter://`, `x://`, `discord://`, `tg://`, `intent://` → Pop-up Fleksibel (opsi Buka di Tab Baru / Tetap di Pop-up). **Whitelist khusus:** `wc:` (WalletConnect) & `rabby:` (Rabby Wallet Mobile) lewat pop-up konfirmasi → tanda tangan di aplikasi resmi → kembali tanpa reload |

## Struktur

```
karsa-browser-android/
├── .github/workflows/build-apk.yml   # Build APK di Actions (WAJIB)
├── app/
│   ├── build.gradle.kts              # AGP 8.5.2, Kotlin 2.0.21, Compose BOM 2024.09.00, versionCode 3 / 1.0.2
│   └── src/main/
│       ├── AndroidManifest.xml       # configChanges agar Full Layar tanpa reload
│       └── java/com/karsa/browser/
│           ├── MainActivity.kt
│           ├── browser/Models.kt / NavigationGuard.kt / WebViewFactory.kt
│           ├── userscript/UserScriptManager.kt
│           └── ui/BrowserScreen.kt / SettingsStore.kt
│               ├── theme/Theme.kt    # Palet + tipografi prototipe
│               └── components/Sheets.kt  # SiteInfo, Extensions, Menu, Tab, Pop-up, Wallet, AddressOverlay, SearchEngineSheet
│               └── components/KarsaHomePage.kt / SearchEngineBadge.kt  # Home orisinal + badge engine
└── gradle/wrapper/ + gradlew
```

## Push ke GitHub & unduh APK (langkah singkat)

```bash
# 1. Buat repo baru di GitHub: karsa-browser-android (kosong, tanpa README)
# 2. Dari folder proyek ini:
git init
git add .
git commit -m "Karsa Browser v1 MVP — WebView + Compose"
git branch -M main
git remote add origin https://github.com/Febbio11/karsa-browser-android.git
git push -u origin main
# 3. Buka repo > tab Actions > workflow "Build APK" > tunggu hijau (±5-10 menit)
# 4. Di halaman run itu, unduh di bagian Artifacts:
#    karsa-browser-debug-apk (atau -release-apk kalau signing sudah diisi)
# 5. Install APK di HP (izinkan "Install unknown apps" sekali saja)
```

### Release signing (opsional, kalau mau APK release bertanda tangan)

1. Buat keystore lokal sekali saja:
   ```bash
   keytool -genkey -v -keystore karsa-release.jks -alias karsa -keyalg RSA -keysize 2048 -validity 10000
   base64 -w0 karsa-release.jks   # salin outputnya
   ```
2. Di repo GitHub → **Settings → Secrets and variables → Actions → New repository secret**:
   - `ANDROID_KEYSTORE_BASE64` = output base64 di atas
   - `KEYSTORE_PASSWORD` = password keystore
   - `KEY_ALIAS` = `karsa`
   - `KEY_PASSWORD` = password key
3. Push lagi ke `main` — workflow otomatis build `assembleRelease` dan upload
   `karsa-browser-release-apk`. Kalau secret belum diisi, workflow fallback
   ke `assembleDebug` dan tetap sukses.

## Batasan v1 (jujur)

- Ekstensi Chrome `.crx` / Web Store **belum** native — v1 hanya Userscript.
  Butuh Chromium fork untuk MV3 penuh (roadmap prototipe Fase 3).
- DevTools v1 hanya Console JS + Terminal. Elements/Network/Storage CDP penuh
  butuh DevToolsAgentHost lokal (Fase 3).
- Proxy per-sesi & FPS limiter BeginFrame: v1 best-effort di WebView;
  kontrol penuh butuh NetworkContext Chromium.
- Terjemahan v1 lewat alur Google Translate web; in-place penuh di v2.
- Pop-up Fleksibel v1: isi dibuka via "Buka di Tab Baru" tanpa reload;
  reparenting WebContents penuh di v2.
