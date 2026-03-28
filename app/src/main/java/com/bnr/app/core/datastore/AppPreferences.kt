package com.bnr.app.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
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
        val KEY_DEFAULT_TTS_VOICE   = stringPreferencesKey("default_tts_voice")
        val KEY_DEFAULT_TTS_ENGINE  = stringPreferencesKey("default_tts_engine")
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

    // ── TTS global defaults ──────────────────────────────────────────────────

    val defaultTtsVoiceId: Flow<String?> =
        dataStore.data.map { it[KEY_DEFAULT_TTS_VOICE] }

    suspend fun setDefaultTtsVoiceId(voiceId: String) =
        dataStore.edit { it[KEY_DEFAULT_TTS_VOICE] = voiceId }

    val defaultTtsEngineId: Flow<String> =
        dataStore.data.map { it[KEY_DEFAULT_TTS_ENGINE] ?: "android" }

    suspend fun setDefaultTtsEngineId(engineId: String) =
        dataStore.edit { it[KEY_DEFAULT_TTS_ENGINE] = engineId }
}
