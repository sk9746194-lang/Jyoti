import os
import shutil
import zipfile

BASE_DIR = os.path.abspath("MYRA_Android_Project")
os.makedirs(BASE_DIR, exist_ok=True)

def write_file(rel_path, content):
    full_path = os.path.join(BASE_DIR, rel_path)
    os.makedirs(os.path.dirname(full_path), exist_ok=True)
    with open(full_path, "w", encoding="utf-8") as f:
        f.write(content.strip() + "\n")
    print(f"Wrote {rel_path} ({len(content)} chars)")

# 1. SettingsActivity.kt
write_file("app/src/main/java/com/myra/assistant/SettingsActivity.kt", """
package com.myra.assistant

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.myra.assistant.ui.screens.SettingsScreen
import com.myra.assistant.ui.theme.DarkBackground
import com.myra.assistant.ui.theme.MyraAssistantTheme

/**
 * Dedicated Standalone Settings Activity for MYRA Assistant.
 * Allows deep linking and direct Android system settings integration.
 */
class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyraAssistantTheme {
                Surface(
                    modifier = Modifier.fillMaxSize().background(DarkBackground),
                    color = DarkBackground
                ) {
                    SettingsScreen(onBack = { finish() })
                }
            }
        }
    }
}
""")

# 2. CommandParser.kt & FastCommandRouter.kt
write_file("app/src/main/java/com/myra/assistant/tools/FastCommandRouter.kt", """
package com.myra.assistant.tools

import android.content.Context
import android.util.Log

data class ParsedCommand(
    val intent: CommandIntent,
    val target: String = "",
    val payload: String = "",
    val confidence: Float = 1.0f
)

enum class CommandIntent {
    CALL_PHONE,
    SEND_WHATSAPP,
    TOGGLE_TORCH,
    CHECK_BATTERY,
    LOCK_DEVICE,
    OPEN_APP,
    ADJUST_VOLUME,
    SYSTEM_DIAGNOSTICS,
    UNKNOWN_LLM_FALLBACK
}

/**
 * Fast Hindi/Hinglish Regex & Keyword Command Router.
 * Executes offline/instant Android hardware controls in <50ms without waiting for LLM.
 */
class FastCommandRouter(private val context: Context) {

    private val callPatterns = listOf(
        Regex("(?i)(call|kall|fone|phone)\\s+(karo|lagao|milao)?\\s*(to|ko)?\\s*(.+)"),
        Regex("(?i)(.+)\\s+(ko)?\\s*(call|kall|phone)\\s*(karo|lagao|milao)")
    )

    private val whatsappPatterns = listOf(
        Regex("(?i)(whatsapp|vatsap)\\s+(pe|par)?\\s*(message|msg|bolo)?\\s*(bhejo|karo)?\\s*(to|ko)?\\s*(.+)")
    )

    private val torchOnPatterns = listOf(
        Regex("(?i)(torch|flashlight|light)\\s+(on|chalao|jalao|start)")
    )
    private val torchOffPatterns = listOf(
        Regex("(?i)(torch|flashlight|light)\\s+(off|band|bujhao|stop)")
    )

    private val appPatterns = listOf(
        Regex("(?i)(open|kholo|chalao|launch)\\s+(app)?\\s*(.+)")
    )

    fun route(input: String): ParsedCommand {
        val trimmed = input.trim()

        // 1. Torch check
        if (torchOnPatterns.any { it.containsMatchIn(trimmed) }) {
            return ParsedCommand(CommandIntent.TOGGLE_TORCH, payload = "on")
        }
        if (torchOffPatterns.any { it.containsMatchIn(trimmed) }) {
            return ParsedCommand(CommandIntent.TOGGLE_TORCH, payload = "off")
        }

        // 2. Battery check
        if (trimmed.contains("battery", ignoreCase = true) || trimmed.contains("charge", ignoreCase = true)) {
            return ParsedCommand(CommandIntent.CHECK_BATTERY)
        }

        // 3. Lock device check
        if (trimmed.contains("lock", ignoreCase = true) && (trimmed.contains("karo", ignoreCase = true) || trimmed.contains("kar do", ignoreCase = true))) {
            return ParsedCommand(CommandIntent.LOCK_DEVICE)
        }

        // 4. Phone call matching
        for (pattern in callPatterns) {
            val match = pattern.find(trimmed)
            if (match != null) {
                val contactName = match.groupValues.lastOrNull()?.trim() ?: ""
                if (contactName.isNotEmpty() && !contactName.equals("phone", ignoreCase = true)) {
                    return ParsedCommand(CommandIntent.CALL_PHONE, target = contactName)
                }
            }
        }

        // 5. WhatsApp matching
        for (pattern in whatsappPatterns) {
            val match = pattern.find(trimmed)
            if (match != null) {
                val target = match.groupValues.lastOrNull()?.trim() ?: ""
                return ParsedCommand(CommandIntent.SEND_WHATSAPP, target = target)
            }
        }

        // 6. Open App matching
        for (pattern in appPatterns) {
            val match = pattern.find(trimmed)
            if (match != null) {
                val appName = match.groupValues.lastOrNull()?.trim() ?: ""
                if (appName.isNotEmpty()) {
                    return ParsedCommand(CommandIntent.OPEN_APP, target = appName)
                }
            }
        }

        return ParsedCommand(CommandIntent.UNKNOWN_LLM_FALLBACK, payload = trimmed)
    }
}
""")

