package com.bnr.app.presentation.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bnr.app.BuildConfig
import com.bnr.app.core.backup.DriveBackupManager
import com.bnr.app.core.datastore.AppPreferences
import com.bnr.app.data.local.saf.SafStorageManager
import com.bnr.app.domain.model.TtsVoice
import com.bnr.app.source.SourceManager
import com.bnr.app.tts.TtsManager
import com.bnr.app.worker.UpdateScheduler
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
    val appVersion: String = "",
    // Background updates
    val backgroundUpdatesEnabled: Boolean = true,
    val updateIntervalHours: Int = 6,
    // Drive backup
    val driveBackupEnabled: Boolean = false,
    val lastBackupTime: Long? = null,
    val driveAccountEmail: String? = null,
    val isBackingUp: Boolean = false,
    val isRestoring: Boolean = false,
    val backupError: String? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val appPreferences: AppPreferences,
    private val safStorageManager: SafStorageManager,
    private val sourceManager: SourceManager,
    private val ttsManager: TtsManager,
    private val updateScheduler: UpdateScheduler,
    private val driveBackupManager: DriveBackupManager
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
        viewModelScope.launch {
            appPreferences.backgroundUpdatesEnabled.collect { enabled ->
                _uiState.update { it.copy(backgroundUpdatesEnabled = enabled) }
            }
        }
        viewModelScope.launch {
            appPreferences.updateIntervalHours.collect { hours ->
                _uiState.update { it.copy(updateIntervalHours = hours) }
            }
        }
        viewModelScope.launch {
            appPreferences.driveBackupEnabled.collect { enabled ->
                _uiState.update { it.copy(driveBackupEnabled = enabled) }
            }
        }
        viewModelScope.launch {
            appPreferences.lastBackupTimeMs.collect { ms ->
                _uiState.update { it.copy(lastBackupTime = ms) }
            }
        }
        viewModelScope.launch {
            appPreferences.driveAccountEmail.collect { email ->
                _uiState.update { it.copy(driveAccountEmail = email) }
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

    // ── Background updates ────────────────────────────────────────────────────

    fun onBackgroundUpdatesToggled(enabled: Boolean) {
        viewModelScope.launch {
            appPreferences.setBackgroundUpdatesEnabled(enabled)
            if (enabled) {
                updateScheduler.schedule(_uiState.value.updateIntervalHours)
            } else {
                updateScheduler.cancel()
            }
        }
    }

    fun onUpdateIntervalChanged(hours: Int) {
        viewModelScope.launch {
            appPreferences.setUpdateIntervalHours(hours)
            if (_uiState.value.backgroundUpdatesEnabled) {
                updateScheduler.schedule(hours)
            }
        }
    }

    // ── Drive backup ──────────────────────────────────────────────────────────

    fun onDriveBackupToggled(enabled: Boolean) {
        viewModelScope.launch {
            appPreferences.setDriveBackupEnabled(enabled)
        }
    }

    fun onBackupNow(accessToken: String, email: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isBackingUp = true, backupError = null) }
            val result = driveBackupManager.backup(accessToken, email)
            result.fold(
                onSuccess = { info ->
                    _uiState.update { it.copy(isBackingUp = false, lastBackupTime = info.timestamp) }
                },
                onFailure = { error ->
                    _uiState.update { it.copy(isBackingUp = false, backupError = error.message) }
                }
            )
        }
    }

    fun onRestoreFromDrive(accessToken: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isRestoring = true, backupError = null) }
            val result = driveBackupManager.restore(accessToken)
            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(isRestoring = false) }
                },
                onFailure = { error ->
                    _uiState.update { it.copy(isRestoring = false, backupError = error.message) }
                }
            )
        }
    }
}
