package com.bnr.app.data.repository

import com.bnr.app.data.local.db.dao.ReaderSettingsDao
import com.bnr.app.data.local.db.toDomain
import com.bnr.app.data.local.db.toEntity
import com.bnr.app.domain.model.NovelReaderSettings
import com.bnr.app.domain.repository.ReaderSettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReaderSettingsRepositoryImpl @Inject constructor(
    private val readerSettingsDao: ReaderSettingsDao
) : ReaderSettingsRepository {

    override fun getReaderSettings(novelId: String): Flow<NovelReaderSettings> =
        readerSettingsDao.getSettingsByNovelId(novelId).map { entity ->
            entity?.toDomain() ?: NovelReaderSettings(novelId = novelId)
        }

    override suspend fun upsertReaderSettings(settings: NovelReaderSettings) =
        readerSettingsDao.upsertSettings(settings.toEntity())
}
