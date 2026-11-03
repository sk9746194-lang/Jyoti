package com.myra.assistant.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.myra.assistant.ui.theme.*
import com.myra.assistant.vision.CameraVisionManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraVisionScreen(
    onNavigateBack: () -> Unit,
    onSpeakResult: (String) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val visionManager = remember { CameraVisionManager(context) }

    var isInspecting by remember { mutableStateOf(false) }
    var inspectionResult by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var customPrompt by remember { mutableStateOf("Describe this scene, identify objects, and read any visible text.") }

    DisposableEffect(Unit) {
        onDispose {
            visionManager.releaseCamera()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "MYRA Vision AI",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Live Scene Analysis & Multimodal Understanding",
                            color = CyanNeon,
                            fontSize = 11.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundDark)
            )
        },
        containerColor = BackgroundDark
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Viewfinder Mock / Frame Target Area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFF0F172A), Color(0xFF020617))
                        )
                    )
                    .border(
                        BorderStroke(
                            2.dp,
                            Brush.linearGradient(listOf(CyanNeon, PurpleNeon))
                        ),
                        RoundedCornerShape(20.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                // Cyberpunk Corner Crosshairs
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Default.CameraAlt,
                        contentDescription = null,
                        tint = CyanNeon,
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = if (isInspecting) "Analyzing scene with Gemini Vision..." else "Camera Ready",
                        color = if (isInspecting) CyanNeon else Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "High-definition frame capture • Privacy guaranteed",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }

                if (isInspecting) {
                    CircularProgressIndicator(
                        color = CyanNeon,
                        strokeWidth = 3.dp,
                        modifier = Modifier.size(100.dp)
                    )
                }
            }

            // Prompt Input
            OutlinedTextField(
                value = customPrompt,
                onValueChange = { customPrompt = it },
                label = { Text("What should MYRA look for?") },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = CyanNeon,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            // Inspect Button
            Button(
                onClick = {
                    if (!visionManager.hasCameraPermission()) {
                        errorMessage = "Camera permission is required. Please grant it in system settings."
                        return@Button
                    }
                    isInspecting = true
                    errorMessage = null
                    inspectionResult = null

                    coroutineScope.launch {
                        val res = visionManager.inspectSceneWithGemini(customPrompt)
                        isInspecting = false
                        res.onSuccess { text ->
                            inspectionResult = text
                            onSpeakResult(text)
                        }.onFailure { err ->
                            errorMessage = err.message ?: "Failed to inspect scene."
                        }
                    }
                },
                enabled = !isInspecting,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Icon(Icons.Default.Visibility, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isInspecting) "Analyzing with Gemini..." else "Capture & Inspect Scene",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Results Section
            if (errorMessage != null) {
                Surface(
                    color = AccentRed.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, AccentRed.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Error, contentDescription = null, tint = AccentRed)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(text = errorMessage!!, color = Color.White, fontSize = 13.sp)
                    }
                }
            }

            if (inspectionResult != null) {
                Surface(
                    color = CardDark,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, CyanNeon.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Gemini Vision Analysis",
                                color = CyanNeon,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            IconButton(onClick = { onSpeakResult(inspectionResult!!) }) {
                                Icon(Icons.Default.VolumeUp, contentDescription = "Speak", tint = Color.White)
                            }
                        }
                        Divider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 8.dp))
                        Text(
                            text = inspectionResult!!,
                            color = Color.White,
                            fontSize = 14.sp,
                            lineHeight = 20.sp
                        )
                    }
                }
            }
        }
    }
}
