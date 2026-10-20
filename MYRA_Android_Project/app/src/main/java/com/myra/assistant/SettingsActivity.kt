package com.myra.assistant

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.myra.assistant.ui.screens.SettingsScreen
import com.myra.assistant.ui.theme.DarkBackground
import com.myra.assistant.ui.theme.MyraAssistantTheme

/**
 * Dedicated Standalone Settings Activity for MYRA Assistant.
 * Allows deep linking and direct Android system settings integration.
 */
class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyraAssistantTheme {
                Surface(
                    modifier = Modifier.fillMaxSize().background(DarkBackground),
                    color = DarkBackground
                ) {
                    SettingsScreen(onBack = { finish() })
                }
            }
        }
    }
}
