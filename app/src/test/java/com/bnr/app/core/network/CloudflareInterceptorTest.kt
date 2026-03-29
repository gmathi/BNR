package com.bnr.app.core.network

import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import okhttp3.Cookie
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class CloudflareInterceptorTest {

    private lateinit var server: MockWebServer
    private lateinit var cookieJar: HostCookieJar
    private lateinit var resolver: WebViewChallengeResolver
    private lateinit var client: OkHttpClient

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()

        cookieJar = HostCookieJar()

        // resolver is mocked so solveChallengeBlocking is a no-op (no WebView/main thread needed)
        resolver = mockk<WebViewChallengeResolver>()
        justRun {
            resolver.solveChallengeBlocking(any(), any())
        }

        val interceptor = CloudflareInterceptor(cookieJar, resolver)

        client = OkHttpClient.Builder()
            .cookieJar(cookieJar)
            .addInterceptor(interceptor)
            .build()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private fun get(path: String = "/"): okhttp3.Response =
        client.newCall(Request.Builder().url(server.url(path)).build()).execute()

    // -------------------------------------------------------------------------
    // Non-CF 200: pass-through, resolver NOT called
    // -------------------------------------------------------------------------

    @Test
    fun `200 response without CF headers passes through and resolver is not called`() {
        server.enqueue(MockResponse().setResponseCode(200).setBody("OK"))

        val response = get()
        assertEquals(200, response.code)
        response.close()

        verify(exactly = 0) { resolver.solveChallengeBlocking(any(), any()) }
    }

    // -------------------------------------------------------------------------
    // Non-CF 404: pass-through, resolver NOT called
    // -------------------------------------------------------------------------

    @Test
    fun `404 response without CF headers passes through and resolver is not called`() {
        server.enqueue(MockResponse().setResponseCode(404).setBody("Not Found"))

        val response = get()
        assertEquals(404, response.code)
        response.close()

        verify(exactly = 0) { resolver.solveChallengeBlocking(any(), any()) }
    }

    // -------------------------------------------------------------------------
    // 503 without "Server: cloudflare": pass-through, resolver NOT called
    // -------------------------------------------------------------------------

    @Test
    fun `503 without cloudflare server header passes through and resolver is not called`() {
        server.enqueue(
            MockResponse()
                .setResponseCode(503)
                .addHeader("Server", "nginx")
                .setBody("Service Unavailable"),
        )

        val response = get()
        assertEquals(503, response.code)
        response.close()

        verify(exactly = 0) { resolver.solveChallengeBlocking(any(), any()) }
    }

    // -------------------------------------------------------------------------
    // 503 WITH "Server: cloudflare": resolver IS called, request retried
    // -------------------------------------------------------------------------

    @Test
    fun `503 with Server cloudflare header triggers resolver and retries request`() {
        // First response — Cloudflare challenge
        server.enqueue(
            MockResponse()
                .setResponseCode(503)
                .addHeader("Server", "cloudflare")
                .setBody("Cloudflare challenge"),
        )
        // Second response — successful retry
        server.enqueue(MockResponse().setResponseCode(200).setBody("Content"))

        val response = get()
        assertEquals(200, response.code)
        response.close()

        verify(exactly = 1) { resolver.solveChallengeBlocking(any(), any()) }
        assertEquals(2, server.requestCount)
    }

    @Test
    fun `503 with Server header case-insensitive cloudflare match triggers resolver`() {
        server.enqueue(
            MockResponse()
                .setResponseCode(503)
                .addHeader("Server", "Cloudflare")   // capital C
                .setBody("challenge"),
        )
        server.enqueue(MockResponse().setResponseCode(200).setBody("OK"))

        val response = get()
        assertEquals(200, response.code)
        response.close()

        verify(exactly = 1) { resolver.solveChallengeBlocking(any(), any()) }
    }

    // -------------------------------------------------------------------------
    // 403 WITH "CF-RAY" header: resolver IS called, request retried
    // -------------------------------------------------------------------------

    @Test
    fun `403 with CF-RAY header triggers resolver and retries request`() {
        server.enqueue(
            MockResponse()
                .setResponseCode(403)
                .addHeader("CF-RAY", "7d3f2a1b2c3d4e5f-LAX")
                .setBody("Forbidden"),
        )
        server.enqueue(MockResponse().setResponseCode(200).setBody("Access granted"))

        val response = get()
        assertEquals(200, response.code)
        response.close()

        verify(exactly = 1) { resolver.solveChallengeBlocking(any(), any()) }
        assertEquals(2, server.requestCount)
    }

    // -------------------------------------------------------------------------
    // After challenge solved, new cookies from cookieJar are sent with retry
    // -------------------------------------------------------------------------

    @Test
    fun `after challenge solved, cookies in cookieJar are sent with the retried request`() {
        val host = server.hostName

        // Simulate the resolver populating the cookieJar with cf_clearance
        every { resolver.solveChallengeBlocking(any(), any()) } answers {
            cookieJar.saveForHost(
                host,
                listOf(
                    Cookie.Builder()
                        .name("cf_clearance")
                        .value("test-clearance-token")
                        .domain(host)
                        .build(),
                ),
            )
        }

        server.enqueue(
            MockResponse()
                .setResponseCode(503)
                .addHeader("Server", "cloudflare")
                .setBody("challenge"),
        )
        server.enqueue(MockResponse().setResponseCode(200).setBody("OK"))

        get().close()

        // The retried (second) request must carry the cookie set by the resolver
        server.takeRequest() // discard initial challenge request
        val retryRequest = server.takeRequest()
        val cookieHeader = retryRequest.getHeader("Cookie") ?: ""
        assert(cookieHeader.contains("cf_clearance=test-clearance-token")) {
            "Expected cf_clearance cookie in retry, got: $cookieHeader"
        }
    }

    // -------------------------------------------------------------------------
    // resolver.solveChallengeBlocking receives correct url and host
    // -------------------------------------------------------------------------

    @Test
    fun `resolver is called with the correct url and host`() {
        server.enqueue(
            MockResponse()
                .setResponseCode(503)
                .addHeader("Server", "cloudflare"),
        )
        server.enqueue(MockResponse().setResponseCode(200))

        get("/some/path").close()

        verify(exactly = 1) {
            resolver.solveChallengeBlocking(
                url = match { it.contains("/some/path") },
                host = server.hostName,
            )
        }
    }

    // -------------------------------------------------------------------------
    // Non-challenge 403 (no CF headers): pass-through, resolver NOT called
    // -------------------------------------------------------------------------

    @Test
    fun `403 without any CF headers passes through and resolver is not called`() {
        server.enqueue(MockResponse().setResponseCode(403).setBody("Forbidden"))

        val response = get()
        assertEquals(403, response.code)
        response.close()

        verify(exactly = 0) { resolver.solveChallengeBlocking(any(), any()) }
    }

    // -------------------------------------------------------------------------
    // 200 WITH cloudflare server header: should NOT trigger (only 403/503 do)
    // -------------------------------------------------------------------------

    @Test
    fun `200 with Server cloudflare header does NOT trigger resolver`() {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .addHeader("Server", "cloudflare")
                .setBody("OK"),
        )

        val response = get()
        assertEquals(200, response.code)
        response.close()

        verify(exactly = 0) { resolver.solveChallengeBlocking(any(), any()) }
    }
}
