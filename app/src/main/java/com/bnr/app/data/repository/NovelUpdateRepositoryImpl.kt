package com.bnr.app.data.repository

import com.bnr.app.data.local.db.dao.NovelUpdateDao
import com.bnr.app.data.local.db.entity.NovelUpdateEntity
import com.bnr.app.domain.model.NovelUpdateInfo
import com.bnr.app.domain.repository.NovelUpdateRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NovelUpdateRepositoryImpl @Inject constructor(
    private val novelUpdateDao: NovelUpdateDao
) : NovelUpdateRepository {

    override fun getUpdatesForLibraryNovels(): Flow<List<NovelUpdateInfo>> =
        novelUpdateDao.getUpdatesForLibraryNovels().map { list ->
            list.map { it.toDomain() }
        }

    override fun getUpdateInfo(novelId: String): Flow<NovelUpdateInfo?> =
        novelUpdateDao.getUpdateInfo(novelId).map { it?.toDomain() }

    override suspend fun recordCheckResult(novelId: String, currentChapterCount: Int, checkedAt: Long) {
        val known = novelUpdateDao.getKnownChapterCount(novelId) ?: 0
        val delta = (currentChapterCount - known).coerceAtLeast(0)
        novelUpdateDao.upsert(
            NovelUpdateEntity(
                novelId           = novelId,
                newChapterCount   = delta,
                knownChapterCount = currentChapterCount,
                lastCheckedAt     = checkedAt,
                lastUpdatedAt     = if (delta > 0) checkedAt else null
            )
        )
    }

    override suspend fun markAsSeen(novelId: String) =
        novelUpdateDao.clearNewChapters(novelId)

    override suspend fun getKnownChapterCount(novelId: String): Int? =
        novelUpdateDao.getKnownChapterCount(novelId)

    private fun NovelUpdateEntity.toDomain() = NovelUpdateInfo(
        novelId = novelId,
        newChapterCount = newChapterCount,
        knownChapterCount = knownChapterCount,
        lastCheckedAt = lastCheckedAt,
        lastUpdatedAt = lastUpdatedAt
    )
}
