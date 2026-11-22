package com.karsa.browser.browser

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Environment
import android.provider.Settings
import androidx.core.content.FileProvider
import androidx.core.content.PackageInfoCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * Update otomatis Karsa Browser (v1.0.3).
 *
 * Cara kerja:
 * 1. Workflow GitHub Actions tiap push ke main meng-upload APK + `update.json`
 *    ke rolling release tag `latest` (lihat .github/workflows/build-apk.yml).
 *    `update.json` berisi {"versionCode": N, "versionName": "x.y.z", "apkUrl": "..."}.
 * 2. Aplikasi mengambil update.json (publik, tanpa auth) saat dibuka dan saat
 *    pengguna mengetuk "Cek Update" di menu ⋮. Bila versionCode remote >
 *    versionCode terpasang -> dialog "Update tersedia".
 * 3. "Update Sekarang" -> DownloadManager mengunduh APK ke folder Download
 *    internal aplikasi; saat selesai, installer sistem dibuka via FileProvider.
 *    Izin "Install unknown apps" diminta sekali saja (Android 8+).
 *
 * Catatan: update otomatis mulai bekerja dari versi BERIKUTNYA setelah
 * workflow Release diaktifkan — 1.0.3 sendiri masih diunduh manual, karena
 * release `latest` + update.json baru ada setelah workflow baru jalan.
 */
object AppUpdater {
    const val UPDATE_JSON_URL =
        "https://github.com/Febbio11/karsa-browser-android/releases/download/latest/update.json"
    const val RELEASES_URL =
        "https://github.com/Febbio11/karsa-browser-android/releases/latest"
    const val APK_FILE_NAME = "karsa-browser-debug.apk"

    data class UpdateInfo(val versionCode: Long, val versionName: String, val apkUrl: String)

    fun installedVersionCode(context: Context): Long = runCatching {
        val info = context.packageManager.getPackageInfo(context.packageName, 0)
        PackageInfoCompat.getLongVersionCode(info)
    }.getOrElse { 0L }

    /** null = sudah terbaru / tidak bisa memeriksa (offline / release belum ada). */
    suspend fun checkForUpdate(context: Context): UpdateInfo? = withContext(Dispatchers.IO) {
        runCatching {
            val conn = (URL(UPDATE_JSON_URL).openConnection() as HttpURLConnection).apply {
                connectTimeout = 10_000
                readTimeout = 10_000
                setRequestProperty("User-Agent", "KarsaBrowser-Updater")
            }
            if (conn.responseCode != HttpURLConnection.HTTP_OK) return@runCatching null
            val json = conn.inputStream.bufferedReader().readText()
            val code = Regex(""""versionCode"\s*:\s*(\d+)""")
                .find(json)?.groupValues?.get(1)?.toLongOrNull() ?: return@runCatching null
            val name = Regex(""""versionName"\s*:\s*"([^"]+)"""")
                .find(json)?.groupValues?.get(1).orEmpty()
            val apk = Regex(""""apkUrl"\s*:\s*"([^"]+)"""")
                .find(json)?.groupValues?.get(1) ?: return@runCatching null
            if (code > installedVersionCode(context)) UpdateInfo(code, name, apk) else null
        }.getOrNull()
    }

    /**
     * Unduh APK lalu pasang. Bila izin install dari sumber tak dikenal belum
     * diberikan, buka halaman Settings itu dulu (sekali saja) dan kembalikan
     * false — pengguna mengetuk "Update Sekarang" lagi setelah mengizinkan.
     */
    fun downloadAndInstall(context: Context, info: UpdateInfo, onDownloadStarted: () -> Unit): Boolean {
        if (!context.packageManager.canRequestPackageInstalls()) {
            val intent = Intent(
                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:${context.packageName}")
            )
            context.startActivity(intent)
            return false
        }
        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val req = DownloadManager.Request(Uri.parse(info.apkUrl))
            .setTitle("Karsa Browser ${info.versionName}")
            .setDescription("Mengunduh update...")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, APK_FILE_NAME)
            .setAllowedOverMetered(true)
        val id = dm.enqueue(req)
        onDownloadStarted()
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context, intent: Intent) {
                val doneId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                if (doneId != id) return
                c.unregisterReceiver(this)
                val file = File(
                    c.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
                    APK_FILE_NAME
                )
                installApk(c, file)
            }
        }
        context.registerReceiver(receiver, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
        return true
    }

    fun installApk(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(
            context, "${context.packageName}.fileprovider", file
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(intent)
    }
}
