package com.bnr.app.data.repository

import app.cash.turbine.test
import com.bnr.app.data.local.db.entity.NovelEntity
import com.bnr.app.data.local.db.dao.NovelDao
import com.bnr.app.domain.model.Novel
import com.bnr.app.domain.model.NovelStatus
import com.bnr.app.source.Source
import com.bnr.app.source.SourceException
import com.bnr.app.source.SourceManager
import com.bnr.app.source.SourceResult
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class NovelRepositoryImplTest {

    private val sourceManager: SourceManager = mockk()
    private val novelDao: NovelDao = mockk(relaxUnitFun = true)
    private val source: Source = mockk()

    private lateinit var repository: NovelRepositoryImpl

    // ── shared fixtures ───────────────────────────────────────────────────────

    private val novelId = "src::slug"
    private val sourceId = "src"
    private val novelUrl = "https://example.com/novel/slug"

    private val domainNovel = Novel(
        id = novelId,
        sourceId = sourceId,
        url = novelUrl,
        title = "Test Novel",
        author = "Author",
        coverUrl = "https://example.com/cover.jpg",
        description = "A description",
        genres = listOf("Fantasy", "Action"),
        status = NovelStatus.ONGOING,
        inLibrary = false,
        lastReadChapterId = null,
        addedToLibraryAt = null,
        chapterCount = 5
    )

    private val novelEntity = NovelEntity(
        id = novelId,
        sourceId = sourceId,
        url = novelUrl,
        title = "Test Novel",
        author = "Author",
        coverUrl = "https://example.com/cover.jpg",
        description = "A description",
        genres = "[\"Fantasy\",\"Action\"]",
        status = "ONGOING",
        chapterCount = 5,
        inLibrary = false,
        lastReadChapterId = null,
        addedToLibraryAt = null
    )

    @Before
    fun setUp() {
        every { sourceManager.getSource(sourceId) } returns source
        repository = NovelRepositoryImpl(sourceManager, novelDao)
    }

    // ── getPopularNovels ──────────────────────────────────────────────────────

    @Test
    fun `getPopularNovels delegates to source and returns Success`() = runTest(UnconfinedTestDispatcher()) {
        val novels = listOf(domainNovel)
        coEvery { source.getPopularNovels(1) } returns SourceResult.Success(novels)

        val result = repository.getPopularNovels(sourceId, 1)

        assertTrue(result is SourceResult.Success)
        assertEquals(novels, (result as SourceResult.Success).data)
        coVerify(exactly = 1) { source.getPopularNovels(1) }
    }

    // ── getNovelDetail ────────────────────────────────────────────────────────

    @Test
    fun `getNovelDetail on Success caches novel and preserves existing inLibrary and addedToLibraryAt`() =
        runTest(UnconfinedTestDispatcher()) {
            val existingEntity = novelEntity.copy(
                inLibrary = true,
                addedToLibraryAt = 123456789L,
                lastReadChapterId = "chapter::1"
            )
            coEvery { source.getNovelDetail(novelUrl) } returns SourceResult.Success(domainNovel)
            coEvery { novelDao.getNovelById(novelId) } returns existingEntity

            val result = repository.getNovelDetail(sourceId, novelUrl)

            assertTrue(result is SourceResult.Success)
            assertEquals(domainNovel, (result as SourceResult.Success).data)

            // The upserted entity must carry preserved library fields from the existing row
            coVerify(exactly = 1) {
                novelDao.upsertNovel(
                    match { entity ->
                        entity.id == novelId &&
                            entity.inLibrary == true &&
                            entity.addedToLibraryAt == 123456789L &&
                            entity.lastReadChapterId == "chapter::1"
                    }
                )
            }
        }

    @Test
    fun `getNovelDetail on Success with no existing row uses default library fields`() =
        runTest(UnconfinedTestDispatcher()) {
            coEvery { source.getNovelDetail(novelUrl) } returns SourceResult.Success(domainNovel)
            coEvery { novelDao.getNovelById(novelId) } returns null

            repository.getNovelDetail(sourceId, novelUrl)

            coVerify(exactly = 1) {
                novelDao.upsertNovel(
                    match { entity ->
                        entity.id == novelId &&
                            !entity.inLibrary &&
                            entity.addedToLibraryAt == null &&
                            entity.lastReadChapterId == null
                    }
                )
            }
        }

    @Test
    fun `getNovelDetail on Error does not call upsertNovel`() =
        runTest(UnconfinedTestDispatcher()) {
            val error = SourceResult.Error(SourceException.NetworkException("timeout"))
            coEvery { source.getNovelDetail(novelUrl) } returns error

            val result = repository.getNovelDetail(sourceId, novelUrl)

            assertTrue(result is SourceResult.Error)
            coVerify(exactly = 0) { novelDao.upsertNovel(any()) }
        }

    // ── addToLibrary ──────────────────────────────────────────────────────────

    @Test
    fun `addToLibrary calls upsertNovel then addToLibrary on dao`() =
        runTest(UnconfinedTestDispatcher()) {
            repository.addToLibrary(domainNovel)

            coVerify(exactly = 1) { novelDao.upsertNovel(any()) }
            coVerify(exactly = 1) { novelDao.addToLibrary(eq(novelId), any()) }
        }

    @Test
    fun `addToLibrary passes correct novelId to addToLibrary`() =
        runTest(UnconfinedTestDispatcher()) {
            repository.addToLibrary(domainNovel)

            coVerify { novelDao.addToLibrary(novelId, any()) }
        }

    // ── removeFromLibrary ─────────────────────────────────────────────────────

    @Test
    fun `removeFromLibrary delegates to dao`() = runTest(UnconfinedTestDispatcher()) {
        repository.removeFromLibrary(novelId)

        coVerify(exactly = 1) { novelDao.removeFromLibrary(novelId) }
    }

    // ── isInLibrary ───────────────────────────────────────────────────────────

    @Test
    fun `isInLibrary returns true when dao entity has inLibrary true`() =
        runTest(UnconfinedTestDispatcher()) {
            coEvery { novelDao.getNovelById(novelId) } returns novelEntity.copy(inLibrary = true)

            val result = repository.isInLibrary(novelId)

            assertTrue(result)
        }

    @Test
    fun `isInLibrary returns false when dao entity has inLibrary false`() =
        runTest(UnconfinedTestDispatcher()) {
            coEvery { novelDao.getNovelById(novelId) } returns novelEntity.copy(inLibrary = false)

            val result = repository.isInLibrary(novelId)

            assertFalse(result)
        }

    @Test
    fun `isInLibrary returns false when dao returns null`() =
        runTest(UnconfinedTestDispatcher()) {
            coEvery { novelDao.getNovelById(novelId) } returns null

            val result = repository.isInLibrary(novelId)

            assertFalse(result)
        }

    // ── getLibraryNovels ──────────────────────────────────────────────────────

    @Test
    fun `getLibraryNovels emits mapped domain models from dao Flow`() =
        runTest(UnconfinedTestDispatcher()) {
            val libraryEntity = novelEntity.copy(inLibrary = true, addedToLibraryAt = 111L)
            every { novelDao.getLibraryNovels() } returns flowOf(listOf(libraryEntity))

            repository.getLibraryNovels().test {
                val emitted = awaitItem()
                assertEquals(1, emitted.size)
                val novel = emitted.first()
                assertEquals(novelId, novel.id)
                assertEquals("Test Novel", novel.title)
                assertTrue(novel.inLibrary)
                assertEquals(111L, novel.addedToLibraryAt)
                awaitComplete()
            }
        }

    @Test
    fun `getLibraryNovels emits empty list when dao returns no novels`() =
        runTest(UnconfinedTestDispatcher()) {
            every { novelDao.getLibraryNovels() } returns flowOf(emptyList())

            repository.getLibraryNovels().test {
                val emitted = awaitItem()
                assertTrue(emitted.isEmpty())
                awaitComplete()
            }
        }

    // ── updateLastReadChapter ─────────────────────────────────────────────────

    @Test
    fun `updateLastReadChapter delegates to dao with correct ids`() =
        runTest(UnconfinedTestDispatcher()) {
            val chapterId = "chapter::42"

            repository.updateLastReadChapter(novelId, chapterId)

            coVerify(exactly = 1) { novelDao.updateLastReadChapter(novelId, chapterId) }
        }
}
