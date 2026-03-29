package com.bnr.app.presentation.explore

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.bnr.app.R
import com.bnr.app.core.ui.components.LoadingFooter
import com.bnr.app.core.ui.components.NovelCard
import com.bnr.app.domain.model.Novel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ExploreScreen(
    onNovelClick: (Novel) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ExploreViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    Column(modifier = modifier.fillMaxSize()) {
        // Search bar at top (shared between modes)
        ExploreSearchBar(
            query = state.query,
            onQueryChange = viewModel::onQueryChanged,
            // Source selector only shown in popular mode and when >1 source
            showSourceSelector = !state.isSearchMode && state.popularSourceNames.size > 1,
            sourceNames = state.popularSourceNames,
            selectedSourceId = state.popularSourceId,
            onSourceSelected = viewModel::onPopularSourceSelected,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        )

        if (!state.isSearchMode) {
            // ── Popular mode ──────────────────────────────────────────────────
            PopularContent(
                state = state,
                onNovelClick = onNovelClick,
                onLoadMore = viewModel::loadNextPopularPage
            )
        } else {
            // ── Search mode ───────────────────────────────────────────────────
            SearchContent(
                state = state,
                onNovelClick = onNovelClick,
                onLoadMore = viewModel::loadNextSourcePage
            )
        }
    }
}

// ── Popular mode content ──────────────────────────────────────────────────────

@Composable
private fun PopularContent(
    state: ExploreUiState,
    onNovelClick: (Novel) -> Unit,
    onLoadMore: () -> Unit
) {
    val gridState = rememberLazyGridState()

    LaunchedEffect(gridState) {
        snapshotFlow { gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
            .distinctUntilChanged()
            .filter { idx -> idx != null && idx >= (state.popularNovels.size - 6) }
            .collect { onLoadMore() }
    }

    state.popularError?.let { err ->
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
        items(state.popularNovels, key = { it.id }) { novel ->
            NovelCard(
                novel = novel,
                onClick = { onNovelClick(novel) },
                inLibrary = novel.id in state.libraryIds
            )
        }
        if (state.popularIsLoading) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                LoadingFooter()
            }
        }
    }
}

// ── Search mode content ───────────────────────────────────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SearchContent(
    state: ExploreUiState,
    onNovelClick: (Novel) -> Unit,
    onLoadMore: (String) -> Unit
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        state.sourceResults.forEach { sourceState ->
            // Sticky header for source name
            stickyHeader(key = "header_${sourceState.sourceId}") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.secondaryContainer)
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = sourceState.sourceName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }

            // Content row for this source
            item(key = "content_${sourceState.sourceId}") {
                when {
                    sourceState.isLoading && sourceState.novels.isEmpty() -> {
                        // Loading state — spinner row
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                    sourceState.novels.isEmpty() && !sourceState.isLoading -> {
                        // Empty state
                        Text(
                            text = stringResource(R.string.explore_source_no_results),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                        )
                    }
                    else -> {
                        // Horizontal scroll row of novel cards
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(sourceState.novels, key = { it.id }) { novel ->
                                NovelCard(
                                    novel = novel,
                                    onClick = { onNovelClick(novel) },
                                    inLibrary = novel.id in state.libraryIds
                                )
                            }
                        }
                    }
                }
            }

            // "Load more" button for this source
            if (sourceState.hasNextPage && !sourceState.isLoading) {
                item(key = "loadmore_${sourceState.sourceId}") {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        TextButton(
                            onClick = { onLoadMore(sourceState.sourceId) },
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .padding(end = 12.dp)
                        ) {
                            Text(stringResource(R.string.explore_load_more))
                        }
                    }
                }
            }
        }
    }
}

// ── Search bar ────────────────────────────────────────────────────────────────

@Composable
private fun ExploreSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    showSourceSelector: Boolean,
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
            placeholder = { Text(stringResource(R.string.explore_hint)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        if (showSourceSelector) {
            Box {
                TextButton(onClick = { dropdownExpanded = true }) {
                    Text("${stringResource(R.string.explore_source_label)}: $selectedName")
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
