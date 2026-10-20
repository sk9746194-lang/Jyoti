package com.myra.assistant.data

import android.content.Context
import android.content.SharedPreferences
import com.myra.assistant.ui.orb.*

class PreferencesManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("myra_prefs", Context.MODE_PRIVATE)

    var wakeWord: String
        get() = prefs.getString("wake_word", "Hey MYRA") ?: "Hey MYRA"
        set(value) = prefs.edit().putString("wake_word", value).apply()

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
            OrbStyle.valueOf(prefs.getString("orb_style", OrbStyle.MYRA_SIGNATURE.name) ?: OrbStyle.MYRA_SIGNATURE.name)
        } catch (e: Exception) { OrbStyle.MYRA_SIGNATURE }
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
}
