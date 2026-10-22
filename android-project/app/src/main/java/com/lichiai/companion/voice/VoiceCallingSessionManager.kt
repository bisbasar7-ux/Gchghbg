package com.lichiai.companion.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import com.lichiai.companion.data.EmotionState
import com.lichiai.companion.network.RetrofitClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

enum class CallState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    LISTENING,
    THINKING,
    SPEAKING
}

data class CallHistoryTurn(
    val role: String,
    val text: String
)

/**
 * VoiceCallingSessionManager handles hands-free continuous real-time voice calls.
 * Implements automatic turn-taking, Voice Activity Detection, barge-in / interruption handling,
 * call duration tracking, and resilient reconnection.
 */
class VoiceCallingSessionManager(
    private val context: Context,
    private val scope: CoroutineScope,
    private val ttsHelper: TextToSpeechHelper,
    private val onStateChanged: (CallState) -> Unit,
    private val onEmotionChanged: (EmotionState) -> Unit,
    private val onDurationChanged: (Long) -> Unit,
    private val onRmsChanged: (Float) -> Unit,
    private val onError: (String) -> Unit
) {

    private var speechRecognizer: SpeechRecognizer? = null
    private var callTimerJob: Job? = null
    private var activeCallJob: Job? = null

    var currentCallState: CallState = CallState.DISCONNECTED
        private set

    var callDurationSeconds: Long = 0L
        private set

    var isMuted: Boolean = false
        private set

    private var isCallActive: Boolean = false
    private val conversationHistory = mutableListOf<CallHistoryTurn>()
    private var reconnectAttempts = 0
    private val MAX_RECONNECT_ATTEMPTS = 3

    init {
        initSpeechRecognizer()
    }

    private fun initSpeechRecognizer() {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        if (isCallActive && currentCallState != CallState.SPEAKING && currentCallState != CallState.THINKING) {
                            updateState(CallState.LISTENING)
                        }
                    }

                    override fun onBeginningOfSpeech() {
                        // User started speaking!
                        // Barge-in detection: If Lichi is speaking, immediately interrupt Lichi
                        if (currentCallState == CallState.SPEAKING) {
                            ttsHelper.stop()
                        }
                        updateState(CallState.LISTENING)
                    }

                    override fun onRmsChanged(rmsdB: Float) {
                        if (!isMuted) {
                            onRmsChanged(rmsdB)
                        }
                    }

                    override fun onBufferReceived(buffer: ByteArray?) {}

                    override fun onEndOfSpeech() {
                        // User finished speaking, processing turn automatically
                        if (isCallActive) {
                            updateState(CallState.THINKING)
                        }
                    }

                    override fun onError(error: Int) {
                        if (!isCallActive) return

                        when (error) {
                            SpeechRecognizer.ERROR_NO_MATCH,
                            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> {
                                // Silent pause by user, immediately resume continuous listening
                                restartListeningIfActive()
                            }
                            SpeechRecognizer.ERROR_NETWORK -> {
                                handleTemporaryNetworkDrop()
                            }
                            else -> {
                                restartListeningIfActive()
                            }
                        }
                    }

                    override fun onResults(results: Bundle?) {
                        if (!isCallActive) return
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val spokenText = matches?.firstOrNull()?.trim()

                        if (!spokenText.isNullOrBlank()) {
                            processUserTurn(spokenText)
                        } else {
                            restartListeningIfActive()
                        }
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
                        val partialMatches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val partial = partialMatches?.firstOrNull()
                        if (!partial.isNullOrBlank() && currentCallState == CallState.SPEAKING) {
                            // Instant barge-in interruption!
                            ttsHelper.stop()
                            updateState(CallState.LISTENING)
                        }
                    }

                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
            }
        }
    }

    fun startCall(baseUrl: String, preferredLanguage: String = "bn") {
        if (isCallActive) return
        isCallActive = true
        callDurationSeconds = 0L
        reconnectAttempts = 0
        conversationHistory.clear()
        updateState(CallState.CONNECTING)

        // Start Call Duration Timer
        startCallTimer()

        // Greet user to open call session
        scope.launch(Dispatchers.Main) {
            delay(400)
            updateState(CallState.CONNECTED)
            delay(300)

            val greetings = listOf(
                "হ্যালো! আমি শুনছি, কেমন আছো তুমি?",
                "শুনছি বলো, তোমার দিনটা কেমন কাটল?",
                "হ্যালো! তোমার সাথে কথা বলতে পেরে খুব ভালো লাগছে।"
            )
            val openingGreeting = greetings.random()
            onEmotionChanged(EmotionState.HAPPY)
            speakLichiTurn(openingGreeting, preferredLanguage)
        }
    }

    private fun processUserTurn(userText: String) {
        if (!isCallActive || isMuted) return
        updateState(CallState.THINKING)

        conversationHistory.add(CallHistoryTurn(role = "user", text = userText))

        activeCallJob = scope.launch(Dispatchers.IO) {
            try {
                val service = RetrofitClient.getService(RetrofitClient.currentBaseUrl.ifEmpty { "http://10.0.2.2:3000/" })
                val history = conversationHistory.takeLast(6).map {
                    com.lichiai.companion.data.ChatHistoryItem(role = it.role, text = it.text)
                }

                val response = service.sendChatMessage(
                    com.lichiai.companion.data.ChatApiRequest(
                        message = userText,
                        history = history,
                        language = "bn"
                    )
                )

                withContext(Dispatchers.Main) {
                    if (!isCallActive) return@withContext

                    if (response.isSuccessful && response.body() != null) {
                        val body = response.body()!!
                        onEmotionChanged(body.emotion)
                        conversationHistory.add(CallHistoryTurn(role = "assistant", text = body.reply))
                        speakLichiTurn(body.reply, "bn")
                    } else {
                        val fallback = "আমি একটু স্পষ্ট শুনতে পাইনি, তুমি কি আরেকবার বলবে?"
                        speakLichiTurn(fallback, "bn")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    if (!isCallActive) return@withContext
                    handleTemporaryNetworkDrop()
                }
            }
        }
    }

    private fun speakLichiTurn(text: String, languageCode: String) {
        if (!isCallActive) return
        updateState(CallState.SPEAKING)

        // Set TTS callback to automatically resume listening when Lichi finishes speaking
        ttsHelper.speak(text, languageCode, 1.15f, 1.05f)

        // Monitor speech completion to resume listening hands-free
        scope.launch(Dispatchers.Main) {
            // Estimate duration or wait for isSpeaking flag
            while (ttsHelper.isSpeaking && isCallActive) {
                delay(100)
            }
            if (isCallActive && currentCallState == CallState.SPEAKING) {
                delay(250) // Small natural conversational pause
                startContinuousListening()
            }
        }
    }

    private fun startContinuousListening() {
        if (!isCallActive || isMuted) return

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "bn-BD")
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "bn-BD")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }

        try {
            speechRecognizer?.startListening(intent)
            updateState(CallState.LISTENING)
        } catch (_: Exception) {
            restartListeningIfActive()
        }
    }

    private fun restartListeningIfActive() {
        if (!isCallActive || isMuted) return
        scope.launch(Dispatchers.Main) {
            delay(200)
            if (isCallActive && currentCallState != CallState.SPEAKING && currentCallState != CallState.THINKING) {
                startContinuousListening()
            }
        }
    }

    fun toggleMute(): Boolean {
        isMuted = !isMuted
        if (isMuted) {
            speechRecognizer?.stopListening()
            updateState(CallState.CONNECTED)
        } else {
            startContinuousListening()
        }
        return isMuted
    }

    private fun handleTemporaryNetworkDrop() {
        if (reconnectAttempts < MAX_RECONNECT_ATTEMPTS) {
            reconnectAttempts++
            updateState(CallState.CONNECTING)
            scope.launch(Dispatchers.Main) {
                delay(1500)
                if (isCallActive) {
                    restartListeningIfActive()
                }
            }
        } else {
            onError("নেটওয়ার্ক সংযোগ বিঘ্নিত হয়েছে। কলটি পুনরায় শুরু করুন।")
            endCall()
        }
    }

    private fun startCallTimer() {
        callTimerJob?.cancel()
        callTimerJob = scope.launch(Dispatchers.Main) {
            while (isActive && isCallActive) {
                delay(1000)
                callDurationSeconds++
                onDurationChanged(callDurationSeconds)
            }
        }
    }

    fun endCall() {
        isCallActive = false
        callTimerJob?.cancel()
        callTimerJob = null
        activeCallJob?.cancel()
        activeCallJob = null

        try {
            speechRecognizer?.stopListening()
        } catch (_: Exception) {}

        ttsHelper.stop()
        updateState(CallState.DISCONNECTED)
        onDurationChanged(0L)
    }

    private fun updateState(state: CallState) {
        currentCallState = state
        onStateChanged(state)
    }

    fun destroy() {
        endCall()
        try {
            speechRecognizer?.destroy()
        } catch (_: Exception) {}
        speechRecognizer = null
    }
}
