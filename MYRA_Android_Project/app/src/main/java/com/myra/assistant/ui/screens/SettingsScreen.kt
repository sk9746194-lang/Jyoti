package com.myra.assistant.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Wallpaper
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
import com.myra.assistant.data.PreferencesManager
import com.myra.assistant.ui.orb.OrbCustomizationManager
import com.myra.assistant.ui.orb.OrbRenderer
import com.myra.assistant.ui.theme.*
import com.myra.assistant.wallpaper.MyraWallpaperPreferences

@Composable
fun SettingsScreen(
    onBack: (() -> Unit)? = null,
    onOpenConnect: () -> Unit = {}
) {
    val context = LocalContext.current
    val prefs = remember { PreferencesManager(context) }
    val wallpaperPrefs = remember { MyraWallpaperPreferences.getInstance(context) }
    val orbManager = remember { OrbCustomizationManager.getInstance(context) }
    val orbSettings by orbManager.settingsState

    var showOrbStudio by remember { mutableStateOf(false) }
    var showWallpaperSettings by remember { mutableStateOf(false) }
    var showSecurityScreen by remember { mutableStateOf(false) }

    var wakeWord by remember { mutableStateOf(prefs.wakeWord) }
    var bluetoothAudio by remember { mutableStateOf(prefs.bluetoothAudio) }
    var speedRate by remember { mutableFloatStateOf(prefs.speechRate) }

    if (showSecurityScreen) {
        SecurityScreen(onBack = { showSecurityScreen = false })
        return
    }

    if (showOrbStudio) {
        OrbCustomizationScreen(onBack = { showOrbStudio = false })
        return
    }

    if (showWallpaperSettings) {
        MyraWallpaperSettingsScreen(onBack = { showWallpaperSettings = false })
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (onBack != null) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                }
                Column {
                    Text(text = "MYRA Assistant Settings", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text(text = "Customizable Orb, Visuals, Voice engine & Audio", color = TextSecondary, fontSize = 12.sp)
                }
            }
        }

        // 🔮 Orb Gallery & Screen Effect Studio Banner Card
        item {
            Surface(
                color = PrimaryBlue.copy(alpha = 0.15f),
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.5.dp, PrimaryBlue),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showOrbStudio = true }
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(BgDark),
                            contentAlignment = Alignment.Center
                        ) {
                            OrbRenderer(
                                state = AssistantState.IDLE,
                                audioLevel = 0.2f,
                                settings = orbSettings.copy(sizeDp = 46f)
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Orb & Screen Studio",
                                    color = TextPrimary,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    color = AccentEmerald.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        "10 STYLES",
                                        color = AccentEmerald,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = "${orbSettings.style.displayName} • ${orbSettings.themeColor.displayName}",
                                color = LightBlue,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Edge Glow: ${orbSettings.edgeEffectMode.displayName} • ${orbSettings.backgroundStyle.displayName}",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = "Open Studio",
                        tint = PrimaryBlue,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        // 🛡️ MYRA Security — Owner Voice & Biometric Shield Card
        item {
            val securityManager = remember { com.myra.assistant.security.OwnerSecurityManager.getInstance(context) }
            val isSecured = securityManager.isOwnerProtectionEnabled
            val isEnrolled = securityManager.isOwnerVoiceEnrolled

            Surface(
                color = AccentEmerald.copy(alpha = 0.12f),
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.5.dp, AccentEmerald.copy(alpha = 0.7f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showSecurityScreen = true }
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            color = AccentEmerald.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.size(46.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Security,
                                    contentDescription = null,
                                    tint = AccentEmerald,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "MYRA Security",
                                    color = TextPrimary,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    color = if (isSecured) AccentEmerald.copy(alpha = 0.25f) else Color.Gray.copy(alpha = 0.25f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        if (isSecured) "PROTECTED" else "ENROLL",
                                        color = if (isSecured) AccentEmerald else TextSecondary,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = "Owner Voice: ${if (isEnrolled) "Enrolled" else "Not Enrolled"} • Biometric: ${if (securityManager.isBiometricLockEnabled) "ON" else "OFF"}",
                                color = LightBlue,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Fingerprint / Face ID & Owner Verification for Sensitive Actions",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = "Open Security",
                        tint = AccentEmerald,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        // 📱 MYRA Connect — Phone to Phone Card
        item {
            Surface(
                color = CyanNeon.copy(alpha = 0.12f),
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.5.dp, CyanNeon.copy(alpha = 0.7f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenConnect() }
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            color = CyanNeon.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.size(46.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.PhoneAndroid,
                                    contentDescription = null,
                                    tint = CyanNeon,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "MYRA Connect",
                                    color = TextPrimary,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    color = CyanNeon.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        "PHONE TO PHONE",
                                        color = CyanNeon,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = "Pair with second Android phone • Wi-Fi / Hotspot",
                                color = LightBlue,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Remote YouTube, Camera, WhatsApp & hardware actions",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = "Open Connect",
                        tint = CyanNeon,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        // 🌌 MYRA Live Wallpaper Card
        item {
            Surface(
                color = Color(0xFF6B21A8).copy(alpha = 0.15f),
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.5.dp, Color(0xFFC084FC).copy(alpha = 0.7f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showWallpaperSettings = true }
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            color = Color(0xFFC084FC).copy(alpha = 0.2f),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.size(46.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Wallpaper,
                                    contentDescription = null,
                                    tint = Color(0xFFE879F9),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "MYRA Live Wallpaper",
                                    color = TextPrimary,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    color = Color(0xFFC084FC).copy(alpha = 0.25f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        "LIVE 3D AI",
                                        color = Color(0xFFE879F9),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = "Holographic Earth • HUD Rings • Neon AI Avatar",
                                color = LightBlue,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Theme: ${wallpaperPrefs.theme.name.replace("_", " + ")} • ${wallpaperPrefs.quality.title}",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = "Open Wallpaper Settings",
                        tint = Color(0xFFE879F9),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        // Wake Phrase / Hotword
        item {
            Surface(
                color = CardDark,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, BorderSlate),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(text = "Wake Phrase / Hotword", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Text(text = "Default: 'Hey MYRA' (Always-on mic detection)", color = TextSecondary, fontSize = 11.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = wakeWord,
                        onValueChange = {
                            wakeWord = it
                            prefs.wakeWord = it
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryBlue,
                            unfocusedBorderColor = BorderSlate,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }
        }

        // Bluetooth Audio SCO
        item {
            Surface(
                color = CardDark,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, BorderSlate),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "Bluetooth Headset / SCO", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            Text(text = "Route mic and audio via car or Bluetooth earbuds", color = TextSecondary, fontSize = 11.sp)
                        }
                        Switch(
                            checked = bluetoothAudio,
                            onCheckedChange = {
                                bluetoothAudio = it
                                prefs.bluetoothAudio = it
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = LightBlue, checkedTrackColor = PrimaryBlue)
                        )
                    }
                }
            }
        }

        // Speech Rate (Speaking Speed)
        item {
            Surface(
                color = CardDark,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, BorderSlate),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(text = "Speech Rate (Speaking Speed): ${"%.1f".format(speedRate)}x", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Slider(
                        value = speedRate,
                        onValueChange = {
                            speedRate = it
                            prefs.speechRate = it
                        },
                        valueRange = 0.5f..2.0f,
                        steps = 15,
                        colors = SliderDefaults.colors(thumbColor = LightBlue, activeTrackColor = PrimaryBlue)
                    )
                }
            }
        }
    }
}
