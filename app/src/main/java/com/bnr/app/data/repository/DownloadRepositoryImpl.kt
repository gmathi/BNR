package com.bnr.app.data.repository

import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.bnr.app.core.datastore.AppPreferences
import com.bnr.app.data.local.db.dao.ChapterDao
import com.bnr.app.data.local.db.dao.DownloadDao
import com.bnr.app.data.local.db.entity.DownloadEntity
import com.bnr.app.domain.model.DownloadState
import com.bnr.app.domain.repository.DownloadRepository
import com.bnr.app.worker.DownloadWorker
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DownloadRepositoryImpl @Inject constructor(
    private val downloadDao: DownloadDao,
    private val chapterDao: ChapterDao,
    private val workManager: WorkManager,
    private val appPreferences: AppPreferences
) : DownloadRepository {

    override fun getDownloadState(chapterId: String): Flow<DownloadState> =
        downloadDao.getDownloadByChapterId(chapterId).map { entity ->
            entity?.state?.let { runCatching { DownloadState.valueOf(it) }.getOrNull() }
                ?: DownloadState.NOT_DOWNLOADED
        }

    override suspend fun enqueueDownload(
        chapterId: String,
        chapterUrl: String,
        novelId: String,
        sourceId: String
    ) {
        val inputData = workDataOf(
            DownloadWorker.KEY_CHAPTER_ID  to chapterId,
            DownloadWorker.KEY_CHAPTER_URL to chapterUrl,
            DownloadWorker.KEY_NOVEL_ID    to novelId,
            DownloadWorker.KEY_SOURCE_ID   to sourceId
        )

        val request = OneTimeWorkRequestBuilder<DownloadWorker>()
            .setInputData(inputData)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .addTag("download_$chapterId")
            .build()

        val workerId = request.id.toString()

        // Record in DB
        downloadDao.upsertDownload(
            DownloadEntity(
                chapterId  = chapterId,
                novelId    = novelId,
                sourceId   = sourceId,
                chapterUrl = chapterUrl,
                workerId   = workerId,
                state      = DownloadState.QUEUED.name,
                enqueuedAt = System.currentTimeMillis()
            )
        )
        chapterDao.updateDownloadState(chapterId, DownloadState.QUEUED.name, null)

        workManager.enqueueUniqueWork(
            "dl_$chapterId",
            ExistingWorkPolicy.KEEP,
            request
        )
    }

    override suspend fun cancelDownload(chapterId: String) {
        workManager.cancelUniqueWork("dl_$chapterId")
        downloadDao.deleteDownload(chapterId)
        chapterDao.updateDownloadState(chapterId, DownloadState.NOT_DOWNLOADED.name, null)
    }

    override suspend fun getDownloadFolderUri(): String? =
        appPreferences.downloadFolderUri.first()

    override suspend fun setDownloadFolderUri(uri: String) =
        appPreferences.setDownloadFolderUri(uri)
}
