package com.bnr.app.tts

import com.bnr.app.domain.model.TtsVoice
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages TTS playback lifecycle and paragraph-by-paragraph sequencing.
 *
 * The [currentEngine] is the active engine. New engines can be swapped at runtime.
 */
@Singleton
class TtsManager @Inject constructor(
    private val androidTtsEngine: com.bnr.app.tts.impl.AndroidTtsEngine
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var playbackJob: Job? = null

    val currentEngine: TtsEngine get() = androidTtsEngine

    val isSpeaking: StateFlow<Boolean> get() = currentEngine.isSpeaking
    val currentWordIndex: StateFlow<Int?> get() = currentEngine.currentWordIndex

    /**
     * Speak all [paragraphs] sequentially.
     * Any in-progress speech is cancelled first.
     */
    fun speakAll(paragraphs: List<String>, voiceId: String?) {
        playbackJob?.cancel()
        playbackJob = scope.launch {
            if (!currentEngine.initialize()) return@launch
            for (paragraph in paragraphs) {
                if (paragraph.isBlank()) continue
                currentEngine.speak(paragraph, voiceId)
                // Wait for the paragraph to finish before moving on
                // isSpeaking transitions false→true→false per utterance
            }
        }
    }

    fun stop() {
        playbackJob?.cancel()
        currentEngine.stop()
    }

    fun pause() = currentEngine.pause()

    fun resume() = currentEngine.resume()

    suspend fun getAvailableVoices(): List<TtsVoice> = currentEngine.getAvailableVoices()

    fun initialize() {
        scope.launch { currentEngine.initialize() }
    }
}
