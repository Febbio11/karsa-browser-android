package com.karsa.browser.browser

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature
import com.karsa.browser.userscript.UserScriptManager

/**
 * Engine Web v1: Android System WebView yang dimodifikasi maksimal.
 * JANGAN kompilasi Chromium dari nol di VM (lihat README) — v1 ini
 * adalah jalur yang disepakati Febbio untuk MVP.
 *
 * Wajib (spesifikasi):
 * - Desktop User-Agent Chrome terbaru yang bersih (hapus "; wv")
 * - setSupportMultipleWindows(true)
 * - javaScriptCanOpenWindowsAutomatically = true
 * - domStorageEnabled = true
 * - setAcceptThirdPartyCookies(webView, true)
 * - Google OAuth (tambahan 22 Nov 2026): hapus header X-Requested-With
 *   via WebSettingsCompat.setRequestedWithHeaderOriginAllowList(..., emptySet())
 *   agar Google tidak mendeteksi WebView dan melempar 403 disallowed_useragent.
 *   Dua lapis ini saling melengkapi: UA bersih saja TIDAK cukup, karena
 *   WebView tetap mengirim X-Requested-With: <package name> secara default.
 */
object DesktopUserAgent {
    // Chrome 131 desktop (sesuai prototipe: "Chrome/131.0 Windows NT 10.0").
    // Tanpa "; wv" agar login X / Discord / Google tidak mendeteksi WebView.
    const val DESKTOP =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"
}

/**
 * Kompatibilitas Login dengan Google (Google OAuth) — anti 403 disallowed_useragent.
 *
 * Google memblokir WebView lewat DUA sinyal: (1) "; wv" di User-Agent dan
 * (2) header `X-Requested-With: com.karsa.browser` yang dikirim WebView
 * secara default. Menghapus (1) saja tidak cukup.
 *
 * `setRequestedWithHeaderOriginAllowList(settings, emptySet())` = header
 * X-Requested-With TIDAK dikirim ke origin mana pun, jadi Google melihat
 * request ini sebagai Chrome desktop biasa.
 * Dijaga dengan WebViewFeature.isFeatureSupported(...) agar WebView lama
 * (sebelum WebKit 1.7) tidak crash — di situ fallback-nya hanya UA bersih.
 * Dipakai di WebView utama DAN WebView pop-up (onCreateWindow), karena
 * alur OAuth Google sering lewat window.open().
 */
object GoogleOAuthCompat {
    fun apply(settings: WebSettings) {
        if (WebViewFeature.isFeatureSupported(WebViewFeature.REQUESTED_WITH_HEADER_ALLOW_LIST)) {
            WebSettingsCompat.setRequestedWithHeaderOriginAllowList(settings, emptySet())
        }
    }
}

class KarsaWebViewCallbacks(
    val onPageStarted: (String) -> Unit = {},
    val onPageFinished: (String, String) -> Unit = { _, _ -> },
    val onProgress: (Int) -> Unit = {},
    val onPopupRequest: (String) -> Unit = {},
    val onWalletDeepLink: (String, String?) -> Unit = { _, _ -> },
    val onBlockedSocial: (String, String) -> Unit = { _, _ -> }
)

