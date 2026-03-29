package com.bnr.app.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "novels")
data class NovelEntity(
    @PrimaryKey val id: String,          // "${sourceId}::${urlSlug}"
    val sourceId: String,
    val url: String,
    val title: String,
    val author: String,
    val coverUrl: String,
    val description: String,
    val genres: String,                  // JSON array as String e.g. ["Fantasy","Action"]
    val status: String,                  // NovelStatus enum name
    val chapterCount: Int = 0,
    val inLibrary: Boolean = false,
    val lastReadChapterId: String? = null,
    val addedToLibraryAt: Long? = null
)
