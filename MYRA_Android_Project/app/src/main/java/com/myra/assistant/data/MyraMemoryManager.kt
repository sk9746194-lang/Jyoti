package com.myra.assistant.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class MyraMemoryItem(
    val id: String,
    val text: String,
    val createdAt: Long
)

/**
 * Explicit, user-managed personal memory.
 * Facts are only stored after the user taps Save and are encrypted at rest.
 * If encrypted storage cannot be initialized, memory remains unavailable (no plaintext fallback).
 */
class MyraMemoryManager(context: Context) {
    private val appContext = context.applicationContext
    private val lock = Any()

    private val securePrefs: SharedPreferences? by lazy {
        try {
            val masterKey = MasterKey.Builder(appContext)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            EncryptedSharedPreferences.create(
                appContext,
                "myra_personal_memory",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (error: Exception) {
            Log.e("MYRA_Memory", "Encrypted memory storage unavailable.", error)
            null
        }
    }

    val isStorageAvailable: Boolean
        get() = securePrefs != null

    var isEnabled: Boolean
        get() = securePrefs?.getBoolean(KEY_ENABLED, false) ?: false
        set(value) {
            securePrefs?.edit()?.putBoolean(KEY_ENABLED, value)?.apply()
        }

    fun getMemories(): List<MyraMemoryItem> = synchronized(lock) {
        val raw = securePrefs?.getString(KEY_ITEMS, null) ?: return@synchronized emptyList()
        try {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.optJSONObject(index) ?: continue
                    val id = item.optString("id")
                    val text = item.optString("text").trim()
                    if (id.isNotBlank() && text.isNotBlank()) {
                        add(MyraMemoryItem(id, text, item.optLong("createdAt")))
                    }
                }
            }.sortedByDescending { it.createdAt }
        } catch (error: Exception) {
            Log.e("MYRA_Memory", "Could not read saved memories.", error)
            emptyList()
        }
    }

    fun saveMemory(rawText: String): Boolean = synchronized(lock) {
        val text = rawText.trim().replace(Regex("\\s+"), " ").take(MAX_TEXT_LENGTH)
        if (text.length < 3) return@synchronized false
        val prefs = securePrefs ?: return@synchronized false
        val existing = getMemories().toMutableList()
        if (existing.any { it.text.equals(text, ignoreCase = true) }) return@synchronized true
        existing.add(0, MyraMemoryItem(UUID.randomUUID().toString(), text, System.currentTimeMillis()))
        val array = JSONArray()
        existing.take(MAX_ITEMS).forEach { memory ->
            array.put(JSONObject().apply {
                put("id", memory.id)
                put("text", memory.text)
                put("createdAt", memory.createdAt)
            })
        }
        prefs.edit().putString(KEY_ITEMS, array.toString()).commit()
    }

    fun deleteMemory(id: String): Boolean = synchronized(lock) {
        val prefs = securePrefs ?: return@synchronized false
        val remaining = getMemories().filterNot { it.id == id }
        val array = JSONArray()
        remaining.forEach { memory ->
            array.put(JSONObject().apply {
                put("id", memory.id)
                put("text", memory.text)
                put("createdAt", memory.createdAt)
            })
        }
        prefs.edit().putString(KEY_ITEMS, array.toString()).commit()
    }

    fun clearMemories(): Boolean = synchronized(lock) {
        val prefs = securePrefs ?: return@synchronized false
        prefs.edit().remove(KEY_ITEMS).commit()
    }

    fun getContextForPrompt(maxItems: Int = 8): String {
        if (!isEnabled) return ""
        return getMemories().take(maxItems.coerceIn(1, MAX_ITEMS))
            .joinToString("\n") { "- ${it.text}" }
            .take(MAX_CONTEXT_LENGTH)
    }

    companion object {
        private const val KEY_ENABLED = "memory_enabled"
        private const val KEY_ITEMS = "memory_items"
        private const val MAX_ITEMS = 50
        private const val MAX_TEXT_LENGTH = 240
        private const val MAX_CONTEXT_LENGTH = 1800
    }
}
