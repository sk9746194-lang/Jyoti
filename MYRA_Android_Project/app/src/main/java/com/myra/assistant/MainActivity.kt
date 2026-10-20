package com.myra.assistant

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.myra.assistant.data.AssistantState
import com.myra.assistant.data.VoiceStatus
import com.myra.assistant.security.OwnerSecurityManager
import com.myra.assistant.tools.AppLauncher
import com.myra.assistant.tools.CommandIntent
import com.myra.assistant.tools.DeviceControlManager
import com.myra.assistant.tools.FastCommandRouter
import com.myra.assistant.tools.ParsedCommand
import com.myra.assistant.tools.PermissionManager
import com.myra.assistant.tools.PhoneCallManager
import com.myra.assistant.tools.WhatsAppAutomation
import com.myra.assistant.ui.screens.MainVoiceScreen
import com.myra.assistant.ui.screens.SettingsScreen
import com.myra.assistant.ui.screens.ToolsScreen
import com.myra.assistant.ui.theme.CardDark
import com.myra.assistant.ui.theme.MyraAssistantTheme
import com.myra.assistant.voice.AudioEngine
import com.myra.assistant.voice.GeminiLiveVoiceEngine
import com.myra.assistant.voice.SystemSpeechEngine
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

    private var liveEngine: GeminiLiveVoiceEngine? = null
    private var systemEngine: SystemSpeechEngine? = null

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // Permissions handled
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

        checkAndRequestPermissions()

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

                LaunchedEffect(Unit) {
                    systemEngine = SystemSpeechEngine(
                        context = this@MainActivity,
                        onResult = { text ->
                            liveTranscript = text
                            assistantState = AssistantState.THINKING
                            handleCommand(text) { reply ->
                                assistantResponse = reply
                                assistantState = AssistantState.SPEAKING
                                deviceControl.triggerHapticFeedback("speech_start")
                                systemEngine?.speak(reply)
                            }
                        },
                        onPartial = { partial ->
                            liveTranscript = partial
                            assistantState = AssistantState.LISTENING
                        }
                    )

                    // Collect real-time audio RMS level for orb & waveform visualizer
                    lifecycleScope.launch {
                        audioEngine.audioRmsLevel.collectLatest { level ->
                            audioLevel = level
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
                                    icon = { Icon(Icons.Default.Mic, contentDescription = "Voice") },
                                    label = { Text("Assistant") }
                                )
                                NavigationBarItem(
                                    selected = currentTab == 1,
                                    onClick = { currentTab = 1 },
                                    icon = { Icon(Icons.Default.Build, contentDescription = "Tools") },
                                    label = { Text("Tools") }
                                )
                                NavigationBarItem(
                                    selected = currentTab == 2,
                                    onClick = { currentTab = 2 },
                                    icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                                    label = { Text("Settings") }
                                )
                            }
                        }
                    ) { paddingValues ->
                        androidx.compose.foundation.layout.Box(modifier = Modifier.padding(paddingValues)) {
                            when (currentTab) {
                                0 -> MainVoiceScreen(
                                    assistantState = assistantState,
                                    voiceStatus = voiceStatus,
                                    audioLevel = audioLevel,
                                    liveTranscript = liveTranscript,
                                    assistantResponse = assistantResponse,
                                    onStartListening = {
                                        assistantState = AssistantState.LISTENING
                                        systemEngine?.startListening()
                                        audioEngine.startListening { chunk ->
                                            liveEngine?.sendAudioChunk(chunk)
                                        }
                                    },
                                    onStopListening = {
                                        assistantState = AssistantState.IDLE
                                        systemEngine?.stopListening()
                                        audioEngine.stopListening()
                                    },
                                    onSendCommand = { cmd ->
                                        liveTranscript = cmd
                                        assistantState = AssistantState.THINKING
                                        deviceControl.triggerHapticFeedback("command_confirm")
                                        handleCommand(cmd) { reply ->
                                            assistantResponse = reply
                                            assistantState = AssistantState.SPEAKING
                                            deviceControl.triggerHapticFeedback("speech_start")
                                            systemEngine?.speak(reply)
                                        }
                                    },
                                    onOpenConnect = { showConnectScreen = true }
                                )
                                1 -> ToolsScreen(
                                    onExecuteCommand = { cmd ->
                                        deviceControl.triggerHapticFeedback("command_confirm")
                                        handleCommand(cmd) { reply ->
                                            assistantResponse = reply
                                            assistantState = AssistantState.SPEAKING
                                            deviceControl.triggerHapticFeedback("speech_start")
                                            systemEngine?.speak(reply)
                                        }
                                    },
                                    onOpenConnect = { showConnectScreen = true }
                                )
                                2 -> SettingsScreen(
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

        // 3. OWNER VOICE ONLY VERIFICATION (Requirement 7 & 8)
        // Commands execute upon Owner Voice verification alone without Fingerprint/Face/PIN
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

        // Voice-only command authorization: Execute command directly once Owner Voice is verified
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
                whatsAppAutomation.handleWhatsAppCommand(parsed.target)
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
            CommandIntent.UNKNOWN_LLM_FALLBACK -> {
                // Honest response - never fake that an action succeeded
                android.util.Log.w("MYRA_ROUTE", "Unrecognized voice command: \"$text\"")
                com.myra.assistant.tools.CommandResult.unsupported(
                    "माफ़ कीजिए, मुझे यह कमांड समझ नहीं आई: \"$text\"। क्या आप दोबारा बोल सकते हैं?",
                    target = "fallback"
                )
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

    override fun onDestroy() {
        super.onDestroy()
        audioEngine.release()
        systemEngine?.destroy()
    }
}
