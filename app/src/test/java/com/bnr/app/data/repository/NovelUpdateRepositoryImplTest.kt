package com.bnr.app.data.repository

import app.cash.turbine.test
import com.bnr.app.data.local.db.dao.NovelUpdateDao
import com.bnr.app.data.local.db.entity.NovelUpdateEntity
import com.bnr.app.domain.model.NovelUpdateInfo
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class NovelUpdateRepositoryImplTest {

    private val novelUpdateDao: NovelUpdateDao = mockk(relaxUnitFun = true)

    private lateinit var repository: NovelUpdateRepositoryImpl

    // ── shared fixtures ───────────────────────────────────────────────────────

    private val novelId = "src::slug"
    private val checkedAt = 1_700_000_000_000L

    private fun makeEntity(
        novelId: String = this.novelId,
        newChapterCount: Int = 0,
        knownChapterCount: Int = 10,
        lastCheckedAt: Long = checkedAt,
        lastUpdatedAt: Long? = null
    ) = NovelUpdateEntity(
        novelId = novelId,
        newChapterCount = newChapterCount,
        knownChapterCount = knownChapterCount,
        lastCheckedAt = lastCheckedAt,
        lastUpdatedAt = lastUpdatedAt
    )

    @Before
    fun setUp() {
        repository = NovelUpdateRepositoryImpl(novelUpdateDao)
    }

    // ── recordCheckResult ─────────────────────────────────────────────────────

    @Test
    fun `recordCheckResult when no previous record creates new entry with delta=0 when count matches`() =
        runTest(UnconfinedTestDispatcher()) {
            coEvery { novelUpdateDao.getKnownChapterCount(novelId) } returns null

            repository.recordCheckResult(novelId, currentChapterCount = 10, checkedAt = checkedAt)

            coVerify(exactly = 1) {
                novelUpdateDao.upsert(
                    match { entity ->
                        entity.novelId == novelId &&
                            entity.newChapterCount == 10 &&  // 10 - 0 = 10 (first check, known=0)
                            entity.knownChapterCount == 10 &&
                            entity.lastCheckedAt == checkedAt &&
                            entity.lastUpdatedAt == checkedAt  // delta > 0 on first check
                    }
                )
            }
        }

    @Test
    fun `recordCheckResult when currentCount greater than known sets newChapterCount to delta and lastUpdatedAt`() =
        runTest(UnconfinedTestDispatcher()) {
            coEvery { novelUpdateDao.getKnownChapterCount(novelId) } returns 10

            repository.recordCheckResult(novelId, currentChapterCount = 15, checkedAt = checkedAt)

            coVerify(exactly = 1) {
                novelUpdateDao.upsert(
                    match { entity ->
                        entity.novelId == novelId &&
                            entity.newChapterCount == 5 &&
                            entity.knownChapterCount == 15 &&
                            entity.lastCheckedAt == checkedAt &&
                            entity.lastUpdatedAt == checkedAt
                    }
                )
            }
        }

    @Test
    fun `recordCheckResult when currentCount equals known sets newChapterCount=0 and lastUpdatedAt=null`() =
        runTest(UnconfinedTestDispatcher()) {
            coEvery { novelUpdateDao.getKnownChapterCount(novelId) } returns 10

            repository.recordCheckResult(novelId, currentChapterCount = 10, checkedAt = checkedAt)

            coVerify(exactly = 1) {
                novelUpdateDao.upsert(
                    match { entity ->
                        entity.novelId == novelId &&
                            entity.newChapterCount == 0 &&
                            entity.knownChapterCount == 10 &&
                            entity.lastCheckedAt == checkedAt &&
                            entity.lastUpdatedAt == null
                    }
                )
            }
        }

    // ── markAsSeen ────────────────────────────────────────────────────────────

    @Test
    fun `markAsSeen calls clearNewChapters on dao`() =
        runTest(UnconfinedTestDispatcher()) {
            repository.markAsSeen(novelId)

            coVerify(exactly = 1) { novelUpdateDao.clearNewChapters(novelId) }
        }

    // ── getKnownChapterCount ──────────────────────────────────────────────────

    @Test
    fun `getKnownChapterCount returns null when no record`() =
        runTest(UnconfinedTestDispatcher()) {
            coEvery { novelUpdateDao.getKnownChapterCount(novelId) } returns null

            val result = repository.getKnownChapterCount(novelId)

            assertNull(result)
        }

    @Test
    fun `getKnownChapterCount returns count from dao`() =
        runTest(UnconfinedTestDispatcher()) {
            coEvery { novelUpdateDao.getKnownChapterCount(novelId) } returns 42

            val result = repository.getKnownChapterCount(novelId)

            assertEquals(42, result)
        }

    // ── getUpdatesForLibraryNovels ────────────────────────────────────────────

    @Test
    fun `getUpdatesForLibraryNovels maps entities to domain models`() =
        runTest(UnconfinedTestDispatcher()) {
            val entity1 = makeEntity(novelId = "src::novel1", newChapterCount = 3, knownChapterCount = 13, lastUpdatedAt = checkedAt)
            val entity2 = makeEntity(novelId = "src::novel2", newChapterCount = 0, knownChapterCount = 5, lastUpdatedAt = null)
            every { novelUpdateDao.getUpdatesForLibraryNovels() } returns flowOf(listOf(entity1, entity2))

            repository.getUpdatesForLibraryNovels().test {
                val items = awaitItem()
                assertEquals(2, items.size)

                val first = items[0]
                assertEquals("src::novel1", first.novelId)
                assertEquals(3, first.newChapterCount)
                assertEquals(13, first.knownChapterCount)
                assertEquals(checkedAt, first.lastCheckedAt)
                assertEquals(checkedAt, first.lastUpdatedAt)

                val second = items[1]
                assertEquals("src::novel2", second.novelId)
                assertEquals(0, second.newChapterCount)
                assertEquals(5, second.knownChapterCount)
                assertNull(second.lastUpdatedAt)

                awaitComplete()
            }
        }

    // ── getUpdateInfo ─────────────────────────────────────────────────────────

    @Test
    fun `getUpdateInfo maps single entity to domain`() =
        runTest(UnconfinedTestDispatcher()) {
            val entity = makeEntity(newChapterCount = 2, knownChapterCount = 12, lastUpdatedAt = checkedAt)
            every { novelUpdateDao.getUpdateInfo(novelId) } returns flowOf(entity)

            repository.getUpdateInfo(novelId).test {
                val item = awaitItem()
                assertEquals(novelId, item!!.novelId)
                assertEquals(2, item.newChapterCount)
                assertEquals(12, item.knownChapterCount)
                assertEquals(checkedAt, item.lastCheckedAt)
                assertEquals(checkedAt, item.lastUpdatedAt)
                awaitComplete()
            }
        }

    @Test
    fun `getUpdateInfo emits null when dao returns null`() =
        runTest(UnconfinedTestDispatcher()) {
            every { novelUpdateDao.getUpdateInfo(novelId) } returns flowOf(null)

            repository.getUpdateInfo(novelId).test {
                assertNull(awaitItem())
                awaitComplete()
            }
        }
}
