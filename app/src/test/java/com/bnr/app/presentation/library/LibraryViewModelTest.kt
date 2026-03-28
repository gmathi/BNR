package com.bnr.app.presentation.library

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import app.cash.turbine.test
import com.bnr.app.domain.model.Novel
import com.bnr.app.domain.model.NovelStatus
import com.bnr.app.domain.model.NovelUpdateInfo
import com.bnr.app.domain.usecase.GetLibraryNovelsUseCase
import com.bnr.app.domain.usecase.GetNovelUpdatesUseCase
import com.bnr.app.domain.usecase.makeNovel
import com.bnr.app.presentation.MainDispatcherRule
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(JUnit4::class)
class LibraryViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @get:Rule
    val instantTask = InstantTaskExecutorRule()

    private val getLibraryNovels: GetLibraryNovelsUseCase = mockk(relaxed = true)
    private val getNovelUpdates: GetNovelUpdatesUseCase = mockk(relaxed = true)

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun makeUpdateInfo(
        novelId: String,
        newChapterCount: Int = 0,
        knownChapterCount: Int = 10
    ) = NovelUpdateInfo(
        novelId = novelId,
        newChapterCount = newChapterCount,
        knownChapterCount = knownChapterCount,
        lastCheckedAt = 1_000L,
        lastUpdatedAt = null
    )

    private fun createViewModel(): LibraryViewModel = LibraryViewModel(
        getLibraryNovels,
        getNovelUpdates
    )

    // ── Tests ─────────────────────────────────────────────────────────────────

    @Test
    fun `initial state has empty displayedNovels, blank searchQuery, and DATE_ADDED sort`() =
        runTest(UnconfinedTestDispatcher()) {
            every { getLibraryNovels() } returns flow { awaitCancellation() }
            every { getNovelUpdates() } returns flow { awaitCancellation() }

            val vm = createViewModel()

            vm.uiState.test {
                val state = awaitItem()
                assertTrue("displayedNovels should be empty", state.displayedNovels.isEmpty())
                assertTrue("allNovels should be empty", state.allNovels.isEmpty())
                assertEquals("", state.searchQuery)
                assertEquals(LibrarySortOption.DATE_ADDED, state.sortOption)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `search filters by title case insensitive`() =
        runTest(UnconfinedTestDispatcher()) {
            val shadowSlave = makeNovel(id = "src::1", title = "Shadow Slave", author = "Author X")
            val cultivation = makeNovel(id = "src::2", title = "Cultivation Chat Group", author = "Author Y")
            val libraryFlow = MutableStateFlow<List<Novel>>(listOf(shadowSlave, cultivation))
            every { getLibraryNovels() } returns libraryFlow
            every { getNovelUpdates() } returns MutableStateFlow(emptyList())

            val vm = createViewModel()

            vm.uiState.test {
                // Consume initial emission with both novels
                var state = awaitItem()
                if (state.displayedNovels.isEmpty()) state = awaitItem()

                vm.onSearchQueryChanged("shadow")
                state = awaitItem()

                assertEquals(1, state.displayedNovels.size)
                assertEquals(shadowSlave, state.displayedNovels[0])
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `search filters by author case insensitive`() =
        runTest(UnconfinedTestDispatcher()) {
            val novel1 = makeNovel(id = "src::1", title = "Novel One", author = "Author1")
            val novel2 = makeNovel(id = "src::2", title = "Novel Two", author = "Author2")
            val novel3 = makeNovel(id = "src::3", title = "Novel Three", author = "Author1")
            val libraryFlow = MutableStateFlow<List<Novel>>(listOf(novel1, novel2, novel3))
            every { getLibraryNovels() } returns libraryFlow
            every { getNovelUpdates() } returns MutableStateFlow(emptyList())

            val vm = createViewModel()

            vm.uiState.test {
                var state = awaitItem()
                if (state.displayedNovels.isEmpty()) state = awaitItem()

                vm.onSearchQueryChanged("author1")
                state = awaitItem()

                assertEquals(2, state.displayedNovels.size)
                assertTrue(state.displayedNovels.all { it.author == "Author1" })
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `clearing search query shows all novels again`() =
        runTest(UnconfinedTestDispatcher()) {
            val novel1 = makeNovel(id = "src::1", title = "Shadow Slave", author = "Author X")
            val novel2 = makeNovel(id = "src::2", title = "Cultivation Chat Group", author = "Author Y")
            val libraryFlow = MutableStateFlow<List<Novel>>(listOf(novel1, novel2))
            every { getLibraryNovels() } returns libraryFlow
            every { getNovelUpdates() } returns MutableStateFlow(emptyList())

            val vm = createViewModel()

            vm.uiState.test {
                var state = awaitItem()
                if (state.displayedNovels.isEmpty()) state = awaitItem()

                vm.onSearchQueryChanged("shadow")
                state = awaitItem()
                assertEquals(1, state.displayedNovels.size)

                vm.onSearchQueryChanged("")
                state = awaitItem()
                assertEquals(2, state.displayedNovels.size)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `sort DATE_ADDED orders novels by addedToLibraryAt descending`() =
        runTest(UnconfinedTestDispatcher()) {
            val older  = makeNovel(id = "src::1", title = "Older",  addedToLibraryAt = 1_000L)
            val newest = makeNovel(id = "src::2", title = "Newest", addedToLibraryAt = 3_000L)
            val middle = makeNovel(id = "src::3", title = "Middle", addedToLibraryAt = 2_000L)
            val libraryFlow = MutableStateFlow<List<Novel>>(listOf(older, newest, middle))
            every { getLibraryNovels() } returns libraryFlow
            every { getNovelUpdates() } returns MutableStateFlow(emptyList())

            val vm = createViewModel()

            vm.uiState.test {
                var state = awaitItem()
                if (state.displayedNovels.isEmpty()) state = awaitItem()

                vm.onSortOptionSelected(LibrarySortOption.DATE_ADDED)
                state = awaitItem()

                assertEquals(listOf(newest, middle, older), state.displayedNovels)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `sort TITLE_AZ orders novels alphabetically ascending`() =
        runTest(UnconfinedTestDispatcher()) {
            val charlie = makeNovel(id = "src::1", title = "Charlie")
            val alpha   = makeNovel(id = "src::2", title = "Alpha")
            val bravo   = makeNovel(id = "src::3", title = "Bravo")
            val libraryFlow = MutableStateFlow<List<Novel>>(listOf(charlie, alpha, bravo))
            every { getLibraryNovels() } returns libraryFlow
            every { getNovelUpdates() } returns MutableStateFlow(emptyList())

            val vm = createViewModel()

            vm.uiState.test {
                var state = awaitItem()
                if (state.displayedNovels.isEmpty()) state = awaitItem()

                vm.onSortOptionSelected(LibrarySortOption.TITLE_AZ)
                state = awaitItem()

                assertEquals(listOf(alpha, bravo, charlie), state.displayedNovels)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `sort TITLE_ZA orders novels alphabetically descending`() =
        runTest(UnconfinedTestDispatcher()) {
            val charlie = makeNovel(id = "src::1", title = "Charlie")
            val alpha   = makeNovel(id = "src::2", title = "Alpha")
            val bravo   = makeNovel(id = "src::3", title = "Bravo")
            val libraryFlow = MutableStateFlow<List<Novel>>(listOf(charlie, alpha, bravo))
            every { getLibraryNovels() } returns libraryFlow
            every { getNovelUpdates() } returns MutableStateFlow(emptyList())

            val vm = createViewModel()

            vm.uiState.test {
                var state = awaitItem()
                if (state.displayedNovels.isEmpty()) state = awaitItem()

                vm.onSortOptionSelected(LibrarySortOption.TITLE_ZA)
                state = awaitItem()

                assertEquals(listOf(charlie, bravo, alpha), state.displayedNovels)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `sort AUTHOR orders novels by author name ascending`() =
        runTest(UnconfinedTestDispatcher()) {
            val novelC = makeNovel(id = "src::1", title = "Novel C", author = "Zhao Wei")
            val novelA = makeNovel(id = "src::2", title = "Novel A", author = "Alice Kim")
            val novelB = makeNovel(id = "src::3", title = "Novel B", author = "Bob Lee")
            val libraryFlow = MutableStateFlow<List<Novel>>(listOf(novelC, novelA, novelB))
            every { getLibraryNovels() } returns libraryFlow
            every { getNovelUpdates() } returns MutableStateFlow(emptyList())

            val vm = createViewModel()

            vm.uiState.test {
                var state = awaitItem()
                if (state.displayedNovels.isEmpty()) state = awaitItem()

                vm.onSortOptionSelected(LibrarySortOption.AUTHOR)
                state = awaitItem()

                assertEquals(listOf(novelA, novelB, novelC), state.displayedNovels)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `sort STATUS orders novels by NovelStatus ordinal`() =
        runTest(UnconfinedTestDispatcher()) {
            val unknown   = makeNovel(id = "src::1", title = "Unknown",   status = NovelStatus.UNKNOWN)
            val completed = makeNovel(id = "src::2", title = "Completed", status = NovelStatus.COMPLETED)
            val ongoing   = makeNovel(id = "src::3", title = "Ongoing",   status = NovelStatus.ONGOING)
            val hiatus    = makeNovel(id = "src::4", title = "Hiatus",    status = NovelStatus.HIATUS)
            val libraryFlow = MutableStateFlow<List<Novel>>(listOf(unknown, completed, ongoing, hiatus))
            every { getLibraryNovels() } returns libraryFlow
            every { getNovelUpdates() } returns MutableStateFlow(emptyList())

            val vm = createViewModel()

            vm.uiState.test {
                var state = awaitItem()
                if (state.displayedNovels.isEmpty()) state = awaitItem()

                vm.onSortOptionSelected(LibrarySortOption.STATUS)
                state = awaitItem()

                // NovelStatus ordinals: ONGOING=0, COMPLETED=1, HIATUS=2, UNKNOWN=3
                assertEquals(
                    listOf(ongoing, completed, hiatus, unknown),
                    state.displayedNovels
                )
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `sort TITLE_AZ combined with search filter returns sorted filtered list`() =
        runTest(UnconfinedTestDispatcher()) {
            val shadowMaster = makeNovel(id = "src::1", title = "Shadow Master", author = "Author A")
            val shadowSlave  = makeNovel(id = "src::2", title = "Shadow Slave",  author = "Author B")
            val cultivation  = makeNovel(id = "src::3", title = "Cultivation Chat Group", author = "Author C")
            val libraryFlow = MutableStateFlow<List<Novel>>(listOf(shadowSlave, shadowMaster, cultivation))
            every { getLibraryNovels() } returns libraryFlow
            every { getNovelUpdates() } returns MutableStateFlow(emptyList())

            val vm = createViewModel()

            vm.uiState.test {
                var state = awaitItem()
                if (state.displayedNovels.isEmpty()) state = awaitItem()

                vm.onSortOptionSelected(LibrarySortOption.TITLE_AZ)
                state = awaitItem()

                vm.onSearchQueryChanged("shadow")
                state = awaitItem()

                assertEquals(2, state.displayedNovels.size)
                // TITLE_AZ: "Shadow Master" before "Shadow Slave"
                assertEquals(shadowMaster, state.displayedNovels[0])
                assertEquals(shadowSlave,  state.displayedNovels[1])
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `onSortOptionSelected updates sortOption in state`() =
        runTest(UnconfinedTestDispatcher()) {
            every { getLibraryNovels() } returns flow { awaitCancellation() }
            every { getNovelUpdates() } returns flow { awaitCancellation() }

            val vm = createViewModel()

            vm.uiState.test {
                val initial = awaitItem()
                assertEquals(LibrarySortOption.DATE_ADDED, initial.sortOption)

                vm.onSortOptionSelected(LibrarySortOption.AUTHOR)
                val updated = awaitItem()
                assertEquals(LibrarySortOption.AUTHOR, updated.sortOption)

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `onSearchQueryChanged updates searchQuery in state`() =
        runTest(UnconfinedTestDispatcher()) {
            every { getLibraryNovels() } returns flow { awaitCancellation() }
            every { getNovelUpdates() } returns flow { awaitCancellation() }

            val vm = createViewModel()

            vm.uiState.test {
                val initial = awaitItem()
                assertEquals("", initial.searchQuery)

                vm.onSearchQueryChanged("shadow")
                val updated = awaitItem()
                assertEquals("shadow", updated.searchQuery)

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `allNovels is unaffected by search filter`() =
        runTest(UnconfinedTestDispatcher()) {
            val novel1 = makeNovel(id = "src::1", title = "Shadow Slave",          author = "Author X")
            val novel2 = makeNovel(id = "src::2", title = "Cultivation Chat Group", author = "Author Y")
            val novel3 = makeNovel(id = "src::3", title = "Omniscient Reader",      author = "Author Z")
            val libraryFlow = MutableStateFlow<List<Novel>>(listOf(novel1, novel2, novel3))
            every { getLibraryNovels() } returns libraryFlow
            every { getNovelUpdates() } returns MutableStateFlow(emptyList())

            val vm = createViewModel()

            vm.uiState.test {
                var state = awaitItem()
                if (state.allNovels.isEmpty()) state = awaitItem()

                vm.onSearchQueryChanged("shadow")
                state = awaitItem()

                assertEquals(1, state.displayedNovels.size)
                assertEquals(3, state.allNovels.size)
                cancelAndIgnoreRemainingEvents()
            }
        }

    // ── updateInfoMap tests ───────────────────────────────────────────────────

    @Test
    fun `updateInfoMap populated from GetNovelUpdatesUseCase`() =
        runTest(UnconfinedTestDispatcher()) {
            val novel = makeNovel(id = "src::1", title = "Shadow Slave")
            val update = makeUpdateInfo(novelId = "src::1", newChapterCount = 3)
            every { getLibraryNovels() } returns MutableStateFlow(listOf(novel))
            every { getNovelUpdates() } returns MutableStateFlow(listOf(update))

            val vm = createViewModel()

            vm.uiState.test {
                var state = awaitItem()
                if (state.updateInfoMap.isEmpty()) state = awaitItem()

                assertTrue(state.updateInfoMap.isNotEmpty())
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `updateInfoMap keyed by novelId`() =
        runTest(UnconfinedTestDispatcher()) {
            val novel1 = makeNovel(id = "src::1", title = "Novel One")
            val novel2 = makeNovel(id = "src::2", title = "Novel Two")
            val update1 = makeUpdateInfo(novelId = "src::1", newChapterCount = 2)
            val update2 = makeUpdateInfo(novelId = "src::2", newChapterCount = 5)
            every { getLibraryNovels() } returns MutableStateFlow(listOf(novel1, novel2))
            every { getNovelUpdates() } returns MutableStateFlow(listOf(update1, update2))

            val vm = createViewModel()

            vm.uiState.test {
                var state = awaitItem()
                if (state.updateInfoMap.size < 2) state = awaitItem()

                assertEquals(2, state.updateInfoMap.size)
                assertTrue(state.updateInfoMap.containsKey("src::1"))
                assertTrue(state.updateInfoMap.containsKey("src::2"))
                assertEquals(update1, state.updateInfoMap["src::1"])
                assertEquals(update2, state.updateInfoMap["src::2"])
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `updateInfoMap empty when no updates`() =
        runTest(UnconfinedTestDispatcher()) {
            val novel = makeNovel(id = "src::1", title = "Shadow Slave")
            every { getLibraryNovels() } returns MutableStateFlow(listOf(novel))
            every { getNovelUpdates() } returns MutableStateFlow(emptyList())

            val vm = createViewModel()

            vm.uiState.test {
                var state = awaitItem()
                if (state.allNovels.isEmpty()) state = awaitItem()

                assertTrue(state.updateInfoMap.isEmpty())
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `novel with 5 new chapters has newChapterCount=5 in updateInfoMap`() =
        runTest(UnconfinedTestDispatcher()) {
            val novel = makeNovel(id = "src::42", title = "Rising Warrior")
            val update = makeUpdateInfo(novelId = "src::42", newChapterCount = 5)
            every { getLibraryNovels() } returns MutableStateFlow(listOf(novel))
            every { getNovelUpdates() } returns MutableStateFlow(listOf(update))

            val vm = createViewModel()

            vm.uiState.test {
                var state = awaitItem()
                if (state.updateInfoMap.isEmpty()) state = awaitItem()

                val info = state.updateInfoMap["src::42"]
                assertEquals(5, info?.newChapterCount)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `combine filter+sort+updates all applied simultaneously`() =
        runTest(UnconfinedTestDispatcher()) {
            val shadowMaster = makeNovel(id = "src::1", title = "Shadow Master", author = "Author A")
            val shadowSlave  = makeNovel(id = "src::2", title = "Shadow Slave",  author = "Author B")
            val cultivation  = makeNovel(id = "src::3", title = "Cultivation Chat Group", author = "Author C")

            val update1 = makeUpdateInfo(novelId = "src::1", newChapterCount = 3)
            val update2 = makeUpdateInfo(novelId = "src::2", newChapterCount = 7)

            val libraryFlow = MutableStateFlow<List<Novel>>(listOf(shadowSlave, shadowMaster, cultivation))
            val updatesFlow = MutableStateFlow<List<NovelUpdateInfo>>(listOf(update1, update2))
            every { getLibraryNovels() } returns libraryFlow
            every { getNovelUpdates() } returns updatesFlow

            val vm = createViewModel()

            vm.uiState.test {
                var state = awaitItem()
                if (state.displayedNovels.isEmpty()) state = awaitItem()

                // Apply sort and search filter
                vm.onSortOptionSelected(LibrarySortOption.TITLE_AZ)
                state = awaitItem()
                vm.onSearchQueryChanged("shadow")
                state = awaitItem()

                // displayedNovels: filtered to "shadow" novels, sorted AZ
                assertEquals(2, state.displayedNovels.size)
                assertEquals(shadowMaster, state.displayedNovels[0])
                assertEquals(shadowSlave,  state.displayedNovels[1])

                // updateInfoMap: still contains all updates regardless of filter
                assertEquals(2, state.updateInfoMap.size)
                assertEquals(3, state.updateInfoMap["src::1"]?.newChapterCount)
                assertEquals(7, state.updateInfoMap["src::2"]?.newChapterCount)

                cancelAndIgnoreRemainingEvents()
            }
        }
}
