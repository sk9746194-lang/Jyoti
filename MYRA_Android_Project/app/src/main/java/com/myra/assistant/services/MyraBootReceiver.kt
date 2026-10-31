package com.myra.assistant.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.myra.assistant.data.PreferencesManager

/**
 * Android restricts starting microphone foreground services from boot broadcasts.
 * Keep the user's hands-free preference, but require opening MYRA before listening resumes.
 */
class MyraBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_MY_PACKAGE_REPLACED ||
            action == "android.intent.action.QUICKBOOT_POWERON") {
            val enabled = PreferencesManager(context).isHandsFreeWakeWordEnabled
            if (enabled) {
                Log.i("MYRA_BootReceiver", "Hands-free is enabled; open MYRA to resume microphone listening safely.")
            }
        }
    }
}
