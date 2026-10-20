# MYRA — 2026 Edition Hindi AI Voice Assistant (Android)

<div align="center">
  <h3>Namaste! Main MYRA hoon — 2026 Edition Voice-First AI Assistant for Android</h3>
  <p>Ultra low-latency duplex voice assistant designed for Hindi and Hinglish speakers with Gemini Live 24kHz audio, offline fast-command routing, Android hardware controls, lock-screen overlay, and accessibility automation.</p>
</div>

---

## 🚀 Features

- **Gemini Live AI Voice Architecture**: Full duplex 24kHz PCM audio streaming via secure WebSocket (`GeminiLiveVoiceEngine`).
- **Natural Hindi / Hinglish Speech**: Fallback to Android TTS (`SystemSpeechEngine`) with high-quality Hindi voice synthesizer.
- **Fast Offline Hardware Router (`FastCommandRouter`)**: Regex & keyword matching for `<50ms` execution of device actions:
  - Flashlight Torch toggle
  - Battery charge level
  - Media volume control
  - Device lock & overlay
  - Direct Phone call via SIM
  - WhatsApp quick message
  - App launcher (Chrome, YouTube, Camera, WhatsApp, Spotify, Settings)
- **Voice Interaction Service**: Default digital assistant integration (`VoiceInteractionService`, `VoiceInteractionSessionService`, `RecognitionService`) for home button / power button long-press invocation.
- **Lock Screen Overlay**: Show over lockscreen with `showWhenLocked = true` and `turnScreenOn = true`.
- **Bluetooth SCO**: Hands-free voice recognition with car Bluetooth & wireless earbuds (`BluetoothAudioManager`).
- **Interactive Audio Orb & Waveform**: Jetpack Compose real-time audio RMS reactive quantum pulsing orb and sinusoidal visualizer.

---

## 🛠️ Project Structure

```text
MYRA_Android_Project/
├── app/
│   ├── build.gradle.kts                     # AGP 8.3.0, Compose BOM 2024.02.02, Kotlin 1.9.23
│   ├── proguard-rules.pro                   # ProGuard rules for OkHttp & coroutines
│   └── src/main/
│       ├── AndroidManifest.xml              # Permissions, Activities & Services
│       ├── java/com/myra/assistant/
│       │   ├── MainActivity.kt              # Main voice hub & scaffold
│       │   ├── SettingsActivity.kt          # Standalone settings activity
│       │   ├── MyraApplication.kt           # Application class & init
│       │   ├── data/
│       │   │   ├── MyraModels.kt            # Enums & data classes
│       │   │   └── PreferencesManager.kt    # SharedPreferences wrapper
│       │   ├── tools/
│       │   │   ├── FastCommandRouter.kt     # Regex Hindi/Hinglish offline parser
│       │   │   ├── AppLauncher.kt           # App & Camera/Browser intent launcher
│       │   │   ├── DeviceControlManager.kt  # Torch, Battery, Volume, Lock
│       │   │   ├── PhoneCallManager.kt      # Direct phone & SMS
│       │   │   ├── WhatsAppAutomation.kt    # WhatsApp intent automation
│       │   │   └── PermissionManager.kt     # Runtime permission checker
│       │   ├── voice/
│       │   │   ├── AudioEngine.kt           # Audio pipeline orchestrator
│       │   │   ├── AudioRecorder.kt         # 16kHz PCM AudioRecord with RMS calculation
│       │   │   ├── AudioTrackPlayer.kt      # 24kHz low-latency streaming AudioTrack
│       │   │   ├── BluetoothAudioManager.kt # Bluetooth SCO routing
│       │   │   ├── GeminiLiveSessionManager.kt # WebSocket duplex connection
│       │   │   ├── GeminiLiveVoiceEngine.kt # Live bidirectional voice loop
│       │   │   └── SystemSpeechEngine.kt    # Android SpeechRecognizer & TextToSpeech
│       │   ├── services/
│       │   │   ├── MyraForegroundService.kt # Foreground notification service
│       │   │   ├── MyraVoiceInteractionService.kt # Digital Assistant binding
│       │   │   ├── MyraVoiceInteractionSessionService.kt # Assistant session
│       │   │   ├── MyraRecognitionService.kt # Speech recognition service
│       │   │   ├── MyraAccessibilityService.kt # Accessibility gestures
│       │   │   └── MyraWakeWordService.kt   # Hotword detection service
│       │   └── ui/
│       │       ├── screens/
│       │       │   ├── MainVoiceScreen.kt   # Main Voice UI & Animated Orb
│       │       │   ├── ToolsScreen.kt       # Daily Goals & Popular Commands
│       │       │   └── SettingsScreen.kt    # Voice & Bluetooth settings
│       │       ├── components/
│       │       │   ├── AnimatedOrb.kt       # Pulse & quantum nodes Canvas Orb
│       │       │   └── WaveformVisualizer.kt # Real-time wave visualizer
│       │       ├── LockScreenOverlayActivity.kt
│       │       └── theme/ (Color.kt, Theme.kt, Type.kt)
│       └── res/ (drawable/, values/, xml/)
├── gradle/wrapper/
│   ├── gradle-wrapper.jar
│   └── gradle-wrapper.properties            # Gradle 8.4
├── .github/workflows/android.yml            # GitHub Actions CI build
├── codemagic.yaml                           # Codemagic CI build
├── build.gradle.kts                         # Root Gradle config
├── settings.gradle.kts                      # Dependency repositories
├── gradle.properties                        # JVM args & AndroidX
├── gradlew & gradlew.bat                    # Executable Gradle wrappers
└── README.md
```

---

## 📱 Build Instructions

### Package Name
`com.myra.assistant`

### How to Build with Gradle (Terminal / AndroidIDE)
```bash
chmod +x gradlew
./gradlew assembleDebug
```

### Expected APK Output Path
```text
app/build/outputs/apk/debug/app-debug.apk
```

### How to Open in Android Studio / AndroidIDE:
1. Open Android Studio (Iguana / Jellyfish / Ladybug or newer) or AndroidIDE on mobile.
2. Select **File -> Open...** and choose the `MYRA_Android_Project` folder.
3. Configure **JDK 17** in `File -> Settings -> Build, Execution, Deployment -> Build Tools -> Gradle`.
4. Wait for Gradle sync to complete.
5. Click **Run 'app'** or execute **Build -> Build Bundle(s) / APK(s) -> Build APK(s)**.
6. Install the resulting `app-debug.apk` on your Android device (Android 8.0 - Android 14+ / API 26-34).

