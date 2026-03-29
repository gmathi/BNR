package com.bnr.app.core.network

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class UserAgentInterceptorTest {

    private lateinit var server: MockWebServer
    private lateinit var client: OkHttpClient

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()

        client = OkHttpClient.Builder()
            .addInterceptor(UserAgentInterceptor())
            .build()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    // -------------------------------------------------------------------------
    // User-Agent header is set correctly
    // -------------------------------------------------------------------------

    @Test
    fun `interceptor sets User-Agent header on every request`() {
        server.enqueue(MockResponse().setResponseCode(200))

        client.newCall(Request.Builder().url(server.url("/")).build()).execute().close()

        val recorded = server.takeRequest()
        assertEquals(UserAgentInterceptor.USER_AGENT, recorded.getHeader("User-Agent"))
    }

    @Test
    fun `User-Agent matches the USER_AGENT constant exactly`() {
        server.enqueue(MockResponse().setResponseCode(200))

        client.newCall(Request.Builder().url(server.url("/path")).build()).execute().close()

        val recorded = server.takeRequest()
        val expectedUa =
            "Mozilla/5.0 (Linux; Android 14; Pixel 8) " +
            "AppleWebKit/537.36 (KHTML, like Gecko) " +
            "Chrome/124.0.0.0 Mobile Safari/537.36"
        assertEquals(expectedUa, recorded.getHeader("User-Agent"))
    }

    @Test
    fun `interceptor overwrites any caller-supplied User-Agent`() {
        server.enqueue(MockResponse().setResponseCode(200))

        val request = Request.Builder()
            .url(server.url("/"))
            .header("User-Agent", "CustomAgent/1.0")
            .build()
        client.newCall(request).execute().close()

        val recorded = server.takeRequest()
        assertEquals(UserAgentInterceptor.USER_AGENT, recorded.getHeader("User-Agent"))
    }

    // -------------------------------------------------------------------------
    // Other headers are preserved
    // -------------------------------------------------------------------------

    @Test
    fun `interceptor preserves other request headers`() {
        server.enqueue(MockResponse().setResponseCode(200))

        val request = Request.Builder()
            .url(server.url("/"))
            .header("Authorization", "Bearer token123")
            .header("Accept", "application/json")
            .build()
        client.newCall(request).execute().close()

        val recorded = server.takeRequest()
        assertEquals("Bearer token123", recorded.getHeader("Authorization"))
        assertEquals("application/json", recorded.getHeader("Accept"))
        // User-Agent is still overwritten
        assertEquals(UserAgentInterceptor.USER_AGENT, recorded.getHeader("User-Agent"))
    }

    @Test
    fun `interceptor does not add unexpected headers`() {
        server.enqueue(MockResponse().setResponseCode(200))

        client.newCall(Request.Builder().url(server.url("/")).build()).execute().close()

        val recorded = server.takeRequest()
        // OkHttp always adds Host and Connection; beyond those the only extra header
        // the interceptor injects is User-Agent.
        assertNull("X-Custom should not be present", recorded.getHeader("X-Custom"))
    }

    // -------------------------------------------------------------------------
    // Interceptor is called for every request in a session
    // -------------------------------------------------------------------------

    @Test
    fun `User-Agent is set on every subsequent request`() {
        repeat(3) { server.enqueue(MockResponse().setResponseCode(200)) }

        repeat(3) {
            client.newCall(Request.Builder().url(server.url("/req-$it")).build()).execute().close()
        }

        repeat(3) {
            val recorded = server.takeRequest()
            assertEquals(
                "Request $it missing correct User-Agent",
                UserAgentInterceptor.USER_AGENT,
                recorded.getHeader("User-Agent"),
            )
        }
    }
}
