package com.bnr.app.presentation.noveldetail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.bnr.app.R
import com.bnr.app.core.ui.components.ChapterItem
import com.bnr.app.core.ui.components.FullScreenLoading
import com.bnr.app.domain.model.Chapter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NovelDetailScreen(
    sourceId: String,
    novelUrl: String,
    onBack: () -> Unit,
    onReadChapter: (chapterId: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: NovelDetailViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = state.novel?.title ?: "",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, stringResource(R.string.cd_back))
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::toggleLibrary) {
                        Icon(
                            imageVector = if (state.inLibrary)
                                Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            contentDescription = if (state.inLibrary)
                                stringResource(R.string.detail_remove_library)
                            else
                                stringResource(R.string.detail_add_library)
                        )
                    }
                    IconButton(onClick = viewModel::downloadAll) {
                        Icon(Icons.Default.CloudDownload, stringResource(R.string.detail_download_all))
                    }
                }
            )
        },
        modifier = modifier
    ) { innerPadding ->
        if (state.isLoadingDetail && state.novel == null) {
            FullScreenLoading(modifier = Modifier.padding(innerPadding))
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                state.novel?.let { novel ->
                    item {
                        NovelHeader(
                            coverUrl = novel.coverUrl,
                            author = novel.author,
                            description = novel.description,
                            genres = novel.genres,
                            status = novel.status.name,
                            inLibrary = state.inLibrary,
                            onToggleLibrary = viewModel::toggleLibrary,
                            onReadFirst = {
                                state.chapters.firstOrNull()?.let { ch ->
                                    onReadChapter(ch.id)
                                }
                            }
                        )
                    }
                }

                item {
                    Text(
                        text = stringResource(R.string.detail_chapters),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }

                items(state.chapters, key = { it.id }) { chapter ->
                    ChapterItem(
                        chapter = chapter,
                        onClick = { onReadChapter(chapter.id) },
                        onDownloadClick = { viewModel.downloadChapter(chapter) }
                    )
                }

                if (state.isLoadingChapters) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            androidx.compose.material3.CircularProgressIndicator()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NovelHeader(
    coverUrl: String,
    author: String,
    description: String,
    genres: List<String>,
    status: String,
    inLibrary: Boolean,
    onToggleLibrary: () -> Unit,
    onReadFirst: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            AsyncImage(
                model = coverUrl,
                contentDescription = stringResource(R.string.cd_novel_cover),
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .width(120.dp)
                    .aspectRatio(2f / 3f)
                    .clip(MaterialTheme.shapes.medium)
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(text = author, style = MaterialTheme.typography.bodyMedium)
                Text(
                    text = status,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                if (genres.isNotEmpty()) {
                    Text(
                        text = genres.take(3).joinToString(" · "),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onReadFirst, modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.detail_read))
            }
            OutlinedButton(onClick = onToggleLibrary, modifier = Modifier.weight(1f)) {
                Text(
                    if (inLibrary) stringResource(R.string.detail_remove_library)
                    else stringResource(R.string.detail_add_library)
                )
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        if (description.isNotBlank()) {
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
