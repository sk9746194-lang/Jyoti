package com.myra.assistant.data

import android.content.Context
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * Message data model for MYRA AI Chat & Voice Conversation System.
 */
data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val sender: String, // "user" or "myra"
    val text: String,
    val time: String = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date()),
    val timestamp: Long = System.currentTimeMillis(),
    val isError: Boolean = false,
    val category: String? = null
)

/**
 * Manages persistent storage, history limits, context window assembly for Gemini,
 * and text sanitization for natural spoken voice output.
 */
object ChatConversationManager {
    private const val TAG = "MYRA_ChatManager"
    private const val FILE_NAME = "myra_chat_history.json"
    private const val MAX_SAVED_MESSAGES = 100
    private const val MAX_CONTEXT_TURNS = 12

    /**
     * Loads previous conversation messages from internal storage.
     */
    fun loadHistory(context: Context): List<ChatMessage> {
        val file = File(context.filesDir, FILE_NAME)
        if (!file.exists()) return emptyList()

        return try {
            val content = file.readText(Charsets.UTF_8)
            val jsonArray = JSONArray(content)
            val list = mutableListOf<ChatMessage>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(
                    ChatMessage(
                        id = obj.optString("id", UUID.randomUUID().toString()),
                        sender = obj.optString("sender", "user"),
                        text = obj.optString("text", ""),
                        time = obj.optString("time", ""),
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                        isError = obj.optBoolean("isError", false),
                        category = if (obj.has("category")) obj.optString("category") else null
                    )
                )
            }
            list
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load chat history: ${e.message}")
            emptyList()
        }
    }

    /**
     * Saves the current conversation history to internal storage.
     */
    fun saveHistory(context: Context, messages: List<ChatMessage>) {
        try {
            val file = File(context.filesDir, FILE_NAME)
            val toSave = if (messages.size > MAX_SAVED_MESSAGES) {
                messages.takeLast(MAX_SAVED_MESSAGES)
            } else {
                messages
            }

            val jsonArray = JSONArray()
            for (msg in toSave) {
                val obj = JSONObject().apply {
                    put("id", msg.id)
                    put("sender", msg.sender)
                    put("text", msg.text)
                    put("time", msg.time)
                    put("timestamp", msg.timestamp)
                    put("isError", msg.isError)
                    if (msg.category != null) put("category", msg.category)
                }
                jsonArray.put(obj)
            }
            file.writeText(jsonArray.toString(), Charsets.UTF_8)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save chat history: ${e.message}")
        }
    }

    /**
     * Clears all persisted chat messages from internal storage.
     */
    fun clearHistory(context: Context) {
        try {
            val file = File(context.filesDir, FILE_NAME)
            if (file.exists()) {
                file.delete()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear chat history: ${e.message}")
        }
    }

    /**
     * Extracts previous valid conversational turns as pairs of ("user" | "model", text)
     * to send to Gemini as multi-turn conversation memory.
     */
    fun buildContextHistory(messages: List<ChatMessage>, maxTurns: Int = MAX_CONTEXT_TURNS): List<Pair<String, String>> {
        val validMessages = messages.filter { !it.isError && it.text.isNotBlank() }
        val recent = if (validMessages.size > maxTurns) validMessages.takeLast(maxTurns) else validMessages

        val history = mutableListOf<Pair<String, String>>()
        for (m in recent) {
            val role = if (m.sender == "user") "user" else "model"
            history.add(Pair(role, m.text))
        }
        return history
    }

    /**
     * Cleans up AI text for spoken voice synthesis: strips markdown code blocks,
     * header hashes, bold asterisks, bullet points, links, and excessive formatting.
     */
    fun sanitizeForSpeech(raw: String): String {
        var clean = raw
        // Remove code blocks
        clean = clean.replace(Regex("```[\\s\\S]*?```"), " कोड ब्लॉक ")
        clean = clean.replace(Regex("`([^`]+)`"), "$1")
        // Remove URLs
        clean = clean.replace(Regex("https?://\\S+"), "वेब लिंक")
        // Remove Markdown headers (#, ##, etc)
        clean = clean.replace(Regex("(?m)^#+\\s*"), "")
        // Remove markdown bold / italic (*, **, _)
        clean = clean.replace(Regex("[*_~]"), "")
        // Remove bullet markers at line start
        clean = clean.replace(Regex("(?m)^[-*•]\\s+"), "")
        clean = clean.replace(Regex("(?m)^\\d+\\.\\s+"), "")
        // Clean multiple newlines
        clean = clean.replace(Regex("\\n{2,}"), "\n")
        return clean.trim()
    }
}
