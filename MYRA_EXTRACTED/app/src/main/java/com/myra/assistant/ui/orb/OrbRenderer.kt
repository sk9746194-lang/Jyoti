package com.myra.assistant.ui.orb

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import com.myra.assistant.data.AssistantState
import kotlin.math.*

/**
 * Universal High-Performance Canvas Orb Renderer for MYRA Assistant.
 *
 * Implements 5 visually distinct, premium architectural styles:
 * 1. LIQUID_GLASS_PEARL - Iridescent nacre core, viscous fluid surface tension, specular glass rim
 * 2. AURORA_CRYSTAL    - Faceted geometric quartz prism, dispersive refraction, undulating boreal ribbons
 * 3. GOLDEN_EMBER       - Molten solar plasma core, rising convective thermal sparks, magma corona
 * 4. OCEAN_SPIRIT       - Deep abyss bioluminescence, concentric oceanic tidal waves, swirling vortex
 * 5. ROSE_QUARTZ        - Translucent crystalline rose gem, authentic 6-ray star asterism, velvet pink refraction
 */
@Composable
fun OrbRenderer(
    state: AssistantState,
    audioLevel: Float,
    settings: OrbSettings,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val speedMultiplier = settings.animationSpeed.coerceIn(0.2f, 3.0f)

    // Master Infinite Transition for 60FPS fluid visual animations
    val infiniteTransition = rememberInfiniteTransition(label = "myra_premium_orb_anim")

    // 1. Primary rotation progression (0 to 360 deg)
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween((7500 / speedMultiplier).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    // 2. Counter-rotation progression
    val counterRotation by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween((9500 / speedMultiplier).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "counter_rotation"
    )

    // 3. Smooth organic breathing pulse
    val breathingPulse by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween((1500 / speedMultiplier).toInt(), easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathing"
    )

    // 4. Harmonic wave progression for fluid dynamics and waves
    val waveProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween((3200 / speedMultiplier).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave_progress"
    )

    // Resolve color pair based on voice state and user theme
    val (coreColor, glowColor) = resolveColors(state, settings.themeColor)

    val effectiveAudio = if (state == AssistantState.SPEAKING || state == AssistantState.LISTENING) {
        audioLevel.coerceIn(0f, 1f)
    } else {
        0.05f
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
    ) {
        Canvas(
            modifier = Modifier
                .size(settings.sizeDp.dp)
                .clickable { onClick() }
        ) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val baseRadius = (size.minDimension / 2f) * 0.72f * breathingPulse * (1f + effectiveAudio * 0.28f)

            when (settings.style) {
                OrbStyle.LIQUID_GLASS_PEARL -> drawLiquidGlassPearlOrb(
                    center, baseRadius, coreColor, glowColor, settings, rotation, waveProgress, effectiveAudio, state
                )
                OrbStyle.AURORA_CRYSTAL -> drawAuroraCrystalOrb(
                    center, baseRadius, coreColor, glowColor, settings, rotation, counterRotation, waveProgress, effectiveAudio, state
                )
                OrbStyle.GOLDEN_EMBER -> drawGoldenEmberOrb(
                    center, baseRadius, coreColor, glowColor, settings, rotation, waveProgress, effectiveAudio, state
                )
                OrbStyle.OCEAN_SPIRIT -> drawOceanSpiritOrb(
                    center, baseRadius, coreColor, glowColor, settings, rotation, counterRotation, waveProgress, effectiveAudio, state
                )
                OrbStyle.ROSE_QUARTZ -> drawRoseQuartzOrb(
                    center, baseRadius, coreColor, glowColor, settings, rotation, waveProgress, effectiveAudio, state
                )
            }

            // Real-time Audio-Reactive Waveform & Radial Equalizer Halo (Microphone Reactivity)
            if (state == AssistantState.LISTENING || state == AssistantState.SPEAKING || effectiveAudio > 0.05f) {
                drawAudioReactiveWaveform(
                    center = center,
                    baseRadius = baseRadius,
                    coreColor = coreColor,
                    glowColor = glowColor,
                    waveProgress = waveProgress,
                    audioLevel = effectiveAudio,
                    state = state
                )
            }
        }
    }
}

