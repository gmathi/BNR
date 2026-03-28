package com.bnr.app.domain.model

data class TtsVoice(
    val id: String,
    val displayName: String,
    val locale: String,
    val isNeural: Boolean = false,
    val engineId: String = "android"
)
