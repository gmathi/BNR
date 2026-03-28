package com.bnr.app.presentation.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bnr.app.core.datastore.AppPreferences
import com.bnr.app.domain.model.Novel
import com.bnr.app.domain.usecase.GetPopularNovelsUseCase
import com.bnr.app.domain.usecase.SearchNovelsUseCase
import com.bnr.app.source.SourceManager
import com.bnr.app.source.SourceResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SearchUiState(
    val query: String = "",
    val selectedSourceId: String = "",
    val sourceNames: List<Pair<String, String>> = emptyList(), // id to name
    val novels: List<Novel> = emptyList(),
    val page: Int = 1,
    val isLoading: Boolean = false,
    val error: String? = null,
    val hasNextPage: Boolean = true,
    val isSearchMode: Boolean = false
)

@OptIn(FlowPreview::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val searchNovels: SearchNovelsUseCase,
    private val getPopularNovels: GetPopularNovelsUseCase,
    private val sourceManager: SourceManager,
    private val appPreferences: AppPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    init {
        val sources = sourceManager.getAllSources()
        viewModelScope.launch {
            val preferred = appPreferences.preferredSourceId.first()
            val defaultSource = sources.find { it.id == preferred } ?: sources.firstOrNull()
            _uiState.update { it.copy(
                selectedSourceId = defaultSource?.id ?: "",
                sourceNames = sources.map { s -> s.id to s.name }
            )}
            if (defaultSource != null) loadPopular(defaultSource.id)
        }
    }

    fun onQueryChanged(query: String) {
        _uiState.update { it.copy(query = query, isSearchMode = query.isNotBlank()) }
        searchJob?.cancel()
        if (query.isBlank()) {
            loadPopular(_uiState.value.selectedSourceId)
        } else {
            searchJob = viewModelScope.launch {
                kotlinx.coroutines.delay(400L) // debounce
                performSearch(query, page = 1, reset = true)
            }
        }
    }

    fun onSourceSelected(sourceId: String) {
        _uiState.update { it.copy(selectedSourceId = sourceId, novels = emptyList(), page = 1) }
        viewModelScope.launch { appPreferences.setPreferredSourceId(sourceId) }
        val query = _uiState.value.query
        if (query.isBlank()) loadPopular(sourceId) else performSearch(query, 1, true)
    }

    fun loadNextPage() {
        val state = _uiState.value
        if (state.isLoading || !state.hasNextPage) return
        val nextPage = state.page + 1
        if (state.query.isBlank()) loadPopular(state.selectedSourceId, nextPage)
        else performSearch(state.query, nextPage, false)
    }

    private fun loadPopular(sourceId: String, page: Int = 1) {
        if (sourceId.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = getPopularNovels(sourceId, page)) {
                is SourceResult.Success -> _uiState.update { state ->
                    state.copy(
                        novels      = if (page == 1) result.data else state.novels + result.data,
                        page        = page,
                        isLoading   = false,
                        hasNextPage = result.data.isNotEmpty()
                    )
                }
                is SourceResult.Error -> _uiState.update {
                    it.copy(isLoading = false, error = result.exception.message)
                }
            }
        }
    }

    private fun performSearch(query: String, page: Int, reset: Boolean) {
        val sourceId = _uiState.value.selectedSourceId
        if (sourceId.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = searchNovels(sourceId, query, page)) {
                is SourceResult.Success -> _uiState.update { state ->
                    state.copy(
                        novels      = if (reset) result.data else state.novels + result.data,
                        page        = page,
                        isLoading   = false,
                        hasNextPage = result.data.isNotEmpty()
                    )
                }
                is SourceResult.Error -> _uiState.update {
                    it.copy(isLoading = false, error = result.exception.message)
                }
            }
        }
    }
}