// =============================================================================
// 1. LIQUID GLASS PEARL
// =============================================================================
private fun DrawScope.drawLiquidGlassPearlOrb(
    center: Offset,
    radius: Float,
    core: Color,
    glow: Color,
    settings: OrbSettings,
    rotation: Float,
    waveProgress: Float,
    audio: Float,
    state: AssistantState
) {
    val glowRadius = radius * (1.35f + 0.35f * settings.glowIntensity)

    // 1. Soft Pearlescent Iridescent Outer Atmosphere
    val outerGlowColor = when (state) {
        AssistantState.LISTENING -> Color(0xFFFF5252)
        AssistantState.THINKING -> Color(0xFFFFD54F)
        AssistantState.SPEAKING -> Color(0xFF69F0AE)
        else -> Color(0xFFE0F2FE)
    }

    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                outerGlowColor.copy(alpha = 0.45f * settings.glowIntensity * settings.opacity),
                glow.copy(alpha = 0.20f * settings.glowIntensity * settings.opacity),
                Color.Transparent
            ),
            center = center,
            radius = glowRadius
        ),
        radius = glowRadius,
        center = center
    )

    // 2. Translucent Glass Shell with Refraction Ring
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.05f),
                Color(0xFFE2E8F0).copy(alpha = 0.22f * settings.opacity),
                Color(0xFF94A3B8).copy(alpha = 0.40f * settings.opacity)
            ),
            center = Offset(center.x - radius * 0.2f, center.y - radius * 0.2f),
            radius = radius
        ),
        radius = radius,
        center = center
    )
    drawCircle(
        brush = Brush.sweepGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.75f * settings.opacity),
                Color(0xFFBAE6FD).copy(alpha = 0.45f * settings.opacity),
                Color(0xFFDDD6FE).copy(alpha = 0.65f * settings.opacity),
                Color(0xFFFCE7F3).copy(alpha = 0.50f * settings.opacity),
                Color.White.copy(alpha = 0.75f * settings.opacity)
            ),
            center = center
        ),
        radius = radius,
        center = center,
        style = Stroke(width = settings.ringThickness * 1.3f)
    )

    // 3. Harmonic Viscous Liquid Pearl Core (Deforming Fourier boundary)
    val steps = 72
    val angleStep = (2 * Math.PI / steps).toFloat()
    val pearlPath = Path()
    val pearlRadius = radius * 0.75f

    for (i in 0..steps) {
        val angle = i * angleStep
        val h1 = sin(angle * 3f + waveProgress) * 0.045f
        val h2 = cos(angle * 4f - waveProgress * 0.8f) * 0.035f
        val audioMod = sin(angle * 5f + waveProgress * 1.5f) * (audio * 0.22f)
        val r = pearlRadius * (1f + h1 + h2 + audioMod)

        val px = center.x + r * cos(angle)
        val py = center.y + r * sin(angle)
        if (i == 0) pearlPath.moveTo(px, py) else pearlPath.lineTo(px, py)
    }
    pearlPath.close()

    // Iridescent Pearl Fluid Fill
    val pearlCenterColor = when (state) {
        AssistantState.LISTENING -> Color(0xFFFFCCD5)
        AssistantState.THINKING -> Color(0xFFFEF08A)
        AssistantState.SPEAKING -> Color(0xFFA7F3D0)
        else -> Color.White
    }

    drawPath(
        path = pearlPath,
        brush = Brush.radialGradient(
            colors = listOf(
                pearlCenterColor,
                Color(0xFFF8FAFC),
                Color(0xFFE0E7FF).copy(alpha = 0.95f),
                Color(0xFFC7D2FE).copy(alpha = 0.90f),
                Color(0xFF0F172A).copy(alpha = 0.85f)
            ),
            center = Offset(center.x - radius * 0.18f, center.y - radius * 0.18f),
            radius = pearlRadius * 1.05f
        )
    )

    // 4. Internal Pulsing Caustic Light Core
    val nucleusPulse = (1f + audio * 0.45f) * (if (state == AssistantState.THINKING) 1.15f else 1.0f)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.95f),
                core.copy(alpha = 0.75f),
                Color.Transparent
            ),
            center = center,
            radius = pearlRadius * 0.42f * nucleusPulse
        ),
        radius = pearlRadius * 0.42f * nucleusPulse,
        center = center
    )

    // 5. Dual Curved Specular Highlights (Optical Curved Glass Reflection)
    drawArc(
        brush = Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.92f * settings.opacity),
                Color.White.copy(alpha = 0.15f * settings.opacity)
            ),
            start = Offset(center.x - radius * 0.8f, center.y - radius * 0.8f),
            end = Offset(center.x, center.y)
        ),
        startAngle = 205f,
        sweepAngle = 75f,
        useCenter = false,
        style = Stroke(width = (radius * 0.08f), cap = StrokeCap.Round),
        topLeft = Offset(center.x - radius * 0.82f, center.y - radius * 0.82f),
        size = Size(radius * 1.64f, radius * 1.64f)
    )

    // Secondary subtle lower-right bounce meniscus reflection
    drawArc(
        color = Color.White.copy(alpha = 0.35f * settings.opacity),
        startAngle = 35f,
        sweepAngle = 55f,
        useCenter = false,
        style = Stroke(width = (radius * 0.045f), cap = StrokeCap.Round),
        topLeft = Offset(center.x - radius * 0.86f, center.y - radius * 0.86f),
        size = Size(radius * 1.72f, radius * 1.72f)
    )
}

