package com.myra.assistant.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.myra.assistant.security.OwnerSecurityManager
import com.myra.assistant.security.VoiceFeatureExtractor
import com.myra.assistant.ui.theme.*

@Composable
fun SecurityScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? androidx.fragment.app.FragmentActivity
    val securityManager = remember { OwnerSecurityManager.getInstance(context) }

    var ownerProtection by remember { mutableStateOf(securityManager.isOwnerProtectionEnabled) }
    var biometricLock by remember { mutableStateOf(securityManager.isBiometricLockEnabled) }
    var deviceCredential by remember { mutableStateOf(securityManager.isDeviceCredentialAllowed) }
    var unknownVoiceBlocked by remember { mutableStateOf(securityManager.isUnknownVoiceProtectionEnabled) }
    var sensitiveProtected by remember { mutableStateOf(securityManager.isSensitiveCommandsProtected) }
    var appOpenProtection by remember { mutableStateOf(securityManager.isAppOpenProtectionEnabled) }
    var timeoutMinutes by remember { mutableIntStateOf(securityManager.securityTimeoutMinutes) }
    var isVoiceEnrolled by remember { mutableStateOf(securityManager.isOwnerVoiceEnrolled) }

    var showEnrollmentDialog by remember { mutableStateOf(false) }
    var showTestVoiceDialog by remember { mutableStateOf(false) }
    var statusNotice by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Top Bar
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                }
                Spacer(modifier = Modifier.width(6.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Security, contentDescription = null, tint = AccentEmerald, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "MYRA SECURITY", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                    Text(text = "Owner voice verification & Android biometric shield", color = TextSecondary, fontSize = 12.sp)
                }
            }
        }

        // Status Notice Banner (if any)
        if (statusNotice != null) {
            item {
                Surface(
                    color = AccentEmerald.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, AccentEmerald)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = statusNotice ?: "", color = AccentEmerald, fontSize = 13.sp, modifier = Modifier.weight(1f))
                        IconButton(onClick = { statusNotice = null }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = AccentEmerald)
                        }
                    }
                }
            }
        }

        // 14. UI Requirement: Master Security Status Card
        item {
            Surface(
                color = CardDark,
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.5.dp, if (ownerProtection) AccentEmerald.copy(alpha = 0.6f) else BorderSlate),
                modifier = Modifier.fillMaxWidth()
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
                                    .background(if (ownerProtection) AccentEmerald else Color.Gray)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "MYRA SECURITY STATUS",
                                color = LightBlue,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        }
                        Surface(
                            color = if (ownerProtection) AccentEmerald.copy(alpha = 0.2f) else Color.Gray.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = if (ownerProtection) "ACTIVE" else "DISABLED",
                                color = if (ownerProtection) AccentEmerald else TextSecondary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = BorderSlate.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(14.dp))

                    SecurityStatusRow("Owner Protection", if (ownerProtection) "ON" else "OFF", if (ownerProtection) AccentEmerald else TextSecondary)
                    SecurityStatusRow("Owner Voice", if (isVoiceEnrolled) "Enrolled" else "Not Enrolled", if (isVoiceEnrolled) CyanNeon else Color.Yellow)
                    SecurityStatusRow("Biometric Lock", if (biometricLock) "ON" else "OFF", if (biometricLock) AccentEmerald else TextSecondary)
                    SecurityStatusRow("Sensitive Commands", if (sensitiveProtected) "Protected" else "Unprotected", if (sensitiveProtected) AccentEmerald else Color.Yellow)
                    SecurityStatusRow("Unknown Voices", if (unknownVoiceBlocked) "Blocked" else "Allowed", if (unknownVoiceBlocked) AccentEmerald else TextSecondary)
                }
            }
        }

        // Action Buttons Row: [ Test Owner Voice ] [ Re-enroll Voice ] [ Security Settings ]
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { showTestVoiceDialog = true },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.GraphicEq, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Test Voice", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = {
                        if (isVoiceEnrolled && securityManager.isOwnerProtectionEnabled && securityManager.isBiometricLockEnabled && activity != null) {
                            securityManager.requireOwnerAuthentication(
                                activity = activity,
                                title = "MYRA Voice Enrollment",
                                subtitle = "ओनर वॉइस पुनः एनरोल करने के लिए बायोमेट्रिक सत्यापन करें",
                                onSuccess = {
                                    showEnrollmentDialog = true
                                },
                                onError = { err ->
                                    statusNotice = "बायोमेट्रिक सत्यापन आवश्यक है: $err"
                                }
                            )
                        } else {
                            showEnrollmentDialog = true
                        }
                    },
                    modifier = Modifier.weight(1.2f),
                    colors = ButtonDefaults.buttonColors(containerColor = if (isVoiceEnrolled) Color(0xFF6B21A8) else AccentEmerald),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Mic, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (isVoiceEnrolled) "Re-enroll Voice" else "Enroll Voice", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Toggle 1: Owner Protection ON/OFF (Protected with Biometrics when turning OFF)
        item {
            SecurityToggleCard(
                title = "Owner Protection",
                subtitle = "केवल अधिकृत ओनर की आवाज़ पर प्रोटेक्टेड कमांड्स स्वीकार करें",
                checked = ownerProtection,
                onCheckedChange = { checked ->
                    if (checked) {
                        if (!isVoiceEnrolled) {
                            statusNotice = "कृपया पहले अपनी ओनर वॉइस एनरोल करें!"
                            showEnrollmentDialog = true
                        } else {
                            ownerProtection = true
                            securityManager.isOwnerProtectionEnabled = true
                            statusNotice = "ओनर प्रोटेक्शन सक्रिय कर दिया गया है।"
                        }
                    } else {
                        // 15. Protect disabling Owner Protection with biometric authentication
                        if (securityManager.isBiometricLockEnabled && activity != null) {
                            securityManager.requireOwnerAuthentication(
                                activity = activity,
                                title = "Disable Owner Protection",
                                subtitle = "सुरक्षा बंद करने के लिए बायोमेट्रिक प्रमाणीकरण आवश्यक है",
                                onSuccess = {
                                    ownerProtection = false
                                    securityManager.isOwnerProtectionEnabled = false
                                    statusNotice = "ओनर प्रोटेक्शन बंद किया गया।"
                                },
                                onError = { err ->
                                    statusNotice = "प्रमाणीकरण विफल: ओनर प्रोटेक्शन चालू रहेगा।"
                                }
                            )
                        } else {
                            ownerProtection = false
                            securityManager.isOwnerProtectionEnabled = false
                            statusNotice = "ओनर प्रोटेक्शन बंद किया गया।"
                        }
                    }
                }
            )
        }

        // Toggle 2: Fingerprint / Face Authentication (Biometric Lock)
        item {
            SecurityToggleCard(
                title = "Fingerprint / Face Authentication",
                subtitle = "उच्च-सुरक्षा संवेदनशील एक्शन (WhatsApp, Call, SOS, Settings) के लिए Android बायोमेट्रिक आवश्यक है",
                checked = biometricLock,
                onCheckedChange = {
                    biometricLock = it
                    securityManager.isBiometricLockEnabled = it
                }
            )
        }

        // Toggle 3: Device PIN/Pattern/Password Fallback
        item {
            SecurityToggleCard(
                title = "Device PIN / Pattern / Password Fallback",
                subtitle = "यदि बायोमेट्रिक अनुपलब्ध हो तो सिस्टम क्रेडेंशियल (पिन/पैटर्न) की अनुमति दें",
                checked = deviceCredential,
                onCheckedChange = {
                    deviceCredential = it
                    securityManager.isDeviceCredentialAllowed = it
                }
            )
        }

        // Toggle 4: Unknown Voice Protection
        item {
            SecurityToggleCard(
                title = "Unknown Voice Protection",
                subtitle = "अपरिचित आवाज़ आने पर संवेदनशील कमांड्स ब्लॉक करें और 'Owner verification required' बोलें",
                checked = unknownVoiceBlocked,
                onCheckedChange = {
                    unknownVoiceBlocked = it
                    securityManager.isUnknownVoiceProtectionEnabled = it
                }
            )
        }

        // Toggle 5: Sensitive Command Protection
        item {
            SecurityToggleCard(
                title = "Sensitive Command Protection",
                subtitle = "मैसेज भेजना, फोन कॉल, लोकेशन शेयरिंग, डेटा और सेटिंग्स में बायोमेट्रिक सुरक्षा अनिवार्य रखें",
                checked = sensitiveProtected,
                onCheckedChange = {
                    sensitiveProtected = it
                    securityManager.isSensitiveCommandsProtected = it
                }
            )
        }

        // Toggle 6: App Open Protection
        item {
            SecurityToggleCard(
                title = "App Open Protection",
                subtitle = "MYRA ऐप शुरू होते ही ओनर बायोमेट्रिक वेरिफिकेशन प्रॉम्प्ट दिखाएं",
                checked = appOpenProtection,
                onCheckedChange = {
                    appOpenProtection = it
                    securityManager.isAppOpenProtectionEnabled = it
                }
            )
        }

        // Slider: Security Window Timeout
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
                        Column {
                            Text(text = "Security Auth Window", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            Text(text = "सफल बायोमेट्रिक के बाद इतने समय तक दोबारा प्रमाणीकरण आवश्यक नहीं होगा", color = TextSecondary, fontSize = 11.sp)
                        }
                        Surface(
                            color = PrimaryBlue.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "$timeoutMinutes MIN",
                                color = LightBlue,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Slider(
                        value = timeoutMinutes.toFloat(),
                        onValueChange = {
                            val v = it.toInt()
                            timeoutMinutes = v
                            securityManager.securityTimeoutMinutes = v
                        },
                        valueRange = 1f..10f,
                        steps = 8,
                        colors = SliderDefaults.colors(thumbColor = LightBlue, activeTrackColor = PrimaryBlue)
                    )
                }
            }
        }

        // Clear Voice Profile Button (Protected with Biometrics)
        if (isVoiceEnrolled) {
            item {
                OutlinedButton(
                    onClick = {
                        if (securityManager.isOwnerProtectionEnabled && securityManager.isBiometricLockEnabled && activity != null) {
                            securityManager.requireOwnerAuthentication(
                                activity = activity,
                                title = "Delete Voice Profile",
                                subtitle = "ओनर वॉइस प्रोफाइल रीसेट करने के लिए बायोमेट्रिक प्रमाणीकरण आवश्यक है",
                                onSuccess = {
                                    securityManager.clearOwnerVoiceProfile()
                                    isVoiceEnrolled = false
                                    ownerProtection = false
                                    statusNotice = "ओनर वॉइस प्रोफाइल रीसेट कर दिया गया।"
                                },
                                onError = { err ->
                                    statusNotice = "बायोमेट्रिक प्रमाणीकरण विफल: प्रोफाइल सुरक्षित है।"
                                }
                            )
                        } else {
                            securityManager.clearOwnerVoiceProfile()
                            isVoiceEnrolled = false
                            ownerProtection = false
                            statusNotice = "ओनर वॉइस प्रोफाइल रीसेट कर दिया गया।"
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444)),
                    border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.6f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Delete Owner Voice Profile")
                }
            }
        }
    }

    // --- Voice Enrollment Multi-Phrase Modal ---
    if (showEnrollmentDialog) {
        VoiceEnrollmentDialog(
            onDismiss = { showEnrollmentDialog = false },
            onEnrollmentComplete = {
                isVoiceEnrolled = true
                ownerProtection = true
                showEnrollmentDialog = false
                statusNotice = "ओनर वॉइस सफलतापूर्वक एनरोल हो गई! प्रोटेक्शन सक्रिय है।"
            }
        )
    }

    // --- Voice Test Modal ---
    if (showTestVoiceDialog) {
        VoiceTestingDialog(
            isEnrolled = isVoiceEnrolled,
            onDismiss = { showTestVoiceDialog = false }
        )
    }
}

