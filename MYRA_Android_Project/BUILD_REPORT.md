# MYRA Android Project — Source Audit Report

**Package:** `com.myra.assistant`  
**Minimum SDK:** 26 (Android 8.0)  
**Target / Compile SDK:** 34  
**Toolchain configured:** Gradle 8.4, AGP 8.3.0, Kotlin 1.9.23, JDK 17  
**Native source inventory:** 61 Kotlin files, 13 XML files, 90 project/configuration files in the cleaned native project.

## Changes applied in this audit

- Standardized native Gemini Live on `models/gemini-3.8-live` and `Aoede`.
- Aligned the secondary Live session manager and UI default voice with the same model and voice.
- Wait for Google's `setupComplete` response before reporting Live as connected or sending Live audio/text.
- Build the WebSocket URL with encoded query parameters instead of string-concatenating the API key.
- Migrate the user-entered Gemini API key into AndroidX Security Crypto encrypted preferences when Android Keystore storage is available.
- Make hands-free wake listening opt-in by default and check runtime microphone permission before starting the microphone foreground service.
- Do not start a microphone foreground service from boot broadcasts; open MYRA to resume listening after a restart.
- Remove duplicate microphone-service startup and overly broad one-word wake triggers.
- Correct repository-level GitHub Actions and Codemagic paths for the nested native project.
- Fixed an extra closing brace in `HomeScreen.kt` found during whole-source Kotlin parser validation.
- Remove machine-specific SDK paths and temporary SDK/Gradle caches from the deliverable.

## Verification performed

- Original source ZIP passed archive integrity validation before editing.
- Android manifest and XML resource files were parsed for well-formed XML.
- Manifest component/resource references were checked against source files.
- Kotlin source was passed through the Kotlin compiler parser; dependency-resolution errors are expected in this environment because Android/OkHttp/AndroidX classpaths are not installed. No Kotlin syntax `expecting` errors were reported in the inspected changed files.
- Final ZIP integrity and extraction checks are recorded in the accompanying audit report.

## Build status — not verified

A full APK build could not be run here. `./gradlew :app:assembleDebug` attempted to download Gradle 8.4, but DNS/network access to `services.gradle.org` was unavailable. The bundled SDK snapshot did not contain Android build-tools, so SDK caches are not shipped. Do not treat this package as a build-verified APK.

## Device tests still required

- Actual Aoede playback and Gemini Live setup with a valid API key.
- Hindi/Hinglish recognition and natural interruption behavior.
- Bluetooth headset/car routing.
- Wake-word stability, battery impact, and lock-screen behavior on the target Android device.
- Android 14+ foreground-service permission and notification behavior.

## Security note

Encrypted local storage protects a user's API key at rest where Android Keystore-backed encryption works. The application still uses a user-supplied key for direct Gemini Live API access. A public multi-user release should use a trusted backend to issue short-lived credentials instead of shipping a shared API key in an APK.
