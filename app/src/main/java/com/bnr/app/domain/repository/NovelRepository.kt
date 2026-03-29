package com.bnr.app.domain.repository

import com.bnr.app.domain.model.Novel
import com.bnr.app.source.SourceResult
import kotlinx.coroutines.flow.Flow

interface NovelRepository {
    suspend fun getPopularNovels(sourceId: String, page: Int): SourceResult<List<Novel>>
    suspend fun searchNovels(sourceId: String, query: String, page: Int, filters: Map<String, String>): SourceResult<List<Novel>>
    suspend fun getNovelDetail(sourceId: String, novelUrl: String): SourceResult<Novel>

    fun getLibraryNovels(): Flow<List<Novel>>
    suspend fun isInLibrary(novelId: String): Boolean
    suspend fun addToLibrary(novel: Novel)
    suspend fun removeFromLibrary(novelId: String)
    suspend fun updateLastReadChapter(novelId: String, chapterId: String)
}
