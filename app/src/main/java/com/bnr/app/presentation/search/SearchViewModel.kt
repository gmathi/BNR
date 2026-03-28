package com.bnr.app.presentation.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bnr.app.core.datastore.AppPreferences
import com.bnr.app.domain.model.Novel
import com.bnr.app.domain.usecase.GetLibraryNovelsUseCase
import com.bnr.app.domain.usecase.GetPopularNovelsUseCase
import com.bnr.app.domain.usecase.SearchNovelsUseCase
import com.bnr.app.source.SourceManager
import com.bnr.app.source.SourceResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SourceSearchState(
    val sourceId: String,
    val sourceName: String,
    val novels: List<Novel> = emptyList(),
    val page: Int = 1,
    val isLoading: Boolean = false,
    val hasNextPage: Boolean = true,
    val error: String? = null
)

data class SearchUiState(
    val query: String = "",
    val isSearchMode: Boolean = false,
    // --- Popular mode ---
    val popularSourceId: String = "",
    val popularSourceNames: List<Pair<String, String>> = emptyList(),
    val popularNovels: List<Novel> = emptyList(),
    val popularPage: Int = 1,
    val popularIsLoading: Boolean = false,
    val popularHasNextPage: Boolean = true,
    val popularError: String? = null,
    // --- Search mode ---
    val sourceResults: List<SourceSearchState> = emptyList(),
    // --- Library highlighting ---
    val libraryIds: Set<String> = emptySet()
)

@OptIn(FlowPreview::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val searchNovels: SearchNovelsUseCase,
    private val getPopularNovels: GetPopularNovelsUseCase,
    private val getLibraryNovels: GetLibraryNovelsUseCase,
    private val sourceManager: SourceManager,
    private val appPreferences: AppPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    init {
        // Collect library IDs for highlighting
        getLibraryNovels()
            .map { novels -> novels.map { it.id }.toSet() }
            .onEach { ids -> _uiState.update { it.copy(libraryIds = ids) } }
            .launchIn(viewModelScope)

        // Initialise sources and load popular
        val sources = sourceManager.getAllSources()
        viewModelScope.launch {
            val preferred = appPreferences.preferredSourceId.first()
            val defaultSource = sources.find { it.id == preferred } ?: sources.firstOrNull()
            _uiState.update {
                it.copy(
                    popularSourceId = defaultSource?.id ?: "",
                    popularSourceNames = sources.map { s -> s.id to s.name }
                )
            }
            if (defaultSource != null) loadPopular(defaultSource.id)
        }
    }

    fun onQueryChanged(query: String) {
        _uiState.update { it.copy(query = query) }
        searchJob?.cancel()
        searchJob = null

        if (query.isBlank()) {
            _uiState.update { it.copy(isSearchMode = false) }
            loadPopular(_uiState.value.popularSourceId)
        } else {
            searchJob = viewModelScope.launch {
                kotlinx.coroutines.delay(400L)
                val sources = sourceManager.getAllSources()
                // Reset all source search states
                _uiState.update { state ->
                    state.copy(
                        isSearchMode = true,
                        sourceResults = sources.map { src ->
                            SourceSearchState(
                                sourceId = src.id,
                                sourceName = src.name
                            )
                        }
                    )
                }
                // Fire parallel search across all sources
                sources.forEach { src ->
                    launch {
                        performSourceSearch(src.id, query, page = 1, reset = true)
                    }
                }
            }
        }
    }

    fun onPopularSourceSelected(sourceId: String) {
        _uiState.update {
            it.copy(
                popularSourceId = sourceId,
                popularNovels = emptyList(),
                popularPage = 1
            )
        }
        viewModelScope.launch { appPreferences.setPreferredSourceId(sourceId) }
        loadPopular(sourceId)
    }

    fun loadNextPopularPage() {
        val state = _uiState.value
        if (state.popularIsLoading || !state.popularHasNextPage) return
        loadPopular(state.popularSourceId, state.popularPage + 1)
    }

    fun loadNextSourcePage(sourceId: String) {
        val sourceState = _uiState.value.sourceResults.find { it.sourceId == sourceId } ?: return
        if (sourceState.isLoading || !sourceState.hasNextPage) return
        val query = _uiState.value.query
        if (query.isBlank()) return
        viewModelScope.launch {
            performSourceSearch(sourceId, query, sourceState.page + 1, reset = false)
        }
    }

    // Keep the old method names around as thin wrappers so the existing
    // SearchScreen that references onSourceSelected / loadNextPage still compiles
    // after we port it; these will go away once the screen is fully migrated.
    fun onSourceSelected(sourceId: String) = onPopularSourceSelected(sourceId)

    fun loadNextPage() = loadNextPopularPage()

    // ── Private helpers ───────────────────────────────────────────────────────

    private fun loadPopular(sourceId: String, page: Int = 1) {
        if (sourceId.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(popularIsLoading = true, popularError = null) }
            when (val result = getPopularNovels(sourceId, page)) {
                is SourceResult.Success -> _uiState.update { state ->
                    state.copy(
                        popularNovels = if (page == 1) result.data
                                        else state.popularNovels + result.data,
                        popularPage = page,
                        popularIsLoading = false,
                        popularHasNextPage = result.data.isNotEmpty()
                    )
                }
                is SourceResult.Error -> _uiState.update {
                    it.copy(popularIsLoading = false, popularError = result.exception.message)
                }
            }
        }
    }

    private suspend fun performSourceSearch(
        sourceId: String,
        query: String,
        page: Int,
        reset: Boolean
    ) {
        // Mark source as loading
        _uiState.update { state ->
            state.copy(
                sourceResults = state.sourceResults.map { src ->
                    if (src.sourceId == sourceId) src.copy(isLoading = true, error = null)
                    else src
                }
            )
        }
        when (val result = searchNovels(sourceId, query, page)) {
            is SourceResult.Success -> _uiState.update { state ->
                state.copy(
                    sourceResults = state.sourceResults.map { src ->
                        if (src.sourceId == sourceId) {
                            src.copy(
                                novels = if (reset) result.data else src.novels + result.data,
                                page = page,
                                isLoading = false,
                                hasNextPage = result.data.isNotEmpty()
                            )
                        } else src
                    }
                )
            }
            is SourceResult.Error -> _uiState.update { state ->
                state.copy(
                    sourceResults = state.sourceResults.map { src ->
                        if (src.sourceId == sourceId) {
                            src.copy(isLoading = false, error = result.exception.message)
                        } else src
                    }
                )
            }
        }
    }
}
