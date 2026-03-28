package com.bnr.app.domain.usecase

import com.bnr.app.domain.model.Novel
import com.bnr.app.domain.repository.NovelRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetLibraryNovelsUseCase @Inject constructor(
    private val repository: NovelRepository
) {
    operator fun invoke(): Flow<List<Novel>> = repository.getLibraryNovels()
}
