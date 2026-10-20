package com.myra.assistant.security

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.util.Log
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.myra.assistant.tools.CommandIntent
import java.util.concurrent.Executor

enum class BiometricAvailability {
    AVAILABLE,
    NONE_ENROLLED,
    NOT_SUPPORTED,
    HARDWARE_UNAVAILABLE
}

data class VoiceVerificationResult(
    val isVerified: Boolean,
    val confidence: Float,
    val message: String,
    val isLockedOut: Boolean = false
)

/**
 * Production-Ready Owner Security Manager for MYRA Assistant.
 * 
 * Implements Defense-in-Depth:
 * 1. Layer 1: Speaker Recognition (Voice biometrics for identity signal and low-risk verification).
 * 2. Layer 2: Android BiometricPrompt (Hardware-backed biometric boundary for sensitive commands).
 * 3. Layer 3: Anti-Replay and dynamic acoustic flux gating.
 * 4. Layer 4: Configurable 2-minute authentication session window.
 * 5. Layer 5: Brute-force lockout and device-credential recovery.
 */
class OwnerSecurityManager private constructor(context: Context) {

    private val tag = "MYRA_Security"
    private val appContext = context.applicationContext

    private val prefs: SharedPreferences = createEncryptedPreferences(appContext)

    // Active session timestamp for the 2-minute biometric window
    private var lastBiometricSuccessTimestamp: Long = 0L

    // Temporary failure lockout
    private var consecutiveFailedAttempts: Int = 0
    private var lockoutExpirationTimestamp: Long = 0L

    companion object {
        @Volatile
        private var instance: OwnerSecurityManager? = null

        fun getInstance(context: Context): OwnerSecurityManager {
            return instance ?: synchronized(this) {
                instance ?: OwnerSecurityManager(context).also { instance = it }
            }
        }

        private fun createEncryptedPreferences(context: Context): SharedPreferences {
            return try {
                val masterKey = MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build()

                EncryptedSharedPreferences.create(
                    context,
                    "myra_owner_security_prefs",
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                )
            } catch (e: Exception) {
                Log.w("MYRA_Security", "Falling back to standard private preferences: ${e.message}")
                context.getSharedPreferences("myra_owner_security_prefs_fallback", Context.MODE_PRIVATE)
            }
        }

        // The 4 standardized voice enrollment prompts
        val ENROLLMENT_PHRASES = listOf(
            "Hi Myra",
            "My name is the owner of Myra",
            "Myra, are you ready?",
            "Myra, unlock owner mode"
        )
    }

    // --- Configuration Flags & Persistent State ---

    var isOwnerProtectionEnabled: Boolean
        get() = prefs.getBoolean("owner_protection_enabled", false)
        set(value) = prefs.edit().putBoolean("owner_protection_enabled", value).apply()

    var isBiometricLockEnabled: Boolean
        get() = prefs.getBoolean("biometric_lock_enabled", true)
        set(value) = prefs.edit().putBoolean("biometric_lock_enabled", value).apply()

    var isDeviceCredentialAllowed: Boolean
        get() = prefs.getBoolean("device_credential_allowed", true)
        set(value) = prefs.edit().putBoolean("device_credential_allowed", value).apply()

    var isUnknownVoiceProtectionEnabled: Boolean
        get() = prefs.getBoolean("unknown_voice_blocked", true)
        set(value) = prefs.edit().putBoolean("unknown_voice_blocked", value).apply()

    var isSensitiveCommandsProtected: Boolean
        get() = prefs.getBoolean("sensitive_commands_protected", true)
        set(value) = prefs.edit().putBoolean("sensitive_commands_protected", value).apply()

    var isAppOpenProtectionEnabled: Boolean
        get() = prefs.getBoolean("app_open_protection", false)
        set(value) = prefs.edit().putBoolean("app_open_protection", value).apply()

    var securityTimeoutMinutes: Int
        get() = prefs.getInt("security_timeout_mins", 2)
        set(value) = prefs.edit().putInt("security_timeout_mins", value.coerceIn(1, 10)).apply()

