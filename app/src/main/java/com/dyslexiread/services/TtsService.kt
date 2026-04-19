package com.dyslexiread.services

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.Locale

/**
 * Wrapper du moteur TTS natif Android.
 * - Hors-ligne (moteur embarqué)
 * - Supporte FR, EN, IT
 * - Expose l'état via StateFlow
 */
class TtsService(private val context: Context) : TextToSpeech.OnInitListener {

    enum class State { IDLE, PLAYING, PAUSED, ERROR }

    private var tts: TextToSpeech? = null
    private var initialized = false

    private val _state = MutableStateFlow(State.IDLE)
    val state: StateFlow<State> = _state

    private var onDoneCallback: (() -> Unit)? = null

    init {
        tts = TextToSpeech(context, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            initialized = true
            setLanguage("fr-FR") // langue par défaut
            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    _state.value = State.PLAYING
                }
                override fun onDone(utteranceId: String?) {
                    _state.value = State.IDLE
                    onDoneCallback?.invoke()
                }
                @Deprecated("Deprecated in API 21")
                override fun onError(utteranceId: String?) {
                    _state.value = State.ERROR
                }
                override fun onError(utteranceId: String?, errorCode: Int) {
                    _state.value = State.ERROR
                }
            })
        } else {
            _state.value = State.ERROR
        }
    }

    /**
     * Démarre la lecture du texte.
     * Découpe automatiquement en chunks si > 4000 caractères
     * (limite interne Android TTS).
     */
    fun speak(text: String, languageCode: String, speed: Float, pitch: Float) {
        if (!initialized) return
        setLanguage(languageCode)
        tts?.setSpeechRate(speed)
        tts?.setPitch(pitch)

        val chunks = splitIntoChunks(text, maxLength = 3900)
        chunks.forEachIndexed { i, chunk ->
            val params = Bundle()
            val utteranceId = "chunk_$i"
            if (i == 0) {
                tts?.speak(chunk, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
            } else {
                tts?.speak(chunk, TextToSpeech.QUEUE_ADD, params, utteranceId)
            }
        }
    }

    fun pause() {
        tts?.stop()
        _state.value = State.PAUSED
    }

    fun stop() {
        tts?.stop()
        _state.value = State.IDLE
    }

    fun setLanguage(code: String) {
        if (!initialized) return
        val locale = when (code) {
            "fr-FR" -> Locale.FRENCH
            "en-US" -> Locale.US
            "it-IT" -> Locale.ITALIAN
            else    -> Locale.getDefault()
        }
        val result = tts?.setLanguage(locale)
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            _state.value = State.ERROR
        }
    }

    fun isLanguageAvailable(code: String): Boolean {
        if (!initialized) return false
        val locale = Locale.forLanguageTag(code)
        val result = tts?.isLanguageAvailable(locale) ?: TextToSpeech.LANG_NOT_SUPPORTED
        return result >= TextToSpeech.LANG_AVAILABLE
    }

    fun setOnDoneCallback(callback: () -> Unit) {
        onDoneCallback = callback
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
    }

    /**
     * Android TTS a une limite ~4000 caractères par utterance.
     * On coupe proprement aux fins de phrase.
     */
    private fun splitIntoChunks(text: String, maxLength: Int): List<String> {
        if (text.length <= maxLength) return listOf(text)
        val chunks = mutableListOf<String>()
        var start = 0
        while (start < text.length) {
            var end = (start + maxLength).coerceAtMost(text.length)
            if (end < text.length) {
                // Couper à la fin de phrase la plus proche
                val lastPeriod = text.lastIndexOf('.', end)
                val lastNewline = text.lastIndexOf('\n', end)
                val cutPoint = maxOf(lastPeriod, lastNewline)
                if (cutPoint > start) end = cutPoint + 1
            }
            chunks.add(text.substring(start, end).trim())
            start = end
        }
        return chunks
    }
}
