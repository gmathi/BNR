package com.bnr.app.domain.repository

import com.bnr.app.domain.model.Chapter
import com.bnr.app.domain.model.ChapterContent
import com.bnr.app.domain.model.DownloadState
import com.bnr.app.source.SourceResult
import kotlinx.coroutines.flow.Flow

interface ChapterRepository {
    suspend fun getChapterList(sourceId: String, novelUrl: String, novelId: String): SourceResult<List<Chapter>>
    fun getCachedChapters(novelId: String): Flow<List<Chapter>>
    suspend fun getChapterById(chapterId: String): Chapter?
    suspend fun getChapterContent(sourceId: String, chapterUrl: String, chapterId: String): SourceResult<ChapterContent>
    suspend fun markChapterRead(chapterId: String)
    suspend fun updateDownloadState(chapterId: String, state: DownloadState, localPath: String? = null)
}
