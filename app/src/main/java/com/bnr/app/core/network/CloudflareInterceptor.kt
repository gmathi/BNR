package com.bnr.app.core.network

import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Detects Cloudflare 403/503 challenge responses and triggers WebView-based solving.
 * The interceptor retries the original request after cookies are updated.
 */
@Singleton
class CloudflareInterceptor @Inject constructor(
    private val cookieJar: HostCookieJar,
    private val resolver: WebViewChallengeResolver
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val response = chain.proceed(request)

        if (response.code in CF_CHALLENGE_CODES && isCloudflareChallenge(response)) {
            response.close()
            resolver.solveChallengeBlocking(
                url = request.url.toString(),
                host = request.url.host
            )
            return chain.proceed(request.newBuilder().build())
        }
        return response
    }

    private fun isCloudflareChallenge(response: Response): Boolean {
        val server = response.header("Server") ?: ""
        val cfRay = response.header("CF-RAY")
        return server.contains("cloudflare", ignoreCase = true) || cfRay != null
    }

    companion object {
        private val CF_CHALLENGE_CODES = setOf(403, 503)
    }
}
