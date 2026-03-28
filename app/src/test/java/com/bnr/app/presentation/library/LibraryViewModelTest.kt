package com.bnr.app.presentation.library

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import app.cash.turbine.test
import com.bnr.app.domain.model.Novel
import com.bnr.app.domain.usecase.GetLibraryNovelsUseCase
import com.bnr.app.domain.usecase.makeNovel
import com.bnr.app.presentation.MainDispatcherRule
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
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

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun createViewModel(): LibraryViewModel = LibraryViewModel(getLibraryNovels)

    // ── Tests ─────────────────────────────────────────────────────────────────

    @Test
    fun `initial state emits empty list before any library novels arrive`() =
        runTest(UnconfinedTestDispatcher()) {
            // Use-case returns a flow that never emits, so the stateIn initial value is used.
            every { getLibraryNovels() } returns flow { awaitCancellation() }

            val vm = createViewModel()

            vm.novels.test {
                val initial = awaitItem()
                assertTrue("Initial emission should be empty list", initial.isEmpty())
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `library novels flow emission updates novels state`() =
        runTest(UnconfinedTestDispatcher()) {
            val novel1 = makeNovel(id = "src::lib1", title = "Library Novel 1", inLibrary = true)
            val novel2 = makeNovel(id = "src::lib2", title = "Library Novel 2", inLibrary = true)
            every { getLibraryNovels() } returns flowOf(listOf(novel1, novel2))

            val vm = createViewModel()

            vm.novels.test {
                // With UnconfinedTestDispatcher the upstream flowOf completes eagerly.
                // Consume items until we see the populated list (skipping the empty initial value).
                var latest: List<Novel> = awaitItem()
                if (latest.isEmpty()) latest = awaitItem()

                assertEquals(2, latest.size)
                assertEquals(novel1, latest[0])
                assertEquals(novel2, latest[1])
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `multiple emissions update state in order`() =
        runTest(UnconfinedTestDispatcher()) {
            val novel1 = makeNovel(id = "src::a", title = "Alpha", inLibrary = true)
            val novel2 = makeNovel(id = "src::b", title = "Beta", inLibrary = true)
            val libraryFlow = MutableStateFlow<List<Novel>>(emptyList())
            every { getLibraryNovels() } returns libraryFlow

            val vm = createViewModel()

            vm.novels.test {
                // 1st emission: stateIn initial value (empty list).
                val first = awaitItem()
                assertTrue(first.isEmpty())

                // 2nd emission: one novel added.
                libraryFlow.value = listOf(novel1)
                val second = awaitItem()
                assertEquals(listOf(novel1), second)

                // 3rd emission: two novels.
                libraryFlow.value = listOf(novel1, novel2)
                val third = awaitItem()
                assertEquals(listOf(novel1, novel2), third)

                cancelAndIgnoreRemainingEvents()
            }
        }
}
