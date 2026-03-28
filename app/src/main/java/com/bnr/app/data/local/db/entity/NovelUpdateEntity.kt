package com.bnr.app.data.local.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "novel_updates",
    foreignKeys = [
        ForeignKey(
            entity = NovelEntity::class,
            parentColumns = ["id"],
            childColumns = ["novelId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class NovelUpdateEntity(
    @PrimaryKey val novelId: String,
    val newChapterCount: Int = 0,
    val knownChapterCount: Int = 0,
    val lastCheckedAt: Long = 0L,
    val lastUpdatedAt: Long? = null
)
