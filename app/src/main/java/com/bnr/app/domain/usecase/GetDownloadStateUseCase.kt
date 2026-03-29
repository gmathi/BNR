package com.bnr.app.domain.usecase

import com.bnr.app.domain.model.DownloadState
import com.bnr.app.domain.repository.DownloadRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetDownloadStateUseCase @Inject constructor(
    private val repository: DownloadRepository
) {
    operator fun invoke(chapterId: String): Flow<DownloadState> =
        repository.getDownloadState(chapterId)
}
