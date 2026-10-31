# MYRA source audit and fixes

## Changes in this package
- Standardized both native Gemini Live code paths on `models/gemini-3.8-live` and the Aoede voice.
- Changed the UI voice status default to Aoede.
- Gemini Live status now becomes connected only after Google's `setupComplete` message; audio/text sends are held until setup is complete.
- Migrated stored user Gemini API keys to AndroidX Security Crypto encrypted preferences when the Android Keystore is available, while retaining a compatibility fallback if encrypted storage initialization fails.
- Disabled hands-free wake listening by default; service startup checks `RECORD_AUDIO` permission.
- Removed automatic microphone foreground-service startup from boot broadcasts and removed the boot receiver registration. The user must open MYRA to resume wake-word listening after reboot.
- Avoided starting a second microphone foreground service from `MyraForegroundService`.
- Removed overly broad one-word wake-word triggers that could cause false activations.
- Corrected GitHub Actions to build from the actual `MYRA_Android_Project` directory and removed the duplicate workflow.
- Updated the web preview default chat model to Gemini 3.8 Flash and corrected API-key security messaging: web-entered keys remain in browser `localStorage` (not encrypted), while the native Android app uses encrypted preferences when available.
- Removed machine-specific `local.properties` and SDK/Gradle caches from the distributable source package.
- Fixed an extra closing brace at the end of `HomeScreen.kt` that caused a Kotlin parser error.

## Not verified in this environment
- Gradle could not download its distribution because this environment has no DNS/network access to `services.gradle.org`; therefore no APK build result is claimed.
- The included SDK snapshot did not contain Android build-tools, so it is excluded from the project ZIP.
- Aoede playback, Hindi/Hinglish recognition, Bluetooth routing, wake-word reliability, lock-screen behavior, and Android 14+ foreground-service behavior require real-device tests.
- The app still uses a user-supplied Gemini API key for direct Live API access. Encrypted local storage protects the key at rest on supported devices, but a production multi-user release should use a backend-issued ephemeral token.
- Wake-word listening uses Android SpeechRecognizer, not an offline hotword model; Android/device vendor restrictions may suspend background recognition.
