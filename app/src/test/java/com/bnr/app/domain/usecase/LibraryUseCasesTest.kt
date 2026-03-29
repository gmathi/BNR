package com.bnr.app.domain.usecase

import app.cash.turbine.test
import com.bnr.app.domain.repository.NovelRepository
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class LibraryUseCasesTest {

    private lateinit var repository: NovelRepository

    private lateinit var addUseCase: AddNovelToLibraryUseCase
    private lateinit var removeUseCase: RemoveNovelFromLibraryUseCase
    private lateinit var getLibraryUseCase: GetLibraryNovelsUseCase

    @Before
    fun setUp() {
        repository = mockk()
        addUseCase = AddNovelToLibraryUseCase(repository)
        removeUseCase = RemoveNovelFromLibraryUseCase(repository)
        getLibraryUseCase = GetLibraryNovelsUseCase(repository)
    }

    // =========================================================================
    // AddNovelToLibraryUseCase
    // =========================================================================

    @Test
    fun `AddToLibrary calls repository addToLibrary with the given novel`() = runTest {
        val novel = makeNovel()
        coJustRun { repository.addToLibrary(novel) }

        addUseCase(novel)

        coVerify(exactly = 1) { repository.addToLibrary(novel) }
    }

    @Test
    fun `AddToLibrary passes through different novel instances correctly`() = runTest {
        val novel1 = makeNovel(id = "src::novel-1", title = "Novel One")
        val novel2 = makeNovel(id = "src::novel-2", title = "Novel Two")
        coJustRun { repository.addToLibrary(any()) }

        addUseCase(novel1)
        addUseCase(novel2)

        coVerify(exactly = 1) { repository.addToLibrary(novel1) }
        coVerify(exactly = 1) { repository.addToLibrary(novel2) }
    }

    @Test
    fun `AddToLibrary does not call removeFromLibrary or any other repository method`() = runTest {
        val novel = makeNovel()
        coJustRun { repository.addToLibrary(novel) }

        addUseCase(novel)

        coVerify(exactly = 0) { repository.removeFromLibrary(any()) }
        coVerify(exactly = 0) { repository.updateLastReadChapter(any(), any()) }
    }

    // =========================================================================
    // RemoveNovelFromLibraryUseCase
    // =========================================================================

    @Test
    fun `RemoveFromLibrary calls repository removeFromLibrary with the given novelId`() = runTest {
        val novelId = "src::slug"
        coJustRun { repository.removeFromLibrary(novelId) }

        removeUseCase(novelId)

        coVerify(exactly = 1) { repository.removeFromLibrary(novelId) }
    }

    @Test
    fun `RemoveFromLibrary passes the exact novelId string to repository`() = runTest {
        val novelId = "com.source.test::my-novel-slug"
        coJustRun { repository.removeFromLibrary(novelId) }

        removeUseCase(novelId)

        coVerify(exactly = 1) { repository.removeFromLibrary(novelId) }
    }

    @Test
    fun `RemoveFromLibrary does not call addToLibrary or any other repository method`() = runTest {
        val novelId = "src::slug"
        coJustRun { repository.removeFromLibrary(novelId) }

        removeUseCase(novelId)

        coVerify(exactly = 0) { repository.addToLibrary(any()) }
        coVerify(exactly = 0) { repository.updateLastReadChapter(any(), any()) }
    }

    // =========================================================================
    // GetLibraryNovelsUseCase
    // =========================================================================

    @Test
    fun `GetLibraryNovels returns flow from repository`() =
        runTest(UnconfinedTestDispatcher()) {
            val novels = listOf(
                makeNovel(id = "src::novel-1", inLibrary = true),
                makeNovel(id = "src::novel-2", inLibrary = true)
            )
            every { repository.getLibraryNovels() } returns flowOf(novels)

            getLibraryUseCase().test {
                val emitted = awaitItem()
                assertEquals(novels, emitted)
                awaitComplete()
            }
        }

    @Test
    fun `GetLibraryNovels emits empty list when library is empty`() =
        runTest(UnconfinedTestDispatcher()) {
            every { repository.getLibraryNovels() } returns flowOf(emptyList())

            getLibraryUseCase().test {
                val emitted = awaitItem()
                assertEquals(emptyList<com.bnr.app.domain.model.Novel>(), emitted)
                awaitComplete()
            }
        }

    @Test
    fun `GetLibraryNovels emits multiple updates in sequence`() =
        runTest(UnconfinedTestDispatcher()) {
            val initial = listOf(makeNovel(id = "src::novel-1", inLibrary = true))
            val updated = listOf(
                makeNovel(id = "src::novel-1", inLibrary = true),
                makeNovel(id = "src::novel-2", inLibrary = true)
            )
            every { repository.getLibraryNovels() } returns
                kotlinx.coroutines.flow.flow {
                    emit(initial)
                    emit(updated)
                }

            getLibraryUseCase().test {
                assertEquals(initial, awaitItem())
                assertEquals(updated, awaitItem())
                awaitComplete()
            }
        }

    @Test
    fun `GetLibraryNovels list items match expected novel data`() =
        runTest(UnconfinedTestDispatcher()) {
            val novel = makeNovel(
                id = "src::slug",
                title = "My Favourite Novel",
                inLibrary = true,
                chapterCount = 42
            )
            every { repository.getLibraryNovels() } returns flowOf(listOf(novel))

            getLibraryUseCase().test {
                val emitted = awaitItem()
                assertEquals(1, emitted.size)
                assertEquals("My Favourite Novel", emitted.first().title)
                assertEquals(42, emitted.first().chapterCount)
                assertTrue(emitted.first().inLibrary)
                awaitComplete()
            }
        }
}
