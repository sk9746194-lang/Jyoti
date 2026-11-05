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
                Log.i("MYRA_BootReceiver", "Hands-free enabled; starting MyraWakeWordService after boot/update")
                MyraWakeWordService.start(context)
            }
        }
    }
}