// =============================================================================
// 2. AURORA CRYSTAL
// =============================================================================
private fun DrawScope.drawAuroraCrystalOrb(
    center: Offset,
    radius: Float,
    core: Color,
    glow: Color,
    settings: OrbSettings,
    rotation: Float,
    counterRotation: Float,
    waveProgress: Float,
    audio: Float,
    state: AssistantState
) {
    val glowRadius = radius * (1.3f + 0.35f * settings.glowIntensity)

    // 1. Aurora Atmospheric Nebula
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFF10B981).copy(alpha = 0.40f * settings.glowIntensity),
                Color(0xFF06B6D4).copy(alpha = 0.30f * settings.glowIntensity),
                Color(0xFF8B5CF6).copy(alpha = 0.20f * settings.glowIntensity),
                Color.Transparent
            ),
            center = center,
            radius = glowRadius
        ),
        radius = glowRadius,
        center = center
    )

    // 2. Faceted Octagonal Crystal Geometry with 3D Rotation
    val numSides = 8
    val vertices = mutableListOf<Offset>()
    val innerVertices = mutableListOf<Offset>()
    val rotRad = Math.toRadians((rotation * 0.4f).toDouble()).toFloat()
    val countRotRad = Math.toRadians((counterRotation * 0.3f).toDouble()).toFloat()

    for (i in 0 until numSides) {
        val angle = i * (2f * Math.PI.toFloat() / numSides) + rotRad
        val r = radius * (0.92f + 0.06f * sin(angle * 2f + waveProgress))
        vertices.add(Offset(center.x + r * cos(angle), center.y + r * sin(angle)))

        val innerAngle = i * (2f * Math.PI.toFloat() / numSides) + countRotRad
        val innerR = radius * 0.48f * (1f + audio * 0.25f)
        innerVertices.add(Offset(center.x + innerR * cos(innerAngle), center.y + innerR * sin(innerAngle)))
    }

    // Draw Prismatic Facet Planes
    for (i in 0 until numSides) {
        val next = (i + 1) % numSides
        val facetPath = Path().apply {
            moveTo(center.x, center.y)
            lineTo(innerVertices[i].x, innerVertices[i].y)
            lineTo(vertices[i].x, vertices[i].y)
            lineTo(vertices[next].x, vertices[next].y)
            close()
        }

        val facetColor = when (i % 3) {
            0 -> Color(0xFF10B981) // Emerald
            1 -> Color(0xFF06B6D4) // Cyan
            else -> Color(0xFFA855F7) // Purple
        }

        drawPath(
            path = facetPath,
            brush = Brush.linearGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.35f * settings.opacity),
                    facetColor.copy(alpha = 0.28f * settings.opacity),
                    Color(0xFF0F172A).copy(alpha = 0.55f)
                ),
                start = innerVertices[i],
                end = vertices[next]
            )
        )
    }

    // 3. Undulating Internal Boreal Aurora Light Ribbons
    val ribbonSteps = 36
    val ribbonPath = Path()
    val ribbonY = center.y
    val rSpan = radius * 0.75f

    for (i in 0..ribbonSteps) {
        val t = (i.toFloat() / ribbonSteps) * 2f - 1f // -1 to +1
        val px = center.x + t * rSpan
        val py = ribbonY + sin(t * 3.5f + waveProgress) * (radius * 0.28f * (1f + audio * 0.6f))
        if (i == 0) ribbonPath.moveTo(px, py) else ribbonPath.lineTo(px, py)
    }

    drawPath(
        path = ribbonPath,
        brush = Brush.horizontalGradient(
            colors = listOf(
                Color(0xFF10B981).copy(alpha = 0.1f),
                Color(0xFF34D399).copy(alpha = 0.85f * settings.opacity),
                Color(0xFF38BDF8).copy(alpha = 0.90f * settings.opacity),
                Color(0xFFC084FC).copy(alpha = 0.85f * settings.opacity),
                Color(0xFFF472B6).copy(alpha = 0.1f)
            )
        ),
        style = Stroke(width = (4f + audio * 5f).dp.toPx(), cap = StrokeCap.Round)
    )

    // 4. Sharp Crystal Facet Seams (Outer & Inner Edges)
    val outerPoly = Path().apply {
        vertices.forEachIndexed { idx, pt -> if (idx == 0) moveTo(pt.x, pt.y) else lineTo(pt.x, pt.y) }
        close()
    }
    drawPath(
        path = outerPoly,
        brush = Brush.sweepGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.8f),
                Color(0xFF67E8F9),
                Color(0xFFE879F9),
                Color.White.copy(alpha = 0.8f)
            ),
            center = center
        ),
        style = Stroke(width = settings.ringThickness * 1.1f)
    )

    // Radial spokes connecting core to vertices
    vertices.forEachIndexed { idx, pt ->
        drawLine(
            color = Color.White.copy(alpha = 0.35f * settings.opacity),
            start = innerVertices[idx],
            end = pt,
            strokeWidth = 1.2.dp.toPx()
        )
    }

    // 5. Prismatic Sparkle Glints at Outer Vertices
    vertices.forEachIndexed { idx, pt ->
        if (idx % 2 == 0 || audio > 0.3f) {
            val glintSize = (4f + audio * 5f).dp.toPx()
            drawLine(
                color = Color.White,
                start = Offset(pt.x - glintSize, pt.y),
                end = Offset(pt.x + glintSize, pt.y),
                strokeWidth = 1.8.dp.toPx(),
                cap = StrokeCap.Round
            )
            drawLine(
                color = Color.White,
                start = Offset(pt.x, pt.y - glintSize),
                end = Offset(pt.x, pt.y + glintSize),
                strokeWidth = 1.8.dp.toPx(),
                cap = StrokeCap.Round
            )
        }
    }
}

