package com.myra.assistant.ui.orb

import androidx.compose.ui.graphics.Color

/**
 * 5 Premium Architectural Orb Styles for MYRA Assistant.
 * Each style has a completely unique rendering algorithm, physical simulation,
 * glass/plasma effects, and voice state reactivity.
 */
enum class OrbStyle(val displayName: String, val subtitle: String) {
    LIQUID_GLASS_PEARL(
        "Liquid Glass Pearl",
        "Luminescent iridescent nacre core, viscous fluid surface refraction & specular glass rim"
    ),
    AURORA_CRYSTAL(
        "Aurora Crystal",
        "Faceted geometric quartz prism, dispersive chromatic refraction & undulating boreal light curtains"
    ),
    GOLDEN_EMBER(
        "Golden Ember",
        "Molten solar plasma core, rising convective thermal sparks & breathing magma corona"
    ),
    OCEAN_SPIRIT(
        "Ocean Spirit",
        "Deep abyss bioluminescence, concentric oceanic tidal waves & swirling vortex currents"
    ),
    ROSE_QUARTZ(
        "Rose Quartz",
        "Translucent crystalline rose gem, internal starlight asterism & velvet pink refraction"
    )
}

/**
 * Visual Color Palettes for Orbs & Screen Effects.
 */
enum class OrbThemeColor(
    val displayName: String,
    val primaryColor: Color,
    val secondaryColor: Color,
    val glowColor: Color
) {
    MYRA_SIGNATURE("MYRA Electric", Color(0xFF00E5FF), Color(0xFF7C4DFF), Color(0xFF00B0FF)),
    CYAN_BLUE("Cyan Fusion", Color(0xFF00F0FF), Color(0xFF0072FF), Color(0xFF38BDF8)),
    NEON_PURPLE("Neon Violet", Color(0xFFE040FB), Color(0xFF7B1FA2), Color(0xFFF472B6)),
    CYBER_AMBER("Cyber Amber", Color(0xFFFFAB00), Color(0xFFFF6D00), Color(0xFFFBBF24)),
    EMERALD_GLOW("Emerald Aura", Color(0xFF00E676), Color(0xFF00897B), Color(0xFF34D399)),
    ROSE_GOLD("Rose Quantum", Color(0xFFFF4081), Color(0xFFC2185B), Color(0xFFFB7185))
}

/**
 * Customizable Background Atmosphere styles.
 */
enum class BackgroundStyle(val displayName: String) {
    SOLID("Deep Matte Solid"),
    GRADIENT("Vibrant Radial Gradient"),
    ANIMATED_GRADIENT("Flowing Chromatic Mesh"),
    PARTICLES("Drifting Quantum Stardust"),
    GALAXY("Deep Space Nebula & Stars"),
    AURORA("Sweeping Atmospheric Aurora"),
    DIGITAL("Cyber Matrix Grid"),
    MYRA_SIGNATURE("MYRA Signature Cosmic Grid")
}

/**
 * Full Screen Edge Speaking/Listening Effect Placement.
 */
enum class EdgeEffectMode(val displayName: String) {
    OFF("Off"),
    BOTTOM_ONLY("Bottom Only"),
    LEFT_RIGHT("Left + Right Sides"),
    TOP_BOTTOM("Top + Bottom Edges"),
    ALL_4_SIDES("All 4 Screen Borders")
}

/**
 * Orb Vertical Positioning.
 */
enum class OrbPosition(val displayName: String, val yOffsetFactor: Float) {
    TOP_THIRD("Top Third", -0.15f),
    CENTER("Absolute Center", 0.0f),
    BOTTOM_OFFSET("Lower Offset", 0.12f)
}

/**
 * Master Customization Model for MYRA Orb, Edge Effects, and Screen Atmosphere.
 */
data class OrbSettings(
    val style: OrbStyle = OrbStyle.LIQUID_GLASS_PEARL,
    val sizeDp: Float = 240f,
    val glowIntensity: Float = 0.85f,
    val animationSpeed: Float = 1.0f,
    val particleAmount: Int = 24,
    val ringThickness: Float = 3.5f,
    val opacity: Float = 0.95f,
    val position: OrbPosition = OrbPosition.CENTER,
    val themeColor: OrbThemeColor = OrbThemeColor.MYRA_SIGNATURE,
    val backgroundStyle: BackgroundStyle = BackgroundStyle.MYRA_SIGNATURE,
    val backgroundSpeed: Float = 1.0f,
    val backgroundOpacity: Float = 0.85f,
    val edgeEffectMode: EdgeEffectMode = EdgeEffectMode.ALL_4_SIDES,
    val edgeGlowIntensity: Float = 0.8f
)
