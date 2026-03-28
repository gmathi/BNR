package com.bnr.app.presentation.reader

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.bnr.app.R
import com.bnr.app.domain.model.NovelReaderSettings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderSettingsSheet(
    settings: NovelReaderSettings,
    onDismiss: () -> Unit,
    onReaderModeToggle: () -> Unit,
    onFontSizeChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(R.string.reader_settings),
                style = MaterialTheme.typography.titleMedium
            )

            // Reader Mode toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.reader_mode_label),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = if (settings.readerModeEnabled)
                            stringResource(R.string.reader_mode_extracted)
                        else
                            stringResource(R.string.reader_mode_webview),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = settings.readerModeEnabled,
                    onCheckedChange = { onReaderModeToggle() }
                )
            }

            // Font size slider (only meaningful in reader mode)
            if (settings.readerModeEnabled) {
                var sliderValue by remember(settings.fontSize) {
                    mutableStateOf(settings.fontSize)
                }
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = stringResource(R.string.reader_font_size),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "${sliderValue.toInt()}sp",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Slider(
                        value = sliderValue,
                        onValueChange = { sliderValue = it },
                        onValueChangeFinished = { onFontSizeChange(sliderValue) },
                        valueRange = 12f..28f,
                        steps = 7
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}
