package com.bnr.app.presentation.library

import androidx.lifecycle.ViewModel
import com.bnr.app.domain.model.Novel
import com.bnr.app.domain.usecase.GetLibraryNovelsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import javax.inject.Inject

@HiltViewModel
class LibraryViewModel @Inject constructor(
    getLibraryNovels: GetLibraryNovelsUseCase
) : ViewModel() {

    val novels: StateFlow<List<Novel>> = getLibraryNovels()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )
}
