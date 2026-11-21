package com.karsa.browser.userscript

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File

/**
 * Userscript v1 (.user.js / .js) — pengelola di ikon 🧩.
 * Arsitektur modular untuk v2 (ekstensi .crx / Chrome Web Store):
 *  - v1: baca metadata ==UserScript==, simpan file ke filesDir/userscripts/,
 *    injeksi document-start di WebView (lihat WebViewFactory).
 *  - v2: antarmuka yang sama (id/name/enabled/code) dipakai ulang oleh
 *    loader .crx — tanpa mengubah UI popover.
 */
data class UserScript(
    val id: String,
    val name: String,
    val version: String = "1.0.0",
    val description: String = "",
    val matches: List<String> = listOf("*://*/*"),
    val runAt: String = "document-start",
    val enabled: Boolean = true,
    val fileName: String,
    val code: String
)

class UserScriptManager(private val context: Context) {
    private val dir: File get() = File(context.filesDir, "userscripts").also { if (!it.exists()) it.mkdirs() }

    suspend fun list(): List<UserScript> = withContext(Dispatchers.IO) {
        dir.listFiles()?.filter { it.isFile && it.name.endsWith(".js", ignoreCase = true) }
            ?.sortedBy { it.name }
            ?.mapNotNull { parse(it) } ?: emptyList()
    }

    suspend fun install(fileName: String, code: String): UserScript? = withContext(Dispatchers.IO) {
        val safe = fileName.replace(Regex("[^A-Za-z0-9._-]"), "_").take(120)
            .let { if (it.endsWith(".js", true)) it else "$it.user.js" }
        val f = File(dir, safe)
        f.writeText(code)
        parse(f)
    }

    suspend fun remove(id: String): Boolean = withContext(Dispatchers.IO) {
        dir.listFiles()?.firstOrNull { it.nameWithoutExtension == id || it.name == id }?.delete() ?: false
    }

    suspend fun setEnabled(id: String, enabled: Boolean) = withContext(Dispatchers.IO) {
        // v1: enabled disimpan di sidecar .json per file agar file .js tetap murni.
        val meta = File(dir, "$id.enabled")
        if (enabled) meta.delete() else meta.writeText("disabled")
    }

    suspend fun isEnabled(id: String): Boolean = withContext(Dispatchers.IO) {
        !File(dir, "$id.enabled").exists()
    }

    /** Gabungkan semua script aktif yang match URL -> satu payload injeksi. */
    fun combinedCodeFor(url: String, scripts: List<UserScript>): String {
        val matched = scripts.filter { it.enabled && matches(it.matches, url) }
        if (matched.isEmpty()) return ""
        return buildString {
            append("(function(){try{")
            matched.forEach { s ->
                append("\n/* == Karsa UserScript: ${s.name} v${s.version} == */\n")
                append(s.code)
                append("\n")
            }
            append("}catch(e){console.warn('[Karsa userscript]',e)}})();")
        }
    }

    private fun matches(patterns: List<String>, url: String): Boolean {
        if (patterns.isEmpty()) return true
        return patterns.any { p ->
            p == "*://*/*" || p == "*" || url.startsWith(p.removeSuffix("*")) ||
                Regex(p.replace(".", "\\.").replace("*", ".*")).matches(url)
        }
    }

    private fun parse(file: File): UserScript? = runCatching {
        val code = file.readText()
        val header = Regex("==UserScript==([\\s\\S]*?)==/UserScript==").find(code)?.groupValues?.getOrNull(1) ?: ""
        fun meta(key: String) = Regex("@$key\\s+(.+)").find(header)?.groupValues?.getOrNull(1)?.trim()
        val id = file.nameWithoutExtension
        UserScript(
            id = id,
            name = meta("name") ?: id.replace("-", " ").replace("_", " ")
                .split(" ").joinToString(" ") { w -> w.replaceFirstChar { it.uppercase() } },
            version = meta("version") ?: "1.0.0",
            description = meta("description") ?: "UserScript lokal (${file.name})",
            matches = Regex("@match\\s+(.+)").findAll(header).map { it.groupValues[1].trim() }.toList()
                .ifEmpty { listOf("*://*/*") },
            runAt = meta("run-at") ?: "document-start",
            enabled = !File(dir, "$id.enabled").exists(),
            fileName = file.name,
            code = code
        )
    }.getOrNull()

    /** Contoh bawaan (opsional, hanya dibuat saat pertama kali, bisa dihapus user). */
    suspend fun ensureSample() = withContext(Dispatchers.IO) {
        if (dir.listFiles()?.isEmpty() != false) {
            File(dir, "karsa-sample.user.js").writeText(
                """
                // ==UserScript==
                // @name        Karsa Sample — Judul Halaman
                // @version     1.0.0
                // @description Contoh userscript v1: log judul halaman ke console.
                // @match       *://*/*
                // @run-at      document-end
                // ==/UserScript==
                console.log('[Karsa] halaman dimuat:', document.title);
                """.trimIndent()
            )
        }
    }
}
