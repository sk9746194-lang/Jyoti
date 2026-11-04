package com.myra.assistant.voice

import android.util.Base64
import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.channels.Channel
import okhttp3.*
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

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
        const val LIVE_WS_ENDPOINT_TEMPLATE = "wss://generativelanguage.googleapis.com/ws/google.ai.generativelanguage.v1alpha.GenerativeService.BidiGenerateContent?key=%s"
    }

    private val tag = "MYRA_LiveVoice"
    private var webSocket: WebSocket? = null
    private val player = AudioTrackPlayer(sampleRate = 24000)
    private val gson = Gson()
    private var isGoogleBidiEndpoint = false
    @Volatile private var isSessionReady = false
    private val connectionGeneration = AtomicLong(0L)

    // One FIFO consumer keeps streamed PCM chunks in server order. Launching a coroutine
    // for every packet can interleave writes and make otherwise-valid Live audio sound choppy.
    private val audioQueue = Channel<ByteArray>(Channel.UNLIMITED)

    init {
        scope.launch(Dispatchers.IO) {
            for (chunk in audioQueue) {
                player.writeChunk(chunk)
            }
        }
    }

    fun connect(wsUrl: String, model: String = LIVE_MODEL) {
        val attempt = connectionGeneration.incrementAndGet()
        isSessionReady = false
        isGoogleBidiEndpoint = wsUrl.contains("generativelanguage.googleapis.com")
        onStatusChanged(false, "Connecting to Google Gemini Live…")

        // Strictly decouple Live model from standard chat model - always use models/gemini-3.8-live
        // and never route gemini-2.5-flash or gemini-2.5-pro to the Live API
        val targetLiveModel = if (model.contains("2.5") || model.isBlank()) LIVE_MODEL else model

        val client = OkHttpClient.Builder()
            .readTimeout(0, TimeUnit.MILLISECONDS)
            .connectTimeout(20, TimeUnit.SECONDS)
            .pingInterval(20, TimeUnit.SECONDS)
            .build()

        val request = Request.Builder().url(wsUrl).build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.i(tag, "Gemini Live WebSocket opened (Google Bidi: $isGoogleBidiEndpoint)")

                if (isGoogleBidiEndpoint) {
                    // A socket connection is not a ready Live session. Wait for Google's
                    // setupComplete message before enabling native audio capture/playback.
                    onStatusChanged(false, "WebSocket connected; waiting for Gemini Live setupComplete")
                    sendGoogleHandshake(webSocket, targetLiveModel)
                    // A TCP/WebSocket open is not proof that the Live model accepted setup.
                    // Bound the handshake wait and surface the actual failure instead of
                    // leaving MYRA in a silent, half-connected state.
                    scope.launch {
                        delay(15_000)
                        if (connectionGeneration.get() == attempt && !isSessionReady) {
                            val reason = "Gemini Live setup timed out after 15 seconds. Check API key, Live API access, model availability, and network."
                            onStatusChanged(false, reason)
                            webSocket.cancel()
                        }
                    }
                } else {
                    isSessionReady = true
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
                        audioQueue.trySend(pcmBytes.copyOf())
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
                isSessionReady = false
                onStatusChanged(false, t.message ?: "Gemini Live WebSocket failed")
                onSpeakingStateChanged(false)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.i(tag, "Live session closed: code $code, reason: $reason")
                isSessionReady = false
                onStatusChanged(false, reason.ifBlank { "Gemini Live session closed" })
                onSpeakingStateChanged(false)
            }
        })
    }

    fun reconnect(wsUrl: String) {
        disconnect()
        connect(wsUrl, LIVE_MODEL)
    }

    private fun sendGoogleHandshake(ws: WebSocket, model: String) {
        val targetModel = if (model.startsWith("models/")) model else "models/$model"
        // Strict guard: ensure gemini-2.5-flash or gemini-2.5-pro is never sent
        val safeModel = if (targetModel.contains("2.5")) LIVE_MODEL else targetModel

        val setupPayload = JSONObject().apply {
            put("setup", JSONObject().apply {
                put("model", safeModel)
                put("generationConfig", JSONObject().apply {
                    put("responseModalities", JSONArray().apply { put("AUDIO") })
                    put("speechConfig", JSONObject().apply {
                        put("voiceConfig", JSONObject().apply {
                            put("prebuiltVoiceConfig", JSONObject().apply {
                                put("voiceName", "Aoede")
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
            isSessionReady = true
            Log.i(tag, "Gemini Live session is ready; native audio can now be sent")
            onStatusChanged(true, null)
            return
        }

        if (root.has("serverContent")) {
            val serverContent = root.getJSONObject("serverContent")

            // Real-time interruption handling (user barge-in)
            if (serverContent.optBoolean("interrupted", false)) {
                Log.d(tag, "User interruption detected - clearing audio buffers")
                while (audioQueue.tryReceive().isSuccess) { /* discard interrupted audio */ }
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
                            audioQueue.trySend(rawBytes.copyOf())
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

    /**
     * Sends one JPEG camera frame to the active Google Gemini Live session.
     * The caller samples frames at approximately 1 FPS; no frame is retained locally.
     */
    fun sendVideoFrame(jpegBytes: ByteArray): Boolean {
        if (!isSessionReady || !isGoogleBidiEndpoint || jpegBytes.isEmpty()) return false
        return try {
            val base64 = Base64.encodeToString(jpegBytes, Base64.NO_WRAP)
            val payload = JSONObject().apply {
                put("realtimeInput", JSONObject().apply {
                    put("video", JSONObject().apply {
                        put("mimeType", "image/jpeg")
                        put("data", base64)
                    })
                })
            }
            webSocket?.send(payload.toString()) == true
        } catch (e: Exception) {
            Log.w(tag, "Unable to send Gemini Live video frame: ${e.message}")
            false
        }
    }

    fun sendAudioChunk(pcmChunk: ByteArray) {
        if (!isSessionReady) return
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

    fun sendTextMessage(text: String): Boolean {
        if (!isSessionReady) return false
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
            return webSocket?.send(payload.toString()) == true
        } else {
            val payload = JsonObject().apply {
                addProperty("text", text)
            }
            return webSocket?.send(payload.toString()) == true
        }
    }

    /** Stops currently buffered Gemini Live audio without closing the session. */
    fun stopPlayback() {
        while (audioQueue.tryReceive().isSuccess) { /* discard queued audio */ }
        player.stop()
        onSpeakingStateChanged(false)
        onAudioLevel(0f)
    }

    fun disconnect() {
        connectionGeneration.incrementAndGet()
        isSessionReady = false
        try {
            webSocket?.close(1000, "User exited")
        } catch (_: Exception) {}
        webSocket = null
        player.release()
    }
}
