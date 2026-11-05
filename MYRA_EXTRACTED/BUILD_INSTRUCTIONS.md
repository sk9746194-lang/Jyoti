# MYRA AI Voice Assistant — Build & Deployment Instructions

This document provides exact, reproducible steps to build the debug and release APKs for both the **MYRA Main Assistant** and the **MYRA Companion App**, as well as how to run the companion backend server.

---

## Prerequisites
- **JDK**: Java Development Kit 17 (Temurin 17 or OpenJDK 17 recommended)
- **Android SDK**: Build Tools 34.0.0, Platform API 34 & 35
- **Node.js**: v18+ (for the companion API server)

---

## 1. Building MYRA Main Application (`MYRA_Android_Project`)

```bash
cd MYRA_Android_Project

# Make gradlew executable
chmod +x gradlew

# Run clean and assemble debug APK
./gradlew :app:clean :app:assembleDebug --stacktrace
```

The compiled APK will be generated at:
`MYRA_Android_Project/app/build/outputs/apk/debug/app-debug.apk`

---

## 2. Building MYRA Companion Application (`MYRA_Companion_Project`)

```bash
cd MYRA_Companion_Project

# Make gradlew executable
chmod +x gradlew

# Run clean and assemble debug APK
./gradlew :app:clean :app:assembleDebug --stacktrace
```

The compiled APK will be generated at:
`MYRA_Companion_Project/app/build/outputs/apk/debug/app-debug.apk`

---

## 3. Running via GitHub Actions (Zero Local Setup)

Both projects include automated GitHub Actions workflows under `.github/workflows/`.
When you push the codebase to a GitHub repository:
1. Navigate to **Actions** in your GitHub repo.
2. Select **Build MYRA Android APK**.
3. Click **Run workflow**.
4. Once completed (approx. 2-3 minutes), download the generated `MYRA-Debug-APK` artifact from the run summary.

---

## 4. Running the Companion Gateway Server

```bash
# In the project root or server folder
npm install
npm run dev
```

The server starts on port `3000` and provides:
- Gemini API Live streaming gateway (`/api/live/ws`)
- Chat completions endpoint (`/api/chat/completions`)
- Direct ZIP download endpoint (`/MYRA_COMPLETE_FINAL_PROJECT.zip`)
