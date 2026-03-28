package com.bnr.app.domain.usecase

import app.cash.turbine.test
import com.bnr.app.domain.model.NovelUpdateInfo
import com.bnr.app.domain.repository.NovelUpdateRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class GetNovelUpdatesUseCaseTest {

    private val repository: NovelUpdateRepository = mockk()

    private lateinit var useCase: GetNovelUpdatesUseCase

    // ── shared fixtures ───────────────────────────────────────────────────────

    private val checkedAt = 1_700_000_000_000L

    private fun makeUpdateInfo(
        novelId: String = "src::slug",
        newChapterCount: Int = 0,
        knownChapterCount: Int = 10,
        lastCheckedAt: Long = checkedAt,
        lastUpdatedAt: Long? = null
    ) = NovelUpdateInfo(
        novelId = novelId,
        newChapterCount = newChapterCount,
        knownChapterCount = knownChapterCount,
        lastCheckedAt = lastCheckedAt,
        lastUpdatedAt = lastUpdatedAt
    )

    @Before
    fun setUp() {
        useCase = GetNovelUpdatesUseCase(repository)
    }

    // ── invoke ────────────────────────────────────────────────────────────────

    @Test
    fun `invoke returns flow from repository`() = runTest(UnconfinedTestDispatcher()) {
        every { repository.getUpdatesForLibraryNovels() } returns flowOf(emptyList())

        val flow = useCase()

        flow.test {
            awaitItem()
            awaitComplete()
        }
    }

    @Test
    fun `flow emits list of NovelUpdateInfo`() = runTest(UnconfinedTestDispatcher()) {
        val updateInfo1 = makeUpdateInfo(novelId = "src::novel1", newChapterCount = 3, knownChapterCount = 13, lastUpdatedAt = checkedAt)
        val updateInfo2 = makeUpdateInfo(novelId = "src::novel2", newChapterCount = 0, knownChapterCount = 5)
        every { repository.getUpdatesForLibraryNovels() } returns flowOf(listOf(updateInfo1, updateInfo2))

        useCase().test {
            val items = awaitItem()
            assertEquals(2, items.size)
            assertEquals("src::novel1", items[0].novelId)
            assertEquals(3, items[0].newChapterCount)
            assertEquals("src::novel2", items[1].novelId)
            assertEquals(0, items[1].newChapterCount)
            awaitComplete()
        }
    }
}
