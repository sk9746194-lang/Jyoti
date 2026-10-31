package com.myra.assistant.data

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Dedicated Gemini API Client for MYRA Assistant.
 * Validates user-configured API keys, executes safe test requests,
 * and handles chat / reasoning inference with proper error classification.
 */
object GeminiApiClient {
    private const val TAG = "MYRA_GeminiApi"

    // Supported models list - modern Google Gemini models verified against API models.list
    val SUPPORTED_MODELS = listOf(
        "gemini-3.8-flash",
        "gemini-3.6-flash",
        "gemini-3.5-flash-lite"
    )

    const val DEFAULT_MODEL = "gemini-3.8-flash"
    const val FALLBACK_CHAT_MODEL = "gemini-3.5-flash-lite"
    const val LIVE_MODEL = "gemini-3.8-live"

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(12, TimeUnit.SECONDS)
        .build()

    private val JSON_MEDIA = "application/json; charset=utf-8".toMediaType()

    /**
     * Sanitizes an API key for safe display (never reveals full secret).
     * Example: "AIzaSyD...k9X1"
     */
    fun maskApiKey(key: String?): String {
        if (key.isNullOrBlank()) return "Not Configured"
        val trimmed = key.trim()
        if (trimmed.length <= 8) return "••••••••"
        return "${trimmed.take(6)}...${trimmed.takeLast(4)}"
    }

    /**
     * Executes a safe real API verification test against the Google Gemini API.
     * Authenticates exclusively via the documented 'x-goog-api-key' header.
     * Returns a pair of (isSuccess, statusMessage).
     */
    suspend fun testConnection(apiKey: String, model: String = DEFAULT_MODEL): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val cleanKey = apiKey.trim()
        if (cleanKey.isBlank()) {
            return@withContext Pair(false, "API Key cannot be empty")
        }

        val targetModel = if (model.isNotBlank()) model.trim() else DEFAULT_MODEL
        // Documented REST generateContent endpoint using header authentication
        val url = "https://generativelanguage.googleapis.com/v1beta/models/$targetModel:generateContent"

