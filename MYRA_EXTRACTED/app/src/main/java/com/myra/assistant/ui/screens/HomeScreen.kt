package com.myra.assistant.ui.screens

import android.graphics.BitmapFactory
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.myra.assistant.R
import com.myra.assistant.data.AssistantState
import com.myra.assistant.data.PreferencesManager
import com.myra.assistant.ui.theme.*

/**
 * MYRA Home Screen - Exact Redesign matching the reference image.
 *
 * 1. Top Header: Menu button, "MYRA ♡" & "Your AI Companion", "● Online" pill, Settings button.
 * 2. Welcome Banner: Rounded neon border card with female assistant portrait + left text overlay
 *    ("Hello", "I'm MYRA ♡", "Your Personal AI Companion").
 * 3. Four Action Cards (2x2 Grid): Chat (Text & Voice), Voice (Hands-Free), Tools (Smart Utilities), Settings (Customize MYRA).
 * 4. Central Voice Interaction: Glowing neon animated audio waveforms + large circular microphone button + "Tap to Speak".
 */
@Composable
fun HomeScreen(
    onNavigateToChat: () -> Unit,
    onNavigateToVoice: () -> Unit,
    onNavigateToTools: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onStartVoiceListening: () -> Unit,
    isListening: Boolean = false,
    assistantState: AssistantState = AssistantState.IDLE,
    onOpenMenu: () -> Unit = {}
) {
    val context = LocalContext.current
    val prefs = remember { PreferencesManager(context) }
    val currentPhotoPath by remember { mutableStateOf(prefs.customPhotoPath) }

    // Decode custom bitmap or fall back to bundled default
    val customBitmap = remember(currentPhotoPath) {
        if (!currentPhotoPath.isNullOrBlank()) {
            try {
                BitmapFactory.decodeFile(currentPhotoPath)?.asImageBitmap()
            } catch (e: Exception) {
                null
            }
        } else null
    }

    // Animation for central voice waveform
    val infiniteTransition = rememberInfiniteTransition(label = "waveform_anim")
    val wavePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(2800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave_phase"
    )
    val micPulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "mic_pulse"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF030712), // Deep obsidian
                        Color(0xFF060D1E), // Deep cyber navy
                        Color(0xFF0A1128),
                        Color(0xFF040713)
                    )
                )
            )
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 10.dp, bottom = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // -------------------------------------------------------------
            // 1. TOP HEADER: MENU, MYRA LOGO + HEART, ONLINE STATUS, SETTINGS
            // -------------------------------------------------------------
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left: Menu button + Brand title & subtitle
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onOpenMenu,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF0B172A))
                                .border(1.dp, CyanNeon.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                        ) {
                            Icon(
                                Icons.Default.Menu,
                                contentDescription = "Menu",
                                tint = CyanNeon,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "MYRA",
                                    color = Color.White,
                                    fontSize = 21.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 0.5.sp
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "♡",
                                    color = Color(0xFFF472B6), // Pink heart
                                    fontSize = 19.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = "Your AI Companion",
                                color = CyanNeon,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    // Right: Online badge + Settings button
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Online Status Pill Badge
                        Surface(
                            color = Color(0xFF061725),
                            shape = RoundedCornerShape(20.dp),
                            border = BorderStroke(1.dp, Color(0xFF14B8A6).copy(alpha = 0.45f)),
                            shadowElevation = 4.dp
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF34D399))
                                        .shadow(4.dp, CircleShape)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Online",
                                    color = Color(0xFF5EEAD4),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Settings Button
                        IconButton(
                            onClick = onNavigateToSettings,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF0B172A))
                                .border(1.dp, CyanNeon.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                        ) {
                            Icon(
                                Icons.Default.Settings,
                                contentDescription = "Settings",
                                tint = CyanNeon,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            // -------------------------------------------------------------
            // 2. MAIN WELCOME BANNER (PORTRAIT + EXACT OVERLAY TEXT)
            // -------------------------------------------------------------
            item {
                Surface(
                    shape = RoundedCornerShape(28.dp),
                    color = Color(0xFF050B18),
                    border = BorderStroke(
                        1.5.dp,
                        Brush.linearGradient(
                            listOf(
                                CyanNeon.copy(alpha = 0.9f),
                                Color(0xFF8B5CF6).copy(alpha = 0.75f),
                                Color(0xFF38BDF8).copy(alpha = 0.9f)
                            )
                        )
                    ),
                    shadowElevation = 14.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(350.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        // Background Portrait Image (Natural Right-Center Framing)
                        if (customBitmap != null) {
                            Image(
                                bitmap = customBitmap,
                                contentDescription = "MYRA Portrait",
                                contentScale = ContentScale.Crop,
                                alignment = Alignment.Center,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Image(
                                painter = painterResource(id = R.drawable.default_home_photo),
                                contentDescription = "MYRA Portrait",
                                contentScale = ContentScale.Crop,
                                alignment = Alignment.Center,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        // Seamless Dark Gradient on Left Side to highlight typography
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.horizontalGradient(
                                        colorStops = arrayOf(
                                            0.0f to Color(0xFF030712).copy(alpha = 0.95f),
                                            0.42f to Color(0xFF030712).copy(alpha = 0.50f),
                                            0.75f to Color.Transparent
                                        )
                                    )
                                )
                        )

                        // Subtle bottom vignette for depth
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colorStops = arrayOf(
                                            0.75f to Color.Transparent,
                                            1.0f to Color(0xFF030712).copy(alpha = 0.60f)
                                        )
                                    )
                                )
                        )

                        // Welcome Text Overlay (Top Left)
                        Column(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(top = 22.dp, start = 22.dp)
                        ) {
                            // Cursive "Hello"
                            Text(
                                text = "Hello",
                                color = Color.White,
                                fontSize = 32.sp,
                                fontFamily = FontFamily.Cursive,
                                fontWeight = FontWeight.Normal
                            )

                            Spacer(modifier = Modifier.height(2.dp))

                            // "I'm MYRA ♡"
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "I'm MYRA",
                                    color = Color.White,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 0.3.sp
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = "♡",
                                    color = Color(0xFFF472B6),
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // "Your Personal AI Companion" (2 lines)
                            Text(
                                text = "Your Personal",
                                color = CyanNeon,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "AI Companion",
                                color = CyanNeon,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            // -------------------------------------------------------------
            // 3. FOUR MAIN FEATURE CARDS (2x2 GRID AS IN REFERENCE)
            // -------------------------------------------------------------
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Row 1: Chat & Voice
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Card 1: Chat – Text & Voice
                        FeatureCardItem(
                            title = "Chat",
                            subtitle = "Text & Voice",
                            icon = Icons.Default.ChatBubble,
                            iconColor = CyanNeon,
                            gradientColors = listOf(Color(0xFF0C1A36), Color(0xFF050C1D)),
                            borderColor = CyanNeon.copy(alpha = 0.50f),
                            onClick = onNavigateToChat,
                            modifier = Modifier.weight(1f)
                        )

                        // Card 2: Voice – Hands-Free
                        FeatureCardItem(
                            title = "Voice",
                            subtitle = "Hands-Free",
                            icon = Icons.Default.Mic,
                            iconColor = Color(0xFFA855F7),
                            gradientColors = listOf(Color(0xFF1D103B), Color(0xFF0A0518)),
                            borderColor = Color(0xFFA855F7).copy(alpha = 0.50f),
                            onClick = onNavigateToVoice,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Row 2: Tools & Settings
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Card 3: Tools – Smart Utilities
                        FeatureCardItem(
                            title = "Tools",
                            subtitle = "Smart Utilities",
                            icon = Icons.Default.Build,
                            iconColor = Color(0xFF10B981),
                            gradientColors = listOf(Color(0xFF052627), Color(0xFF031314)),
                            borderColor = Color(0xFF10B981).copy(alpha = 0.50f),
                            onClick = onNavigateToTools,
                            modifier = Modifier.weight(1f)
                        )

                        // Card 4: Settings – Customize MYRA
                        FeatureCardItem(
                            title = "Settings",
                            subtitle = "Customize MYRA",
                            icon = Icons.Default.Settings,
                            iconColor = Color(0xFFF43F5E),
                            gradientColors = listOf(Color(0xFF2C0E22), Color(0xFF14050F)),
                            borderColor = Color(0xFFF43F5E).copy(alpha = 0.50f),
                            onClick = onNavigateToSettings,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // -------------------------------------------------------------
            // 4. CENTRAL VOICE INTERACTION (NEON WAVEFORM + CIRCULAR MIC BUTTON)
            // -------------------------------------------------------------
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Flowing Neon Audio Waveforms behind the microphone
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(86.dp)
                    ) {
                        val width = size.width
                        val height = size.height
                        val midY = height / 2f

                        // Neon wave 1: Cyan to Purple to Pink
                        val path1 = Path()
                        path1.moveTo(0f, midY)
                        var x = 0f
                        while (x <= width) {
                            val relX = x / width
                            val y = midY + Math.sin((relX * 3.5 * Math.PI) + wavePhase).toFloat() * 18f
                            path1.lineTo(x, y)
                            x += 8f
                        }
                        drawPath(
                            path = path1,
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    CyanNeon.copy(alpha = 0.1f),
                                    CyanNeon.copy(alpha = 0.85f),
                                    Color(0xFF818CF8).copy(alpha = 0.9f),
                                    Color(0xFFC084FC).copy(alpha = 0.85f),
                                    Color(0xFFF472B6).copy(alpha = 0.1f)
                                )
                            ),
                            style = Stroke(width = 2.8.dp.toPx())
                        )

                        // Neon wave 2: Deep blue to magenta
                        val path2 = Path()
                        path2.moveTo(0f, midY)
                        var x2 = 0f
                        while (x2 <= width) {
                            val relX = x2 / width
                            val y = midY + Math.sin((relX * 2.8 * Math.PI) - wavePhase).toFloat() * 14f
                            path2.lineTo(x2, y)
                            x2 += 8f
                        }
                        drawPath(
                            path = path2,
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    Color(0xFF38BDF8).copy(alpha = 0.05f),
                                    Color(0xFF6366F1).copy(alpha = 0.75f),
                                    Color(0xFFD946EF).copy(alpha = 0.8f),
                                    Color(0xFFA855F7).copy(alpha = 0.1f)
                                )
                            ),
                            style = Stroke(width = 2.2.dp.toPx())
                        )
                    }

                    // Column containing Central Mic Button & "Tap to Speak" Label
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        // Large Circular Microphone Button with Glowing Gradient Ring
                        Box(
                            modifier = Modifier
                                .size(92.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        if (isListening) {
                                            listOf(Color(0xFFF43F5E), Color(0xFFBE123C), Color(0xFFA855F7))
                                        } else {
                                            listOf(CyanNeon, Color(0xFF3B82F6), Color(0xFFA855F7))
                                        }
                                    )
                                )
                                .padding(3.dp) // Border thickness for glowing ring
                                .clickable {
                                    onStartVoiceListening()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                                    .background(Color(0xFF07132C)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isListening) Icons.Default.MicOff else Icons.Default.Mic,
                                    contentDescription = "Voice Assistant",
                                    tint = Color.White,
                                    modifier = Modifier.size(38.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // "Tap to Speak" Text
                        Text(
                            text = if (isListening) "Listening..." else "Tap to Speak",
                            color = CyanNeon,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.3.sp
                        )
                    }
                }
            }
        }
    }
}

/**
 * Feature Card Item matching exact dimensions, icons, and typography of the reference screenshot.
 * Without extraneous arrow buttons.
 */
@Composable
private fun FeatureCardItem(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    gradientColors: List<Color>,
    borderColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = Color(0xFF0B172A),
        border = BorderStroke(1.dp, borderColor),
        shadowElevation = 8.dp,
        modifier = modifier.height(116.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(gradientColors))
                .padding(14.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Circular Glowing Icon
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(iconColor.copy(alpha = 0.18f))
                        .border(1.dp, iconColor.copy(alpha = 0.45f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        icon,
                        contentDescription = title,
                        tint = iconColor,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Title and Subtitle Text
                Column {
                    Text(
                        text = title,
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.2.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        color = iconColor.copy(alpha = 0.9f),
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