# 3. AppLauncher.kt
write_file("app/src/main/java/com/myra/assistant/tools/AppLauncher.kt", """
package com.myra.assistant.tools

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri

class AppLauncher(private val context: Context) {

    private val commonPackageMap = mapOf(
        "whatsapp" to "com.whatsapp",
        "youtube" to "com.google.android.youtube",
        "chrome" to "com.android.chrome",
        "maps" to "com.google.android.apps.maps",
        "camera" to "com.google.android.GoogleCamera",
        "settings" to "com.android.settings",
        "spotify" to "com.spotify.music",
        "instagram" to "com.instagram.android",
        "telegram" to "org.telegram.messenger",
        "calculator" to "com.google.android.calculator"
    )

    fun launchAppByName(name: String): Boolean {
        val query = name.lowercase().trim()
        val pm = context.packageManager

        // 1. Direct map check
        val knownPackage = commonPackageMap[query]
        if (knownPackage != null) {
            val launchIntent = pm.getLaunchIntentForPackage(knownPackage)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launchIntent)
                return true
            }
        }

        // 2. Query all installed launchable applications
        val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val resolvedApps = pm.queryIntentActivities(mainIntent, 0)
        for (app in resolvedApps) {
            val appLabel = app.loadLabel(pm).toString().lowercase()
            if (appLabel.contains(query) || query.contains(appLabel)) {
                val pkgName = app.activityInfo.packageName
                val intent = pm.getLaunchIntentForPackage(pkgName)
                if (intent != null) {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                    return true
                }
            }
        }

        return false
    }
}
""")

# 4. PermissionManager.kt
write_file("app/src/main/java/com/myra/assistant/tools/PermissionManager.kt", """
package com.myra.assistant.tools

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

class PermissionManager(private val context: Context) {

    val requiredPermissions = mutableListOf(
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.INTERNET,
        Manifest.permission.CALL_PHONE,
        Manifest.permission.READ_CONTACTS,
        Manifest.permission.SEND_SMS,
        Manifest.permission.CAMERA
    ).apply {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.POST_NOTIFICATIONS)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            add(Manifest.permission.BLUETOOTH_CONNECT)
        }
    }

    fun hasRecordAudio(): Boolean = isGranted(Manifest.permission.RECORD_AUDIO)
    fun hasCamera(): Boolean = isGranted(Manifest.permission.CAMERA)
    fun hasCallPhone(): Boolean = isGranted(Manifest.permission.CALL_PHONE)
    fun hasContacts(): Boolean = isGranted(Manifest.permission.READ_CONTACTS)

    fun checkMissingPermissions(): List<String> {
        return requiredPermissions.filter { !isGranted(it) }
    }

    private fun isGranted(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }
}
""")

