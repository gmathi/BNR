package com.bnr.app.presentation.explore

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import app.cash.turbine.test
import com.bnr.app.core.datastore.AppPreferences
import com.bnr.app.domain.usecase.GetLibraryNovelsUseCase
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
import kotlinx.coroutines.flow.MutableStateFlow
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
class ExploreViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @get:Rule
    val instantTask = InstantTaskExecutorRule()

    // ── Mocks ─────────────────────────────────────────────────────────────────

    private val searchNovels: SearchNovelsUseCase = mockk(relaxed = true)
    private val getPopularNovels: GetPopularNovelsUseCase = mockk(relaxed = true)
    private val getLibraryNovels: GetLibraryNovelsUseCase = mockk(relaxed = true)
    private val sourceManager: SourceManager = mockk(relaxed = true)
    private val appPreferences: AppPreferences = mockk(relaxed = true)

    private val fakeSource = mockk<com.bnr.app.source.Source>(relaxed = true).also { src ->
        every { src.id } returns "com.source.test"
        every { src.name } returns "Test Source"
    }

    private val fakeSource2 = mockk<com.bnr.app.source.Source>(relaxed = true).also { src ->
        every { src.id } returns "com.source.other"
        every { src.name } returns "Other Source"
    }

    private val novel1 = makeNovel(id = "src::n1", title = "Novel One")
    private val novel2 = makeNovel(id = "src::n2", title = "Novel Two")

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Creates a fresh ViewModel with default stubs (single source, empty library,
     * empty popular result unless overridden).
     */
    private fun createViewModel(
        popularResult: SourceResult<List<com.bnr.app.domain.model.Novel>> =
            SourceResult.Success(emptyList()),
        sources: List<com.bnr.app.source.Source> = listOf(fakeSource),
        libraryFlow: kotlinx.coroutines.flow.Flow<List<com.bnr.app.domain.model.Novel>> =
            flowOf(emptyList())
    ): ExploreViewModel {
        every { sourceManager.getAllSources() } returns sources
        every { appPreferences.preferredSourceId } returns flowOf(fakeSource.id)
        every { getLibraryNovels() } returns libraryFlow
        coEvery { getPopularNovels(any(), any()) } returns popularResult
        return ExploreViewModel(searchNovels, getPopularNovels, getLibraryNovels, sourceManager, appPreferences)
    }

    // ── 1. Initial state ──────────────────────────────────────────────────────

    @Test
    fun `initial state isSearchMode false libraryIds empty sourceResults empty`() =
        runTest(UnconfinedTestDispatcher()) {
            val vm = createViewModel()
            val state = vm.uiState.value
            assertFalse("isSearchMode should be false initially", state.isSearchMode)
            assertTrue("libraryIds should be empty initially", state.libraryIds.isEmpty())
            assertTrue("sourceResults should be empty initially", state.sourceResults.isEmpty())
        }

    // ── 2. Library IDs populated from use case ────────────────────────────────

    @Test
    fun `library IDs populated from use case`() =
        runTest(UnconfinedTestDispatcher()) {
            val libraryFlow = MutableStateFlow(listOf(novel1, novel2))
            val vm = createViewModel(libraryFlow = libraryFlow)

            val state = vm.uiState.value
            assertTrue("libraryIds should contain novel1.id", novel1.id in state.libraryIds)
            assertTrue("libraryIds should contain novel2.id", novel2.id in state.libraryIds)
        }

    // ── 3. onQueryChanged blank switches to popular mode ──────────────────────

    @Test
    fun `onQueryChanged blank switches isSearchMode to false and loads popular`() =
        runTest(UnconfinedTestDispatcher()) {
            coEvery { getPopularNovels("com.source.test", 1) } returns
                SourceResult.Success(listOf(novel1))

            val vm = createViewModel()

            // First go into search mode
            vm.onQueryChanged("magic")
            advanceTimeBy(500)

            // Now clear the query
            vm.onQueryChanged("")

            assertFalse("isSearchMode should be false when query is blank", vm.uiState.value.isSearchMode)
            coVerify(atLeast = 1) { getPopularNovels("com.source.test", 1) }
        }

    // ── 4. onQueryChanged non-blank switches to search mode after debounce ────

    @Test
    fun `onQueryChanged non-blank after 400ms debounce isSearchMode true`() =
        runTest(UnconfinedTestDispatcher()) {
            coEvery { searchNovels(any(), any(), any()) } returns SourceResult.Success(emptyList())

            val vm = createViewModel()
            vm.onQueryChanged("dragon")
            advanceTimeBy(500)

            assertTrue("isSearchMode should be true after non-blank query", vm.uiState.value.isSearchMode)
        }

    // ── 5. Search fires for all sources in parallel ───────────────────────────

    @Test
    fun `search fires for all sources when query is non-blank`() =
        runTest(UnconfinedTestDispatcher()) {
            coEvery { searchNovels(any(), any(), any()) } returns SourceResult.Success(emptyList())

            val vm = createViewModel(sources = listOf(fakeSource, fakeSource2))
            vm.onQueryChanged("magic")
            advanceTimeBy(500)

            coVerify(atLeast = 1) { searchNovels("com.source.test", "magic", 1) }
            coVerify(atLeast = 1) { searchNovels("com.source.other", "magic", 1) }
        }

    // ── 6. Per-source loading state ───────────────────────────────────────────

    @Test
    fun `per-source loading state is true during search and false after completion`() =
        runTest(UnconfinedTestDispatcher()) {
            // Use a suspended stub so we can observe the loading state mid-flight
            coEvery { searchNovels("com.source.test", "magic", 1) } coAnswers {
                delay(Long.MAX_VALUE / 2)
                SourceResult.Success(emptyList())
            }

            val vm = createViewModel()

            vm.uiState.test {
                awaitItem() // initial state

                vm.onQueryChanged("magic")
                advanceTimeBy(500) // past debounce, triggers search (which then suspends)

                // Drain to the state where source is loading
                var latest = expectMostRecentItem()
                while (latest.sourceResults.none { it.isLoading }) {
                    latest = awaitItem()
                }
                assertTrue(
                    "sourceResults should have at least one source with isLoading=true",
                    latest.sourceResults.any { it.isLoading }
                )
                cancelAndIgnoreRemainingEvents()
            }
        }

    // ── 7. Per-source success ─────────────────────────────────────────────────

    @Test
    fun `per-source success novels appear in correct SourceSearchState`() =
        runTest(UnconfinedTestDispatcher()) {
            coEvery { searchNovels("com.source.test", "magic", 1) } returns
                SourceResult.Success(listOf(novel1))
            coEvery { searchNovels("com.source.other", "magic", 1) } returns
                SourceResult.Success(listOf(novel2))

            val vm = createViewModel(sources = listOf(fakeSource, fakeSource2))
            vm.onQueryChanged("magic")
            advanceTimeBy(500)

            val state = vm.uiState.value
            val src1State = state.sourceResults.find { it.sourceId == "com.source.test" }
            val src2State = state.sourceResults.find { it.sourceId == "com.source.other" }

            assertEquals(listOf(novel1), src1State?.novels)
            assertEquals(listOf(novel2), src2State?.novels)
            assertFalse("src1 should not be loading", src1State?.isLoading ?: true)
            assertFalse("src2 should not be loading", src2State?.isLoading ?: true)
        }

    // ── 8. Per-source error ───────────────────────────────────────────────────

    @Test
    fun `per-source error sets error on correct source and leaves other source unaffected`() =
        runTest(UnconfinedTestDispatcher()) {
            coEvery { searchNovels("com.source.test", "fail", 1) } returns
                SourceResult.Error(SourceException.NetworkException("Network failure"))
            coEvery { searchNovels("com.source.other", "fail", 1) } returns
                SourceResult.Success(listOf(novel1))

            val vm = createViewModel(sources = listOf(fakeSource, fakeSource2))
            vm.onQueryChanged("fail")
            advanceTimeBy(500)

            val state = vm.uiState.value
            val src1State = state.sourceResults.find { it.sourceId == "com.source.test" }
            val src2State = state.sourceResults.find { it.sourceId == "com.source.other" }

            assertEquals("Network failure", src1State?.error)
            assertFalse("src1 should not be loading", src1State?.isLoading ?: true)
            assertNull("src2 should have no error", src2State?.error)
            assertEquals(listOf(novel1), src2State?.novels)
        }

    // ── 9. loadNextSourcePage ─────────────────────────────────────────────────

    @Test
    fun `loadNextSourcePage increments page for correct source only`() =
        runTest(UnconfinedTestDispatcher()) {
            coEvery { searchNovels(any(), any(), 1) } returns SourceResult.Success(listOf(novel1))
            coEvery { searchNovels("com.source.test", "magic", 2) } returns
                SourceResult.Success(listOf(novel2))

            val vm = createViewModel(sources = listOf(fakeSource, fakeSource2))
            vm.onQueryChanged("magic")
            advanceTimeBy(500)

            // Verify page 1 is loaded for both
            val beforeState = vm.uiState.value
            assertEquals(1, beforeState.sourceResults.find { it.sourceId == "com.source.test" }?.page)
            assertEquals(1, beforeState.sourceResults.find { it.sourceId == "com.source.other" }?.page)

            // Load next page for source 1 only
            vm.loadNextSourcePage("com.source.test")

            val afterState = vm.uiState.value
            val src1After = afterState.sourceResults.find { it.sourceId == "com.source.test" }
            val src2After = afterState.sourceResults.find { it.sourceId == "com.source.other" }

            assertEquals("page should be 2 for source 1", 2, src1After?.page)
            assertEquals("page should still be 1 for source 2", 1, src2After?.page)
            // Novels should be appended for source 1
            assertEquals(listOf(novel1, novel2), src1After?.novels)
        }

    // ── 10. Popular load on init ──────────────────────────────────────────────

    @Test
    fun `popular load populates popularNovels on init`() =
        runTest(UnconfinedTestDispatcher()) {
            val vm = createViewModel(popularResult = SourceResult.Success(listOf(novel1, novel2)))
            val state = vm.uiState.value
            assertEquals(listOf(novel1, novel2), state.popularNovels)
            assertFalse("popularIsLoading should be false after load", state.popularIsLoading)
        }

    // ── 11. Popular source switch ─────────────────────────────────────────────

    @Test
    fun `onPopularSourceSelected reloads popular with new source`() =
        runTest(UnconfinedTestDispatcher()) {
            val vm = createViewModel()

            coEvery { getPopularNovels("com.source.other", 1) } returns
                SourceResult.Success(listOf(novel1, novel2))

            vm.onPopularSourceSelected("com.source.other")

            assertEquals("com.source.other", vm.uiState.value.popularSourceId)
            coVerify(atLeast = 1) { getPopularNovels("com.source.other", 1) }
            assertEquals(listOf(novel1, novel2), vm.uiState.value.popularNovels)
            assertEquals(1, vm.uiState.value.popularPage)
        }

    // ── 12. loadNextPopularPage ───────────────────────────────────────────────

    @Test
    fun `loadNextPopularPage increments page and appends novels`() =
        runTest(UnconfinedTestDispatcher()) {
            coEvery { getPopularNovels("com.source.test", 1) } returns
                SourceResult.Success(listOf(novel1))
            coEvery { getPopularNovels("com.source.test", 2) } returns
                SourceResult.Success(listOf(novel2))

            val vm = createViewModel(popularResult = SourceResult.Success(listOf(novel1)))

            assertEquals(1, vm.uiState.value.popularPage)
            assertEquals(listOf(novel1), vm.uiState.value.popularNovels)

            vm.loadNextPopularPage()

            assertEquals(2, vm.uiState.value.popularPage)
            assertEquals(listOf(novel1, novel2), vm.uiState.value.popularNovels)
        }

    @Test
    fun `loadNextPopularPage does nothing when hasNextPage is false`() =
        runTest(UnconfinedTestDispatcher()) {
            // Empty result on page 1 means hasNextPage becomes false
            coEvery { getPopularNovels("com.source.test", 1) } returns
                SourceResult.Success(emptyList())

            val vm = createViewModel(popularResult = SourceResult.Success(emptyList()))

            assertFalse("hasNextPage should be false when page returns empty", vm.uiState.value.popularHasNextPage)

            vm.loadNextPopularPage()

            // Page should not advance
            assertEquals(1, vm.uiState.value.popularPage)
            coVerify(atLeast = 1) { getPopularNovels("com.source.test", 1) }
            coVerify(exactly = 0) { getPopularNovels("com.source.test", 2) }
        }
}
