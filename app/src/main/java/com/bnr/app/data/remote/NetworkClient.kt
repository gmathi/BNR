package com.bnr.app.data.remote

import com.bnr.app.core.network.CloudflareInterceptor
import com.bnr.app.core.network.HostCookieJar
import com.bnr.app.core.network.UserAgentInterceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit

object NetworkClient {

    fun buildOkHttpClient(
        cookieJar: HostCookieJar,
        cloudflareInterceptor: CloudflareInterceptor,
        userAgentInterceptor: UserAgentInterceptor,
        enableLogging: Boolean = false
    ): OkHttpClient = OkHttpClient.Builder()
        .cookieJar(cookieJar)
        .addInterceptor(userAgentInterceptor)
        .addInterceptor(cloudflareInterceptor)
        .apply {
            if (enableLogging) {
                addInterceptor(
                    HttpLoggingInterceptor().apply {
                        level = HttpLoggingInterceptor.Level.HEADERS
                    }
                )
            }
        }
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()
}
