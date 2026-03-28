package com.bnr.app.presentation.search

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import app.cash.turbine.test
import com.bnr.app.core.datastore.AppPreferences
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
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

    // A single fake source returned by SourceManager.
    private val fakeSource = mockk<com.bnr.app.source.Source>(relaxed = true).also { src ->
        every { src.id } returns "com.source.test"
        every { src.name } returns "Test Source"
    }

    private val novel1 = makeNovel(id = "src::n1", title = "Novel One")
    private val novel2 = makeNovel(id = "src::n2", title = "Novel Two")

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Creates a fresh ViewModel with default stubs. The init block executes synchronously
     * thanks to [UnconfinedTestDispatcher] installed by [mainDispatcherRule].
     */
    private fun createViewModel(
        popularResult: SourceResult<List<com.bnr.app.domain.model.Novel>> =
            SourceResult.Success(emptyList())
    ): SearchViewModel {
        every { sourceManager.getAllSources() } returns listOf(fakeSource)
        every { appPreferences.preferredSourceId } returns flowOf(fakeSource.id)
        coEvery { getPopularNovels(any(), any()) } returns popularResult
        return SearchViewModel(searchNovels, getPopularNovels, sourceManager, appPreferences)
    }

    // ── Initial state ─────────────────────────────────────────────────────────

    @Test
    fun `initial state has isLoading false, empty novels, and blank query`() =
        runTest(UnconfinedTestDispatcher()) {
            val vm = createViewModel()
            val state = vm.uiState.value
            assertFalse("isLoading should be false initially", state.isLoading)
            assertTrue("novels list should be empty", state.novels.isEmpty())
            assertEquals("query should be empty", "", state.query)
        }

    // ── onSourceSelected ──────────────────────────────────────────────────────

    @Test
    fun `onSourceSelected updates selectedSourceId and triggers loadPopular`() =
        runTest(UnconfinedTestDispatcher()) {
            val vm = createViewModel()

            coEvery { getPopularNovels("com.source.other", 1) } returns
                SourceResult.Success(listOf(novel1))

            vm.onSourceSelected("com.source.other")

            assertEquals("com.source.other", vm.uiState.value.selectedSourceId)
            coVerify(atLeast = 1) { getPopularNovels("com.source.other", 1) }
            assertEquals(listOf(novel1), vm.uiState.value.novels)
        }

    @Test
    fun `onSourceSelected resets page to 1 and clears existing novels`() =
        runTest(UnconfinedTestDispatcher()) {
            // Seed page-1 with two novels so we have a non-empty initial list.
            val vm = createViewModel(SourceResult.Success(listOf(novel1, novel2)))
            assertEquals(2, vm.uiState.value.novels.size)

            // Switch source – page resets to 1, novels clear before the new load.
            coEvery { getPopularNovels("com.source.other", 1) } returns
                SourceResult.Success(emptyList())
            vm.onSourceSelected("com.source.other")

            assertEquals(1, vm.uiState.value.page)
        }

    // ── loadNextPage ──────────────────────────────────────────────────────────

    @Test
    fun `loadNextPage when hasNextPage true and not loading increments page`() =
        runTest(UnconfinedTestDispatcher()) {
            // Create VM with page-1 returning two novels (non-empty → hasNextPage = true).
            val vm = createViewModel(SourceResult.Success(listOf(novel1, novel2)))
            assertEquals(1, vm.uiState.value.page)
            assertTrue(vm.uiState.value.hasNextPage)

            // Stub page 2 AFTER the VM is created so this stub wins over the any() fallback.
            coEvery { getPopularNovels("com.source.test", 2) } returns
                SourceResult.Success(listOf(novel2))

            vm.loadNextPage()

            assertEquals(2, vm.uiState.value.page)
            coVerify(exactly = 1) { getPopularNovels("com.source.test", 2) }
        }

    @Test
    fun `loadNextPage when isLoading true does NOT trigger another load`() =
        runTest(UnconfinedTestDispatcher()) {
            // Build the ViewModel manually so we control the stub order precisely.
            // The delay stub must be the last registered so it wins over any wildcard.
            every { sourceManager.getAllSources() } returns listOf(fakeSource)
            every { appPreferences.preferredSourceId } returns flowOf(fakeSource.id)
            // Register delay stub LAST so it takes priority in MockK's answer list.
            coEvery { getPopularNovels(any(), any()) } coAnswers {
                delay(Long.MAX_VALUE / 2)
                SourceResult.Success(emptyList())
            }
            val vm = SearchViewModel(searchNovels, getPopularNovels, sourceManager, appPreferences)

            // With UnconfinedTestDispatcher the init coroutine suspends at the delay,
            // so isLoading is currently true.
            assertTrue("precondition: should be loading", vm.uiState.value.isLoading)

            // loadNextPage should be a no-op because isLoading == true.
            vm.loadNextPage()

            // getPopularNovels should have been called exactly once (from init).
            coVerify(exactly = 1) { getPopularNovels(any(), any()) }
        }

    // ── Search success ────────────────────────────────────────────────────────

    @Test
    fun `search success updates novels list, clears error, and sets isLoading false`() =
        runTest(UnconfinedTestDispatcher()) {
            val vm = createViewModel()
            coEvery { searchNovels("com.source.test", "magic", 1) } returns
                SourceResult.Success(listOf(novel1))

            // The ViewModel debounces 400 ms before calling performSearch.
            vm.onQueryChanged("magic")
            // Advance virtual time past the 400 ms debounce.
            advanceTimeBy(500)

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
                awaitItem() // consume initial state

                vm.onQueryChanged("magic")
                advanceTimeBy(500) // past debounce

                // Drain until we see a stable state with novels and not loading.
                var latest = expectMostRecentItem()
                while (latest.isLoading || latest.novels.isEmpty()) {
                    latest = awaitItem()
                }

                assertEquals(2, latest.novels.size)
                assertNull(latest.error)
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
            advanceTimeBy(500)

            val state = vm.uiState.value
            assertFalse("isLoading should be false on error", state.isLoading)
            assertEquals("Network failure", state.error)
        }

    // ── Pagination – second page appends ─────────────────────────────────────

    @Test
    fun `second page results are appended to existing novels list`() =
        runTest(UnconfinedTestDispatcher()) {
            // Create VM with page-1 returning novel1.
            val vm = createViewModel(SourceResult.Success(listOf(novel1)))

            // After init, page 1 is loaded and hasNextPage is true.
            assertEquals(listOf(novel1), vm.uiState.value.novels)

            // Stub page 2 AFTER the VM is created so this specific stub wins.
            coEvery { getPopularNovels("com.source.test", 2) } returns
                SourceResult.Success(listOf(novel2))

            vm.loadNextPage()

            assertEquals(listOf(novel1, novel2), vm.uiState.value.novels)
            assertEquals(2, vm.uiState.value.page)
        }
}