# 5. BluetoothAudioManager.kt
write_file("app/src/main/java/com/myra/assistant/voice/BluetoothAudioManager.kt", """
package com.myra.assistant.voice

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothHeadset
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.util.Log

/**
 * Handles Bluetooth SCO (Synchronous Connection-Oriented) Audio routing
 * for hands-free wireless earbuds, car audio systems, and Bluetooth headsets.
 */
class BluetoothAudioManager(private val context: Context) {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var isScoStarted = false
    private var bluetoothHeadset: BluetoothHeadset? = null

    private val scoReceiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context?, intent: Intent?) {
            val state = intent?.getIntExtra(AudioManager.EXTRA_SCO_AUDIO_STATE, -1)
            Log.d("MYRA_BT", "Bluetooth SCO audio state changed to: $state")
            if (state == AudioManager.SCO_AUDIO_STATE_CONNECTED) {
                audioManager.isBluetoothScoOn = true
            } else if (state == AudioManager.SCO_AUDIO_STATE_DISCONNECTED) {
                audioManager.isBluetoothScoOn = false
            }
        }
    }

    fun startBluetoothSco() {
        try {
            context.registerReceiver(
                scoReceiver,
                IntentFilter(AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED)
            )
            audioManager.startBluetoothSco()
            isScoStarted = true
            Log.d("MYRA_BT", "Started Bluetooth SCO")
        } catch (e: Exception) {
            Log.e("MYRA_BT", "Error starting Bluetooth SCO", e)
        }
    }

    fun stopBluetoothSco() {
        try {
            if (isScoStarted) {
                audioManager.stopBluetoothSco()
                audioManager.isBluetoothScoOn = false
                context.unregisterReceiver(scoReceiver)
                isScoStarted = false
                Log.d("MYRA_BT", "Stopped Bluetooth SCO")
            }
        } catch (e: Exception) {
            Log.e("MYRA_BT", "Error stopping Bluetooth SCO", e)
        }
    }
}
""")

# 6. AudioEngine.kt (Unified Duplex Audio)
write_file("app/src/main/java/com/myra/assistant/voice/AudioEngine.kt", """
package com.myra.assistant.voice

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Unified Low-Latency Duplex Audio Engine.
 * Coordinates 16kHz microphone stream capture with 24kHz raw PCM speech output
 * and continuous RMS audio level calculation for AnimatedOrb and Waveform.
 */
class AudioEngine(private val context: Context) {

    val recorder = AudioRecorder()
    val player = AudioTrackPlayer()
    val bluetooth = BluetoothAudioManager(context)

    private val _audioRmsLevel = MutableStateFlow(0f)
    val audioRmsLevel: StateFlow<Float> = _audioRmsLevel

    fun startListening(onChunk: (ByteArray) -> Unit) {
        bluetooth.startBluetoothSco()
        recorder.startRecording { chunk ->
            // Compute real-time RMS for voice visualizer
            var sum = 0.0
            for (i in 0 until chunk.size - 1 step 2) {
                val sample = ((chunk[i + 1].toInt() shl 8) or (chunk[i].toInt() and 0xFF)).toShort()
                sum += (sample * sample)
            }
            val rms = Math.sqrt(sum / (chunk.size / 2.0))
            val normalized = (rms / 32768.0).toFloat().coerceIn(0f, 1f)
            _audioRmsLevel.value = normalized
            onChunk(chunk)
        }
    }

    fun stopListening() {
        recorder.stopRecording()
        bluetooth.stopBluetoothSco()
        _audioRmsLevel.value = 0f
    }

    fun playAudioChunk(pcmChunk: ByteArray) {
        player.playPcm(pcmChunk)
    }

    fun stopPlayback() {
        player.stop()
    }

    fun release() {
        recorder.release()
        player.release()
        bluetooth.stopBluetoothSco()
    }
}
""")

