package com.bnr.app.core.network

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.webkit.CookieManager
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.Cookie
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * Solves Cloudflare JS challenges using a headless WebView.
 * Must NOT be called from the main thread — it blocks via runBlocking.
 */
@Singleton
class WebViewChallengeResolver @Inject constructor(
    @ApplicationContext private val context: Context,
    private val cookieJar: HostCookieJar
) {
    private val mainHandler = Handler(Looper.getMainLooper())

    fun solveChallengeBlocking(url: String, host: String) {
        runBlocking {
            withTimeoutOrNull(CHALLENGE_TIMEOUT_MS) {
                solveChallengeAsync(url, host)
            }
        }
    }

    private suspend fun solveChallengeAsync(url: String, host: String) =
        suspendCancellableCoroutine<Unit> { continuation ->
            var webView: WebView? = null

            mainHandler.post {
                webView = WebView(context).apply {
                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        userAgentString = UserAgentInterceptor.USER_AGENT
                        cacheMode = WebSettings.LOAD_DEFAULT
                        mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                    }
                    CookieManager.getInstance().setAcceptCookie(true)
                    CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)

                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView, loadedUrl: String) {
                            val rawCookies = CookieManager.getInstance().getCookie(loadedUrl)
                                ?: return
                            if ("cf_clearance" in rawCookies) {
                                transferCookiesToJar(rawCookies, host)
                                if (continuation.isActive) continuation.resume(Unit)
                            }
                        }
                    }
                    loadUrl(url)
                }

                continuation.invokeOnCancellation {
                    mainHandler.post { webView?.destroy() }
                }
            }
        }

    private fun transferCookiesToJar(rawCookies: String, host: String) {
        val cookies = rawCookies.split(";").mapNotNull { pair ->
            val parts = pair.trim().split("=", limit = 2)
            if (parts.size == 2) {
                runCatching {
                    Cookie.Builder()
                        .domain(host)
                        .name(parts[0].trim())
                        .value(parts[1].trim())
                        .build()
                }.getOrNull()
            } else null
        }
        if (cookies.isNotEmpty()) {
            cookieJar.saveForHost(host, cookies)
        }
    }

    companion object {
        private const val CHALLENGE_TIMEOUT_MS = 30_000L
    }
}
