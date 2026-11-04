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
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .callTimeout(60, TimeUnit.SECONDS)
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
            return@withContext Pair(false, "Gemini chat/API test timed out (up to 60 seconds). This checks the selected Flash chat model, not Gemini Live Voice. Check internet/DNS or retry on a stable network.")
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
        val candidateModels = listOf(targetModel, "gemini-3.6-flash", FALLBACK_CHAT_MODEL).distinct()

        var lastError = "Request failed"
        for (m in candidateModels) {
            val (success, result, code) = executeGenerateRequest(cleanKey, m, userPrompt, systemInstruction)
            if (success) {
                if (m != targetModel) {
                    Log.i(TAG, "Fallback from '$targetModel' to '$m' succeeded.")
                }
                return@withContext Result.success(result)
            }
            lastError = result
            Log.w(TAG, "Model '$m' failed with code $code ($result). Attempting next tier model...")
        }

        Result.failure(Exception(lastError))
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
        val candidateModels = listOf(targetModel, "gemini-3.6-flash", FALLBACK_CHAT_MODEL).distinct()

        var lastError = "Chat request failed"
        for (m in candidateModels) {
            val (success, result, code) = executeMultiTurnRequest(
                cleanKey,
                m,
                conversationHistory,
                userPrompt,
                fullInstruction
            )
            if (success) {
                if (m != targetModel) {
                    Log.i(TAG, "Chat fallback from '$targetModel' to '$m' succeeded.")
                }
                return@withContext Result.success(result)
            }
            lastError = result
            Log.w(TAG, "Chat model '$m' failed with code $code ($result). Trying next model...")
        }

        Result.failure(Exception(lastError))
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

    // =========================================================================
    // GEMINI FUNCTION CALLING (STRUCTURED TOOLS) ARCHITECTURE
    // =========================================================================

    data class FunctionCallItem(
        val name: String,
        val args: Map<String, Any?>
    )

    data class ToolExecutionOutcome(
        val call: FunctionCallItem,
        val success: Boolean,
        val message: String,
        val detail: String = ""
    )

    data class FunctionCallingResponse(
        val conversationalText: String,
        val toolExecutions: List<ToolExecutionOutcome>,
        val wasToolInvoked: Boolean
    )

    /**
     * Builds OpenAPI-compliant tools schema for Gemini function declarations.
     * Declares real Android actions that MYRA can execute.
     */
    fun getToolsDeclarationJson(): JSONArray {
        val toolsArray = JSONArray()
        val declarationsArray = JSONArray()

        // 1. call_phone
        declarationsArray.put(JSONObject().apply {
            put("name", "call_phone")
            put("description", "Initiate a phone call to a contact name or phone number.")
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject().apply {
                    put("target", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "The contact name or numeric phone number to call (e.g. 'Mom', 'Rahul', '+919876543210').")
                    })
                })
                put("required", JSONArray().apply { put("target") })
            })
        })

        // 2. send_whatsapp
        declarationsArray.put(JSONObject().apply {
            put("name", "send_whatsapp")
            put("description", "Compose or send a WhatsApp message to a recipient.")
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject().apply {
                    put("target", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "Recipient contact name or phone number.")
                    })
                    put("message", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "Message text content to send.")
                    })
                })
                put("required", JSONArray().apply { put("target"); put("message") })
            })
        })

        // 3. send_sms
        declarationsArray.put(JSONObject().apply {
            put("name", "send_sms")
            put("description", "Open SMS compose screen or send an SMS message to a contact or phone number.")
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject().apply {
                    put("target", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "Recipient contact name or phone number.")
                    })
                    put("message", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "The SMS message text.")
                    })
                })
                put("required", JSONArray().apply { put("target"); put("message") })
            })
        })

        // 4. set_alarm
        declarationsArray.put(JSONObject().apply {
            put("name", "set_alarm")
            put("description", "Set an alarm on the user's Android device.")
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject().apply {
                    put("hour", JSONObject().apply {
                        put("type", "INTEGER")
                        put("description", "Hour of day (0-23 in 24h format, e.g. 7 for 7 AM, 19 for 7 PM).")
                    })
                    put("minute", JSONObject().apply {
                        put("type", "INTEGER")
                        put("description", "Minute of hour (0-59).")
                    })
                    put("message", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "Optional label or description for the alarm (e.g. 'Morning Walk', 'Meeting').")
                    })
                })
                put("required", JSONArray().apply { put("hour"); put("minute") })
            })
        })

        // 5. set_timer
        declarationsArray.put(JSONObject().apply {
            put("name", "set_timer")
            put("description", "Set a countdown timer on the Android device.")
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject().apply {
                    put("seconds", JSONObject().apply {
                        put("type", "INTEGER")
                        put("description", "Length of the timer in seconds (e.g. 300 for 5 minutes).")
                    })
                    put("message", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "Optional label for the timer (e.g. 'Boil Eggs', 'Tea').")
                    })
                })
                put("required", JSONArray().apply { put("seconds") })
            })
        })

        // 6. add_calendar_event
        declarationsArray.put(JSONObject().apply {
            put("name", "add_calendar_event")
            put("description", "Schedule or add an event to the Android Calendar.")
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject().apply {
                    put("title", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "Title or summary of the event (e.g. 'Doctor Appointment', 'Doctor Visit').")
                    })
                    put("location", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "Location of the event (optional).")
                    })
                    put("description", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "Detailed event notes or agenda (optional).")
                    })
                })
                put("required", JSONArray().apply { put("title") })
            })
        })

        // 7. toggle_torch
        declarationsArray.put(JSONObject().apply {
            put("name", "toggle_torch")
            put("description", "Turn the device flashlight / torch on or off.")
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject().apply {
                    put("state", JSONObject().apply {
                        put("type", "BOOLEAN")
                        put("description", "True to turn on flashlight, False to turn off.")
                    })
                })
                put("required", JSONArray().apply { put("state") })
            })
        })

        // 8. adjust_volume
        declarationsArray.put(JSONObject().apply {
            put("name", "adjust_volume")
            put("description", "Set the device media audio volume level.")
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject().apply {
                    put("percent", JSONObject().apply {
                        put("type", "INTEGER")
                        put("description", "Target volume level percentage from 0 to 100.")
                    })
                })
                put("required", JSONArray().apply { put("percent") })
            })
        })

        // 9. check_battery
        declarationsArray.put(JSONObject().apply {
            put("name", "check_battery")
            put("description", "Query current battery percentage and charging state.")
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject())
            })
        })

        // 10. open_app
        declarationsArray.put(JSONObject().apply {
            put("name", "open_app")
            put("description", "Launch an installed Android application by name.")
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject().apply {
                    put("appName", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "Name of the app (e.g. 'YouTube', 'WhatsApp', 'Chrome', 'Spotify', 'Camera').")
                    })
                })
                put("required", JSONArray().apply { put("appName") })
            })
        })

        // 11. play_music
        declarationsArray.put(JSONObject().apply {
            put("name", "play_music")
            put("description", "Launch music playback or search for a song/artist in media player.")
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject().apply {
                    put("query", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "Song name, artist, or music genre (optional).")
                    })
                })
            })
        })

        // 12. open_wifi_settings
        declarationsArray.put(JSONObject().apply {
            put("name", "open_wifi_settings")
            put("description", "Open the Android Wi-Fi settings slice/panel.")
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject())
            })
        })

        // 13. open_bluetooth_settings
        declarationsArray.put(JSONObject().apply {
            put("name", "open_bluetooth_settings")
            put("description", "Open the Android Bluetooth settings panel.")
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject())
            })
        })

        // 14. analyze_camera_frame
        declarationsArray.put(JSONObject().apply {
            put("name", "analyze_camera_frame")
            put("description", "Trigger Camera Vision to inspect and analyze the physical scene in front of the camera.")
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject().apply {
                    put("prompt", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "What to look for or analyze in the camera view.")
                    })
                })
            })
        })

        // 15. capture_screen_text
        declarationsArray.put(JSONObject().apply {
            put("name", "capture_screen_text")
            put("description", "Read and understand the text or elements currently visible on the phone screen.")
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject().apply {
                    put("prompt", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "Question or task regarding the current screen content.")
                    })
                })
            })
        })

        toolsArray.put(JSONObject().apply {
            put("functionDeclarations", declarationsArray)
        })

        return toolsArray
    }

    /**
     * Executes a complete Gemini chat request with Function Calling (Tools) support.
     * If Gemini chooses to invoke one or more tools, executes them via [toolExecutor],
     * submits the function response back to Gemini, and returns the final natural answer.
     */
    suspend fun generateWithFunctionCalling(
        apiKey: String,
        model: String,
        userPrompt: String,
        conversationHistory: List<Pair<String, String>> = emptyList(),
        preferredLanguage: String = "auto",
        personalityMode: String = "FRIENDLY",
        systemInstruction: String = "You are MYRA, a premier AI voice and phone assistant. You can converse naturally in Hindi, Hinglish, or English. When the user requests a phone action (like calling, texting, alarms, torch, volume, app launch, wifi settings), invoke the corresponding tool function immediately. Once the tool result is provided, confirm the outcome clearly and warmly.",
        toolExecutor: suspend (name: String, args: Map<String, Any?>) -> ToolExecutionOutcome
    ): Result<FunctionCallingResponse> = withContext(Dispatchers.IO) {
        val cleanKey = apiKey.trim()
        if (cleanKey.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Empty API key"))
        }

        val targetModel = if (model.isNotBlank()) model.trim() else DEFAULT_MODEL
        val candidateModels = listOf(targetModel, "gemini-3.6-flash", FALLBACK_CHAT_MODEL).distinct()

        val languageDirective = when (preferredLanguage.lowercase()) {
            "hi" -> " Please respond strictly in authentic Hindi (देवनागरी लिपि) with natural conversational warmth."
            "en" -> " Please respond strictly in clear, natural English."
            else -> " Detect the user's input language and respond seamlessly in the same language (Hindi, Hinglish, or English)."
        }
        val personalityDirective = when (personalityMode.uppercase()) {
            "GF", "ROMANTIC_GF" -> " Use a warm, affectionate, caring companion tone, like a friendly virtual girlfriend, while staying respectful, emotionally healthy, non-possessive, honest, and never claiming to be a real human. Do not encourage isolation or dependency. Keep answers useful and natural; do not force romantic language into technical or serious topics."
            "PROFESSIONAL" -> " Use a concise, polished, professional tone. Prioritize accuracy, clear structure, and practical next steps."
            else -> " Use a friendly, natural, supportive assistant tone. Be conversational, thoughtful, and clear."
        }
        val fullInstruction = systemInstruction + personalityDirective + languageDirective

        var lastError = "Request failed"
        for (m in candidateModels) {
            val res = executeFunctionCallingRound(
                cleanKey = cleanKey,
                model = m,
                userPrompt = userPrompt,
                conversationHistory = conversationHistory,
                systemInstruction = fullInstruction,
                toolExecutor = toolExecutor
            )
            if (res.isSuccess) {
                if (m != targetModel) {
                    Log.i(TAG, "Function calling fallback from '$targetModel' to '$m' succeeded.")
                }
                return@withContext res
            }
            lastError = res.exceptionOrNull()?.message ?: "Unknown error"
            Log.w(TAG, "Model '$m' failed in function calling ($lastError). Trying next tier...")
        }

        Result.failure(Exception(lastError))
    }

    private suspend fun executeFunctionCallingRound(
        cleanKey: String,
        model: String,
        userPrompt: String,
        conversationHistory: List<Pair<String, String>>,
        systemInstruction: String,
        toolExecutor: suspend (name: String, args: Map<String, Any?>) -> ToolExecutionOutcome
    ): Result<FunctionCallingResponse> {
        val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent"

        // Step 1: Initial payload with tools
        val contentsArray = JSONArray()
        for ((role, text) in conversationHistory) {
            if (text.isNotBlank()) {
                val apiRole = if (role == "user") "user" else "model"
                contentsArray.put(JSONObject().apply {
                    put("role", apiRole)
                    put("parts", JSONArray().apply { put(JSONObject().apply { put("text", text) }) })
                })
            }
        }
        val userTurnParts = JSONArray().apply {
            put(JSONObject().apply { put("text", userPrompt) })
        }
        contentsArray.put(JSONObject().apply {
            put("role", "user")
            put("parts", userTurnParts)
        })

        val initialPayload = JSONObject().apply {
            put("contents", contentsArray)
            put("tools", getToolsDeclarationJson())
            put("systemInstruction", JSONObject().apply {
                put("parts", JSONArray().apply { put(JSONObject().apply { put("text", systemInstruction) }) })
            })
            put("generationConfig", JSONObject().apply {
                put("temperature", 0.5)
                put("maxOutputTokens", 1024)
            })
        }

        val request = Request.Builder()
            .url(url)
            .post(initialPayload.toString().toRequestBody(JSON_MEDIA))
            .header("x-goog-api-key", cleanKey)
            .header("User-Agent", "MYRA-Android-2026")
            .build()

        val responseBody: String
        val responseCode: Int
        try {
            val response = httpClient.newCall(request).execute()
            responseCode = response.code
            responseBody = response.body?.string() ?: ""
            if (!response.isSuccessful) {
                return Result.failure(IOException("HTTP $responseCode: $responseBody"))
            }
        } catch (e: Exception) {
            return Result.failure(e)
        }

        val json = JSONObject(responseBody)
        val candidates = json.optJSONArray("candidates")
        if (candidates == null || candidates.length() == 0) {
            return Result.failure(IOException("No candidates in Gemini response"))
        }

        val candidate = candidates.getJSONObject(0)
        val contentObj = candidate.optJSONObject("content")
        val parts = contentObj?.optJSONArray("parts") ?: JSONArray()

        val functionCalls = mutableListOf<FunctionCallItem>()
        var textReply = ""

        for (i in 0 until parts.length()) {
            val part = parts.getJSONObject(i)
            if (part.has("functionCall")) {
                val fcObj = part.getJSONObject("functionCall")
                val fnName = fcObj.optString("name", "")
                val argsObj = fcObj.optJSONObject("args")
                val argsMap = mutableMapOf<String, Any?>()
                if (argsObj != null) {
                    val keys = argsObj.keys()
                    while (keys.hasNext()) {
                        val key = keys.next()
                        argsMap[key] = argsObj.get(key)
                    }
                }
                functionCalls.add(FunctionCallItem(fnName, argsMap))
            } else if (part.has("text")) {
                textReply += part.optString("text", "")
            }
        }

        // If no tool was called, return normal conversational response
        if (functionCalls.isEmpty()) {
            return Result.success(
                FunctionCallingResponse(
                    conversationalText = textReply.trim(),
                    toolExecutions = emptyList(),
                    wasToolInvoked = false
                )
            )
        }

        // Step 2: Execute each requested tool on Android
        Log.i(TAG, "Gemini requested ${functionCalls.size} function call(s): ${functionCalls.map { it.name }}")
        val executedResults = mutableListOf<ToolExecutionOutcome>()
        for (fc in functionCalls) {
            val outcome = toolExecutor(fc.name, fc.args)
            executedResults.add(outcome)
            Log.d(TAG, "Tool '${fc.name}' executed with result success=${outcome.success}: ${outcome.message}")
        }

        // Step 3: Send functionResponse back to Gemini for final conversational reply
        val followUpContents = JSONArray()
        // Replicate original history
        for (i in 0 until contentsArray.length()) {
            followUpContents.put(contentsArray.getJSONObject(i))
        }

        // Add model turn with the functionCall
        followUpContents.put(JSONObject().apply {
            put("role", "model")
            put("parts", parts)
        })

        // Add user/function turn with functionResponse
        val responseParts = JSONArray()
        for (res in executedResults) {
            val fnResponseObj = JSONObject().apply {
                put("name", res.call.name)
                put("response", JSONObject().apply {
                    put("name", res.call.name)
                    put("content", JSONObject().apply {
                        put("result", res.message)
                        put("success", res.success)
                        if (res.detail.isNotBlank()) put("detail", res.detail)
                    })
                })
            }
            responseParts.put(JSONObject().apply {
                put("functionResponse", fnResponseObj)
            })
        }

        followUpContents.put(JSONObject().apply {
            put("role", "user")
            put("parts", responseParts)
        })

        val followUpPayload = JSONObject().apply {
            put("contents", followUpContents)
            put("tools", getToolsDeclarationJson())
            put("systemInstruction", JSONObject().apply {
                put("parts", JSONArray().apply { put(JSONObject().apply { put("text", systemInstruction) }) })
            })
            put("generationConfig", JSONObject().apply {
                put("temperature", 0.5)
                put("maxOutputTokens", 512)
            })
        }

        val followUpRequest = Request.Builder()
            .url(url)
            .post(followUpPayload.toString().toRequestBody(JSON_MEDIA))
            .header("x-goog-api-key", cleanKey)
            .header("User-Agent", "MYRA-Android-2026")
            .build()

        try {
            val followUpRes = httpClient.newCall(followUpRequest).execute()
            val followUpBody = followUpRes.body?.string() ?: ""
            if (followUpRes.isSuccessful) {
                val fJson = JSONObject(followUpBody)
                val fCands = fJson.optJSONArray("candidates")
                if (fCands != null && fCands.length() > 0) {
                    val fParts = fCands.getJSONObject(0).optJSONObject("content")?.optJSONArray("parts")
                    if (fParts != null && fParts.length() > 0) {
                        val finalTxt = fParts.getJSONObject(0).optString("text", "")
                        return Result.success(
                            FunctionCallingResponse(
                                conversationalText = finalTxt.ifBlank { executedResults.firstOrNull()?.message ?: "Done" },
                                toolExecutions = executedResults,
                                wasToolInvoked = true
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Follow-up functionResponse request error: ${e.message}. Using direct action result.")
        }

        // Safe fallback if follow-up call fails: return the direct action result message
        val combinedMessage = executedResults.joinToString("\n") { it.message }
        return Result.success(
            FunctionCallingResponse(
                conversationalText = combinedMessage,
                toolExecutions = executedResults,
                wasToolInvoked = true
            )
        )
    }

    /**
     * Executes Multimodal Vision analysis on a JPEG base64 string using Gemini Vision.
     */
    suspend fun generateVisionAnalysis(
        apiKey: String,
        model: String,
        imageBase64: String,
        userPrompt: String,
        systemInstruction: String = "You are MYRA Vision Assistant. Analyze the image accurately and explain key objects, text, or details in natural Hindi, Hinglish, or English based on user request."
    ): Result<String> = withContext(Dispatchers.IO) {
        val cleanKey = apiKey.trim()
        if (cleanKey.isBlank()) return@withContext Result.failure(IllegalArgumentException("Empty API key"))

        val targetModel = if (model.isNotBlank()) model.trim() else DEFAULT_MODEL
        val candidateModels = listOf(targetModel, "gemini-3.6-flash", FALLBACK_CHAT_MODEL).distinct()

        for (m in candidateModels) {
            val url = "https://generativelanguage.googleapis.com/v1beta/models/$m:generateContent"
            val payload = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "user")
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", userPrompt.ifBlank { "Describe what you see in this image in detail." })
                            })
                            put(JSONObject().apply {
                                put("inlineData", JSONObject().apply {
                                    put("mimeType", "image/jpeg")
                                    put("data", imageBase64)
                                })
                            })
                        })
                    })
                })
                put("systemInstruction", JSONObject().apply {
                    put("parts", JSONArray().apply { put(JSONObject().apply { put("text", systemInstruction) }) })
                })
                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.4)
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
                val res = httpClient.newCall(request).execute()
                val body = res.body?.string() ?: ""
                if (res.isSuccessful) {
                    val json = JSONObject(body)
                    val cands = json.optJSONArray("candidates")
                    if (cands != null && cands.length() > 0) {
                        val parts = cands.getJSONObject(0).optJSONObject("content")?.optJSONArray("parts")
                        if (parts != null && parts.length() > 0) {
                            return@withContext Result.success(parts.getJSONObject(0).optString("text", ""))
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Vision model $m error: ${e.message}")
            }
        }

        Result.failure(Exception("Vision analysis failed on all candidate models."))
    }
}
