package com.bnr.app.domain.usecase

import com.bnr.app.domain.model.NovelUpdateInfo
import com.bnr.app.domain.repository.NovelUpdateRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetNovelUpdatesUseCase @Inject constructor(
    private val repository: NovelUpdateRepository
) {
    operator fun invoke(): Flow<List<NovelUpdateInfo>> =
        repository.getUpdatesForLibraryNovels()
}