    val isOwnerVoiceEnrolled: Boolean
        get() = !prefs.getString("owner_voice_embedding", "").isNullOrBlank()

    // --- Speaker Voice Profile Management ---

    fun saveOwnerVoiceProfile(embeddingsList: List<FloatArray>): Boolean {
        if (embeddingsList.isEmpty()) return false

        // Average embeddings from all 4 multi-phrase recordings
        val dim = VoiceFeatureExtractor.EMBEDDING_DIM
        val averaged = FloatArray(dim)

        for (embedding in embeddingsList) {
            for (i in 0 until dim) {
                averaged[i] += embedding[i]
            }
        }

        var sumSq = 0f
        for (i in 0 until dim) {
            val avgVal = averaged[i] / embeddingsList.size.toFloat()
            averaged[i] = avgVal
            sumSq += avgVal * avgVal
        }

        val norm = kotlin.math.sqrt(sumSq).coerceAtLeast(1e-6f)
        for (i in 0 until dim) {
            averaged[i] = averaged[i] / norm
        }

        val serialized = VoiceFeatureExtractor.serializeEmbedding(averaged)
        prefs.edit()
            .putString("owner_voice_embedding", serialized)
            .putLong("owner_enrolled_timestamp", System.currentTimeMillis())
            .putBoolean("owner_protection_enabled", true)
            .apply()

        Log.i(tag, "Owner voice profile successfully enrolled with ${embeddingsList.size} phrase embeddings.")
        return true
    }

    fun getStoredOwnerEmbedding(): FloatArray? {
        val serialized = prefs.getString("owner_voice_embedding", null) ?: return null
        return VoiceFeatureExtractor.deserializeEmbedding(serialized)
    }

    fun clearOwnerVoiceProfile() {
        prefs.edit()
            .remove("owner_voice_embedding")
            .remove("owner_enrolled_timestamp")
            .putBoolean("owner_protection_enabled", false)
            .apply()
        Log.i(tag, "Owner voice profile deleted and protection disabled.")
    }

    // --- Speaker Verification Logic ---

    /**
     * Verifies the voice sample against the enrolled owner embedding.
     * Rejects if locked out or similarity is below the security threshold (0.76).
     */
    fun verifySpeaker(pcmSamples: ShortArray): VoiceVerificationResult {
        if (!isOwnerProtectionEnabled) {
            return VoiceVerificationResult(isVerified = true, confidence = 1.0f, message = "Owner protection is OFF")
        }

        val now = System.currentTimeMillis()
        if (now < lockoutExpirationTimestamp) {
            val remainingSecs = ((lockoutExpirationTimestamp - now) / 1000).coerceAtLeast(1)
            return VoiceVerificationResult(
                isVerified = false,
                confidence = 0f,
                message = "सुरक्षा चेतावनी: बहुत अधिक असफल प्रयास। $remainingSecs सेकंड प्रतीक्षा करें।",
                isLockedOut = true
            )
        }

        val ownerEmbedding = getStoredOwnerEmbedding()
        if (ownerEmbedding == null) {
            return VoiceVerificationResult(
                isVerified = false,
                confidence = 0f,
                message = "ओनर वॉइस प्रोफाइल एनरोल नहीं है।"
            )
        }

        val sampleEmbedding = VoiceFeatureExtractor.extractSpeakerEmbedding(pcmSamples)
        if (sampleEmbedding == null) {
            // Anti-replay failure or insufficient speech
            recordFailedAttempt()
            return VoiceVerificationResult(
                isVerified = false,
                confidence = 0f,
                message = "Sorry, I couldn't verify the owner."
            )
        }

        val similarity = VoiceFeatureExtractor.computeCosineSimilarity(ownerEmbedding, sampleEmbedding)
        Log.d(tag, "Speaker similarity score: $similarity (Threshold: 0.76)")

        val threshold = 0.76f
        return if (similarity >= threshold) {
            consecutiveFailedAttempts = 0
            VoiceVerificationResult(
                isVerified = true,
                confidence = similarity,
                message = "ओनर वॉइस सत्यापित।"
            )
        } else {
            recordFailedAttempt()
            VoiceVerificationResult(
                isVerified = false,
                confidence = similarity,
                message = if (isUnknownVoiceProtectionEnabled) "Owner verification required." else "Sorry, I couldn't verify the owner."
            )
        }
    }

