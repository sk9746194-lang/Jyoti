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
 * Universal High-Performance Canvas Orb Renderer.
 * Supports 10 distinct architectural styles with real-time audio-reactivity and state transitions.
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

    // Master Infinite Transition for fluid 60FPS animations
    val infiniteTransition = rememberInfiniteTransition(label = "myra_orb_anim")

    // 1. Rotation progression (0 to 360 deg)
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween((7000 / speedMultiplier).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    // 2. Counter-rotation progression
    val counterRotation by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween((9000 / speedMultiplier).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "counter_rotation"
    )

    // 3. Smooth Breathing Pulse
    val breathingPulse by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween((1400 / speedMultiplier).toInt(), easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathing"
    )

    // 4. Wave progression for fluid and waves
    val waveProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween((3000 / speedMultiplier).toInt(), easing = LinearEasing),
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
                OrbStyle.QUANTUM_FLUX, OrbStyle.ENERGY_CORE, OrbStyle.MYRA_SIGNATURE -> drawEnergyCoreOrb(
                    center, baseRadius, coreColor, glowColor, settings, rotation, waveProgress, effectiveAudio
                )
                OrbStyle.COSMIC_SINGULARITY, OrbStyle.GALAXY_PARTICLE, OrbStyle.AURORA -> drawGalaxyParticleOrb(
                    center, baseRadius, coreColor, glowColor, settings, rotation, effectiveAudio
                )
                OrbStyle.NEURAL_LATTICE, OrbStyle.GLASS, OrbStyle.WAVE -> drawGlassOrb(
                    center, baseRadius, coreColor, glowColor, settings, waveProgress, effectiveAudio
                )
                OrbStyle.LIQUID_QUICKSILVER, OrbStyle.SIRI_FLUID -> drawSiriFluidOrb(
                    center, baseRadius, coreColor, glowColor, settings, waveProgress, effectiveAudio
                )
                OrbStyle.CELESTIAL_VORTEX, OrbStyle.NEON_RING, OrbStyle.PULSE -> drawNeonRingOrb(
                    center, baseRadius, coreColor, glowColor, settings, rotation, counterRotation, effectiveAudio
                )
                OrbStyle.CLASSIC_GLOW -> drawClassicGlowOrb(
                    center, baseRadius, coreColor, glowColor, settings, effectiveAudio
                )
            }

            // Real-Time Audio-Reactive Waveform & Radial Equalizer Halo (Microphone Reactivity)
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

// -------------------------------------------------------------
// 1. CLASSIC GLOW ORB
// -------------------------------------------------------------
private fun DrawScope.drawClassicGlowOrb(
    center: Offset,
    radius: Float,
    core: Color,
    glow: Color,
    settings: OrbSettings,
    audio: Float
) {
    val glowRadius = radius * (1.3f + 0.35f * settings.glowIntensity)

    // Outer Diffuse Glow
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(glow.copy(alpha = 0.45f * settings.glowIntensity * settings.opacity), Color.Transparent),
            center = center,
            radius = glowRadius
        ),
        radius = glowRadius,
        center = center
    )

    // Mid Atmosphere
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(glow.copy(alpha = 0.7f * settings.opacity), core.copy(alpha = 0.25f)),
            center = center,
            radius = radius * 1.12f
        ),
        radius = radius * 1.12f,
        center = center
    )

    // Core Solid Gradient
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color.White, core, Color(0xFF0A0F1D)),
            center = Offset(center.x - radius * 0.22f, center.y - radius * 0.22f),
            radius = radius
        ),
        radius = radius,
        center = center
    )

    // Specular Rim Arc
    drawArc(
        color = Color.White.copy(alpha = 0.5f * settings.opacity),
        startAngle = 200f,
        sweepAngle = 70f,
        useCenter = false,
        style = Stroke(width = settings.ringThickness * 1.2f),
        topLeft = Offset(center.x - radius * 0.9f, center.y - radius * 0.9f),
        size = Size(radius * 1.8f, radius * 1.8f)
    )
}

