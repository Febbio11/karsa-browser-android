// Top-level build file — Karsa Browser v1 MVP
// Kotlin 2.0.21 (bukan 1.9.24): plugin Compose Compiler
// 'org.jetbrains.kotlin.plugin.compose' BARU diterbitkan mulai Kotlin 2.0 —
// marker 1.9.24 TIDAK ADA di Maven Central (cek live: 404), marker 2.0.21 ada
// (200). Run #2 gagal di sini: "Plugin [id: 'org.jetbrains.kotlin.plugin.compose',
// version: '1.9.24'] was not found". AGP 8.5.2 + Compose BOM 2024.09.00
// kompatibel dengan Kotlin 2.0.x.
plugins {
    id("com.android.application") version "8.5.2" apply false
    id("org.jetbrains.kotlin.android") version "2.0.21" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.21" apply false
}
