package com.bnr.app.domain.usecase

import com.bnr.app.domain.model.Novel
import com.bnr.app.domain.repository.NovelRepository
import com.bnr.app.source.SourceResult
import javax.inject.Inject

class GetNovelDetailUseCase @Inject constructor(
    private val repository: NovelRepository
) {
    suspend operator fun invoke(sourceId: String, novelUrl: String): SourceResult<Novel> =
        repository.getNovelDetail(sourceId, novelUrl)
}
