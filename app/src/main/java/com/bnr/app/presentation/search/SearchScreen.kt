package com.bnr.app.presentation.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.bnr.app.R
import com.bnr.app.core.ui.components.LoadingFooter
import com.bnr.app.core.ui.components.NovelCard
import com.bnr.app.domain.model.Novel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onNovelClick: (Novel) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SearchViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val gridState = rememberLazyGridState()

    // Trigger next page when near the end
    LaunchedEffect(gridState) {
        snapshotFlow { gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
            .distinctUntilChanged()
            .filter { idx -> idx != null && idx >= (state.novels.size - 6) }
            .collect { viewModel.loadNextPage() }
    }

    Column(modifier = modifier.fillMaxSize()) {
        // Search bar + source picker row
        SearchBar(
            query = state.query,
            onQueryChange = viewModel::onQueryChanged,
            sourceNames = state.sourceNames,
            selectedSourceId = state.selectedSourceId,
            onSourceSelected = viewModel::onSourceSelected,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        )

        state.error?.let { err ->
            Text(
                text = err,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        LazyVerticalGrid(
            state = gridState,
            columns = GridCells.Adaptive(minSize = 130.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(state.novels, key = { it.id }) { novel ->
                NovelCard(novel = novel, onClick = { onNovelClick(novel) })
            }
            if (state.isLoading) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    LoadingFooter()
                }
            }
        }
    }
}

@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    sourceNames: List<Pair<String, String>>,
    selectedSourceId: String,
    onSourceSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var dropdownExpanded by remember { mutableStateOf(false) }
    val selectedName = sourceNames.find { it.first == selectedSourceId }?.second ?: ""

    Column(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = { Text(stringResource(R.string.search_hint)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        if (sourceNames.size > 1) {
            Box {
                TextButton(onClick = { dropdownExpanded = true }) {
                    Text("${stringResource(R.string.search_source_label)}: $selectedName")
                }
                DropdownMenu(
                    expanded = dropdownExpanded,
                    onDismissRequest = { dropdownExpanded = false }
                ) {
                    sourceNames.forEach { (id, name) ->
                        DropdownMenuItem(
                            text = { Text(name) },
                            onClick = {
                                onSourceSelected(id)
                                dropdownExpanded = false
                            }
                        )
                    }
                }
            }
        }
    }
}
