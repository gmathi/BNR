package com.bnr.app.domain.model

data class Chapter(
    val id: String,                      // "${novelId}::${chapterIndex}"
    val novelId: String,
    val sourceId: String,
    val url: String,
    val title: String,
    val chapterNumber: Float,            // allows half-chapters like 12.5
    val uploadedAt: Long?,
    val isRead: Boolean = false,
    val downloadState: DownloadState = DownloadState.NOT_DOWNLOADED,
    val localFilePath: String? = null
)
