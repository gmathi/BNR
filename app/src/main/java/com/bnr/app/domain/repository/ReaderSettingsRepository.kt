package com.bnr.app.domain.repository

import com.bnr.app.domain.model.NovelReaderSettings
import kotlinx.coroutines.flow.Flow

interface ReaderSettingsRepository {
    fun getReaderSettings(novelId: String): Flow<NovelReaderSettings>
    suspend fun upsertReaderSettings(settings: NovelReaderSettings)
}
