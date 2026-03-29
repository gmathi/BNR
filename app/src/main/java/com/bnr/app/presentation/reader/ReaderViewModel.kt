package com.bnr.app.presentation.reader

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bnr.app.core.network.HostCookieJar
import com.bnr.app.domain.model.Chapter
import com.bnr.app.domain.model.ChapterContent
import com.bnr.app.domain.model.NovelReaderSettings
import com.bnr.app.domain.repository.ChapterRepository
import com.bnr.app.domain.repository.NovelRepository
import com.bnr.app.domain.usecase.GetChapterContentUseCase
import com.bnr.app.domain.usecase.GetReaderSettingsUseCase
import com.bnr.app.domain.usecase.UpdateReaderSettingsUseCase
import com.bnr.app.source.SourceResult
import com.bnr.app.tts.TtsManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ReaderUiState(
    val chapter: Chapter? = null,
    val content: ChapterContent? = null,
    val settings: NovelReaderSettings = NovelReaderSettings(""),
    val isLoading: Boolean = false,
    val error: String? = null,
    val showSettingsSheet: Boolean = false
)

@HiltViewModel
class ReaderViewModel @Inject constructor(
    private val getChapterContent: GetChapterContentUseCase,
    private val getReaderSettings: GetReaderSettingsUseCase,
    private val updateReaderSettings: UpdateReaderSettingsUseCase,
    private val chapterRepository: ChapterRepository,
    private val novelRepository: NovelRepository,
    val ttsManager: TtsManager,
    val cookieJar: HostCookieJar,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val chapterId: String = savedStateHandle["chapterId"]?.let {
        runCatching { java.net.URLDecoder.decode(it, "UTF-8") }.getOrDefault(it)
    } ?: ""

    private val _uiState = MutableStateFlow(ReaderUiState())
    val uiState: StateFlow<ReaderUiState> = _uiState.asStateFlow()

    init {
        loadChapter(chapterId)
        ttsManager.initialize()
    }

    private fun loadChapter(id: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val chapter = chapterRepository.getChapterById(id)
            if (chapter == null) {
                _uiState.update { it.copy(isLoading = false, error = "Chapter not found") }
                return@launch
            }

            _uiState.update { it.copy(chapter = chapter) }

            // Load per-novel reader settings
            getReaderSettings(chapter.novelId).collect { settings ->
                _uiState.update { it.copy(settings = settings) }
            }
        }

        viewModelScope.launch {
            val chapter = chapterRepository.getChapterById(id) ?: return@launch
            when (val result = getChapterContent(chapter)) {
                is SourceResult.Success -> {
                    _uiState.update { it.copy(content = result.data, isLoading = false) }
                    chapterRepository.markChapterRead(id)
                    // Update last read chapter on the novel
                    novelRepository.updateLastReadChapter(chapter.novelId, id)
                }
                is SourceResult.Error -> _uiState.update {
                    it.copy(isLoading = false, error = result.exception.message)
                }
            }
        }
    }

    fun toggleReaderMode() {
        ttsManager.stop()
        val settings = _uiState.value.settings
        viewModelScope.launch {
            updateReaderSettings(settings.copy(readerModeEnabled = !settings.readerModeEnabled))
        }
    }

    fun updateFontSize(size: Float) {
        val settings = _uiState.value.settings
        viewModelScope.launch {
            updateReaderSettings(settings.copy(fontSize = size))
        }
    }

    fun updateTtsVoice(voiceId: String) {
        val settings = _uiState.value.settings
        viewModelScope.launch {
            updateReaderSettings(settings.copy(ttsVoiceId = voiceId))
        }
    }

    fun updateColorPreset(preset: String) {
        val settings = _uiState.value.settings
        viewModelScope.launch {
            updateReaderSettings(settings.copy(readerColorPreset = preset))
        }
    }

    fun updateCustomBgColor(colorArgb: Long) {
        val settings = _uiState.value.settings
        viewModelScope.launch {
            updateReaderSettings(settings.copy(customBgColor = colorArgb))
        }
    }

    fun updateCustomTextColor(colorArgb: Long) {
        val settings = _uiState.value.settings
        viewModelScope.launch {
            updateReaderSettings(settings.copy(customTextColor = colorArgb))
        }
    }

    fun toggleSettingsSheet() {
        _uiState.update { it.copy(showSettingsSheet = !it.showSettingsSheet) }
    }

    fun playTts() {
        val paragraphs = _uiState.value.content?.paragraphs ?: return
        val voiceId = _uiState.value.settings.ttsVoiceId
        ttsManager.speakAll(paragraphs, voiceId)
    }

    fun stopTts() = ttsManager.stop()
    fun pauseTts() = ttsManager.pause()
    fun resumeTts() = ttsManager.resume()

    override fun onCleared() {
        super.onCleared()
        ttsManager.stop()
    }
}
