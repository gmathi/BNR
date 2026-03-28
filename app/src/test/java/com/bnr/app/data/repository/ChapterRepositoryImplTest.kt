package com.bnr.app.data.repository

import app.cash.turbine.test
import com.bnr.app.data.local.db.dao.ChapterDao
import com.bnr.app.data.local.db.entity.ChapterEntity
import com.bnr.app.domain.model.Chapter
import com.bnr.app.domain.model.DownloadState
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ChapterRepositoryImplTest {

    private val sourceManager: SourceManager = mockk()
    private val chapterDao: ChapterDao = mockk(relaxUnitFun = true)
    private val source: Source = mockk()

    private lateinit var repository: ChapterRepositoryImpl

    // ── shared fixtures ───────────────────────────────────────────────────────

    private val sourceId = "src"
    private val novelId = "src::novel-slug"
    private val novelUrl = "https://example.com/novel/slug"
    private val chapterId = "src::novel-slug::1"

    private val domainChapter = Chapter(
        id = chapterId,
        novelId = novelId,
        sourceId = sourceId,
        url = "https://example.com/chapter/1",
        title = "Chapter 1",
        chapterNumber = 1f,
        uploadedAt = 1_000_000L,
        isRead = false,
        downloadState = DownloadState.NOT_DOWNLOADED,
        localFilePath = null
    )

    private val chapterEntity = ChapterEntity(
        id = chapterId,
        novelId = novelId,
        sourceId = sourceId,
        url = "https://example.com/chapter/1",
        title = "Chapter 1",
        chapterNumber = 1f,
        uploadedAt = 1_000_000L,
        isRead = false,
        downloadState = "NOT_DOWNLOADED",
        localFilePath = null
    )

    @Before
    fun setUp() {
        every { sourceManager.getSource(sourceId) } returns source
        repository = ChapterRepositoryImpl(sourceManager, chapterDao)
    }

    // ── getChapterList ────────────────────────────────────────────────────────

    @Test
    fun `getChapterList on Success upserts mapped chapter entities`() =
        runTest(UnconfinedTestDispatcher()) {
            val chapters = listOf(domainChapter)
            coEvery { source.getChapterList(novelUrl) } returns SourceResult.Success(chapters)
            coEvery { chapterDao.getChaptersByNovelId(novelId) } returns flowOf(listOf(chapterEntity))

            val result = repository.getChapterList(sourceId, novelUrl, novelId)

            assertTrue(result is SourceResult.Success)
            coVerify(exactly = 1) {
                chapterDao.upsertChapters(
                    match { entities ->
                        entities.size == 1 &&
                            entities.first().id == chapterId &&
                            entities.first().novelId == novelId &&
                            entities.first().chapterNumber == 1f
                    }
                )
            }
        }

    @Test
    fun `getChapterList on Error does not call upsertChapters`() =
        runTest(UnconfinedTestDispatcher()) {
            val error = SourceResult.Error(SourceException.NetworkException("connection refused"))
            coEvery { source.getChapterList(novelUrl) } returns error

            val result = repository.getChapterList(sourceId, novelUrl, novelId)

            assertTrue(result is SourceResult.Error)
            coVerify(exactly = 0) { chapterDao.upsertChapters(any()) }
        }

    @Test
    fun `getChapterList returns the SourceResult from source unchanged`() =
        runTest(UnconfinedTestDispatcher()) {
            val chapters = listOf(domainChapter)
            coEvery { source.getChapterList(novelUrl) } returns SourceResult.Success(chapters)
            coEvery { chapterDao.getChaptersByNovelId(novelId) } returns flowOf(listOf(chapterEntity))

            val result = repository.getChapterList(sourceId, novelUrl, novelId)

            assertEquals(SourceResult.Success(chapters), result)
        }

    // ── getCachedChapters ─────────────────────────────────────────────────────

    @Test
    fun `getCachedChapters emits mapped domain chapters from dao Flow`() =
        runTest(UnconfinedTestDispatcher()) {
            every { chapterDao.getChaptersByNovelId(novelId) } returns flowOf(listOf(chapterEntity))

            repository.getCachedChapters(novelId).test {
                val emitted = awaitItem()
                assertEquals(1, emitted.size)
                val chapter = emitted.first()
                assertEquals(chapterId, chapter.id)
                assertEquals(novelId, chapter.novelId)
                assertEquals(1f, chapter.chapterNumber)
                assertEquals(DownloadState.NOT_DOWNLOADED, chapter.downloadState)
                awaitComplete()
            }
        }

    @Test
    fun `getCachedChapters emits empty list when dao has no chapters`() =
        runTest(UnconfinedTestDispatcher()) {
            every { chapterDao.getChaptersByNovelId(novelId) } returns flowOf(emptyList())

            repository.getCachedChapters(novelId).test {
                val emitted = awaitItem()
                assertTrue(emitted.isEmpty())
                awaitComplete()
            }
        }

    @Test
    fun `getCachedChapters correctly maps isRead flag from entity`() =
        runTest(UnconfinedTestDispatcher()) {
            val readEntity = chapterEntity.copy(isRead = true)
            every { chapterDao.getChaptersByNovelId(novelId) } returns flowOf(listOf(readEntity))

            repository.getCachedChapters(novelId).test {
                val emitted = awaitItem()
                assertTrue(emitted.first().isRead)
                awaitComplete()
            }
        }

    // ── getChapterById ────────────────────────────────────────────────────────

    @Test
    fun `getChapterById maps entity to domain chapter when found`() =
        runTest(UnconfinedTestDispatcher()) {
            coEvery { chapterDao.getChapterById(chapterId) } returns chapterEntity

            val result = repository.getChapterById(chapterId)

            assertNotNull(result)
            assertEquals(chapterId, result!!.id)
            assertEquals(novelId, result.novelId)
            assertEquals("Chapter 1", result.title)
            assertEquals(DownloadState.NOT_DOWNLOADED, result.downloadState)
        }

    @Test
    fun `getChapterById returns null when entity not found`() =
        runTest(UnconfinedTestDispatcher()) {
            coEvery { chapterDao.getChapterById(chapterId) } returns null

            val result = repository.getChapterById(chapterId)

            assertNull(result)
        }

    @Test
    fun `getChapterById correctly maps downloaded state`() =
        runTest(UnconfinedTestDispatcher()) {
            val downloadedEntity = chapterEntity.copy(
                downloadState = "DOWNLOADED",
                localFilePath = "/storage/chapter1.html"
            )
            coEvery { chapterDao.getChapterById(chapterId) } returns downloadedEntity

            val result = repository.getChapterById(chapterId)

            assertNotNull(result)
            assertEquals(DownloadState.DOWNLOADED, result!!.downloadState)
            assertEquals("/storage/chapter1.html", result.localFilePath)
        }

    // ── markChapterRead ───────────────────────────────────────────────────────

    @Test
    fun `markChapterRead delegates to chapterDao markRead`() =
        runTest(UnconfinedTestDispatcher()) {
            repository.markChapterRead(chapterId)

            coVerify(exactly = 1) { chapterDao.markRead(chapterId) }
        }

    // ── updateDownloadState ───────────────────────────────────────────────────

    @Test
    fun `updateDownloadState passes enum name and path to dao`() =
        runTest(UnconfinedTestDispatcher()) {
            val localPath = "/storage/chapter1.html"

            repository.updateDownloadState(chapterId, DownloadState.DOWNLOADED, localPath)

            coVerify(exactly = 1) {
                chapterDao.updateDownloadState(chapterId, "DOWNLOADED", localPath)
            }
        }

    @Test
    fun `updateDownloadState passes null path when not downloaded`() =
        runTest(UnconfinedTestDispatcher()) {
            repository.updateDownloadState(chapterId, DownloadState.NOT_DOWNLOADED, null)

            coVerify(exactly = 1) {
                chapterDao.updateDownloadState(chapterId, "NOT_DOWNLOADED", null)
            }
        }

    @Test
    fun `updateDownloadState passes FAILED enum name with null path`() =
        runTest(UnconfinedTestDispatcher()) {
            repository.updateDownloadState(chapterId, DownloadState.FAILED, null)

            coVerify(exactly = 1) {
                chapterDao.updateDownloadState(chapterId, "FAILED", null)
            }
        }
}
