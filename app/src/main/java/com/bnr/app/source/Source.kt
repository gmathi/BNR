package com.bnr.app.source

import com.bnr.app.domain.model.Chapter
import com.bnr.app.domain.model.ChapterContent
import com.bnr.app.domain.model.Novel

/**
 * Pluggable source contract. Implement this interface for each novel site/API.
 *
 * [id] must be a stable, unique reverse-domain string (e.g. "com.source.royalroad").
 * Never change [id] after release — it is used as the key in the database.
 */
interface Source {
    val id: String
    val name: String
    val baseUrl: String
    val language: String
    val requiresCloudflareBypass: Boolean get() = false

    suspend fun getPopularNovels(page: Int): SourceResult<List<Novel>>

    suspend fun searchNovels(
        query: String,
        page: Int,
        filters: Map<String, String> = emptyMap()
    ): SourceResult<List<Novel>>

    suspend fun getNovelDetail(novelUrl: String): SourceResult<Novel>

    suspend fun getChapterList(novelUrl: String): SourceResult<List<Chapter>>

    suspend fun getChapterContent(chapterUrl: String): SourceResult<ChapterContent>

    suspend fun getAvailableFilters(): List<SourceFilter> = emptyList()
}

data class SourceFilter(
    val key: String,
    val label: String,
    val options: List<FilterOption>
)

data class FilterOption(val value: String, val label: String)
