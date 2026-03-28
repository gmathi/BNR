package com.bnr.app.core.network

import okhttp3.Cookie
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class HostCookieJarTest {

    private lateinit var cookieJar: HostCookieJar

    @Before
    fun setUp() {
        cookieJar = HostCookieJar()
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private fun cookie(name: String, value: String, domain: String): Cookie =
        Cookie.Builder()
            .name(name)
            .value(value)
            .domain(domain)
            .build()

    private fun urlFor(host: String) = "https://$host/".toHttpUrl()

    // -------------------------------------------------------------------------
    // saveFromResponse + loadForRequest round-trip
    // -------------------------------------------------------------------------

    @Test
    fun `saveFromResponse then loadForRequest returns saved cookies for same host`() {
        val url = urlFor("site-a.com")
        val cookies = listOf(
            cookie("session", "abc123", "site-a.com"),
            cookie("pref", "dark", "site-a.com"),
        )

        cookieJar.saveFromResponse(url, cookies)
        val loaded = cookieJar.loadForRequest(url)

        assertEquals(2, loaded.size)
        assertTrue(loaded.any { it.name == "session" && it.value == "abc123" })
        assertTrue(loaded.any { it.name == "pref" && it.value == "dark" })
    }

    // -------------------------------------------------------------------------
    // Per-host isolation
    // -------------------------------------------------------------------------

    @Test
    fun `cookies saved for host A are NOT returned for host B`() {
        val urlA = urlFor("site-a.com")
        val urlB = urlFor("site-b.com")

        cookieJar.saveFromResponse(urlA, listOf(cookie("token", "secret", "site-a.com")))

        val loadedForB = cookieJar.loadForRequest(urlB)
        assertTrue(loadedForB.isEmpty())
    }

    @Test
    fun `loadForRequest returns empty list when no cookies saved for host`() {
        val loaded = cookieJar.loadForRequest(urlFor("unknown.com"))
        assertTrue(loaded.isEmpty())
    }

    // -------------------------------------------------------------------------
    // Merging — same name replaces old value
    // -------------------------------------------------------------------------

    @Test
    fun `saving cookie with same name replaces old cookie for same host`() {
        val url = urlFor("site-a.com")

        cookieJar.saveFromResponse(url, listOf(cookie("session", "old-value", "site-a.com")))
        cookieJar.saveFromResponse(url, listOf(cookie("session", "new-value", "site-a.com")))

        val loaded = cookieJar.loadForRequest(url)
        assertEquals(1, loaded.size)
        assertEquals("new-value", loaded.first { it.name == "session" }.value)
    }

    @Test
    fun `saving cookie with same name via saveForHost replaces old value`() {
        cookieJar.saveForHost("site-a.com", listOf(cookie("token", "v1", "site-a.com")))
        cookieJar.saveForHost("site-a.com", listOf(cookie("token", "v2", "site-a.com")))

        val loaded = cookieJar.getCookiesForHost("site-a.com")
        assertEquals(1, loaded.size)
        assertEquals("v2", loaded.first().value)
    }

    // -------------------------------------------------------------------------
    // Merging — different names accumulate
    // -------------------------------------------------------------------------

    @Test
    fun `saving cookies with different names accumulates them`() {
        val url = urlFor("site-a.com")

        cookieJar.saveFromResponse(url, listOf(cookie("session", "abc", "site-a.com")))
        cookieJar.saveFromResponse(url, listOf(cookie("pref", "light", "site-a.com")))

        val loaded = cookieJar.loadForRequest(url)
        assertEquals(2, loaded.size)
        assertTrue(loaded.any { it.name == "session" })
        assertTrue(loaded.any { it.name == "pref" })
    }

    @Test
    fun `saveForHost with different names accumulates cookies`() {
        cookieJar.saveForHost("site-a.com", listOf(cookie("a", "1", "site-a.com")))
        cookieJar.saveForHost("site-a.com", listOf(cookie("b", "2", "site-a.com")))

        val loaded = cookieJar.getCookiesForHost("site-a.com")
        assertEquals(2, loaded.size)
        assertTrue(loaded.any { it.name == "a" && it.value == "1" })
        assertTrue(loaded.any { it.name == "b" && it.value == "2" })
    }

    // -------------------------------------------------------------------------
    // saveForHost / getCookiesForHost direct API
    // -------------------------------------------------------------------------

    @Test
    fun `saveForHost stores cookies retrievable via getCookiesForHost`() {
        val cookies = listOf(
            cookie("cf_clearance", "xyz", "example.com"),
            cookie("__cfduid", "abc", "example.com"),
        )

        cookieJar.saveForHost("example.com", cookies)
        val retrieved = cookieJar.getCookiesForHost("example.com")

        assertEquals(2, retrieved.size)
        assertTrue(retrieved.any { it.name == "cf_clearance" && it.value == "xyz" })
        assertTrue(retrieved.any { it.name == "__cfduid" && it.value == "abc" })
    }

    @Test
    fun `getCookiesForHost returns empty list for unknown host`() {
        val result = cookieJar.getCookiesForHost("not-stored.com")
        assertTrue(result.isEmpty())
    }

    // -------------------------------------------------------------------------
    // clearHost
    // -------------------------------------------------------------------------

    @Test
    fun `clearHost removes cookies only for the specified host`() {
        cookieJar.saveForHost("site-a.com", listOf(cookie("s", "1", "site-a.com")))
        cookieJar.saveForHost("site-b.com", listOf(cookie("s", "2", "site-b.com")))

        cookieJar.clearHost("site-a.com")

        assertTrue(cookieJar.getCookiesForHost("site-a.com").isEmpty())
        assertEquals(1, cookieJar.getCookiesForHost("site-b.com").size)
    }

    @Test
    fun `clearHost on unknown host is a no-op`() {
        cookieJar.saveForHost("site-a.com", listOf(cookie("s", "1", "site-a.com")))

        cookieJar.clearHost("does-not-exist.com") // must not throw

        assertEquals(1, cookieJar.getCookiesForHost("site-a.com").size)
    }

    // -------------------------------------------------------------------------
    // clearAll
    // -------------------------------------------------------------------------

    @Test
    fun `clearAll removes cookies for all hosts`() {
        cookieJar.saveForHost("site-a.com", listOf(cookie("s", "1", "site-a.com")))
        cookieJar.saveForHost("site-b.com", listOf(cookie("s", "2", "site-b.com")))
        cookieJar.saveForHost("site-c.com", listOf(cookie("s", "3", "site-c.com")))

        cookieJar.clearAll()

        assertTrue(cookieJar.getCookiesForHost("site-a.com").isEmpty())
        assertTrue(cookieJar.getCookiesForHost("site-b.com").isEmpty())
        assertTrue(cookieJar.getCookiesForHost("site-c.com").isEmpty())
    }

    // -------------------------------------------------------------------------
    // Thread safety — concurrent reads/writes must not throw
    // -------------------------------------------------------------------------

    @Test
    fun `concurrent saves and loads do not throw`() {
        val threadCount = 20
        val iterationsPerThread = 50
        val executor = Executors.newFixedThreadPool(threadCount)
        val startLatch = CountDownLatch(1)
        val doneLatch = CountDownLatch(threadCount)
        val errors = mutableListOf<Throwable>()

        repeat(threadCount) { threadIndex ->
            executor.submit {
                try {
                    startLatch.await()
                    val host = "host-${threadIndex % 4}.com"
                    repeat(iterationsPerThread) { i ->
                        cookieJar.saveForHost(host, listOf(cookie("key-$i", "val-$i", host)))
                        cookieJar.getCookiesForHost(host)
                    }
                } catch (t: Throwable) {
                    synchronized(errors) { errors.add(t) }
                } finally {
                    doneLatch.countDown()
                }
            }
        }

        startLatch.countDown()
        val completed = doneLatch.await(10, TimeUnit.SECONDS)
        executor.shutdown()

        assertTrue("Threads did not finish in time", completed)
        assertTrue("Concurrent access threw: ${errors.firstOrNull()}", errors.isEmpty())
    }

    @Test
    fun `concurrent clearAll and saves do not throw`() {
        val executor = Executors.newFixedThreadPool(10)
        val startLatch = CountDownLatch(1)
        val doneLatch = CountDownLatch(10)
        val errors = mutableListOf<Throwable>()

        // 5 writers, 5 clearAll callers
        repeat(5) { i ->
            executor.submit {
                try {
                    startLatch.await()
                    repeat(30) { j ->
                        cookieJar.saveForHost("host-$i.com", listOf(cookie("c", "$j", "host-$i.com")))
                    }
                } catch (t: Throwable) {
                    synchronized(errors) { errors.add(t) }
                } finally {
                    doneLatch.countDown()
                }
            }
        }
        repeat(5) {
            executor.submit {
                try {
                    startLatch.await()
                    repeat(30) { cookieJar.clearAll() }
                } catch (t: Throwable) {
                    synchronized(errors) { errors.add(t) }
                } finally {
                    doneLatch.countDown()
                }
            }
        }

        startLatch.countDown()
        val completed = doneLatch.await(10, TimeUnit.SECONDS)
        executor.shutdown()

        assertTrue("Threads did not finish in time", completed)
        assertTrue("Concurrent clearAll threw: ${errors.firstOrNull()}", errors.isEmpty())
    }
}