// =============================================================================
// 3. GOLDEN EMBER
// =============================================================================
private fun DrawScope.drawGoldenEmberOrb(
    center: Offset,
    radius: Float,
    core: Color,
    glow: Color,
    settings: OrbSettings,
    rotation: Float,
    waveProgress: Float,
    audio: Float,
    state: AssistantState
) {
    val glowRadius = radius * (1.4f + 0.4f * settings.glowIntensity)

    // 1. Radiant Solar Magma Atmosphere
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFFF59E0B).copy(alpha = 0.45f * settings.glowIntensity),
                Color(0xFFDC2626).copy(alpha = 0.30f * settings.glowIntensity),
                Color(0xFF78350F).copy(alpha = 0.15f * settings.glowIntensity),
                Color.Transparent
            ),
            center = center,
            radius = glowRadius
        ),
        radius = glowRadius,
        center = center
    )

    // 2. Molten Plasma Core with Turbulent Surface Tension
    val steps = 60
    val angleStep = (2 * Math.PI / steps).toFloat()
    val plasmaPath = Path()
    val coreR = radius * 0.72f

    for (i in 0..steps) {
        val angle = i * angleStep
        val wave1 = sin(angle * 4f + waveProgress * 1.4f) * 0.055f
        val wave2 = cos(angle * 6f - waveProgress) * 0.035f
        val audioFlare = sin(angle * 8f + waveProgress * 2f) * (audio * 0.26f)
        val r = coreR * (1f + wave1 + wave2 + audioFlare)

        val px = center.x + r * cos(angle)
        val py = center.y + r * sin(angle)
        if (i == 0) plasmaPath.moveTo(px, py) else plasmaPath.lineTo(px, py)
    }
    plasmaPath.close()

    drawPath(
        path = plasmaPath,
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFFFFFBEB), // White-hot center
                Color(0xFFFEF08A), // Vibrant yellow
                Color(0xFFF59E0B), // Molten amber
                Color(0xFFB45309), // Copper magma
                Color(0xFF451A03)  // Deep volcanic rim
            ),
            center = Offset(center.x - coreR * 0.15f, center.y - coreR * 0.15f),
            radius = coreR * 1.05f
        )
    )

    // 3. Convective Rising Thermal Sparks / Embers (16 Ember Particles)
    val numEmbers = 16
    for (i in 0 until numEmbers) {
        val seed = i * 137.5f
        val emberPhase = (waveProgress * 0.8f + i * (2f * Math.PI.toFloat() / numEmbers))
        val dist = radius * (0.45f + 0.48f * ((emberPhase / (2f * Math.PI.toFloat())) % 1f)) * (1f + audio * 0.35f)
        val angle = seed + emberPhase * 0.5f

        // Rising convective draft (tendency to float upward)
        val px = center.x + dist * cos(angle)
        val py = center.y + dist * sin(angle) - (emberPhase % 2f) * 12f

        val emberAlpha = (1f - (dist / (radius * 1.1f)).coerceIn(0f, 1f)) * settings.opacity
        val emberSize = (2.2f + (i % 3) * 1.2f + audio * 3f).dp.toPx()

        drawCircle(
            color = if (i % 2 == 0) Color(0xFFFEF08A).copy(alpha = emberAlpha) else Color(0xFFF97316).copy(alpha = emberAlpha),
            radius = emberSize,
            center = Offset(px, py)
        )
    }

    // 4. Coronal Solar Prominences / Magnetic Loops
    rotate(rotation * 0.6f, center) {
        for (flare in 0..2) {
            val flareAngle = flare * 120f
            drawArc(
                brush = Brush.sweepGradient(
                    colors = listOf(
                        Color(0xFFF59E0B).copy(alpha = 0.85f * settings.opacity),
                        Color(0xFFEF4444).copy(alpha = 0.60f * settings.opacity),
                        Color.Transparent
                    ),
                    center = center
                ),
                startAngle = flareAngle,
                sweepAngle = 45f + audio * 30f,
                useCenter = false,
                style = Stroke(width = (2.5f + audio * 3f).dp.toPx(), cap = StrokeCap.Round),
                topLeft = Offset(center.x - radius * 0.95f, center.y - radius * 0.95f),
                size = Size(radius * 1.9f, radius * 1.9f)
            )
        }
    }

    // 5. Specular Golden Rim Sheen
    drawArc(
        color = Color(0xFFFEF3C7).copy(alpha = 0.85f * settings.opacity),
        startAngle = 210f,
        sweepAngle = 60f,
        useCenter = false,
        style = Stroke(width = (radius * 0.065f), cap = StrokeCap.Round),
        topLeft = Offset(center.x - coreR * 0.95f, center.y - coreR * 0.95f),
        size = Size(coreR * 1.9f, coreR * 1.9f)
    )
}

