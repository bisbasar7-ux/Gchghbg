package com.lichiai.companion

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.PhoneInTalk
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.lichiai.companion.actions.ActionResult
import com.lichiai.companion.actions.DeviceActionHandler
import com.lichiai.companion.camera.CameraXManager
import com.lichiai.companion.data.ChatApiRequest
import com.lichiai.companion.data.ChatHistoryItem
import com.lichiai.companion.data.ChatMessage
import com.lichiai.companion.data.DeviceActionRequest
import com.lichiai.companion.data.EmotionState
import com.lichiai.companion.data.MemoryItem
import com.lichiai.companion.data.MessageSender
import com.lichiai.companion.data.VisionApiRequest
import com.lichiai.companion.network.RetrofitClient
import com.lichiai.companion.ui.components.DeviceActionConfirmationDialog
import com.lichiai.companion.ui.screens.CallScreen
import com.lichiai.companion.ui.screens.CameraVisionScreen
import com.lichiai.companion.ui.screens.ChatScreen
import com.lichiai.companion.ui.screens.DeviceActionsScreen
import com.lichiai.companion.ui.screens.MemoryScreen
import com.lichiai.companion.ui.screens.SettingsScreen
import com.lichiai.companion.ui.theme.BorderDark
import com.lichiai.companion.ui.theme.LichiCyan
import com.lichiai.companion.ui.theme.LichiAITheme
import com.lichiai.companion.ui.theme.SurfaceDark
import com.lichiai.companion.voice.CallState
import com.lichiai.companion.voice.SpeechRecognizerHelper
import com.lichiai.companion.voice.TextToSpeechHelper
import com.lichiai.companion.voice.VoiceCallingSessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class AppNavTab(val title: String, val icon: ImageVector) {
    CALL("Call", Icons.Default.PhoneInTalk),
    CHAT("Chat", Icons.Default.Chat),
    VISION("Vision", Icons.Default.CameraAlt),
    ACTIONS("Actions", Icons.Default.Widgets),
    MEMORY("Memory", Icons.Default.Psychology),
    SETTINGS("Settings", Icons.Default.Settings),
    GEMINI("Gemini API", Icons.Default.Key)
}

class MainActivity : ComponentActivity() {

    private lateinit var ttsHelper: TextToSpeechHelper
    private var speechHelper: SpeechRecognizerHelper? = null
    private lateinit var callingManager: VoiceCallingSessionManager
    private lateinit var cameraManager: CameraXManager
    private lateinit var actionHandler: DeviceActionHandler

    // Reactive State
    private val _currentTab = mutableStateOf(AppNavTab.CALL)
    private val _emotion = mutableStateOf(EmotionState.HAPPY)
    private val _callState = mutableStateOf(CallState.DISCONNECTED)
    private val _callDurationSeconds = mutableLongStateOf(0L)
    private val _isMuted = mutableStateOf(false)
    private val _voiceSpeed = mutableStateOf(1.05f)
    private val _voicePitch = mutableStateOf(1.15f)

    // Gemini API Configuration States
    private val _geminiStatus = mutableStateOf(com.lichiai.companion.ui.screens.GeminiConnectionStatus.DISCONNECTED)
    private val _isTestingGemini = mutableStateOf(false)
    private val _geminiSuccessMsg = mutableStateOf<String?>(null)
    private val _geminiErrorMsg = mutableStateOf<String?>(null)
    private val _geminiErrorCode = mutableStateOf<String?>(null)
    private val _canRetryGemini = mutableStateOf(true)
    private val _selectedGeminiModel = mutableStateOf("gemini-3.8-flash")
    private val _geminiLatencyMs = mutableStateOf<Long?>(null)

    private val _isSpeaking = mutableStateOf(false)
    private val _isListening = mutableStateOf(false)
    private val _isProcessing = mutableStateOf(false)
    private val _statusText = mutableStateOf("প্রস্তুত (Ready)")
    private val _chatMessages = mutableStateListOf<ChatMessage>()
    private val _pendingAction = mutableStateOf<DeviceActionRequest?>(null)
    private val _lastActionResult = mutableStateOf<String?>(null)

    // Camera Vision states
    private val _isCameraActive = mutableStateOf(false)
    private val _capturedBitmap = mutableStateOf<Bitmap?>(null)
    private val _capturedBase64 = mutableStateOf<String?>(null)
    private val _isAnalyzingVision = mutableStateOf(false)
    private val _visionResult = mutableStateOf<String?>(null)
    private val _visionObjects = mutableStateListOf<String>()

