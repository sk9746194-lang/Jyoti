package com.myra.assistant.wallpaper

import android.content.Context
import android.content.SharedPreferences

enum class WallpaperQuality(val title: String, val fps: Int) {
    LOW("Low (30 FPS)", 30),
    MEDIUM("Medium (45 FPS)", 45),
    HIGH("High (60 FPS)", 60)
}

enum class WallpaperSpeed(val title: String, val multiplier: Float) {
    SLOW("Slow (0.6x)", 0.6f),
    NORMAL("Normal (1.0x)", 1.0f),
    FAST("Fast (1.5x)", 1.5f)
}

enum class WallpaperTheme(val title: String) {
    BLUE("Neon Electric Blue"),
    RED("Cyberpunk Red"),
    BLUE_RED("Dual Blue + Red Neon")
}

class MyraWallpaperPreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var isEnabled: Boolean
        get() = prefs.getBoolean(KEY_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_ENABLED, value).apply()

    var quality: WallpaperQuality
        get() = try {
            WallpaperQuality.valueOf(prefs.getString(KEY_QUALITY, WallpaperQuality.HIGH.name) ?: WallpaperQuality.HIGH.name)
        } catch (e: Exception) {
            WallpaperQuality.HIGH
        }
        set(value) = prefs.edit().putString(KEY_QUALITY, value.name).apply()

    // Backward-compatible alias for quality
    var fps: WallpaperQuality
        get() = quality
        set(value) { quality = value }

    var speed: WallpaperSpeed
        get() = try {
            WallpaperSpeed.valueOf(prefs.getString(KEY_SPEED, WallpaperSpeed.NORMAL.name) ?: WallpaperSpeed.NORMAL.name)
        } catch (e: Exception) {
            WallpaperSpeed.NORMAL
        }
        set(value) = prefs.edit().putString(KEY_SPEED, value.name).apply()

    var theme: WallpaperTheme
        get() = try {
            WallpaperTheme.valueOf(prefs.getString(KEY_THEME, WallpaperTheme.BLUE_RED.name) ?: WallpaperTheme.BLUE_RED.name)
        } catch (e: Exception) {
            WallpaperTheme.BLUE_RED
        }
        set(value) = prefs.edit().putString(KEY_THEME, value.name).apply()

    var showAppIcons: Boolean
        get() = prefs.getBoolean(KEY_SHOW_ICONS, true)
        set(value) = prefs.edit().putBoolean(KEY_SHOW_ICONS, value).apply()

    var showOrb: Boolean
        get() = prefs.getBoolean(KEY_SHOW_ORB, true)
        set(value) = prefs.edit().putBoolean(KEY_SHOW_ORB, value).apply()

    var showEarth: Boolean
        get() = prefs.getBoolean(KEY_SHOW_EARTH, true)
        set(value) = prefs.edit().putBoolean(KEY_SHOW_EARTH, value).apply()

    // Backward-compatible alias for Earth Hologram
    var showEarthHologram: Boolean
        get() = showEarth
        set(value) { showEarth = value }

    var particleEffects: Boolean
        get() = prefs.getBoolean(KEY_PARTICLES, true)
        set(value) = prefs.edit().putBoolean(KEY_PARTICLES, value).apply()

    // Backward-compatible alias for particles
    var showParticles: Boolean
        get() = particleEffects
        set(value) { particleEffects = value }

    var showHudRings: Boolean
        get() = prefs.getBoolean(KEY_SHOW_HUD_RINGS, true)
        set(value) = prefs.edit().putBoolean(KEY_SHOW_HUD_RINGS, value).apply()

    var showWaveform: Boolean
        get() = prefs.getBoolean(KEY_SHOW_WAVEFORM, true)
        set(value) = prefs.edit().putBoolean(KEY_SHOW_WAVEFORM, value).apply()

    var batterySaverMode: Boolean
        get() = prefs.getBoolean(KEY_BATTERY_SAVER, false)
        set(value) = prefs.edit().putBoolean(KEY_BATTERY_SAVER, value).apply()

    companion object {
        private const val PREFS_NAME = "myra_live_wallpaper_prefs"
        private const val KEY_ENABLED = "wallpaper_enabled"
        private const val KEY_QUALITY = "wallpaper_quality"
        private const val KEY_SPEED = "wallpaper_speed"
        private const val KEY_THEME = "wallpaper_theme"
        private const val KEY_SHOW_ICONS = "wallpaper_show_icons"
        private const val KEY_SHOW_ORB = "wallpaper_show_orb"
        private const val KEY_SHOW_EARTH = "wallpaper_show_earth"
        private const val KEY_PARTICLES = "wallpaper_particles"
        private const val KEY_SHOW_HUD_RINGS = "wallpaper_show_hud_rings"
        private const val KEY_SHOW_WAVEFORM = "wallpaper_show_waveform"
        private const val KEY_BATTERY_SAVER = "wallpaper_battery_saver"

        @Volatile
        private var instance: MyraWallpaperPreferences? = null

        fun getInstance(context: Context): MyraWallpaperPreferences {
            return instance ?: synchronized(this) {
                instance ?: MyraWallpaperPreferences(context.applicationContext).also { instance = it }
            }
        }
    }
}
