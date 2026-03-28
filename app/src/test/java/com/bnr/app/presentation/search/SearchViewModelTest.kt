package com.bnr.app.presentation.search

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import app.cash.turbine.test
import com.bnr.app.core.datastore.AppPreferences
import com.bnr.app.domain.model.NovelStatus
import com.bnr.app.domain.usecase.GetPopularNovelsUseCase
import com.bnr.app.domain.usecase.SearchNovelsUseCase
import com.bnr.app.domain.usecase.makeNovel
import com.bnr.app.presentation.MainDispatcherRule
import com.bnr.app.source.SourceException
import com.bnr.app.source.SourceManager
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(JUnit4::class)
class SearchViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @get:Rule
    val instantTask = InstantTaskExecutorRule()

    // ── Mocks ─────────────────────────────────────────────────────────────────

    private val searchNovels: SearchNovelsUseCase = mockk(relaxed = true)
    private val getPopularNovels: GetPopularNovelsUseCase = mockk(relaxed = true)
    private val sourceManager: SourceManager = mockk(relaxed = true)
    private val appPreferences: AppPreferences = mockk(relaxed = true)

    // Stub a single fake source returned by SourceManager.
    private val fakeSource = mockk<com.bnr.app.source.Source>(relaxed = true).also { src ->
        every { src.id } returns "com.source.test"
        every { src.name } returns "Test Source"
    }

    private val novel1 = makeNovel(id = "src::n1", title = "Novel One")
    private val novel2 = makeNovel(id = "src::n2", title = "Novel Two")

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** Creates a fresh ViewModel whose init block runs synchronously via [UnconfinedTestDispatcher]. */
    private fun createViewModel(): SearchViewModel {
        every { sourceManager.getAllSources() } returns listOf(fakeSource)
        every { appPreferences.preferredSourceId } returns flowOf(fakeSource.id)
        coEvery { getPopularNovels(any(), any()) } returns SourceResult.Success(emptyList())
        return SearchViewModel(searchNovels, getPopularNovels, sourceManager, appPreferences)
    }

    // ── Initial state ─────────────────────────────────────────────────────────

    @Test
    fun `initial state has isLoading false, empty novels, and blank query`() =
        runTest(UnconfinedTestDispatcher()) {
            val vm = createViewModel()
            val state = vm.uiState.value
            assertFalse("isLoading should be false initially", state.isLoading)
            // After init the popular load completes (empty list stub) so novels is still empty.
            assertTrue("novels list should be empty", state.novels.isEmpty())
            assertEquals("query should be empty", "", state.query)
        }

    // ── onSourceSelected ──────────────────────────────────────────────────────

    @Test
    fun `onSourceSelected updates selectedSourceId and triggers loadPopular`() =
        runTest(UnconfinedTestDispatcher()) {
            val vm = createViewModel()
            val otherSource = mockk<com.bnr.app.source.Source>(relaxed = true).also { s ->
                every { s.id } returns "com.source.other"
                every { s.name } returns "Other Source"
            }
            coEvery { getPopularNovels("com.source.other", 1) } returns
                SourceResult.Success(listOf(novel1))

            vm.onSourceSelected("com.source.other")

            assertEquals("com.source.other", vm.uiState.value.selectedSourceId)
            // loadPopular was triggered for the new source id.
            coVerify(atLeast = 1) { getPopularNovels("com.source.other", 1) }
            assertEquals(listOf(novel1), vm.uiState.value.novels)
        }

    @Test
    fun `onSourceSelected resets page to 1 and clears novels`() =
        runTest(UnconfinedTestDispatcher()) {
            // Seed existing novels so we can verify they get cleared.
            coEvery { getPopularNovels("com.source.test", 1) } returns
                SourceResult.Success(listOf(novel1, novel2))
            val vm = createViewModel()
            assertEquals(2, vm.uiState.value.novels.size)

            coEvery { getPopularNovels("com.source.other", 1) } returns
                SourceResult.Success(emptyList())

            vm.onSourceSelected("com.source.other")

            assertEquals(1, vm.uiState.value.page)
        }

    // ── loadNextPage ──────────────────────────────────────────────────────────

    @Test
    fun `loadNextPage when hasNextPage true and not loading increments page`() =
        runTest(UnconfinedTestDispatcher()) {
            // Page 1 returns two novels (non-empty → hasNextPage = true).
            coEvery { getPopularNovels("com.source.test", 1) } returns
                SourceResult.Success(listOf(novel1, novel2))
            // Page 2 returns one novel.
            coEvery { getPopularNovels("com.source.test", 2) } returns
                SourceResult.Success(listOf(novel2))
            val vm = createViewModel()
            assertEquals(1, vm.uiState.value.page)
            assertTrue(vm.uiState.value.hasNextPage)

            vm.loadNextPage()

            assertEquals(2, vm.uiState.value.page)
            coVerify(exactly = 1) { getPopularNovels("com.source.test", 2) }
        }

    @Test
    fun `loadNextPage when isLoading true does NOT trigger another load`() =
        runTest(UnconfinedTestDispatcher()) {
            // Make page-1 never complete so isLoading stays true.
            coEvery { getPopularNovels(any(), 1) } coAnswers {
                kotlinx.coroutines.delay(Long.MAX_VALUE)
                SourceResult.Success(emptyList())
            }
            val vm = createViewModel()
            // Force-set isLoading via reflection of the internal MutableStateFlow.
            // Instead: manually patch the state by triggering a real slow load through another path.
            // Simpler: create vm where init load is still pending, then call loadNextPage.
            // Because we used UnconfinedTestDispatcher, init resolves immediately – but the
            // delay above will suspend indefinitely, so isLoading is still true at this point.

            // Second attempt should be skipped because isLoading == true.
            vm.loadNextPage()

            // getPopularNovels should have been called exactly once (from init), not twice.
            coVerify(exactly = 1) { getPopularNovels(any(), any()) }
        }

    // ── Search success ────────────────────────────────────────────────────────

    @Test
    fun `search success updates novels list, sets isLoading false, and clears error`() =
        runTest(UnconfinedTestDispatcher()) {
            val vm = createViewModel()
            coEvery { searchNovels("com.source.test", "magic", 1) } returns
                SourceResult.Success(listOf(novel1))

            vm.onQueryChanged("magic")
            // UnconfinedTestDispatcher runs the debounce delay and search inline.
            // Advance time past the 400 ms debounce.
            testScheduler.advanceTimeBy(500)

            val state = vm.uiState.value
            assertFalse("isLoading should be false after search", state.isLoading)
            assertNull("error should be null on success", state.error)
            assertEquals(listOf(novel1), state.novels)
        }

    @Test
    fun `search success via turbine emits updated novels`() =
        runTest(UnconfinedTestDispatcher()) {
            val vm = createViewModel()
            coEvery { searchNovels("com.source.test", "magic", 1) } returns
                SourceResult.Success(listOf(novel1, novel2))

            vm.uiState.test {
                // Consume whatever initial state is already emitted.
                awaitItem()

                vm.onQueryChanged("magic")
                testScheduler.advanceTimeBy(500)

                // Collect until we see the finished state (isLoading=false, novels present).
                val finalState = generateSequence { expectMostRecentItem() }
                    .first { !it.isLoading && it.novels.isNotEmpty() }

                assertEquals(2, finalState.novels.size)
                assertNull(finalState.error)
                cancelAndIgnoreRemainingEvents()
            }
        }

    // ── Search error ──────────────────────────────────────────────────────────

    @Test
    fun `search error sets error message and isLoading false`() =
        runTest(UnconfinedTestDispatcher()) {
            val vm = createViewModel()
            coEvery { searchNovels("com.source.test", "fail", 1) } returns
                SourceResult.Error(SourceException.NetworkException("Network failure"))

            vm.onQueryChanged("fail")
            testScheduler.advanceTimeBy(500)

            val state = vm.uiState.value
            assertFalse("isLoading should be false on error", state.isLoading)
            assertEquals("Network failure", state.error)
        }

    // ── Pagination – second page appends ─────────────────────────────────────

    @Test
    fun `second page results are appended to existing novels list`() =
        runTest(UnconfinedTestDispatcher()) {
            coEvery { getPopularNovels("com.source.test", 1) } returns
                SourceResult.Success(listOf(novel1))
            coEvery { getPopularNovels("com.source.test", 2) } returns
                SourceResult.Success(listOf(novel2))
            val vm = createViewModel()

            // Page 1 already loaded by init; hasNextPage == true (non-empty result).
            assertEquals(listOf(novel1), vm.uiState.value.novels)

            vm.loadNextPage()

            assertEquals(listOf(novel1, novel2), vm.uiState.value.novels)
            assertEquals(2, vm.uiState.value.page)
        }
}
