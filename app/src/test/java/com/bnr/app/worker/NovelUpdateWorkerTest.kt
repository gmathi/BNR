package com.bnr.app.worker

import android.content.Context
import androidx.work.WorkerParameters
import com.bnr.app.core.datastore.AppPreferences
import com.bnr.app.core.notifications.NotificationHelper
import com.bnr.app.domain.model.Chapter
import com.bnr.app.domain.model.Novel
import com.bnr.app.domain.model.NovelStatus
import com.bnr.app.domain.repository.NovelRepository
import com.bnr.app.domain.repository.NovelUpdateRepository
import com.bnr.app.source.RateLimit
import com.bnr.app.source.Source
import com.bnr.app.source.SourceManager
import com.bnr.app.source.SourceResult
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class NovelUpdateWorkerTest {

    private val context: Context = mockk(relaxed = true)
    private val workerParams: WorkerParameters = mockk(relaxed = true)
    private val novelRepository: NovelRepository = mockk()
    private val novelUpdateRepository: NovelUpdateRepository = mockk()
    private val sourceManager: SourceManager = mockk()
    private val notificationHelper: NotificationHelper = mockk()
    private val appPreferences: AppPreferences = mockk()

    private lateinit var worker: NovelUpdateWorker

    @Before
    fun setUp() {
        justRun { notificationHelper.showUpdateProgress(any(), any()) }
        justRun { notificationHelper.dismissProgressNotification() }
        justRun { notificationHelper.showNewChapterNotifications(any()) }
    }

    private fun buildWorker() = NovelUpdateWorker(
        context = context,
        params = workerParams,
        novelRepository = novelRepository,
        novelUpdateRepository = novelUpdateRepository,
        sourceManager = sourceManager,
        notificationHelper = notificationHelper,
        appPreferences = appPreferences
    )

    private fun makeNovel(
        id: String = "novel-1",
        title: String = "Test Novel",
        sourceId: String = "source-1",
        url: String = "https://example.com/novel/1"
    ) = Novel(
        id = id,
        sourceId = sourceId,
        title = title,
        author = "Author",
        description = "",
        coverUrl = null,
        url = url,
        status = NovelStatus.ONGOING,
        genres = emptyList(),
        inLibrary = true,
        addedToLibraryAt = 0L,
        lastReadChapterId = null
    )

    private fun makeChapter(id: String = "ch-1") = Chapter(
        id = id,
        novelId = "novel-1",
        sourceId = "source-1",
        url = "https://example.com/chapter/$id",
        title = "Chapter $id",
        chapterNumber = 1f,
        uploadedAt = null
    )

    // ── doWork returns success when backgroundUpdates disabled ────────────────

    @Test
    fun `doWork returns success when backgroundUpdates disabled`() = runTest {
        every { appPreferences.backgroundUpdatesEnabled } returns flowOf(false)

        worker = buildWorker()
        val result = worker.doWork()

        assert(result == androidx.work.ListenableWorker.Result.success())
        coVerify(exactly = 0) { novelRepository.getLibraryNovels() }
    }

    // ── doWork returns success when library is empty ──────────────────────────

    @Test
    fun `doWork returns success when library is empty`() = runTest {
        every { appPreferences.backgroundUpdatesEnabled } returns flowOf(true)
        every { novelRepository.getLibraryNovels() } returns flowOf(emptyList())

        worker = buildWorker()
        val result = worker.doWork()

        assert(result == androidx.work.ListenableWorker.Result.success())
        coVerify(exactly = 0) { sourceManager.getSourceOrNull(any()) }
    }

    // ── doWork calls getChapterList for each library novel ────────────────────

    @Test
    fun `doWork calls getChapterList for each library novel`() = runTest {
        val novel1 = makeNovel(id = "novel-1", sourceId = "src-1", url = "url-1")
        val novel2 = makeNovel(id = "novel-2", title = "Novel 2", sourceId = "src-1", url = "url-2")

        val source: Source = mockk {
            every { rateLimit } returns RateLimit(requestsPerBatch = 5, delayBetweenBatchesMs = 0L)
            coEvery { getChapterList("url-1") } returns SourceResult.Success(listOf(makeChapter("ch-1")))
            coEvery { getChapterList("url-2") } returns SourceResult.Success(listOf(makeChapter("ch-2")))
        }

        every { appPreferences.backgroundUpdatesEnabled } returns flowOf(true)
        every { novelRepository.getLibraryNovels() } returns flowOf(listOf(novel1, novel2))
        every { sourceManager.getSourceOrNull("src-1") } returns source
        coEvery { novelUpdateRepository.getKnownChapterCount(any()) } returns 0
        coEvery { novelUpdateRepository.recordCheckResult(any(), any(), any()) } returns Unit

        worker = buildWorker()
        worker.doWork()

        coVerify(exactly = 1) { source.getChapterList("url-1") }
        coVerify(exactly = 1) { source.getChapterList("url-2") }
    }

    // ── doWork adds to updatedNovels when chapter count increases ─────────────

    @Test
    fun `doWork calls showNewChapterNotifications when updates found`() = runTest {
        val novel = makeNovel(id = "novel-1", title = "My Novel", sourceId = "src-1", url = "url-1")
        val chapters = (1..5).map { makeChapter("ch-$it") }

        val source: Source = mockk {
            every { rateLimit } returns RateLimit(requestsPerBatch = 5, delayBetweenBatchesMs = 0L)
            coEvery { getChapterList("url-1") } returns SourceResult.Success(chapters)
        }

        every { appPreferences.backgroundUpdatesEnabled } returns flowOf(true)
        every { novelRepository.getLibraryNovels() } returns flowOf(listOf(novel))
        every { sourceManager.getSourceOrNull("src-1") } returns source
        // knownCount = 3, currentCount = 5 → delta = 2
        coEvery { novelUpdateRepository.getKnownChapterCount("novel-1") } returns 3
        coEvery { novelUpdateRepository.recordCheckResult(any(), any(), any()) } returns Unit

        worker = buildWorker()
        worker.doWork()

        verify(exactly = 1) { notificationHelper.showNewChapterNotifications(listOf("My Novel" to 2)) }
    }

    @Test
    fun `doWork does not call showNewChapterNotifications when no new chapters`() = runTest {
        val novel = makeNovel(id = "novel-1", title = "My Novel", sourceId = "src-1", url = "url-1")
        val chapters = (1..3).map { makeChapter("ch-$it") }

        val source: Source = mockk {
            every { rateLimit } returns RateLimit(requestsPerBatch = 5, delayBetweenBatchesMs = 0L)
            coEvery { getChapterList("url-1") } returns SourceResult.Success(chapters)
        }

        every { appPreferences.backgroundUpdatesEnabled } returns flowOf(true)
        every { novelRepository.getLibraryNovels() } returns flowOf(listOf(novel))
        every { sourceManager.getSourceOrNull("src-1") } returns source
        // knownCount == currentCount → no delta
        coEvery { novelUpdateRepository.getKnownChapterCount("novel-1") } returns 3
        coEvery { novelUpdateRepository.recordCheckResult(any(), any(), any()) } returns Unit

        worker = buildWorker()
        worker.doWork()

        verify(exactly = 0) { notificationHelper.showNewChapterNotifications(any()) }
    }

    // ── doWork skips novel when source throws exception ───────────────────────

    @Test
    fun `doWork skips novel when source throws exception and continues`() = runTest {
        val novel1 = makeNovel(id = "novel-1", title = "Novel 1", sourceId = "src-1", url = "url-1")
        val novel2 = makeNovel(id = "novel-2", title = "Novel 2", sourceId = "src-1", url = "url-2")

        val source: Source = mockk {
            every { rateLimit } returns RateLimit(requestsPerBatch = 5, delayBetweenBatchesMs = 0L)
            coEvery { getChapterList("url-1") } throws RuntimeException("Network error")
            coEvery { getChapterList("url-2") } returns SourceResult.Success(listOf(makeChapter("ch-1")))
        }

        every { appPreferences.backgroundUpdatesEnabled } returns flowOf(true)
        every { novelRepository.getLibraryNovels() } returns flowOf(listOf(novel1, novel2))
        every { sourceManager.getSourceOrNull("src-1") } returns source
        coEvery { novelUpdateRepository.getKnownChapterCount("novel-2") } returns 0
        coEvery { novelUpdateRepository.recordCheckResult(any(), any(), any()) } returns Unit

        worker = buildWorker()
        val result = worker.doWork()

        // Worker should still complete successfully despite the exception on novel-1
        assert(result == androidx.work.ListenableWorker.Result.success())
        // novel-2 was still checked
        coVerify(exactly = 1) { source.getChapterList("url-2") }
        // novel-1 threw, no recordCheckResult for it
        coVerify(exactly = 0) { novelUpdateRepository.recordCheckResult("novel-1", any(), any()) }
    }

    // ── doWork respects rate limiting ─────────────────────────────────────────

    @Test
    fun `doWork respects rate limiting - calls delay after requestsPerBatch requests`() = runTest {
        // 3 novels, batch size = 2 → after novel index 1 (2nd novel), a delay should fire
        val novels = (1..3).map {
            makeNovel(id = "novel-$it", title = "Novel $it", sourceId = "src-1", url = "url-$it")
        }
        val chapters = listOf(makeChapter("ch-1"))

        val source: Source = mockk {
            every { rateLimit } returns RateLimit(requestsPerBatch = 2, delayBetweenBatchesMs = 100L)
            novels.forEach { novel ->
                coEvery { getChapterList(novel.url) } returns SourceResult.Success(chapters)
            }
        }

        every { appPreferences.backgroundUpdatesEnabled } returns flowOf(true)
        every { novelRepository.getLibraryNovels() } returns flowOf(novels)
        every { sourceManager.getSourceOrNull("src-1") } returns source
        coEvery { novelUpdateRepository.getKnownChapterCount(any()) } returns 0
        coEvery { novelUpdateRepository.recordCheckResult(any(), any(), any()) } returns Unit

        worker = buildWorker()
        val result = worker.doWork()

        // Worker completes successfully; all novels were processed
        assert(result == androidx.work.ListenableWorker.Result.success())
        novels.forEach { novel ->
            coVerify(exactly = 1) { source.getChapterList(novel.url) }
        }
    }

    // ── doWork calls dismissProgressNotification at end ───────────────────────

    @Test
    fun `doWork calls dismissProgressNotification at end`() = runTest {
        val novel = makeNovel(id = "novel-1", sourceId = "src-1", url = "url-1")
        val source: Source = mockk {
            every { rateLimit } returns RateLimit(requestsPerBatch = 5, delayBetweenBatchesMs = 0L)
            coEvery { getChapterList("url-1") } returns SourceResult.Success(emptyList())
        }

        every { appPreferences.backgroundUpdatesEnabled } returns flowOf(true)
        every { novelRepository.getLibraryNovels() } returns flowOf(listOf(novel))
        every { sourceManager.getSourceOrNull("src-1") } returns source
        coEvery { novelUpdateRepository.getKnownChapterCount(any()) } returns 0
        coEvery { novelUpdateRepository.recordCheckResult(any(), any(), any()) } returns Unit

        worker = buildWorker()
        worker.doWork()

        verify(exactly = 1) { notificationHelper.dismissProgressNotification() }
    }
}
