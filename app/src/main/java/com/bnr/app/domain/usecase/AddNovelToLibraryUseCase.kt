package com.bnr.app.domain.usecase

import com.bnr.app.domain.model.Novel
import com.bnr.app.domain.repository.NovelRepository
import javax.inject.Inject

class AddNovelToLibraryUseCase @Inject constructor(
    private val repository: NovelRepository
) {
    suspend operator fun invoke(novel: Novel) = repository.addToLibrary(novel)
}
