package com.myra.assistant.ui.screens

import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.myra.assistant.R
import com.myra.assistant.data.AssistantState
import com.myra.assistant.data.GeminiApiClient
import com.myra.assistant.data.PreferencesManager
import com.myra.assistant.data.MyraMemoryManager
import com.myra.assistant.ui.orb.OrbCustomizationManager
import com.myra.assistant.ui.orb.OrbRenderer
import com.myra.assistant.ui.theme.*
import com.myra.assistant.tools.WhatsAppAutomation
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    onBack: (() -> Unit)? = null,
    onOpenConnect: () -> Unit = {}
) {
    val context = LocalContext.current
    val whatsAppAutomation = remember(context) { WhatsAppAutomation(context) }
    val scope = rememberCoroutineScope()
    val prefs = remember { PreferencesManager(context) }
    val memoryManager = remember(context) { MyraMemoryManager(context) }
    var memoryEnabled by remember { mutableStateOf(memoryManager.isEnabled) }
    var memoryDraft by remember { mutableStateOf("") }
    var savedMemories by remember { mutableStateOf(memoryManager.getMemories()) }
    val orbManager = remember { OrbCustomizationManager.getInstance(context) }
    val orbSettings by orbManager.settingsState

    var showOrbStudio by remember { mutableStateOf(false) }
    var showSecurityScreen by remember { mutableStateOf(false) }

    var wakeWord by remember { mutableStateOf(prefs.wakeWord) }
    var bluetoothAudio by remember { mutableStateOf(prefs.bluetoothAudio) }
    var speedRate by remember { mutableFloatStateOf(prefs.speechRate) }

    // --- Gemini API Key & Model Configuration State ---
    var apiKeyInput by remember { mutableStateOf(prefs.geminiApiKey) }
    var isApiKeyVisible by remember { mutableStateOf(false) }
    var selectedModel by remember { mutableStateOf(prefs.selectedGeminiModel) }
    var apiStatusMessage by remember { mutableStateOf(prefs.geminiApiConnectionStatus) }
    var isTestingKey by remember { mutableStateOf(false) }

    // --- AI Chat & Voice Conversation State ---
    var isVoiceResponseEnabled by remember { mutableStateOf(prefs.isVoiceResponseInChatEnabled) }
    var isContinuousVoiceEnabled by remember { mutableStateOf(prefs.isContinuousChatVoiceEnabled) }
    var preferredChatLanguage by remember { mutableStateOf(prefs.preferredChatLanguage) }
    var showClearHistoryDialog by remember { mutableStateOf(false) }

    if (showSecurityScreen) {
        SecurityScreen(onBack = { showSecurityScreen = false })
        return
    }

    if (showOrbStudio) {
        OrbCustomizationScreen(onBack = { showOrbStudio = false })
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (onBack != null) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                }
                Column {
                    Text(text = "MYRA Assistant Settings", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text(text = "Gemini API Key, Voice Engine, Orb & Security", color = TextSecondary, fontSize = 12.sp)
                }
            }
        }

        // 🔑 1. Dedicated Gemini API Key & Model Settings Card
        item {
            Surface(
                color = CardDark,
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.5.dp, if (apiStatusMessage.startsWith("Connected")) AccentEmerald else PrimaryBlue.copy(alpha = 0.6f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                color = if (apiStatusMessage.startsWith("Connected")) AccentEmerald.copy(alpha = 0.2f) else PrimaryBlue.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.VpnKey,
                                        contentDescription = null,
                                        tint = if (apiStatusMessage.startsWith("Connected")) AccentEmerald else LightBlue,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Gemini API Settings",
                                    color = TextPrimary,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Custom Google AI Studio Key Integration",
                                    color = TextSecondary,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        // Connection Status Badge
                        Surface(
                            color = when {
                                apiStatusMessage.startsWith("Connected") -> AccentEmerald.copy(alpha = 0.2f)
                                apiStatusMessage.startsWith("Invalid") || apiStatusMessage.startsWith("Connection Failed") -> Color(0xFF7F1D1D)
                                else -> PrimaryBlue.copy(alpha = 0.15f)
                            },
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(1.dp, when {
                                apiStatusMessage.startsWith("Connected") -> AccentEmerald.copy(alpha = 0.5f)
                                apiStatusMessage.startsWith("Invalid") || apiStatusMessage.startsWith("Connection Failed") -> Color(0xFFEF4444)
                                else -> PrimaryBlue.copy(alpha = 0.4f)
                            })
                        ) {
                            Text(
                                text = when {
                                    apiStatusMessage.startsWith("Connected") -> "CONNECTED"
                                    apiStatusMessage.startsWith("Invalid") -> "INVALID KEY"
                                    apiStatusMessage.startsWith("Connection Failed") -> "FAILED"
                                    else -> "OPTIONAL"
                                },
                                color = when {
                                    apiStatusMessage.startsWith("Connected") -> AccentEmerald
                                    apiStatusMessage.startsWith("Invalid") || apiStatusMessage.startsWith("Connection Failed") -> Color(0xFFFCA5A5)
                                    else -> LightBlue
                                },
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    // API Key Input Field with Show/Hide toggle
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(text = "Gemini API Key", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        OutlinedTextField(
                            value = apiKeyInput,
                            onValueChange = { apiKeyInput = it },
                            placeholder = { Text("AIzaSy... (Paste Google Gemini API Key)", color = TextSecondary, fontSize = 12.sp) },
                            visualTransformation = if (isApiKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { isApiKeyVisible = !isApiKeyVisible }) {
                                    Icon(
                                        imageVector = if (isApiKeyVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = if (isApiKeyVisible) "Hide Key" else "Show Key",
                                        tint = TextSecondary
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PrimaryBlue,
                                unfocusedBorderColor = BorderSlate,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )
                    }

                    // Supported Model Selector
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(text = "Active Gemini Model", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            GeminiApiClient.SUPPORTED_MODELS.take(3).forEach { m ->
                                val isSelected = selectedModel == m
                                Surface(
                                    color = if (isSelected) PrimaryBlue.copy(alpha = 0.3f) else BgDark,
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, if (isSelected) PrimaryBlue else BorderSlate),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            selectedModel = m
                                            prefs.selectedGeminiModel = m
                                        }
                                ) {
                                    Text(
                                        text = m.replace("gemini-", ""),
                                        color = if (isSelected) Color.White else TextSecondary,
                                        fontSize = 10.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                        modifier = Modifier.padding(vertical = 8.dp)
                                    )
                                }
                            }
                        }
                        Surface(
                            color = BgDark.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, BorderSlate.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Mic, contentDescription = null, tint = AccentCyan, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Gemini Live Voice: gemini-3.8-live (16kHz in / 24kHz out, full-duplex)",
                                    color = TextSecondary,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }

                    // Connection Status Message
                    Surface(
                        color = BgDark,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = when {
                                    apiStatusMessage.startsWith("Connected") -> Icons.Default.CheckCircle
                                    apiStatusMessage.startsWith("Invalid") || apiStatusMessage.startsWith("Connection Failed") -> Icons.Default.Warning
                                    else -> Icons.Default.Terminal
                                },
                                contentDescription = null,
                                tint = when {
                                    apiStatusMessage.startsWith("Connected") -> AccentEmerald
                                    apiStatusMessage.startsWith("Invalid") || apiStatusMessage.startsWith("Connection Failed") -> Color(0xFFEF4444)
                                    else -> TextSecondary
                                },
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = apiStatusMessage,
                                color = when {
                                    apiStatusMessage.startsWith("Connected") -> AccentEmerald
                                    apiStatusMessage.startsWith("Invalid") || apiStatusMessage.startsWith("Connection Failed") -> Color(0xFFFCA5A5)
                                    else -> TextSecondary
                                },
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    // Action Buttons: Save, Test Connection, Clear
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Save API Key
                        Button(
                            onClick = {
                                prefs.geminiApiKey = apiKeyInput
                                prefs.selectedGeminiModel = selectedModel
                                Toast.makeText(context, "API Key saved securely on device", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Save Key", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        // Test Connection (Real Safe Request)
                        Button(
                            onClick = {
                                scope.launch {
                                    isTestingKey = true
                                    apiStatusMessage = "Testing connection with Google Gemini..."
                                    val (isOk, msg) = GeminiApiClient.testConnection(apiKeyInput, selectedModel)
                                    apiStatusMessage = msg
                                    prefs.geminiApiConnectionStatus = msg
                                    isTestingKey = false
                                }
                            },
                            enabled = !isTestingKey && apiKeyInput.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(containerColor = AccentEmerald),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1.2f)
                        ) {
                            if (isTestingKey) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(12.dp), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Testing...", fontSize = 11.sp)
                            } else {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Test Connection", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        // Clear API Key
                        OutlinedButton(
                            onClick = {
                                apiKeyInput = ""
                                prefs.clearGeminiApiKey()
                                apiStatusMessage = "Not Configured"
                                Toast.makeText(context, "API Key cleared", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                            border = BorderStroke(1.dp, BorderSlate),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(0.8f)
                        ) {
                            Icon(Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(2.dp))
                            Text("Clear", fontSize = 11.sp)
                        }
                    }

                    // Security & Architecture Limitations Notice
                    Surface(
                        color = PrimaryBlue.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, PrimaryBlue.copy(alpha = 0.25f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = "🔒 Security Architecture Notice",
                                color = LightBlue,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Keys saved here are stored privately on this device and never exposed in APK or Git commits. For multi-user production apps, routing via a secure backend proxy (/api/gemini/*) is recommended to prevent client-side key reverse-engineering.",
                                color = TextSecondary,
                                fontSize = 10.sp,
                                lineHeight = 14.sp
                            )
                        }
                    }
                }
            }
        }

        // 💬 2. Dedicated AI Chat & Voice Conversation Settings Card
        item {
            Surface(
                color = CardDark,
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.5.dp, PrimaryBlue.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                color = PrimaryBlue.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.Chat,
                                        contentDescription = null,
                                        tint = CyanNeon,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "AI Chat & Voice System",
                                    color = TextPrimary,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Spoken replies, continuous turns & memory",
                                    color = TextSecondary,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Surface(
                            color = AccentCyan.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, AccentCyan.copy(alpha = 0.3f))
                        ) {
                            Text(
                                text = "CHATGPT-STYLE",
                                color = AccentCyan,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    // Setting 1: Voice Responses in Chat
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Voice Responses in Chat", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Text("Automatically read AI responses aloud using Hindi/English speech engine", color = TextSecondary, fontSize = 10.5.sp)
                        }
                        Switch(
                            checked = isVoiceResponseEnabled,
                            onCheckedChange = {
                                isVoiceResponseEnabled = it
                                prefs.isVoiceResponseInChatEnabled = it
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = PrimaryBlue)
                        )
                    }

                    // Setting 2: Continuous Voice Conversation (Turn-taking)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Continuous Voice Turn-Taking", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("⚡", fontSize = 11.sp)
                            }
                            Text("Keep listening automatically after MYRA finishes speaking for uninterrupted dialogue", color = TextSecondary, fontSize = 10.5.sp)
                        }
                        Switch(
                            checked = isContinuousVoiceEnabled,
                            onCheckedChange = {
                                isContinuousVoiceEnabled = it
                                prefs.isContinuousChatVoiceEnabled = it
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = AccentEmerald)
                        )
                    }

                    // Setting 3: Preferred Response Language
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Preferred Chat Response Language", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val langOptions = listOf("auto" to "Auto-Detect", "hi" to "Hindi (हिन्दी)", "en" to "English")
                            langOptions.forEach { (code, label) ->
                                val isSelected = preferredChatLanguage == code
                                Surface(
                                    color = if (isSelected) PrimaryBlue.copy(alpha = 0.35f) else BgDark,
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, if (isSelected) CyanNeon else BorderSlate),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            preferredChatLanguage = code
                                            prefs.preferredChatLanguage = code
                                        }
                                ) {
                                    Text(
                                        text = label,
                                        color = if (isSelected) Color.White else TextSecondary,
                                        fontSize = 10.5.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                        modifier = Modifier.padding(vertical = 8.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Setting 4: Clear Chat History Button
                    OutlinedButton(
                        onClick = { showClearHistoryDialog = true },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFF43F5E)),
                        border = BorderStroke(1.dp, Color(0xFFF43F5E).copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = Color(0xFFF43F5E), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Clear All Chat History & Memory", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // 🔮 Orb Gallery & Screen Effect Studio Banner Card
        item {
            Surface(
                color = PrimaryBlue.copy(alpha = 0.15f),
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.5.dp, PrimaryBlue),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showOrbStudio = true }
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(BgDark),
                            contentAlignment = Alignment.Center
                        ) {
                            OrbRenderer(
                                state = AssistantState.IDLE,
                                audioLevel = 0.2f,
                                settings = orbSettings.copy(sizeDp = 46f)
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Orb & Screen Studio",
                                    color = TextPrimary,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    color = AccentEmerald.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        "10 STYLES",
                                        color = AccentEmerald,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = "${orbSettings.style.displayName} • ${orbSettings.themeColor.displayName}",
                                color = LightBlue,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Edge Glow: ${orbSettings.edgeEffectMode.displayName} • ${orbSettings.backgroundStyle.displayName}",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = "Open Studio",
                        tint = PrimaryBlue,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        // 🖼️ Home Screen Portrait & Photo Studio Card (Moved to Settings)
        item {
            var currentPhotoPath by remember { mutableStateOf(prefs.customPhotoPath) }
            val customBitmap = remember(currentPhotoPath) {
                if (!currentPhotoPath.isNullOrBlank()) {
                    try {
                        BitmapFactory.decodeFile(currentPhotoPath)?.asImageBitmap()
                    } catch (e: Exception) {
                        null
                    }
                } else null
            }

            val photoPickerLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.GetContent()
            ) { uri: Uri? ->
                if (uri != null) {
                    val savedPath = prefs.saveCustomPhotoFromUri(context, uri)
                    if (savedPath != null) {
                        currentPhotoPath = savedPath
                        Toast.makeText(context, "Home portrait updated successfully!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Could not save photo", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            Surface(
                color = CardDark,
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, CyanNeon.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                color = CyanNeon.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.size(42.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.CameraAlt,
                                        contentDescription = null,
                                        tint = CyanNeon,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Home Screen Portrait",
                                    color = TextPrimary,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (customBitmap != null) "Custom Photo Active" else "Default MYRA Portrait Active",
                                    color = if (customBitmap != null) AccentEmerald else LightBlue,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        // Thumbnail
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .border(1.dp, CyanNeon.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                                .background(Color.Black),
                            contentAlignment = Alignment.Center
                        ) {
                            if (customBitmap != null) {
                                Image(
                                    bitmap = customBitmap,
                                    contentDescription = "Portrait Preview",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Image(
                                    painter = painterResource(id = R.drawable.default_home_photo),
                                    contentDescription = "Default Portrait",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }

                    Text(
                        text = "Home Screen की फोटो बदलने का विकल्प सेटिंग में है। यहाँ से आप अपनी पसंदीदा फोटो सेट कर सकते हैं या डिफ़ॉल्ट पर रीसेट कर सकते हैं।",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { photoPickerLauncher.launch("image/*") },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (customBitmap != null) "Change Photo" else "Upload Photo", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        if (customBitmap != null) {
                            OutlinedButton(
                                onClick = {
                                    prefs.resetHomePhoto(context)
                                    currentPhotoPath = null
                                    Toast.makeText(context, "Reset to default portrait", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFDA4AF)),
                                border = BorderStroke(1.dp, Color(0xFFF43F5E).copy(alpha = 0.6f)),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(0.8f)
                            ) {
                                Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Reset", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }

        // 🛡️ MYRA Security — Owner Voice & Biometric Shield Card
        item {
            val securityManager = remember { com.myra.assistant.security.OwnerSecurityManager.getInstance(context) }
            val isSecured = securityManager.isOwnerProtectionEnabled
            val isEnrolled = securityManager.isOwnerVoiceEnrolled

            Surface(
                color = AccentEmerald.copy(alpha = 0.12f),
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.5.dp, AccentEmerald.copy(alpha = 0.7f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showSecurityScreen = true }
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            color = AccentEmerald.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.size(46.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Security,
                                    contentDescription = null,
                                    tint = AccentEmerald,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "MYRA Security",
                                    color = TextPrimary,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    color = if (isSecured) AccentEmerald.copy(alpha = 0.25f) else Color.Gray.copy(alpha = 0.25f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        if (isSecured) "PROTECTED" else "ENROLL",
                                        color = if (isSecured) AccentEmerald else TextSecondary,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = "Owner Voice: ${if (isEnrolled) "Enrolled" else "Not Enrolled"} • Biometric: ${if (securityManager.isBiometricLockEnabled) "ON" else "OFF"}",
                                color = LightBlue,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Fingerprint / Face ID & Owner Verification for Sensitive Actions",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = "Open Security",
                        tint = AccentEmerald,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        // 📱 MYRA Connect — Phone to Phone Card
        item {
            Surface(
                color = CyanNeon.copy(alpha = 0.12f),
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.5.dp, CyanNeon.copy(alpha = 0.7f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenConnect() }
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            color = CyanNeon.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.size(46.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.PhoneAndroid,
                                    contentDescription = null,
                                    tint = CyanNeon,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "MYRA Connect",
                                    color = TextPrimary,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    color = CyanNeon.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        "PHONE TO PHONE",
                                        color = CyanNeon,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = "Pair with second Android phone • Wi-Fi / Hotspot",
                                color = LightBlue,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Remote YouTube, Camera, WhatsApp & hardware actions",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = "Open Connect",
                        tint = CyanNeon,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        // 🎙 Hands-Free Hotword & Lock Screen Voice Architecture
        item {
            var isHandsFree by remember { mutableStateOf(prefs.isHandsFreeWakeWordEnabled) }
            var isLockScreenAllowed by remember { mutableStateOf(prefs.isLockScreenAllowed) }
            var isKeepScreenAwake by remember { mutableStateOf(prefs.isKeepScreenOnEnabled) }

            Surface(
                color = CardDark,
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.5.dp, if (isHandsFree) PrimaryBlue else BorderSlate),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                color = if (isHandsFree) PrimaryBlue.copy(alpha = 0.2f) else BorderSlate.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.Mic,
                                        contentDescription = null,
                                        tint = if (isHandsFree) LightBlue else TextSecondary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Hands-Free & Lock Screen",
                                    color = TextPrimary,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Always-on voice detection & display wake",
                                    color = TextSecondary,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Surface(
                            color = if (isHandsFree) AccentEmerald.copy(alpha = 0.2f) else BorderSlate.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = if (isHandsFree) "ACTIVE" else "DISABLED",
                                color = if (isHandsFree) AccentEmerald else TextSecondary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                    }

                    HorizontalDivider(color = BorderSlate.copy(alpha = 0.5f))

                    // 1. Hands-Free Wake Word Switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Hands-Free \"Hey MYRA\" Listening",
                                color = TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Uses Android speech recognition with a visible notification. Microphone permission is required; background and lock-screen availability vary by device and Android version.",
                                color = TextSecondary,
                                fontSize = 11.sp,
                                lineHeight = 15.sp
                            )
                        }
                        Switch(
                            checked = isHandsFree,
                            onCheckedChange = { enabled ->
                                isHandsFree = enabled
                                prefs.isHandsFreeWakeWordEnabled = enabled
                                if (enabled) {
                                    val micGranted = androidx.core.content.ContextCompat.checkSelfPermission(
                                        context, android.Manifest.permission.RECORD_AUDIO
                                    ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                                    if (micGranted) {
                                        com.myra.assistant.services.MyraWakeWordService.start(context)
                                    } else if (context is android.app.Activity) {
                                        androidx.core.app.ActivityCompat.requestPermissions(
                                            context, arrayOf(android.Manifest.permission.RECORD_AUDIO), 4102
                                        )
                                    }
                                } else {
                                    com.myra.assistant.services.MyraWakeWordService.stop(context)
                                }
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = LightBlue, checkedTrackColor = PrimaryBlue)
                        )
                    }

                    // 2. Lock Screen Access Switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Work on Lock Screen",
                                color = TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Wake up display (TurnScreenOn) and show MYRA assistant over lock screen when hotword is spoken.",
                                color = TextSecondary,
                                fontSize = 11.sp,
                                lineHeight = 15.sp
                            )
                        }
                        Switch(
                            checked = isLockScreenAllowed,
                            onCheckedChange = { enabled ->
                                isLockScreenAllowed = enabled
                                prefs.isLockScreenAllowed = enabled
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = LightBlue, checkedTrackColor = PrimaryBlue)
                        )
                    }

                    // 3. Keep Screen Awake
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Keep Screen Awake",
                                color = TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Keeps the screen on while actively talking to MYRA.",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                        Switch(
                            checked = isKeepScreenAwake,
                            onCheckedChange = { enabled ->
                                isKeepScreenAwake = enabled
                                prefs.isKeepScreenOnEnabled = enabled
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = LightBlue, checkedTrackColor = PrimaryBlue)
                        )
                    }

                    // 4. Custom Wake Phrase Input
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Recognized Wake Phrases",
                            color = TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Supports: 'Hey MYRA', 'मायरा', 'सुनो मायरा', 'नमस्ते मायरा', 'Hello MYRA'",
                            color = LightBlue,
                            fontSize = 11.sp
                        )
                        OutlinedTextField(
                            value = wakeWord,
                            onValueChange = {
                                wakeWord = it
                                prefs.wakeWord = it
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PrimaryBlue,
                                unfocusedBorderColor = BorderSlate,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    // 5. Test Hotword Trigger Button
                    Button(
                        onClick = {
                            val testIntent = android.content.Intent(context, com.myra.assistant.ui.LockScreenOverlayActivity::class.java).apply {
                                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                putExtra("EXTRA_VOICE_COMMAND", "नमस्ते MYRA, क्या तुम ठीक काम कर रही हो?")
                            }
                            context.startActivity(testIntent)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue.copy(alpha = 0.25f)),
                        border = BorderStroke(1.dp, PrimaryBlue),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.VolumeUp, contentDescription = null, tint = LightBlue, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("🧪 Test Lock Screen Voice Assistant", color = TextPrimary, fontSize = 13.sp)
                    }
                }
            }
        }

        // 💬 WhatsApp Automation & Accessibility Service Card
        item {
            var isAccessibilityActive by remember { mutableStateOf(whatsAppAutomation.isAccessibilityActive()) }
            val isWhatsAppInstalled = remember { whatsAppAutomation.isInstalled() }
            var isRequireConfirm by remember { mutableStateOf(prefs.isWhatsAppConfirmBeforeSend) }

            Surface(
                color = CardDark,
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.5.dp, if (isAccessibilityActive) AccentEmerald else PrimaryBlue.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    // Header & Status Badge
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                color = if (isAccessibilityActive) AccentEmerald.copy(alpha = 0.2f) else PrimaryBlue.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.size(38.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.Send,
                                        contentDescription = null,
                                        tint = if (isAccessibilityActive) AccentEmerald else LightBlue,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "WhatsApp Automation",
                                    color = TextPrimary,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Hands-free contact messaging & verification",
                                    color = TextSecondary,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Surface(
                            color = when {
                                !isWhatsAppInstalled -> Color(0xFFFCA5A5).copy(alpha = 0.2f)
                                isAccessibilityActive -> AccentEmerald.copy(alpha = 0.2f)
                                else -> Color(0xFFFBBF24).copy(alpha = 0.2f)
                            },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = when {
                                    !isWhatsAppInstalled -> "NOT INSTALLED"
                                    isAccessibilityActive -> "SERVICE ACTIVE"
                                    else -> "SETUP NEEDED"
                                },
                                color = when {
                                    !isWhatsAppInstalled -> Color(0xFFFCA5A5)
                                    isAccessibilityActive -> AccentEmerald
                                    else -> Color(0xFFFBBF24)
                                },
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                    }

                    HorizontalDivider(color = BorderSlate.copy(alpha = 0.5f))

                    // Accessibility Service Explanation & Setup Instructions
                    Surface(
                        color = BgDark,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.AccessibilityNew,
                                    contentDescription = null,
                                    tint = if (isAccessibilityActive) AccentEmerald else LightBlue,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isAccessibilityActive) "Accessibility Service is Running" else "Android Accessibility Setup Required",
                                    color = TextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            Text(
                                text = if (isAccessibilityActive) {
                                    "✓ MYRA Voice & Automation Service is enabled. MYRA can automatically locate WhatsApp chat input fields, verify recipient names before sending, and detect sent confirmation."
                                } else {
                                    "To send messages without manually tapping on WhatsApp, Android requires you to enable the MYRA Accessibility Service in Settings. This permission allows MYRA to type your dictated message and verify the contact name."
                                },
                                color = TextSecondary,
                                fontSize = 11.sp,
                                lineHeight = 15.sp
                            )

                            if (!isAccessibilityActive) {
                                Column(
                                    modifier = Modifier
                                        .background(PrimaryBlue.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                                        .padding(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Text("How to enable manually:", color = LightBlue, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Text("1. Tap \"Open Accessibility Settings\" below", color = TextSecondary, fontSize = 10.sp)
                                    Text("2. Look under \"Downloaded Apps\" or \"Installed Services\"", color = TextSecondary, fontSize = 10.sp)
                                    Text("3. Tap \"MYRA Voice & Automation Service\"", color = TextSecondary, fontSize = 10.sp)
                                    Text("4. Toggle switch to ON and confirm permission", color = TextSecondary, fontSize = 10.sp)
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        whatsAppAutomation.openAccessibilitySettings()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = if (isAccessibilityActive) BorderSlate else PrimaryBlue),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Open Accessibility Settings", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }

                                OutlinedButton(
                                    onClick = {
                                        isAccessibilityActive = whatsAppAutomation.isAccessibilityActive()
                                        Toast.makeText(
                                            context,
                                            if (isAccessibilityActive) "Service is active!" else "Service not enabled yet",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = LightBlue),
                                    border = BorderStroke(1.dp, BorderSlate),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                    }

                    // User Confirmation Before Send Switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Require Confirmation Before Send",
                                color = TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = if (isRequireConfirm) {
                                    "Safety ON: MYRA opens the contact chat and pre-fills your message for review. You tap Send."
                                } else {
                                    "Hands-Free Auto-Send: MYRA verifies the recipient name and automatically clicks Send."
                                },
                                color = TextSecondary,
                                fontSize = 11.sp,
                                lineHeight = 15.sp
                            )
                        }
                        Switch(
                            checked = isRequireConfirm,
                            onCheckedChange = { checked ->
                                isRequireConfirm = checked
                                prefs.isWhatsAppConfirmBeforeSend = checked
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = LightBlue, checkedTrackColor = PrimaryBlue)
                        )
                    }

                    // Security & Recipient Verification Notice
                    Surface(
                        color = PrimaryBlue.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, PrimaryBlue.copy(alpha = 0.25f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(
                                text = "🔒 Strict Recipient Verification Rule",
                                color = LightBlue,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "To prevent sending messages to the wrong person, MYRA checks the WhatsApp title against the target contact name. If the screen does not match, auto-send is aborted immediately.",
                                color = TextSecondary,
                                fontSize = 10.sp,
                                lineHeight = 14.sp
                            )
                        }
                    }
                }
            }
        }

        // MYRA Personal Memory — user-controlled and encrypted at rest
        item {
            Surface(
                color = CardDark,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, BorderSlate),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("MYRA Personal Memory", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            Text("आपकी अनुमति से सेव की गई बातें ही MYRA बातचीत में उपयोग करेगी।", color = TextSecondary, fontSize = 11.sp)
                        }
                        Switch(
                            checked = memoryEnabled,
                            onCheckedChange = {
                                memoryManager.isEnabled = it
                                memoryEnabled = it
                            },
                            enabled = memoryManager.isStorageAvailable,
                            colors = SwitchDefaults.colors(checkedThumbColor = LightBlue, checkedTrackColor = PrimaryBlue)
                        )
                    }
                    if (!memoryManager.isStorageAvailable) {
                        Text("Encrypted memory storage इस डिवाइस पर उपलब्ध नहीं है; सुरक्षा के लिए Memory बंद रखी गई है।", color = Color(0xFFFCA5A5), fontSize = 11.sp)
                    } else {
                        OutlinedTextField(
                            value = memoryDraft,
                            onValueChange = { memoryDraft = it.take(240) },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = memoryEnabled,
                            label = { Text("MYRA को क्या याद रखना चाहिए?") },
                            placeholder = { Text("उदाहरण: मुझे जवाब हिंदी में पसंद हैं") },
                            minLines = 2,
                            maxLines = 3
                        )
                        Button(
                            onClick = {
                                if (memoryManager.saveMemory(memoryDraft)) {
                                    savedMemories = memoryManager.getMemories()
                                    memoryDraft = ""
                                    Toast.makeText(context, "बात सुरक्षित रूप से याद रखी गई।", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Memory सेव नहीं हो सकी। कृपया टेक्स्ट जाँचें।", Toast.LENGTH_SHORT).show()
                                }
                            },
                            enabled = memoryEnabled && memoryDraft.trim().length >= 3,
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("याद रखें")
                        }
                        Text("Saved memories: ${savedMemories.size}", color = TextSecondary, fontSize = 11.sp)
                        savedMemories.forEach { memory ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(memory.text, color = TextPrimary, fontSize = 12.sp, modifier = Modifier.weight(1f))
                                IconButton(onClick = {
                                    memoryManager.deleteMemory(memory.id)
                                    savedMemories = memoryManager.getMemories()
                                }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete memory", tint = Color(0xFFF87171))
                                }
                            }
                        }
                    }
                }
            }
        }

        // Bluetooth Audio SCO
        item {
            Surface(
                color = CardDark,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, BorderSlate),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "Bluetooth Headset / SCO", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            Text(text = "Route mic and audio via car or Bluetooth earbuds", color = TextSecondary, fontSize = 11.sp)
                        }
                        Switch(
                            checked = bluetoothAudio,
                            onCheckedChange = {
                                bluetoothAudio = it
                                prefs.bluetoothAudio = it
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = LightBlue, checkedTrackColor = PrimaryBlue)
                        )
                    }
                }
            }
        }

        // Speech Rate (Speaking Speed)
        item {
            Surface(
                color = CardDark,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, BorderSlate),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(text = "Speech Rate (Speaking Speed): ${"%.1f".format(speedRate)}x", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Slider(
                        value = speedRate,
                        onValueChange = {
                            speedRate = it
                            prefs.speechRate = it
                        },
                        valueRange = 0.5f..2.0f,
                        steps = 15,
                        colors = SliderDefaults.colors(thumbColor = LightBlue, activeTrackColor = PrimaryBlue)
                    )
                }
            }
        }
    }

    if (showClearHistoryDialog) {
        AlertDialog(
            onDismissRequest = { showClearHistoryDialog = false },
            title = { Text("Clear Chat History & Memory?", color = Color.White, fontWeight = FontWeight.Bold) },
            text = { Text("This will permanently delete all conversational messages and reset memory across sessions.", color = TextSecondary, fontSize = 13.sp) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearHistoryDialog = false
                        com.myra.assistant.data.ChatConversationManager.clearHistory(context)
                        Toast.makeText(context, "Chat history and memory cleared", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("Clear All", color = Color(0xFFF43F5E), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearHistoryDialog = false }) {
                    Text("Cancel", color = LightBlue)
                }
            },
            containerColor = Color(0xFF0F1A30),
            shape = RoundedCornerShape(16.dp)
        )
    }
}