    private val _memories = mutableStateListOf<MemoryItem>()
    private val _connectionStatus = mutableStateOf<String?>(null)
    private val _hasCameraPermission = mutableStateOf(false)

    // Permission Launchers
    private val callAudioPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            val prefs = LichiApplication.instance.preferencesManager
            callingManager.startCall(prefs.backendBaseUrl, prefs.preferredLanguage)
        } else {
            Toast.makeText(this, "কল করার জন্য মাইক্রোফোন অনুমতি প্রয়োজন।", Toast.LENGTH_LONG).show()
        }
    }

    private val micPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startChatVoiceListening()
        } else {
            Toast.makeText(this, "Microphone permission denied.", Toast.LENGTH_SHORT).show()
        }
    }

    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        _hasCameraPermission.value = isGranted
        if (!isGranted) {
            Toast.makeText(this, "Camera permission denied.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = LichiApplication.instance.preferencesManager
        actionHandler = DeviceActionHandler(this)
        cameraManager = CameraXManager(this, this)
        _hasCameraPermission.value = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

        ttsHelper = TextToSpeechHelper(this) { isSpeaking ->
            _isSpeaking.value = isSpeaking
        }

        // Initialize Real-Time Voice Calling Manager
        callingManager = VoiceCallingSessionManager(
            context = this,
            scope = lifecycleScope,
            ttsHelper = ttsHelper,
            onStateChanged = { state ->
                _callState.value = state
                if (state == CallState.SPEAKING) {
                    _isSpeaking.value = true
                } else if (state == CallState.LISTENING) {
                    _isSpeaking.value = false
                    _isListening.value = true
                }
            },
            onEmotionChanged = { emotion ->
                _emotion.value = emotion
            },
            onDurationChanged = { seconds ->
                _callDurationSeconds.longValue = seconds
            },
            onRmsChanged = {},
            onError = { err ->
                Toast.makeText(this, err, Toast.LENGTH_LONG).show()
            }
        )

        // Initialize Speech Helper for manual chat screen
        speechHelper = SpeechRecognizerHelper(
            context = this,
            onResult = { text ->
                _isListening.value = false
                sendUserChatMessage(text)
            },
            onError = { error ->
                _isListening.value = false
                _statusText.value = error
            },
            onRmsChanged = {},
            onStateChanged = { listening ->
                _isListening.value = listening
                _statusText.value = if (listening) "শুনছি... (Listening)" else "প্রস্তুত (Ready)"
            }
        )

        setContent {
            LichiAITheme {
                MainAppScaffold()
            }
        }
    }

    @Composable
    private fun MainAppScaffold() {
        val prefs = remember { LichiApplication.instance.preferencesManager }
        val isCallActive = _callState.value != CallState.DISCONNECTED

        LaunchedEffect(Unit) {
            _memories.clear()
            _memories.addAll(prefs.getMemories())

            if (_chatMessages.isEmpty()) {
                val welcome = "স্বাগতম! আমি লিচি, আপনার এআই সঙ্গী। সরাসরি ভয়েস কলে কথা বলতে 'কল শুরু করুন' চাপুন।"
                _chatMessages.add(
                    ChatMessage(
                        sender = MessageSender.LICHI,
                        text = welcome,
                        emotion = EmotionState.HAPPY
                    )
                )
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            Scaffold(
                bottomBar = {
                    // Hide navigation bar during active call to keep interface clean
                    AnimatedVisibility(
                        visible = !isCallActive,
                        enter = slideInVertically(initialOffsetY = { it }),
                        exit = slideOutVertically(targetOffsetY = { it })
                    ) {
                        NavigationBar(
                            containerColor = SurfaceDark,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(64.dp)
                                .border(1.dp, BorderDark, RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                        ) {
                            AppNavTab.values().forEach { tab ->
                                val isSelected = _currentTab.value == tab
                                NavigationBarItem(
                                    selected = isSelected,
                                    onClick = { _currentTab.value = tab },
                                    icon = {
                                        Icon(
                                            imageVector = tab.icon,
                                            contentDescription = tab.title,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    },
                                    label = { Text(text = tab.title, fontSize = 10.sp) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = LichiCyan,
                                        selectedTextColor = LichiCyan,
                                        unselectedIconColor = Color(0xFF64748B),
                                        unselectedTextColor = Color(0xFF64748B),
                                        indicatorColor = Color(0x2200E5FF)
                                    )
                                )
                            }
                        }
                    }
                }
            ) { padding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                ) {
                    when (_currentTab.value) {
                        // 1. DEDICATED FULL-SCREEN CALL SCREEN
                        AppNavTab.CALL -> CallScreen(
                            callState = _callState.value,
                            emotion = _emotion.value,
                            callDurationSeconds = _callDurationSeconds.longValue,
                            isMuted = _isMuted.value,
                            onStartCall = { handleStartCall() },
                            onEndCall = { callingManager.endCall() },
                            onToggleMute = { _isMuted.value = callingManager.toggleMute() },
                            onOpenApiSettings = { _currentTab.value = AppNavTab.GEMINI },
                            voiceSpeed = _voiceSpeed.value,
                            voicePitch = _voicePitch.value,
                            onVoiceSpeedChange = { _voiceSpeed.value = it },
                            onVoicePitchChange = { _voicePitch.value = it }
                        )

                        // 2. TEXT CHAT
                        AppNavTab.CHAT -> ChatScreen(
                            messages = _chatMessages,
                            isProcessing = _isProcessing.value,
                            isListening = _isListening.value,
                            isSpeaking = _isSpeaking.value,
                            onSendMessage = { text -> sendUserChatMessage(text) },
                            onMicClick = { toggleChatMic() },
                            onStopSpeaking = { ttsHelper.stop() },
                            onReplayAudio = { text ->
                                ttsHelper.speak(text, prefs.preferredLanguage, _voicePitch.value, _voiceSpeed.value)
                            },
                            onExecuteAction = { action -> executeDeviceAction(action, isUserConfirmed = false) },
                            onClearChat = { _chatMessages.clear() }
                        )

                        // 3. CAMERAX VISION
                        AppNavTab.VISION -> CameraVisionScreen(
                            hasCameraPermission = _hasCameraPermission.value,
                            isCameraActive = _isCameraActive.value,
                            capturedBitmap = _capturedBitmap.value,
                            capturedBase64 = _capturedBase64.value,
                            isAnalyzing = _isAnalyzingVision.value,
                            analysisResult = _visionResult.value,
                            identifiedObjects = _visionObjects,
                            onRequestPermission = {
                                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                            },
                            onStartCamera = { previewView -> startCameraPreview(previewView) },
                            onStopCamera = {
                                cameraManager.stopCamera()
                                _isCameraActive.value = false
                            },
                            onCaptureFrame = { captureCameraSnapshot() },
                            onDiscardCapture = {
                                _capturedBitmap.value = null
                                _capturedBase64.value = null
                                _visionResult.value = null
                                _visionObjects.clear()
                            },
                            onAnalyzeImage = { prompt -> analyzeVisionSnapshot(prompt) },
                            onReplayAudio = { text ->
                                ttsHelper.speak(text, prefs.preferredLanguage, _voicePitch.value, _voiceSpeed.value)
                            }
                        )

                        // 4. DEVICE ACTIONS
                        AppNavTab.ACTIONS -> DeviceActionsScreen(
                            onTriggerAction = { action -> executeDeviceAction(action, isUserConfirmed = false) },
                            lastActionResultMessage = _lastActionResult.value
                        )

                        // 5. USER MEMORY
                        AppNavTab.MEMORY -> MemoryScreen(
                            memories = _memories,
                            isMemoryEnabled = prefs.isMemoryEnabled,
                            onToggleMemoryEnabled = { enabled -> prefs.isMemoryEnabled = enabled },
                            onAddMemory = { fact ->
                                val item = prefs.saveMemory(fact)
                                _memories.add(0, item)
                            },
                            onDeleteMemory = { id ->
                                prefs.deleteMemory(id)
                                _memories.removeAll { it.id == id }
                            },
                            onClearAllMemories = {
                                prefs.clearAllMemories()
                                _memories.clear()
                            }
                        )

                        // 6. SETTINGS
                        AppNavTab.SETTINGS -> SettingsScreen(
                            backendUrl = prefs.backendBaseUrl,
                            preferredLanguage = prefs.preferredLanguage,
                            isTtsEnabled = prefs.isTtsEnabled,
                            ttsPitch = _voicePitch.value,
                            ttsSpeed = _voiceSpeed.value,
                            onSaveBackendUrl = { url ->
                                prefs.backendBaseUrl = url
                                Toast.makeText(this@MainActivity, "URL Saved", Toast.LENGTH_SHORT).show()
                            },
                            onLanguageChanged = { lang -> prefs.preferredLanguage = lang },
                            onToggleTts = { enabled -> prefs.isTtsEnabled = enabled },
                            onPitchChanged = { pitch -> _voicePitch.value = pitch },
                            onSpeedChanged = { speed -> _voiceSpeed.value = speed },
                            onTestConnection = { testBackendHealth() },
                            connectionStatus = _connectionStatus.value
                        )

                        // 7. GEMINI API SETTINGS (Dedicated Screen)
                        AppNavTab.GEMINI -> com.lichiai.companion.ui.screens.GeminiApiSettingsScreen(
                            connectionStatus = _geminiStatus.value,
                            isTesting = _isTestingGemini.value,
                            testSuccessMessage = _geminiSuccessMsg.value,
                            testErrorMessage = _geminiErrorMsg.value,
                            errorCode = _geminiErrorCode.value,
                            latencyMs = _geminiLatencyMs.value,
                            selectedModel = _selectedGeminiModel.value,
                            canRetry = _canRetryGemini.value,
                            onModelSelected = { model ->
                                _selectedGeminiModel.value = model
                                updateBackendModel(model)
                            },
                            onSaveKey = { key, model -> saveGeminiKey(key, model) },
                            onTestConnection = { key, model -> testGeminiConnection(key, model) },
                            onDeleteKey = { deleteGeminiKey() },
                            onStartVoiceCall = {
                                _currentTab.value = AppNavTab.CALL
                                handleStartCall()
                            }
                        )
                    }
                }
            }

            // Sensitive Action Dialog
            _pendingAction.value?.let { action ->
                DeviceActionConfirmationDialog(
                    action = action,
                    onConfirm = {
                        _pendingAction.value = null
                        executeDeviceAction(action, isUserConfirmed = true)
                    },
                    onDismiss = {
                        _pendingAction.value = null
                        _lastActionResult.value = "অ্যাকশন বাতিল করা হয়েছে।"
                    }
                )
            }
        }
    }

    private fun handleStartCall() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            val prefs = LichiApplication.instance.preferencesManager
            callingManager.startCall(prefs.backendBaseUrl, prefs.preferredLanguage)
        } else {
            callAudioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    private fun toggleChatMic() {
        if (_isListening.value) {
            speechHelper?.stopListening()
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                startChatVoiceListening()
            } else {
                micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }

    private fun startChatVoiceListening() {
        ttsHelper.stop()
        val prefs = LichiApplication.instance.preferencesManager
        speechHelper?.startListening(prefs.preferredLanguage)
    }

    private fun sendUserChatMessage(text: String) {
        if (text.isBlank()) return
        val prefs = LichiApplication.instance.preferencesManager
        _chatMessages.add(ChatMessage(sender = MessageSender.USER, text = text))
        _isProcessing.value = true
        _statusText.value = "চিন্তা করছি... (Thinking)"

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val service = RetrofitClient.getService(prefs.backendBaseUrl)
                val history = _chatMessages.takeLast(8).map {
                    ChatHistoryItem(role = if (it.sender == MessageSender.USER) "user" else "assistant", text = it.text)
                }

                val response = service.sendChatMessage(
                    ChatApiRequest(message = text, history = history, language = prefs.preferredLanguage)
                )

                withContext(Dispatchers.Main) {
                    _isProcessing.value = false
                    _statusText.value = "প্রস্তুত (Ready)"

                    if (response.isSuccessful && response.body() != null) {
                        val body = response.body()!!
                        _emotion.value = body.emotion
                        _chatMessages.add(
                            ChatMessage(
                                sender = MessageSender.LICHI,
                                text = body.reply,
                                emotion = body.emotion,
                                actionRequest = body.deviceAction
                            )
                        )
                        if (prefs.isTtsEnabled) {
                            ttsHelper.speak(body.reply, prefs.preferredLanguage, _voicePitch.value, _voiceSpeed.value)
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _isProcessing.value = false
                    _statusText.value = "ত্রুটি ঘটেছে"
                }
            }
        }
    }

    private fun executeDeviceAction(action: DeviceActionRequest, isUserConfirmed: Boolean) {
        val result = actionHandler.executeAction(action, isUserConfirmed)
        when (result) {
            is ActionResult.RequiresConfirmation -> _pendingAction.value = result.action
            is ActionResult.Success -> {
                _lastActionResult.value = result.message
                Toast.makeText(this, result.message, Toast.LENGTH_SHORT).show()
            }
            is ActionResult.Failure -> {
                _lastActionResult.value = "ব্যর্থ: ${result.error}"
                Toast.makeText(this, "Failed: ${result.error}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun startCameraPreview(previewView: PreviewView) {
        cameraManager.startCamera(
            previewView = previewView,
            onStarted = { _isCameraActive.value = true },
            onError = { err -> Toast.makeText(this, err, Toast.LENGTH_SHORT).show() }
        )
    }

    private fun captureCameraSnapshot() {
        cameraManager.captureImage(
            onCaptured = { bitmap, base64 ->
                _capturedBitmap.value = bitmap
                _capturedBase64.value = base64
                cameraManager.stopCamera()
                _isCameraActive.value = false
            },
            onError = { err -> Toast.makeText(this, err, Toast.LENGTH_SHORT).show() }
        )
    }

    private fun analyzeVisionSnapshot(prompt: String) {
        val base64 = _capturedBase64.value ?: return
        val prefs = LichiApplication.instance.preferencesManager
        _isAnalyzingVision.value = true

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val service = RetrofitClient.getService(prefs.backendBaseUrl)
                val response = service.sendVisionImage(
                    VisionApiRequest(imageBase64 = base64, prompt = prompt, language = "bn")
                )
                withContext(Dispatchers.Main) {
                    _isAnalyzingVision.value = false
                    if (response.isSuccessful && response.body() != null) {
                        val body = response.body()!!
                        _visionResult.value = body.description
                        _emotion.value = body.emotion
                        _visionObjects.clear()
                        _visionObjects.addAll(body.identifiedObjects)
                        ttsHelper.speak(body.description, "bn", _voicePitch.value, _voiceSpeed.value)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _isAnalyzingVision.value = false
                    _visionResult.value = "নেটওয়ার্ক সংযোগ ত্রুটি: ${e.localizedMessage}"
                }
            }
        }
    }

    private fun testBackendHealth() {
        val prefs = LichiApplication.instance.preferencesManager
        _connectionStatus.value = "সংযোগ পরীক্ষা করা হচ্ছে..."
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val service = RetrofitClient.getService(prefs.backendBaseUrl)
                val response = service.checkHealth()
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        _connectionStatus.value = "✅ লাইভ এআই সার্ভার সংযুক্ত! (Real-time Calling Ready)"
                    } else {
                        _connectionStatus.value = "❌ সার্ভার ত্রুটি: ${response.code()}"
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _connectionStatus.value = "❌ সংযোগ ব্যর্থ: ${e.localizedMessage}"
                }
            }
        }
    }

    private fun updateBackendModel(model: String) {
        val prefs = LichiApplication.instance.preferencesManager
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val service = RetrofitClient.getService(prefs.backendBaseUrl)
                service.updateModel(com.lichiai.companion.data.GeminiModelSelectionRequest(model = model))
            } catch (_: Exception) {}
        }
    }

    private fun testGeminiConnection(keyToTest: String?, model: String) {
        val prefs = LichiApplication.instance.preferencesManager
        _isTestingGemini.value = true
        _geminiStatus.value = com.lichiai.companion.ui.screens.GeminiConnectionStatus.TESTING
        _geminiSuccessMsg.value = null
        _geminiErrorMsg.value = null
        _geminiErrorCode.value = null
        _canRetryGemini.value = false

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val service = RetrofitClient.getService(prefs.backendBaseUrl)
                val response = service.testGeminiConnection(
                    com.lichiai.companion.data.GeminiTestRequest(apiKey = keyToTest, model = model)
                )

                withContext(Dispatchers.Main) {
                    _isTestingGemini.value = false
                    if (response.isSuccessful && response.body() != null) {
                        val body = response.body()!!
                        if (body.success) {
                            _geminiStatus.value = com.lichiai.companion.ui.screens.GeminiConnectionStatus.CONNECTED
                            _geminiSuccessMsg.value = body.message ?: "Gemini API সফলভাবে যাচাই হয়েছে।"
                            _geminiLatencyMs.value = body.latencyMs
                            _geminiErrorCode.value = null
                            _canRetryGemini.value = false
                            Toast.makeText(this@MainActivity, "Gemini API Connected!", Toast.LENGTH_SHORT).show()
                        } else {
                            _geminiStatus.value = com.lichiai.companion.ui.screens.GeminiConnectionStatus.DISCONNECTED
                            _geminiErrorMsg.value = body.error ?: "যাচাইকরণ ব্যর্থ হয়েছে।"
                            _geminiErrorCode.value = body.errorCode
                            _canRetryGemini.value = body.canRetry
                        }
                    } else {
                        _geminiStatus.value = com.lichiai.companion.ui.screens.GeminiConnectionStatus.DISCONNECTED
                        try {
                            val rawError = response.errorBody()?.string() ?: "{}"
                            val errorJson = org.json.JSONObject(rawError)
                            _geminiErrorMsg.value = errorJson.optString("error", "সার্ভার এরর: ${response.code()}")
                            _geminiErrorCode.value = errorJson.optString("errorCode", if (response.code() == 503) "UNAVAILABLE" else "ERROR")
                            _canRetryGemini.value = errorJson.optBoolean("canRetry", true)
                        } catch (_: Exception) {
                            _geminiErrorMsg.value = if (response.code() == 503) {
                                "মডেলটিতে সাময়িকভাবে উচ্চ চাপ রয়েছে (503 UNAVAILABLE)। অনুগ্রহ করে কিছুক্ষণ পর চেষ্টা করুন।"
                            } else {
                                "সার্ভার এরর: ${response.code()} - API কী যাচাই করা যায়নি।"
                            }
                            _geminiErrorCode.value = if (response.code() == 503) "UNAVAILABLE" else "ERROR"
                            _canRetryGemini.value = true
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _isTestingGemini.value = false
                    _geminiStatus.value = com.lichiai.companion.ui.screens.GeminiConnectionStatus.DISCONNECTED
                    _geminiErrorMsg.value = "নেটওয়ার্ক সংযোগ ত্রুটি: ${e.localizedMessage ?: "সার্ভারে পৌঁছানো যায়নি।"}"
                    _geminiErrorCode.value = "NETWORK_ERROR"
                    _canRetryGemini.value = true
                }
            }
        }
    }

    private fun saveGeminiKey(keyToSave: String, model: String) {
        val prefs = LichiApplication.instance.preferencesManager
        _isTestingGemini.value = true
        _geminiSuccessMsg.value = null
        _geminiErrorMsg.value = null
        _geminiErrorCode.value = null

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val service = RetrofitClient.getService(prefs.backendBaseUrl)
                val response = service.saveGeminiKey(
                    com.lichiai.companion.data.GeminiSaveRequest(apiKey = keyToSave, model = model)
                )

                withContext(Dispatchers.Main) {
                    _isTestingGemini.value = false
                    if (response.isSuccessful && response.body()?.success == true) {
                        _geminiStatus.value = com.lichiai.companion.ui.screens.GeminiConnectionStatus.CONNECTED
                        _geminiSuccessMsg.value = response.body()?.message ?: "কী সংরক্ষিত হয়েছে।"
                        Toast.makeText(this@MainActivity, "API Key Saved", Toast.LENGTH_SHORT).show()
                    } else {
                        try {
                            val rawError = response.errorBody()?.string() ?: "{}"
                            val errorJson = org.json.JSONObject(rawError)
                            _geminiErrorMsg.value = errorJson.optString("error", "সংরক্ষণ ব্যর্থ হয়েছে।")
                            _geminiErrorCode.value = errorJson.optString("errorCode", "ERROR")
                            _canRetryGemini.value = errorJson.optBoolean("canRetry", true)
                        } catch (_: Exception) {
                            _geminiErrorMsg.value = response.body()?.error ?: "সংরক্ষণ ব্যর্থ হয়েছে।"
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _isTestingGemini.value = false
                    _geminiErrorMsg.value = "সংরক্ষণ ব্যর্থ: ${e.localizedMessage}"
                }
            }
        }
    }

    private fun deleteGeminiKey() {
        val prefs = LichiApplication.instance.preferencesManager
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val service = RetrofitClient.getService(prefs.backendBaseUrl)
                service.deleteGeminiKey()
                withContext(Dispatchers.Main) {
                    _geminiSuccessMsg.value = "API কী মুছে ফেলা হয়েছে।"
                    _geminiErrorMsg.value = null
                    _geminiStatus.value = com.lichiai.companion.ui.screens.GeminiConnectionStatus.DISCONNECTED
                    Toast.makeText(this@MainActivity, "Key Deleted", Toast.LENGTH_SHORT).show()
                }
            } catch (_: Exception) {}
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        callingManager.destroy()
        ttsHelper.shutdown()
        speechHelper?.destroy()
        cameraManager.shutdown()
    }
}