// -------------------------------------------------------------
// 2. SIRI-STYLE FLUID ORB
// -------------------------------------------------------------
private fun DrawScope.drawSiriFluidOrb(
    center: Offset,
    radius: Float,
    core: Color,
    glow: Color,
    settings: OrbSettings,
    waveProgress: Float,
    audio: Float
) {
    // Outer halo
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(glow.copy(alpha = 0.4f * settings.glowIntensity), Color.Transparent),
            center = center,
            radius = radius * 1.4f
        ),
        radius = radius * 1.4f,
        center = center
    )

    val steps = 72
    val angleStep = (2 * Math.PI / steps).toFloat()

    // 2 overlapping fluid organic blobs with shifting harmonics
    for (layer in 0..1) {
        val path = Path()
        val phaseOffset = if (layer == 0) waveProgress else -waveProgress * 1.3f
        val layerScale = if (layer == 0) 1.0f else 0.88f
        val colorSweep = if (layer == 0) core else glow

        for (i in 0..steps) {
            val angle = i * angleStep
            val harmonic1 = sin(angle * 3f + phaseOffset) * 0.12f
            val harmonic2 = cos(angle * 5f - phaseOffset * 0.7f) * 0.08f
            val audioMod = sin(angle * 2f + phaseOffset) * (audio * 0.25f)
            val r = radius * layerScale * (1f + harmonic1 + harmonic2 + audioMod)

            val px = center.x + r * cos(angle)
            val py = center.y + r * sin(angle)

            if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
        }
        path.close()

        drawPath(
            path = path,
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.9f),
                    colorSweep.copy(alpha = 0.75f * settings.opacity),
                    Color(0xFF050811).copy(alpha = 0.9f)
                ),
                center = Offset(center.x - radius * 0.15f, center.y - radius * 0.15f),
                radius = radius
            )
        )
    }
}

// -------------------------------------------------------------
// 3. NEON RING ORB
// -------------------------------------------------------------
private fun DrawScope.drawNeonRingOrb(
    center: Offset,
    radius: Float,
    core: Color,
    glow: Color,
    settings: OrbSettings,
    rotation: Float,
    counterRotation: Float,
    audio: Float
) {
    val thickness = settings.ringThickness * 1.5f

    // Ambient Center Glow
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(core.copy(alpha = 0.6f * settings.glowIntensity), Color.Transparent),
            center = center,
            radius = radius * 0.7f
        ),
        radius = radius * 0.7f,
        center = center
    )

    // Inner Glowing Core
    drawCircle(
        color = core.copy(alpha = 0.85f * settings.opacity),
        radius = radius * 0.35f * (1f + audio * 0.4f),
        center = center
    )
    drawCircle(
        color = Color.White.copy(alpha = 0.9f),
        radius = radius * 0.18f,
        center = center
    )

    // Primary Neon Ring
    drawCircle(
        color = glow.copy(alpha = 0.9f * settings.opacity),
        radius = radius * 0.75f,
        center = center,
        style = Stroke(width = thickness)
    )

    // Outer Segmented Orbiting Ring
    rotate(rotation, center) {
        drawArc(
            color = core,
            startAngle = 10f,
            sweepAngle = 100f,
            useCenter = false,
            style = Stroke(width = thickness * 1.2f, cap = StrokeCap.Round),
            topLeft = Offset(center.x - radius, center.y - radius),
            size = Size(radius * 2f, radius * 2f)
        )
        drawArc(
            color = Color.White,
            startAngle = 190f,
            sweepAngle = 70f,
            useCenter = false,
            style = Stroke(width = thickness * 1.2f, cap = StrokeCap.Round),
            topLeft = Offset(center.x - radius, center.y - radius),
            size = Size(radius * 2f, radius * 2f)
        )
    }

    // Counter Orbiting Secondary Arc
    rotate(counterRotation, center) {
        drawArc(
            color = glow.copy(alpha = 0.7f),
            startAngle = 50f,
            sweepAngle = 130f,
            useCenter = false,
            style = Stroke(
                width = thickness * 0.8f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 15f), 0f)
            ),
            topLeft = Offset(center.x - radius * 1.2f, center.y - radius * 1.2f),
            size = Size(radius * 2.4f, radius * 2.4f)
        )
    }
}

