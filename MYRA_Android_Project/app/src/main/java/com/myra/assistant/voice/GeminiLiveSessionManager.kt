package com.myra.assistant.voice

import android.util.Base64
import android.util.Log
import okhttp3.*
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Gemini Live Duplex Audio Session Manager.
 * Handles framing, 24kHz PCM stream upload, and parsed server turns.
 */
class GeminiLiveSessionManager(
    private val apiKey: String,
    private val onAudioChunkReceived: (ByteArray) -> Unit,
    private val onTranscriptReceived: (String) -> Unit
) {
    private val client = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .build()

    private var webSocket: WebSocket? = null
    var isConnected = false
        private set

    fun connect() {
        val url = "wss://generativelanguage.googleapis.com/ws/google.ai.generativelanguage.v1alpha.GenerativeService.BidiGenerateContent?key=$apiKey"
        val request = Request.Builder().url(url).build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(ws: WebSocket, response: Response) {
                isConnected = true
                Log.d("MYRA_LIVE", "Connected to Gemini Live Bidi WebSocket")
                sendInitialHandshake(ws)
            }

            override fun onMessage(ws: WebSocket, text: String) {
                try {
                    val root = JSONObject(text)
                    if (root.has("serverContent")) {
                        val serverContent = root.getJSONObject("serverContent")
                        if (serverContent.has("modelTurn")) {
                            val parts = serverContent.getJSONObject("modelTurn").getJSONArray("parts")
                            for (i in 0 until parts.length()) {
                                val part = parts.getJSONObject(i)
                                if (part.has("inlineData")) {
                                    val inlineData = part.getJSONObject("inlineData")
                                    val base64Data = inlineData.getString("data")
                                    val rawBytes = Base64.decode(base64Data, Base64.DEFAULT)
                                    onAudioChunkReceived(rawBytes)
                                }
                                if (part.has("text")) {
                                    onTranscriptReceived(part.getString("text"))
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e("MYRA_LIVE", "Error parsing server turn", e)
                }
            }

            override fun onClosed(ws: WebSocket, code: Int, reason: String) {
                isConnected = false
                Log.d("MYRA_LIVE", "Session closed: $reason")
            }

            override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
                isConnected = false
                Log.e("MYRA_LIVE", "Session connection error", t)
            }
        })
    }

    private fun sendInitialHandshake(ws: WebSocket) {
        val setupPayload = JSONObject().apply {
            put("setup", JSONObject().apply {
                put("model", "models/gemini-2.0-flash-exp")
                put("generationConfig", JSONObject().apply {
                    put("responseModalities", org.json.JSONArray().apply { put("AUDIO") })
                    put("speechConfig", JSONObject().apply {
                        put("voiceConfig", JSONObject().apply {
                            put("prebuiltVoiceConfig", JSONObject().apply {
                                put("voiceName", "Kore")
                            })
                        })
                    })
                })
            })
        }
        ws.send(setupPayload.toString())
    }

    fun sendAudioChunk(pcmChunk: ByteArray) {
        if (!isConnected || webSocket == null) return
        val base64 = Base64.encodeToString(pcmChunk, Base64.NO_WRAP)
        val payload = JSONObject().apply {
            put("realtimeInput", JSONObject().apply {
                put("mediaChunks", org.json.JSONArray().apply {
                    put(JSONObject().apply {
                        put("mimeType", "audio/pcm;rate=16000")
                        put("data", base64)
                    })
                })
            })
        }
        webSocket?.send(payload.toString())
    }

    fun disconnect() {
        webSocket?.close(1000, "User disconnected")
        webSocket = null
        isConnected = false
    }
}
