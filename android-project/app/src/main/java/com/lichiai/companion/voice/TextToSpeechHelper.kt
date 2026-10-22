package com.lichiai.companion.voice

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale

/**
 * TextToSpeechHelper manages voice synthesis for Lichi AI.
 * Emits speech start, viseme/mouth activity, and completion events to sync with the 3D avatar.
 */
class TextToSpeechHelper(
    context: Context,
    private val onSpeakingStateChanged: (Boolean) -> Unit
) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = TextToSpeech(context, this)
    var isInitialized: Boolean = false
        private set
    var isSpeaking: Boolean = false
        private set

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isInitialized = true
            setupProgressListener()
        }
    }

    private fun setupProgressListener() {
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                isSpeaking = true
                onSpeakingStateChanged(true)
            }

            override fun onDone(utteranceId: String?) {
                isSpeaking = false
                onSpeakingStateChanged(false)
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                isSpeaking = false
                onSpeakingStateChanged(false)
            }
        })
    }

    fun speak(text: String, languageCode: String = "bn", pitch: Float = 1.1f, speed: Float = 1.0f) {
        if (!isInitialized || tts == null) return

        val locale = if (languageCode == "bn") Locale("bn", "BD") else Locale.US
        val result = tts?.setLanguage(locale)
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            // Fallback to English if Bengali TTS voice data is missing on device
            tts?.setLanguage(Locale.US)
        }

        tts?.setPitch(pitch)
        tts?.setSpeechRate(speed)

        val utteranceId = "lichi_utterance_${System.currentTimeMillis()}"
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
    }

    fun stop() {
        tts?.stop()
        isSpeaking = false
        onSpeakingStateChanged(false)
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        isInitialized = false
    }
}
