package com.myra.assistant.ui.screens

import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.myra.assistant.connect.*
import com.myra.assistant.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyraConnectScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val connectManager = remember { MyraConnectManager.getInstance(context) }

    val pairedDevices by connectManager.pairedDevices
    val localIp by connectManager.localIp
    val activeScreenFrame by connectManager.activeScreenFrame
    val isScreenStreaming by connectManager.isScreenStreaming
    val fileTransferProgress by connectManager.fileTransferProgress
    val discoveredDevices by connectManager.discoveredDevices

    var selectedTargetDevice by remember { mutableStateOf<PairedDevice?>(null) }
    LaunchedEffect(pairedDevices) {
        if (selectedTargetDevice == null && pairedDevices.isNotEmpty()) {
            selectedTargetDevice = pairedDevices.firstOrNull { it.status == ConnectionStatus.CONNECTED } ?: pairedDevices.first()
        }
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        val target = selectedTargetDevice
        if (uri != null && target != null) {
            var fileName = "myra_file_${System.currentTimeMillis()}"
            var fileSize = 0L
            val mimeType = context.contentResolver.getType(uri) ?: "application/octet-stream"
            try {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (cursor.moveToFirst()) {
                        if (nameIdx != -1) fileName = cursor.getString(nameIdx)
                        if (sizeIdx != -1) fileSize = cursor.getLong(sizeIdx)
                    }
                }
            } catch (_: Exception) {}

            connectManager.sendFile(target, uri, fileName, fileSize, mimeType) { success, err ->
                // Handled via fileTransferProgress state
            }
        }
    }

    var permissions by remember { mutableStateOf(connectManager.secureStore.getDevicePermissions()) }

    var showPairDialog by remember { mutableStateOf(false) }
    var renameTargetDevice by remember { mutableStateOf<PairedDevice?>(null) }
    var renameNewName by remember { mutableStateOf("") }

    var lastTestResult by remember { mutableStateOf<RemoteCommandResult?>(null) }
    var isExecutingTest by remember { mutableStateOf(false) }
    var isStartingStream by remember { mutableStateOf(false) }
    var streamErrorMessage by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "MYRA Connect",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                        Text(
                            text = "Phone-to-Phone Interconnect • IP: $localIp",
                            style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { connectManager.checkAllDevicesStatus() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = CyanNeon
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundDark)
            )
        },
        containerColor = BackgroundDark
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Connection Status Banner & Add Button
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = CardDark)
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
                                        .background(if (pairedDevices.any { it.status == ConnectionStatus.CONNECTED }) EmeraldGlow else Color.Gray)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (pairedDevices.any { it.status == ConnectionStatus.CONNECTED }) "ONLINE • CONNECTED" else "DISCONNECTED",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (pairedDevices.any { it.status == ConnectionStatus.CONNECTED }) EmeraldGlow else TextSecondary
                                    )
                                )
                            }

                            Button(
                                onClick = { showPairDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = CyanNeon),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Icon(Icons.Default.AddLink, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Add Device", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "घर के दूसरे Android फोन से सुरक्षित जोड़ें और वॉइस कमांड से WhatsApp, YouTube, Camera और टूल्स चलाएं।",
                            style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                        )
                    }
                }
            }

            // 2. Paired Devices List
            item {
                Text(
                    text = "PAIRED PHONES (${pairedDevices.size})",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary,
                        letterSpacing = 1.sp
                    )
                )
            }

            if (pairedDevices.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = CardDark)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.PhoneAndroid, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(40.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("कोई भी फोन पेयर नहीं है", color = Color.White, fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("ऊपर '+ Add Device' बटन दबाकर 6-digit कोड या QR से जोड़ें।", color = TextSecondary, fontSize = 12.sp, textAlign = TextAlign.Center)
                        }
                    }
                }
            } else {
                items(pairedDevices) { device ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = CardDark),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (device.status == ConnectionStatus.CONNECTED) CyanNeon.copy(alpha = 0.4f) else Color.DarkGray
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Smartphone,
                                        contentDescription = null,
                                        tint = if (device.status == ConnectionStatus.CONNECTED) CyanNeon else Color.Gray,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = device.name,
                                            style = MaterialTheme.typography.titleSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                        )
                                        Text(
                                            text = "${device.ipAddress}:${device.port} • Wi-Fi",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = TextSecondary,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        )
                                    }
                                }

                                // Status Badge
                                val (statusBg, statusFg, statusText) = when (device.status) {
                                    ConnectionStatus.CONNECTED -> Triple(EmeraldGlow.copy(alpha = 0.15f), EmeraldGlow, "CONNECTED")
                                    ConnectionStatus.CONNECTING -> Triple(Color(0xFFFFB300).copy(alpha = 0.15f), Color(0xFFFFB300), "CONNECTING")
                                    ConnectionStatus.DISCONNECTED -> Triple(Color.Gray.copy(alpha = 0.15f), Color.LightGray, "OFFLINE")
                                    ConnectionStatus.PAIRING_REQUIRED -> Triple(Color.Red.copy(alpha = 0.15f), Color.Red, "PAIR REQUIRED")
                                }

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(statusBg)
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = statusText,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = statusFg
                                        )
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            Divider(color = Color.DarkGray.copy(alpha = 0.5f))
                            Spacer(modifier = Modifier.height(10.dp))

                            // Action Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(
                                        onClick = {
                                            coroutineScope.launch {
                                                isExecutingTest = true
                                                val online = connectManager.transport.pingDevice(device)
                                                val newStatus = if (online) ConnectionStatus.CONNECTED else ConnectionStatus.DISCONNECTED
                                                connectManager.secureStore.updateDeviceStatus(device.id, newStatus)
                                                connectManager.refreshDevices()
                                                lastTestResult = RemoteCommandResult(
                                                    commandId = "ping",
                                                    status = if (online) RemoteResultStatus.SUCCESS else RemoteResultStatus.NOT_CONNECTED,
                                                    message = if (online) "${device.name} से सफलतापूर्वक संपर्क हुआ (Ping OK)" else "${device.name} से संपर्क नहीं हो पाया"
                                                )
                                                isExecutingTest = false
                                            }
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = CyanNeon)
                                    ) {
                                        Text("Test Ping", fontSize = 12.sp)
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            renameTargetDevice = device
                                            renameNewName = device.name
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.LightGray)
                                    ) {
                                        Text("Rename", fontSize = 12.sp)
                                    }
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    TextButton(
                                        onClick = { connectManager.disconnectDevice(device.id) },
                                        contentPadding = PaddingValues(horizontal = 8.dp)
                                    ) {
                                        Text("Disconnect", color = Color(0xFFFFB300), fontSize = 12.sp)
                                    }

                                    TextButton(
                                        onClick = { connectManager.removeDevice(device.id) },
                                        contentPadding = PaddingValues(horizontal = 8.dp)
                                    ) {
                                        Text("Remove", color = Color.Red, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 2.5 Live Remote Screen Viewer & Touch Interaction Panel
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = CardDark)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "📱 LIVE REMOTE SCREEN & CONTROL",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = CyanNeon,
                                    letterSpacing = 1.sp
                                )
                            )
                            if (isScreenStreaming) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(EmeraldGlow.copy(alpha = 0.2f))
                                        .border(1.dp, EmeraldGlow, RoundedCornerShape(8.dp))
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text("LIVE STREAM", color = EmeraldGlow, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "हॉटस्पॉट से जुड़े दूसरे फोन (Android 15) की लाइव स्क्रीन देखें और टच / स्वाइप करें:",
                            style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Target device selector if multiple
                        if (pairedDevices.size > 1) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("टारगेट फोन:", style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary))
                                pairedDevices.forEach { dev ->
                                    val isSelected = selectedTargetDevice?.id == dev.id
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSelected) CyanNeon.copy(alpha = 0.2f) else Color(0xFF1E293B))
                                            .border(1.dp, if (isSelected) CyanNeon else Color.Gray, RoundedCornerShape(8.dp))
                                            .clickable { selectedTargetDevice = dev }
                                            .padding(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text(dev.name, color = if (isSelected) CyanNeon else Color.White, fontSize = 11.sp)
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                        }

                        val targetDev = selectedTargetDevice

                        if (isScreenStreaming && activeScreenFrame != null && targetDev != null) {
                            // Live Screen Display with Touch Gestures
                            val frame = activeScreenFrame!!
                            val bitmap = remember(frame.jpegBase64) {
                                try {
                                    val bytes = Base64.decode(frame.jpegBase64, Base64.DEFAULT)
                                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                                } catch (_: Exception) {
                                    null
                                }
                            }

                            var containerSize by remember { mutableStateOf(IntSize.Zero) }
                            var dragStartOffset by remember { mutableStateOf<androidx.compose.ui.geometry.Offset?>(null) }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(380.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.Black)
                                    .border(1.dp, CyanNeon.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                    .onGloballyPositioned { containerSize = it.size }
                                    .pointerInput(targetDev) {
                                        detectTapGestures { offset ->
                                            if (containerSize.width > 0 && containerSize.height > 0) {
                                                val normX = (offset.x / containerSize.width).coerceIn(0f, 1f)
                                                val normY = (offset.y / containerSize.height).coerceIn(0f, 1f)
                                                connectManager.sendRemoteTap(targetDev, normX, normY)
                                            }
                                        }
                                    }
                                    .pointerInput(targetDev) {
                                        detectDragGestures(
                                            onDragStart = { offset -> dragStartOffset = offset },
                                            onDragEnd = { dragStartOffset = null },
                                            onDragCancel = { dragStartOffset = null },
                                            onDrag = { change, dragAmount ->
                                                change.consume()
                                                val start = dragStartOffset
                                                if (start != null && containerSize.width > 0 && containerSize.height > 0) {
                                                    val normX1 = (start.x / containerSize.width).coerceIn(0f, 1f)
                                                    val normY1 = (start.y / containerSize.height).coerceIn(0f, 1f)
                                                    val endX = start.x + dragAmount.x
                                                    val endY = start.y + dragAmount.y
                                                    val normX2 = (endX / containerSize.width).coerceIn(0f, 1f)
                                                    val normY2 = (endY / containerSize.height).coerceIn(0f, 1f)
                                                    connectManager.sendRemoteSwipe(targetDev, normX1, normY1, normX2, normY2, 200)
                                                    dragStartOffset = androidx.compose.ui.geometry.Offset(endX, endY)
                                                }
                                            }
                                        )
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (bitmap != null) {
                                    Image(
                                        bitmap = bitmap.asImageBitmap(),
                                        contentDescription = "Remote Screen",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Fit
                                    )
                                } else {
                                    CircularProgressIndicator(color = CyanNeon)
                                }

                                // Interactive indicator badge
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(8.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color.Black.copy(alpha = 0.7f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "${frame.width}x${frame.height} • #${frame.frameIndex}",
                                        color = Color.LightGray,
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Remote Navigation Soft Bar
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = { connectManager.sendRemoteBack(targetDev) },
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", modifier = Modifier.size(16.dp), tint = Color.White)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Back", fontSize = 12.sp, color = Color.White)
                                    }

                                    Button(
                                        onClick = { connectManager.sendRemoteHome(targetDev) },
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Icon(Icons.Default.Home, contentDescription = "Home", modifier = Modifier.size(16.dp), tint = Color.White)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Home", fontSize = 12.sp, color = Color.White)
                                    }
                                }

                                Button(
                                    onClick = { connectManager.stopScreenStream(targetDev) },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF991B1B)),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Stop", modifier = Modifier.size(16.dp), tint = Color.White)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Stop Stream", fontSize = 12.sp, color = Color.White)
                                }
                            }
                        } else {
                            // Empty / Start Streaming State
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF0F172A))
                                    .border(1.dp, Color(0xFF334155), RoundedCornerShape(12.dp))
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Share,
                                        contentDescription = "Screen Stream",
                                        tint = CyanNeon,
                                        modifier = Modifier.size(36.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Remote View & Touch Controller",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = if (targetDev != null)
                                            "${targetDev.name} पर स्क्रीन शेयरिंग शुरू करने के लिए नीचे टैप करें।"
                                        else
                                            "स्क्रीन देखने के लिए पहले किसी दूसरे फोन को पेयर करें।",
                                        color = TextSecondary,
                                        fontSize = 12.sp,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))

                                    Button(
                                        onClick = {
                                            targetDev?.let { dev ->
                                                isStartingStream = true
                                                streamErrorMessage = null
                                                connectManager.startScreenStream(dev) { success, err ->
                                                    isStartingStream = false
                                                    if (!success) streamErrorMessage = err
                                                }
                                            }
                                        },
                                        enabled = targetDev != null && !isStartingStream,
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = CyanNeon)
                                    ) {
                                        if (isStartingStream) {
                                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.Black)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Connecting Stream...", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        } else {
                                            Icon(Icons.Default.PlayArrow, contentDescription = "Start", tint = Color.Black)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Start Screen Stream", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }

                                    if (streamErrorMessage != null) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(text = streamErrorMessage!!, color = Color.Red, fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 2.6 Secure Two-Way Chunked File Transfer Panel
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = CardDark)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "📁 SECURE FILE TRANSFER (SAF)",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldGlow,
                                    letterSpacing = 1.sp
                                )
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFF0F172A))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text("SHA-256 VERIFIED", color = Color.LightGray, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Storage Access Framework के तहत 64KB एनक्रिप्टेड चंक्स में सुरक्षित फाइल ट्रांसफर:",
                            style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        val targetDev = selectedTargetDevice

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = { filePickerLauncher.launch("*/*") },
                                enabled = targetDev != null,
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669))
                            ) {
                                Icon(Icons.Default.Send, contentDescription = "Send File", tint = Color.White)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Send File to Remote Phone", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }

                        // Transfer progress card
                        if (fileTransferProgress != null) {
                            val prog = fileTransferProgress!!
                            Spacer(modifier = Modifier.height(12.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFF0F172A))
                                    .border(1.dp, if (prog.status == "completed") EmeraldGlow else CyanNeon, RoundedCornerShape(10.dp))
                                    .padding(12.dp)
                            ) {
                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = prog.fileName,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            maxLines = 1
                                        )
                                        Text(
                                            text = when (prog.status) {
                                                "completed" -> "VERIFIED ✓"
                                                "verifying" -> "VERIFYING..."
                                                "failed" -> "FAILED"
                                                else -> "${(prog.bytesTransferred * 100 / (prog.totalBytes.coerceAtLeast(1))).toInt()}%"
                                            },
                                            color = when (prog.status) {
                                                "completed" -> EmeraldGlow
                                                "failed" -> Color.Red
                                                else -> CyanNeon
                                            },
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    val progressFraction = if (prog.totalBytes > 0)
                                        (prog.bytesTransferred.toFloat() / prog.totalBytes.toFloat()).coerceIn(0f, 1f)
                                    else 0f

                                    LinearProgressIndicator(
                                        progress = progressFraction,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(6.dp)
                                            .clip(RoundedCornerShape(3.dp)),
                                        color = if (prog.status == "completed") EmeraldGlow else CyanNeon,
                                        trackColor = Color(0xFF1E293B)
                                    )

                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "${prog.bytesTransferred / 1024} KB / ${prog.totalBytes / 1024} KB",
                                            color = TextSecondary,
                                            fontSize = 10.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                        if (prog.errorMessage != null) {
                                            Text(text = prog.errorMessage, color = Color.Red, fontSize = 10.sp)
                                        } else if (prog.checksumVerified) {
                                            Text(text = "SHA-256 Checksum Matched", color = EmeraldGlow, fontSize = 10.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 3. Quick Remote Action Test Panel
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = CardDark)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "⚡ TEST REMOTE COMMANDS",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = CyanNeon,
                                letterSpacing = 1.sp
                            )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "आवाज दिए बिना सीधे दूसरे फोन पर टेस्ट कमांड भेजें:",
                            style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        val testCommands = listOf(
                            "YouTube खोलो" to "youtube",
                            "Camera खोलो" to "camera",
                            "Chrome खोलो" to "chrome",
                            "Flashlight जलाओ" to "torch",
                            "Battery Check" to "battery",
                            "Volume 80%" to "volume"
                        )

                        // Grid of command buttons
                        for (i in testCommands.indices step 2) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                for (j in 0..1) {
                                    if (i + j < testCommands.size) {
                                        val cmd = testCommands[i + j]
                                        Button(
                                            onClick = {
                                                coroutineScope.launch {
                                                    isExecutingTest = true
                                                    val res = connectManager.executeRemoteVoiceCommand(cmd.first)
                                                    lastTestResult = res
                                                    isExecutingTest = false
                                                }
                                            },
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(10.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                                            enabled = pairedDevices.isNotEmpty() && !isExecutingTest
                                        ) {
                                            Text(cmd.first, fontSize = 12.sp, color = Color.White)
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        // Display result box
                        if (isExecutingTest) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = CyanNeon)
                            }
                        } else if (lastTestResult != null) {
                            val res = lastTestResult!!
                            val (badgeColor, statusTitle) = when (res.status) {
                                RemoteResultStatus.SUCCESS -> EmeraldGlow to "SUCCESS"
                                RemoteResultStatus.APP_NOT_INSTALLED -> Color(0xFFFFB300) to "NOT INSTALLED"
                                RemoteResultStatus.PERMISSION_REQUIRED -> Color.Red to "PERMISSION REQUIRED"
                                RemoteResultStatus.NOT_CONNECTED -> Color.Gray to "NOT CONNECTED"
                                else -> Color.Red to "FAILED"
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFF0F172A))
                                    .border(1.dp, badgeColor.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                                    .padding(12.dp)
                            ) {
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = statusTitle,
                                            color = badgeColor,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = res.message,
                                        color = Color.White,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 4. Companion Phone Permissions Control
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = CardDark)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "🛡️ REMOTE PERMISSIONS ON THIS PHONE",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextSecondary,
                                letterSpacing = 1.sp
                            )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "यदि यह फोन 'Companion / दूसरा फोन' है, तो तय करें कि मुख्य फोन क्या-क्या चला सकता है:",
                            style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        PermissionToggleRow(
                            title = "Open Apps",
                            desc = "YouTube, Chrome, Camera, WhatsApp आदि लॉन्च करने की अनुमति",
                            checked = permissions.openApps,
                            onCheckedChange = {
                                permissions = permissions.copy(openApps = it)
                                connectManager.secureStore.saveDevicePermissions(permissions)
                            }
                        )

                        Divider(color = Color.DarkGray.copy(alpha = 0.4f), modifier = Modifier.padding(vertical = 8.dp))

                        PermissionToggleRow(
                            title = "Media Controls",
                            desc = "YouTube Music, Spotify और संगीत चलाने/रोकने की अनुमति",
                            checked = permissions.mediaControls,
                            onCheckedChange = {
                                permissions = permissions.copy(mediaControls = it)
                                connectManager.secureStore.saveDevicePermissions(permissions)
                            }
                        )

                        Divider(color = Color.DarkGray.copy(alpha = 0.4f), modifier = Modifier.padding(vertical = 8.dp))

                        PermissionToggleRow(
                            title = "Device Commands",
                            desc = "Flashlight, Volume नियंत्रण और बैटरी स्थिति जाँचने की अनुमति",
                            checked = permissions.deviceCommands,
                            onCheckedChange = {
                                permissions = permissions.copy(deviceCommands = it)
                                connectManager.secureStore.saveDevicePermissions(permissions)
                            }
                        )

                        Divider(color = Color.DarkGray.copy(alpha = 0.4f), modifier = Modifier.padding(vertical = 8.dp))

                        PermissionToggleRow(
                            title = "Notifications (Optional)",
                            desc = "रिमोट फोन पर अलर्ट या नोटिफिकेशन भेजना",
                            checked = permissions.notifications,
                            onCheckedChange = {
                                permissions = permissions.copy(notifications = it)
                                connectManager.secureStore.saveDevicePermissions(permissions)
                            }
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }

    // --- PAIR DEVICE DIALOG ---
    if (showPairDialog) {
        var activeTab by remember { mutableIntStateOf(0) } // 0 = Host, 1 = Client Connect
        var hostInfo by remember { mutableStateOf<PairingHostInfo?>(null) }
        var targetIpInput by remember { mutableStateOf("") }
        var targetCodeInput by remember { mutableStateOf("") }
        var pairingErrorMessage by remember { mutableStateOf<String?>(null) }
        var isPairingInProgress by remember { mutableStateOf(false) }

        LaunchedEffect(activeTab) {
            if (activeTab == 0) {
                hostInfo = connectManager.hostPairing()
                connectManager.stopNsdDiscovery()
            } else {
                connectManager.stopHostingPairing()
                connectManager.startNsdDiscovery()
            }
        }

        AlertDialog(
            onDismissRequest = {
                connectManager.stopHostingPairing()
                connectManager.stopNsdDiscovery()
                showPairDialog = false
            },
            title = {
                Text("Pair Phone with MYRA Connect", color = Color.White, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    TabRow(
                        selectedTabIndex = activeTab,
                        containerColor = CardDark,
                        contentColor = CyanNeon
                    ) {
                        Tab(
                            selected = activeTab == 0,
                            onClick = { activeTab = 0 },
                            text = { Text("यह दूसरा फोन है (Host)") }
                        )
                        Tab(
                            selected = activeTab == 1,
                            onClick = { activeTab = 1 },
                            text = { Text("मुख्य फोन से जोड़ें") }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (activeTab == 0) {
                        // Host Mode: Shows 6-digit Code & IP
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "दूसरे फोन पर यह 6-Digit कोड और IP दर्ज करें:",
                                style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary),
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            // Big Code Display
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF0F172A))
                                    .border(1.dp, CyanNeon, RoundedCornerShape(12.dp))
                                    .padding(horizontal = 24.dp, vertical = 12.dp)
                            ) {
                                Text(
                                    text = hostInfo?.pairingCode ?: "------",
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    letterSpacing = 6.sp,
                                    color = CyanNeon
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Local IP: ${hostInfo?.ipAddress ?: localIp} (Port: 9876)",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = Color.LightGray,
                                    fontFamily = FontFamily.Monospace
                                )
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "सुनिश्चित करें कि दोनों फोन एक ही Wi-Fi या Hotspot से जुड़े हैं।",
                                style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary),
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        // Client Mode: Enter IP and 6-digit code
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "दूसरे फोन पर दिख रहा IP और 6-Digit कोड भरें:",
                                style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                            )

                            if (discoveredDevices.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "🔍 हॉटस्पॉट पर मिले फोन (टैप करें):",
                                    style = MaterialTheme.typography.labelSmall.copy(color = CyanNeon, fontWeight = FontWeight.Bold)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    discoveredDevices.forEach { devTriple ->
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(Color(0xFF0F172A))
                                                .border(1.dp, CyanNeon, RoundedCornerShape(8.dp))
                                                .clickable { targetIpInput = devTriple.second }
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text(
                                                text = "${devTriple.first} (${devTriple.second})",
                                                color = Color.White,
                                                fontSize = 11.sp
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            OutlinedTextField(
                                value = targetIpInput,
                                onValueChange = { targetIpInput = it },
                                label = { Text("दूसरे फोन का IP (e.g. 192.168.1.15)") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = targetCodeInput,
                                onValueChange = { if (it.length <= 6) targetCodeInput = it },
                                label = { Text("6-Digit Pairing Code") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            if (pairingErrorMessage != null) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = pairingErrorMessage!!,
                                    color = Color.Red,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                if (activeTab == 1) {
                    Button(
                        onClick = {
                            if (targetIpInput.isNotBlank() && targetCodeInput.length == 6) {
                                isPairingInProgress = true
                                pairingErrorMessage = null
                                connectManager.pairWithRemoteDevice(
                                    targetIp = targetIpInput.trim(),
                                    port = 9876,
                                    sixDigitCode = targetCodeInput.trim()
                                ) { success, error ->
                                    isPairingInProgress = false
                                    if (success) {
                                        showPairDialog = false
                                    } else {
                                        pairingErrorMessage = error ?: "Pairing failed. Re-check IP & Code."
                                    }
                                }
                            } else {
                                pairingErrorMessage = "कृपया सही IP और 6-Digit कोड भरें।"
                            }
                        },
                        enabled = !isPairingInProgress,
                        colors = ButtonDefaults.buttonColors(containerColor = CyanNeon)
                    ) {
                        if (isPairingInProgress) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.Black)
                        } else {
                            Text("Pair Now", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    Button(
                        onClick = {
                            connectManager.stopHostingPairing()
                            showPairDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CyanNeon)
                    ) {
                        Text("Done", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        connectManager.stopHostingPairing()
                        showPairDialog = false
                    }
                ) {
                    Text("Cancel", color = Color.LightGray)
                }
            },
            containerColor = CardDark
        )
    }

    // --- RENAME DEVICE DIALOG ---
    if (renameTargetDevice != null) {
        val target = renameTargetDevice!!
        AlertDialog(
            onDismissRequest = { renameTargetDevice = null },
            title = { Text("Rename Device", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = renameNewName,
                    onValueChange = { renameNewName = it },
                    label = { Text("Device Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (renameNewName.isNotBlank()) {
                            connectManager.renameDevice(target.id, renameNewName.trim())
                        }
                        renameTargetDevice = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyanNeon)
                ) {
                    Text("Save", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { renameTargetDevice = null }) {
                    Text("Cancel", color = Color.LightGray)
                }
            },
            containerColor = CardDark
        )
    }
}

@Composable
private fun PermissionToggleRow(
    title: String,
    desc: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = desc, color = TextSecondary, fontSize = 12.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.Black,
                checkedTrackColor = CyanNeon,
                uncheckedThumbColor = Color.Gray,
                uncheckedTrackColor = Color.DarkGray
            )
        )
    }
}