// -------------------------------------------------------------
// 4. GALAXY PARTICLE ORB
// -------------------------------------------------------------
private fun DrawScope.drawGalaxyParticleOrb(
    center: Offset,
    radius: Float,
    core: Color,
    glow: Color,
    settings: OrbSettings,
    rotation: Float,
    audio: Float
) {
    // Deep Event Horizon Center
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color.White, core, Color(0xFF04060C)),
            center = center,
            radius = radius * 0.45f
        ),
        radius = radius * 0.45f,
        center = center
    )

    val count = settings.particleAmount.coerceIn(12, 64)
    val arms = 3

    rotate(rotation, center) {
        for (i in 0 until count) {
            val arm = i % arms
            val progress = (i.toFloat() / count)
            val spiralAngle = (progress * 3.5f * Math.PI + (arm * 2 * Math.PI / arms)).toFloat()
            val dist = radius * 0.4f + progress * radius * 0.85f * (1f + audio * 0.35f)

            val px = center.x + dist * cos(spiralAngle)
            val py = center.y + dist * sin(spiralAngle)
            val particleRadius = (2.5f + (1f - progress) * 3.5f) * (1f + audio * 0.4f)

            val alpha = (0.35f + 0.65f * (1f - progress)) * settings.opacity
            val pColor = if (i % 2 == 0) core.copy(alpha = alpha) else glow.copy(alpha = alpha)

            drawCircle(
                color = pColor,
                radius = particleRadius,
                center = Offset(px, py)
            )

            // Connective faint stardust
            if (i > 0 && i % arms == arm) {
                drawLine(
                    color = glow.copy(alpha = 0.15f * settings.opacity),
                    start = center,
                    end = Offset(px, py),
                    strokeWidth = 1f
                )
            }
        }
    }
}

// -------------------------------------------------------------
// 5. ENERGY CORE ORB
// -------------------------------------------------------------
private fun DrawScope.drawEnergyCoreOrb(
    center: Offset,
    radius: Float,
    core: Color,
    glow: Color,
    settings: OrbSettings,
    rotation: Float,
    waveProgress: Float,
    audio: Float
) {
    // Outer Containment Corona
    drawCircle(
        color = glow.copy(alpha = 0.5f * settings.glowIntensity),
        radius = radius * 1.15f,
        center = center,
        style = Stroke(width = settings.ringThickness)
    )

    // Rotating Energy Spokes
    rotate(rotation, center) {
        val spokes = 8
        val angleStep = (2 * Math.PI / spokes).toFloat()
        for (i in 0 until spokes) {
            val angle = i * angleStep
            val outerX = center.x + radius * 1.15f * cos(angle)
            val outerY = center.y + radius * 1.15f * sin(angle)
            val innerX = center.x + radius * 0.4f * cos(angle)
            val innerY = center.y + radius * 0.4f * sin(angle)

            drawLine(
                color = if (i % 2 == 0) Color.White else core,
                start = Offset(innerX, innerY),
                end = Offset(outerX, outerY),
                strokeWidth = settings.ringThickness * (if (i % 2 == 0) 1.5f else 1.0f)
            )
        }
    }

    // High Energy Singularity Core
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color.White, core, Color(0xFF030508)),
            center = center,
            radius = radius * 0.45f * (1f + audio * 0.5f)
        ),
        radius = radius * 0.45f * (1f + audio * 0.5f),
        center = center
    )
}

// -------------------------------------------------------------
// 6. AURORA ORB
// -------------------------------------------------------------
private fun DrawScope.drawAuroraOrb(
    center: Offset,
    radius: Float,
    core: Color,
    glow: Color,
    settings: OrbSettings,
    waveProgress: Float,
    audio: Float
) {
    // Soft Ethereal Boundary
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(core.copy(alpha = 0.4f * settings.glowIntensity), Color.Transparent),
            center = center,
            radius = radius * 1.35f
        ),
        radius = radius * 1.35f,
        center = center
    )

    // 4 Undulating Aurora Curtains across vertical bands
    for (i in -2..2) {
        val yOffset = i * (radius * 0.28f)
        val path = Path()
        val steps = 40
        val stepWidth = (radius * 2f) / steps

        path.moveTo(center.x - radius, center.y + yOffset)
        for (step in 0..steps) {
            val px = center.x - radius + step * stepWidth
            val wave = sin((step * 0.3f) + waveProgress + i) * (radius * 0.12f * (1f + audio * 0.5f))
            val py = center.y + yOffset + wave
            path.lineTo(px, py)
        }

        drawPath(
            path = path,
            color = if (i % 2 == 0) core.copy(alpha = 0.7f * settings.opacity) else glow.copy(alpha = 0.7f * settings.opacity),
            style = Stroke(width = settings.ringThickness * 2f, cap = StrokeCap.Round)
        )
    }

    // Central Diffuse Moon Glow
    drawCircle(
        color = Color.White.copy(alpha = 0.4f * settings.opacity),
        radius = radius * 0.25f,
        center = center
    )
}

