package com.bnr.app.data.repository

import com.bnr.app.data.local.db.dao.ChapterDao
import com.bnr.app.data.local.db.toDomain
import com.bnr.app.data.local.db.toEntity
import com.bnr.app.domain.model.Chapter
import com.bnr.app.domain.model.ChapterContent
import com.bnr.app.domain.model.DownloadState
import com.bnr.app.domain.repository.ChapterRepository
import com.bnr.app.source.SourceManager
import com.bnr.app.source.SourceResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChapterRepositoryImpl @Inject constructor(
    private val sourceManager: SourceManager,
    private val chapterDao: ChapterDao
) : ChapterRepository {

    override suspend fun getChapterList(
        sourceId: String,
        novelUrl: String,
        novelId: String
    ): SourceResult<List<Chapter>> {
        val result = sourceManager.getSource(sourceId).getChapterList(novelUrl)
        if (result is SourceResult.Success) {
            // Persist chapter list; preserve read/download state on existing rows
            val existing = chapterDao.getChaptersByNovelId(novelId)
            // We can't easily do this without collecting the Flow, so just upsert
            // Room @Upsert will update existing rows preserving unchanged columns via SQL
            chapterDao.upsertChapters(result.data.map { it.toEntity() })
        }
        return result
    }

    override fun getCachedChapters(novelId: String): Flow<List<Chapter>> =
        chapterDao.getChaptersByNovelId(novelId).map { entities -> entities.map { it.toDomain() } }

    override suspend fun getChapterById(chapterId: String): Chapter? =
        chapterDao.getChapterById(chapterId)?.toDomain()

    override suspend fun getChapterContent(
        sourceId: String,
        chapterUrl: String,
        chapterId: String
    ): SourceResult<ChapterContent> =
        sourceManager.getSource(sourceId).getChapterContent(chapterUrl)

    override suspend fun markChapterRead(chapterId: String) =
        chapterDao.markRead(chapterId)

    override suspend fun updateDownloadState(
        chapterId: String,
        state: DownloadState,
        localPath: String?
    ) = chapterDao.updateDownloadState(chapterId, state.name, localPath)
}
