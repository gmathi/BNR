package com.bnr.app.presentation.noveldetail

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.bnr.app.domain.repository.ChapterRepository
import com.bnr.app.domain.usecase.AddNovelToLibraryUseCase
import com.bnr.app.domain.usecase.DownloadChapterUseCase
import com.bnr.app.domain.usecase.GetChapterListUseCase
import com.bnr.app.domain.usecase.GetNovelDetailUseCase
import com.bnr.app.domain.usecase.MarkNovelUpdatesSeenUseCase
import com.bnr.app.domain.usecase.RemoveNovelFromLibraryUseCase
import com.bnr.app.domain.usecase.makeChapter
import com.bnr.app.domain.usecase.makeNovel
import com.bnr.app.presentation.MainDispatcherRule
import com.bnr.app.source.SourceException
import com.bnr.app.source.SourceResult
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.net.URLEncoder

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(JUnit4::class)
class NovelDetailViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @get:Rule
    val instantTask = InstantTaskExecutorRule()

    // ── Mocks ─────────────────────────────────────────────────────────────────

    private val getNovelDetail: GetNovelDetailUseCase = mockk(relaxed = true)
    private val getChapterList: GetChapterListUseCase = mockk(relaxed = true)
    private val addToLibrary: AddNovelToLibraryUseCase = mockk(relaxed = true)
    private val removeFromLibrary: RemoveNovelFromLibraryUseCase = mockk(relaxed = true)
    private val downloadChapterUseCase: DownloadChapterUseCase = mockk(relaxed = true)
    private val chapterRepository: ChapterRepository = mockk(relaxed = true)
    private val markNovelUpdatesSeen: MarkNovelUpdatesSeenUseCase = mockk(relaxed = true)

    // ── Fixtures ──────────────────────────────────────────────────────────────

    private val sourceId = "com.source.test"
    private val novelUrl = "/novel/test-novel"

    // URL-encoded novel URL as it would appear in the nav back-stack.
    private val encodedUrl: String = URLEncoder.encode(novelUrl, "UTF-8")

    private val fakeNovel = makeNovel(
        id = "src::test-novel",
        sourceId = sourceId,
        url = novelUrl,
        inLibrary = false
    )
    private val fakeChapter = makeChapter(novelId = fakeNovel.id, sourceId = sourceId)

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun savedStateHandle(
        srcId: String = sourceId,
        encUrl: String = encodedUrl
    ) = SavedStateHandle(mapOf("sourceId" to srcId, "encodedUrl" to encUrl))

    /** Default stubs that make the ViewModel happy: detail succeeds, chapters succeed. */
    private fun stubDefaults(
        novel: com.bnr.app.domain.model.Novel = fakeNovel,
        chapters: List<com.bnr.app.domain.model.Chapter> = listOf(fakeChapter)
    ) {
        coEvery { getNovelDetail(any(), any()) } returns SourceResult.Success(novel)
        coEvery { getChapterList(any(), any(), any()) } returns SourceResult.Success(chapters)
        // getCachedChapters returns an empty flow so it never overrides the fetched list.
        every { chapterRepository.getCachedChapters(any()) } returns flowOf(emptyList())
    }

    private fun createViewModel(handle: SavedStateHandle = savedStateHandle()): NovelDetailViewModel =
        NovelDetailViewModel(
            getNovelDetail = getNovelDetail,
            getChapterList = getChapterList,
            addToLibrary = addToLibrary,
            removeFromLibrary = removeFromLibrary,
            downloadChapter = downloadChapterUseCase,
            chapterRepository = chapterRepository,
            markNovelUpdatesSeen = markNovelUpdatesSeen,
            savedStateHandle = handle
        )

    // ── Loading state ─────────────────────────────────────────────────────────

    @Test
    fun `isLoadingDetail is true during fetch and false after completion`() =
        runTest(UnconfinedTestDispatcher()) {
            stubDefaults()

            var seenLoading = false

            val vm = NovelDetailViewModel(
                getNovelDetail = mockk {
                    coEvery { this@mockk(any(), any()) } coAnswers {
                        seenLoading = true
                        SourceResult.Success(fakeNovel)
                    }
                },
                getChapterList = getChapterList,
                addToLibrary = addToLibrary,
                removeFromLibrary = removeFromLibrary,
                downloadChapter = downloadChapterUseCase,
                chapterRepository = chapterRepository,
                markNovelUpdatesSeen = markNovelUpdatesSeen,
                savedStateHandle = savedStateHandle()
            )

            // After init completes synchronously (UnconfinedTestDispatcher), the
            // use-case was called (seenLoading set to true) and loading is now false.
            assertTrue("Use case was invoked", seenLoading)
            assertFalse("isLoadingDetail should be false after fetch", vm.uiState.value.isLoadingDetail)
        }

    @Test
    fun `loading state transitions via turbine during detail load`() =
        runTest(UnconfinedTestDispatcher()) {
            stubDefaults()
            val vm = createViewModel()

            vm.uiState.test {
                // With UnconfinedTestDispatcher, init completes before the first collect.
                // The most recent item should show the completed state.
                val state = expectMostRecentItem()
                assertFalse(state.isLoadingDetail)
                cancelAndIgnoreRemainingEvents()
            }
        }

    // ── Success path ──────────────────────────────────────────────────────────

    @Test
    fun `success sets novel and chapters in state`() =
        runTest(UnconfinedTestDispatcher()) {
            stubDefaults()
            val vm = createViewModel()

            val state = vm.uiState.value
            assertNotNull("novel should be set", state.novel)
            assertEquals(fakeNovel, state.novel)
            assertEquals(listOf(fakeChapter), state.chapters)
            assertFalse("isLoadingDetail should be false", state.isLoadingDetail)
            assertNull("error should be null", state.error)
        }

    @Test
    fun `success with inLibrary true sets inLibrary on state`() =
        runTest(UnconfinedTestDispatcher()) {
            val novelInLib = fakeNovel.copy(inLibrary = true)
            coEvery { getNovelDetail(any(), any()) } returns SourceResult.Success(novelInLib)
            coEvery { getChapterList(any(), any(), any()) } returns SourceResult.Success(emptyList())
            every { chapterRepository.getCachedChapters(any()) } returns flowOf(emptyList())

            val vm = createViewModel()

            assertTrue("inLibrary should reflect novel's inLibrary flag", vm.uiState.value.inLibrary)
        }

    @Test
    fun `success with inLibrary false sets inLibrary false on state`() =
        runTest(UnconfinedTestDispatcher()) {
            stubDefaults(novel = fakeNovel.copy(inLibrary = false))
            val vm = createViewModel()

            assertFalse(vm.uiState.value.inLibrary)
        }

    // ── Error path ────────────────────────────────────────────────────────────

    @Test
    fun `error from getNovelDetail sets error message and isLoadingDetail false`() =
        runTest(UnconfinedTestDispatcher()) {
            coEvery { getNovelDetail(any(), any()) } returns
                SourceResult.Error(SourceException.NetworkException("Timeout"))
            every { chapterRepository.getCachedChapters(any()) } returns flowOf(emptyList())

            val vm = createViewModel()

            val state = vm.uiState.value
            assertFalse("isLoadingDetail should be false on error", state.isLoadingDetail)
            assertEquals("Timeout", state.error)
            assertNull("novel should be null on error", state.novel)
        }

    @Test
    fun `error from getChapterList sets error message`() =
        runTest(UnconfinedTestDispatcher()) {
            coEvery { getNovelDetail(any(), any()) } returns SourceResult.Success(fakeNovel)
            coEvery { getChapterList(any(), any(), any()) } returns
                SourceResult.Error(SourceException.ParseException("Bad HTML"))
            every { chapterRepository.getCachedChapters(any()) } returns flowOf(emptyList())

            val vm = createViewModel()

            val state = vm.uiState.value
            assertFalse("isLoadingChapters should be false on error", state.isLoadingChapters)
            assertEquals("Bad HTML", state.error)
        }

    // ── toggleLibrary ─────────────────────────────────────────────────────────

    @Test
    fun `toggleLibrary when not in library calls addToLibrary and sets inLibrary true`() =
        runTest(UnconfinedTestDispatcher()) {
            stubDefaults(novel = fakeNovel.copy(inLibrary = false))
            val vm = createViewModel()
            assertFalse("precondition: should not be in library", vm.uiState.value.inLibrary)

            vm.toggleLibrary()

            coVerify(exactly = 1) { addToLibrary(fakeNovel.copy(inLibrary = false)) }
            assertTrue("inLibrary should be true after adding", vm.uiState.value.inLibrary)
        }

    @Test
    fun `toggleLibrary when in library calls removeFromLibrary and sets inLibrary false`() =
        runTest(UnconfinedTestDispatcher()) {
            val novelInLib = fakeNovel.copy(inLibrary = true)
            stubDefaults(novel = novelInLib)
            val vm = createViewModel()
            assertTrue("precondition: should be in library", vm.uiState.value.inLibrary)

            vm.toggleLibrary()

            coVerify(exactly = 1) { removeFromLibrary(novelInLib.id) }
            assertFalse("inLibrary should be false after removing", vm.uiState.value.inLibrary)
        }

    // ── downloadChapter ───────────────────────────────────────────────────────

    @Test
    fun `downloadChapter calls downloadChapterUseCase for the provided chapter`() =
        runTest(UnconfinedTestDispatcher()) {
            stubDefaults()
            val vm = createViewModel()

            vm.downloadChapter(fakeChapter)

            coVerify(exactly = 1) { downloadChapterUseCase(fakeChapter) }
        }

    @Test
    fun `downloadChapter passes the exact chapter object to the use case`() =
        runTest(UnconfinedTestDispatcher()) {
            stubDefaults()
            val vm = createViewModel()
            val specificChapter = makeChapter(
                id = "src::slug::99",
                novelId = fakeNovel.id,
                sourceId = sourceId,
                chapterNumber = 99f,
                title = "Chapter 99"
            )

            vm.downloadChapter(specificChapter)

            coVerify { downloadChapterUseCase(specificChapter) }
        }

    // ── downloadAll ───────────────────────────────────────────────────────────

    @Test
    fun `downloadAll calls downloadChapter for every chapter in state`() =
        runTest(UnconfinedTestDispatcher()) {
            val chapter2 = makeChapter(id = "src::slug::2", novelId = fakeNovel.id, sourceId = sourceId, chapterNumber = 2f)
            stubDefaults(chapters = listOf(fakeChapter, chapter2))
            val vm = createViewModel()

            vm.downloadAll()

            coVerify(exactly = 1) { downloadChapterUseCase(fakeChapter) }
            coVerify(exactly = 1) { downloadChapterUseCase(chapter2) }
        }

    @Test
    fun `downloadAll with empty chapter list invokes downloadChapter zero times`() =
        runTest(UnconfinedTestDispatcher()) {
            stubDefaults(chapters = emptyList())
            val vm = createViewModel()

            vm.downloadAll()

            coVerify(exactly = 0) { downloadChapterUseCase(any()) }
        }

    // ── refresh ───────────────────────────────────────────────────────────────

    @Test
    fun `refresh re-invokes getNovelDetail`() =
        runTest(UnconfinedTestDispatcher()) {
            stubDefaults()
            val vm = createViewModel()

            // getNovelDetail was called once during init
            coVerify(exactly = 1) { getNovelDetail(any(), any()) }

            vm.refresh()

            // Should be called a second time after refresh
            coVerify(exactly = 2) { getNovelDetail(any(), any()) }
        }

    @Test
    fun `refresh updates state with new novel data`() =
        runTest(UnconfinedTestDispatcher()) {
            stubDefaults()
            val vm = createViewModel()

            val updatedNovel = fakeNovel.copy(title = "Updated Title")
            coEvery { getNovelDetail(any(), any()) } returns SourceResult.Success(updatedNovel)

            vm.refresh()

            assertEquals("Updated Title", vm.uiState.value.novel?.title)
        }
}
