package com.bnr.app.source

import com.bnr.app.domain.model.Chapter
import com.bnr.app.domain.model.ChapterContent
import com.bnr.app.domain.model.Novel
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Test

class SourceManagerTest {

    // Three mock Sources with deliberately unsorted names to verify sort order.
    private lateinit var sourceRoyalRoad: Source
    private lateinit var sourceScribbleHub: Source
    private lateinit var sourceWebNovel: Source

    private lateinit var manager: SourceManager

    @Before
    fun setUp() {
        sourceRoyalRoad = mockk<Source>().also {
            every { it.id } returns "com.source.royalroad"
            every { it.name } returns "Royal Road"
        }
        sourceScribbleHub = mockk<Source>().also {
            every { it.id } returns "com.source.scribblehub"
            every { it.name } returns "Scribble Hub"
        }
        sourceWebNovel = mockk<Source>().also {
            every { it.id } returns "com.source.webnovel"
            every { it.name } returns "Web Novel"
        }

        // Provide sources in non-alphabetical order to ensure sorting is tested properly.
        manager = SourceManager(
            sources = setOf(sourceWebNovel, sourceRoyalRoad, sourceScribbleHub)
        )
    }

    // ─── getSource ────────────────────────────────────────────────────────────

    @Test
    fun `getSource returns correct source for known id`() {
        assertSame(sourceRoyalRoad, manager.getSource("com.source.royalroad"))
        assertSame(sourceScribbleHub, manager.getSource("com.source.scribblehub"))
        assertSame(sourceWebNovel, manager.getSource("com.source.webnovel"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `getSource throws IllegalArgumentException for unknown id`() {
        manager.getSource("com.source.doesnotexist")
    }

    @Test
    fun `getSource exception message contains the unknown id`() {
        val unknownId = "com.source.mystery"
        try {
            manager.getSource(unknownId)
        } catch (e: IllegalArgumentException) {
            assert(e.message?.contains(unknownId) == true) {
                "Expected message to contain '$unknownId' but was: ${e.message}"
            }
            return
        }
        throw AssertionError("Expected IllegalArgumentException to be thrown")
    }

    // ─── getSourceOrNull ──────────────────────────────────────────────────────

    @Test
    fun `getSourceOrNull returns source when id is found`() {
        assertSame(sourceRoyalRoad, manager.getSourceOrNull("com.source.royalroad"))
        assertSame(sourceScribbleHub, manager.getSourceOrNull("com.source.scribblehub"))
        assertSame(sourceWebNovel, manager.getSourceOrNull("com.source.webnovel"))
    }

    @Test
    fun `getSourceOrNull returns null for unknown id`() {
        assertNull(manager.getSourceOrNull("com.source.unknown"))
    }

    @Test
    fun `getSourceOrNull returns null for empty string id`() {
        assertNull(manager.getSourceOrNull(""))
    }

    // ─── getAllSources ────────────────────────────────────────────────────────

    @Test
    fun `getAllSources returns all registered sources`() {
        val all = manager.getAllSources()
        assertEquals(3, all.size)
        assert(all.containsAll(listOf(sourceRoyalRoad, sourceScribbleHub, sourceWebNovel)))
    }

    @Test
    fun `getAllSources returns sources sorted by name ascending`() {
        val all = manager.getAllSources()
        val names = all.map { it.name }
        assertEquals(listOf("Royal Road", "Scribble Hub", "Web Novel"), names)
    }

    @Test
    fun `getAllSources with single source returns list of one`() {
        val single = mockk<Source>().also {
            every { it.id } returns "com.source.only"
            every { it.name } returns "Only Source"
        }
        val singleManager = SourceManager(sources = setOf(single))
        val all = singleManager.getAllSources()
        assertEquals(1, all.size)
        assertSame(single, all[0])
    }

    @Test
    fun `getAllSources with empty set returns empty list`() {
        val emptyManager = SourceManager(sources = emptySet())
        assertEquals(emptyList<Source>(), emptyManager.getAllSources())
    }

    // ─── consistency between getSource and getAllSources ──────────────────────

    @Test
    fun `every source returned by getAllSources is retrievable by getSource`() {
        manager.getAllSources().forEach { source ->
            assertSame(source, manager.getSource(source.id))
        }
    }
}
