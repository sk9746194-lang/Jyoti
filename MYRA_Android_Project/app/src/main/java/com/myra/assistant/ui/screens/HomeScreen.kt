package com.myra.assistant.ui.screens

import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.myra.assistant.R
import com.myra.assistant.data.AssistantState
import com.myra.assistant.data.PreferencesManager
import com.myra.assistant.ui.theme.*

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

    // Pulsing animation for active equalizer bars in speech card
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseHeight1 by infiniteTransition.animateFloat(
        initialValue = 6f, targetValue = 22f,
        animationSpec = infiniteRepeatable(tween(450, easing = LinearEasing), RepeatMode.Reverse),
        label = "h1"
    )
    val pulseHeight2 by infiniteTransition.animateFloat(
        initialValue = 18f, targetValue = 8f,
        animationSpec = infiniteRepeatable(tween(550, easing = LinearEasing), RepeatMode.Reverse),
        label = "h2"
    )
    val pulseHeight3 by infiniteTransition.animateFloat(
        initialValue = 10f, targetValue = 26f,
        animationSpec = infiniteRepeatable(tween(380, easing = LinearEasing), RepeatMode.Reverse),
        label = "h3"
    )
    val pulseHeight4 by infiniteTransition.animateFloat(
        initialValue = 20f, targetValue = 10f,
        animationSpec = infiniteRepeatable(tween(600, easing = LinearEasing), RepeatMode.Reverse),
        label = "h4"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF030712), // Deepest obsidian
                        Color(0xFF070D1D), // Midnight navy
                        Color(0xFF0A1128), // Cyber navy
                        Color(0xFF050814)
                    )
                )
            )
    ) {
        // Ambient background glow accents
        Box(
            modifier = Modifier
                .size(260.dp)
                .offset(x = (-60).dp, y = (-40).dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0xFF1E3A8A).copy(alpha = 0.25f), Color.Transparent)
                    )
                )
        )
        Box(
            modifier = Modifier
                .size(240.dp)
                .align(Alignment.TopEnd)
                .offset(x = 60.dp, y = 140.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0xFF7C3AED).copy(alpha = 0.20f), Color.Transparent)
                    )
                )
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // -------------------------------------------------------------
            // 1. TOP HEADER: MENU, MYRA BRANDING, ONLINE STATUS, SETTINGS
            // -------------------------------------------------------------
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onOpenMenu,
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF0D1424))
                                .border(1.dp, BorderSlate, RoundedCornerShape(12.dp))
                        ) {
                            Icon(
                                Icons.Default.Menu,
                                contentDescription = "Menu",
                                tint = LightBlue,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "MYRA",
                                    color = Color.White,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 0.5.sp
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "♡",
                                    color = Color(0xFFF43F5E),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = "Your AI Companion",
                                color = LightBlue,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Online Pill Badge
                        Surface(
                            color = Color(0xFF0A1526).copy(alpha = 0.9f),
                            shape = RoundedCornerShape(20.dp),
                            border = BorderStroke(1.dp, CyanNeon.copy(alpha = 0.4f)),
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
                                        .background(AccentEmerald)
                                        .shadow(4.dp, CircleShape)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Online",
                                    color = Color(0xFFA7F3D0),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        // Settings Quick Gear Icon
                        IconButton(
                            onClick = onNavigateToSettings,
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF0D1424))
                                .border(1.dp, BorderSlate, RoundedCornerShape(12.dp))
                        ) {
                            Icon(
                                Icons.Default.Settings,
                                contentDescription = "Settings",
                                tint = LightBlue,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                    }
                }
            }

            // -------------------------------------------------------------
            // 2. HERO PORTRAIT CARD (CLEAN DISPLAY - CUSTOMIZED IN SETTINGS)
            // -------------------------------------------------------------
            item {
                Surface(
                    shape = RoundedCornerShape(26.dp),
                    color = Color(0xFF0A0F1D),
                    border = BorderStroke(1.dp, Brush.linearGradient(listOf(CyanNeon.copy(alpha = 0.6f), Color(0xFF8B5CF6).copy(alpha = 0.4f)))),
                    shadowElevation = 12.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(380.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        // Background Photo (Custom or Bundled Default)
                        if (customBitmap != null) {
                            Image(
                                bitmap = customBitmap,
                                contentDescription = "MYRA Home Portrait",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Image(
                                painter = painterResource(id = R.drawable.default_home_photo),
                                contentDescription = "MYRA Default Portrait",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        // Gradient protection overlay to guarantee crisp readability
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colorStops = arrayOf(
                                            0.0f to Color.Black.copy(alpha = 0.25f),
                                            0.5f to Color.Black.copy(alpha = 0.15f),
                                            1.0f to Color(0xFF050814).copy(alpha = 0.88f)
                                        )
                                    )
                                )
                        )

                        // Top-left Greeting & Feature badges
                        Column(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(16.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Hello\nI'm MYRA",
                                    color = Color.White,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    lineHeight = 26.sp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "♡",
                                    color = Color(0xFFF43F5E),
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = "Your Personal AI\nCompanion",
                                color = LightBlue,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                lineHeight = 14.sp,
                                modifier = Modifier.padding(top = 4.dp, bottom = 10.dp)
                            )

                            // Quick Feature Tags
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                FeatureTag(icon = Icons.Default.Chat, title = "Chat", subtitle = "Always here for you", color = CyanNeon)
                                FeatureTag(icon = Icons.Default.Mic, title = "Voice", subtitle = "Talk naturally", color = LightBlue)
                                FeatureTag(icon = Icons.Default.AutoAwesome, title = "Smart", subtitle = "Powered by Gemini", color = Color(0xFFA78BFA))
                                FeatureTag(icon = Icons.Default.Favorite, title = "Always with you", subtitle = "Anytime, Anywhere", color = Color(0xFFFDA4AF))
                            }
                        }

                        // Bottom-right Calligraphy & Photo Edit Trigger
                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(14.dp),
                            horizontalAlignment = Alignment.End
                        ) {
                            Text(
                                text = "मुस्कान\nहमेशा रहे ♡",
                                color = Color.White.copy(alpha = 0.95f),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.End,
                                lineHeight = 20.sp,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }
                    }
                }
            }

            // -------------------------------------------------------------
            // 3. SPEECH PROMPT CARD ("मुझसे कुछ कहिए..." + SAY HEY MYRA)
            // -------------------------------------------------------------
            item {
                Surface(
                    onClick = onStartVoiceListening,
                    shape = RoundedCornerShape(22.dp),
                    color = Color(0xFF0C1326).copy(alpha = 0.92f),
                    border = BorderStroke(1.5.dp, Brush.horizontalGradient(listOf(CyanNeon, Color(0xFF3B82F6), Color(0xFF8B5CF6)))),
                    shadowElevation = 8.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            // Glowing Mic Orb Button
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.linearGradient(
                                            listOf(
                                                if (isListening) Color(0xFFE11D48) else CyanNeon,
                                                if (isListening) Color(0xFFBE123C) else PrimaryBlue
                                            )
                                        )
                                    )
                                    .border(1.5.dp, Color.White.copy(alpha = 0.8f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    if (isListening) Icons.Default.MicOff else Icons.Default.Mic,
                                    contentDescription = "Voice",
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column {
                                Text(
                                    text = if (isListening) "सुन रही हूँ... बोलिए" else "मुझसे कुछ कहिए...",
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (isListening) "Tap to pause speech" else "Say \"Hey MYRA\" or tap to start",
                                    color = LightBlue,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        // Animated Audio Waveform Bars
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp),
                            modifier = Modifier.padding(start = 8.dp)
                        ) {
                            Box(modifier = Modifier.width(3.dp).height(pulseHeight1.dp).clip(CircleShape).background(CyanNeon))
                            Box(modifier = Modifier.width(3.dp).height(pulseHeight2.dp).clip(CircleShape).background(Color(0xFF60A5FA)))
                            Box(modifier = Modifier.width(3.dp).height(pulseHeight3.dp).clip(CircleShape).background(Color(0xFFA78BFA)))
                            Box(modifier = Modifier.width(3.dp).height(pulseHeight4.dp).clip(CircleShape).background(CyanNeon))
                        }
                    }
                }
            }

            // -------------------------------------------------------------
            // 4. FOUR FUNCTIONAL ACTION CARDS (CHAT, VOICE, TOOLS, SETTINGS)
            // -------------------------------------------------------------
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Card 1: Chat (Text & Voice)
                        ActionGridCard(
                            title = "Chat",
                            subtitle = "Text & Voice",
                            icon = Icons.Default.Chat,
                            gradientColors = listOf(Color(0xFF0F1E38), Color(0xFF142C54)),
                            iconColor = CyanNeon,
                            onClick = onNavigateToChat,
                            modifier = Modifier.weight(1f)
                        )

                        // Card 2: Voice (Hands-Free)
                        ActionGridCard(
                            title = "Voice",
                            subtitle = "Hands-Free",
                            icon = Icons.Default.Mic,
                            gradientColors = listOf(Color(0xFF1B1438), Color(0xFF2C1952)),
                            iconColor = Color(0xFFA78BFA),
                            onClick = onNavigateToVoice,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Card 3: Tools (Smart Utilities)
                        ActionGridCard(
                            title = "Tools",
                            subtitle = "Smart Utilities",
                            icon = Icons.Default.Build,
                            gradientColors = listOf(Color(0xFF0C2424), Color(0xFF133838)),
                            iconColor = AccentEmerald,
                            onClick = onNavigateToTools,
                            modifier = Modifier.weight(1f)
                        )

                        // Card 4: Settings (Personalize)
                        ActionGridCard(
                            title = "Settings",
                            subtitle = "Personalize",
                            icon = Icons.Default.Settings,
                            gradientColors = listOf(Color(0xFF261922), Color(0xFF381B2D)),
                            iconColor = Color(0xFFFDA4AF),
                            onClick = onNavigateToSettings,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FeatureTag(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    color: Color
) {
    Surface(
        color = Color(0xFF030712).copy(alpha = 0.70f),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(0.8.dp, color.copy(alpha = 0.4f))
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(12.dp))
            }
            Spacer(modifier = Modifier.width(6.dp))
            Column {
                Text(text = title, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Text(text = subtitle, color = TextSecondary, fontSize = 8.5.sp)
            }
        }
    }
}

@Composable
private fun ActionGridCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    gradientColors: List<Color>,
    iconColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        color = Color(0xFF0D1424),
        border = BorderStroke(1.dp, iconColor.copy(alpha = 0.35f)),
        shadowElevation = 6.dp,
        modifier = modifier.height(108.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(gradientColors))
                .padding(12.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(iconColor.copy(alpha = 0.15f))
                            .border(1.dp, iconColor.copy(alpha = 0.4f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            icon,
                            contentDescription = title,
                            tint = iconColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.08f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.ArrowForward,
                            contentDescription = "Open",
                            tint = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }

                Column {
                    Text(
                        text = title,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = subtitle,
                        color = LightBlue,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
