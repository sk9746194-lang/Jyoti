package com.myra.assistant

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.myra.assistant.data.AssistantState
import com.myra.assistant.data.VoiceStatus
import com.myra.assistant.security.BiometricManager
import com.myra.assistant.security.OwnerSecurityManager
import com.myra.assistant.tools.AppLauncher
import com.myra.assistant.tools.CommandIntent
import com.myra.assistant.tools.DeviceControlManager
import com.myra.assistant.tools.FastCommandRouter
import com.myra.assistant.tools.ParsedCommand
import com.myra.assistant.tools.PermissionManager
import com.myra.assistant.tools.PhoneCallManager
import com.myra.assistant.tools.WhatsAppAutomation
import com.myra.assistant.data.ChatMessage
import com.myra.assistant.data.ChatConversationManager
import com.myra.assistant.data.GeminiApiClient
import com.myra.assistant.ui.screens.ChatScreen
import com.myra.assistant.ui.screens.HomeScreen
import com.myra.assistant.ui.screens.MainVoiceScreen
import com.myra.assistant.ui.screens.SettingsScreen
import com.myra.assistant.ui.screens.ToolsScreen
import com.myra.assistant.ui.theme.CardDark
import com.myra.assistant.ui.theme.MyraAssistantTheme
import com.myra.assistant.voice.AudioEngine
import com.myra.assistant.voice.GeminiLiveVoiceEngine
import com.myra.assistant.voice.SystemSpeechEngine
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainActivity : FragmentActivity() {

    private lateinit var deviceControl: DeviceControlManager
    private lateinit var phoneControl: PhoneCallManager
    private lateinit var whatsAppAutomation: WhatsAppAutomation
    private lateinit var commandRouter: FastCommandRouter
    private lateinit var appLauncher: AppLauncher
    private lateinit var permissionManager: PermissionManager
    private lateinit var audioEngine: AudioEngine
    private lateinit var securityManager: OwnerSecurityManager
    private lateinit var biometricManager: BiometricManager

    private var liveEngine: GeminiLiveVoiceEngine? = null
    private var systemEngine: SystemSpeechEngine? = null
    private var externalCommandDispatcher: ((String) -> Unit)? = null
    private var externalVoiceStarter: (() -> Unit)? = null

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        startWakeWordIfPermitted()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        deviceControl = DeviceControlManager(this)
        phoneControl = PhoneCallManager(this)
        whatsAppAutomation = WhatsAppAutomation(this)
        commandRouter = FastCommandRouter(this)
        appLauncher = AppLauncher(this)
        permissionManager = PermissionManager(this)
        audioEngine = AudioEngine(this)
        securityManager = OwnerSecurityManager.getInstance(this)
        biometricManager = BiometricManager.getInstance(this)

        checkAndRequestPermissions()

        // 9. LOCK SCREEN & HANDS-FREE CONFIGURATION
        val prefs = com.myra.assistant.data.PreferencesManager(this)
        if (prefs.isLockScreenAllowed) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O_MR1) {
                setShowWhenLocked(true)
                setTurnScreenOn(true)
                val km = getSystemService(android.content.Context.KEYGUARD_SERVICE) as android.app.KeyguardManager
                km.requestDismissKeyguard(this, null)
            } else {
                @Suppress("DEPRECATION")
                window.addFlags(
                    android.view.WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    android.view.WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                )
            }
        }
        if (prefs.isKeepScreenOnEnabled) {
            window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }

        // Start microphone listening only when the user opted in and permission is granted.
        startWakeWordIfPermitted()

        // 10. APP OPEN PROTECTION
        if (securityManager.isOwnerProtectionEnabled && securityManager.isAppOpenProtectionEnabled) {
            securityManager.requireOwnerAuthentication(
                activity = this,
                title = "MYRA Owner Verification",
                subtitle = "असिस्टेंट शुरू करने के लिए ओनर वेरिफिकेशन आवश्यक है",
                onSuccess = {
                    android.util.Log.i("MYRA_Security", "App open biometric verified successfully.")
                },
                onError = { err ->
                    android.util.Log.w("MYRA_Security", "App open authentication cancelled or failed: $err")
                }
            )
        }

        setContent {
            MyraAssistantTheme {
                var currentTab by remember { mutableIntStateOf(0) }
                var showConnectScreen by remember { mutableStateOf(false) }
                var assistantState by remember { mutableStateOf(AssistantState.IDLE) }
                var voiceStatus by remember { mutableStateOf(VoiceStatus()) }
                var audioLevel by remember { mutableFloatStateOf(0f) }
                var liveTranscript by remember { mutableStateOf("") }
                var assistantResponse by remember { mutableStateOf("") }

                val prefs = remember { com.myra.assistant.data.PreferencesManager(this@MainActivity) }
                val chatMessages = remember { mutableStateListOf<ChatMessage>() }
                var isGeneratingChat by remember { mutableStateOf(false) }
                var isTtsSpeaking by remember { mutableStateOf(false) }
                var activeChatJob by remember { mutableStateOf<Job?>(null) }

                fun sendChatMessage(cmd: String) {
                    val trimmed = cmd.trim()
                    if (trimmed.isEmpty()) return

                    // Add user message
                    val userMsg = ChatMessage(sender = "user", text = trimmed)
                    chatMessages.add(userMsg)
                    ChatConversationManager.saveHistory(this@MainActivity, chatMessages)
                    liveTranscript = trimmed
                    deviceControl.triggerHapticFeedback("command_confirm")

                    // 1. Direct Device Tool Execution (Torch, Call, WhatsApp, Battery, Volume, Apps)
                    val parsed = commandRouter.route(trimmed)
                    if (parsed.intent != CommandIntent.CONVERSATIONAL_CHAT && parsed.intent != CommandIntent.UNKNOWN_LLM_FALLBACK) {
                        assistantState = AssistantState.THINKING
                        handleCommand(trimmed) { reply ->
                            assistantResponse = reply
                            assistantState = AssistantState.SPEAKING
                            deviceControl.triggerHapticFeedback("speech_start")
                            val toolMsg = ChatMessage(sender = "myra", text = reply, category = "Device Tool")
                            chatMessages.add(toolMsg)
                            ChatConversationManager.saveHistory(this@MainActivity, chatMessages)
                            if (prefs.isVoiceResponseInChatEnabled) {
                                isTtsSpeaking = true
                                systemEngine?.speak(ChatConversationManager.sanitizeForSpeech(reply))
                            } else {
                                assistantState = AssistantState.IDLE
                            }
                        }
                        return
                    }

                    // 2. Multimodal AI Question / ChatGPT-Like Conversation with History
                    isGeneratingChat = true
                    assistantState = AssistantState.THINKING

                    if (!prefs.hasConfiguredApiKey) {
                        val reply = "मैं MYRA हूँ। आपने पूछा: \"$trimmed\"।\n\nविस्तृत AI उत्तर के लिए सेटिंग्स में अपनी Google Gemini API Key दर्ज करें, या मुझसे कॉल, वॉट्सऐप और टॉर्च जैसी कमांड्स कहें।"
                        val guideMsg = ChatMessage(sender = "myra", text = reply, category = "Setup Guide")
                        chatMessages.add(guideMsg)
                        ChatConversationManager.saveHistory(this@MainActivity, chatMessages)
                        isGeneratingChat = false
                        assistantState = AssistantState.IDLE
                        if (prefs.isVoiceResponseInChatEnabled) {
                            isTtsSpeaking = true
                            systemEngine?.speak(ChatConversationManager.sanitizeForSpeech(reply))
                        }
                        return
                    }

                    val contextHistory = ChatConversationManager.buildContextHistory(chatMessages.dropLast(1))
                    activeChatJob?.cancel()
                    activeChatJob = lifecycleScope.launch {
                        try {
                            val res = GeminiApiClient.generateChatConversation(
                                apiKey = prefs.geminiApiKey,
                                model = prefs.selectedGeminiModel,
                                conversationHistory = contextHistory,
                                userPrompt = trimmed,
                                preferredLanguage = prefs.preferredChatLanguage
                            )

                            res.onSuccess { aiReply ->
                                val aiMsg = ChatMessage(sender = "myra", text = aiReply, category = "MYRA AI")
                                chatMessages.add(aiMsg)
                                ChatConversationManager.saveHistory(this@MainActivity, chatMessages)
                                isGeneratingChat = false
                                assistantState = AssistantState.SPEAKING
                                deviceControl.triggerHapticFeedback("speech_start")

                                if (prefs.isVoiceResponseInChatEnabled) {
                                    isTtsSpeaking = true
                                    systemEngine?.speak(ChatConversationManager.sanitizeForSpeech(aiReply))
                                } else {
                                    assistantState = AssistantState.IDLE
                                }
                            }.onFailure { err ->
                                val errMsg = ChatMessage(
                                    sender = "myra",
                                    text = "त्रुटि: ${err.localizedMessage ?: "AI सेवा से संपर्क नहीं हो सका। कृपया नेटवर्क या API Key जांचें।"}",
                                    isError = true,
                                    category = "API Error"
                                )
                                chatMessages.add(errMsg)
                                ChatConversationManager.saveHistory(this@MainActivity, chatMessages)
                                isGeneratingChat = false
                                assistantState = AssistantState.IDLE
                            }
                        } catch (e: Exception) {
                            if (e !is kotlinx.coroutines.CancellationException) {
                                val cancelMsg = ChatMessage(
                                    sender = "myra",
                                    text = "Request error: ${e.message}",
                                    isError = true
                                )
                                chatMessages.add(cancelMsg)
                                isGeneratingChat = false
                                assistantState = AssistantState.IDLE
                            }
                        }
                    }
                }

                fun dispatchUserCommand(cmd: String) {
                    sendChatMessage(cmd)
                }

                LaunchedEffect(Unit) {
                    // Load persistent chat history from local disk
                    val savedHistory = ChatConversationManager.loadHistory(this@MainActivity)
                    if (savedHistory.isNotEmpty()) {
                        chatMessages.addAll(savedHistory)
                    } else {
                        val welcome = ChatMessage(
                            sender = "myra",
                            text = "Namaste! Main MYRA hoon — aapki personal AI companion. Phone calls, WhatsApp, torch ya kuch bhi poochne ke liye boliye ya likhiye!",
                            category = "Welcome"
                        )
                        chatMessages.add(welcome)
                        ChatConversationManager.saveHistory(this@MainActivity, chatMessages)
                    }

                    systemEngine = SystemSpeechEngine(
                        context = this@MainActivity,
                        onResult = { text ->
                            if (currentTab == 1) {
                                sendChatMessage(text)
                            } else {
                                dispatchUserCommand(text)
                            }
                        },
                        onPartial = { partial ->
                            liveTranscript = partial
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
                            isTtsSpeaking = speaking
                            if (speaking) {
                                assistantState = AssistantState.SPEAKING
                            } else {
                                if (assistantState == AssistantState.SPEAKING) {
                                    assistantState = AssistantState.IDLE
                                }
                                // Continuous Voice Turn-taking: resume listening after speaking completes
                                if (currentTab == 1 && prefs.isContinuousChatVoiceEnabled && !isGeneratingChat) {
                                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                                        if (prefs.isContinuousChatVoiceEnabled && !isTtsSpeaking && !isGeneratingChat) {
                                            systemEngine?.startListening(continuous = false)
                                        }
                                    }, 500)
                                }
                            }
                        },
                        onError = { code, msg ->
                            android.util.Log.d("MYRA_Voice", "SpeechEngine status ($code): $msg")
                        }
                    )

                    externalCommandDispatcher = { cmd ->
                        currentTab = 1 // Switch to Chat tab
                        sendChatMessage(cmd)
                    }

                    externalVoiceStarter = {
                        currentTab = 2 // Switch to Voice tab
                        assistantState = AssistantState.LISTENING
                        liveTranscript = "Sun rahi hoon..."
                        val isLiveActive = liveEngine != null && !voiceStatus.fallbackActive
                        if (isLiveActive) {
                            audioEngine.startListening { pcmChunk ->
                                liveEngine?.sendAudioChunk(pcmChunk)
                            }
                        } else {
                            systemEngine?.startListening(continuous = true)
                        }
                    }

                    // Initialize Gemini Live Voice Engine if API key is present
                    if (prefs.hasConfiguredApiKey) {
                        try {
                            liveEngine = GeminiLiveVoiceEngine(
                                scope = lifecycleScope,
                                onTranscript = { transcript ->
                                    liveTranscript = transcript
                                },
                                onAudioLevel = { level ->
                                    audioLevel = level
                                },
                                onSpeakingStateChanged = { isSpeaking ->
                                    assistantState = if (isSpeaking) AssistantState.SPEAKING else AssistantState.IDLE
                                },
                                onStatusChanged = { connected, reason ->
                                    voiceStatus = voiceStatus.copy(
                                        fallbackActive = !connected,
                                        fallbackReason = reason
                                    )
                                }
                            )
                            val wsUrl = GeminiLiveVoiceEngine.buildLiveWebSocketUrl(prefs.geminiApiKey)
                            liveEngine?.connect(wsUrl, GeminiLiveVoiceEngine.LIVE_MODEL)
                        } catch (e: Exception) {
                            android.util.Log.w("MYRA_Voice", "Gemini Live voice engine init: ${e.message}")
                            voiceStatus = voiceStatus.copy(fallbackActive = true, fallbackReason = e.message)
                        }
                    } else {
                        voiceStatus = voiceStatus.copy(fallbackActive = true, fallbackReason = "API Key not configured")
                    }

                    // Check if launched by wake-word or lock screen trigger
                    handleIncomingVoiceTrigger(intent)

                    // Bridge audio RMS level from AudioEngine if actively capturing
                    lifecycleScope.launch {
                        audioEngine.audioRmsLevel.collectLatest { level ->
                            if (level > 0.01f && assistantState == AssistantState.LISTENING) {
                                audioLevel = level
                            }
                        }
                    }
                }

                if (showConnectScreen) {
                    com.myra.assistant.ui.screens.MyraConnectScreen(
                        onNavigateBack = { showConnectScreen = false }
                    )
                } else {
                    Scaffold(
                        bottomBar = {
                            NavigationBar(
                                containerColor = CardDark
                            ) {
                                NavigationBarItem(
                                    selected = currentTab == 0,
                                    onClick = { currentTab = 0 },
                                    icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                                    label = { Text("Home", fontSize = 11.sp, fontWeight = if (currentTab == 0) FontWeight.Bold else FontWeight.Normal) }
                                )
                                NavigationBarItem(
                                    selected = currentTab == 1,
                                    onClick = { currentTab = 1 },
                                    icon = { Icon(Icons.Default.Chat, contentDescription = "Chat") },
                                    label = { Text("Chat", fontSize = 11.sp, fontWeight = if (currentTab == 1) FontWeight.Bold else FontWeight.Normal) }
                                )
                                NavigationBarItem(
                                    selected = currentTab == 2,
                                    onClick = { currentTab = 2 },
                                    icon = { Icon(Icons.Default.Mic, contentDescription = "Voice") },
                                    label = { Text("Voice", fontSize = 11.sp, fontWeight = if (currentTab == 2) FontWeight.Bold else FontWeight.Normal) }
                                )
                                NavigationBarItem(
                                    selected = currentTab == 3,
                                    onClick = { currentTab = 3 },
                                    icon = { Icon(Icons.Default.Build, contentDescription = "Tools") },
                                    label = { Text("Tools", fontSize = 11.sp, fontWeight = if (currentTab == 3) FontWeight.Bold else FontWeight.Normal) }
                                )
                                NavigationBarItem(
                                    selected = currentTab == 4,
                                    onClick = { currentTab = 4 },
                                    icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                                    label = { Text("Settings", fontSize = 11.sp, fontWeight = if (currentTab == 4) FontWeight.Bold else FontWeight.Normal) }
                                )
                            }
                        }
                    ) { paddingValues ->
                        androidx.compose.foundation.layout.Box(modifier = Modifier.padding(paddingValues)) {
                            when (currentTab) {
                                0 -> HomeScreen(
                                    onNavigateToChat = { currentTab = 1 },
                                    onNavigateToVoice = { currentTab = 2 },
                                    onNavigateToTools = { currentTab = 3 },
                                    onNavigateToSettings = { currentTab = 4 },
                                    onStartVoiceListening = {
                                        currentTab = 2
                                        assistantState = AssistantState.LISTENING
                                        liveTranscript = "Sun rahi hoon..."
                                        val isLiveReady = liveEngine != null && !voiceStatus.fallbackActive
                                        if (isLiveReady) {
                                            audioEngine.startListening { pcmChunk ->
                                                liveEngine?.sendAudioChunk(pcmChunk)
                                            }
                                        } else {
                                            systemEngine?.startListening(continuous = true)
                                        }
                                    },
                                    isListening = assistantState == AssistantState.LISTENING,
                                    assistantState = assistantState,
                                    onOpenMenu = { showConnectScreen = true }
                                )
                                1 -> ChatScreen(
                                    messages = chatMessages,
                                    isGenerating = isGeneratingChat,
                                    isSpeaking = isTtsSpeaking,
                                    isListening = assistantState == AssistantState.LISTENING,
                                    activeModel = prefs.selectedGeminiModel,
                                    isVoiceResponseEnabled = prefs.isVoiceResponseInChatEnabled,
                                    isContinuousVoiceEnabled = prefs.isContinuousChatVoiceEnabled,
                                    onSendMessage = { text ->
                                        sendChatMessage(text)
                                    },
                                    onStopGeneration = {
                                        activeChatJob?.cancel()
                                        isGeneratingChat = false
                                        assistantState = AssistantState.IDLE
                                    },
                                    onRetryMessage = { failedMsg ->
                                        val idx = chatMessages.indexOf(failedMsg)
                                        val prevUserMsg = if (idx > 0 && chatMessages[idx - 1].sender == "user") {
                                            chatMessages[idx - 1].text
                                        } else {
                                            failedMsg.text
                                        }
                                        chatMessages.remove(failedMsg)
                                        sendChatMessage(prevUserMsg)
                                    },
                                    onStartVoiceInput = {
                                        assistantState = AssistantState.LISTENING
                                        liveTranscript = "Sun rahi hoon..."
                                        systemEngine?.startListening(continuous = false)
                                    },
                                    onStopVoiceInput = {
                                        assistantState = AssistantState.IDLE
                                        systemEngine?.stopListening()
                                    },
                                    onSpeakMessage = { text ->
                                        isTtsSpeaking = true
                                        systemEngine?.speak(text)
                                    },
                                    onStopSpeaking = {
                                        systemEngine?.stopSpeaking()
                                        isTtsSpeaking = false
                                        assistantState = AssistantState.IDLE
                                    },
                                    onNewChat = {
                                        activeChatJob?.cancel()
                                        isGeneratingChat = false
                                        chatMessages.clear()
                                        ChatConversationManager.clearHistory(this@MainActivity)
                                        val freshWelcome = ChatMessage(
                                            sender = "myra",
                                            text = "Namaste! New chat session started. Ask me anything in Hindi, Hinglish, or English!",
                                            category = "Welcome"
                                        )
                                        chatMessages.add(freshWelcome)
                                        ChatConversationManager.saveHistory(this@MainActivity, chatMessages)
                                    },
                                    onClearChatHistory = {
                                        activeChatJob?.cancel()
                                        isGeneratingChat = false
                                        chatMessages.clear()
                                        ChatConversationManager.clearHistory(this@MainActivity)
                                    },
                                    onToggleVoiceResponse = {
                                        prefs.isVoiceResponseInChatEnabled = !prefs.isVoiceResponseInChatEnabled
                                    },
                                    onToggleContinuousVoice = {
                                        prefs.isContinuousChatVoiceEnabled = !prefs.isContinuousChatVoiceEnabled
                                        if (!prefs.isContinuousChatVoiceEnabled) {
                                            systemEngine?.stopListening()
                                            assistantState = AssistantState.IDLE
                                        }
                                    },
                                    onNavigateBack = { currentTab = 0 }
                                )
                                2 -> MainVoiceScreen(
                                    assistantState = assistantState,
                                    voiceStatus = voiceStatus,
                                    audioLevel = audioLevel,
                                    liveTranscript = liveTranscript,
                                    assistantResponse = assistantResponse,
                                    onStartListening = {
                                        assistantState = AssistantState.LISTENING
                                        liveTranscript = "Sun rahi hoon..."
                                        val isLiveReady = liveEngine != null && !voiceStatus.fallbackActive
                                        if (isLiveReady) {
                                            audioEngine.startListening { pcmChunk ->
                                                liveEngine?.sendAudioChunk(pcmChunk)
                                            }
                                        } else {
                                            systemEngine?.startListening(continuous = true)
                                        }
                                    },
                                    onStopListening = {
                                        assistantState = AssistantState.IDLE
                                        audioEngine.stopListening()
                                        systemEngine?.stopAll()
                                        audioLevel = 0f
                                    },
                                    onSendCommand = { cmd ->
                                        dispatchUserCommand(cmd)
                                    },
                                    onOpenConnect = { showConnectScreen = true }
                                )
                                3 -> ToolsScreen(
                                    onExecuteCommand = { cmd ->
                                        dispatchUserCommand(cmd)
                                    },
                                    onOpenConnect = { showConnectScreen = true }
                                )
                                4 -> SettingsScreen(
                                    onOpenConnect = { showConnectScreen = true }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private fun handleCommand(text: String, onReply: (String) -> Unit) {
        val parsed = commandRouter.route(text)
        android.util.Log.d("MYRA_CMD", "VOICE_COMMAND: \"$text\"")
        android.util.Log.d("MYRA_ROUTE", "INTENT: ${parsed.intent}, TARGET: '${parsed.target}', PAYLOAD: '${parsed.payload}'")

        // 1. Speaker Voice Verification (Layer 1 Voice Biometrics)
        if (securityManager.isOwnerProtectionEnabled) {
            // Verify speaker identity against enrolled profile using real audio buffer
            val speechSamples = audioEngine.getLastSpeechBuffer()
            val verificationResult = securityManager.verifySpeaker(speechSamples)
            if (!verificationResult.isVerified) {
                android.util.Log.w("MYRA_Security", "Voice verification rejected: ${verificationResult.message}")
                onReply(verificationResult.message)
                return
            }
        }

        // 2. Android BiometricPrompt Enforcement for Sensitive Commands via BiometricManager
        // Enforces Owner Verification before sensitive voice commands reach the tool execution layer
        val isSensitive = biometricManager.checkIsSensitiveAction(parsed.intent, parsed.target, text)
        if (isSensitive && securityManager.isOwnerProtectionEnabled && securityManager.isSensitiveCommandsProtected) {
            if (biometricManager.isSessionValid()) {
                android.util.Log.i("MYRA_Security", "Sensitive command authorized via active BiometricManager session window.")
                executeResolvedCommand(parsed, text, onReply)
            } else {
                android.util.Log.i("MYRA_Security", "Sensitive voice command detected (\"$text\"). Intercepting with BiometricManager.")
                biometricManager.authenticate(
                    activity = this@MainActivity,
                    title = "MYRA Owner Verification",
                    subtitle = "संवेदनशील एक्शन के लिए बायोमेट्रिक प्रमाणीकरण आवश्यक है",
                    description = "कमांड: \"$text\" को निष्पादित करने के लिए अधिकृत ओनर की पहचान सत्यापित करें।",
                    commandText = text,
                    actionCategory = parsed.intent.name,
                    onSuccess = {
                        android.util.Log.i("MYRA_Security", "Owner Biometric verification succeeded for sensitive command: \"$text\"")
                        securityManager.recordBiometricSuccess()
                        executeResolvedCommand(parsed, text, onReply)
                    },
                    onError = { errorCode, errString ->
                        android.util.Log.w("MYRA_Security", "Owner verification failed/cancelled ($errorCode): $errString")
                        onReply("बायोमेट्रिक प्रमाणीकरण आवश्यक है: संवेदनशील एक्शन रद्द कर दिया गया।")
                    }
                )
            }
            return
        }

        // Non-sensitive commands (app launches, music, torch, battery, volume) execute directly
        executeResolvedCommand(parsed, text, onReply)
    }

    private fun executeResolvedCommand(parsed: ParsedCommand, text: String, onReply: (String) -> Unit) {
        // MYRA Connect: Route command to paired remote phone if detected
        if (parsed.intent == CommandIntent.EXECUTE_REMOTE_DEVICE || parsed.isRemote) {
            lifecycleScope.launch {
                val connectManager = com.myra.assistant.connect.MyraConnectManager.getInstance(this@MainActivity)
                val remoteResult = connectManager.executeRemoteVoiceCommand(
                    cleanedCommand = parsed.payload.ifBlank { text },
                    preferredDeviceName = parsed.remoteTargetDevice.ifBlank { null }
                )
                val replyMessage = when (remoteResult.status) {
                    com.myra.assistant.connect.RemoteResultStatus.SUCCESS -> {
                        "${remoteResult.message}।"
                    }
                    com.myra.assistant.connect.RemoteResultStatus.APP_NOT_INSTALLED -> {
                        "दूसरे फोन पर यह ऐप इंस्टॉल नहीं है।"
                    }
                    com.myra.assistant.connect.RemoteResultStatus.PERMISSION_REQUIRED -> {
                        "दूसरे फोन पर इस एक्शन की अनुमति नहीं है।"
                    }
                    com.myra.assistant.connect.RemoteResultStatus.NOT_CONNECTED -> {
                        "दूसरा फोन अभी connected नहीं है।"
                    }
                    else -> {
                        "दूसरे फोन पर कमांड execute नहीं हो पाया।"
                    }
                }
                android.util.Log.d("MYRA_REMOTE", "REMOTE_RESULT: ${remoteResult.status}, REPLY: \"$replyMessage\"")
                onReply(replyMessage)
            }
            return
        }

        val result = when (parsed.intent) {
            CommandIntent.EMERGENCY_SOS -> {
                // Emergency SOS directly executed once Owner Voice is verified without BiometricPrompt
                val emergencyCall = phoneControl.handleCallCommand("112")
                deviceControl.toggleFlashlight(true)
                com.myra.assistant.tools.CommandResult.success(
                    "इमरजेंसी SOS सक्रिय कर दिया गया है। 112 डायल किया जा रहा है और फ्लैशलाइट ऑन है।",
                    target = "emergency"
                )
            }
            CommandIntent.TOGGLE_TORCH -> {
                val state = parsed.payload == "on"
                deviceControl.toggleFlashlight(state)
            }
            CommandIntent.OPEN_APP -> {
                appLauncher.launchAppByName(parsed.target)
            }
            CommandIntent.PLAY_MUSIC -> {
                appLauncher.launchMusicPlayer()
            }
            CommandIntent.CALL_PHONE -> {
                phoneControl.handleCallCommand(parsed.target)
            }
            CommandIntent.SEND_WHATSAPP -> {
                whatsAppAutomation.handleWhatsAppCommand(
                    target = parsed.target,
                    messageText = parsed.payload,
                    autoSend = false,
                    onAsyncResult = { asyncRes ->
                        onReply(asyncRes.message)
                    }
                )
            }
            CommandIntent.CHECK_BATTERY -> {
                deviceControl.getBatteryPercentage()
            }
            CommandIntent.LOCK_DEVICE -> {
                deviceControl.lockScreen()
            }
            CommandIntent.ADJUST_VOLUME -> {
                val level = parsed.payload.toIntOrNull() ?: 80
                deviceControl.setVolume(level)
            }
            CommandIntent.SYSTEM_DIAGNOSTICS -> {
                val batteryResult = deviceControl.getBatteryPercentage()
                val missingPerms = permissionManager.checkMissingPermissions()
                val statusText = "MYRA सिस्टम सक्रिय है। ${batteryResult.message} अनुमतियाँ बाकी: ${missingPerms.size}।"
                com.myra.assistant.tools.CommandResult.success(statusText, target = "diagnostics")
            }
            CommandIntent.EXECUTE_REMOTE_DEVICE -> {
                // Handled prior to local dispatch; fallback response if reached synchronously
                com.myra.assistant.tools.CommandResult.success("दूसरे फोन पर कमांड भेज दी गई है।", target = "remote_device")
            }
            CommandIntent.CONVERSATIONAL_CHAT -> {
                com.myra.assistant.tools.CommandResult.success(parsed.payload, target = "conversation")
            }
            CommandIntent.UNKNOWN_LLM_FALLBACK -> {
                val prefs = com.myra.assistant.data.PreferencesManager(this@MainActivity)
                if (prefs.hasConfiguredApiKey) {
                    lifecycleScope.launch {
                        val res = com.myra.assistant.data.GeminiApiClient.generateResponse(
                            apiKey = prefs.geminiApiKey,
                            model = prefs.selectedGeminiModel,
                            userPrompt = text
                        )
                        res.onSuccess { aiReply ->
                            android.util.Log.i("MYRA_Gemini", "Received response from custom Gemini model: ${prefs.selectedGeminiModel}")
                            onReply(aiReply)
                        }.onFailure { err ->
                            android.util.Log.w("MYRA_Gemini", "Custom Gemini API call failed: ${err.message}")
                            onReply("माफ़ कीजिए, मुझे यह कमांड समझ नहीं आई: \"$text\"। क्या आप दोबारा बोल सकते हैं?")
                        }
                    }
                    return
                } else {
                    android.util.Log.w("MYRA_ROUTE", "Unrecognized voice command: \"$text\"")
                    com.myra.assistant.tools.CommandResult.unsupported(
                        "मैं MYRA हूँ। आपने पूछा: \"$text\"। विस्तृत AI उत्तर के लिए सेटिंग्स में अपनी Google Gemini API Key दर्ज करें, या मुझसे कॉल, वॉट्सऐप, टॉर्च और म्यूज़िक चलाने को कहें!",
                        target = "fallback"
                    )
                }
            }
        }

        android.util.Log.d("MYRA_RESULT", "STATUS: ${result.status}, SUCCESS: ${result.success}, MESSAGE: \"${result.message}\"")
        onReply(result.message)
    }

    fun openFullSettingsActivity() {
        val intent = Intent(this, SettingsActivity::class.java)
        startActivity(intent)
    }

    private fun checkAndRequestPermissions() {
        val missing = permissionManager.checkMissingPermissions()
        if (missing.isNotEmpty()) {
            requestPermissionLauncher.launch(missing.toTypedArray())
        }
    }

    private fun startWakeWordIfPermitted() {
        val prefs = com.myra.assistant.data.PreferencesManager(this)
        val micGranted = androidx.core.content.ContextCompat.checkSelfPermission(
            this, android.Manifest.permission.RECORD_AUDIO
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        if (prefs.isHandsFreeWakeWordEnabled && micGranted) {
            com.myra.assistant.services.MyraWakeWordService.start(this)
        } else if (!micGranted) {
            android.util.Log.i("MYRA_Voice", "Wake-word listener is waiting for microphone permission.")
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 4102) {
            val prefs = com.myra.assistant.data.PreferencesManager(this)
            val granted = grantResults.isNotEmpty() &&
                grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED
            if (granted) {
                startWakeWordIfPermitted()
            } else {
                prefs.isHandsFreeWakeWordEnabled = false
                android.util.Log.i("MYRA_Voice", "Hands-free remains disabled because microphone permission was denied.")
            }
        }
    }

    override fun onResume() {
        super.onResume()
        android.util.Log.d("MYRA_Lifecycle", "MainActivity onResume: biometric session valid = ${biometricManager.isSessionValid()}")
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingVoiceTrigger(intent)
    }

    private fun handleIncomingVoiceTrigger(intent: Intent?) {
        if (intent == null) return
        val isAutoVoice = intent.getBooleanExtra(com.myra.assistant.services.MyraWakeWordService.EXTRA_AUTO_START_VOICE, false)
        if (isAutoVoice) {
            val followUp = intent.getStringExtra(com.myra.assistant.services.MyraWakeWordService.EXTRA_FOLLOW_UP_COMMAND)
            if (!followUp.isNullOrBlank()) {
                android.util.Log.i("MYRA_Voice", "Auto-dispatching follow-up wake command: '$followUp'")
                externalCommandDispatcher?.invoke(followUp)
            } else {
                android.util.Log.i("MYRA_Voice", "Wake word triggered; starting voice session")
                externalVoiceStarter?.invoke()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        liveEngine?.disconnect()
        biometricManager.cancelAuthentication()
        securityManager.cancelActiveBiometricPrompt()
        audioEngine.release()
        systemEngine?.destroy()
    }
}
