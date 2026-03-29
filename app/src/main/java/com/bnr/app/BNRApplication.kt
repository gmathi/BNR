package com.bnr.app

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.bnr.app.core.datastore.AppPreferences
import com.bnr.app.core.notifications.NotificationHelper
import com.bnr.app.worker.UpdateScheduler
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class BNRApplication : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var updateScheduler: UpdateScheduler
    @Inject lateinit var appPreferences: AppPreferences
    @Inject lateinit var notificationHelper: NotificationHelper

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        notificationHelper.createNotificationChannels()
        applicationScope.launch {
            val enabled = appPreferences.backgroundUpdatesEnabled.first()
            val intervalHours = appPreferences.updateIntervalHours.first()
            if (enabled) updateScheduler.schedule(intervalHours)
        }
    }
}
