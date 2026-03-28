package com.bnr.app.data.local.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "chapters",
    foreignKeys = [
        ForeignKey(
            entity = NovelEntity::class,
            parentColumns = ["id"],
            childColumns = ["novelId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("novelId")]
)
data class ChapterEntity(
    @PrimaryKey val id: String,
    val novelId: String,
    val sourceId: String,
    val url: String,
    val title: String,
    val chapterNumber: Float,
    val uploadedAt: Long?,
    val isRead: Boolean = false,
    val downloadState: String = "NOT_DOWNLOADED",   // DownloadState enum name
    val localFilePath: String? = null
)
