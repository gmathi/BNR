package com.bnr.app.presentation.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bnr.app.BuildConfig
import com.bnr.app.core.datastore.AppPreferences
import com.bnr.app.data.local.saf.SafStorageManager
import com.bnr.app.domain.model.TtsVoice
import com.bnr.app.source.SourceManager
import com.bnr.app.tts.TtsManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val downloadFolderUri: String? = null,
    val downloadFolderName: String? = null,
    val availableSources: List<Pair<String, String>> = emptyList(),  // id to name
    val preferredSourceId: String = "",
    val availableVoices: List<TtsVoice> = emptyList(),
    val selectedVoiceId: String? = null,
    val isLoadingVoices: Boolean = false,
    val appVersion: String = ""
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val appPreferences: AppPreferences,
    private val safStorageManager: SafStorageManager,
    private val sourceManager: SourceManager,
    private val ttsManager: TtsManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        val sources = sourceManager.getAllSources().map { it.id to it.name }
        _uiState.update { it.copy(availableSources = sources) }

        viewModelScope.launch {
            appPreferences.downloadFolderUri.collect { uri ->
                _uiState.update { state ->
                    state.copy(
                        downloadFolderUri = uri,
                        downloadFolderName = uri?.let { safStorageManager.getFolderDisplayName(it) }
                    )
                }
            }
        }
        viewModelScope.launch {
            appPreferences.preferredSourceId.collect { id ->
                _uiState.update { it.copy(preferredSourceId = id ?: sources.firstOrNull()?.first ?: "") }
            }
        }
        viewModelScope.launch {
            appPreferences.defaultTtsVoiceId.collect { id ->
                _uiState.update { it.copy(selectedVoiceId = id) }
            }
        }
        loadVoices()
    }

    private fun loadVoices() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingVoices = true) }
            val voices = ttsManager.getAvailableVoices()
            _uiState.update { it.copy(availableVoices = voices, isLoadingVoices = false) }
        }
    }

    /**
     * Called with the URI returned by SAF OpenDocumentTree picker.
     * Takes persistable permission so it survives app restarts.
     */
    fun onDownloadFolderSelected(uri: Uri, context: Context) {
        context.contentResolver.takePersistableUriPermission(
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        )
        viewModelScope.launch {
            appPreferences.setDownloadFolderUri(uri.toString())
        }
    }

    fun onSourceSelected(sourceId: String) {
        viewModelScope.launch {
            appPreferences.setPreferredSourceId(sourceId)
        }
    }

    fun onVoiceSelected(voiceId: String) {
        viewModelScope.launch {
            appPreferences.setDefaultTtsVoiceId(voiceId)
        }
    }
}