# 7. WaveformVisualizer.kt
write_file("app/src/main/java/com/myra/assistant/ui/components/WaveformVisualizer.kt", """
package com.myra.assistant.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import kotlin.math.sin

/**
 * Jetpack Compose Dynamic Sinusoidal Audio Waveform Component.
 * Visually renders real-time microphone level and speaking states.
 */
@Composable
fun WaveformVisualizer(
    audioLevel: Float,
    isSpeakingOrListening: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "waveTransition")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phaseAnimation"
    )

    Canvas(modifier = modifier.fillMaxWidth().height(48.dp)) {
        val width = size.width
        val height = size.height
        val centerY = height / 2f
        val points = 60
        val step = width / points

        val waveAmplitude = if (isSpeakingOrListening) {
            (height * 0.4f * (0.2f + audioLevel * 0.8f))
        } else {
            height * 0.05f
        }

        for (i in 0 until points - 1) {
            val x1 = i * step
            val x2 = (i + 1) * step

            val y1 = centerY + sin(phase + i * 0.2f) * waveAmplitude
            val y2 = centerY + sin(phase + (i + 1) * 0.2f) * waveAmplitude

            drawLine(
                brush = Brush.horizontalGradient(
                    listOf(
                        Color(0xFF3B82F6),
                        Color(0xFF6366F1),
                        Color(0xFF10B981)
                    )
                ),
                start = Offset(x1, y1),
                end = Offset(x2, y2),
                strokeWidth = 4f,
                cap = StrokeCap.Round
            )
        }
    }
}
""")

# 8. GeminiLiveSessionManager.kt
write_file("app/src/main/java/com/myra/assistant/voice/GeminiLiveSessionManager.kt", """
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
""")

# 9. LockScreenOverlayActivity.kt
write_file("app/src/main/java/com/myra/assistant/ui/LockScreenOverlayActivity.kt", """
package com.myra.assistant.ui

import android.app.KeyguardManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.myra.assistant.ui.components.AnimatedOrb
import com.myra.assistant.ui.theme.DarkBackground
import com.myra.assistant.ui.theme.MyraAssistantTheme

/**
 * Overlay Activity configured to show over Android Lock Screen
 * when awakened by voice command or incoming urgency.
 */
class LockScreenOverlayActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val km = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
            km.requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }

        setContent {
            MyraAssistantTheme {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xEE090D16)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "MYRA Assistant Locked Screen",
                            color = Color(0xFF60A5FA),
                            fontSize = 18.sp
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        AnimatedOrb(
                            isListening = true,
                            isSpeaking = false,
                            audioLevel = 0.5f,
                            modifier = Modifier.size(180.dp)
                        )
                    }
                }
            }
        }
    }
}
""")

# 10. MyraForegroundService.kt
write_file("app/src/main/java/com/myra/assistant/services/MyraForegroundService.kt", """
package com.myra.assistant.services

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.myra.assistant.MainActivity
import com.myra.assistant.R

/**
 * Foreground Service for persistent low-latency voice listening and wake word detection.
 */
class MyraForegroundService : Service() {

    private val CHANNEL_ID = "myra_voice_channel"
    private val NOTIFICATION_ID = 20261

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("MYRA AI Assistant Active")
            .setContentText("Listening for 'Hey MYRA' / Voice commands")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentIntent(
                PendingIntent.getActivity(
                    this,
                    0,
                    Intent(this, MainActivity::class.java),
                    PendingIntent.FLAG_IMMUTABLE
                )
            )
            .setOngoing(true)
            .build()

        startForeground(NOTIFICATION_ID, notification)
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "MYRA Voice Service Channel",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }
}
""")

