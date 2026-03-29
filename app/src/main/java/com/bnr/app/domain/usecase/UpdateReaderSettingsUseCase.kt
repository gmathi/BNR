package com.bnr.app.domain.usecase

import com.bnr.app.domain.model.NovelReaderSettings
import com.bnr.app.domain.repository.ReaderSettingsRepository
import javax.inject.Inject

class UpdateReaderSettingsUseCase @Inject constructor(
    private val repository: ReaderSettingsRepository
) {
    suspend operator fun invoke(settings: NovelReaderSettings) =
        repository.upsertReaderSettings(settings)
}
