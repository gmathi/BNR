package com.bnr.app.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "downloads")
data class DownloadEntity(
    @PrimaryKey val chapterId: String,
    val novelId: String,
    val sourceId: String,
    val chapterUrl: String,
    val workerId: String?,               // WorkManager UUID string for cancellation
    val state: String,                   // DownloadState enum name
    val enqueuedAt: Long
)
