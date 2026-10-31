package com.myra.assistant.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.myra.assistant.data.AssistantState
import com.myra.assistant.ui.orb.*
import com.myra.assistant.ui.theme.*

@Composable
fun OrbCustomizationScreen(onBack: (() -> Unit)? = null) {
    val context = LocalContext.current
    val manager = remember { OrbCustomizationManager.getInstance(context) }
    val currentSettings by manager.settingsState

    // Live preview voice state
    var previewState by remember { mutableStateOf(AssistantState.SPEAKING) }
    var previewAudioLevel by remember { mutableFloatStateOf(0.45f) }

    // Active customization tab
    var activeSubTab by remember { mutableStateOf(0) } // 0: Orb Gallery, 1: Sliders & Size, 2: Theme & Background, 3: Edge Screen Effect

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
    ) {
        // Live Preview Background
        BackgroundRenderer(
            style = currentSettings.backgroundStyle,
            speed = currentSettings.backgroundSpeed,
            opacity = currentSettings.backgroundOpacity,
            theme = currentSettings.themeColor
        )

        // Live Preview Edge Effect
        EdgeEffectRenderer(
            state = previewState,
            audioLevel = previewAudioLevel,
            mode = currentSettings.edgeEffectMode,
            intensity = currentSettings.edgeGlowIntensity,
            theme = currentSettings.themeColor
        )

        Column(modifier = Modifier.fillMaxSize()) {
            // Header bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Column {
                        Text(
                            text = "Orb & Screen Studio",
                            color = TextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "10 Styles • Edge Effects • Backgrounds",
                            color = PrimaryBlue,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                TextButton(
                    onClick = { manager.resetToDefaults() },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFF87171))
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Reset", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Reset", fontSize = 12.sp)
                }
            }

            // Compact Live Preview Stage with State Switcher
            Surface(
                color = CardDark.copy(alpha = 0.75f),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, BorderSlate),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Orb Live Preview Canvas
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        OrbRenderer(
                            state = previewState,
                            audioLevel = previewAudioLevel,
                            settings = currentSettings.copy(sizeDp = minOf(currentSettings.sizeDp, 180f)),
                            onClick = {
                                previewAudioLevel = if (previewAudioLevel > 0.5f) 0.15f else 0.85f
                            }
                        )
                    }

                    Text(
                        text = "${currentSettings.style.displayName} • Tap to toggle audio pulse",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Voice State Switcher for Live Testing
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            AssistantState.IDLE to "IDLE",
                            AssistantState.LISTENING to "LISTENING",
                            AssistantState.THINKING to "THINKING",
                            AssistantState.SPEAKING to "SPEAKING",
                            AssistantState.ERROR to "ERROR"
                        ).forEach { (st, label) ->
                            val isSelected = previewState == st
                            FilterChip(
                                selected = isSelected,
                                onClick = { previewState = st },
                                label = { Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = PrimaryBlue,
                                    selectedLabelColor = Color.White,
                                    containerColor = BgDark,
                                    labelColor = TextSecondary
                                ),
                                border = BorderStroke(1.dp, if (isSelected) PrimaryBlue else BorderSlate)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Sub Navigation Tabs
            ScrollableTabRow(
                selectedTabIndex = activeSubTab,
                containerColor = Color.Transparent,
                contentColor = PrimaryBlue,
                edgePadding = 16.dp,
                divider = {}
            ) {
                listOf(
                    "🔮 10 Orbs" to 0,
                    "⚙️ Size & Physics" to 1,
                    "🎨 Theme & Background" to 2,
                    "⚡ Edge Glow" to 3
                ).forEach { (title, index) ->
                    Tab(
                        selected = activeSubTab == index,
                        onClick = { activeSubTab = index },
                        text = {
                            Text(
                                title,
                                fontSize = 12.sp,
                                fontWeight = if (activeSubTab == index) FontWeight.Bold else FontWeight.Normal,
                                color = if (activeSubTab == index) PrimaryBlue else TextSecondary
                            )
                        }
                    )
                }
            }

            // Controls Lazy Column
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                when (activeSubTab) {
                    0 -> {
                        // ---------------------------------------------
                        // TAB 0: 10 ORB GALLERY
                        // ---------------------------------------------
                        items(OrbStyle.values().toList()) { style ->
                            val isSelected = currentSettings.style == style
                            Surface(
                                color = if (isSelected) PrimaryBlue.copy(alpha = 0.2f) else CardDark,
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(
                                    if (isSelected) 1.5.dp else 1.dp,
                                    if (isSelected) PrimaryBlue else BorderSlate
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { manager.updateStyle(style) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(44.dp)
                                                .clip(CircleShape)
                                                .background(BgDark)
                                                .border(1.dp, if (isSelected) PrimaryBlue else BorderSlate, CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            OrbRenderer(
                                                state = AssistantState.IDLE,
                                                audioLevel = 0.1f,
                                                settings = currentSettings.copy(style = style, sizeDp = 40f)
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(12.dp))

                                        Column {
                                            Text(
                                                text = style.displayName,
                                                color = TextPrimary,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Text(
                                                text = style.subtitle,
                                                color = TextSecondary,
                                                fontSize = 11.sp
                                            )
                                        }
                                    }

                                    RadioButton(
                                        selected = isSelected,
                                        onClick = { manager.updateStyle(style) },
                                        colors = RadioButtonDefaults.colors(selectedColor = PrimaryBlue)
                                    )
                                }
                            }
                        }
                    }

                    1 -> {
                        // ---------------------------------------------
                        // TAB 1: SLIDERS & SIZE
                        // ---------------------------------------------
                        item {
                            CustomSliderCard(
                                title = "Orb Diameter / Size: ${currentSettings.sizeDp.toInt()} dp",
                                subtitle = "Range 140dp - 320dp (Optimized for one-handed operation)",
                                value = currentSettings.sizeDp,
                                onValueChange = { manager.updateSize(it) },
                                valueRange = 140f..320f
                            )
                        }
                        item {
                            CustomSliderCard(
                                title = "Glow Intensity: ${(currentSettings.glowIntensity * 100).toInt()}%",
                                subtitle = "Aura radius and photonic atmospheric diffusion",
                                value = currentSettings.glowIntensity,
                                onValueChange = { manager.updateGlow(it) },
                                valueRange = 0.2f..1.0f
                            )
                        }
                        item {
                            CustomSliderCard(
                                title = "Animation Speed: ${"%.1f".format(currentSettings.animationSpeed)}x",
                                subtitle = "Rotation velocity, breathing frequency and orbital cycles",
                                value = currentSettings.animationSpeed,
                                onValueChange = { manager.updateSpeed(it) },
                                valueRange = 0.4f..2.5f
                            )
                        }
                        item {
                            CustomSliderCard(
                                title = "Particle Density: ${currentSettings.particleAmount}",
                                subtitle = "Stardust and quantum node count in galaxy & signature orbs",
                                value = currentSettings.particleAmount.toFloat(),
                                onValueChange = { manager.updateParticles(it.toInt()) },
                                valueRange = 12f..64f
                            )
                        }
                        item {
                            CustomSliderCard(
                                title = "Ring Thickness: ${"%.1f".format(currentSettings.ringThickness)} dp",
                                subtitle = "Geometric orbital lines and neon ring width",
                                value = currentSettings.ringThickness,
                                onValueChange = { manager.updateRingThickness(it) },
                                valueRange = 1.5f..8.0f
                            )
                        }
                        item {
                            CustomSliderCard(
                                title = "Orb Opacity: ${(currentSettings.opacity * 100).toInt()}%",
                                subtitle = "Alpha transparency against background atmosphere",
                                value = currentSettings.opacity,
                                onValueChange = { manager.updateOpacity(it) },
                                valueRange = 0.4f..1.0f
                            )
                        }
                        item {
                            // Orb Screen Position
                            Surface(
                                color = CardDark,
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.dp, BorderSlate),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text("Vertical Screen Position", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                    Text("Anchor the Orb in center or offset for transcript room", color = TextSecondary, fontSize = 11.sp)
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OrbPosition.values().forEach { pos ->
                                            val isSel = currentSettings.position == pos
                                            Button(
                                                onClick = { manager.updatePosition(pos) },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = if (isSel) PrimaryBlue else BgDark
                                                ),
                                                shape = RoundedCornerShape(12.dp),
                                                border = BorderStroke(1.dp, if (isSel) PrimaryBlue else BorderSlate),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Text(
                                                    pos.displayName,
                                                    fontSize = 11.sp,
                                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                                    color = if (isSel) Color.White else TextSecondary
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    2 -> {
                        // ---------------------------------------------
                        // TAB 2: THEME COLOR & BACKGROUND
                        // ---------------------------------------------
                        item {
                            Surface(
                                color = CardDark,
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.dp, BorderSlate),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text("Color Theme Palette", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                    Text("Applied to IDLE state, ambient auras and edge light", color = TextSecondary, fontSize = 11.sp)
                                    Spacer(modifier = Modifier.height(12.dp))

                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        items(OrbThemeColor.values().toList()) { theme ->
                                            val isSel = currentSettings.themeColor == theme
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                modifier = Modifier.clickable { manager.updateThemeColor(theme) }
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(52.dp)
                                                        .clip(CircleShape)
                                                        .background(theme.primaryColor)
                                                        .border(
                                                            if (isSel) 3.dp else 1.dp,
                                                            if (isSel) Color.White else BorderSlate,
                                                            CircleShape
                                                        )
                                                )
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    theme.displayName,
                                                    color = if (isSel) TextPrimary else TextSecondary,
                                                    fontSize = 10.sp,
                                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        item {
                            Surface(
                                color = CardDark,
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.dp, BorderSlate),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text("Background Atmosphere", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                    Text("Custom animated backdrop rendered behind MYRA Orb", color = TextSecondary, fontSize = 11.sp)
                                    Spacer(modifier = Modifier.height(10.dp))

                                    BackgroundStyle.values().forEach { bg ->
                                        val isSel = currentSettings.backgroundStyle == bg
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp)
                                                .clickable { manager.updateBackgroundStyle(bg) },
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                bg.displayName,
                                                color = if (isSel) PrimaryBlue else TextPrimary,
                                                fontSize = 13.sp,
                                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal
                                            )
                                            RadioButton(
                                                selected = isSel,
                                                onClick = { manager.updateBackgroundStyle(bg) },
                                                colors = RadioButtonDefaults.colors(selectedColor = PrimaryBlue)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        item {
                            CustomSliderCard(
                                title = "Background Animation Speed: ${"%.1f".format(currentSettings.backgroundSpeed)}x",
                                subtitle = "Velocity of shifting mesh, stardust drift and auroral waves",
                                value = currentSettings.backgroundSpeed,
                                onValueChange = { manager.updateBackgroundSpeed(it) },
                                valueRange = 0.3f..2.5f
                            )
                        }

                        item {
                            CustomSliderCard(
                                title = "Background Opacity: ${(currentSettings.backgroundOpacity * 100).toInt()}%",
                                subtitle = "Blend transparency between backdrop and matte dark layer",
                                value = currentSettings.backgroundOpacity,
                                onValueChange = { manager.updateBackgroundOpacity(it) },
                                valueRange = 0.2f..1.0f
                            )
                        }
                    }

                    3 -> {
                        // ---------------------------------------------
                        // TAB 3: FULL SCREEN EDGE SPEAKING EFFECT
                        // ---------------------------------------------
                        item {
                            Surface(
                                color = CardDark,
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.dp, BorderSlate),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text("Full-Screen Edge Speaking Effect", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                    Text(
                                        "Illuminates phone screen borders with flowing light, waveforms & glowing aurora when MYRA speaks or listens.",
                                        color = TextSecondary,
                                        fontSize = 11.sp
                                    )
                                    Spacer(modifier = Modifier.height(14.dp))

                                    EdgeEffectMode.values().forEach { mode ->
                                        val isSel = currentSettings.edgeEffectMode == mode
                                        Surface(
                                            color = if (isSel) PrimaryBlue.copy(alpha = 0.15f) else BgDark,
                                            shape = RoundedCornerShape(12.dp),
                                            border = BorderStroke(1.dp, if (isSel) PrimaryBlue else BorderSlate),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp)
                                                .clickable { manager.updateEdgeEffect(mode) }
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(12.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Column {
                                                    Text(mode.displayName, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                                    Text(
                                                        when (mode) {
                                                            EdgeEffectMode.OFF -> "Disable all perimeter border lighting"
                                                            EdgeEffectMode.BOTTOM_ONLY -> "Sleek flowing equalizer beam on bottom border"
                                                            EdgeEffectMode.LEFT_RIGHT -> "Dual lateral acoustic wave bars on sides"
                                                            EdgeEffectMode.TOP_BOTTOM -> "Top notification edge & bottom speech bar"
                                                            EdgeEffectMode.ALL_4_SIDES -> "Full 360° perimeter illumination & flowing light"
                                                        },
                                                        color = TextSecondary,
                                                        fontSize = 11.sp
                                                    )
                                                }
                                                RadioButton(
                                                    selected = isSel,
                                                    onClick = { manager.updateEdgeEffect(mode) },
                                                    colors = RadioButtonDefaults.colors(selectedColor = PrimaryBlue)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        item {
                            CustomSliderCard(
                                title = "Edge Glow Luminance: ${(currentSettings.edgeGlowIntensity * 100).toInt()}%",
                                subtitle = "Brightness of border equalizer bars and traveling laser beams",
                                value = currentSettings.edgeGlowIntensity,
                                onValueChange = { manager.updateEdgeGlow(it) },
                                valueRange = 0.2f..1.0f
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CustomSliderCard(
    title: String,
    subtitle: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>
) {
    Surface(
        color = CardDark,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, BorderSlate),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(title, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = TextSecondary, fontSize = 11.sp)
            Spacer(modifier = Modifier.height(6.dp))
            Slider(
                value = value,
                onValueChange = onValueChange,
                valueRange = valueRange,
                colors = SliderDefaults.colors(
                    thumbColor = LightBlue,
                    activeTrackColor = PrimaryBlue,
                    inactiveTrackColor = BorderSlate
                )
            )
        }
    }
}