// =============================================================================
// 4. OCEAN SPIRIT
// =============================================================================
private fun DrawScope.drawOceanSpiritOrb(
    center: Offset,
    radius: Float,
    core: Color,
    glow: Color,
    settings: OrbSettings,
    rotation: Float,
    counterRotation: Float,
    waveProgress: Float,
    audio: Float,
    state: AssistantState
) {
    val glowRadius = radius * (1.38f + 0.35f * settings.glowIntensity)

    // 1. Deep Oceanic Abyss Glow
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFF06B6D4).copy(alpha = 0.42f * settings.glowIntensity),
                Color(0xFF0284C7).copy(alpha = 0.28f * settings.glowIntensity),
                Color(0xFF0F172A).copy(alpha = 0.15f),
                Color.Transparent
            ),
            center = center,
            radius = glowRadius
        ),
        radius = glowRadius,
        center = center
    )

    // 2. Concentric Acoustic Tidal Wave Ripples (3 Outward Propagating Rings)
    for (i in 0..2) {
        val fraction = ((waveProgress / (2f * Math.PI.toFloat()) + i * 0.33f) % 1.0f)
        val waveR = radius * (0.42f + fraction * 0.56f) * (1f + audio * 0.3f)
        val waveAlpha = (1f - fraction) * 0.75f * settings.opacity

        drawCircle(
            brush = Brush.sweepGradient(
                colors = listOf(
                    Color(0xFF22D3EE).copy(alpha = waveAlpha),
                    Color(0xFF38BDF8).copy(alpha = waveAlpha * 0.8f),
                    Color(0xFF14B8A6).copy(alpha = waveAlpha * 0.9f),
                    Color(0xFF22D3EE).copy(alpha = waveAlpha)
                ),
                center = center
            ),
            radius = waveR,
            center = center,
            style = Stroke(width = (2.2f + (1f - fraction) * 2f).dp.toPx())
        )
    }

    // 3. Swirling Archimedean Oceanic Vortex Current Ribbon
    rotate(counterRotation * 0.7f, center) {
        val vortexSteps = 48
        val vortexPath = Path()
        val turns = 1.8f

        for (i in 0..vortexSteps) {
            val t = i.toFloat() / vortexSteps
            val theta = t * (turns * 2f * Math.PI.toFloat())
            val r = (radius * 0.18f) + (t * radius * 0.62f) * (1f + audio * 0.2f)
            val px = center.x + r * cos(theta)
            val py = center.y + r * sin(theta)

            if (i == 0) vortexPath.moveTo(px, py) else vortexPath.lineTo(px, py)
        }

        drawPath(
            path = vortexPath,
            brush = Brush.linearGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.9f),
                    Color(0xFF22D3EE).copy(alpha = 0.85f * settings.opacity),
                    Color(0xFF0284C7).copy(alpha = 0.1f)
                ),
                start = center,
                end = Offset(center.x + radius * 0.7f, center.y + radius * 0.7f)
            ),
            style = Stroke(width = (3.2f + audio * 4f).dp.toPx(), cap = StrokeCap.Round)
        )
    }

    // 4. Central Bioluminescent Oceanic Nucleus (Abyssal Eye)
    val eyeRadius = radius * 0.52f * (1f + audio * 0.35f)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color.White,
                Color(0xFF67E8F9),
                Color(0xFF0EA5E9),
                Color(0xFF0369A1),
                Color(0xFF082F49)
            ),
            center = Offset(center.x - eyeRadius * 0.2f, center.y - eyeRadius * 0.2f),
            radius = eyeRadius
        ),
        radius = eyeRadius,
        center = center
    )

    // 5. Orbiting Deep-Sea Bio-Plankton Nodes
    val numSpores = 12
    for (s in 0 until numSpores) {
        val sporeAngle = (s * (2 * Math.PI / numSpores) + Math.toRadians((rotation * 0.8f).toDouble())).toFloat()
        val sporeDist = radius * (0.65f + 0.18f * sin(s * 1.5f + waveProgress))
        val sx = center.x + sporeDist * cos(sporeAngle)
        val sy = center.y + sporeDist * sin(sporeAngle) * 0.65f // Elliptical 3D tilt

        drawCircle(
            color = if (s % 2 == 0) Color(0xFF5EEAD4) else Color.White,
            radius = (2.2f + audio * 3.5f).dp.toPx(),
            center = Offset(sx, sy)
        )
    }

    // 6. Watery Lens Specular Arc
    drawArc(
        brush = Brush.linearGradient(
            colors = listOf(Color.White.copy(alpha = 0.85f * settings.opacity), Color.Transparent),
            start = Offset(center.x - radius * 0.7f, center.y - radius * 0.7f),
            end = center
        ),
        startAngle = 200f,
        sweepAngle = 70f,
        useCenter = false,
        style = Stroke(width = (radius * 0.06f), cap = StrokeCap.Round),
        topLeft = Offset(center.x - eyeRadius * 0.9f, center.y - eyeRadius * 0.9f),
        size = Size(eyeRadius * 1.8f, eyeRadius * 1.8f)
    )
}

