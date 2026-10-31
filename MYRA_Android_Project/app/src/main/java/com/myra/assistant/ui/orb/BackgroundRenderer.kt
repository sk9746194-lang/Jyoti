package com.myra.assistant.ui.orb

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.sin

/**
 * Universal Ambient Background Atmosphere Engine for MYRA.
 * Renders 8 distinct styles: Solid, Gradient, Animated Gradient, Particles, Galaxy, Aurora, Digital, MYRA Signature.
 */
@Composable
fun BackgroundRenderer(
    style: BackgroundStyle,
    speed: Float,
    opacity: Float,
    theme: OrbThemeColor,
    modifier: Modifier = Modifier
) {
    val speedMult = speed.coerceIn(0.2f, 3.0f)
    val infiniteTransition = rememberInfiniteTransition(label = "bg_anim")

    // Slow ambient rotation / shifting phase
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween((12000 / speedMult).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "bg_phase"
    )

    // Particle drift progress
    val driftProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((8000 / speedMult).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "drift"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val baseDark = Color(0xFF070A12)

        // Always draw deep base dark canvas first
        drawRect(color = baseDark)

        val effOpacity = opacity.coerceIn(0f, 1f)

        when (style) {
            BackgroundStyle.SOLID -> {
                // Pure minimal matte deep space
            }
            BackgroundStyle.GRADIENT -> drawStaticGradient(theme, effOpacity)
            BackgroundStyle.ANIMATED_GRADIENT -> drawAnimatedGradient(theme, phase, effOpacity)
            BackgroundStyle.PARTICLES -> drawParticlesBackground(theme, driftProgress, effOpacity)
            BackgroundStyle.GALAXY -> drawGalaxyBackground(theme, phase, effOpacity)
            BackgroundStyle.AURORA -> drawAuroraBackground(theme, phase, effOpacity)
            BackgroundStyle.DIGITAL -> drawDigitalGridBackground(theme, phase, effOpacity)
            BackgroundStyle.MYRA_SIGNATURE -> drawMyraSignatureBackground(theme, phase, driftProgress, effOpacity)
        }
    }
}

private fun DrawScope.drawStaticGradient(theme: OrbThemeColor, opacity: Float) {
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                theme.primaryColor.copy(alpha = 0.22f * opacity),
                theme.secondaryColor.copy(alpha = 0.08f * opacity),
                Color.Transparent
            ),
            center = Offset(size.width * 0.5f, size.height * 0.45f),
            radius = size.maxDimension * 0.65f
        ),
        radius = size.maxDimension * 0.65f,
        center = Offset(size.width * 0.5f, size.height * 0.45f)
    )
}

private fun DrawScope.drawAnimatedGradient(theme: OrbThemeColor, phase: Float, opacity: Float) {
    val cx = size.width * (0.5f + 0.15f * sin(phase))
    val cy = size.height * (0.45f + 0.12f * sin(phase * 0.8f))

    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                theme.primaryColor.copy(alpha = 0.28f * opacity),
                theme.secondaryColor.copy(alpha = 0.12f * opacity),
                Color.Transparent
            ),
            center = Offset(cx, cy),
            radius = size.maxDimension * 0.6f
        ),
        radius = size.maxDimension * 0.6f,
        center = Offset(cx, cy)
    )
}

private fun DrawScope.drawParticlesBackground(theme: OrbThemeColor, progress: Float, opacity: Float) {
    val count = 35
    for (i in 0 until count) {
        val seedX = ((i * 37) % 100) / 100f
        val seedY = ((i * 53) % 100) / 100f
        val speedFactor = 0.5f + ((i * 17) % 50) / 50f

        val x = seedX * size.width
        val y = ((seedY - progress * speedFactor + 1f) % 1f) * size.height
        val pRadius = (1.5f + (i % 3) * 1.2f)
        val alpha = (0.25f + ((i % 5) / 5f) * 0.5f) * opacity

        drawCircle(
            color = if (i % 2 == 0) theme.primaryColor.copy(alpha = alpha) else Color.White.copy(alpha = alpha),
            radius = pRadius,
            center = Offset(x, y)
        )
    }
}

