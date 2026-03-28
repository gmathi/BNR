package com.bnr.app.presentation.noveldetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bnr.app.domain.model.Chapter
import com.bnr.app.domain.model.Novel
import com.bnr.app.domain.usecase.AddNovelToLibraryUseCase
import com.bnr.app.domain.usecase.DownloadChapterUseCase
import com.bnr.app.domain.usecase.GetChapterListUseCase
import com.bnr.app.domain.usecase.GetNovelDetailUseCase
import com.bnr.app.domain.usecase.RemoveNovelFromLibraryUseCase
import com.bnr.app.domain.repository.ChapterRepository
import com.bnr.app.source.SourceResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NovelDetailUiState(
    val novel: Novel? = null,
    val chapters: List<Chapter> = emptyList(),
    val isLoadingDetail: Boolean = false,
    val isLoadingChapters: Boolean = false,
    val error: String? = null,
    val inLibrary: Boolean = false
)

@HiltViewModel
class NovelDetailViewModel @Inject constructor(
    private val getNovelDetail: GetNovelDetailUseCase,
    private val getChapterList: GetChapterListUseCase,
    private val addToLibrary: AddNovelToLibraryUseCase,
    private val removeFromLibrary: RemoveNovelFromLibraryUseCase,
    private val downloadChapter: DownloadChapterUseCase,
    private val chapterRepository: ChapterRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val sourceId: String = savedStateHandle["sourceId"] ?: ""
    private val novelUrl: String = savedStateHandle["encodedUrl"]?.let {
        java.net.URLDecoder.decode(it, "UTF-8")
    } ?: ""

    private val _uiState = MutableStateFlow(NovelDetailUiState())
    val uiState: StateFlow<NovelDetailUiState> = _uiState.asStateFlow()

    init {
        loadDetail()
    }

    private fun loadDetail() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingDetail = true, error = null) }
            when (val result = getNovelDetail(sourceId, novelUrl)) {
                is SourceResult.Success -> {
                    _uiState.update { it.copy(
                        novel = result.data,
                        inLibrary = result.data.inLibrary,
                        isLoadingDetail = false
                    )}
                    loadChapters(result.data)
                }
                is SourceResult.Error -> _uiState.update {
                    it.copy(isLoadingDetail = false, error = result.exception.message)
                }
            }
        }
    }

    private fun loadChapters(novel: Novel) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingChapters = true) }
            when (val result = getChapterList(sourceId, novelUrl, novel.id)) {
                is SourceResult.Success -> _uiState.update {
                    it.copy(chapters = result.data, isLoadingChapters = false)
                }
                is SourceResult.Error -> _uiState.update {
                    it.copy(isLoadingChapters = false, error = result.exception.message)
                }
            }
            // Also observe cached chapters (for download state updates)
            chapterRepository.getCachedChapters(novel.id).collect { cached ->
                if (cached.isNotEmpty()) {
                    _uiState.update { it.copy(chapters = cached) }
                }
            }
        }
    }

    fun toggleLibrary() {
        val novel = _uiState.value.novel ?: return
        viewModelScope.launch {
            if (_uiState.value.inLibrary) {
                removeFromLibrary(novel.id)
                _uiState.update { it.copy(inLibrary = false) }
            } else {
                addToLibrary(novel)
                _uiState.update { it.copy(inLibrary = true) }
            }
        }
    }

    fun downloadChapter(chapter: Chapter) {
        viewModelScope.launch { downloadChapter.invoke(chapter) }
    }

    fun downloadAll() {
        val chapters = _uiState.value.chapters
        viewModelScope.launch {
            chapters.forEach { chapter -> downloadChapter.invoke(chapter) }
        }
    }

    fun refresh() = loadDetail()
}
