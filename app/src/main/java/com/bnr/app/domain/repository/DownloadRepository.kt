package com.bnr.app.domain.repository

import com.bnr.app.domain.model.DownloadState
import kotlinx.coroutines.flow.Flow

interface DownloadRepository {
    fun getDownloadState(chapterId: String): Flow<DownloadState>
    suspend fun enqueueDownload(chapterId: String, chapterUrl: String, novelId: String, sourceId: String)
    suspend fun cancelDownload(chapterId: String)
    suspend fun getDownloadFolderUri(): String?
    suspend fun setDownloadFolderUri(uri: String)
}
