package com.bnr.app.worker

import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UpdateScheduler @Inject constructor(
    private val workManager: WorkManager
) {
    companion object {
        const val WORK_NAME = "novel_background_updates"
    }

    fun schedule(intervalHours: Int) {
        val request = PeriodicWorkRequestBuilder<NovelUpdateWorker>(
            repeatInterval = intervalHours.toLong(),
            repeatIntervalTimeUnit = TimeUnit.HOURS
        )
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()

        workManager.enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    fun cancel() {
        workManager.cancelUniqueWork(WORK_NAME)
    }
}
