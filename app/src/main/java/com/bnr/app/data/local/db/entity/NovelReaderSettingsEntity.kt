package com.bnr.app.data.local.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "novel_reader_settings",
    foreignKeys = [
        ForeignKey(
            entity = NovelEntity::class,
            parentColumns = ["id"],
            childColumns = ["novelId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class NovelReaderSettingsEntity(
    @PrimaryKey val novelId: String,
    val readerModeEnabled: Boolean = true,
    val fontSize: Float = 16f,
    val ttsVoiceId: String? = null,
    val ttsEngineId: String = "android",
    val readerColorPreset: String = "default",   // key from ReaderColorPreset
    val customBgColor: Long? = null,             // ARGB as Long, only if preset=custom
    val customTextColor: Long? = null            // ARGB as Long, only if preset=custom
)
