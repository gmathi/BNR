package com.bnr.app.domain.usecase

import com.bnr.app.domain.model.Novel
import com.bnr.app.domain.repository.NovelRepository
import com.bnr.app.source.SourceResult
import javax.inject.Inject

class SearchNovelsUseCase @Inject constructor(
    private val repository: NovelRepository
) {
    suspend operator fun invoke(
        sourceId: String,
        query: String,
        page: Int = 1,
        filters: Map<String, String> = emptyMap()
    ): SourceResult<List<Novel>> =
        repository.searchNovels(sourceId, query, page, filters)
}
