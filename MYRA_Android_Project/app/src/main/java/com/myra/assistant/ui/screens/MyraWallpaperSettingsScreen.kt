package com.myra.assistant.ui.screens

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.myra.assistant.ui.theme.*
import com.myra.assistant.wallpaper.*

@Composable
fun MyraWallpaperSettingsScreen(
    onBack: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val prefs = remember { MyraWallpaperPreferences.getInstance(context) }

    var isEnabled by remember { mutableStateOf(prefs.isEnabled) }
    var quality by remember { mutableStateOf(prefs.quality) }
    var speed by remember { mutableStateOf(prefs.speed) }
    var theme by remember { mutableStateOf(prefs.theme) }
    var showAppIcons by remember { mutableStateOf(prefs.showAppIcons) }
    var showOrb by remember { mutableStateOf(prefs.showOrb) }
    var showEarth by remember { mutableStateOf(prefs.showEarth) }
    var particleEffects by remember { mutableStateOf(prefs.particleEffects) }
    var showHudRings by remember { mutableStateOf(prefs.showHudRings) }
    var showWaveform by remember { mutableStateOf(prefs.showWaveform) }
    var batterySaverMode by remember { mutableStateOf(prefs.batterySaverMode) }

    fun launchSetWallpaper() {
        try {
            val intent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).apply {
                putExtra(
                    WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                    ComponentName(context, MyraLiveWallpaperService::class.java)
                )
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            // Fallback for devices where ACTION_CHANGE_LIVE_WALLPAPER is not directly supported
            try {
                val fallbackIntent = Intent(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER)
                context.startActivity(fallbackIntent)
            } catch (e2: Exception) {
                Toast.makeText(context, "System live wallpaper picker unavailable", Toast.LENGTH_SHORT).show()
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Bar
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
                    Text(
                        text = "MYRA Live Wallpaper",
                        color = TextPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Futuristic AI Hologram • Earth • HUD Rings • Neon",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }
            }
        }

        // Action Hero Card: Set as Wallpaper
        item {
            Surface(
                color = PrimaryBlue.copy(alpha = 0.15f),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.5.dp, PrimaryBlue),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = PrimaryBlue.copy(alpha = 0.25f),
                                modifier = Modifier.size(46.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.Wallpaper,
                                        contentDescription = null,
                                        tint = PrimaryBlue,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Active Wallpaper Engine",
                                    color = TextPrimary,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (isEnabled) "Ready to apply to Home & Lock screen" else "Wallpaper disabled",
                                    color = if (isEnabled) AccentEmerald else TextSecondary,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        Switch(
                            checked = isEnabled,
                            onCheckedChange = {
                                isEnabled = it
                                prefs.isEnabled = it
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = PrimaryBlue
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = { launchSetWallpaper() },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Set as Live Wallpaper",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Section: Neon Theme Selection
        item {
            Text(
                text = "NEON COLOR THEME",
                color = LightBlue,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                WallpaperTheme.values().forEach { t ->
                    val isSelected = theme == t
                    Surface(
                        color = if (isSelected) PrimaryBlue.copy(alpha = 0.25f) else CardDark,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(
                            1.5.dp,
                            if (isSelected) PrimaryBlue else Color.White.copy(alpha = 0.1f)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                theme = t
                                prefs.theme = t
                            }
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 12.dp, horizontal = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            val dotColor = when (t) {
                                WallpaperTheme.BLUE -> Color(0xFF00E5FF)
                                WallpaperTheme.RED -> Color(0xFFFF2A55)
                                WallpaperTheme.BLUE_RED -> Color(0xFF8A2BE2)
                            }
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .background(dotColor)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = t.name.replace("_", " + "),
                                color = if (isSelected) TextPrimary else TextSecondary,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        }

        // Section: Quality & Speed
        item {
            Text(
                text = "ANIMATION PERFORMANCE",
                color = LightBlue,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))

            Surface(
                color = CardDark,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    // Quality
                    Column {
                        Text(text = "Target Frame Rate / Quality", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            WallpaperQuality.values().forEach { q ->
                                val selected = quality == q
                                Surface(
                                    color = if (selected) PrimaryBlue else Color.Black.copy(alpha = 0.3f),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            quality = q
                                            prefs.quality = q
                                        }
                                ) {
                                    Text(
                                        text = q.title,
                                        color = if (selected) Color.White else TextSecondary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                }
                            }
                        }
                    }

                    // Speed
                    Column {
                        Text(text = "Animation Speed", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            WallpaperSpeed.values().forEach { s ->
                                val selected = speed == s
                                Surface(
                                    color = if (selected) CyanNeon else Color.Black.copy(alpha = 0.3f),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            speed = s
                                            prefs.speed = s
                                        }
                                ) {
                                    Text(
                                        text = s.title,
                                        color = if (selected) Color.Black else TextSecondary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Section: Visual Elements Toggles
        item {
            Text(
                text = "SCENERY & COMPONENT TOGGLES",
                color = LightBlue,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))

            Surface(
                color = CardDark,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SettingToggleRow(
                        title = "Show Holographic Earth",
                        subtitle = "Animated rotating 3D Earth behind MYRA",
                        checked = showEarth,
                        onCheckedChange = {
                            showEarth = it
                            prefs.showEarth = it
                        }
                    )
                    Divider(color = Color.White.copy(alpha = 0.06f))

                    SettingToggleRow(
                        title = "Show MYRA Central Orb",
                        subtitle = "Breathing glowing AI core with cyber rings",
                        checked = showOrb,
                        onCheckedChange = {
                            showOrb = it
                            prefs.showOrb = it
                        }
                    )
                    Divider(color = Color.White.copy(alpha = 0.06f))

                    SettingToggleRow(
                        title = "Show Radial App Icons",
                        subtitle = "Floating WhatsApp, Chrome, YouTube & Tools",
                        checked = showAppIcons,
                        onCheckedChange = {
                            showAppIcons = it
                            prefs.showAppIcons = it
                        }
                    )
                    Divider(color = Color.White.copy(alpha = 0.06f))

                    SettingToggleRow(
                        title = "Cosmic Particle Effects",
                        subtitle = "Floating energy sparks and moving stars",
                        checked = particleEffects,
                        onCheckedChange = {
                            particleEffects = it
                            prefs.particleEffects = it
                        }
                    )
                    Divider(color = Color.White.copy(alpha = 0.06f))

                    SettingToggleRow(
                        title = "Rotating Cyber HUD Rings",
                        subtitle = "Concentric glowing rings with radar telemetry",
                        checked = showHudRings,
                        onCheckedChange = {
                            showHudRings = it
                            prefs.showHudRings = it
                        }
                    )
                    Divider(color = Color.White.copy(alpha = 0.06f))

                    SettingToggleRow(
                        title = "Sound Waveform Audio Aura",
                        subtitle = "Futuristic equalizer light waves around orb",
                        checked = showWaveform,
                        onCheckedChange = {
                            showWaveform = it
                            prefs.showWaveform = it
                        }
                    )
                    Divider(color = Color.White.copy(alpha = 0.06f))

                    SettingToggleRow(
                        title = "Smart Battery Saver",
                        subtitle = "Caps FPS at 30, pauses completely when screen is off",
                        checked = batterySaverMode,
                        onCheckedChange = {
                            batterySaverMode = it
                            prefs.batterySaverMode = it
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text(text = subtitle, color = TextSecondary, fontSize = 11.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = PrimaryBlue
            )
        )
    }
}
