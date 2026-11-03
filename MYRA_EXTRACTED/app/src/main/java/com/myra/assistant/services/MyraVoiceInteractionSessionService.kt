package com.myra.assistant.services

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.service.voice.VoiceInteractionSession
import android.service.voice.VoiceInteractionSessionService
import com.myra.assistant.MainActivity

/**
 * Android VoiceInteractionSessionService providing interactive voice assist sessions.
 */
class MyraVoiceInteractionSessionService : VoiceInteractionSessionService() {
    override fun onNewSession(args: Bundle?): VoiceInteractionSession {
        return MyraVoiceInteractionSession(this)
    }
}

class MyraVoiceInteractionSession(context: Context) : VoiceInteractionSession(context) {
    override fun onShow(args: Bundle?, flags: Int) {
        super.onShow(args, flags)
        val intent = Intent(context, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            putExtra("voice_assist_triggered", true)
        }
        context.startActivity(intent)
    }
}
