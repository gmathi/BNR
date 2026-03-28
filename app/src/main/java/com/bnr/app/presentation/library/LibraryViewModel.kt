package com.bnr.app.presentation.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bnr.app.domain.model.Novel
import com.bnr.app.domain.usecase.GetLibraryNovelsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import javax.inject.Inject

enum class LibrarySortOption {
    DATE_ADDED, TITLE_AZ, TITLE_ZA, AUTHOR, STATUS
}

data class LibraryUiState(
    val displayedNovels: List<Novel> = emptyList(),
    val allNovels: List<Novel> = emptyList(),
    val searchQuery: String = "",
    val sortOption: LibrarySortOption = LibrarySortOption.DATE_ADDED
)

@HiltViewModel
class LibraryViewModel @Inject constructor(
    getLibraryNovels: GetLibraryNovelsUseCase
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _sortOption = MutableStateFlow(LibrarySortOption.DATE_ADDED)

    val uiState: StateFlow<LibraryUiState> = combine(
        getLibraryNovels(),
        _searchQuery,
        _sortOption
    ) { novels, query, sort ->
        val filtered = if (query.isBlank()) novels
        else novels.filter { novel ->
            novel.title.contains(query, ignoreCase = true) ||
                novel.author.contains(query, ignoreCase = true)
        }
        val sorted = when (sort) {
            LibrarySortOption.DATE_ADDED -> filtered.sortedByDescending { it.addedToLibraryAt ?: 0L }
            LibrarySortOption.TITLE_AZ   -> filtered.sortedBy { it.title.lowercase() }
            LibrarySortOption.TITLE_ZA   -> filtered.sortedByDescending { it.title.lowercase() }
            LibrarySortOption.AUTHOR     -> filtered.sortedBy { it.author.lowercase() }
            LibrarySortOption.STATUS     -> filtered.sortedBy { it.status.ordinal }
        }
        LibraryUiState(
            displayedNovels = sorted,
            allNovels = novels,
            searchQuery = query,
            sortOption = sort
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = LibraryUiState()
    )

    fun onSearchQueryChanged(query: String) = _searchQuery.update { query }

    fun onSortOptionSelected(option: LibrarySortOption) = _sortOption.update { option }
}