        val minimalPayload = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "user")
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", "Say OK in one word")
                        })
                    })
                })
            })
            put("generationConfig", JSONObject().apply {
                put("maxOutputTokens", 5)
                put("temperature", 0.1)
            })
        }

        val request = Request.Builder()
            .url(url)
            .post(minimalPayload.toString().toRequestBody(JSON_MEDIA))
            .header("x-goog-api-key", cleanKey)
            .header("User-Agent", "MYRA-Android-2026")
            .build()

        val startTime = System.currentTimeMillis()

        try {
            val response = httpClient.newCall(request).execute()
            val latency = System.currentTimeMillis() - startTime
            val responseBody = response.body?.string() ?: ""
            val code = response.code

            Log.d(TAG, "Test connection response code: $code (latency: ${latency}ms)")

            if (response.isSuccessful) {
                return@withContext Pair(true, "Connected (${latency}ms) • $targetModel")
            }

            // Parse detailed error message safely without logging or revealing secret
            var errorDesc = "HTTP $code"
            try {
                val errJson = JSONObject(responseBody)
                if (errJson.has("error")) {
                    val errObj = errJson.getJSONObject("error")
                    val rawMsg = errObj.optString("message", "")
                    if (rawMsg.isNotBlank()) {
                        errorDesc = rawMsg.replace(cleanKey, maskApiKey(cleanKey))
                    }
                }
            } catch (_: Exception) {}

            val userFriendlyMessage = when (code) {
                400 -> {
                    if (errorDesc.contains("API_KEY_INVALID", ignoreCase = true) || errorDesc.contains("key not valid", ignoreCase = true)) {
                        "Invalid Key (400): Google Gemini rejected this API key. Please check for copy/paste typos."
                    } else {
                        "Invalid Request (400): $errorDesc"
                    }
                }
                401, 403 -> "Invalid Key or Permission Denied (HTTP $code): $errorDesc. Ensure Gemini API is enabled in your Google AI Studio project."
                404 -> "Unsupported Model (404): Model '$targetModel' not found on Gemini v1beta. Please select 'gemini-3.8-flash' or 'gemini-3.6-flash'."
                429 -> "Quota Exceeded (429): Rate limit or usage quota reached on this API key. Please check Google AI Studio console."
                503 -> "High Demand (503): Primary model '$targetModel' is experiencing high demand. Verified fallback '$FALLBACK_CHAT_MODEL' is ready."
                else -> "Connection Failed (HTTP $code): $errorDesc"
            }

            return@withContext Pair(false, userFriendlyMessage)
        } catch (e: java.net.UnknownHostException) {
            Log.e(TAG, "DNS resolution failure: ${e.message}")
            return@withContext Pair(false, "DNS Resolution Failed: Unable to resolve 'generativelanguage.googleapis.com'. Please check device internet connection or DNS.")
        } catch (e: java.net.SocketTimeoutException) {
            Log.e(TAG, "Socket timeout: ${e.message}")
            return@withContext Pair(false, "Connection Timeout: Google Gemini servers did not respond within 15 seconds. Please check network latency.")
        } catch (e: javax.net.ssl.SSLException) {
            Log.e(TAG, "SSL handshake error: ${e.message}")
            return@withContext Pair(false, "SSL/TLS Error: Secure handshake with Google servers failed (${e.localizedMessage ?: "TLS certificate issue"}).")
        } catch (e: java.net.ConnectException) {
            Log.e(TAG, "Connect error: ${e.message}")
            return@withContext Pair(false, "Network Unreachable: Unable to open TCP socket to Google servers (${e.localizedMessage ?: "Connection refused"}).")
        } catch (e: IOException) {
            Log.e(TAG, "Network connection error while testing Gemini API: ${e.message}")
            return@withContext Pair(false, "Network Error: ${e.localizedMessage ?: "Unable to reach Google servers. Please check network connection."}")
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error testing Gemini API: ${e.message}")
            return@withContext Pair(false, "Connection Error: ${e.message ?: "Unknown error"}")
        }
    }

    /**
     * Queries the Google Gemini 'models.list' endpoint to discover available models for this API key.
     * Filters for models supporting 'generateContent' and excludes legacy/deprecated models.
     */
    suspend fun fetchAvailableModels(apiKey: String): Result<List<String>> = withContext(Dispatchers.IO) {
        val cleanKey = apiKey.trim()
        if (cleanKey.isBlank()) return@withContext Result.failure(IllegalArgumentException("Empty API key"))

        val url = "https://generativelanguage.googleapis.com/v1beta/models"
        val request = Request.Builder()
            .url(url)
            .get()
            .header("x-goog-api-key", cleanKey)
            .header("User-Agent", "MYRA-Android-2026")
            .build()

        try {
            val response = httpClient.newCall(request).execute()
            val body = response.body?.string() ?: ""
            if (!response.isSuccessful) {
                return@withContext Result.failure(IOException("Failed to fetch models: HTTP ${response.code}"))
            }

            val json = JSONObject(body)
            val modelsArray = json.optJSONArray("models") ?: JSONArray()
            val resultList = mutableListOf<String>()

            for (i in 0 until modelsArray.length()) {
                val item = modelsArray.getJSONObject(i)
                val rawName = item.optString("name", "")
                val cleanName = rawName.removePrefix("models/")
                val methods = item.optJSONArray("supportedGenerationMethods")

                var canGenerate = false
                if (methods != null) {
                    for (m in 0 until methods.length()) {
                        if (methods.optString(m) == "generateContent") {
                            canGenerate = true
                            break
                        }
                    }
                }

                // Filter out legacy/deprecated models that fail with 404 or non-text image generation models
                val isDeprecated = cleanName.contains("2.5-pro") || cleanName.contains("2.5-flash") || cleanName.contains("image") || cleanName.contains("tts")
                if (canGenerate && !isDeprecated && cleanName.startsWith("gemini-")) {
                    resultList.add(cleanName)
                }
            }

            // Ensure our verified tier is first
            val orderedList = mutableListOf<String>()
            SUPPORTED_MODELS.forEach { m -> if (resultList.contains(m)) orderedList.add(m) }
            resultList.forEach { m -> if (!orderedList.contains(m)) orderedList.add(m) }

            Result.success(if (orderedList.isNotEmpty()) orderedList else SUPPORTED_MODELS)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to query models.list: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Generates a conversational response using the user's saved Gemini API Key.
     * If the primary model returns HTTP 503 (High Demand / Unavailable) or times out,
     * automatically executes fallback to FALLBACK_CHAT_MODEL ('gemini-3.5-flash-lite').
     */
    suspend fun generateResponse(
        apiKey: String,
        model: String,
        userPrompt: String,
        systemInstruction: String = "You are MYRA, a premier Hindi & Hinglish voice assistant."
    ): Result<String> = withContext(Dispatchers.IO) {
        val cleanKey = apiKey.trim()
        val targetModel = if (model.isNotBlank()) model.trim() else DEFAULT_MODEL

        val (primarySuccess, primaryResult, httpCode) = executeGenerateRequest(cleanKey, targetModel, userPrompt, systemInstruction)

        if (primarySuccess) {
            return@withContext Result.success(primaryResult)
        }

        // Automatic fallback on HTTP 503 (High Demand), 429 (Rate Limit), or timeout/network error (0)
        if ((httpCode == 503 || httpCode == 429 || httpCode == 0) && targetModel != FALLBACK_CHAT_MODEL) {
            Log.w(TAG, "Primary model '$targetModel' failed with code $httpCode ($primaryResult). Executing automatic fallback to '$FALLBACK_CHAT_MODEL'...")
            val (fallbackSuccess, fallbackResult, _) = executeGenerateRequest(cleanKey, FALLBACK_CHAT_MODEL, userPrompt, systemInstruction)
            if (fallbackSuccess) {
                Log.i(TAG, "Fallback to '$FALLBACK_CHAT_MODEL' succeeded.")
                return@withContext Result.success(fallbackResult)
            }
        }

        Result.failure(Exception(primaryResult))
    }

    private fun executeGenerateRequest(
        cleanKey: String,
        targetModel: String,
        userPrompt: String,
        systemInstruction: String
    ): Triple<Boolean, String, Int> {
        val url = "https://generativelanguage.googleapis.com/v1beta/models/$targetModel:generateContent"
        val payload = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "user")
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", userPrompt)
                        })
                    })
                })
            })
            put("systemInstruction", JSONObject().apply {
                put("parts", JSONArray().apply {
                    put(JSONObject().apply {
                        put("text", systemInstruction)
                    })
                })
            })
            put("generationConfig", JSONObject().apply {
                put("temperature", 0.7)
                put("maxOutputTokens", 512)
            })
        }

        val request = Request.Builder()
            .url(url)
            .post(payload.toString().toRequestBody(JSON_MEDIA))
            .header("x-goog-api-key", cleanKey)
            .header("User-Agent", "MYRA-Android-2026")
            .build()

        try {
            val response = httpClient.newCall(request).execute()
            val code = response.code
            val body = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                var errDetail = "HTTP $code"
                try {
                    val errJson = JSONObject(body)
                    if (errJson.has("error")) {
                        val msg = errJson.getJSONObject("error").optString("message", "")
                        if (msg.isNotBlank()) errDetail = msg.replace(cleanKey, maskApiKey(cleanKey))
                    }
                } catch (_: Exception) {}
                return Triple(false, "Gemini error ($errDetail)", code)
            }

            val json = JSONObject(body)
            val candidates = json.optJSONArray("candidates")
            if (candidates != null && candidates.length() > 0) {
                val first = candidates.getJSONObject(0)
                val content = first.optJSONObject("content")
                val parts = content?.optJSONArray("parts")
                if (parts != null && parts.length() > 0) {
                    val text = parts.getJSONObject(0).optString("text", "")
                    return Triple(true, text, code)
                }
            }
            return Triple(false, "Empty candidate response", code)
        } catch (e: Exception) {
            return Triple(false, e.localizedMessage ?: "Network error", 0)
        }
    }

    /**
     * Generates a multi-turn chat response retaining conversation history/memory.
     * Passes previous turns as role="user" and role="model" objects to Gemini API.
     * Supports automatic failover to FALLBACK_CHAT_MODEL on 503/429/timeout.
     */
    suspend fun generateChatConversation(
        apiKey: String,
        model: String,
        conversationHistory: List<Pair<String, String>>,
        userPrompt: String,
        preferredLanguage: String = "auto",
        systemInstruction: String = "You are MYRA, a premier AI voice and chat assistant. Answer questions on science, math, coding, general knowledge, translation, and everyday tasks with high accuracy, clarity, and empathy in natural Hindi, Hinglish, or English based on the user's language. Maintain continuous context from previous turns."
    ): Result<String> = withContext(Dispatchers.IO) {
        val cleanKey = apiKey.trim()
        val targetModel = if (model.isNotBlank()) model.trim() else DEFAULT_MODEL

        val languageDirective = when (preferredLanguage.lowercase()) {
            "hi" -> " Please answer strictly in authentic Hindi (देवनागरी लिपि) with natural conversational warmth."
            "en" -> " Please answer strictly in clear, natural English."
            else -> " Detect the user's input language and respond seamlessly in the same language (Hindi, Hinglish, or English)."
        }
        val fullInstruction = systemInstruction + languageDirective

        val (primarySuccess, primaryResult, httpCode) = executeMultiTurnRequest(
            cleanKey,
            targetModel,
            conversationHistory,
            userPrompt,
            fullInstruction
        )

        if (primarySuccess) {
            return@withContext Result.success(primaryResult)
        }

        // Automatic fallback on HTTP 503 (High Demand), 429 (Rate Limit), or timeout/network error (0)
        if ((httpCode == 503 || httpCode == 429 || httpCode == 0) && targetModel != FALLBACK_CHAT_MODEL) {
            Log.w(TAG, "Primary chat model '$targetModel' failed with code $httpCode ($primaryResult). Executing automatic fallback to '$FALLBACK_CHAT_MODEL'...")
            val (fallbackSuccess, fallbackResult, _) = executeMultiTurnRequest(
                cleanKey,
                FALLBACK_CHAT_MODEL,
                conversationHistory,
                userPrompt,
                fullInstruction
            )
            if (fallbackSuccess) {
                Log.i(TAG, "Fallback to '$FALLBACK_CHAT_MODEL' succeeded.")
                return@withContext Result.success(fallbackResult)
            }
        }

        Result.failure(Exception(primaryResult))
    }

    private fun executeMultiTurnRequest(
        cleanKey: String,
        targetModel: String,
        conversationHistory: List<Pair<String, String>>,
        userPrompt: String,
        systemInstruction: String
    ): Triple<Boolean, String, Int> {
        val url = "https://generativelanguage.googleapis.com/v1beta/models/$targetModel:generateContent"
        val payload = JSONObject().apply {
            val contentsArray = JSONArray()
            // Add previous conversation turns
            for ((role, text) in conversationHistory) {
                if (text.isNotBlank()) {
                    val apiRole = if (role == "user") "user" else "model"
                    contentsArray.put(JSONObject().apply {
                        put("role", apiRole)
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply { put("text", text) })
                        })
                    })
                }
            }
            // Add current turn
            contentsArray.put(JSONObject().apply {
                put("role", "user")
                put("parts", JSONArray().apply {
                    put(JSONObject().apply { put("text", userPrompt) })
                })
            })
            put("contents", contentsArray)

            put("systemInstruction", JSONObject().apply {
                put("parts", JSONArray().apply {
                    put(JSONObject().apply {
                        put("text", systemInstruction)
                    })
                })
            })
            put("generationConfig", JSONObject().apply {
                put("temperature", 0.7)
                put("maxOutputTokens", 1024)
            })
        }

        val request = Request.Builder()
            .url(url)
            .post(payload.toString().toRequestBody(JSON_MEDIA))
            .header("x-goog-api-key", cleanKey)
            .header("User-Agent", "MYRA-Android-2026")
            .build()

        try {
            val response = httpClient.newCall(request).execute()
            val code = response.code
            val body = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                var errDetail = "HTTP $code"
                try {
                    val errJson = JSONObject(body)
                    if (errJson.has("error")) {
                        val msg = errJson.getJSONObject("error").optString("message", "")
                        if (msg.isNotBlank()) errDetail = msg.replace(cleanKey, maskApiKey(cleanKey))
                    }
                } catch (_: Exception) {}
                return Triple(false, "Gemini error ($errDetail)", code)
            }

            val json = JSONObject(body)
            val candidates = json.optJSONArray("candidates")
            if (candidates != null && candidates.length() > 0) {
                val first = candidates.getJSONObject(0)
                val content = first.optJSONObject("content")
                val parts = content?.optJSONArray("parts")
                if (parts != null && parts.length() > 0) {
                    val text = parts.getJSONObject(0).optString("text", "")
                    return Triple(true, text, code)
                }
            }
            return Triple(false, "Empty candidate response", code)
        } catch (e: Exception) {
            return Triple(false, e.localizedMessage ?: "Network error", 0)
        }
    }
}
