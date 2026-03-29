package com.bnr.app.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppPreferences @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    companion object {
        val KEY_DOWNLOAD_FOLDER_URI = stringPreferencesKey("download_folder_uri")
        val KEY_PREFERRED_SOURCE_ID = stringPreferencesKey("preferred_source_id")
        val KEY_READER_FONT_SIZE    = floatPreferencesKey("reader_font_size")
        val KEY_READER_THEME        = stringPreferencesKey("reader_theme")  // "light", "dark", "system"
        val KEY_APP_THEME           = stringPreferencesKey("app_theme")     // "light" | "dark" | "system"
        val KEY_DEFAULT_TTS_VOICE   = stringPreferencesKey("default_tts_voice")
        val KEY_DEFAULT_TTS_ENGINE  = stringPreferencesKey("default_tts_engine")

        // Background updates
        val KEY_BACKGROUND_UPDATES_ENABLED = booleanPreferencesKey("background_updates_enabled")
        val KEY_UPDATE_INTERVAL_HOURS      = intPreferencesKey("update_interval_hours")

        // Drive backup
        val KEY_DRIVE_BACKUP_ENABLED = booleanPreferencesKey("drive_backup_enabled")
        val KEY_LAST_BACKUP_TIME_MS  = longPreferencesKey("last_backup_time_ms")
        val KEY_DRIVE_ACCOUNT_EMAIL  = stringPreferencesKey("drive_account_email")
    }

    // ── Download folder ──────────────────────────────────────────────────────

    val downloadFolderUri: Flow<String?> =
        dataStore.data.map { it[KEY_DOWNLOAD_FOLDER_URI] }

    suspend fun setDownloadFolderUri(uri: String) =
        dataStore.edit { it[KEY_DOWNLOAD_FOLDER_URI] = uri }

    // ── Source preference ────────────────────────────────────────────────────

    val preferredSourceId: Flow<String?> =
        dataStore.data.map { it[KEY_PREFERRED_SOURCE_ID] }

    suspend fun setPreferredSourceId(id: String) =
        dataStore.edit { it[KEY_PREFERRED_SOURCE_ID] = id }

    // ── Reader global defaults ───────────────────────────────────────────────

    val readerFontSize: Flow<Float> =
        dataStore.data.map { it[KEY_READER_FONT_SIZE] ?: 16f }

    suspend fun setReaderFontSize(size: Float) =
        dataStore.edit { it[KEY_READER_FONT_SIZE] = size }

    val readerTheme: Flow<String> =
        dataStore.data.map { it[KEY_READER_THEME] ?: "system" }

    suspend fun setReaderTheme(theme: String) =
        dataStore.edit { it[KEY_READER_THEME] = theme }

    val appTheme: Flow<String> =
        dataStore.data.map { it[KEY_APP_THEME] ?: "system" }

    suspend fun setAppTheme(theme: String) =
        dataStore.edit { it[KEY_APP_THEME] = theme }

    // ── TTS global defaults ──────────────────────────────────────────────────

    val defaultTtsVoiceId: Flow<String?> =
        dataStore.data.map { it[KEY_DEFAULT_TTS_VOICE] }

    suspend fun setDefaultTtsVoiceId(voiceId: String) =
        dataStore.edit { it[KEY_DEFAULT_TTS_VOICE] = voiceId }

    val defaultTtsEngineId: Flow<String> =
        dataStore.data.map { it[KEY_DEFAULT_TTS_ENGINE] ?: "android" }

    suspend fun setDefaultTtsEngineId(engineId: String) =
        dataStore.edit { it[KEY_DEFAULT_TTS_ENGINE] = engineId }

    // ── Background updates ───────────────────────────────────────────────────

    val backgroundUpdatesEnabled: Flow<Boolean> =
        dataStore.data.map { it[KEY_BACKGROUND_UPDATES_ENABLED] ?: true }

    suspend fun setBackgroundUpdatesEnabled(enabled: Boolean) =
        dataStore.edit { it[KEY_BACKGROUND_UPDATES_ENABLED] = enabled }

    val updateIntervalHours: Flow<Int> =
        dataStore.data.map { it[KEY_UPDATE_INTERVAL_HOURS] ?: 6 }

    suspend fun setUpdateIntervalHours(hours: Int) =
        dataStore.edit { it[KEY_UPDATE_INTERVAL_HOURS] = hours }

    // ── Drive backup ─────────────────────────────────────────────────────────

    val driveBackupEnabled: Flow<Boolean> =
        dataStore.data.map { it[KEY_DRIVE_BACKUP_ENABLED] ?: false }

    suspend fun setDriveBackupEnabled(enabled: Boolean) =
        dataStore.edit { it[KEY_DRIVE_BACKUP_ENABLED] = enabled }

    val lastBackupTimeMs: Flow<Long?> =
        dataStore.data.map { it[KEY_LAST_BACKUP_TIME_MS] }

    suspend fun setLastBackupTimeMs(ms: Long) =
        dataStore.edit { it[KEY_LAST_BACKUP_TIME_MS] = ms }

    val driveAccountEmail: Flow<String?> =
        dataStore.data.map { it[KEY_DRIVE_ACCOUNT_EMAIL] }

    suspend fun setDriveAccountEmail(email: String) =
        dataStore.edit { it[KEY_DRIVE_ACCOUNT_EMAIL] = email }
}
