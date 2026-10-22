package com.lichiai.companion.data

import com.google.gson.annotations.SerializedName
import java.util.UUID

/**
 * Supported emotion states for 3D Lichi companion
 */
enum class EmotionState {
    @SerializedName("happy") HAPPY,
    @SerializedName("calm") CALM,
    @SerializedName("curious") CURIOUS,
    @SerializedName("surprised") SURPRISED,
    @SerializedName("empathetic") EMPATHETIC
}

/**
 * Supported Android device action types
 */
enum class DeviceActionType {
    @SerializedName("OPEN_APP") OPEN_APP,
    @SerializedName("OPEN_SETTINGS") OPEN_SETTINGS,
    @SerializedName("TOGGLE_FLASHLIGHT") TOGGLE_FLASHLIGHT,
    @SerializedName("SET_VOLUME") SET_VOLUME,
    @SerializedName("SET_ALARM") SET_ALARM,
    @SerializedName("SEARCH_WEB") SEARCH_WEB,
    @SerializedName("UNKNOWN") UNKNOWN
}

/**
 * Structured Device Action request returned by AI Backend
 */
data class DeviceActionRequest(
    @SerializedName("type") val type: DeviceActionType = DeviceActionType.UNKNOWN,
    @SerializedName("target") val target: String = "",
    @SerializedName("label") val label: String = "",
    @SerializedName("explanation") val explanation: String = "",
    @SerializedName("requiresConfirmation") val requiresConfirmation: Boolean = true,
    @SerializedName("parameters") val parameters: Map<String, String> = emptyMap()
)

/**
 * Chat message model for history and UI
 */
data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val sender: MessageSender,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val emotion: EmotionState = EmotionState.CALM,
    val actionRequest: DeviceActionRequest? = null,
    val imageBase64: String? = null
)

enum class MessageSender {
    USER,
    LICHI,
    SYSTEM
}

/**
 * Saved long-term user memory item
 */
data class MemoryItem(
    val id: String = UUID.randomUUID().toString(),
    val fact: String,
    val category: String = "general",
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Backend API request & response payloads
 */
data class SupportedModelItem(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("description") val description: String,
    @SerializedName("recommended") val recommended: Boolean = false
)

data class ChatApiRequest(
    @SerializedName("message") val message: String,
    @SerializedName("history") val history: List<ChatHistoryItem> = emptyList(),
    @SerializedName("memories") val memories: List<String> = emptyList(),
    @SerializedName("language") val language: String = "auto",
    @SerializedName("model") val model: String? = null
)

data class ChatHistoryItem(
    @SerializedName("role") val role: String,
    @SerializedName("text") val text: String
)

data class ChatApiResponse(
    @SerializedName("reply") val reply: String,
    @SerializedName("emotion") val emotion: EmotionState = EmotionState.CALM,
    @SerializedName("deviceAction") val deviceAction: DeviceActionRequest? = null,
    @SerializedName("newMemory") val newMemory: String? = null,
    @SerializedName("usedModel") val usedModel: String? = null
)

data class VisionApiRequest(
    @SerializedName("imageBase64") val imageBase64: String,
    @SerializedName("prompt") val prompt: String = "Describe what you see and highlight important details in Bengali and English.",
    @SerializedName("language") val language: String = "auto",
    @SerializedName("model") val model: String? = null
)

data class VisionApiResponse(
    @SerializedName("description") val description: String,
    @SerializedName("emotion") val emotion: EmotionState = EmotionState.CURIOUS,
    @SerializedName("identifiedObjects") val identifiedObjects: List<String> = emptyList(),
    @SerializedName("usedModel") val usedModel: String? = null
)

data class GeminiConfigResponse(
    @SerializedName("isConfigured") val isConfigured: Boolean,
    @SerializedName("hasKey") val hasKey: Boolean,
    @SerializedName("maskedKey") val maskedKey: String?,
    @SerializedName("model") val model: String?,
    @SerializedName("supportedModels") val supportedModels: List<SupportedModelItem> = emptyList(),
    @SerializedName("supportedFeatures") val supportedFeatures: List<String> = emptyList(),
    @SerializedName("status") val status: String,
    @SerializedName("voiceCallingReady") val voiceCallingReady: Boolean
)

data class GeminiModelSelectionRequest(
    @SerializedName("model") val model: String
)

data class GeminiTestRequest(
    @SerializedName("apiKey") val apiKey: String? = null,
    @SerializedName("model") val model: String? = null
)

data class GeminiTestResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String?,
    @SerializedName("error") val error: String?,
    @SerializedName("errorCode") val errorCode: String? = null,
    @SerializedName("statusCode") val statusCode: Int? = null,
    @SerializedName("latencyMs") val latencyMs: Long? = null,
    @SerializedName("model") val model: String?,
    @SerializedName("canRetry") val canRetry: Boolean = false,
    @SerializedName("features") val features: List<String> = emptyList(),
    @SerializedName("status") val status: String?
)

data class GeminiSaveRequest(
    @SerializedName("apiKey") val apiKey: String,
    @SerializedName("model") val model: String? = null
)

data class GeminiSaveResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String?,
    @SerializedName("error") val error: String?,
    @SerializedName("model") val model: String? = null,
    @SerializedName("status") val status: String?
)

