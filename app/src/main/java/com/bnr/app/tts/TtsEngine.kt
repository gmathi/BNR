package com.bnr.app.tts

import com.bnr.app.domain.model.TtsVoice
import kotlinx.coroutines.flow.StateFlow

/**
 * Pluggable TTS engine contract.
 * Implement this to support additional AI/cloud TTS providers.
 */
interface TtsEngine {
    /** Stable unique ID, e.g. "android", "elevenlabs" */
    val id: String

    /** Human-readable name shown in voice picker */
    val name: String

    /** Initialize the engine. Returns true if successful. */
    suspend fun initialize(): Boolean

    /** Speak the given [text] with the optionally specified [voiceId]. */
    suspend fun speak(text: String, voiceId: String?)

    /** Stop any ongoing speech immediately. */
    fun stop()

    /** Pause speech (if supported by the engine). */
    fun pause()

    /** Resume paused speech. */
    fun resume()

    /** Return all available voices for this engine. */
    suspend fun getAvailableVoices(): List<TtsVoice>

    /** True while the engine is actively speaking. */
    val isSpeaking: StateFlow<Boolean>

    /**
     * The index of the word currently being spoken within the current paragraph.
     * Null if not supported or no speech is in progress.
     */
    val currentWordIndex: StateFlow<Int?>
}
