package com.bnr.app.presentation.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.bnr.app.R
import com.bnr.app.core.ui.theme.ReaderBgAmoled
import com.bnr.app.core.ui.theme.ReaderBgDarkPreset
import com.bnr.app.core.ui.theme.ReaderBgForest
import com.bnr.app.core.ui.theme.ReaderBgNightBlue
import com.bnr.app.core.ui.theme.ReaderBgPaper
import com.bnr.app.core.ui.theme.ReaderBgSolarized
import com.bnr.app.core.ui.theme.ReaderColorPreset
import com.bnr.app.core.ui.theme.ReaderTextAmoled
import com.bnr.app.core.ui.theme.ReaderTextDarkPreset
import com.bnr.app.core.ui.theme.ReaderTextForest
import com.bnr.app.core.ui.theme.ReaderTextNightBlue
import com.bnr.app.core.ui.theme.ReaderTextPaper
import com.bnr.app.core.ui.theme.ReaderTextSolarized
import com.bnr.app.domain.model.NovelReaderSettings

private fun presetSwatchColor(preset: ReaderColorPreset): Pair<Color, Color> = when (preset) {
    ReaderColorPreset.DEFAULT    -> Pair(Color(0xFFFEFBFF), Color(0xFF1B1B1F))
    ReaderColorPreset.PAPER      -> Pair(ReaderBgPaper, ReaderTextPaper)
    ReaderColorPreset.DARK       -> Pair(ReaderBgDarkPreset, ReaderTextDarkPreset)
    ReaderColorPreset.AMOLED     -> Pair(ReaderBgAmoled, ReaderTextAmoled)
    ReaderColorPreset.SOLARIZED  -> Pair(ReaderBgSolarized, ReaderTextSolarized)
    ReaderColorPreset.FOREST     -> Pair(ReaderBgForest, ReaderTextForest)
    ReaderColorPreset.NIGHT_BLUE -> Pair(ReaderBgNightBlue, ReaderTextNightBlue)
    ReaderColorPreset.CUSTOM     -> Pair(Color(0xFFFFFFFF), Color(0xFF000000))
}

@Composable
private fun presetLabel(preset: ReaderColorPreset): String = when (preset) {
    ReaderColorPreset.DEFAULT    -> stringResource(R.string.reader_theme_default)
    ReaderColorPreset.PAPER      -> stringResource(R.string.reader_theme_paper)
    ReaderColorPreset.DARK       -> stringResource(R.string.reader_theme_dark)
    ReaderColorPreset.AMOLED     -> stringResource(R.string.reader_theme_amoled)
    ReaderColorPreset.SOLARIZED  -> stringResource(R.string.reader_theme_solarized)
    ReaderColorPreset.FOREST     -> stringResource(R.string.reader_theme_forest)
    ReaderColorPreset.NIGHT_BLUE -> stringResource(R.string.reader_theme_night_blue)
    ReaderColorPreset.CUSTOM     -> stringResource(R.string.reader_theme_custom)
}

private val BG_COLOR_OPTIONS: List<Long> = listOf(
    0xFFFFFFFF, // white
    0xFFF5F0E8, // cream (paper)
    0xFFD2B48C, // tan
    0xFFBDBDBD, // gray
    0xFF616161, // charcoal
    0xFF212121, // near black
    0xFF000000, // black
    0xFF0D1B2A, // navy
    0xFF1A2415, // dark green
    0xFF1C1B18, // dark charcoal
    0xFF263238, // blue-gray dark
    0xFFFDF6E3  // solarized bg
)

private val TEXT_COLOR_OPTIONS: List<Long> = listOf(
    0xFF000000, // black
    0xFF212121, // dark gray
    0xFF424242, // gray
    0xFF757575, // medium gray
    0xFFBDBDBD, // light gray
    0xFFFFFFFF, // white
    0xFFF5F0E8, // cream
    0xFFEEEEEE, // near white
    0xFFE0DDD5, // warm white
    0xFFB8D4A8, // forest green
    0xFFCDD5E0, // night blue
    0xFF657B83  // solarized text
)

@Composable
private fun ColorPickerRow(
    label: String,
    colorArgb: Long?,
    onColorSelected: (Long) -> Unit,
    colorOptions: List<Long>
) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(4.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(colorOptions) { argb ->
                val isSelected = colorArgb == argb
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color(argb))
                        .then(
                            if (isSelected) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                            else Modifier
                        )
                        .clickable { onColorSelected(argb) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderSettingsSheet(
    settings: NovelReaderSettings,
    onDismiss: () -> Unit,
    onReaderModeToggle: () -> Unit,
    onFontSizeChange: (Float) -> Unit,
    onColorPresetChange: (String) -> Unit,
    onCustomBgColorChange: (Long) -> Unit,
    onCustomTextColorChange: (Long) -> Unit,
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

            // Color theme picker — only in reader mode
            if (settings.readerModeEnabled) {
                // Section label
                Text(
                    text = stringResource(R.string.reader_color_theme_section),
                    style = MaterialTheme.typography.bodyMedium
                )
                // Row of preset swatches
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(ReaderColorPreset.entries) { preset ->
                        val isSelected = settings.readerColorPreset == preset.key
                        // Each swatch: a small column with a colored circle + label
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clickable { onColorPresetChange(preset.key) }
                        ) {
                            // Color circle
                            val (bg, _) = presetSwatchColor(preset)
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(bg)
                                    .then(if (isSelected) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, CircleShape) else Modifier)
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = presetLabel(preset),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                    }
                }

                // Custom color pickers (shown only when CUSTOM preset is selected)
                if (settings.readerColorPreset == ReaderColorPreset.CUSTOM.key) {
                    ColorPickerRow(
                        label = stringResource(R.string.reader_bg_color),
                        colorArgb = settings.customBgColor,
                        onColorSelected = { onCustomBgColorChange(it) },
                        colorOptions = BG_COLOR_OPTIONS
                    )
                    ColorPickerRow(
                        label = stringResource(R.string.reader_text_color),
                        colorArgb = settings.customTextColor,
                        onColorSelected = { onCustomTextColorChange(it) },
                        colorOptions = TEXT_COLOR_OPTIONS
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}
