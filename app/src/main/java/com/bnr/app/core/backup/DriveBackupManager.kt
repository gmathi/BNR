package com.bnr.app.core.backup

import android.content.Context
import com.google.gson.Gson
import com.bnr.app.core.datastore.AppPreferences
import com.bnr.app.data.local.db.dao.ChapterDao
import com.bnr.app.data.local.db.dao.NovelDao
import com.bnr.app.data.local.db.dao.ReaderSettingsDao
import com.bnr.app.data.local.db.entity.ChapterEntity
import com.bnr.app.data.local.db.entity.NovelEntity
import com.bnr.app.data.local.db.entity.NovelReaderSettingsEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DriveBackupManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val novelDao: NovelDao,
    private val chapterDao: ChapterDao,
    private val readerSettingsDao: ReaderSettingsDao,
    private val appPreferences: AppPreferences,
    private val okHttpClient: OkHttpClient,
    private val gson: Gson
) {
    companion object {
        private const val DRIVE_FILES_API = "https://www.googleapis.com/upload/drive/v3/files"
        private const val BACKUP_FILENAME = "bnr_backup.json"
        private const val BACKUP_MIME = "application/json"
        private val APP_DATA_FOLDER = listOf("appDataFolder")
    }

    /**
     * Backs up library data + preferences to Google Drive.
     * [accessToken] is obtained from Google Sign-In.
     */
    suspend fun backup(accessToken: String, accountEmail: String): Result<BackupInfo> = runCatching {
        val novels   = novelDao.getLibraryNovels().first()
        val chapters = novels.flatMap { novel ->
            chapterDao.getChaptersByNovelId(novel.id).first()
        }
        val readerSettings = readerSettingsDao.let { dao ->
            novels.mapNotNull { novel ->
                dao.getSettingsByNovelId(novel.id).first()
            }
        }
        val prefs = collectPreferences()

        val backupData = BackupData(
            exportedAt     = System.currentTimeMillis(),
            novels         = novels,
            chapters       = chapters,
            readerSettings = readerSettings,
            preferences    = prefs
        )

        val json = gson.toJson(backupData)
        val fileId = uploadToDrive(accessToken, json)

        appPreferences.setLastBackupTimeMs(System.currentTimeMillis())
        appPreferences.setDriveAccountEmail(accountEmail)

        BackupInfo(
            fileId       = fileId,
            timestamp    = System.currentTimeMillis(),
            novelCount   = novels.size,
            accountEmail = accountEmail
        )
    }

    /**
     * Restores library data from Google Drive.
     */
    suspend fun restore(accessToken: String): Result<Int> = runCatching {
        val fileId = findBackupFileId(accessToken)
            ?: error("No backup found in Google Drive")
        val json   = downloadFromDrive(accessToken, fileId)
        val data   = gson.fromJson(json, BackupData::class.java)

        // Import novels (only library novels, preserve inLibrary=true)
        data.novels.forEach { novelDao.upsertNovel(it.copy(inLibrary = true)) }
        chapterDao.upsertChapters(data.chapters)
        data.readerSettings.forEach { readerSettingsDao.upsertSettings(it) }

        // Restore app preferences that were backed up
        data.preferences["preferred_source"]
            ?.takeIf { it.isNotEmpty() }
            ?.let { appPreferences.setPreferredSourceId(it) }
        data.preferences["reader_font_size"]
            ?.toFloatOrNull()
            ?.let { appPreferences.setReaderFontSize(it) }
        data.preferences["reader_theme"]
            ?.takeIf { it.isNotEmpty() }
            ?.let { appPreferences.setReaderTheme(it) }
        data.preferences["update_interval"]
            ?.toIntOrNull()
            ?.let { appPreferences.setUpdateIntervalHours(it) }

        data.novels.size
    }

    private suspend fun collectPreferences(): Map<String, String> {
        return mapOf(
            "preferred_source"  to (appPreferences.preferredSourceId.first() ?: ""),
            "reader_font_size"  to appPreferences.readerFontSize.first().toString(),
            "reader_theme"      to appPreferences.readerTheme.first(),
            "update_interval"   to appPreferences.updateIntervalHours.first().toString()
        )
    }

    private fun uploadToDrive(accessToken: String, json: String): String {
        // Multipart upload: metadata + content
        val metadataJson = JSONObject().apply {
            put("name", BACKUP_FILENAME)
            put("parents", APP_DATA_FOLDER)
        }.toString()

        val boundary = "backup_boundary_bnr"
        val body = buildString {
            append("--$boundary\r\n")
            append("Content-Type: application/json; charset=UTF-8\r\n\r\n")
            append(metadataJson)
            append("\r\n--$boundary\r\n")
            append("Content-Type: $BACKUP_MIME\r\n\r\n")
            append(json)
            append("\r\n--$boundary--")
        }.toRequestBody("multipart/related; boundary=$boundary".toMediaType())

        val request = Request.Builder()
            .url("$DRIVE_FILES_API?uploadType=multipart&fields=id")
            .header("Authorization", "Bearer $accessToken")
            .post(body)
            .build()

        val response = okHttpClient.newCall(request).execute()
        val responseBody = response.body?.string() ?: error("Empty response from Drive")
        if (!response.isSuccessful) error("Drive upload failed (${response.code})")
        return JSONObject(responseBody).getString("id")
    }

    private fun findBackupFileId(accessToken: String): String? {
        val url = "https://www.googleapis.com/drive/v3/files" +
            "?spaces=appDataFolder&q=name='$BACKUP_FILENAME'&fields=files(id,modifiedTime)&orderBy=modifiedTime desc"
        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $accessToken")
            .get()
            .build()
        val response = okHttpClient.newCall(request).execute()
        val body = response.body?.string() ?: return null
        val files = JSONObject(body).getJSONArray("files")
        return if (files.length() > 0) files.getJSONObject(0).getString("id") else null
    }

    private fun downloadFromDrive(accessToken: String, fileId: String): String {
        val request = Request.Builder()
            .url("https://www.googleapis.com/drive/v3/files/$fileId?alt=media")
            .header("Authorization", "Bearer $accessToken")
            .get()
            .build()
        val response = okHttpClient.newCall(request).execute()
        return response.body?.string() ?: error("Empty backup file from Drive")
    }
}
