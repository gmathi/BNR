package com.bnr.app.domain.model

data class Novel(
    val id: String,                      // "${sourceId}::${urlSlug}"
    val sourceId: String,
    val url: String,
    val title: String,
    val author: String,
    val coverUrl: String,
    val description: String,
    val genres: List<String>,
    val status: NovelStatus,
    val inLibrary: Boolean = false,
    val lastReadChapterId: String? = null,
    val addedToLibraryAt: Long? = null,
    val chapterCount: Int = 0
)

enum class NovelStatus { ONGOING, COMPLETED, HIATUS, UNKNOWN }
