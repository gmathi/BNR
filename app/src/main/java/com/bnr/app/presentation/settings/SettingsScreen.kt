package com.bnr.app.presentation.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.bnr.app.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        uri?.let { viewModel.onDownloadFolderSelected(it, context) }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // ── Download section ──────────────────────────────────────────────────
        SectionHeader(text = stringResource(R.string.settings_download_folder))

        ListItem(
            headlineContent = { Text(stringResource(R.string.settings_download_folder)) },
            supportingContent = {
                Text(
                    text = state.downloadFolderName
                        ?: stringResource(R.string.settings_download_folder_not_set),
                    color = if (state.downloadFolderUri == null)
                        MaterialTheme.colorScheme.error
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            leadingContent = {
                Icon(Icons.Default.Folder, contentDescription = null)
            },
            modifier = Modifier.clickable { folderPickerLauncher.launch(null) }
        )

        HorizontalDivider()

        // ── Source section ────────────────────────────────────────────────────
        SectionHeader(text = stringResource(R.string.settings_default_source))

        if (state.availableSources.isNotEmpty()) {
            var sourceDropdownExpanded by remember { mutableStateOf(false) }
            val selectedSourceName = state.availableSources
                .find { it.first == state.preferredSourceId }?.second ?: ""

            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_default_source)) },
                supportingContent = { Text(selectedSourceName) },
                modifier = Modifier.clickable { sourceDropdownExpanded = true }
            )
            DropdownMenu(
                expanded = sourceDropdownExpanded,
                onDismissRequest = { sourceDropdownExpanded = false }
            ) {
                state.availableSources.forEach { (id, name) ->
                    DropdownMenuItem(
                        text = { Text(name) },
                        onClick = {
                            viewModel.onSourceSelected(id)
                            sourceDropdownExpanded = false
                        }
                    )
                }
            }
        }

        HorizontalDivider()

        // ── TTS section ───────────────────────────────────────────────────────
        SectionHeader(text = stringResource(R.string.settings_tts_section))

        var voiceDialogVisible by remember { mutableStateOf(false) }
        val selectedVoiceName = state.availableVoices
            .find { it.id == state.selectedVoiceId }?.displayName
            ?: stringResource(R.string.settings_tts_voice)

        ListItem(
            headlineContent = { Text(stringResource(R.string.settings_tts_voice)) },
            supportingContent = { Text(selectedVoiceName) },
            modifier = Modifier.clickable { voiceDialogVisible = true }
        )

        if (voiceDialogVisible) {
            AlertDialog(
                onDismissRequest = { voiceDialogVisible = false },
                title = { Text(stringResource(R.string.settings_tts_voice)) },
                text = {
                    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                        state.availableVoices.forEach { voice ->
                            ListItem(
                                headlineContent = { Text(voice.displayName) },
                                supportingContent = { Text(voice.locale) },
                                modifier = Modifier.clickable {
                                    viewModel.onVoiceSelected(voice.id)
                                    voiceDialogVisible = false
                                }
                            )
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { voiceDialogVisible = false }) {
                        Text("Close")
                    }
                }
            )
        }

        HorizontalDivider()
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
    )
}
