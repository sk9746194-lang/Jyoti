package com.myra.assistant.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.myra.assistant.ui.theme.*

data class PopularCommand(
    val title: String,
    val hindiPhrase: String,
    val command: String,
    val icon: ImageVector
)

data class DailyGoal(
    val id: String,
    val title: String,
    val target: String,
    var isCompleted: Boolean
)

@Composable
fun ToolsScreen(
    onExecuteCommand: (String) -> Unit,
    onOpenConnect: () -> Unit = {}
) {
    var goals by remember {
        mutableStateOf(
            listOf(
                DailyGoal("1", "Morning Hindi Briefing", "Daily News & Weather", true),
                DailyGoal("2", "Daily Water Hydration", "2.5 Liters reminder", false),
                DailyGoal("3", "System Diagnostics & Memory", "Phone cache optimize", false),
                DailyGoal("4", "Evening Walk / Step Goal", "6,000 steps tracker", true)
            )
        )
    }

    val popularCommands = listOf(
        PopularCommand("Remote YouTube", "\"दूसरे फोन पर YouTube खोलो\"", "दूसरे फोन पर YouTube खोलो", Icons.Default.SmartDisplay),
        PopularCommand("Remote Camera", "\"घर वाले फोन पर Camera खोलो\"", "घर वाले फोन पर Camera खोलो", Icons.Default.CameraAlt),
        PopularCommand("Launch Chrome", "\"Chrome kholo\"", "Chrome kholo", Icons.Default.Public),
        PopularCommand("Play YouTube", "\"YouTube chalao\"", "YouTube chalao", Icons.Default.PlayCircle),
        PopularCommand("Play Music", "\"Music chalao\"", "Music chalao", Icons.Default.MusicNote),
        PopularCommand("WhatsApp Message", "\"Rahul ko WhatsApp message bhejo\"", "Rahul ko WhatsApp message bhejo main aa raha hoon", Icons.Default.Chat),
        PopularCommand("Open WhatsApp", "\"WhatsApp kholo\"", "WhatsApp kholo", Icons.Default.ChatBubbleOutline),
        PopularCommand("Open Camera", "\"Camera open karo\"", "Camera open karo", Icons.Default.CameraAlt),
        PopularCommand("Remote Torch", "\"दूसरे फोन पर टॉर्च जलाओ\"", "दूसरे फोन पर टॉर्च जलाओ", Icons.Default.FlashlightOn),
        PopularCommand("Flashlight ON", "\"Torch jalao\"", "Torch jalao", Icons.Default.FlashlightOn),
        PopularCommand("Flashlight OFF", "\"Torch band karo\"", "Torch band karo", Icons.Default.FlashlightOff),
        PopularCommand("Check Battery", "\"Battery kitni hai?\"", "Battery check karo", Icons.Default.BatteryChargingFull),
        PopularCommand("Lock Device", "\"Phone lock karo\"", "Phone lock karo", Icons.Default.Lock),
        PopularCommand("Adjust Volume", "\"Volume 80% karo\"", "Volume 80% karo", Icons.Default.VolumeUp),
        PopularCommand("Quick Phone Call", "\"Papa ko call lagao\"", "Papa ko call lagao", Icons.Default.Call),
        PopularCommand("Set Daily Alarm", "\"Subah 6 baje ka alarm lagao\"", "Subah 6:00 baje ka alarm set karo", Icons.Default.Alarm)
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        item {
            Column {
                Text(
                    text = "MYRA Tools & Automations",
                    color = TextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Hinglish voice shortcuts, daily productivity goals & device tools",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }
        }

        // MYRA Connect Banner
        item {
            Surface(
                color = CardDark,
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, CyanNeon.copy(alpha = 0.5f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenConnect() }
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Surface(
                            color = CyanNeon.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.size(44.dp)
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
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "MYRA Connect",
                                    color = TextPrimary,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Phone to Phone",
                                    color = CyanNeon,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = "दूसरे फोन से सुरक्षित कनेक्ट करें और रिमोट कमांड चलाएं",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }
                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = "Open",
                        tint = CyanNeon,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Popular Commands Carousel / Horizontal Grid
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "⚡ Popular Hindi / Hinglish Voice Commands",
                    color = AccentIndigo,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(horizontal = 2.dp)
                ) {
                    items(popularCommands) { item ->
                        Surface(
                            color = CardDark,
                            shape = RoundedCornerShape(14.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, BorderSlate),
                            modifier = Modifier
                                .width(160.dp)
                                .clickable { onExecuteCommand(item.command) }
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Icon(item.icon, contentDescription = item.title, tint = LightBlue, modifier = Modifier.size(22.dp))
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(text = item.title, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                Text(text = item.hindiPhrase, color = TextSecondary, fontSize = 11.sp)
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(text = "Tap to Run →", color = PrimaryBlue, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // Daily Goals Section
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🎯 Daily Goals & Habits",
                        color = AccentEmerald,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${goals.count { it.isCompleted }}/${goals.size} Done",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }

                goals.forEachIndexed { index, goal ->
                    Surface(
                        color = CardDark,
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSlate),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp)
                            .clickable {
                                goals = goals.toMutableList().also { list ->
                                    list[index] = list[index].copy(isCompleted = !list[index].isCompleted)
                                }
                            }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = goal.title,
                                    color = if (goal.isCompleted) TextSecondary else TextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(text = goal.target, color = TextSecondary, fontSize = 11.sp)
                            }
                            Checkbox(
                                checked = goal.isCompleted,
                                onCheckedChange = { checked ->
                                    goals = goals.toMutableList().also { list ->
                                        list[index] = list[index].copy(isCompleted = checked)
                                    }
                                },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = AccentEmerald,
                                    uncheckedColor = BorderSlate
                                )
                            )
                        }
                    }
                }
            }
        }

        // Deep System Tools
        val advancedTools = listOf(
            Triple("Emergency SOS & Siren", "High-priority SMS & loud siren activation", "emergency sos"),
            Triple("Flashlight Torch", "Hardware torch ON/OFF toggle", "torch jalao"),
            Triple("Battery Health Monitor", "Checks battery level, voltage & charging state", "battery status"),
            Triple("Direct Phone Dialer", "Place instant voice call via SIM", "call papa"),
            Triple("WhatsApp Quick Action", "Automate WhatsApp chat without typing", "whatsapp kholo"),
            Triple("System Lock & Protect", "Instant lock screen overlay invocation", "phone lock karo")
        )

        item {
            Text(
                text = "🛠️ System Automation Tools",
                color = AccentAmber,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }

        items(advancedTools.size) { idx ->
            val (title, desc, cmd) = advancedTools[idx]
            Surface(
                color = CardDark,
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderSlate),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = title, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Text(text = desc, color = TextSecondary, fontSize = 11.sp)
                    }
                    Button(
                        onClick = { onExecuteCommand(cmd) },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(text = "Run", fontSize = 11.sp)
                    }
                }
            }
        }
    }
}
