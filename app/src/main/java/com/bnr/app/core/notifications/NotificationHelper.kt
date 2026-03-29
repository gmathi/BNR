package com.bnr.app.core.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        const val CHANNEL_UPDATE_PROGRESS = "novel_update_progress"
        const val CHANNEL_NEW_CHAPTERS    = "novel_new_chapters"
        const val NOTIF_ID_PROGRESS       = 1001
        const val NOTIF_ID_NEW_CHAPTERS   = 1002
        const val GROUP_KEY_NEW_CHAPTERS  = "com.bnr.app.NEW_CHAPTERS"
    }

    fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java)

        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_UPDATE_PROGRESS,
                "Update Progress",
                NotificationManager.IMPORTANCE_LOW
            ).apply { description = "Shows progress while checking for new chapters" }
        )
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_NEW_CHAPTERS,
                "New Chapters",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = "Notifies when new chapters are available" }
        )
    }

    /** Shows/updates a progress notification. Call with current=total to auto-dismiss. */
    fun showUpdateProgress(current: Int, total: Int) {
        val notification = NotificationCompat.Builder(context, CHANNEL_UPDATE_PROGRESS)
            .setSmallIcon(android.R.drawable.ic_popup_sync)
            .setContentTitle("Checking for updates…")
            .setContentText("$current / $total novels checked")
            .setProgress(total, current, false)
            .setOngoing(current < total)
            .setAutoCancel(current >= total)
            .setSilent(true)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIF_ID_PROGRESS, notification)
    }

    fun dismissProgressNotification() {
        NotificationManagerCompat.from(context).cancel(NOTIF_ID_PROGRESS)
    }

    /**
     * Shows grouped notifications for novels with new chapters.
     * Each novel gets its own child notification; a summary notification groups them.
     * [updates] = list of (novelTitle, newChapterCount)
     */
    fun showNewChapterNotifications(updates: List<Pair<String, Int>>) {
        if (updates.isEmpty()) return
        val nm = NotificationManagerCompat.from(context)

        updates.forEachIndexed { idx, (title, count) ->
            val childNotif = NotificationCompat.Builder(context, CHANNEL_NEW_CHAPTERS)
                .setSmallIcon(android.R.drawable.ic_menu_agenda)
                .setContentTitle(title)
                .setContentText("$count new chapter${if (count > 1) "s" else ""} available")
                .setGroup(GROUP_KEY_NEW_CHAPTERS)
                .setAutoCancel(true)
                .build()
            nm.notify(NOTIF_ID_NEW_CHAPTERS + idx + 1, childNotif)
        }

        // Summary notification (required for grouping on Android 7+)
        val totalNew = updates.sumOf { it.second }
        val summaryNotif = NotificationCompat.Builder(context, CHANNEL_NEW_CHAPTERS)
            .setSmallIcon(android.R.drawable.ic_menu_agenda)
            .setContentTitle("BNR — New Chapters Available")
            .setContentText("$totalNew new chapter${if (totalNew > 1) "s" else ""} across ${updates.size} novels")
            .setStyle(
                NotificationCompat.InboxStyle().also { style ->
                    updates.forEach { (title, count) ->
                        style.addLine("$title (+$count)")
                    }
                    style.setBigContentTitle("New chapters available")
                    style.setSummaryText("${updates.size} novels updated")
                }
            )
            .setGroup(GROUP_KEY_NEW_CHAPTERS)
            .setGroupSummary(true)
            .setAutoCancel(true)
            .build()
        nm.notify(NOTIF_ID_NEW_CHAPTERS, summaryNotif)
    }
}