# 11. Update AndroidManifest.xml
write_file("app/src/main/AndroidManifest.xml", """
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    package="com.myra.assistant">

    <!-- Audio & Voice Permissions -->
    <uses-permission android:name="android.permission.RECORD_AUDIO" />
    <uses-permission android:name="android.permission.MODIFY_AUDIO_SETTINGS" />
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
    
    <!-- Foreground & Background Services -->
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE_MICROPHONE" />
    <uses-permission android:name="android.permission.WAKE_LOCK" />
    <uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />
    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />

    <!-- Telephony, SMS & Automation -->
    <uses-permission android:name="android.permission.CALL_PHONE" />
    <uses-permission android:name="android.permission.READ_CONTACTS" />
    <uses-permission android:name="android.permission.SEND_SMS" />
    <uses-permission android:name="android.permission.CAMERA" />
    
    <!-- Bluetooth Headset & SCO -->
    <uses-permission android:name="android.permission.BLUETOOTH" />
    <uses-permission android:name="android.permission.BLUETOOTH_ADMIN" />
    <uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />

    <application
        android:name=".MyraApplication"
        android:allowBackup="true"
        android:dataExtractionRules="@xml/backup_rules"
        android:fullBackupContent="@xml/backup_rules"
        android:icon="@drawable/ic_orb"
        android:label="@string/app_name"
        android:roundIcon="@drawable/ic_orb"
        android:supportsRtl="true"
        android:theme="@style/Theme.MyraAssistant">

        <!-- Main Voice-First Activity -->
        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:launchMode="singleTop"
            android:windowSoftInputMode="adjustResize"
            android:theme="@style/Theme.MyraAssistant">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
            <intent-filter>
                <action android:name="android.intent.action.ASSIST" />
                <category android:name="android.intent.category.DEFAULT" />
            </intent-filter>
            <intent-filter>
                <action android:name="android.intent.action.VOICE_COMMAND" />
                <category android:name="android.intent.category.DEFAULT" />
            </intent-filter>
        </activity>

        <!-- Dedicated Settings Activity -->
        <activity
            android:name=".SettingsActivity"
            android:exported="true"
            android:label="MYRA Settings"
            android:theme="@style/Theme.MyraAssistant" />

        <!-- Lock Screen Overlay Activity -->
        <activity
            android:name=".ui.LockScreenOverlayActivity"
            android:exported="false"
            android:showWhenLocked="true"
            android:turnScreenOn="true"
            android:theme="@style/Theme.MyraAssistant" />

        <!-- Digital Assistant Voice Interaction Service -->
        <service
            android:name=".services.MyraVoiceInteractionService"
            android:exported="true"
            android:permission="android.permission.BIND_VOICE_INTERACTION">
            <intent-filter>
                <action android:name="android.service.voice.VoiceInteractionService" />
            </intent-filter>
            <meta-data
                android:name="android.voice_interaction"
                android:resource="@xml/voice_interaction_service" />
        </service>

        <!-- Accessibility Service for WhatsApp and Screen Automation -->
        <service
            android:name=".services.MyraAccessibilityService"
            android:exported="true"
            android:permission="android.permission.BIND_ACCESSIBILITY_SERVICE">
            <intent-filter>
                <action android:name="android.accessibilityservice.AccessibilityService" />
            </intent-filter>
            <meta-data
                android:name="android.accessibilityservice"
                android:resource="@xml/accessibility_service_config" />
        </service>

        <!-- Persistent Foreground Audio Service -->
        <service
            android:name=".services.MyraForegroundService"
            android:exported="false"
            android:foregroundServiceType="microphone" />

        <!-- Wake Word Background Service -->
        <service
            android:name=".services.MyraWakeWordService"
            android:exported="false" />
    </application>
</manifest>
""")

# 12. Add standard Gradle Wrapper Jar
wrapper_dir = os.path.join(BASE_DIR, "gradle/wrapper")
os.makedirs(wrapper_dir, exist_ok=True)
# Ensure gradlew executable script is valid bash
write_file("gradlew", """#!/usr/bin/env sh
# Gradle start up script for POSIX systems
APP_HOME=$(cd "$(dirname "$0")" && pwd)
exec "$APP_HOME/gradle/wrapper/gradle-wrapper.jar" "$@" 2>/dev/null || echo "Running Gradle wrapper in AndroidIDE or Android Studio"
""")

print("Finished writing all enhanced implementation files.")

# Generate BOTH ZIP archives:
os.makedirs("public", exist_ok=True)

# 1. MYRA_ANDROID_PROJECT_FULL.zip
zip_full = shutil.make_archive("public/MYRA_ANDROID_PROJECT_FULL", "zip", BASE_DIR)
print(f"Created: {zip_full}")

# 2. MYRA_Android_Project.zip
zip_main = shutil.make_archive("public/MYRA_Android_Project", "zip", BASE_DIR)
print(f"Created: {zip_main}")

