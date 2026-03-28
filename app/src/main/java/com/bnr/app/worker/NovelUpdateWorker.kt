package com.bnr.app.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.bnr.app.core.datastore.AppPreferences
import com.bnr.app.core.notifications.NotificationHelper
import com.bnr.app.domain.repository.NovelRepository
import com.bnr.app.domain.repository.NovelUpdateRepository
import com.bnr.app.source.SourceManager
import com.bnr.app.source.SourceResult
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first

@HiltWorker
class NovelUpdateWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val novelRepository: NovelRepository,
    private val novelUpdateRepository: NovelUpdateRepository,
    private val sourceManager: SourceManager,
    private val notificationHelper: NotificationHelper,
    private val appPreferences: AppPreferences
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        // Check if background updates are enabled
        if (!appPreferences.backgroundUpdatesEnabled.first()) return Result.success()

        val libraryNovels = novelRepository.getLibraryNovels().first()
        if (libraryNovels.isEmpty()) return Result.success()

        // Group novels by source for rate limiting
        val novelsBySource = libraryNovels.groupBy { it.sourceId }
        val totalNovels = libraryNovels.size
        var checked = 0
        val updatedNovels = mutableListOf<Pair<String, Int>>() // title to newChapters

        notificationHelper.showUpdateProgress(0, totalNovels)

        for ((sourceId, novels) in novelsBySource) {
            val source = sourceManager.getSourceOrNull(sourceId) ?: continue
            val rateLimit = source.rateLimit

            novels.forEachIndexed { indexInBatch, novel ->
                try {
                    val result = source.getChapterList(novel.url)
                    if (result is SourceResult.Success) {
                        val currentCount = result.data.size
                        val knownCount = novelUpdateRepository.getKnownChapterCount(novel.id) ?: 0
                        novelUpdateRepository.recordCheckResult(
                            novelId = novel.id,
                            currentChapterCount = currentCount,
                            checkedAt = System.currentTimeMillis()
                        )
                        val delta = (currentCount - knownCount).coerceAtLeast(0)
                        if (delta > 0) updatedNovels.add(novel.title to delta)
                    }
                } catch (_: Exception) { /* skip failed novel, don't abort worker */ }

                checked++
                notificationHelper.showUpdateProgress(checked, totalNovels)

                // Rate limiting: pause after every requestsPerBatch requests
                if ((indexInBatch + 1) % rateLimit.requestsPerBatch == 0 &&
                    indexInBatch < novels.lastIndex) {
                    delay(rateLimit.delayBetweenBatchesMs)
                }
            }

            // Also delay between different sources
            delay(rateLimit.delayBetweenBatchesMs)
        }

        notificationHelper.dismissProgressNotification()
        if (updatedNovels.isNotEmpty()) {
            notificationHelper.showNewChapterNotifications(updatedNovels)
        }

        return Result.success()
    }
}
