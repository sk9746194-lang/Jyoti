package com.myra.assistant.ui

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.myra.assistant.MainActivity
import com.myra.assistant.data.AssistantState
import com.myra.assistant.data.GeminiApiClient
import com.myra.assistant.data.PreferencesManager
import com.myra.assistant.tools.CommandIntent
import com.myra.assistant.tools.DeviceControlManager
import com.myra.assistant.tools.FastCommandRouter
import com.myra.assistant.tools.PhoneCallManager
import com.myra.assistant.ui.components.AnimatedOrb
import com.myra.assistant.ui.theme.MyraAssistantTheme
import com.myra.assistant.voice.SystemSpeechEngine
import kotlinx.coroutines.launch

/**
 * High-priority Lock Screen Voice Assistant Activity for MYRA.
 * Fully functional: works directly above the Android lock screen when triggered
 * by hotword "Hey MYRA", hardware button, or lock screen shortcut.
 */
class LockScreenOverlayActivity : ComponentActivity() {

    private lateinit var deviceControl: DeviceControlManager
    private lateinit var commandRouter: FastCommandRouter
    private lateinit var phoneControl: PhoneCallManager
    private var speechEngine: SystemSpeechEngine? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Configure activity to display over locked screen and turn display on
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

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        deviceControl = DeviceControlManager(this)
        commandRouter = FastCommandRouter(this)
        phoneControl = PhoneCallManager(this)

        val initialCommand = intent.getStringExtra("EXTRA_VOICE_COMMAND") ?: ""

