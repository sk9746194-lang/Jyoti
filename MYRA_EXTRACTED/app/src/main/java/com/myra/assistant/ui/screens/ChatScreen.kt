package com.myra.assistant.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.myra.assistant.data.ChatMessage
import com.myra.assistant.data.ChatConversationManager
import com.myra.assistant.ui.theme.*
import kotlinx.coroutines.launch

/**
 * Complete ChatGPT-Like AI Chat & Voice Conversation System for MYRA.
 * Features:
 * 1. Clean conversational UI with user/assistant message alignment and timestamps.
 * 2. Multi-turn follow-up conversation memory with Gemini API.
 * 3. Integrated voice chat (speak & listen) with natural Hindi and English TTS.
 * 4. Stop-generation, copy-to-clipboard, retry failed prompts, and stop-speaking controls.
 * 5. Continuous voice turn-taking mode with echo protection.
 * 6. Local persistent chat history with New Chat and Clear History confirmation.
 * 7. Modern empty state with prompt suggestions in Hindi and English.
 */
@Composable
fun ChatScreen(
    messages: List<ChatMessage>,
    isGenerating: Boolean = false,
    isSpeaking: Boolean = false,
    isListening: Boolean = false,
    activeModel: String = "gemini-3.8-flash",
    isVoiceResponseEnabled: Boolean = true,
    isContinuousVoiceEnabled: Boolean = false,
    onSendMessage: (String) -> Unit,
    onStopGeneration: () -> Unit = {},
    onRetryMessage: (ChatMessage) -> Unit = {},
    onStartVoiceInput: () -> Unit = {},
    onStopVoiceInput: () -> Unit = {},
    onSpeakMessage: (String) -> Unit = {},
    onStopSpeaking: () -> Unit = {},
    onNewChat: () -> Unit = {},
    onClearChatHistory: () -> Unit = {},
    onToggleVoiceResponse: () -> Unit = {},
    onToggleContinuousVoice: () -> Unit = {},
    onNavigateBack: () -> Unit = {}
) {
    val context = LocalContext.current
    var textInput by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    var showClearDialog by remember { mutableStateOf(false) }

    // Pulsing animation for listening/thinking states
    val infiniteTransition = rememberInfiniteTransition(label = "pulseTransition")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    // Auto-scroll to the latest message whenever messages change or generation starts
    LaunchedEffect(messages.size, isGenerating) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    val promptSuggestions = listOf(
        "विज्ञान को हिंदी में समझाएं",
        "Who is Narendra Modi?",
        "Help me with mathematics",
        "Write a leave application",
        "Translate Hindi to English",
        "Explain Android development",
        "Call Mummy",
        "WhatsApp message bhejo"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF030712),
                        Color(0xFF070D1D),
                        Color(0xFF0A1128)
                    )
                )
            )
    ) {
        // -------------------------------------------------------------
        // 1. TOP HEADER (ChatGPT-Style with Controls)
        // -------------------------------------------------------------
        Surface(
            color = Color(0xFF0A1020).copy(alpha = 0.96f),
            border = BorderStroke(1.dp, BorderSlate),
            shadowElevation = 8.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onNavigateBack,
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF0D1424))
                                .border(1.dp, BorderSlate, RoundedCornerShape(8.dp))
                        ) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = LightBlue, modifier = Modifier.size(18.dp))
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // MYRA Glowing Avatar
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Brush.linearGradient(listOf(CyanNeon, PrimaryBlue))),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("M", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("MYRA AI", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Spacer(modifier = Modifier.width(4.dp))
                                Surface(
                                    color = Color(0xFF0F172A),
                                    shape = RoundedCornerShape(4.dp),
                                    border = BorderStroke(0.5.dp, CyanNeon.copy(alpha = 0.5f))
                                ) {
                                    Text(
                                        text = activeModel.replace("gemini-", ""),
                                        color = CyanNeon,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                            Text(
                                text = if (isSpeaking) "Speaking..." else if (isGenerating) "Thinking..." else if (isListening) "Listening..." else "Ready • Hindi & English",
                                color = if (isSpeaking) AccentCyan else if (isGenerating) AccentAmber else if (isListening) AccentEmerald else TextSecondary,
                                fontSize = 10.sp
                            )
                        }
                    }

                    // Action Controls: New Chat, Voice Response Toggle, Clear
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        // Voice Response Audio Toggle
                        IconButton(
                            onClick = onToggleVoiceResponse,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                if (isVoiceResponseEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                                contentDescription = "Voice Responses",
                                tint = if (isVoiceResponseEnabled) CyanNeon else TextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // Continuous Voice Toggle
                        IconButton(
                            onClick = onToggleContinuousVoice,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.Repeat,
                                contentDescription = "Continuous Conversation",
                                tint = if (isContinuousVoiceEnabled) AccentEmerald else TextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // New Chat Button
                        IconButton(
                            onClick = onNewChat,
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF0F172A))
                                .border(1.dp, BorderSlate, RoundedCornerShape(8.dp))
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "New Chat", tint = Color.White, modifier = Modifier.size(18.dp))
                        }

                        // Clear History Button
                        IconButton(
                            onClick = { showClearDialog = true },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = "Clear History", tint = TextSecondary, modifier = Modifier.size(18.dp))
                        }
                    }
                }

                // Continuous Voice Active Banner
                if (isContinuousVoiceEnabled) {
                    Surface(
                        color = AccentEmerald.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, AccentEmerald.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(AccentEmerald)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Continuous Voice Conversation Active (Hands-Free)",
                                    color = AccentEmerald,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Text(
                                text = "TAP TO STOP",
                                color = Color.White,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .clickable { onToggleContinuousVoice() }
                                    .padding(2.dp)
                            )
                        }
                    }
                }
            }
        }

        // -------------------------------------------------------------
        // 2. MESSAGES LIST OR EMPTY STATE
        // -------------------------------------------------------------
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (messages.isEmpty() && !isGenerating) {
                // ChatGPT-Style Empty State
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Surface(
                        color = Color(0xFF0F1E38).copy(alpha = 0.9f),
                        shape = CircleShape,
                        border = BorderStroke(1.5.dp, CyanNeon.copy(alpha = 0.5f)),
                        modifier = Modifier.size(68.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.ChatBubbleOutline,
                                contentDescription = null,
                                tint = CyanNeon,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "How can I help you today?",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Ask general knowledge, science, mathematics, coding, translation, or speak hands-free in Hindi & English.",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 17.sp,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = "Suggested Prompts",
                        color = TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.align(Alignment.Start)
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // Suggestion Grid
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        promptSuggestions.take(4).forEach { prompt ->
                            Surface(
                                color = Color(0xFF0D1527),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, BorderSlate),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSendMessage(prompt) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = prompt, color = Color(0xFFE2E8F0), fontSize = 12.sp)
                                    Icon(Icons.Default.ArrowForward, contentDescription = null, tint = CyanNeon, modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(top = 10.dp, bottom = 16.dp)
                ) {
                    items(messages, key = { it.id }) { msg ->
                        val isUser = msg.sender == "user"

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                        ) {
                            if (!isUser) {
                                Box(
                                    modifier = Modifier
                                        .size(30.dp)
                                        .clip(CircleShape)
                                        .background(Brush.linearGradient(listOf(CyanNeon, PrimaryBlue))),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("M", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                            }

                            Column(
                                horizontalAlignment = if (isUser) Alignment.End else Alignment.Start,
                                modifier = Modifier.widthIn(max = 300.dp)
                            ) {
                                Surface(
                                    color = if (isUser) PrimaryBlue else if (msg.isError) Color(0xFF3B151E) else Color(0xFF0F1A30),
                                    shape = RoundedCornerShape(
                                        topStart = 16.dp,
                                        topEnd = 16.dp,
                                        bottomStart = if (isUser) 16.dp else 4.dp,
                                        bottomEnd = if (isUser) 4.dp else 16.dp
                                    ),
                                    border = BorderStroke(
                                        1.dp,
                                        if (isUser) Color(0xFF38BDF8).copy(alpha = 0.5f)
                                        else if (msg.isError) Color(0xFFF43F5E).copy(alpha = 0.6f)
                                        else Color(0xFF1E293B)
                                    ),
                                    shadowElevation = 2.dp
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(
                                            text = msg.text,
                                            color = if (msg.isError) Color(0xFFFECDD3) else Color.White,
                                            fontSize = 13.5.sp,
                                            lineHeight = 19.sp
                                        )

                                        Spacer(modifier = Modifier.height(6.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            if (msg.category != null) {
                                                Text(
                                                    text = msg.category,
                                                    color = CyanNeon,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            } else {
                                                Spacer(modifier = Modifier.width(1.dp))
                                            }

                                            Text(
                                                text = msg.time,
                                                color = if (isUser) Color.White.copy(alpha = 0.7f) else TextSecondary,
                                                fontSize = 9.5.sp
                                            )
                                        }
                                    }
                                }

                                // Action Row below Assistant Responses
                                if (!isUser) {
                                    Row(
                                        modifier = Modifier.padding(top = 4.dp, start = 2.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Copy Response Button
                                        IconButton(
                                            onClick = {
                                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                clipboard.setPrimaryClip(ClipData.newPlainText("MYRA Response", msg.text))
                                                Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                                            },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = TextSecondary, modifier = Modifier.size(13.dp))
                                        }

                                        // Read-Aloud / Speak Button
                                        if (!msg.isError) {
                                            IconButton(
                                                onClick = {
                                                    val speechText = ChatConversationManager.sanitizeForSpeech(msg.text)
                                                    onSpeakMessage(speechText)
                                                },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(Icons.Default.VolumeUp, contentDescription = "Speak", tint = LightBlue, modifier = Modifier.size(14.dp))
                                            }
                                        }

                                        // Retry Button on Error
                                        if (msg.isError) {
                                            Surface(
                                                onClick = { onRetryMessage(msg) },
                                                color = Color(0xFF261219),
                                                shape = RoundedCornerShape(8.dp),
                                                border = BorderStroke(1.dp, Color(0xFFF43F5E).copy(alpha = 0.5f))
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(Icons.Default.Refresh, contentDescription = "Retry", tint = Color(0xFFF43F5E), modifier = Modifier.size(11.dp))
                                                    Spacer(modifier = Modifier.width(3.dp))
                                                    Text("Retry", color = Color(0xFFF43F5E), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Generation in Progress Indicator
                    if (isGenerating) {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Start,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(30.dp)
                                        .clip(CircleShape)
                                        .background(Brush.linearGradient(listOf(CyanNeon, PrimaryBlue))),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("M", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.width(8.dp))

                                Surface(
                                    color = Color(0xFF0F1A30),
                                    shape = RoundedCornerShape(14.dp),
                                    border = BorderStroke(1.dp, CyanNeon.copy(alpha = 0.4f)),
                                    modifier = Modifier.padding(vertical = 4.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        CircularProgressIndicator(
                                            color = CyanNeon,
                                            strokeWidth = 2.dp,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "MYRA is thinking...",
                                            color = TextSecondary,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))

                                        // Stop Generation Button
                                        Surface(
                                            onClick = onStopGeneration,
                                            color = Color(0xFF1E293B),
                                            shape = RoundedCornerShape(6.dp),
                                            border = BorderStroke(1.dp, BorderSlate)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(Icons.Default.Stop, contentDescription = "Stop", tint = AccentAmber, modifier = Modifier.size(11.dp))
                                                Spacer(modifier = Modifier.width(2.dp))
                                                Text("Stop", color = AccentAmber, fontSize = 9.5.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // -------------------------------------------------------------
        // 3. QUICK CHIPS (When conversation has started)
        // -------------------------------------------------------------
        if (messages.isNotEmpty()) {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                contentPadding = PaddingValues(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(promptSuggestions) { query ->
                    Surface(
                        onClick = { onSendMessage(query) },
                        color = Color(0xFF0F172A).copy(alpha = 0.85f),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, CyanNeon.copy(alpha = 0.3f))
                    ) {
                        Text(
                            text = query,
                            color = LightBlue,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }
            }
        }

        // -------------------------------------------------------------
        // 4. BOTTOM INPUT BAR (Text + Voice Integration)
        // -------------------------------------------------------------
        Surface(
            color = Color(0xFF090E1A),
            border = BorderStroke(1.dp, BorderSlate),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Microphone Voice Input Button
                IconButton(
                    onClick = {
                        if (isListening) onStopVoiceInput() else onStartVoiceInput()
                    },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(if (isListening) AccentEmerald else Color(0xFF0F1A30))
                        .border(1.dp, if (isListening) AccentCyan else BorderSlate, CircleShape)
                        .scale(if (isListening) pulseScale else 1f)
                ) {
                    Icon(
                        Icons.Default.Mic,
                        contentDescription = "Voice Input",
                        tint = if (isListening) Color.White else CyanNeon,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Text Input Field
                OutlinedTextField(
                    value = textInput,
                    onValueChange = { textInput = it },
                    placeholder = {
                        Text(
                            text = if (isListening) "Listening in Hindi/English..." else "Message MYRA or ask anything...",
                            color = if (isListening) AccentEmerald else TextSecondary,
                            fontSize = 12.5.sp
                        )
                    },
                    modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyanNeon,
                        unfocusedBorderColor = BorderSlate,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    shape = RoundedCornerShape(22.dp),
                    maxLines = 4
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Stop Speaking button if active, otherwise Send button
                if (isSpeaking) {
                    IconButton(
                        onClick = onStopSpeaking,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF3B151E))
                            .border(1.dp, Color(0xFFF43F5E), CircleShape)
                    ) {
                        Icon(
                            Icons.Default.VolumeOff,
                            contentDescription = "Stop Speaking",
                            tint = Color(0xFFF43F5E),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                } else {
                    IconButton(
                        onClick = {
                            val trimmed = textInput.trim()
                            if (trimmed.isNotEmpty() && !isGenerating) {
                                textInput = ""
                                onSendMessage(trimmed)
                            }
                        },
                        enabled = textInput.isNotBlank() && !isGenerating,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(
                                if (textInput.isNotBlank() && !isGenerating)
                                    Brush.linearGradient(listOf(CyanNeon, PrimaryBlue))
                                else
                                    Brush.linearGradient(listOf(Color(0xFF1E293B), Color(0xFF1E293B)))
                            )
                    ) {
                        Icon(
                            Icons.Default.Send,
                            contentDescription = "Send",
                            tint = if (textInput.isNotBlank() && !isGenerating) Color.White else TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }

    // Confirmation Dialog for Clearing Chat History
    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Clear Chat History?", color = Color.White, fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to delete all messages in this conversation? This action cannot be undone.", color = TextSecondary, fontSize = 13.sp) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearDialog = false
                        onClearChatHistory()
                    }
                ) {
                    Text("Clear All", color = Color(0xFFF43F5E), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Cancel", color = LightBlue)
                }
            },
            containerColor = Color(0xFF0F1A30),
            shape = RoundedCornerShape(16.dp)
        )
    }
}
