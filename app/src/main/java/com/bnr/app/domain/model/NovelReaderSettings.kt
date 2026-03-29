package com.bnr.app.domain.model

data class NovelReaderSettings(
    val novelId: String,
    val readerModeEnabled: Boolean = true,  // false = raw WebView
    val fontSize: Float = 16f,              // applies only in reader mode
    val ttsVoiceId: String? = null,
    val ttsEngineId: String = "android",
    val readerColorPreset: String = "default",   // key from ReaderColorPreset
    val customBgColor: Long? = null,             // ARGB as Long, only if preset=custom
    val customTextColor: Long? = null            // ARGB as Long, only if preset=custom
)
