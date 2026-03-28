package com.bnr.app.data.repository

import com.bnr.app.data.local.db.dao.NovelDao
import com.bnr.app.data.local.db.toDomain
import com.bnr.app.data.local.db.toEntity
import com.bnr.app.domain.model.Novel
import com.bnr.app.domain.repository.NovelRepository
import com.bnr.app.source.SourceManager
import com.bnr.app.source.SourceResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NovelRepositoryImpl @Inject constructor(
    private val sourceManager: SourceManager,
    private val novelDao: NovelDao
) : NovelRepository {

    override suspend fun getPopularNovels(sourceId: String, page: Int): SourceResult<List<Novel>> =
        sourceManager.getSource(sourceId).getPopularNovels(page)

    override suspend fun searchNovels(
        sourceId: String,
        query: String,
        page: Int,
        filters: Map<String, String>
    ): SourceResult<List<Novel>> =
        sourceManager.getSource(sourceId).searchNovels(query, page, filters)

    override suspend fun getNovelDetail(sourceId: String, novelUrl: String): SourceResult<Novel> {
        val result = sourceManager.getSource(sourceId).getNovelDetail(novelUrl)
        if (result is SourceResult.Success) {
            // Cache in Room, preserve library-specific fields from existing row
            val existing = novelDao.getNovelById(result.data.id)
            novelDao.upsertNovel(
                result.data.toEntity().copy(
                    inLibrary          = existing?.inLibrary ?: false,
                    addedToLibraryAt   = existing?.addedToLibraryAt,
                    lastReadChapterId  = existing?.lastReadChapterId
                )
            )
        }
        return result
    }

    override fun getLibraryNovels(): Flow<List<Novel>> =
        novelDao.getLibraryNovels().map { entities -> entities.map { it.toDomain() } }

    override suspend fun isInLibrary(novelId: String): Boolean =
        novelDao.getNovelById(novelId)?.inLibrary ?: false

    override suspend fun addToLibrary(novel: Novel) {
        novelDao.upsertNovel(novel.toEntity())
        novelDao.addToLibrary(novel.id, System.currentTimeMillis())
    }

    override suspend fun removeFromLibrary(novelId: String) =
        novelDao.removeFromLibrary(novelId)

    override suspend fun updateLastReadChapter(novelId: String, chapterId: String) =
        novelDao.updateLastReadChapter(novelId, chapterId)
}
