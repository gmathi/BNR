package com.bnr.app.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.bnr.app.data.local.saf.SafStorageManager
import com.bnr.app.domain.model.DownloadState
import com.bnr.app.domain.repository.ChapterRepository
import com.bnr.app.domain.repository.DownloadRepository
import com.bnr.app.source.SourceManager
import com.bnr.app.source.SourceResult
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class DownloadWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val sourceManager: SourceManager,
    private val chapterRepository: ChapterRepository,
    private val downloadRepository: DownloadRepository,
    private val safStorageManager: SafStorageManager
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val chapterId  = inputData.getString(KEY_CHAPTER_ID)  ?: return Result.failure()
        val chapterUrl = inputData.getString(KEY_CHAPTER_URL) ?: return Result.failure()
        val novelId    = inputData.getString(KEY_NOVEL_ID)    ?: return Result.failure()
        val sourceId   = inputData.getString(KEY_SOURCE_ID)   ?: return Result.failure()

        chapterRepository.updateDownloadState(chapterId, DownloadState.DOWNLOADING)

        val rootUri = downloadRepository.getDownloadFolderUri()
            ?: return Result.failure()  // User hasn't selected a folder yet

        return try {
            val source = sourceManager.getSource(sourceId)
            when (val contentResult = source.getChapterContent(chapterUrl)) {
                is SourceResult.Success -> {
                    val savedUri = safStorageManager.writeChapterContent(
                        rootUriString = rootUri,
                        novelId       = novelId,
                        chapterId     = chapterId,
                        content       = contentResult.data
                    )
                    if (savedUri != null) {
                        chapterRepository.updateDownloadState(
                            chapterId = chapterId,
                            state     = DownloadState.DOWNLOADED,
                            localPath = savedUri
                        )
                        Result.success()
                    } else {
                        chapterRepository.updateDownloadState(chapterId, DownloadState.FAILED)
                        if (runAttemptCount < MAX_RETRIES) Result.retry() else Result.failure()
                    }
                }
                is SourceResult.Error -> {
                    chapterRepository.updateDownloadState(chapterId, DownloadState.FAILED)
                    if (runAttemptCount < MAX_RETRIES) Result.retry() else Result.failure()
                }
            }
        } catch (e: Exception) {
            chapterRepository.updateDownloadState(chapterId, DownloadState.FAILED)
            if (runAttemptCount < MAX_RETRIES) Result.retry() else Result.failure()
        }
    }

    companion object {
        const val KEY_CHAPTER_ID  = "chapter_id"
        const val KEY_CHAPTER_URL = "chapter_url"
        const val KEY_NOVEL_ID    = "novel_id"
        const val KEY_SOURCE_ID   = "source_id"
        private const val MAX_RETRIES = 3
    }
}
