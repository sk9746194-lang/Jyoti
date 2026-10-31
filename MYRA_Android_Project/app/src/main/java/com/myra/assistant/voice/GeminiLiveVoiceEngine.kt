package com.myra.assistant.voice

import android.util.Base64
import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.*
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Universal Gemini Live Voice Engine for MYRA Assistant.
 * Supports both:
 * 1. Direct official Google Gemini Bidi WebSocket protocol (with setup handshake & realtimeInput chunks)
 * 2. Proxy / Server-side simplified WebSocket protocol (with type: audio / transcript)
 */
class GeminiLiveVoiceEngine(
    private val scope: CoroutineScope,
    private val onTranscript: (String) -> Unit,
    private val onAudioLevel: (Float) -> Unit,
    private val onSpeakingStateChanged: (Boolean) -> Unit,
    private val onStatusChanged: (Boolean, String?) -> Unit
) {
    companion object {
        const val LIVE_MODEL = "models/gemini-3.8-live"
        const val LIVE_VOICE_NAME = "Aoede"
        const val DEFAULT_SYSTEM_INSTRUCTION = "You are MYRA, a natural, helpful Hindi-first personal voice assistant. Speak conversationally and concisely. Never claim a phone action succeeded unless the Android tool confirms it."

        fun buildLiveWebSocketUrl(apiKey: String): String {
            require(apiKey.isNotBlank()) { "Gemini API key is required" }
            return HttpUrl.Builder()
                .scheme("wss")
                .host("generativelanguage.googleapis.com")
                .addPathSegments("ws/google.ai.generativelanguage.v1alpha.GenerativeService.BidiGenerateContent")
                .addQueryParameter("key", apiKey.trim())
                .build()
                .toString()
        }
    }

    private val tag = "MYRA_LiveVoice"
    private var webSocket: WebSocket? = null
    private val player = AudioTrackPlayer(sampleRate = 24000)
    private val gson = Gson()
    private var isGoogleBidiEndpoint = false
    @Volatile private var isSetupComplete = false
    @Volatile private var activeSystemInstruction = DEFAULT_SYSTEM_INSTRUCTION

    fun connect(
        wsUrl: String,
        model: String = LIVE_MODEL,
        systemInstruction: String = DEFAULT_SYSTEM_INSTRUCTION
    ) {
        activeSystemInstruction = systemInstruction.ifBlank { DEFAULT_SYSTEM_INSTRUCTION }
        isGoogleBidiEndpoint = wsUrl.contains("generativelanguage.googleapis.com")
        isSetupComplete = false

        // Strictly decouple Live model from standard chat model - always use models/gemini-3.8-live
        // and never route gemini-2.5-flash or gemini-2.5-pro to the Live API
        val targetLiveModel = if (model.contains("2.5") || model.isBlank()) LIVE_MODEL else model

        val client = OkHttpClient.Builder()
            .readTimeout(0, TimeUnit.MILLISECONDS)
            .connectTimeout(10, TimeUnit.SECONDS)
            .build()

        val request = Request.Builder().url(wsUrl).build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.i(tag, "Gemini Live WebSocket opened (Google Bidi: $isGoogleBidiEndpoint)")
                if (isGoogleBidiEndpoint) {
                    sendGoogleHandshake(webSocket, targetLiveModel, activeSystemInstruction)
                } else {
                    isSetupComplete = true
                    onStatusChanged(true, null)
                }
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                try {
                    // Protocol 1: Official Google Gemini Bidi
                    if (isGoogleBidiEndpoint || text.contains("serverContent") || text.contains("setupComplete")) {
                        parseGoogleBidiMessage(text)
                        return
                    }

                    // Protocol 2: Simplified proxy JSON
                    val json = gson.fromJson(text, JsonObject::class.java)
                    val type = json.get("type")?.asString

                    if (type == "audio") {
                        val base64 = json.get("audio")?.asString ?: return
                        val pcmBytes = Base64.decode(base64, Base64.DEFAULT)
                        onSpeakingStateChanged(true)
                        calculatePcmRms(pcmBytes)
                        scope.launch(Dispatchers.IO) {
                            player.writeChunk(pcmBytes)
                        }
                    } else if (type == "transcript") {
                        val transcript = json.get("text")?.asString ?: ""
                        onTranscript(transcript)
                    } else if (type == "turnComplete") {
                        onSpeakingStateChanged(false)
                        onAudioLevel(0f)
                    }
                } catch (e: Exception) {
                    Log.e(tag, "Error processing live audio message: ${e.message}")
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.w(tag, "Live session error: ${t.message}")
                isSetupComplete = false
                onStatusChanged(false, t.message)
                onSpeakingStateChanged(false)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.i(tag, "Live session closed: code $code, reason: $reason")
                isSetupComplete = false
                onStatusChanged(false, reason)
                onSpeakingStateChanged(false)
            }
        })
    }

    fun reconnect(wsUrl: String) {
        disconnect()
        connect(wsUrl, LIVE_MODEL, activeSystemInstruction)
    }

    private fun sendGoogleHandshake(ws: WebSocket, model: String, systemInstruction: String) {
        val targetModel = if (model.startsWith("models/")) model else "models/$model"
        // Strict guard: ensure gemini-2.5-flash or gemini-2.5-pro is never sent
        val safeModel = if (targetModel.contains("2.5")) LIVE_MODEL else targetModel

        val setupPayload = JSONObject().apply {
            put("setup", JSONObject().apply {
                put("model", safeModel)
                put("systemInstruction", JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply { put("text", systemInstruction) })
                    })
                })
                put("generationConfig", JSONObject().apply {
                    put("responseModalities", JSONArray().apply { put("AUDIO") })
                    put("speechConfig", JSONObject().apply {
                        put("voiceConfig", JSONObject().apply {
                            put("prebuiltVoiceConfig", JSONObject().apply {
                                put("voiceName", LIVE_VOICE_NAME)
                            })
                        })
                    })
                })
            })
        }
        ws.send(setupPayload.toString())
        Log.i(tag, "Dispatched Google Gemini Bidi setup handshake for $safeModel")
    }

    private fun parseGoogleBidiMessage(text: String) {
        val root = JSONObject(text)

        // Handshake response confirmation
        if (root.has("setupComplete")) {
            Log.i(tag, "Gemini Live setup complete confirmed by Google Bidi server")
            isSetupComplete = true
            onStatusChanged(true, null)
            return
        }

        if (root.has("serverContent")) {
            val serverContent = root.getJSONObject("serverContent")

            // Real-time interruption handling (user barge-in)
            if (serverContent.optBoolean("interrupted", false)) {
                Log.d(tag, "User interruption detected - clearing audio buffers")
                player.stop()
                onSpeakingStateChanged(false)
                onAudioLevel(0f)
                return
            }

            if (serverContent.has("modelTurn")) {
                val modelTurn = serverContent.getJSONObject("modelTurn")
                val parts = modelTurn.optJSONArray("parts")
                if (parts != null) {
                    for (i in 0 until parts.length()) {
                        val part = parts.getJSONObject(i)
                        if (part.has("inlineData")) {
                            val inlineData = part.getJSONObject("inlineData")
                            val base64Data = inlineData.getString("data")
                            val rawBytes = Base64.decode(base64Data, Base64.DEFAULT)
                            onSpeakingStateChanged(true)
                            calculatePcmRms(rawBytes)
                            scope.launch(Dispatchers.IO) {
                                player.writeChunk(rawBytes)
                            }
                        }
                        if (part.has("text")) {
                            val chunkText = part.getString("text")
                            onTranscript(chunkText)
                        }
                    }
                }
            }
            if (serverContent.optBoolean("turnComplete", false)) {
                onSpeakingStateChanged(false)
                onAudioLevel(0f)
            }
        }
    }

    private fun calculatePcmRms(pcmBytes: ByteArray) {
        if (pcmBytes.size < 2) return
        var sum = 0.0
        val sampleCount = pcmBytes.size / 2
        for (i in 0 until pcmBytes.size - 1 step 2) {
            val sample = ((pcmBytes[i + 1].toInt() shl 8) or (pcmBytes[i].toInt() and 0xFF)).toShort()
            sum += sample * sample
        }
        val rms = Math.sqrt(sum / sampleCount)
        val normalized = (rms / 32768.0).toFloat().coerceIn(0f, 1f)
        onAudioLevel(normalized)
    }

    fun sendAudioChunk(pcmChunk: ByteArray) {
        if (webSocket == null || (isGoogleBidiEndpoint && !isSetupComplete)) return
        val base64 = Base64.encodeToString(pcmChunk, Base64.NO_WRAP)
        if (isGoogleBidiEndpoint) {
            val payload = JSONObject().apply {
                put("realtimeInput", JSONObject().apply {
                    put("mediaChunks", JSONArray().apply {
                        put(JSONObject().apply {
                            put("mimeType", "audio/pcm;rate=16000")
                            put("data", base64)
                        })
                    })
                })
            }
            webSocket?.send(payload.toString())
        } else {
            val payload = JsonObject().apply {
                addProperty("audio", base64)
            }
            webSocket?.send(payload.toString())
        }
    }

    fun sendTextMessage(text: String) {
        if (webSocket == null || (isGoogleBidiEndpoint && !isSetupComplete)) return
        if (isGoogleBidiEndpoint) {
            val payload = JSONObject().apply {
                put("clientContent", JSONObject().apply {
                    put("turns", JSONArray().apply {
                        put(JSONObject().apply {
                            put("role", "user")
                            put("parts", JSONArray().apply {
                                put(JSONObject().apply {
                                    put("text", text)
                                })
                            })
                        })
                    })
                    put("turnComplete", true)
                })
            }
            webSocket?.send(payload.toString())
        } else {
            val payload = JsonObject().apply {
                addProperty("text", text)
            }
            webSocket?.send(payload.toString())
        }
    }

    fun disconnect() {
        try {
            webSocket?.close(1000, "User exited")
        } catch (_: Exception) {}
        webSocket = null
        isSetupComplete = false
        player.release()
    }
}