    private fun recordFailedAttempt() {
        consecutiveFailedAttempts++
        Log.w(tag, "Failed voice verification attempt #$consecutiveFailedAttempts")
        if (consecutiveFailedAttempts >= 3) {
            // 60-second temporary security cooldown
            lockoutExpirationTimestamp = System.currentTimeMillis() + (60 * 1000L)
            Log.e(tag, "Security Lockout triggered for 60 seconds.")
        }
    }

    // --- Biometric Authentication Boundary ---

    fun isBiometricWindowActive(): Boolean {
        val windowDurationMs = securityTimeoutMinutes * 60 * 1000L
        val elapsed = System.currentTimeMillis() - lastBiometricSuccessTimestamp
        return elapsed in 0..windowDurationMs
    }

    fun recordBiometricSuccess() {
        lastBiometricSuccessTimestamp = System.currentTimeMillis()
        consecutiveFailedAttempts = 0
        lockoutExpirationTimestamp = 0L
        Log.i(tag, "Biometric authentication confirmed. Active session window started for $securityTimeoutMinutes mins.")
    }

    fun resetBiometricSession() {
        lastBiometricSuccessTimestamp = 0L
    }

    fun checkBiometricAvailability(): BiometricAvailability {
        val biometricManager = BiometricManager.from(appContext)
        val authenticators = if (isDeviceCredentialAllowed && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL
        } else {
            BiometricManager.Authenticators.BIOMETRIC_STRONG
        }

        return when (biometricManager.canAuthenticate(authenticators)) {
            BiometricManager.BIOMETRIC_SUCCESS -> BiometricAvailability.AVAILABLE
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> BiometricAvailability.NONE_ENROLLED
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> BiometricAvailability.NOT_SUPPORTED
            else -> BiometricAvailability.HARDWARE_UNAVAILABLE
        }
    }

    /**
     * Requirement 4 & 12: Enforces owner authentication before sensitive actions.
     * Evaluates BiometricManager status and invokes AndroidX BiometricPrompt on the host Activity.
     * Handles:
     * - BIOMETRIC_SUCCESS
     * - BIOMETRIC_ERROR_NO_BIOMETRICS
     * - BIOMETRIC_ERROR_NONE_ENROLLED
     * - BIOMETRIC_ERROR_USER_CANCELED
     * - BIOMETRIC_ERROR_NEGATIVE_BUTTON
     * - BIOMETRIC_ERROR_HW_UNAVAILABLE
     * - BIOMETRIC_ERROR_NO_HARDWARE
     */
    fun requireOwnerAuthentication(
        activity: FragmentActivity,
        title: String = "MYRA Owner Verification",
        subtitle: String = "संवेदनशील एक्शन के लिए बायोमेट्रिक प्रमाणीकरण आवश्यक है",
        description: String = "संवेदनशील एक्शन केवल अधिकृत ओनर द्वारा ही निष्पादित हो सकता है।",
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        // 1. If Owner Protection is disabled, allow execution immediately
        if (!isOwnerProtectionEnabled) {
            onSuccess()
            return
        }

        // 2. Active Session Window Check (Default 2 minutes)
        if (isBiometricWindowActive()) {
            Log.i(tag, "Active biometric session valid ($securityTimeoutMinutes min window). Authorization granted.")
            onSuccess()
            return
        }

        val biometricManager = BiometricManager.from(appContext)
        val canUseDeviceCredential = isDeviceCredentialAllowed && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
        val authenticators = if (canUseDeviceCredential) {
            BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL
        } else {
            BiometricManager.Authenticators.BIOMETRIC_STRONG
        }

        val availabilityStatus = biometricManager.canAuthenticate(authenticators)
        when (availabilityStatus) {
            BiometricManager.BIOMETRIC_SUCCESS -> {
                Log.d(tag, "Biometric status: BIOMETRIC_SUCCESS")
            }
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                Log.w(tag, "Biometric status: BIOMETRIC_ERROR_NONE_ENROLLED")
                if (!canUseDeviceCredential) {
                    onError("डिवाइस पर कोई फिंगरप्रिंट या फेस एनरोल नहीं है: Owner verification required.")
                    return
                }
            }
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> {
                Log.w(tag, "Biometric status: BIOMETRIC_ERROR_NO_HARDWARE")
                if (!canUseDeviceCredential) {
                    onError("डिवाइस में बायोमेट्रिक हार्डवेयर नहीं है: Owner verification required.")
                    return
                }
            }
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> {
                Log.w(tag, "Biometric status: BIOMETRIC_ERROR_HW_UNAVAILABLE")
                if (!canUseDeviceCredential) {
                    onError("बायोमेट्रिक हार्डवेयर व्यस्त है: Owner verification required.")
                    return
                }
            }
            else -> {
                Log.w(tag, "Biometric availability status code: $availabilityStatus")
            }
        }

