package com.myra.assistant.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.myra.assistant.data.AssistantState
import com.myra.assistant.data.VoiceStatus
import com.myra.assistant.ui.components.WaveformVisualizer
import com.myra.assistant.ui.orb.*
import com.myra.assistant.ui.theme.*

@Composable
fun MainVoiceScreen(
    assistantState: AssistantState,
    voiceStatus: VoiceStatus,
    audioLevel: Float,
    liveTranscript: String,
    assistantResponse: String,
    onStartListening: () -> Unit,
    onStopListening: () -> Unit,
    onSendCommand: (String) -> Unit,
    onOpenConnect: () -> Unit = {}
) {
    val context = LocalContext.current
    val orbManager = remember { OrbCustomizationManager.getInstance(context) }
    val orbSettings by orbManager.settingsState
    val connectManager = remember { com.myra.assistant.connect.MyraConnectManager.getInstance(context) }
    val pairedDevices by connectManager.pairedDevices
    val connectedRemote = pairedDevices.firstOrNull { it.status == com.myra.assistant.connect.ConnectionStatus.CONNECTED }

    var textInput by remember { mutableStateOf("") }

    Box(modifier = Modifier.fillMaxSize()) {
        // 1. Full-Screen Atmospheric Background
        BackgroundRenderer(
            style = orbSettings.backgroundStyle,
            speed = orbSettings.backgroundSpeed,
            opacity = orbSettings.backgroundOpacity,
            theme = orbSettings.themeColor
        )

        // 2. Full-Screen Dynamic Edge Speaking / Listening Light
        EdgeEffectRenderer(
            state = assistantState,
            audioLevel = audioLevel,
            mode = orbSettings.edgeEffectMode,
            intensity = orbSettings.edgeGlowIntensity,
            theme = orbSettings.themeColor
        )

        // 3. Main Screen UI Layout
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Header: MYRA Branding & AI Engine Status
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = Color(0xFF1E3A8A).copy(alpha = 0.6f),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, PrimaryBlue.copy(alpha = 0.4f))
                        ) {
                            Text(
                                text = "MYRA 2026",
                                color = Color(0xFF93C5FD),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${orbSettings.style.displayName} • ${orbSettings.themeColor.displayName}",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (connectedRemote != null) {
                            Surface(
                                onClick = onOpenConnect,
                                color = Color(0xFF0F172A),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, CyanNeon.copy(alpha = 0.5f)),
                                modifier = Modifier.padding(end = 6.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(AccentEmerald)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = connectedRemote.name,
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }

                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(AccentEmerald)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Live",
                            color = AccentEmerald,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Gemini Voice Status Badge
                Surface(
                    color = if (voiceStatus.fallbackActive) Color(0xFF451A03).copy(alpha = 0.7f) else Color(0xFF064E3B).copy(alpha = 0.7f),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, if (voiceStatus.fallbackActive) AccentAmber else AccentEmerald)
                ) {
                    Text(
                        text = if (voiceStatus.fallbackActive) "⚠️ Fallback Browser TTS Active" else "⚡ Gemini Live 24kHz • Kore Voice",
                        color = if (voiceStatus.fallbackActive) AccentAmber else Color(0xFFA7F3D0),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                    )
                }
            }

            // Center Stage: Customizable Orb Visualizer & Sinusoidal Waveform
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(y = (orbSettings.position.yOffsetFactor * 100).dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                OrbRenderer(
                    state = assistantState,
                    audioLevel = audioLevel,
                    settings = orbSettings,
                    onClick = {
                        if (assistantState == AssistantState.LISTENING) onStopListening() else onStartListening()
                    }
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Waveform Equalizer
                WaveformVisualizer(
                    audioLevel = audioLevel,
                    isSpeakingOrListening = (assistantState == AssistantState.LISTENING || assistantState == AssistantState.SPEAKING),
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .height(30.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // State Action Badge
                Surface(
                    color = when (assistantState) {
                        AssistantState.LISTENING -> Color(0xFF881337)
                        AssistantState.SPEAKING -> Color(0xFF064E3B)
                        AssistantState.THINKING -> Color(0xFF78350F)
                        AssistantState.ERROR -> Color(0xFF7F1D1D)
                        AssistantState.WAKE_WORD -> Color(0xFF1E3A8A)
                        AssistantState.IDLE -> CardDark
                    },
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(
                        1.dp,
                        when (assistantState) {
                            AssistantState.LISTENING -> AccentRose
                            AssistantState.SPEAKING -> AccentEmerald
                            AssistantState.THINKING -> AccentAmber
                            AssistantState.ERROR -> Color(0xFFEF4444)
                            AssistantState.WAKE_WORD -> Color(0xFF60A5FA)
                            AssistantState.IDLE -> PrimaryBlue.copy(alpha = 0.5f)
                        }
                    )
                ) {
                    Text(
                        text = when (assistantState) {
                            AssistantState.IDLE -> "TAP ORB TO SPEAK"
                            AssistantState.LISTENING -> "Listening to you..."
                            AssistantState.THINKING -> "Thinking & Processing..."
                            AssistantState.SPEAKING -> "MYRA Speaking..."
                            AssistantState.ERROR -> "Error / Reconnecting..."
                            AssistantState.WAKE_WORD -> "Wake Word Detected!"
                        },
                        color = TextPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Live Transcript & Speech Display Card
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = CardDark.copy(alpha = 0.88f),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, BorderSlate)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        if (liveTranscript.isNotEmpty()) {
                            Text(text = "Listening to you:", color = AccentIndigo, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(text = "\"$liveTranscript\"", color = TextPrimary, fontSize = 14.sp)
                        } else if (assistantResponse.isNotEmpty()) {
                            Text(text = "MYRA:", color = LightBlue, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(text = assistantResponse, color = TextPrimary, fontSize = 14.sp)
                        } else {
                            Text(
                                text = "Namaste! Main MYRA hoon — 2026 Edition Hindi AI Assistant.",
                                color = TextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Phone calls, WhatsApp, music, flashlight ya settings ke liye boliye...",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }

            // Bottom Bar: Voice Mic Trigger & Fast Command Input
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = textInput,
                    onValueChange = { textInput = it },
                    placeholder = { Text("Boliye ya type karein...", color = TextSecondary, fontSize = 12.sp) },
                    modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryBlue,
                        unfocusedBorderColor = BorderSlate,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    shape = RoundedCornerShape(24.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.width(8.dp))

                if (textInput.isNotBlank()) {
                    IconButton(
                        onClick = {
                            val cmd = textInput
                            textInput = ""
                            onSendCommand(cmd)
                        },
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(PrimaryBlue)
                    ) {
                        Icon(Icons.Default.Send, contentDescription = "Send", tint = Color.White)
                    }
                } else {
                    IconButton(
                        onClick = {
                            if (assistantState == AssistantState.LISTENING) onStopListening() else onStartListening()
                        },
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(if (assistantState == AssistantState.LISTENING) AccentRose else PrimaryBlue)
                    ) {
                        Icon(
                            if (assistantState == AssistantState.LISTENING) Icons.Default.MicOff else Icons.Default.Mic,
                            contentDescription = "Mic",
                            tint = Color.White
                        )
                    }
                }
            }
        }
    }
}
