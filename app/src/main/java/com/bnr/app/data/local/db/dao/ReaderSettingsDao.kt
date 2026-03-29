package com.bnr.app.data.local.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.bnr.app.data.local.db.entity.NovelReaderSettingsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReaderSettingsDao {

    @Query("SELECT * FROM novel_reader_settings WHERE novelId = :novelId")
    fun getSettingsByNovelId(novelId: String): Flow<NovelReaderSettingsEntity?>

    @Upsert
    suspend fun upsertSettings(settings: NovelReaderSettingsEntity)
}
