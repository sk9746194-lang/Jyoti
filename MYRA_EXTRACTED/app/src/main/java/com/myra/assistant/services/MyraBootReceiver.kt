package com.myra.assistant.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.myra.assistant.data.PreferencesManager

/**
 * Boot & Package Update Receiver for MYRA Assistant.
 * Restarts Hands-Free "Hey MYRA" service after device reboot or app upgrade if enabled.
 */
class MyraBootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        Log.d("MYRA_BootReceiver", "Received action: $action")

        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_MY_PACKAGE_REPLACED ||
            action == "android.intent.action.QUICKBOOT_POWERON"
        ) {
            val prefs = PreferencesManager(context)
            if (prefs.isHandsFreeWakeWordEnabled) {
                // Android 14+ restricts starting a microphone foreground service from
                // BOOT_COMPLETED. The user must open MYRA once after reboot; MainActivity
                // starts the listener after RECORD_AUDIO permission is confirmed.
                Log.i(
                    "MYRA_BootReceiver",
                    "Hands-free is enabled; waiting for the user to open MYRA before starting microphone capture."
                )
            }
        }
    }
}