// =============================================================================
// 5. ROSE QUARTZ
// =============================================================================
private fun DrawScope.drawRoseQuartzOrb(
    center: Offset,
    radius: Float,
    core: Color,
    glow: Color,
    settings: OrbSettings,
    rotation: Float,
    waveProgress: Float,
    audio: Float,
    state: AssistantState
) {
    val glowRadius = radius * (1.35f + 0.35f * settings.glowIntensity)

    // 1. Velvet Blush & Rose Gold Aura
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFFF472B6).copy(alpha = 0.42f * settings.glowIntensity),
                Color(0xFFFB7185).copy(alpha = 0.28f * settings.glowIntensity),
                Color(0xFFFFF1F2).copy(alpha = 0.12f * settings.glowIntensity),
                Color.Transparent
            ),
            center = center,
            radius = glowRadius
        ),
        radius = glowRadius,
        center = center
    )

    // 2. Translucent Rose Quartz Cabochon Dome
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFFFFF5F7), // Soft crystalline highlight
                Color(0xFFFBCFE8), // Velvet rose petal
                Color(0xFFF472B6), // Deep rich rose
                Color(0xFFBE185D), // Magenta wine depth
                Color(0xFF4C0519)  // Shadow contour
            ),
            center = Offset(center.x - radius * 0.22f, center.y - radius * 0.22f),
            radius = radius * 0.98f
        ),
        radius = radius * 0.78f,
        center = center
    )

    // Polished Glass Outer Rim
    drawCircle(
        brush = Brush.sweepGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.8f * settings.opacity),
                Color(0xFFFDA4AF).copy(alpha = 0.5f * settings.opacity),
                Color(0xFFF472B6).copy(alpha = 0.7f * settings.opacity),
                Color(0xFFFECDD3).copy(alpha = 0.4f * settings.opacity),
                Color.White.copy(alpha = 0.8f * settings.opacity)
            ),
            center = center
        ),
        radius = radius * 0.78f,
        center = center,
        style = Stroke(width = settings.ringThickness * 1.2f)
    )

    // 3. Authentic 6-Ray Star Asterism (Optical Starlight Phenomenon)
    // Star quartz optical reflection consists of three intersecting needle rays at 60° angles
    val asterismLength = radius * 0.74f * (1f + audio * 0.3f)
    val starRot = Math.toRadians((rotation * 0.25f).toDouble()).toFloat()

    for (r in 0..2) {
        val baseAngle = starRot + r * (Math.PI.toFloat() / 3f)

        val start = Offset(
            center.x - asterismLength * cos(baseAngle),
            center.y - asterismLength * sin(baseAngle)
        )
        val end = Offset(
            center.x + asterismLength * cos(baseAngle),
            center.y + asterismLength * sin(baseAngle)
        )

        // Starlight ray gradient with brilliant center and tapered needle tips
        drawLine(
            brush = Brush.linearGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.05f),
                    Color.White.copy(alpha = 0.88f * settings.opacity),
                    Color(0xFFFDA4AF).copy(alpha = 0.92f * settings.opacity),
                    Color.White.copy(alpha = 0.88f * settings.opacity),
                    Color.White.copy(alpha = 0.05f)
                ),
                start = start,
                end = end
            ),
            start = start,
            end = end,
            strokeWidth = (2.6f + audio * 3.5f).dp.toPx(),
            cap = StrokeCap.Round
        )
    }

    // Central Asterism Star Nexus (Brilliant Starlight Core)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color.White,
                Color(0xFFFECDD3).copy(alpha = 0.8f),
                Color.Transparent
            ),
            center = center,
            radius = (radius * 0.18f * (1f + audio * 0.4f))
        ),
        radius = (radius * 0.18f * (1f + audio * 0.4f)),
        center = center
    )

    // 4. Curved Specular Dome Highlight (Glazed Gem Reflection)
    drawArc(
        brush = Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.90f * settings.opacity),
                Color.White.copy(alpha = 0.20f * settings.opacity)
            ),
            start = Offset(center.x - radius * 0.6f, center.y - radius * 0.6f),
            end = center
        ),
        startAngle = 205f,
        sweepAngle = 70f,
        useCenter = false,
        style = Stroke(width = (radius * 0.07f), cap = StrokeCap.Round),
        topLeft = Offset(center.x - radius * 0.72f, center.y - radius * 0.72f),
        size = Size(radius * 1.44f, radius * 1.44f)
    )
}

