package com.myra.assistant.wallpaper

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.myra.assistant.ui.screens.MyraWallpaperSettingsScreen
import com.myra.assistant.ui.theme.MyraAssistantTheme

/**
 * Dedicated settings activity launched directly from Android's system
 * Wallpaper Chooser / Live Wallpaper preview screen or from MYRA Settings.
 */
class MyraWallpaperSettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyraAssistantTheme {
                MyraWallpaperSettingsScreen(onBack = { finish() })
            }
        }
    }
}
