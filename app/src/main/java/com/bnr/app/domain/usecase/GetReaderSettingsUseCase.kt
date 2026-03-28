package com.bnr.app.domain.usecase

import com.bnr.app.domain.model.NovelReaderSettings
import com.bnr.app.domain.repository.ReaderSettingsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetReaderSettingsUseCase @Inject constructor(
    private val repository: ReaderSettingsRepository
) {
    operator fun invoke(novelId: String): Flow<NovelReaderSettings> =
        repository.getReaderSettings(novelId)
}
