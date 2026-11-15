# MYRA Independent Code Audit — 15 November 2026

## Scope and verification

- Repository: `sk9746194-lang/Jyoti`, branch `main`.
- Reviewed the latest patched source snapshot from GitHub Actions.
- Main Android app: 92 Kotlin files, approximately 26,182 Kotlin source lines.
- Companion Android app: 5 Kotlin files, approximately 1,093 Kotlin source lines.
- Also inspected the Android manifest/network-security config, Gradle dependencies, backend TypeScript server, and existing audit/feature claims.
- GitHub Actions run `43714710717` completed successfully, including the Main APK build. This proves compilation/package generation only; it does not prove device behavior or security.
- No Android unit-test or instrumentation-test source directories were found. A local Gradle build could not be run in this environment because the Gradle distribution host was unreachable.

## Critical findings

### 1. Sensitive-action confirmation is not enforced on every execution path

- `MainActivity.handleCommand()` invokes biometric checks only when owner protection is enabled. Owner protection defaults to disabled in `OwnerSecurityManager`.
- Gemini function calls reach `executeFunctionCallOnAndroid()` directly. The `call_phone` branch calls `PhoneCallManager.handleCallCommand()` without a separate confirmation step.
- The Live voice function-call callback uses the same executor, so it has the same gap.
- `LockScreenOverlayActivity` directly handles CALL_PHONE and EMERGENCY_SOS; it does not route through the MainActivity security gate.
- `ActionBrain` is not referenced by the active MainActivity dispatch path (it is currently an unused/parallel safety layer).
- Result: safety depends on which UI/voice path triggered the command. A single central authorization gate is needed.

### 2. SOS can report success without verifying the result

- In `MainActivity.executeResolvedCommand()`, the SOS branch calls the 112 call manager and flashlight manager but ignores both returned results.
- It then unconditionally returns a success message saying SOS was activated.
- `LockScreenOverlayActivity` also announces SOS success after issuing the calls without checking the returned result.
- The UI must report each action's actual outcome and must not imply that an emergency call connected merely because an Intent was launched.

### 3. Remote-device socket endpoints do not enforce the received auth header consistently

- `ConnectionTransport.handleClientSocket()` reads `Authorization` / `X-Auth-Token` into `authTokenHeader`, but the header is not checked for `/screen/frame`, `/gesture`, and `/file/*` routes.
- `/command` has a separate body-token check, but the other privileged endpoints proceed without the same paired-device check.
- The Companion server (`CompanionHotspotServer.kt`) similarly checks the token in `/command`, but its `/gesture` and `/file/*` handlers do not validate `authTokenHeader`. Its command handler also permits commands while the authorized token is blank.
- Both socket servers bind a plain HTTP/TCP listener. This exposes control tokens and traffic to local-network interception. The file handlers also need request-size, transfer-size, and socket-timeout limits.
- The Android network security config allows cleartext using CIDR-like strings (`192.168.0.0/16`, `10.0.0.0/8`) inside `<domain>`; Android domain rules are hostname matches, not subnet ACLs. The current entries do not reliably authorize dynamic private IP addresses while the base policy denies cleartext.

### 4. API-key storage has two conflicting implementations

- Settings saves the Gemini key through `PreferencesManager.geminiApiKey` into ordinary `myra_prefs` SharedPreferences.
- The active provider pipeline (`GeminiDirectProvider` / `AIProviderManager`) reads from `SecureKeyStorage`'s encrypted vault.
- A repository-wide search found `SecureKeyStorage.saveGeminiApiKey()` only at its declaration; the Settings save path does not call it.
- Result: direct MainActivity calls can see the key while the central provider/fallback path may see an empty secure vault. This is a split-brain configuration bug, and the ordinary preference copy is not encrypted at rest.

### 5. The microphone lifecycle can start two recognizers at once

- `MainActivity.onResume()` pauses the Wake Word service, but `onStop()` unconditionally resumes it.
- Voice capture is not necessarily stopped before `onStop()`; the app may still have an active `AudioEngine` or `SystemSpeechEngine` session.
- `MyraWakeWordService` and `SystemSpeechEngine` both use Android `SpeechRecognizer` restart loops. Backoff reduces rapid reopen frequency but does not make the wake listener continuous or guarantee single ownership.
- The current wake implementation is SpeechRecognizer phrase matching, not a dedicated local neural wake-word detector.

