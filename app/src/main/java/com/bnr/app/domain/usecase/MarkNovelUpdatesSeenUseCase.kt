package com.bnr.app.domain.usecase

import com.bnr.app.domain.repository.NovelUpdateRepository
import javax.inject.Inject

class MarkNovelUpdatesSeenUseCase @Inject constructor(
    private val repository: NovelUpdateRepository
) {
    suspend operator fun invoke(novelId: String) = repository.markAsSeen(novelId)
}
