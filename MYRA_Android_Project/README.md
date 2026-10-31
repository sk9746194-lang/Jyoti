# MYRA — 2026 Edition Hindi AI Voice Assistant (Android)

<div align="center">
  <h3>Namaste! Main MYRA hoon — 2026 Edition Voice-First AI Assistant for Android</h3>
  <p>Ultra low-latency duplex voice assistant designed for Hindi and Hinglish speakers with Gemini Live 24kHz audio, offline fast-command routing, Android hardware controls, lock-screen overlay, and accessibility automation.</p>
</div>

---

## 🚀 Features

- **Gemini Live AI Voice Architecture**: Full-duplex Gemini Live audio streaming (16kHz PCM input / 24kHz PCM output) via WebSocket (`GeminiLiveVoiceEngine`), using the `gemini-3.8-live` model and Aoede voice. Connection state waits for Google's setup confirmation.
- **Natural Hindi / Hinglish Speech**: Fallback to Android TTS (`SystemSpeechEngine`); actual voice quality depends on the installed Android speech engine.
- **Fast Offline Hardware Router (`FastCommandRouter`)**: Regex & keyword matching for `<50ms` execution of device actions:
  - Flashlight Torch toggle
  - Battery charge level
  - Media volume control
  - Device lock & overlay
  - Direct Phone call via SIM
  - WhatsApp quick message
  - App launcher (Chrome, YouTube, Camera, WhatsApp, Spotify, Settings)
- **Voice Interaction Service**: Android digital-assistant integration (`VoiceInteractionService`, `VoiceInteractionSessionService`, `RecognitionService`), subject to the user's system-level assistant selection and device support.
- **Lock Screen Overlay**: The activity can request lock-screen display with `showWhenLocked` / `turnScreenOn`; actual behavior depends on device security settings and Android version.
- **Bluetooth SCO**: Hands-free voice recognition routing support for compatible car Bluetooth devices and earbuds (`BluetoothAudioManager`); behavior varies by Android version and headset.
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
├── codemagic.yaml                           # Repository-level Codemagic config (in source bundle)
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
cd MYRA_Android_Project
# Copy local.properties.example to local.properties and set sdk.dir if needed
chmod +x gradlew
./gradlew :app:assembleDebug
```

### Expected APK Output Path
```text
MYRA_Android_Project/app/build/outputs/apk/debug/app-debug.apk
```

### How to Open in Android Studio / AndroidIDE:
1. Open Android Studio (Iguana / Jellyfish / Ladybug or newer) or AndroidIDE on mobile.
2. Select **File -> Open...** and choose the `MYRA_Android_Project` folder.
3. Configure **JDK 17** in `File -> Settings -> Build, Execution, Deployment -> Build Tools -> Gradle`.
4. Wait for Gradle sync to complete.
5. Click **Run 'app'** or execute **Build -> Build Bundle(s) / APK(s) -> Build APK(s)**.
6. Install the resulting `app-debug.apk` on your Android device (Android 8.0+ / API 26+).



## Important Android behavior and security notes

- Hands-free wake-word listening is **off by default** and requires the user to enable it and grant microphone permission. Android does not allow a microphone foreground service to be silently restarted from a boot broadcast on modern Android versions; open MYRA to resume listening after reboot.
- The Gemini API key entered by the user is migrated to Android Keystore-backed encrypted preferences when available. For a public production release, use a trusted backend to issue short-lived Gemini Live credentials rather than distributing a shared API key in the APK.
- Wake-word detection currently uses Android `SpeechRecognizer`; it is not a dedicated always-on offline hotword engine. Background availability, battery behavior, and lock-screen behavior vary by device and Android version and must be tested on the target phone.
- A successful source audit is not the same as a successful APK build. Run the included GitHub Actions workflow or build locally with JDK 17 and Android SDK platform/build-tools installed.


## Source audit status

See `AUDIT_FIXES.md` and `BUILD_REPORT.md` for the exact source changes and limitations. The APK has **not** been build-verified in this environment because Gradle could not be downloaded. Aoede playback and Android behavior must be tested on a real device.
