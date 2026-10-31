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
import kotlin.math.abs
import kotlin.math.sin

/**
 * Jetpack Compose Dynamic Sinusoidal Audio Waveform & Multi-Band Equalizer Component.
 * Visually reacts to real-time microphone level and speaking states.
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

    Canvas(modifier = modifier.fillMaxWidth().height(56.dp)) {
        val width = size.width
        val height = size.height
        val centerY = height / 2f
        val points = 60
        val step = width / points

        val waveAmplitude = if (isSpeakingOrListening) {
            (height * 0.42f * (0.25f + audioLevel * 0.75f))
        } else {
            height * 0.06f
        }

        // 1. Symmetrical 24-band background Equalizer Bars
        val numBars = 24
        val barWidth = width / (numBars * 1.6f)
        val barSpacing = width / numBars

        for (b in 0 until numBars) {
            val distFromCenter = abs(b - (numBars / 2f)) / (numBars / 2f)
            val freqWeight = 1.0f - distFromCenter * 0.4f
            val harmonic = sin(b * 0.5f + phase) * 0.4f + 0.6f
            val barAmp = if (isSpeakingOrListening) {
                (height * 0.4f * (0.2f + audioLevel * 0.8f) * freqWeight * harmonic)
            } else {
                height * 0.08f * harmonic
            }

            val x = b * barSpacing + (barSpacing / 2f)
            drawLine(
                brush = Brush.verticalGradient(
                    colors = if (isSpeakingOrListening) listOf(
                        Color(0xFFF43F5E),
                        Color(0xFF38BDF8),
                        Color(0xFF10B981)
                    ) else listOf(
                        Color(0xFF1E3A8A),
                        Color(0xFF3B82F6)
                    ),
                    startY = centerY - barAmp,
                    endY = centerY + barAmp
                ),
                start = Offset(x, centerY - barAmp),
                end = Offset(x, centerY + barAmp),
                strokeWidth = barWidth,
                cap = StrokeCap.Round
            )
        }

        // 2. Continuous foreground harmonic sine wave
        for (i in 0 until points - 1) {
            val x1 = i * step
            val x2 = (i + 1) * step

            val y1 = centerY + sin(phase + i * 0.2f) * waveAmplitude
            val y2 = centerY + sin(phase + (i + 1) * 0.2f) * waveAmplitude

            drawLine(
                brush = Brush.horizontalGradient(
                    listOf(
                        Color(0xFF00E5FF),
                        Color(0xFF8B5CF6),
                        Color(0xFFF43F5E)
                    )
                ),
                start = Offset(x1, y1),
                end = Offset(x2, y2),
                strokeWidth = (3.5f + audioLevel * 2f).dp.toPx(),
                cap = StrokeCap.Round
            )
        }
    }
}
