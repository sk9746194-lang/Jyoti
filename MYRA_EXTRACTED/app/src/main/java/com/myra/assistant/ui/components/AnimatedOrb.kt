package com.myra.assistant.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.myra.assistant.data.AssistantState
import com.myra.assistant.ui.orb.OrbCustomizationManager
import com.myra.assistant.ui.orb.OrbRenderer
import com.myra.assistant.ui.orb.OrbSettings

/**
 * Audio-reactive Glowing Orb for MYRA Assistant.
 * Integrates directly with the 10-Orb Customization Architecture while maintaining
 * 100% backward compatibility for existing callers.
 */
@Composable
fun AnimatedOrb(
    state: AssistantState,
    audioLevel: Float,
    settings: OrbSettings? = null,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val manager = remember { OrbCustomizationManager.getInstance(context) }
    val currentSettings by manager.settingsState

    val effectiveSettings = settings ?: currentSettings

    OrbRenderer(
        state = state,
        audioLevel = audioLevel,
        settings = effectiveSettings,
        onClick = onClick,
        modifier = modifier
    )
}