/**
 * Resolves color palette seamlessly between Voice Assistant States and Theme Color.
 */
private fun resolveColors(state: AssistantState, theme: OrbThemeColor): Pair<Color, Color> {
    return when (state) {
        AssistantState.IDLE -> theme.primaryColor to theme.secondaryColor
        AssistantState.LISTENING -> Color(0xFFFF1744) to Color(0xFFFF5252)
        AssistantState.THINKING -> Color(0xFFFFB300) to Color(0xFFFFD54F)
        AssistantState.SPEAKING -> Color(0xFF00E676) to Color(0xFF69F0AE)
        AssistantState.ERROR -> Color(0xFFD50000) to Color(0xFFFF1744)
        AssistantState.WAKE_WORD -> Color(0xFFFFFFFF) to theme.primaryColor
    }
}

/**
 * High-performance real-time audio waveform and radial equalizer animation.
 * Reacts directly to microphone decibel levels during speech recognition.
 */
private fun DrawScope.drawAudioReactiveWaveform(
    center: Offset,
    baseRadius: Float,
    coreColor: Color,
    glowColor: Color,
    waveProgress: Float,
    audioLevel: Float,
    state: AssistantState
) {
    val isListening = state == AssistantState.LISTENING
    val primary = if (isListening) Color(0xFFF43F5E) else glowColor
    val secondary = if (isListening) Color(0xFFFDA4AF) else coreColor

    // 1. Dual sinusoidal audio waveform rings around orb perimeter
    val wavePath = Path()
    val steps = 60
    val waveRadius = baseRadius * 1.12f

    for (i in 0..steps) {
        val angle = (i.toFloat() / steps) * (2f * Math.PI.toFloat())
        val waveOffset = sin(angle * 6f + waveProgress * 2f) * (baseRadius * 0.05f * (1f + audioLevel * 2.5f))
        val r = waveRadius + waveOffset
        val x = center.x + cos(angle) * r
        val y = center.y + sin(angle) * r

        if (i == 0) wavePath.moveTo(x, y) else wavePath.lineTo(x, y)
    }
    wavePath.close()

    drawPath(
        path = wavePath,
        color = primary,
        style = Stroke(width = (2.5f + audioLevel * 3f).dp.toPx(), cap = StrokeCap.Round)
    )

    // 2. Real-time 36-band radial equalizer halo
    val numBars = 36
    for (i in 0 until numBars) {
        val angle = (i.toFloat() / numBars) * (2f * Math.PI.toFloat()) + (waveProgress * 0.2f)
        val harmonicPhase = sin(i * 0.6f + waveProgress * 3f) * 0.5f + 0.5f
        val dynamicHeight = (audioLevel * 30f * harmonicPhase) + (if (audioLevel > 0.1f) 4f else 2f)

        val innerDist = baseRadius * 1.16f
        val outerDist = innerDist + dynamicHeight.dp.toPx()

        val start = Offset(
            center.x + cos(angle) * innerDist,
            center.y + sin(angle) * innerDist
        )
        val end = Offset(
            center.x + cos(angle) * outerDist,
            center.y + sin(angle) * outerDist
        )

        drawLine(
            brush = Brush.linearGradient(
                colors = listOf(primary.copy(alpha = 0.5f), secondary),
                start = start,
                end = end
            ),
            start = start,
            end = end,
            strokeWidth = 2.5f.dp.toPx(),
            cap = StrokeCap.Round
        )
    }
}
