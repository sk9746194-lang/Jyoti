package com.myra.assistant.voice

import android.util.Base64
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.*
import java.util.concurrent.TimeUnit

class GeminiLiveVoiceEngine(
    private val scope: CoroutineScope,
    private val onTranscript: (String) -> Unit,
    private val onAudioLevel: (Float) -> Unit,
    private val onSpeakingStateChanged: (Boolean) -> Unit,
    private val onStatusChanged: (Boolean, String?) -> Unit
) {
    private var webSocket: WebSocket? = null
    private val player = AudioTrackPlayer(sampleRate = 24000)
    private val gson = Gson()

    fun connect(wsUrl: String) {
        val client = OkHttpClient.Builder()
            .readTimeout(0, TimeUnit.MILLISECONDS)
            .build()

        val request = Request.Builder().url(wsUrl).build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                onStatusChanged(true, null)
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                try {
                    val json = gson.fromJson(text, JsonObject::class.java)
                    val type = json.get("type")?.asString

                    if (type == "audio") {
                        val base64 = json.get("audio")?.asString ?: return
                        val pcmBytes = Base64.decode(base64, Base64.DEFAULT)
                        onSpeakingStateChanged(true)
                        scope.launch(Dispatchers.IO) {
                            player.writeChunk(pcmBytes)
                        }
                    } else if (type == "transcript") {
                        val transcript = json.get("text")?.asString ?: ""
                        onTranscript(transcript)
                    } else if (type == "turnComplete") {
                        onSpeakingStateChanged(false)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                onStatusChanged(false, t.message)
            }
        })
    }

    fun sendAudioChunk(pcmChunk: ByteArray) {
        val base64 = Base64.encodeToString(pcmChunk, Base64.NO_WRAP)
        val payload = JsonObject().apply {
            addProperty("audio", base64)
        }
        webSocket?.send(payload.toString())
    }

    fun sendTextMessage(text: String) {
        val payload = JsonObject().apply {
            addProperty("text", text)
        }
        webSocket?.send(payload.toString())
    }

    fun disconnect() {
        webSocket?.close(1000, "User exited")
        player.release()
    }
}