        val executor: Executor = ContextCompat.getMainExecutor(activity)
        val promptInfoBuilder = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setDescription(description)

        if (canUseDeviceCredential) {
            promptInfoBuilder.setAllowedAuthenticators(authenticators)
        } else {
            promptInfoBuilder.setNegativeButtonText("रद्द करें")
            promptInfoBuilder.setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
        }

        val biometricPrompt = BiometricPrompt(activity, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                Log.i(tag, "BiometricPrompt: AUTHENTICATION_SUCCEEDED")
                recordBiometricSuccess()
                onSuccess()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                Log.w(tag, "BiometricPrompt Error ($errorCode): $errString")
                when (errorCode) {
                    BiometricPrompt.ERROR_USER_CANCELED,
                    BiometricPrompt.ERROR_NEGATIVE_BUTTON -> {
                        onError("Owner verification required.")
                    }
                    BiometricPrompt.ERROR_NO_BIOMETRICS -> {
                        onError("डिवाइस पर कोई बायोमेट्रिक एनरोल नहीं है: Owner verification required.")
                    }
                    BiometricPrompt.ERROR_LOCKOUT,
                    BiometricPrompt.ERROR_LOCKOUT_PERMANENT -> {
                        onError("बहुत सारे असफल प्रयास: डिवाइस लॉक। Owner verification required.")
                    }
                    else -> {
                        onError("Owner verification required.")
                    }
                }
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                Log.w(tag, "BiometricPrompt: Authentication failed (fingerprint/face mismatch).")
            }
        })

        try {
            biometricPrompt.authenticate(promptInfoBuilder.build())
        } catch (e: Exception) {
            Log.e(tag, "Failed to launch BiometricPrompt: ${e.message}", e)
            onError("Owner verification required.")
        }
    }

    /**
     * Alias for requireOwnerAuthentication() to preserve backward compatibility.
     */
    fun showBiometricPrompt(
        activity: FragmentActivity,
        title: String = "MYRA Owner Verification",
        subtitle: String = "सुरक्षित बायोमेट्रिक प्रमाणीकरण आवश्यक है",
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        requireOwnerAuthentication(
            activity = activity,
            title = title,
            subtitle = subtitle,
            onSuccess = onSuccess,
            onError = onError
        )
    }

    // --- Sensitive Command Classifier ---

    /**
     * Determines whether a command is sensitive and requires biometric authorization.
     * Enforces the security boundary specified in Requirement 5.
     * 
     * Low-risk normal commands (opening installed apps like YouTube, WhatsApp, Instagram, Facebook,
     * Chrome, etc., torch, music, battery, volume) must NEVER require biometric authentication.
     */
    fun isSensitiveCommand(intent: CommandIntent, target: String, rawText: String): Boolean {
        if (!isSensitiveCommandsProtected) return false

        // 0. NORMAL APP LAUNCH RULE: Opening installed apps is low-risk and must NEVER require biometric authentication
        if (intent == CommandIntent.OPEN_APP) {
            return false
        }

        val lower = rawText.lowercase().trim()

        // Explicit safeguard: Any app-opening phrasing without sending messages/calls/data modification is non-sensitive
        val isOpenAppPhrasing = (lower.contains("kholo") || lower.contains("open") || lower.contains("launch") || lower.contains("chalao")) &&
                                !lower.contains("message") && !lower.contains("msg") && !lower.contains("bhejo") &&
                                !lower.contains("send") && !lower.contains("sos") && !lower.contains("otp") &&
                                !lower.contains("password") && !lower.contains("call lagao") && !lower.contains("phone lagao")
        if (isOpenAppPhrasing) {
            return false
        }

        // 1. Sending WhatsApp messages
        if (intent == CommandIntent.SEND_WHATSAPP) return true
        if (lower.contains("whatsapp") && (lower.contains("send") || lower.contains("bhejo") || lower.contains("message") || lower.contains("msg") || lower.contains("text") || lower.contains("bolo"))) return true

        // 2. Sending SMS
        if (lower.contains("sms") || lower.contains("text message") || (lower.contains("message") && (lower.contains("bhejo") || lower.contains("send") || lower.contains("kar do")))) return true
        if (lower.contains("मैसेज भेजो") || lower.contains("एसएमएस")) return true

        // 3. Sending Emails
        if (lower.contains("email") || lower.contains("e-mail") || lower.contains("mail") || lower.contains("ईमेल") || lower.contains("मेल")) {
            if (lower.contains("send") || lower.contains("bhejo") || lower.contains("karo") || lower.contains("write") || lower.contains("compose")) return true
        }

        // 4. Important phone calls
        if (intent == CommandIntent.CALL_PHONE) return true
        if (lower.contains("call lagao") || lower.contains("phone lagao") || lower.contains("कॉल लगाओ") || lower.contains("फोन लगाओ") || lower.contains("कॉल करो")) return true

        // 5. SOS actions & Emergency triggers
        if (lower.contains("sos") || lower.contains("emergency") || lower.contains("मदद") || lower.contains("help me") || lower.contains("बचाओ") || lower.contains("आपातकाल")) return true

        // 6. Sharing location & GPS tracking
        if (lower.contains("share location") || lower.contains("location share") || lower.contains("live location") ||
            lower.contains("लोकेशन भेजो") || lower.contains("लाइव लोकेशन") || lower.contains("लोकेशन शेयर") ||
            (lower.contains("gps") && lower.contains("share"))) return true

        // 7. Reading private messages & Notifications
        if (lower.contains("read message") || lower.contains("read sms") || lower.contains("message padho") ||
            lower.contains("मैसेज पढ़ो") || lower.contains("sms padho") || lower.contains("notification padho") ||
            lower.contains("read notification") || lower.contains("otp padho") || lower.contains("check message")) return true

        // 8. Accessing protected personal data, OTP, Passwords, Bank info
        if (lower.contains("otp") || lower.contains("password") || lower.contains("पासवर्ड") ||
            lower.contains("bank") || lower.contains("बैंक") || lower.contains("balance") ||
            lower.contains("private") || lower.contains("पर्सनल") || lower.contains("गोपनीय") ||
            lower.contains("personal data") || lower.contains("account balance")) return true

        // 9. Connecting another device / PC (MYRA Connect)
        if (intent == CommandIntent.EXECUTE_REMOTE_DEVICE || lower.contains("दूसरा फोन") ||
            lower.contains("दूसरे फोन") || lower.contains("pc connect") || lower.contains("computer") ||
            lower.contains("laptop connect") || lower.contains("remote phone") || lower.contains("pair device") ||
            lower.contains("connect device")) return true

        // 10. Changing MYRA security settings, disabling Owner Protection, re-enrolling voice, credentials, export
        if (lower.contains("security") || lower.contains("सुरक्षा") || lower.contains("voice change") ||
            lower.contains("re-enroll") || lower.contains("reenroll") || lower.contains("export") ||
            lower.contains("credential") || lower.contains("api key") || lower.contains("reset owner") ||
            lower.contains("disable protection") || lower.contains("turn off protection") ||
            lower.contains("delete profile") || lower.contains("backup data") || lower.contains("export data")) return true

        return false
    }
}
