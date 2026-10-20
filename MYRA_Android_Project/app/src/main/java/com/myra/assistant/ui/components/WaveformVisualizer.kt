package com.myra.assistant.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import kotlin.math.sin

/**
 * Jetpack Compose Dynamic Sinusoidal Audio Waveform Component.
 * Visually renders real-time microphone level and speaking states.
 */
@Composable
fun WaveformVisualizer(
    audioLevel: Float,
    isSpeakingOrListening: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "waveTransition")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phaseAnimation"
    )

    Canvas(modifier = modifier.fillMaxWidth().height(48.dp)) {
        val width = size.width
        val height = size.height
        val centerY = height / 2f
        val points = 60
        val step = width / points

        val waveAmplitude = if (isSpeakingOrListening) {
            (height * 0.4f * (0.2f + audioLevel * 0.8f))
        } else {
            height * 0.05f
        }

        for (i in 0 until points - 1) {
            val x1 = i * step
            val x2 = (i + 1) * step

            val y1 = centerY + sin(phase + i * 0.2f) * waveAmplitude
            val y2 = centerY + sin(phase + (i + 1) * 0.2f) * waveAmplitude

            drawLine(
                brush = Brush.horizontalGradient(
                    listOf(
                        Color(0xFF3B82F6),
                        Color(0xFF6366F1),
                        Color(0xFF10B981)
                    )
                ),
                start = Offset(x1, y1),
                end = Offset(x2, y2),
                strokeWidth = 4f,
                cap = StrokeCap.Round
            )
        }
    }
}
