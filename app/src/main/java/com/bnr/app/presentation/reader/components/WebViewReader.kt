package com.bnr.app.presentation.reader.components

import android.webkit.CookieManager
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.bnr.app.core.network.HostCookieJar
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

/**
 * Renders the raw chapter URL in a WebView.
 * Syncs cookies from [HostCookieJar] so CF-protected sites work correctly.
 */
@Composable
fun WebViewReader(
    url: String,
    cookieJar: HostCookieJar,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Sync OkHttp cookies into Android's CookieManager
    syncCookies(url, cookieJar)

    AndroidView(
        factory = {
            WebView(context).apply {
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    loadWithOverviewMode = true
                    useWideViewPort = true
                    setSupportZoom(true)
                    builtInZoomControls = true
                    displayZoomControls = false
                    mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                }
                CookieManager.getInstance().setAcceptCookie(true)
                CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                webViewClient = WebViewClient()
                loadUrl(url)
            }
        },
        update = { webView ->
            if (webView.url != url) {
                syncCookies(url, cookieJar)
                webView.loadUrl(url)
            }
        },
        modifier = modifier
    )
}

private fun syncCookies(url: String, cookieJar: HostCookieJar) {
    val host = url.toHttpUrlOrNull()?.host ?: return
    val cookies = cookieJar.getCookiesForHost(host)
    val cookieManager = CookieManager.getInstance()
    cookies.forEach { cookie ->
        cookieManager.setCookie(url, "${cookie.name}=${cookie.value}")
    }
    cookieManager.flush()
}
