package com.bnr.app.tts.impl

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.bnr.app.domain.model.TtsVoice
import com.bnr.app.tts.TtsEngine
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.Locale
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class AndroidTtsEngine @Inject constructor(
    @ApplicationContext private val context: Context
) : TtsEngine {

    override val id = "android"
    override val name = "Android TTS"

    private var tts: TextToSpeech? = null
    private var isInitialized = false

    private val _isSpeaking = MutableStateFlow(false)
    override val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _currentWordIndex = MutableStateFlow<Int?>(null)
    override val currentWordIndex: StateFlow<Int?> = _currentWordIndex.asStateFlow()

    // Maps utterance ID → coroutine continuation so speak() can suspend until done
    private val pendingContinuations = ConcurrentHashMap<String, CancellableContinuation<Unit>>()

    override suspend fun initialize(): Boolean = suspendCancellableCoroutine { continuation ->
        if (isInitialized && tts != null) {
            continuation.resume(true)
            return@suspendCancellableCoroutine
        }
        tts = TextToSpeech(context) { status ->
            isInitialized = (status == TextToSpeech.SUCCESS)
            if (isInitialized) {
                tts?.language = Locale.getDefault()
                setupProgressListener()
            }
            if (continuation.isActive) continuation.resume(isInitialized)
        }
        continuation.invokeOnCancellation { tts?.shutdown() }
    }

    private fun setupProgressListener() {
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                _isSpeaking.value = true
            }

            override fun onDone(utteranceId: String?) {
                _isSpeaking.value = false
                _currentWordIndex.value = null
                utteranceId?.let { id ->
                    pendingContinuations.remove(id)?.let { cont ->
                        if (cont.isActive) cont.resume(Unit)
                    }
                }
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                _isSpeaking.value = false
                _currentWordIndex.value = null
                utteranceId?.let { id ->
                    pendingContinuations.remove(id)?.let { cont ->
                        if (cont.isActive) cont.resume(Unit)
                    }
                }
            }

            override fun onRangeStart(utteranceId: String?, start: Int, end: Int, frame: Int) {
                // Approximate word index via character offset — not perfect but functional
                _currentWordIndex.value = start
            }
        })
    }

    override suspend fun speak(text: String, voiceId: String?) = suspendCancellableCoroutine<Unit> { continuation ->
        if (!isInitialized || tts == null) {
            continuation.resume(Unit)
            return@suspendCancellableCoroutine
        }
        val engine = tts!!

        voiceId?.let { id ->
            engine.voices?.find { it.name == id }?.let { engine.voice = it }
        }

        val utteranceId = UUID.randomUUID().toString()
        pendingContinuations[utteranceId] = continuation

        // QUEUE_ADD so TtsManager controls sequencing; stop() clears the queue when cancelling
        engine.speak(text, TextToSpeech.QUEUE_ADD, null, utteranceId)

        continuation.invokeOnCancellation {
            pendingContinuations.remove(utteranceId)
            engine.stop()
            _isSpeaking.value = false
        }
    }

    override fun stop() {
        tts?.stop()
        _isSpeaking.value = false
        _currentWordIndex.value = null
    }

    override fun pause() {
        // Android TTS doesn't natively support pause; stop is the closest equivalent
        tts?.stop()
        _isSpeaking.value = false
    }

    override fun resume() {
        // Resume is not supported by Android TTS natively — no-op
    }

    override suspend fun getAvailableVoices(): List<TtsVoice> {
        if (!isInitialized) initialize()
        return tts?.voices
            ?.filter { !it.isNetworkConnectionRequired || it.features?.contains("network") == true }
            ?.map { voice ->
                TtsVoice(
                    id = voice.name,
                    displayName = buildDisplayName(voice.name, voice.locale),
                    locale = voice.locale.toLanguageTag(),
                    engineId = id,
                    isNeural = voice.quality >= android.speech.tts.Voice.QUALITY_VERY_HIGH
                )
            }
            ?.sortedWith(compareBy({ it.locale }, { it.displayName }))
            ?: emptyList()
    }

    private fun buildDisplayName(name: String, locale: Locale): String {
        val lang = locale.getDisplayLanguage(Locale.getDefault())
        val country = locale.getDisplayCountry(Locale.getDefault())
        val suffix = if (country.isNotEmpty()) "$lang ($country)" else lang
        // Strip engine-specific prefix noise from the raw voice name
        val clean = name.substringAfterLast("#").replace("-", " ").trimEnd()
        return "$clean — $suffix"
    }

    fun shutdown() {
        tts?.shutdown()
        tts = null
        isInitialized = false
    }
}
