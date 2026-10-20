package com.myra.assistant

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build

class MyraApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID_VOICE_SERVICE,
                "MYRA Voice Assistant Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Runs background wake word detection and speech processing"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_ID_VOICE_SERVICE = "myra_voice_service_channel"
    }
}
