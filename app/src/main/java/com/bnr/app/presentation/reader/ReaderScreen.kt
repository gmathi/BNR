package com.bnr.app.presentation.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.isSystemInDarkTheme
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.bnr.app.R
import com.bnr.app.core.ui.components.FullScreenLoading
import com.bnr.app.core.ui.theme.ReaderBackground
import com.bnr.app.core.ui.theme.ReaderBackgroundDark
import com.bnr.app.core.ui.theme.ReaderText
import com.bnr.app.core.ui.theme.ReaderTextDark
import com.bnr.app.presentation.reader.components.ExtractedTextReader
import com.bnr.app.presentation.reader.components.TtsControls
import com.bnr.app.presentation.reader.components.WebViewReader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(
    chapterId: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ReaderViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val darkTheme = isSystemInDarkTheme()

    val bgColor = if (darkTheme) ReaderBackgroundDark else ReaderBackground
    val textColor = if (darkTheme) ReaderTextDark else ReaderText

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(bgColor)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopAppBar(
                title = {
                    Text(
                        text = state.content?.title ?: state.chapter?.title ?: "",
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, stringResource(R.string.cd_back))
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::toggleSettingsSheet) {
                        Icon(Icons.Default.Settings, stringResource(R.string.reader_settings))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = bgColor.copy(alpha = 0.95f)
                )
            )

            Box(modifier = Modifier.weight(1f)) {
                when {
                    state.isLoading -> FullScreenLoading()

                    state.error != null -> Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = state.error!!,
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    state.settings.readerModeEnabled -> {
                        ExtractedTextReader(
                            paragraphs = state.content?.paragraphs ?: emptyList(),
                            fontSize = state.settings.fontSize,
                            textColor = textColor,
                            backgroundColor = bgColor
                        )
                    }

                    else -> {
                        state.chapter?.url?.let { url ->
                            WebViewReader(
                                url = url,
                                cookieJar = viewModel.cookieJar
                            )
                        }
                    }
                }
            }

            if (state.content != null) {
                TtsControls(
                    ttsManager = viewModel.ttsManager,
                    onPlay     = viewModel::playTts,
                    onPause    = viewModel::pauseTts,
                    onStop     = viewModel::stopTts,
                    modifier   = Modifier.fillMaxWidth()
                )
            }
        }

        if (state.showSettingsSheet) {
            ReaderSettingsSheet(
                settings           = state.settings,
                onDismiss          = viewModel::toggleSettingsSheet,
                onReaderModeToggle = viewModel::toggleReaderMode,
                onFontSizeChange   = viewModel::updateFontSize
            )
        }
    }
}