        setContent {
            MyraAssistantTheme {
                var assistantState by remember { mutableStateOf(AssistantState.LISTENING) }
                var transcript by remember { mutableStateOf(if (initialCommand.isNotBlank()) initialCommand else "Sun rahi hoon...") }
                var assistantReply by remember { mutableStateOf("") }
                var audioLevel by remember { mutableFloatStateOf(0.4f) }

                fun processUserSpeech(text: String) {
                    val trimmed = text.trim()
                    if (trimmed.isEmpty()) return
                    transcript = trimmed
                    assistantState = AssistantState.THINKING
                    deviceControl.triggerHapticFeedback("command_confirm")

                    // Route through local fast command router first
                    val parsed = commandRouter.route(trimmed)
                    when (parsed.intent) {
                        CommandIntent.TOGGLE_TORCH -> {
                            val state = parsed.payload == "on"
                            val res = deviceControl.toggleFlashlight(state)
                            assistantReply = res.message
                            assistantState = AssistantState.SPEAKING
                            speechEngine?.speak(res.message)
                        }
                        CommandIntent.CHECK_BATTERY -> {
                            val res = deviceControl.getBatteryPercentage()
                            assistantReply = res.message
                            assistantState = AssistantState.SPEAKING
                            speechEngine?.speak(res.message)
                        }
                        CommandIntent.CALL_PHONE -> {
                            val res = phoneControl.handleCallCommand(parsed.target)
                            assistantReply = res.message
                            assistantState = AssistantState.SPEAKING
                            speechEngine?.speak(res.message)
                        }
                        CommandIntent.EMERGENCY_SOS -> {
                            phoneControl.handleCallCommand("112")
                            deviceControl.toggleFlashlight(true)
                            val msg = "इमरजेंसी SOS सक्रिय! 112 डायल किया जा रहा है।"
                            assistantReply = msg
                            assistantState = AssistantState.SPEAKING
                            speechEngine?.speak(msg)
                        }
                        CommandIntent.ADJUST_VOLUME -> {
                            val lvl = parsed.payload.toIntOrNull() ?: 80
                            val res = deviceControl.setVolume(lvl)
                            assistantReply = res.message
                            assistantState = AssistantState.SPEAKING
                            speechEngine?.speak(res.message)
                        }
                        CommandIntent.CONVERSATIONAL_CHAT -> {
                            assistantReply = parsed.payload
                            assistantState = AssistantState.SPEAKING
                            speechEngine?.speak(parsed.payload)
                        }
                        else -> {
                            // Consult Gemini AI
                            val prefs = PreferencesManager(this@LockScreenOverlayActivity)
                            if (prefs.hasConfiguredApiKey) {
                                lifecycleScope.launch {
                                    val res = GeminiApiClient.generateResponse(
                                        apiKey = prefs.geminiApiKey,
                                        model = prefs.selectedGeminiModel,
                                        userPrompt = trimmed
                                    )
                                    res.onSuccess { reply ->
                                        assistantReply = reply
                                        assistantState = AssistantState.SPEAKING
                                        speechEngine?.speak(reply)
                                    }.onFailure {
                                        val fallback = "माफ़ कीजिए, मुझे यह समझ नहीं आया: \"$trimmed\""
                                        assistantReply = fallback
                                        assistantState = AssistantState.IDLE
                                        speechEngine?.speak(fallback)
                                    }
                                }
                            } else {
                                val fallback = "MYRA लॉक स्क्रीन पर तैयार है। अधिक विकल्पों के लिए फोन अनलॉक करें।"
                                assistantReply = fallback
                                assistantState = AssistantState.IDLE
                                speechEngine?.speak(fallback)
                            }
                        }
                    }
                }

                LaunchedEffect(Unit) {
                    speechEngine = SystemSpeechEngine(
                        context = this@LockScreenOverlayActivity,
                        onResult = { result ->
                            processUserSpeech(result)
                        },
                        onPartial = { partial ->
                            transcript = partial
                            assistantState = AssistantState.LISTENING
                        },
                        onRmsChanged = { level ->
                            audioLevel = level
                        },
                        onListeningStateChanged = { listening ->
                            if (listening) {
                                assistantState = AssistantState.LISTENING
                            } else if (assistantState == AssistantState.LISTENING) {
                                assistantState = AssistantState.IDLE
                            }
                        },
                        onSpeakingStateChanged = { speaking ->
                            if (speaking) {
                                assistantState = AssistantState.SPEAKING
                            } else if (assistantState == AssistantState.SPEAKING) {
                                assistantState = AssistantState.IDLE
                            }
                        }
                    )

                    if (initialCommand.isNotBlank()) {
                        processUserSpeech(initialCommand)
                    } else {
                        speechEngine?.startListening(continuous = true)
                    }
                }

                // UI Layout
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFF030712),
                                    Color(0xFF090D1C),
                                    Color(0xFF0F172A)
                                )
                            )
                        )
                        .padding(24.dp)
                ) {
                    // Top Bar with Lock indicator & close
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = Color(0x333B82F6),
                            shape = RoundedCornerShape(20.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x6660A5FA))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(Color(0xFF10B981), CircleShape)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "MYRA Lock Screen Active",
                                    color = Color(0xFF93C5FD),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        IconButton(
                            onClick = { finish() },
                            modifier = Modifier
                                .size(36.dp)
                                .background(Color(0x331F2937), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = Color.White
                            )
                        }
                    }

                    // Center Content: Animated Voice Orb + Transcript
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        AnimatedOrb(
                            state = assistantState,
                            audioLevel = audioLevel,
                            modifier = Modifier.size(190.dp)
                        )

                        Spacer(modifier = Modifier.height(28.dp))

                        Text(
                            text = transcript,
                            color = Color(0xFFE2E8F0),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )

                        AnimatedVisibility(visible = assistantReply.isNotBlank()) {
                            Surface(
                                modifier = Modifier
                                    .padding(top = 16.dp)
                                    .fillMaxWidth(),
                                color = Color(0x401E293B),
                                shape = RoundedCornerShape(16.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x4038BDF8))
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.VolumeUp,
                                        contentDescription = null,
                                        tint = Color(0xFF38BDF8),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = assistantReply,
                                        color = Color(0xFFF1F5F9),
                                        fontSize = 15.sp,
                                        lineHeight = 22.sp
                                    )
                                }
                            }
                        }
                    }

                    // Bottom Bar: Mic tap & Unlock Phone button
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Listen again button
                            IconButton(
                                onClick = {
                                    transcript = "Sun rahi hoon..."
                                    assistantState = AssistantState.LISTENING
                                    speechEngine?.startListening()
                                },
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.linearGradient(
                                            listOf(Color(0xFF2563EB), Color(0xFF7C3AED))
                                        )
                                    )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Mic,
                                    contentDescription = "Speak",
                                    tint = Color.White,
                                    modifier = Modifier.size(28.dp)
                                )
                            }

                            // Open Full MYRA (requests unlock)
                            Button(
                                onClick = {
                                    val intent = Intent(this@LockScreenOverlayActivity, MainActivity::class.java).apply {
                                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                                    }
                                    startActivity(intent)
                                    finish()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0x333B82F6)),
                                shape = RoundedCornerShape(24.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x6660A5FA))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LockOpen,
                                    contentDescription = null,
                                    tint = Color(0xFF93C5FD),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Open Full MYRA", color = Color(0xFFE0E7FF), fontSize = 14.sp)
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        speechEngine?.destroy()
    }
}
