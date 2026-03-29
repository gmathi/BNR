package com.bnr.app.core.network

import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Per-host cookie store. Cookies for site A never bleed into site B.
 * Thread-safe via ConcurrentHashMap.
 */
@Singleton
class HostCookieJar @Inject constructor() : CookieJar {

    private val store = ConcurrentHashMap<String, MutableList<Cookie>>()

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        saveForHost(url.host, cookies)
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> =
        store[url.host]?.toList() ?: emptyList()

    fun saveForHost(host: String, cookies: List<Cookie>) {
        val existing = store.getOrPut(host) { mutableListOf() }
        synchronized(existing) {
            existing.removeAll { old -> cookies.any { new -> new.name == old.name } }
            existing.addAll(cookies)
        }
    }

    fun getCookiesForHost(host: String): List<Cookie> =
        store[host]?.toList() ?: emptyList()

    fun clearHost(host: String) = store.remove(host)

    fun clearAll() = store.clear()
}
