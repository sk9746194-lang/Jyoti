package com.myra.assistant.ui

import android.app.KeyguardManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.myra.assistant.data.AssistantState
import com.myra.assistant.ui.components.AnimatedOrb
import com.myra.assistant.ui.theme.DarkBackground
import com.myra.assistant.ui.theme.MyraAssistantTheme

/**
 * Overlay Activity configured to show over Android Lock Screen
 * when awakened by voice command or incoming urgency.
 */
class LockScreenOverlayActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val km = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
            km.requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }

        setContent {
            MyraAssistantTheme {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xEE090D16)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "MYRA Assistant Locked Screen",
                            color = Color(0xFF60A5FA),
                            fontSize = 18.sp
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        AnimatedOrb(
                            state = AssistantState.LISTENING,
                            audioLevel = 0.5f,
                            modifier = Modifier.size(180.dp)
                        )
                    }
                }
            }
        }
    }
}
