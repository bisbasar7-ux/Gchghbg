package com.lichiai.companion.data

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Handles persistent preferences and user-approved memory.
 * Provides controls for users to review, add, and clear saved memories.
 */
class PreferencesManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()

    var backendBaseUrl: String
        get() = prefs.getString(KEY_BACKEND_URL, "http://10.0.2.2:3000/") ?: "http://10.0.2.2:3000/"
        set(value) = prefs.edit().putString(KEY_BACKEND_URL, value).apply()

    var preferredLanguage: String
        get() = prefs.getString(KEY_LANGUAGE, "bn") ?: "bn" // "bn" (Bengali) or "en" (English)
        set(value) = prefs.edit().putString(KEY_LANGUAGE, value).apply()

    var isTtsEnabled: Boolean
        get() = prefs.getBoolean(KEY_TTS_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_TTS_ENABLED, value).apply()

    var ttsPitch: Float
        get() = prefs.getFloat(KEY_TTS_PITCH, 1.1f)
        set(value) = prefs.edit().putFloat(KEY_TTS_PITCH, value).apply()

    var ttsSpeed: Float
        get() = prefs.getFloat(KEY_TTS_SPEED, 1.0f)
        set(value) = prefs.edit().putFloat(KEY_TTS_SPEED, value).apply()

    var isMemoryEnabled: Boolean
        get() = prefs.getBoolean(KEY_MEMORY_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_MEMORY_ENABLED, value).apply()

    fun getMemories(): List<MemoryItem> {
        val json = prefs.getString(KEY_MEMORIES, null) ?: return emptyList()
        val type = object : TypeToken<List<MemoryItem>>() {}.type
        return try {
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveMemory(fact: String, category: String = "general"): MemoryItem {
        val current = getMemories().toMutableList()
        val item = MemoryItem(fact = fact, category = category)
        current.add(0, item)
        prefs.edit().putString(KEY_MEMORIES, gson.toJson(current)).apply()
        return item
    }

    fun deleteMemory(id: String) {
        val current = getMemories().toMutableList()
        current.removeAll { it.id == id }
        prefs.edit().putString(KEY_MEMORIES, gson.toJson(current)).apply()
    }

    fun clearAllMemories() {
        prefs.edit().remove(KEY_MEMORIES).apply()
    }

    companion object {
        private const val PREFS_NAME = "lichi_ai_preferences"
        private const val KEY_BACKEND_URL = "backend_url"
        private const val KEY_LANGUAGE = "preferred_language"
        private const val KEY_TTS_ENABLED = "tts_enabled"
        private const val KEY_TTS_PITCH = "tts_pitch"
        private const val KEY_TTS_SPEED = "tts_speed"
        private const val KEY_MEMORY_ENABLED = "memory_enabled"
        private const val KEY_MEMORIES = "saved_memories"
    }
}
