package com.bnr.app.core.backup

import com.bnr.app.data.local.db.entity.ChapterEntity
import com.bnr.app.data.local.db.entity.NovelEntity
import com.bnr.app.data.local.db.entity.NovelReaderSettingsEntity

data class BackupData(
    val version: Int = 1,
    val exportedAt: Long,
    val novels: List<NovelEntity>,
    val chapters: List<ChapterEntity>,
    val readerSettings: List<NovelReaderSettingsEntity>,
    val preferences: Map<String, String>     // serialized flat key-value
)

data class BackupInfo(
    val fileId: String,
    val timestamp: Long,
    val novelCount: Int,
    val accountEmail: String
)
