package com.lichiai.companion.network

import com.lichiai.companion.data.ChatApiRequest
import com.lichiai.companion.data.ChatApiResponse
import com.lichiai.companion.data.VisionApiRequest
import com.lichiai.companion.data.VisionApiResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

/**
 * Retrofit interface connecting to the secure Lichi AI Backend.
 * All Gemini API keys are maintained exclusively on the server side.
 */
interface LichiBackendService {

    @POST("api/lichi/chat")
    suspend fun sendChatMessage(
        @Body request: ChatApiRequest
    ): Response<ChatApiResponse>

    @POST("api/lichi/vision")
    suspend fun sendVisionImage(
        @Body request: VisionApiRequest
    ): Response<VisionApiResponse>

    @GET("api/health")
    suspend fun checkHealth(): Response<Map<String, String>>

    @GET("api/config/gemini")
    suspend fun getGeminiConfig(): Response<com.lichiai.companion.data.GeminiConfigResponse>

    @POST("api/config/gemini/test")
    suspend fun testGeminiConnection(
        @Body request: com.lichiai.companion.data.GeminiTestRequest
    ): Response<com.lichiai.companion.data.GeminiTestResponse>

    @POST("api/config/gemini/save")
    suspend fun saveGeminiKey(
        @Body request: com.lichiai.companion.data.GeminiSaveRequest
    ): Response<com.lichiai.companion.data.GeminiSaveResponse>

    @POST("api/config/gemini/model")
    suspend fun updateModel(
        @Body request: com.lichiai.companion.data.GeminiModelSelectionRequest
    ): Response<Map<String, Any>>

    @POST("api/config/gemini/delete")
    suspend fun deleteGeminiKey(): Response<Map<String, Any>>
}