### 6. Gemini Live endpoint version is stale in the direct Android client

- `GeminiLiveVoiceEngine.LIVE_WS_ENDPOINT_TEMPLATE`, `MainActivity`, and `GeminiLiveSessionManager` use the `v1alpha` WebSocket endpoint.
- Google's current Live API WebSocket reference documents the `v1beta` endpoint. This mismatch should be corrected and tested with a real session.
- The current direct-client API-key query parameter is accepted by Google's WebSocket guide, but Google recommends ephemeral tokens for client-side Live API connections to reduce the risk of exposing a long-lived key in a mobile app.

### 7. ActionBrain can manufacture success for actions it does not execute

- `ActionBrain.executeAction()` has a generic `else` branch that returns `VERIFIED_SUCCESS` for any intent not explicitly implemented.
- SMS, remote-device, and lock-device intents are not all implemented in that executor.
- The WhatsApp branch requests `autoSend = true`, which is unsafe unless an explicit user confirmation is guaranteed at the point of execution.
- Although ActionBrain is not currently the active dispatch path, leaving this behavior in place is dangerous if it is wired in later.

### 8. Backend local fallback returns fabricated device/private information

- `server/server.ts` has a `parseLocally()` fallback used when no Gemini key is configured.
- It returns hard-coded sample values, including battery 84%, a fixed OTP (482910), a parking location, a location in Noida, and synthetic system-health values.
- These are not readings from the Android device and must never be presented as real user/device data.

### 9. Backend public endpoints have no visible authentication or rate-limiting middleware

- The server exposes chat, TTS, key validation, and Live WebSocket routes.
- No authentication middleware, `express-rate-limit`, or equivalent per-client request limiter was found in `server/server.ts`.
- If this backend is deployed publicly with `GEMINI_API_KEY` configured, unauthenticated callers may consume the project's quota.

### 10. Existing feature/audit documentation overstates verification

- `MYRA_FINAL_AUDIT_REPORT.md` labels many features “Tested Successfully” and describes the wake detector as acoustic energy/pattern matching.
- The source currently shows Android SpeechRecognizer phrase matching and several unverified or incomplete paths described above.
- Documentation should separate: implemented in source, compiled in CI, tested on a physical device, and independently security-verified.

## Additional engineering risks

- `PermissionManager` requests multiple unrelated permissions at app startup, while the permission-result callback in `MainActivity` does not process grant/denial outcomes. Permissions should be requested just-in-time for the feature.
- Main app and Companion app both use `compileSdk = 34` and `targetSdk = 34`; Android 15 behavior and newer platform requirements need a deliberate compatibility pass.
- The Main APK build has no corresponding automated behavior/security test suite in the repository.
- There are parallel implementations of Gemini request handling and Live session handling. The split paths make it easy for fixes, credentials, and safety checks to differ.

## Recommended repair order

1. Enforce one central confirmation + permission + authorization gate for all sensitive actions, including chat function calls, Live function calls, lock-screen commands, and remote commands.
2. Fix SOS result reporting and remove all hard-coded/mock private or device values.
3. Authenticate every remote socket route in both Main and Companion servers; add transfer limits/timeouts and harden the LAN transport.
4. Unify API-key storage and migrate the existing key into encrypted storage without leaving a plaintext duplicate.
5. Fix microphone ownership and lifecycle; later replace SpeechRecognizer looping with a dedicated wake-word engine if true always-on detection is required.
6. Move Gemini Live to the current v1beta endpoint and use short-lived ephemeral tokens for direct mobile Live sessions.
7. Make unimplemented actions fail explicitly; add automated tests for command routing, confirmation, permissions, remote auth, and result verification.
8. Correct feature documentation to reflect code, CI build, and real-device test status separately.

## Status

This report records findings; it does not claim that these repairs have been applied. The latest successful CI build is not a substitute for physical-device testing.