private fun DrawScope.drawGalaxyBackground(theme: OrbThemeColor, phase: Float, opacity: Float) {
    // Nebula Clouds
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                theme.secondaryColor.copy(alpha = 0.25f * opacity),
                Color.Transparent
            ),
            center = Offset(size.width * 0.25f, size.height * 0.35f),
            radius = size.width * 0.7f
        ),
        radius = size.width * 0.7f,
        center = Offset(size.width * 0.25f, size.height * 0.35f)
    )

    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                theme.primaryColor.copy(alpha = 0.2f * opacity),
                Color.Transparent
            ),
            center = Offset(size.width * 0.8f, size.height * 0.65f),
            radius = size.width * 0.65f
        ),
        radius = size.width * 0.65f,
        center = Offset(size.width * 0.8f, size.height * 0.65f)
    )

    // Twinkling Star Field
    val stars = 40
    for (i in 0 until stars) {
        val sx = (((i * 47 + 13) % 100) / 100f) * size.width
        val sy = (((i * 61 + 29) % 100) / 100f) * size.height
        val twinkle = (0.3f + 0.7f * (0.5f + 0.5f * sin(phase * 2f + i))) * opacity
        drawCircle(
            color = Color.White.copy(alpha = twinkle),
            radius = (1f + (i % 3) * 0.8f),
            center = Offset(sx, sy)
        )
    }
}

private fun DrawScope.drawAuroraBackground(theme: OrbThemeColor, phase: Float, opacity: Float) {
    val ribbons = 3
    for (r in 0 until ribbons) {
        val path = Path()
        val steps = 30
        val stepW = size.width / steps
        val baseY = size.height * (0.2f + r * 0.16f)

        path.moveTo(0f, baseY)
        for (i in 0..steps) {
            val px = i * stepW
            val py = baseY + sin((i * 0.25f) + phase + r) * 45f
            path.lineTo(px, py)
        }
        path.lineTo(size.width, size.height)
        path.lineTo(0f, size.height)
        path.close()

        val color = if (r % 2 == 0) theme.primaryColor else theme.secondaryColor
        drawPath(
            path = path,
            brush = Brush.verticalGradient(
                colors = listOf(
                    color.copy(alpha = 0.16f * opacity),
                    Color.Transparent
                ),
                startY = baseY - 50f,
                endY = baseY + 180f
            )
        )
    }
}

private fun DrawScope.drawDigitalGridBackground(theme: OrbThemeColor, phase: Float, opacity: Float) {
    val gridColor = theme.primaryColor.copy(alpha = 0.08f * opacity)
    val horizonY = size.height * 0.6f

    // Vertical Perspective Lines
    val lines = 12
    for (i in 0..lines) {
        val bottomX = (i.toFloat() / lines) * size.width
        val topX = size.width * 0.5f + (bottomX - size.width * 0.5f) * 0.15f
        drawLine(
            color = gridColor,
            start = Offset(topX, horizonY),
            end = Offset(bottomX, size.height),
            strokeWidth = 1f
        )
    }

    // Horizontal Perspective Grid Steps
    val hSteps = 8
    for (j in 0..hSteps) {
        val norm = (j.toFloat() / hSteps)
        val y = horizonY + (norm * norm) * (size.height - horizonY)
        drawLine(
            color = gridColor,
            start = Offset(0f, y),
            end = Offset(size.width, y),
            strokeWidth = 1f
        )
    }
}

private fun DrawScope.drawMyraSignatureBackground(
    theme: OrbThemeColor,
    phase: Float,
    progress: Float,
    opacity: Float
) {
    // 1. Dual Ambient Nebula
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(theme.primaryColor.copy(alpha = 0.18f * opacity), Color.Transparent),
            center = Offset(size.width * 0.3f, size.height * 0.4f),
            radius = size.width * 0.75f
        ),
        radius = size.width * 0.75f,
        center = Offset(size.width * 0.3f, size.height * 0.4f)
    )
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(theme.secondaryColor.copy(alpha = 0.15f * opacity), Color.Transparent),
            center = Offset(size.width * 0.7f, size.height * 0.6f),
            radius = size.width * 0.75f
        ),
        radius = size.width * 0.75f,
        center = Offset(size.width * 0.7f, size.height * 0.6f)
    )

    // 2. Subtle Quantum Grid Horizon
    val gridColor = theme.primaryColor.copy(alpha = 0.05f * opacity)
    val step = (size.width / 8f).coerceAtLeast(40f)
    var x = 0f
    while (x < size.width) {
        drawLine(color = gridColor, start = Offset(x, 0f), end = Offset(x, size.height), strokeWidth = 0.8f)
        x += step
    }
    var y = 0f
    while (y < size.height) {
        drawLine(color = gridColor, start = Offset(0f, y), end = Offset(size.width, y), strokeWidth = 0.8f)
        y += step
    }

    // 3. Stardust Nodes
    val count = 20
    for (i in 0 until count) {
        val px = (((i * 41 + 17) % 100) / 100f) * size.width
        val py = (((i * 59 + 23) % 100) / 100f) * size.height
        val a = (0.2f + 0.4f * (0.5f + 0.5f * sin(phase + i))) * opacity
        drawCircle(
            color = theme.primaryColor.copy(alpha = a),
            radius = 1.6f,
            center = Offset(px, py)
        )
    }
}