// -------------------------------------------------------------
// 7. GLASS ORB
// -------------------------------------------------------------
private fun DrawScope.drawGlassOrb(
    center: Offset,
    radius: Float,
    core: Color,
    glow: Color,
    settings: OrbSettings,
    waveProgress: Float,
    audio: Float
) {
    // Outer Shadow & Ambient Glow
    drawCircle(
        color = glow.copy(alpha = 0.3f * settings.glowIntensity),
        radius = radius * 1.25f,
        center = center
    )

    // Frosted Translucent Glass Body
    drawCircle(
        brush = Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.35f * settings.opacity),
                core.copy(alpha = 0.25f * settings.opacity),
                Color(0xFF0A101D).copy(alpha = 0.75f * settings.opacity)
            ),
            start = Offset(center.x - radius, center.y - radius),
            end = Offset(center.x + radius, center.y + radius)
        ),
        radius = radius,
        center = center
    )

    // Inner Suspended Quantum Marble
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color.White, core),
            center = center,
            radius = radius * 0.4f * (1f + audio * 0.3f)
        ),
        radius = radius * 0.4f * (1f + audio * 0.3f),
        center = center
    )

    // Sharp Refractive Glass Rim
    drawCircle(
        brush = Brush.sweepGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.85f),
                glow.copy(alpha = 0.2f),
                Color.White.copy(alpha = 0.85f),
                core.copy(alpha = 0.1f)
            ),
            center = center
        ),
        radius = radius,
        center = center,
        style = Stroke(width = settings.ringThickness)
    )

    // Upper Left Crescent Specular Glare
    drawArc(
        color = Color.White.copy(alpha = 0.8f * settings.opacity),
        startAngle = 210f,
        sweepAngle = 60f,
        useCenter = false,
        style = Stroke(width = settings.ringThickness * 1.5f, cap = StrokeCap.Round),
        topLeft = Offset(center.x - radius * 0.85f, center.y - radius * 0.85f),
        size = Size(radius * 1.7f, radius * 1.7f)
    )
}

// -------------------------------------------------------------
// 8. PULSE ORB
// -------------------------------------------------------------
private fun DrawScope.drawPulseOrb(
    center: Offset,
    radius: Float,
    core: Color,
    glow: Color,
    settings: OrbSettings,
    waveProgress: Float,
    audio: Float
) {
    // 3 Concentric Expanding Sonar Shockwaves
    val waves = 3
    for (i in 0 until waves) {
        val fraction = ((waveProgress / (2 * Math.PI).toFloat()) + (i.toFloat() / waves)) % 1f
        val waveRadius = radius * 0.4f + fraction * radius * 0.9f * (1f + audio * 0.4f)
        val alpha = (1f - fraction) * 0.8f * settings.opacity

        drawCircle(
            color = glow.copy(alpha = alpha),
            radius = waveRadius,
            center = center,
            style = Stroke(width = settings.ringThickness * (1.5f - 0.5f * fraction))
        )
    }

    // Dense Pulsing Core Nucleus
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color.White, core, Color(0xFF04060E)),
            center = center,
            radius = radius * 0.42f * (1f + audio * 0.5f)
        ),
        radius = radius * 0.42f * (1f + audio * 0.5f),
        center = center
    )
}