@SuppressLint("SetJavaScriptEnabled")
@Suppress("DEPRECATION")
fun createKarsaWebView(
    context: Context,
    guard: NavigationGuard,
    settings: RenderSettings,
    userScripts: List<com.karsa.browser.userscript.UserScript>,
    scriptManager: UserScriptManager?,
    callbacks: KarsaWebViewCallbacks = KarsaWebViewCallbacks()
): WebView {
    val webView = WebView(context)
    webView.layoutParams = ViewGroup.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
    )

    val ws: WebSettings = webView.settings
    ws.javaScriptEnabled = true
    ws.domStorageEnabled = true
    ws.databaseEnabled = true
    ws.javaScriptCanOpenWindowsAutomatically = true
    ws.setSupportMultipleWindows(true)
    ws.userAgentString = DesktopUserAgent.DESKTOP
    // Google OAuth: hapus X-Requested-With (lihat GoogleOAuthCompat di atas).
    GoogleOAuthCompat.apply(ws)
    ws.loadWithOverviewMode = true
    ws.useWideViewPort = true
    ws.builtInZoomControls = true
    ws.displayZoomControls = false
    // Paksa Zoom v1: abaikan user-scalable=no di meta viewport (best-effort di WebView).
    ws.setSupportZoom(settings.forceZoom)
    ws.textZoom = settings.zoomPercent
    ws.mediaPlaybackRequiresUserGesture = false
    ws.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
    ws.cacheMode = WebSettings.LOAD_DEFAULT
    ws.allowFileAccess = false
    ws.allowContentAccess = true

    // Cookie pihak ketiga: wajib agar login Google / X / Discord lancar.
    CookieManager.getInstance().setAcceptCookie(true)
    CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true)

    // Force Dark v1 (WebView): pakai FORCE_DARK_AUTO di Android 10+ bila aktif.
    if (settings.forceDarkMode) {
        ws.forceDark = WebSettings.FORCE_DARK_ON
    }

    webView.webViewClient = object : WebViewClient() {
        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
            val url = request.url.toString()
            return when (val d = guard.intercept(url, isPopupRequest = false)) {
                is NavigationDecision.AllowInCurrentTab -> false // biarkan WebView yang load
                is NavigationDecision.OpenInFlexiblePopup -> {
                    callbacks.onBlockedSocial(d.resolvedUrl, d.reason)
                    true
                }
                is NavigationDecision.AskExternalWallet -> {
                    callbacks.onWalletDeepLink(d.deepLinkUri, d.targetPackage)
                    true
                }
                is NavigationDecision.BlockSilently -> true
            }
        }

        override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
            super.onPageStarted(view, url, favicon)
            callbacks.onPageStarted(url)
        }

        override fun onPageFinished(view: WebView, url: String) {
            super.onPageFinished(view, url)
            callbacks.onPageFinished(url, view.title ?: "")
            // Injeksi userscript aktif (document-end di v1; document-start
            // penuh butuh WebViewAssetLoader / evaluateJavascript di onPageStarted
            // untuk v2 — payload tetap sama, hanya timing yang disempurnakan).
            val code = scriptManager?.combinedCodeFor(url, userScripts).orEmpty()
            if (code.isNotEmpty()) view.evaluateJavascript(code, null)
            // Desktop spoof ringan (v1): hapus jejak mobile di navigator.
            view.evaluateJavascript(
                "(function(){try{Object.defineProperty(Navigator.prototype,'platform',{get:function(){return 'Win32'}});Object.defineProperty(Navigator.prototype,'maxTouchPoints',{get:function(){return 0}});}catch(e){}})();",
                null
            )
            // Ad-blocker v1 (ringan, berbasis CSS): sembunyikan slot iklan umum.
            if (settings.adBlockEnabled) {
                view.evaluateJavascript(
                    "(function(){var s=document.createElement('style');s.textContent='[class*=ads],[id*=ads],iframe[src*=doubleclick]{display:none!important}';document.documentElement.appendChild(s);})();",
                    null
                )
            }
        }
    }

    webView.webChromeClient = object : WebChromeClient() {
        override fun onProgressChanged(view: WebView, newProgress: Int) {
            callbacks.onProgress(newProgress)
        }

        // window.open() / target=_blank -> Pop-up Fleksibel, bukan tab baru paksa.
        override fun onCreateWindow(view: WebView, isDialog: Boolean, isUserGesture: Boolean, resultMsg: android.os.Message): Boolean {
            val transport = resultMsg.obj as? WebView.WebViewTransport ?: return false
            val popupView = WebView(view.context)
            popupView.settings.javaScriptEnabled = true
            popupView.settings.domStorageEnabled = true
            popupView.settings.javaScriptCanOpenWindowsAutomatically = true
            popupView.settings.setSupportMultipleWindows(true)
            popupView.settings.userAgentString = DesktopUserAgent.DESKTOP
            // Pop-up OAuth (Google / X / Discord) juga wajib tanpa X-Requested-With.
            GoogleOAuthCompat.apply(popupView.settings)
            CookieManager.getInstance().setAcceptThirdPartyCookies(popupView, true)
            transport.webView = popupView
            resultMsg.sendToTarget()
            // URL pop-up dibaca setelah load; host (BrowserScreen) menampilkan
            // FlexiblePopup saat onCreateWindow terpanggil via callback ini.
            callbacks.onPopupRequest(popupView.url ?: "about:blank")
            return true
        }
    }
    return webView
}
