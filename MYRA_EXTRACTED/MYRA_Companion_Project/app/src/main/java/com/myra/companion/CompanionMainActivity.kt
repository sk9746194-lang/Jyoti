package com.myra.companion

import android.content.Context
import android.content.Intent
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.myra.companion.service.CompanionAccessibilityService
import com.myra.companion.service.CompanionHotspotServerService
import com.myra.companion.service.CompanionScreenCaptureService
import java.net.Inet4Address
import java.net.NetworkInterface

class CompanionMainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Start background connection server service
        val serverIntent = Intent(this, CompanionHotspotServerService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serverIntent)
        } else {
            startService(serverIntent)
        }

        setContent {
            CompanionAppScreen(
                onOpenAccessibilitySettings = {
                    startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                },
                onStopAll = {
                    CompanionHotspotServerService.authorizedAuthToken = ""
                    CompanionScreenCaptureService.stop(this)
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompanionAppScreen(
    onOpenAccessibilitySettings: () -> Unit,
    onStopAll: () -> Unit
) {
    var localIp by remember { mutableStateOf("127.0.0.1") }
    val isAccessibilityActive = CompanionAccessibilityService.isRunning()
    val isScreenStreaming = CompanionScreenCaptureService.isRunning

    LaunchedEffect(Unit) {
        localIp = resolveLocalIp()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "MYRA Companion",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                        Text(
                            text = "Secondary Phone (Android 15) • Hotspot Node",
                            style = MaterialTheme.typography.labelSmall.copy(color = Color.LightGray)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0F172A))
            )
        },
        containerColor = Color(0xFF020617)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Hotspot Connection Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF10B981))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "HOTSPOT SERVER ACTIVE",
                                color = Color(0xFF10B981),
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }

                        Text(
                            text = "PORT: 9876",
                            color = Color(0xFF00E5FF),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "इस फोन का Hotspot IP:",
                        color = Color.LightGray,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = localIp,
                        color = Color(0xFF00E5FF),
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    Divider(color = Color(0xFF1E293B))
                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "6-DIGIT PAIRING PIN:",
                        color = Color.LightGray,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF1E293B))
                            .border(1.dp, Color(0xFF00E5FF), RoundedCornerShape(10.dp))
                            .padding(horizontal = 20.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = CompanionHotspotServerService.hostPairingPin,
                            color = Color(0xFF00E5FF),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 4.sp
                        )
                    }
                }
            }

            // 2. Service Permissions & Status Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "📱 REMOTE ACCESS CONTROLS",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Accessibility Service Status
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Touch & Swipe Automation", color = Color.White, fontSize = 13.sp)
                            Text(
                                text = if (isAccessibilityActive) "Accessibility Service Enabled" else "Service Disabled",
                                color = if (isAccessibilityActive) Color(0xFF10B981) else Color(0xFFFFB300),
                                fontSize = 11.sp
                            )
                        }

                        if (!isAccessibilityActive) {
                            Button(
                                onClick = onOpenAccessibilitySettings,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Enable", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Icon(Icons.Default.CheckCircle, contentDescription = "Active", tint = Color(0xFF10B981))
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Divider(color = Color(0xFF1E293B))
                    Spacer(modifier = Modifier.height(12.dp))

                    // Screen Streaming Status
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Live Screen Stream", color = Color.White, fontSize = 13.sp)
                            Text(
                                text = if (isScreenStreaming) "Streaming to Primary Phone" else "Idle (Awaiting Request)",
                                color = if (isScreenStreaming) Color(0xFF10B981) else Color.LightGray,
                                fontSize = 11.sp
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isScreenStreaming) Color(0xFF10B981).copy(alpha = 0.2f) else Color(0xFF1E293B))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = if (isScreenStreaming) "STREAMING" else "STANDBY",
                                color = if (isScreenStreaming) Color(0xFF10B981) else Color.Gray,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // 3. Emergency Disconnect Card
            Button(
                onClick = onStopAll,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF991B1B))
            ) {
                Icon(Icons.Default.Close, contentDescription = "Disconnect All", tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Emergency: Disconnect & Kill All Sessions", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}

private fun resolveLocalIp(): String {
    try {
        val interfaces = NetworkInterface.getNetworkInterfaces()
        while (interfaces.hasMoreElements()) {
            val intf = interfaces.nextElement()
            if (intf.isLoopback || !intf.isUp) continue
            val addrs = intf.inetAddresses
            while (addrs.hasMoreElements()) {
                val addr = addrs.nextElement()
                if (!addr.isLoopbackAddress && addr is Inet4Address) {
                    return addr.hostAddress ?: "127.0.0.1"
                }
            }
        }
    } catch (_: Exception) {}
    return "127.0.0.1"
}