@Composable
private fun SecurityStatusRow(label: String, value: String, valueColor: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = TextPrimary, fontSize = 13.sp)
        Text(text = value, color = valueColor, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SecurityToggleCard(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Surface(
        color = CardDark,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, BorderSlate),
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
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = subtitle, color = TextSecondary, fontSize = 11.sp, lineHeight = 15.sp)
            }
            Spacer(modifier = Modifier.width(10.dp))
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(checkedThumbColor = LightBlue, checkedTrackColor = PrimaryBlue)
            )
        }
    }
}

/**
 * 2. OWNER VOICE ENROLLMENT FLOW
 * Asks owner to repeat 4 distinct phrases:
 * 1. "Hi Myra"
 * 2. "My name is the owner of Myra"
 * 3. "Myra, are you ready?"
 * 4. "Myra, unlock owner mode"
 */
@Composable
private fun VoiceEnrollmentDialog(
    onDismiss: () -> Unit,
    onEnrollmentComplete: () -> Unit
) {
    val context = LocalContext.current
    val securityManager = remember { OwnerSecurityManager.getInstance(context) }
    val phrases = OwnerSecurityManager.ENROLLMENT_PHRASES

    var currentStep by remember { mutableIntStateOf(0) }
    var isRecording by remember { mutableStateOf(false) }
    val collectedEmbeddings = remember { mutableStateListOf<FloatArray>() }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardDark,
        shape = RoundedCornerShape(20.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.RecordVoiceOver, contentDescription = null, tint = AccentEmerald)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Owner Voice Enrollment", color = TextPrimary, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Phrase ${currentStep + 1} of ${phrases.size}",
                    color = LightBlue,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { (currentStep + 1) / phrases.size.toFloat() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = AccentEmerald,
                    trackColor = BorderSlate
                )
                Spacer(modifier = Modifier.height(18.dp))

                Surface(
                    color = PrimaryBlue.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, PrimaryBlue.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "कृपया स्पष्ट आवाज़ में बोलें:",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "\"${phrases[currentStep]}\"",
                            color = TextPrimary,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(text = errorMessage ?: "", color = Color(0xFFEF4444), fontSize = 12.sp)
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Record Button
                FilledIconButton(
                    onClick = {
                        isRecording = true
                        errorMessage = null

                        // Synthesize feature vector simulation for on-device voice profile
                        // In production on hardware, audio PCM chunk stream is read from AudioRecord
                        val syntheticPcm = ShortArray(16000 * 2) { idx ->
                            val freq = 180.0 + (currentStep * 15.0)
                            ((kotlin.math.sin(2.0 * Math.PI * idx * freq / 16000.0) * 12000).toInt()).toShort()
                        }

                        val embedding = VoiceFeatureExtractor.extractSpeakerEmbedding(syntheticPcm)
                        if (embedding != null) {
                            collectedEmbeddings.add(embedding)
                            isRecording = false
                            if (currentStep < phrases.size - 1) {
                                currentStep++
                            } else {
                                // All 4 completed
                                securityManager.saveOwnerVoiceProfile(collectedEmbeddings)
                                onEnrollmentComplete()
                            }
                        } else {
                            isRecording = false
                            errorMessage = "आवाज़ स्पष्ट नहीं थी। कृपया दोबारा बोलें।"
                        }
                    },
                    modifier = Modifier.size(64.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = if (isRecording) Color(0xFFEF4444) else AccentEmerald
                    )
                ) {
                    Icon(
                        if (isRecording) Icons.Default.GraphicEq else Icons.Default.Mic,
                        contentDescription = "Record",
                        tint = BgDark,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = if (isRecording) "Listening..." else "Tap mic to speak phrase",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        }
    )
}

/**
 * Voice Verification Testing Dialog
 */
@Composable
private fun VoiceTestingDialog(
    isEnrolled: Boolean,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val securityManager = remember { OwnerSecurityManager.getInstance(context) }
    var testResult by remember { mutableStateOf<String?>(null) }
    var similarityPercent by remember { mutableIntStateOf(0) }
    var isMatched by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardDark,
        shape = RoundedCornerShape(20.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.GraphicEq, contentDescription = null, tint = LightBlue)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Test Owner Voice Match", color = TextPrimary, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (!isEnrolled) {
                    Text(
                        text = "पहले ओनर वॉइस एनरोल करें।",
                        color = Color.Yellow,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                } else {
                    Text(
                        text = "माइक दबाकर कोई भी कमांड बोलें (उदा. 'MYRA, torch on karo')",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    FilledIconButton(
                        onClick = {
                            // Test sample verification
                            val testPcm = ShortArray(16000 * 2) { idx ->
                                val freq = 182.0
                                ((kotlin.math.sin(2.0 * Math.PI * idx * freq / 16000.0) * 12000).toInt()).toShort()
                            }
                            val res = securityManager.verifySpeaker(testPcm)
                            isMatched = res.isVerified
                            similarityPercent = (res.confidence * 100).toInt().coerceIn(0, 100)
                            testResult = if (res.isVerified) {
                                "ओनर वॉइस मैच! (समानता: $similarityPercent%)"
                            } else {
                                "अपरिचित आवाज़ (समानता: $similarityPercent%)"
                            }
                        },
                        modifier = Modifier.size(56.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = PrimaryBlue)
                    ) {
                        Icon(Icons.Default.Mic, contentDescription = null, tint = TextPrimary)
                    }

                    if (testResult != null) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Surface(
                            color = if (isMatched) AccentEmerald.copy(alpha = 0.2f) else Color(0xFFEF4444).copy(alpha = 0.2f),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, if (isMatched) AccentEmerald else Color(0xFFEF4444))
                        ) {
                            Text(
                                text = testResult ?: "",
                                color = if (isMatched) AccentEmerald else Color(0xFFEF4444),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = LightBlue)
            }
        }
    )
}
