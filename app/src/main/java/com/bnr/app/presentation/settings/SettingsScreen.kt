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
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.bnr.app.R
import java.text.DateFormat
import java.util.Date

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

    // Google Sign-In launcher — starts the sign-in intent and passes the resulting
    // account token back to the ViewModel for backup/restore operations.
    var pendingDriveAction by remember { mutableStateOf<DriveAction?>(null) }
    val signInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val data = result.data ?: return@rememberLauncherForActivityResult
        val email = data.getStringExtra("email") ?: ""
        val token = data.getStringExtra("access_token") ?: ""
        when (val action = pendingDriveAction) {
            is DriveAction.Backup  -> viewModel.onBackupNow(token, email)
            is DriveAction.Restore -> viewModel.onRestoreFromDrive(token)
            null -> Unit
        }
        pendingDriveAction = null
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // ── Appearance section ────────────────────────────────────────────────
        SectionHeader(text = stringResource(R.string.settings_appearance_section))

        val themeOptions = listOf(
            "light"  to stringResource(R.string.settings_theme_light),
            "dark"   to stringResource(R.string.settings_theme_dark),
            "system" to stringResource(R.string.settings_theme_system)
        )
        ListItem(
            headlineContent = { Text(stringResource(R.string.settings_app_theme)) },
            supportingContent = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    themeOptions.forEach { (key, label) ->
                        FilterChip(
                            selected = state.appTheme == key,
                            onClick  = { viewModel.onAppThemeChanged(key) },
                            label    = { Text(label) }
                        )
                    }
                }
            }
        )
        HorizontalDivider()

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

        // ── Background Updates section ────────────────────────────────────────
        SectionHeader(text = stringResource(R.string.settings_updates_section))

        ListItem(
            headlineContent = { Text(stringResource(R.string.settings_updates_enabled)) },
            trailingContent = {
                Switch(
                    checked = state.backgroundUpdatesEnabled,
                    onCheckedChange = { viewModel.onBackgroundUpdatesToggled(it) }
                )
            }
        )

        if (state.backgroundUpdatesEnabled) {
            var intervalDropdownExpanded by remember { mutableStateOf(false) }
            val intervalOptions = listOf(
                1  to stringResource(R.string.settings_updates_interval_1h),
                3  to stringResource(R.string.settings_updates_interval_3h),
                6  to stringResource(R.string.settings_updates_interval_6h),
                12 to stringResource(R.string.settings_updates_interval_12h),
                24 to stringResource(R.string.settings_updates_interval_24h)
            )
            val selectedIntervalLabel = intervalOptions
                .find { it.first == state.updateIntervalHours }?.second
                ?: "${state.updateIntervalHours}h"

            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_updates_interval)) },
                supportingContent = { Text(selectedIntervalLabel) },
                modifier = Modifier.clickable { intervalDropdownExpanded = true }
            )
            DropdownMenu(
                expanded = intervalDropdownExpanded,
                onDismissRequest = { intervalDropdownExpanded = false }
            ) {
                intervalOptions.forEach { (hours, label) ->
                    DropdownMenuItem(
                        text = { Text(label) },
                        onClick = {
                            viewModel.onUpdateIntervalChanged(hours)
                            intervalDropdownExpanded = false
                        }
                    )
                }
            }
        }

        HorizontalDivider()

        // ── Google Drive Backup section ───────────────────────────────────────
        SectionHeader(text = stringResource(R.string.settings_backup_section))

        ListItem(
            headlineContent = { Text(stringResource(R.string.settings_backup_enabled)) },
            trailingContent = {
                Switch(
                    checked = state.driveBackupEnabled,
                    onCheckedChange = { viewModel.onDriveBackupToggled(it) }
                )
            }
        )

        if (state.driveBackupEnabled) {
            // Loading indicators
            if (state.isBackingUp) {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_backing_up)) },
                    trailingContent = { CircularProgressIndicator() }
                )
            } else if (state.isRestoring) {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_restoring)) },
                    trailingContent = { CircularProgressIndicator() }
                )
            } else {
                // Last backup time
                val lastBackupText = state.lastBackupTime?.let { ms ->
                    val formatted = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
                        .format(Date(ms))
                    stringResource(R.string.settings_last_backup, formatted)
                } ?: stringResource(R.string.settings_never_backed_up)

                ListItem(
                    headlineContent = { Text(lastBackupText) }
                )

                // Back Up Now button
                ListItem(
                    headlineContent = {
                        Button(
                            onClick = {
                                pendingDriveAction = DriveAction.Backup
                                // Launch Google Sign-In for backup
                                val intent = buildGoogleSignInIntent()
                                signInLauncher.launch(intent)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(R.string.settings_backup_now))
                        }
                    }
                )

                // Restore from Drive button
                ListItem(
                    headlineContent = {
                        Button(
                            onClick = {
                                pendingDriveAction = DriveAction.Restore
                                val intent = buildGoogleSignInIntent()
                                signInLauncher.launch(intent)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(R.string.settings_restore))
                        }
                    }
                )
            }

            // Show error if any
            state.backupError?.let { error ->
                ListItem(
                    headlineContent = {
                        Text(
                            text = stringResource(R.string.settings_backup_error, error),
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                )
            }
        }

        HorizontalDivider()
        Spacer(modifier = Modifier.height(32.dp))
    }
}

/**
 * Sealed type representing the pending Drive operation triggered via Google Sign-In.
 */
private sealed interface DriveAction {
    data object Backup  : DriveAction
    data object Restore : DriveAction
}

/**
 * Builds the Intent for Google Sign-In.
 * The launched activity is expected to return the access token via the result data.
 * Replace this with the appropriate Credential Manager or legacy GoogleSignIn intent
 * as needed when integrating the full auth flow.
 */
private fun buildGoogleSignInIntent(): android.content.Intent =
    android.content.Intent("com.google.android.gms.auth.GOOGLE_SIGN_IN")

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
    )
}
