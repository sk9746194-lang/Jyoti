package com.myra.assistant.ui.orb

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import com.myra.assistant.data.AssistantState
import kotlin.math.abs
import kotlin.math.sin

/**
 * Full-Screen Perimeter Edge Effect Engine for MYRA Assistant.
 * Illuminates phone borders during Speaking & Listening states with dynamic
 * flowing light beams, equalizer waveforms, and perimeter quantum glow.
 */
@Composable
fun EdgeEffectRenderer(
    state: AssistantState,
    audioLevel: Float,
    mode: EdgeEffectMode,
    intensity: Float,
    theme: OrbThemeColor,
    modifier: Modifier = Modifier
) {
    if (mode == EdgeEffectMode.OFF) return

    // Active during SPEAKING, LISTENING, or THINKING
    val isActive = (state == AssistantState.SPEAKING || 
                    state == AssistantState.LISTENING || 
                    state == AssistantState.THINKING || 
                    state == AssistantState.WAKE_WORD)

    val transition = rememberInfiniteTransition(label = "edge_flow")

    // Flowing light beam progress (0f to 1f)
    val flowProgress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "edge_flow_progress"
    )

    // Breathing pulse for edge aura
    val edgePulse by transition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "edge_pulse"
    )

    // Base target alpha depending on active state
    val targetAlpha = when {
        !isActive -> 0.0f
        state == AssistantState.SPEAKING -> (0.85f * intensity) * (0.8f + audioLevel * 0.4f)
        state == AssistantState.LISTENING -> (0.9f * intensity) * (0.7f + audioLevel * 0.5f)
        state == AssistantState.THINKING -> 0.65f * intensity
        state == AssistantState.WAKE_WORD -> 1.0f * intensity
        else -> 0.0f
    }.coerceIn(0f, 1f)

    if (targetAlpha <= 0.02f) return

    val (primaryEdge, secondaryEdge) = when (state) {
        AssistantState.SPEAKING -> Color(0xFF00E676) to Color(0xFF69F0AE)
        AssistantState.LISTENING -> Color(0xFFFF1744) to Color(0xFFFF5252)
        AssistantState.THINKING -> Color(0xFFFFB300) to Color(0xFFFFD54F)
        AssistantState.WAKE_WORD -> Color(0xFFFFFFFF) to theme.primaryColor
        else -> theme.primaryColor to theme.secondaryColor
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val edgeDepth = 40.dp.toPx()
        val effAlpha = targetAlpha * edgePulse

        val drawTop = mode == EdgeEffectMode.ALL_4_SIDES || mode == EdgeEffectMode.TOP_BOTTOM
        val drawBottom = mode == EdgeEffectMode.ALL_4_SIDES || mode == EdgeEffectMode.TOP_BOTTOM || mode == EdgeEffectMode.BOTTOM_ONLY
        val drawLeft = mode == EdgeEffectMode.ALL_4_SIDES || mode == EdgeEffectMode.LEFT_RIGHT
        val drawRight = mode == EdgeEffectMode.ALL_4_SIDES || mode == EdgeEffectMode.LEFT_RIGHT

        // 1. TOP EDGE
        if (drawTop) {
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(primaryEdge.copy(alpha = 0.7f * effAlpha), Color.Transparent),
                    startY = 0f,
                    endY = edgeDepth
                ),
                topLeft = Offset(0f, 0f),
                size = Size(w, edgeDepth)
            )
            // Equalizer ticks along top border
            drawEdgeWaveform(
                startX = 0f, startY = 0f, endX = w, endY = 0f,
                isHorizontal = true, isTopOrLeft = true,
                color = secondaryEdge.copy(alpha = effAlpha),
                flow = flowProgress, audio = audioLevel
            )
        }

        // 2. BOTTOM EDGE
        if (drawBottom) {
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.Transparent, primaryEdge.copy(alpha = 0.85f * effAlpha)),
                    startY = h - edgeDepth,
                    endY = h
                ),
                topLeft = Offset(0f, h - edgeDepth),
                size = Size(w, edgeDepth)
            )
            // Equalizer ticks along bottom border
            drawEdgeWaveform(
                startX = 0f, startY = h, endX = w, endY = h,
                isHorizontal = true, isTopOrLeft = false,
                color = secondaryEdge.copy(alpha = effAlpha),
                flow = flowProgress, audio = audioLevel
            )
        }

        // 3. LEFT EDGE
        if (drawLeft) {
            drawRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(primaryEdge.copy(alpha = 0.7f * effAlpha), Color.Transparent),
                    startX = 0f,
                    endX = edgeDepth
                ),
                topLeft = Offset(0f, 0f),
                size = Size(edgeDepth, h)
            )
            drawEdgeWaveform(
                startX = 0f, startY = 0f, endX = 0f, endY = h,
                isHorizontal = false, isTopOrLeft = true,
                color = secondaryEdge.copy(alpha = effAlpha),
                flow = flowProgress, audio = audioLevel
            )
        }

        // 4. RIGHT EDGE
        if (drawRight) {
            drawRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(Color.Transparent, primaryEdge.copy(alpha = 0.7f * effAlpha)),
                    startX = w - edgeDepth,
                    endX = w
                ),
                topLeft = Offset(w - edgeDepth, 0f),
                size = Size(edgeDepth, h)
            )
            drawEdgeWaveform(
                startX = w, startY = 0f, endX = w, endY = h,
                isHorizontal = false, isTopOrLeft = false,
                color = secondaryEdge.copy(alpha = effAlpha),
                flow = flowProgress, audio = audioLevel
            )
        }

        // Flowing Light Beams moving across perimeter
        if (drawBottom) {
            val beamX = (flowProgress * w)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.White.copy(alpha = effAlpha), Color.Transparent),
                    center = Offset(beamX, h - 2.dp.toPx()),
                    radius = 80.dp.toPx()
                ),
                radius = 80.dp.toPx(),
                center = Offset(beamX, h - 2.dp.toPx())
            )
        }
    }
}

private fun DrawScope.drawEdgeWaveform(
    startX: Float, startY: Float, endX: Float, endY: Float,
    isHorizontal: Boolean, isTopOrLeft: Boolean,
    color: Color, flow: Float, audio: Float
) {
    val barCount = 32
    val length = if (isHorizontal) abs(endX - startX) else abs(endY - startY)
    val step = length / barCount

    for (i in 0 until barCount) {
        val norm = i.toFloat() / barCount
        val harmonic = sin((norm * 6 * Math.PI + flow * 2 * Math.PI).toFloat())
        val barHeight = (4.dp.toPx() + abs(harmonic) * 16.dp.toPx() * (1f + audio * 1.5f))

        if (isHorizontal) {
            val bx = startX + i * step
            val y1 = startY
            val y2 = if (isTopOrLeft) startY + barHeight else startY - barHeight
            drawLine(
                color = color.copy(alpha = 0.4f + 0.6f * abs(harmonic)),
                start = Offset(bx, y1),
                end = Offset(bx, y2),
                strokeWidth = 2.dp.toPx()
            )
        } else {
            val by = startY + i * step
            val x1 = startX
            val x2 = if (isTopOrLeft) startX + barHeight else startX - barHeight
            drawLine(
                color = color.copy(alpha = 0.4f + 0.6f * abs(harmonic)),
                start = Offset(x1, by),
                end = Offset(x2, by),
                strokeWidth = 2.dp.toPx()
            )
        }
    }
}
