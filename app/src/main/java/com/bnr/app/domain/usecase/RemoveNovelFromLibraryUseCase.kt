package com.bnr.app.domain.usecase

import com.bnr.app.domain.repository.NovelRepository
import javax.inject.Inject

class RemoveNovelFromLibraryUseCase @Inject constructor(
    private val repository: NovelRepository
) {
    suspend operator fun invoke(novelId: String) = repository.removeFromLibrary(novelId)
}
