package com.myra.assistant.services

import android.content.Intent
import android.os.Bundle
import android.service.voice.VoiceInteractionService
import android.service.voice.VoiceInteractionSession
import android.util.Log

/**
 * Android VoiceInteractionService implementation for default digital assistant.
 * Enables long-press power button / home button assist invocation on Android.
 */
class MyraVoiceInteractionService : VoiceInteractionService() {

    override fun onReady() {
        super.onReady()
        Log.d("MYRA_ASSIST", "MyraVoiceInteractionService is ready and bound as default digital assistant")
    }

    override fun onLaunchVoiceAssistFromKeyguard() {
        super.onLaunchVoiceAssistFromKeyguard()
        Log.d("MYRA_ASSIST", "Assist invoked from keyguard")
        val args = Bundle().apply {
            putBoolean("from_keyguard", true)
        }
        showSession(args, VoiceInteractionSession.SHOW_SOURCE_ASSIST_GESTURE)
    }

    override fun onShutdown() {
        super.onShutdown()
        Log.d("MYRA_ASSIST", "MyraVoiceInteractionService shut down")
    }
}

