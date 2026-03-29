package com.bnr.app.source

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class SourceResultTest {

    // ─── getOrThrow ───────────────────────────────────────────────────────────

    @Test
    fun `getOrThrow returns data when Success`() {
        val result: SourceResult<String> = SourceResult.Success("hello")
        assertEquals("hello", result.getOrThrow())
    }

    @Test
    fun `getOrThrow throws the wrapped exception when Error`() {
        val exception = SourceException.NetworkException("no network")
        val result: SourceResult<String> = SourceResult.Error(exception)
        try {
            result.getOrThrow()
            fail("Expected exception to be thrown")
        } catch (e: SourceException.NetworkException) {
            assertSame(exception, e)
        }
    }

    // ─── map ─────────────────────────────────────────────────────────────────

    @Test
    fun `map transforms data on Success`() {
        val result: SourceResult<Int> = SourceResult.Success(3)
        val mapped = result.map { it * 10 }
        assertEquals(SourceResult.Success(30), mapped)
    }

    @Test
    fun `map passes Error through unchanged without invoking transform`() {
        val exception = SourceException.ParseException("bad html")
        val result: SourceResult<Int> = SourceResult.Error(exception)
        var transformCalled = false
        val mapped = result.map<Int, Int> { transformCalled = true; it * 10 }
        assertTrue("transform must not be called on Error", !transformCalled)
        assertTrue(mapped is SourceResult.Error)
        assertSame(exception, (mapped as SourceResult.Error).exception)
    }

    // ─── onSuccess ────────────────────────────────────────────────────────────

    @Test
    fun `onSuccess calls action and returns same result on Success`() {
        val result: SourceResult<String> = SourceResult.Success("data")
        var captured: String? = null
        val returned = result.onSuccess { captured = it }
        assertEquals("data", captured)
        assertSame(result, returned)
    }

    @Test
    fun `onSuccess skips action and returns same result on Error`() {
        val exception = SourceException.NotFoundException("not found")
        val result: SourceResult<String> = SourceResult.Error(exception)
        var actionCalled = false
        val returned = result.onSuccess { actionCalled = true }
        assertTrue("action must not be called on Error", !actionCalled)
        assertSame(result, returned)
    }

    // ─── onError ─────────────────────────────────────────────────────────────

    @Test
    fun `onError calls action and returns same result on Error`() {
        val exception = SourceException.RateLimitException("too many requests")
        val result: SourceResult<String> = SourceResult.Error(exception)
        var captured: SourceException? = null
        val returned = result.onError { captured = it }
        assertSame(exception, captured)
        assertSame(result, returned)
    }

    @Test
    fun `onError skips action and returns same result on Success`() {
        val result: SourceResult<Int> = SourceResult.Success(42)
        var actionCalled = false
        val returned = result.onError { actionCalled = true }
        assertTrue("action must not be called on Success", !actionCalled)
        assertSame(result, returned)
    }

    // ─── getOrNull ────────────────────────────────────────────────────────────

    @Test
    fun `getOrNull returns data on Success`() {
        val result: SourceResult<String> = SourceResult.Success("value")
        assertEquals("value", result.getOrNull())
    }

    @Test
    fun `getOrNull returns null on Error`() {
        val result: SourceResult<String> = SourceResult.Error(
            SourceException.CloudflareException("blocked")
        )
        assertNull(result.getOrNull())
    }

    // ─── SourceException subtypes ─────────────────────────────────────────────

    @Test
    fun `NetworkException can be created with message only`() {
        val ex = SourceException.NetworkException("timeout")
        assertEquals("timeout", ex.message)
        assertNull(ex.cause)
    }

    @Test
    fun `NetworkException can be created with message and cause`() {
        val cause = RuntimeException("root")
        val ex = SourceException.NetworkException("timeout", cause)
        assertEquals("timeout", ex.message)
        assertSame(cause, ex.cause)
    }

    @Test
    fun `ParseException can be created with message only`() {
        val ex = SourceException.ParseException("unexpected tag")
        assertEquals("unexpected tag", ex.message)
        assertNull(ex.cause)
    }

    @Test
    fun `ParseException can be created with message and cause`() {
        val cause = IllegalStateException("npe")
        val ex = SourceException.ParseException("parse error", cause)
        assertSame(cause, ex.cause)
    }

    @Test
    fun `CloudflareException can be created with message only`() {
        val ex = SourceException.CloudflareException("cf challenge")
        assertEquals("cf challenge", ex.message)
        assertNull(ex.cause)
    }

    @Test
    fun `CloudflareException can be created with message and cause`() {
        val cause = Exception("underlying")
        val ex = SourceException.CloudflareException("cf challenge", cause)
        assertSame(cause, ex.cause)
    }

    @Test
    fun `NotFoundException can be created with message`() {
        val ex = SourceException.NotFoundException("chapter missing")
        assertEquals("chapter missing", ex.message)
        assertNull(ex.cause)
    }

    @Test
    fun `RateLimitException can be created with message`() {
        val ex = SourceException.RateLimitException("slow down")
        assertEquals("slow down", ex.message)
        assertNull(ex.cause)
    }

    // ─── SourceResult is correctly typed ──────────────────────────────────────

    @Test
    fun `Success and Error are distinct subtypes of SourceResult`() {
        val success: SourceResult<Int> = SourceResult.Success(1)
        val error: SourceResult<Int> = SourceResult.Error(SourceException.NotFoundException("x"))
        assertTrue(success is SourceResult.Success)
        assertTrue(error is SourceResult.Error)
    }
}
