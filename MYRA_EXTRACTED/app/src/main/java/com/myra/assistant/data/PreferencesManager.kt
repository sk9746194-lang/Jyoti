package com.myra.assistant.data

import android.content.Context
import android.content.SharedPreferences
import com.myra.assistant.ui.orb.*

class PreferencesManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("myra_prefs", Context.MODE_PRIVATE)

    var wakeWord: String
        get() = prefs.getString("wake_word", "Hey MYRA") ?: "Hey MYRA"
        set(value) = prefs.edit().putString("wake_word", value).apply()

    var isHandsFreeWakeWordEnabled: Boolean
        get() = prefs.getBoolean("hands_free_wake_word", true)
        set(value) = prefs.edit().putBoolean("hands_free_wake_word", value).apply()

    var isLockScreenAllowed: Boolean
        get() = prefs.getBoolean("lock_screen_allowed", true)
        set(value) = prefs.edit().putBoolean("lock_screen_allowed", value).apply()

    var isKeepScreenOnEnabled: Boolean
        get() = prefs.getBoolean("keep_screen_on", true)
        set(value) = prefs.edit().putBoolean("keep_screen_on", value).apply()

    var personality: String
        get() = prefs.getString("personality", "friendly_assistant") ?: "friendly_assistant"
        set(value) = prefs.edit().putString("personality", value).apply()

    var speechRate: Float
        get() = prefs.getFloat("speech_rate", 1.0f)
        set(value) = prefs.edit().putFloat("speech_rate", value).apply()

    var voicePitch: Float
        get() = prefs.getFloat("voice_pitch", 1.0f)
        set(value) = prefs.edit().putFloat("voice_pitch", value).apply()

    var bluetoothAudio: Boolean
        get() = prefs.getBoolean("bluetooth_audio", true)
        set(value) = prefs.edit().putBoolean("bluetooth_audio", value).apply()

    // --- Persistent Orb, Screen Effect & Background Customization ---

    var orbStyle: OrbStyle
        get() = try {
            OrbStyle.valueOf(prefs.getString("orb_style", OrbStyle.LIQUID_GLASS_PEARL.name) ?: OrbStyle.LIQUID_GLASS_PEARL.name)
        } catch (e: Exception) { OrbStyle.LIQUID_GLASS_PEARL }
        set(value) = prefs.edit().putString("orb_style", value.name).apply()

    var orbSizeDp: Float
        get() = prefs.getFloat("orb_size_dp", 240f)
        set(value) = prefs.edit().putFloat("orb_size_dp", value).apply()

    var orbGlowIntensity: Float
        get() = prefs.getFloat("orb_glow_intensity", 0.85f)
        set(value) = prefs.edit().putFloat("orb_glow_intensity", value).apply()

    var orbAnimationSpeed: Float
        get() = prefs.getFloat("orb_anim_speed", 1.0f)
        set(value) = prefs.edit().putFloat("orb_anim_speed", value).apply()

    var orbParticleAmount: Int
        get() = prefs.getInt("orb_particles", 24)
        set(value) = prefs.edit().putInt("orb_particles", value).apply()

    var orbRingThickness: Float
        get() = prefs.getFloat("orb_ring_thickness", 3.5f)
        set(value) = prefs.edit().putFloat("orb_ring_thickness", value).apply()

    var orbOpacity: Float
        get() = prefs.getFloat("orb_opacity", 0.95f)
        set(value) = prefs.edit().putFloat("orb_opacity", value).apply()

    var orbPosition: OrbPosition
        get() = try {
            OrbPosition.valueOf(prefs.getString("orb_position", OrbPosition.CENTER.name) ?: OrbPosition.CENTER.name)
        } catch (e: Exception) { OrbPosition.CENTER }
        set(value) = prefs.edit().putString("orb_position", value.name).apply()

    var orbThemeColor: OrbThemeColor
        get() = try {
            OrbThemeColor.valueOf(prefs.getString("orb_theme_color", OrbThemeColor.MYRA_SIGNATURE.name) ?: OrbThemeColor.MYRA_SIGNATURE.name)
        } catch (e: Exception) { OrbThemeColor.MYRA_SIGNATURE }
        set(value) = prefs.edit().putString("orb_theme_color", value.name).apply()

    var backgroundStyle: BackgroundStyle
        get() = try {
            BackgroundStyle.valueOf(prefs.getString("bg_style", BackgroundStyle.MYRA_SIGNATURE.name) ?: BackgroundStyle.MYRA_SIGNATURE.name)
        } catch (e: Exception) { BackgroundStyle.MYRA_SIGNATURE }
        set(value) = prefs.edit().putString("bg_style", value.name).apply()

    var backgroundSpeed: Float
        get() = prefs.getFloat("bg_speed", 1.0f)
        set(value) = prefs.edit().putFloat("bg_speed", value).apply()

    var backgroundOpacity: Float
        get() = prefs.getFloat("bg_opacity", 0.85f)
        set(value) = prefs.edit().putFloat("bg_opacity", value).apply()

    var edgeEffectMode: EdgeEffectMode
        get() = try {
            EdgeEffectMode.valueOf(prefs.getString("edge_effect_mode", EdgeEffectMode.ALL_4_SIDES.name) ?: EdgeEffectMode.ALL_4_SIDES.name)
        } catch (e: Exception) { EdgeEffectMode.ALL_4_SIDES }
        set(value) = prefs.edit().putString("edge_effect_mode", value.name).apply()

    var edgeGlowIntensity: Float
        get() = prefs.getFloat("edge_glow_intensity", 0.8f)
        set(value) = prefs.edit().putFloat("edge_glow_intensity", value).apply()

    fun getOrbSettings(): OrbSettings = OrbSettings(
        style = orbStyle,
        sizeDp = orbSizeDp,
        glowIntensity = orbGlowIntensity,
        animationSpeed = orbAnimationSpeed,
        particleAmount = orbParticleAmount,
        ringThickness = orbRingThickness,
        opacity = orbOpacity,
        position = orbPosition,
        themeColor = orbThemeColor,
        backgroundStyle = backgroundStyle,
        backgroundSpeed = backgroundSpeed,
        backgroundOpacity = backgroundOpacity,
        edgeEffectMode = edgeEffectMode,
        edgeGlowIntensity = edgeGlowIntensity
    )

    fun saveOrbSettings(settings: OrbSettings) {
        orbStyle = settings.style
        orbSizeDp = settings.sizeDp
        orbGlowIntensity = settings.glowIntensity
        orbAnimationSpeed = settings.animationSpeed
        orbParticleAmount = settings.particleAmount
        orbRingThickness = settings.ringThickness
        orbOpacity = settings.opacity
        orbPosition = settings.position
        orbThemeColor = settings.themeColor
        backgroundStyle = settings.backgroundStyle
        backgroundSpeed = settings.backgroundSpeed
        backgroundOpacity = settings.backgroundOpacity
        edgeEffectMode = settings.edgeEffectMode
        edgeGlowIntensity = settings.edgeGlowIntensity
    }

    // --- Custom Home Photo Management ---

    var customPhotoPath: String?
        get() {
            val path = prefs.getString("custom_home_photo_path", null)
            if (path != null && java.io.File(path).exists()) {
                return path
            }
            return null
        }
        set(value) = prefs.edit().putString("custom_home_photo_path", value).apply()

    val hasCustomPhoto: Boolean
        get() = customPhotoPath != null

    /**
     * Saves a user-picked photo from URI into internal app storage (myra_home_portrait.jpg)
     * so it persists permanently without URI permission expiry across app restarts.
     * Returns the absolute path of the saved photo, or null on failure.
     */
    fun saveCustomPhotoFromUri(context: Context, sourceUri: android.net.Uri): String? {
        return try {
            val destinationFile = java.io.File(context.filesDir, "myra_home_portrait.jpg")
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                java.io.FileOutputStream(destinationFile).use { output ->
                    input.copyTo(output)
                }
            }
            val path = destinationFile.absolutePath
            customPhotoPath = path
            path
        } catch (e: Exception) {
            android.util.Log.e("MYRA_Photo", "Failed to save custom photo from URI", e)
            null
        }
    }

    /**
     * Resets Home Photo back to the bundled default resource.
     */
    fun resetHomePhoto(context: Context) {
        try {
            val file = java.io.File(context.filesDir, "myra_home_portrait.jpg")
            if (file.exists()) {
                file.delete()
            }
        } catch (_: Exception) {}
        customPhotoPath = null
    }

    // --- Gemini API Key & Model Configuration ---

    var geminiApiKey: String
        get() = prefs.getString("user_gemini_api_key", "") ?: ""
        set(value) = prefs.edit().putString("user_gemini_api_key", value.trim()).apply()

    val hasConfiguredApiKey: Boolean
        get() = geminiApiKey.isNotBlank()

    var selectedGeminiModel: String
        get() = prefs.getString("selected_gemini_model", GeminiApiClient.DEFAULT_MODEL) ?: GeminiApiClient.DEFAULT_MODEL
        set(value) = prefs.edit().putString("selected_gemini_model", value.trim()).apply()

    var geminiApiConnectionStatus: String
        get() = prefs.getString("gemini_api_status", "Not Configured") ?: "Not Configured"
        set(value) = prefs.edit().putString("gemini_api_status", value).apply()

    fun clearGeminiApiKey() {
        prefs.edit()
            .remove("user_gemini_api_key")
            .putString("gemini_api_status", "Not Configured")
            .apply()
    }

    // --- WhatsApp Automation Preferences ---

    var isWhatsAppConfirmBeforeSend: Boolean
        get() = prefs.getBoolean("whatsapp_confirm_before_send", true)
        set(value) = prefs.edit().putBoolean("whatsapp_confirm_before_send", value).apply()

    // --- AI Chat & Voice Conversation System Preferences ---

    var isVoiceResponseInChatEnabled: Boolean
        get() = prefs.getBoolean("chat_voice_response_enabled", true)
        set(value) = prefs.edit().putBoolean("chat_voice_response_enabled", value).apply()

    var isContinuousChatVoiceEnabled: Boolean
        get() = prefs.getBoolean("chat_continuous_voice_enabled", false)
        set(value) = prefs.edit().putBoolean("chat_continuous_voice_enabled", value).apply()

    var preferredChatLanguage: String
        get() = prefs.getString("chat_preferred_language", "auto") ?: "auto"
        set(value) = prefs.edit().putString("chat_preferred_language", value).apply()

    // --- Free API & Paid Usage Protection (PART 4 & PART 5) ---

    /**
     * Controls whether paid Gemini API models can be used.
     * KEPT FALSE (OFF) BY DEFAULT to prevent unexpected billing.
     */
    var isPaidUsageAllowed: Boolean
        get() = prefs.getBoolean("allow_paid_api_usage", false)
        set(value) = prefs.edit().putBoolean("allow_paid_api_usage", value).apply()

    var dailyRequestCount: Int
        get() = prefs.getInt("api_daily_request_count", 0)
        set(value) = prefs.edit().putInt("api_daily_request_count", value).apply()

    var lastRequestTimestamp: Long
        get() = prefs.getLong("api_last_request_time", 0L)
        set(value) = prefs.edit().putLong("api_last_request_time", value).apply()

    /**
     * Resets or increments daily request counter for local rate estimation.
     */
    fun recordApiRequest() {
        val now = System.currentTimeMillis()
        val oneDayMs = 24 * 60 * 60 * 1000L
        if (now - lastRequestTimestamp > oneDayMs) {
            dailyRequestCount = 1
        } else {
            dailyRequestCount = dailyRequestCount + 1
        }
        lastRequestTimestamp = now
    }
}
