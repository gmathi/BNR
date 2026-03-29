package com.bnr.app.core.ui.theme

import androidx.compose.ui.graphics.Color

enum class ReaderColorPreset(val key: String) {
    DEFAULT("default"),
    PAPER("paper"),
    DARK("dark"),
    AMOLED("amoled"),
    SOLARIZED("solarized"),
    FOREST("forest"),
    NIGHT_BLUE("night_blue"),
    CUSTOM("custom");

    companion object {
        fun fromKey(key: String) = entries.find { it.key == key } ?: DEFAULT
    }
}

data class ReaderColors(
    val background: Color,
    val text: Color
)

/**
 * Returns the preset ReaderColors for a given preset.
 * [defaultBg] and [defaultText] are used for DEFAULT preset (injected from MaterialTheme).
 */
fun ReaderColorPreset.toColors(
    defaultBg: Color,
    defaultText: Color,
    customBg: Color? = null,
    customText: Color? = null
): ReaderColors = when (this) {
    ReaderColorPreset.DEFAULT    -> ReaderColors(defaultBg, defaultText)
    ReaderColorPreset.PAPER      -> ReaderColors(ReaderBgPaper, ReaderTextPaper)
    ReaderColorPreset.DARK       -> ReaderColors(ReaderBgDarkPreset, ReaderTextDarkPreset)
    ReaderColorPreset.AMOLED     -> ReaderColors(ReaderBgAmoled, ReaderTextAmoled)
    ReaderColorPreset.SOLARIZED  -> ReaderColors(ReaderBgSolarized, ReaderTextSolarized)
    ReaderColorPreset.FOREST     -> ReaderColors(ReaderBgForest, ReaderTextForest)
    ReaderColorPreset.NIGHT_BLUE -> ReaderColors(ReaderBgNightBlue, ReaderTextNightBlue)
    ReaderColorPreset.CUSTOM     -> ReaderColors(
        customBg   ?: ReaderBgPaper,
        customText ?: ReaderTextPaper
    )
}