// -------------------------------------------------------------
// 9. WAVE ORB
// -------------------------------------------------------------
private fun DrawScope.drawWaveOrb(
    center: Offset,
    radius: Float,
    core: Color,
    glow: Color,
    settings: OrbSettings,
    waveProgress: Float,
    audio: Float
) {
    // Background boundary
    drawCircle(
        color = core.copy(alpha = 0.15f * settings.opacity),
        radius = radius,
        center = center
    )

    // Horizontal spherical equalizer waveform slices
    val lines = 7
    for (i in -lines / 2..lines / 2) {
        val normalized = i.toFloat() / (lines / 2)
        val chordWidth = sqrt(max(0f, 1f - normalized * normalized)) * radius * 2f
        val py = center.y + normalized * radius * 0.85f

        val path = Path()
        val steps = 24
        val stepW = chordWidth / steps
        val startX = center.x - chordWidth / 2f

        path.moveTo(startX, py)
        for (s in 0..steps) {
            val px = startX + s * stepW
            val sineVal = sin(s * 0.6f + waveProgress + i) * (radius * 0.09f * (1f + audio * 0.7f))
            path.lineTo(px, py + sineVal)
        }

        drawPath(
            path = path,
            color = if (i == 0) Color.White else glow.copy(alpha = (1f - abs(normalized) * 0.5f) * settings.opacity),
            style = Stroke(width = settings.ringThickness * (if (i == 0) 1.4f else 1.0f))
        )
    }

    // Spherical Outer Wireframe Ring
    drawCircle(
        color = core.copy(alpha = 0.75f * settings.opacity),
        radius = radius,
        center = center,
        style = Stroke(width = settings.ringThickness)
    )
}

// -------------------------------------------------------------
// 10. MYRA SIGNATURE ORB (Flagship Quantum Gyroscope & Nexus)
// -------------------------------------------------------------
private fun DrawScope.drawMyraSignatureOrb(
    center: Offset,
    radius: Float,
    core: Color,
    glow: Color,
    settings: OrbSettings,
    rotation: Float,
    counterRotation: Float,
    audio: Float
) {
    // 1. Quantum Diffuse Aura
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(glow.copy(alpha = 0.55f * settings.glowIntensity), Color.Transparent),
            center = center,
            radius = radius * 1.45f
        ),
        radius = radius * 1.45f,
        center = center
    )

    // 2. High-Tech Luminous Core
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color.White, core, Color(0xFF050814)),
            center = Offset(center.x - radius * 0.18f, center.y - radius * 0.18f),
            radius = radius * 0.7f
        ),
        radius = radius * 0.7f,
        center = center
    )

    // 3. Dual Gyroscopic Orbital Rings
    val ringThick = settings.ringThickness * 1.2f

    // Ring 1 (Clockwise inclined)
    rotate(rotation, center) {
        drawOval(
            color = glow.copy(alpha = 0.85f * settings.opacity),
            topLeft = Offset(center.x - radius * 1.15f, center.y - radius * 0.45f),
            size = Size(radius * 2.3f, radius * 0.9f),
            style = Stroke(width = ringThick)
        )
        // Orbiting Energy Pearl 1
        drawCircle(
            color = Color.White,
            radius = ringThick * 1.6f,
            center = Offset(center.x + radius * 1.15f, center.y)
        )
    }

    // Ring 2 (Counter-clockwise cross-inclined)
    rotate(counterRotation + 60f, center) {
        drawOval(
            color = core.copy(alpha = 0.85f * settings.opacity),
            topLeft = Offset(center.x - radius * 1.15f, center.y - radius * 0.45f),
            size = Size(radius * 2.3f, radius * 0.9f),
            style = Stroke(width = ringThick)
        )
        // Orbiting Energy Pearl 2
        drawCircle(
            color = Color.White,
            radius = ringThick * 1.6f,
            center = Offset(center.x - radius * 1.15f, center.y)
        )
    }

    // 4. 8-Node Quantum Constellation Nexus
    val nodes = 8
    val angleStep = (2 * Math.PI / nodes).toFloat()
    val radOffset = Math.toRadians(rotation.toDouble()).toFloat()

    for (i in 0 until nodes) {
        val ang = i * angleStep + radOffset
        val dist = radius * (0.85f + (if (i % 2 == 0) 0.25f else 0.12f) * (1f + audio * 0.4f))
        val nx = center.x + dist * cos(ang)
        val ny = center.y + dist * sin(ang)

        // Laser Filaments
        drawLine(
            color = glow.copy(alpha = 0.25f * settings.opacity),
            start = center,
            end = Offset(nx, ny),
            strokeWidth = 1.2f
        )

        // Quantum Node Bead
        drawCircle(
            color = if (i % 2 == 0) Color.White else core,
            radius = (3f + audio * 3f),
            center = Offset(nx, ny)
        )
    }
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
