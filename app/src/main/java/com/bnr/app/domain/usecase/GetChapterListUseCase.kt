package com.bnr.app.domain.usecase

import com.bnr.app.domain.model.Chapter
import com.bnr.app.domain.repository.ChapterRepository
import com.bnr.app.source.SourceResult
import javax.inject.Inject

class GetChapterListUseCase @Inject constructor(
    private val repository: ChapterRepository
) {
    suspend operator fun invoke(
        sourceId: String,
        novelUrl: String,
        novelId: String
    ): SourceResult<List<Chapter>> =
        repository.getChapterList(sourceId, novelUrl, novelId)
}
