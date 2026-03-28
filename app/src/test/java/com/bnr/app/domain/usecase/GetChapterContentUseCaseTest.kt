package com.bnr.app.domain.usecase

import com.bnr.app.data.local.saf.SafStorageManager
import com.bnr.app.domain.model.DownloadState
import com.bnr.app.domain.repository.ChapterRepository
import com.bnr.app.source.SourceException
import com.bnr.app.source.SourceResult
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class GetChapterContentUseCaseTest {

    private lateinit var chapterRepository: ChapterRepository
    private lateinit var safStorageManager: SafStorageManager
    private lateinit var useCase: GetChapterContentUseCase

    @Before
    fun setUp() {
        chapterRepository = mockk()
        safStorageManager = mockk()
        useCase = GetChapterContentUseCase(chapterRepository, safStorageManager)
    }

    // -------------------------------------------------------------------------
    // Offline-first: DOWNLOADED + localFilePath present + SAF returns content
    // -------------------------------------------------------------------------

    @Test
    fun `when chapter is DOWNLOADED and SAF returns content, returns local content without network call`() =
        runTest {
            val localPath = "content://authority/tree/primary/document/novels_novel__1.txt"
            val localContent = makeChapterContent(chapterId = "novel::1")
            val chapter = makeChapter(
                downloadState = DownloadState.DOWNLOADED,
                localFilePath = localPath
            )

            coEvery { safStorageManager.readChapterContent(localPath) } returns localContent

            val result = useCase(chapter)

            assertTrue(result is SourceResult.Success)
            assertEquals(localContent, (result as SourceResult.Success).data)

            // Network must NOT be touched
            coVerify(exactly = 0) {
                chapterRepository.getChapterContent(any(), any(), any())
            }
        }

    // -------------------------------------------------------------------------
    // SAF returns null → fall back to network
    // -------------------------------------------------------------------------

    @Test
    fun `when chapter is DOWNLOADED but SAF returns null, falls back to network`() = runTest {
        val localPath = "content://authority/tree/primary/document/novels_novel__1.txt"
        val networkContent = makeChapterContent(chapterId = "novel::1", title = "Network Title")
        val chapter = makeChapter(
            downloadState = DownloadState.DOWNLOADED,
            localFilePath = localPath
        )

        coEvery { safStorageManager.readChapterContent(localPath) } returns null
        coEvery {
            chapterRepository.getChapterContent(
                chapter.sourceId,
                chapter.url,
                chapter.id
            )
        } returns SourceResult.Success(networkContent)

        val result = useCase(chapter)

        assertTrue(result is SourceResult.Success)
        assertEquals(networkContent, (result as SourceResult.Success).data)
        coVerify(exactly = 1) {
            chapterRepository.getChapterContent(chapter.sourceId, chapter.url, chapter.id)
        }
    }

    // -------------------------------------------------------------------------
    // downloadState != DOWNLOADED → calls network directly (no SAF access)
    // -------------------------------------------------------------------------

    @Test
    fun `when chapter is NOT_DOWNLOADED, calls network directly without touching SAF`() = runTest {
        val networkContent = makeChapterContent()
        val chapter = makeChapter(downloadState = DownloadState.NOT_DOWNLOADED, localFilePath = null)

        coEvery {
            chapterRepository.getChapterContent(
                chapter.sourceId,
                chapter.url,
                chapter.id
            )
        } returns SourceResult.Success(networkContent)

        val result = useCase(chapter)

        assertTrue(result is SourceResult.Success)
        assertEquals(networkContent, (result as SourceResult.Success).data)

        coVerify(exactly = 0) { safStorageManager.readChapterContent(any()) }
        coVerify(exactly = 1) {
            chapterRepository.getChapterContent(chapter.sourceId, chapter.url, chapter.id)
        }
    }

    @Test
    fun `when chapter is QUEUED, calls network directly without touching SAF`() = runTest {
        val networkContent = makeChapterContent()
        val chapter = makeChapter(downloadState = DownloadState.QUEUED, localFilePath = null)

        coEvery {
            chapterRepository.getChapterContent(
                chapter.sourceId,
                chapter.url,
                chapter.id
            )
        } returns SourceResult.Success(networkContent)

        useCase(chapter)

        coVerify(exactly = 0) { safStorageManager.readChapterContent(any()) }
        coVerify(exactly = 1) {
            chapterRepository.getChapterContent(chapter.sourceId, chapter.url, chapter.id)
        }
    }

    @Test
    fun `when chapter is DOWNLOADING, calls network directly without touching SAF`() = runTest {
        val networkContent = makeChapterContent()
        val chapter = makeChapter(downloadState = DownloadState.DOWNLOADING, localFilePath = null)

        coEvery {
            chapterRepository.getChapterContent(
                chapter.sourceId,
                chapter.url,
                chapter.id
            )
        } returns SourceResult.Success(networkContent)

        useCase(chapter)

        coVerify(exactly = 0) { safStorageManager.readChapterContent(any()) }
    }

    @Test
    fun `when chapter is FAILED, calls network directly without touching SAF`() = runTest {
        val networkContent = makeChapterContent()
        val chapter = makeChapter(downloadState = DownloadState.FAILED, localFilePath = null)

        coEvery {
            chapterRepository.getChapterContent(
                chapter.sourceId,
                chapter.url,
                chapter.id
            )
        } returns SourceResult.Success(networkContent)

        useCase(chapter)

        coVerify(exactly = 0) { safStorageManager.readChapterContent(any()) }
    }

    // -------------------------------------------------------------------------
    // DOWNLOADED but localFilePath is null → skip SAF, go to network
    // -------------------------------------------------------------------------

    @Test
    fun `when chapter is DOWNLOADED but localFilePath is null, calls network without touching SAF`() =
        runTest {
            val networkContent = makeChapterContent()
            val chapter = makeChapter(
                downloadState = DownloadState.DOWNLOADED,
                localFilePath = null
            )

            coEvery {
                chapterRepository.getChapterContent(
                    chapter.sourceId,
                    chapter.url,
                    chapter.id
                )
            } returns SourceResult.Success(networkContent)

            val result = useCase(chapter)

            assertTrue(result is SourceResult.Success)
            coVerify(exactly = 0) { safStorageManager.readChapterContent(any()) }
        }

    // -------------------------------------------------------------------------
    // Network returns error → propagates SourceResult.Error
    // -------------------------------------------------------------------------

    @Test
    fun `when network returns error, propagates SourceResult Error`() = runTest {
        val chapter = makeChapter(downloadState = DownloadState.NOT_DOWNLOADED)
        val networkError = SourceResult.Error(
            SourceException.NetworkException("Connection timed out")
        )

        coEvery {
            chapterRepository.getChapterContent(
                chapter.sourceId,
                chapter.url,
                chapter.id
            )
        } returns networkError

        val result = useCase(chapter)

        assertTrue(result is SourceResult.Error)
        assertSame(networkError, result)
    }

    @Test
    fun `when DOWNLOADED + SAF null and network returns error, propagates SourceResult Error`() =
        runTest {
            val localPath = "content://authority/novels/novel__1.txt"
            val chapter = makeChapter(
                downloadState = DownloadState.DOWNLOADED,
                localFilePath = localPath
            )
            val networkError = SourceResult.Error(
                SourceException.NetworkException("No connection")
            )

            coEvery { safStorageManager.readChapterContent(localPath) } returns null
            coEvery {
                chapterRepository.getChapterContent(
                    chapter.sourceId,
                    chapter.url,
                    chapter.id
                )
            } returns networkError

            val result = useCase(chapter)

            assertTrue(result is SourceResult.Error)
        }

    // -------------------------------------------------------------------------
    // markChapterRead is NOT called by this use case
    // -------------------------------------------------------------------------

    @Test
    fun `markChapterRead is never called by GetChapterContentUseCase`() = runTest {
        val networkContent = makeChapterContent()
        val chapter = makeChapter(downloadState = DownloadState.NOT_DOWNLOADED)

        coEvery {
            chapterRepository.getChapterContent(any(), any(), any())
        } returns SourceResult.Success(networkContent)

        useCase(chapter)

        coVerify(exactly = 0) { chapterRepository.markChapterRead(any()) }
    }
}
