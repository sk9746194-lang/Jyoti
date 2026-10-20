package com.myra.assistant.services

import android.app.Notification
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.myra.assistant.MyraApplication

class MyraWakeWordService : Service() {

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification: Notification = NotificationCompat.Builder(this, MyraApplication.CHANNEL_ID_VOICE_SERVICE)
            .setContentTitle("MYRA AI Assistant Active")
            .setContentText("Listening for wake phrase...")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .build()

        startForeground(101, notification)
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
