package com.myra.assistant.ui.orb

import android.content.Context
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import com.myra.assistant.data.AssistantState
import com.myra.assistant.data.PreferencesManager

/**
 * Singleton StateHolder & Decoupled Customization Controller for MYRA Orbs & Screen Atmosphere.
 * Allows instant, hot-swappable visual customization without touching voice engine internals.
 */
class OrbCustomizationManager private constructor(context: Context) {

    private val preferencesManager = PreferencesManager(context)

    private val _settingsState = mutableStateOf(preferencesManager.getOrbSettings())
    val settingsState: State<OrbSettings> = _settingsState

    fun updateSettings(newSettings: OrbSettings) {
        _settingsState.value = newSettings
        preferencesManager.saveOrbSettings(newSettings)
    }

    fun updateStyle(style: OrbStyle) {
        updateSettings(_settingsState.value.copy(style = style))
    }

    fun updateSize(sizeDp: Float) {
        updateSettings(_settingsState.value.copy(sizeDp = sizeDp))
    }

    fun updateGlow(glow: Float) {
        updateSettings(_settingsState.value.copy(glowIntensity = glow))
    }

    fun updateSpeed(speed: Float) {
        updateSettings(_settingsState.value.copy(animationSpeed = speed))
    }

    fun updateParticles(amount: Int) {
        updateSettings(_settingsState.value.copy(particleAmount = amount))
    }

    fun updateRingThickness(thickness: Float) {
        updateSettings(_settingsState.value.copy(ringThickness = thickness))
    }

    fun updateOpacity(opacity: Float) {
        updateSettings(_settingsState.value.copy(opacity = opacity))
    }

    fun updatePosition(pos: OrbPosition) {
        updateSettings(_settingsState.value.copy(position = pos))
    }

    fun updateThemeColor(theme: OrbThemeColor) {
        updateSettings(_settingsState.value.copy(themeColor = theme))
    }

    fun updateBackgroundStyle(bg: BackgroundStyle) {
        updateSettings(_settingsState.value.copy(backgroundStyle = bg))
    }

    fun updateBackgroundSpeed(speed: Float) {
        updateSettings(_settingsState.value.copy(backgroundSpeed = speed))
    }

    fun updateBackgroundOpacity(opacity: Float) {
        updateSettings(_settingsState.value.copy(backgroundOpacity = opacity))
    }

    fun updateEdgeEffect(mode: EdgeEffectMode) {
        updateSettings(_settingsState.value.copy(edgeEffectMode = mode))
    }

    fun updateEdgeGlow(glow: Float) {
        updateSettings(_settingsState.value.copy(edgeGlowIntensity = glow))
    }

    fun resetToDefaults() {
        val defaults = OrbSettings()
        updateSettings(defaults)
    }

    /**
     * Resolves high-contrast reactive colors according to Assistant voice state
     * seamlessly combined with user's selected theme palette.
     */
    fun resolveStateColors(state: AssistantState, theme: OrbThemeColor): Pair<Color, Color> {
        return when (state) {
            AssistantState.IDLE -> theme.primaryColor to theme.secondaryColor
            AssistantState.LISTENING -> Color(0xFFFF1744) to Color(0xFFFF5252)
            AssistantState.THINKING -> Color(0xFFFFB300) to Color(0xFFFFD54F)
            AssistantState.SPEAKING -> Color(0xFF00E676) to Color(0xFF69F0AE)
            AssistantState.ERROR -> Color(0xFFD50000) to Color(0xFFFF1744)
            AssistantState.WAKE_WORD -> Color(0xFFFFFFFF) to theme.primaryColor
        }
    }

    companion object {
        @Volatile
        private var instance: OrbCustomizationManager? = null

        fun getInstance(context: Context): OrbCustomizationManager {
            return instance ?: synchronized(this) {
                instance ?: OrbCustomizationManager(context.applicationContext).also { instance = it }
            }
        }
    }
}
