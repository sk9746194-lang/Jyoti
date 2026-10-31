package com.myra.assistant.services

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.content.pm.PackageManager
import android.Manifest
import android.os.*
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import androidx.core.app.NotificationCompat
import com.myra.assistant.MainActivity
import com.myra.assistant.MyraApplication
import com.myra.assistant.R
import com.myra.assistant.data.PreferencesManager
import java.util.Locale

/**
 * Experimental hands-free wake-word service backed by Android SpeechRecognizer.
 * Requires explicit microphone permission and a visible foreground notification.
 * Background activity launch and lock-screen behavior are subject to Android restrictions.
 */
class MyraWakeWordService : Service() {

    private val TAG = "MYRA_WakeWordService"
    private val NOTIFICATION_ID = 2026

    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false
    private var isDestroyed = false
    private var wakeLock: PowerManager.WakeLock? = null
    private val handler = Handler(Looper.getMainLooper())

    private val restartRunnable = Runnable {
        if (!isDestroyed) {
            startWakeWordListening()
        }
    }

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        acquireCpuWakeLock()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP_SERVICE) {
            stopSelf()
            return START_NOT_STICKY
        }

        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "Microphone permission is missing; wake-word service will not start.")
            stopSelf()
            return START_NOT_STICKY
        }

        val openIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val stopIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, MyraWakeWordService::class.java).apply {
                action = ACTION_STOP_SERVICE
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification: Notification = NotificationCompat.Builder(this, MyraApplication.CHANNEL_ID_VOICE_SERVICE)
            .setContentTitle("MYRA Hands-Free Voice Active")
            .setContentText("Listening for 'Hey MYRA' / 'मायरा' / Voice Commands...")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setOngoing(true)
            .setContentIntent(openIntent)
            .addAction(android.R.drawable.ic_media_play, "Open MYRA", openIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop Hands-Free", stopIntent)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        initAndStartListening()
        return START_STICKY
    }

    private fun acquireCpuWakeLock() {
        try {
            val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "myra:wakeword_cpu_lock").apply {
                setReferenceCounted(false)
                acquire(24 * 60 * 60 * 1000L) // 24 hours max
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to acquire partial wake lock", e)
        }
    }

    private fun initAndStartListening() {
        handler.post {
            try {
                if (SpeechRecognizer.isRecognitionAvailable(this)) {
                    speechRecognizer?.destroy()
                    speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this).apply {
                        setRecognitionListener(createListener())
                    }
                    startWakeWordListening()
                } else {
                    Log.e(TAG, "Speech recognition unavailable on this device")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error initializing speech recognizer for wake word", e)
                scheduleRestart(2000)
            }
        }
    }

    private fun startWakeWordListening() {
        if (isDestroyed) return
        val prefs = PreferencesManager(this)
        if (!prefs.isHandsFreeWakeWordEnabled) {
            Log.i(TAG, "Hands-free wake word is disabled in preferences; pausing listener")
            return
        }

        try {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "hi-IN")
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, packageName)
            }

            speechRecognizer?.startListening(intent)
            isListening = true
            Log.d(TAG, "Wake word listening started successfully")
        } catch (e: Exception) {
            Log.w(TAG, "startListening failed: ${e.message}")
            scheduleRestart(1000)
        }
    }

    private fun scheduleRestart(delayMs: Long) {
        handler.removeCallbacks(restartRunnable)
        if (!isDestroyed) {
            handler.postDelayed(restartRunnable, delayMs)
        }
    }

    private fun createListener() = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {}
        override fun onBeginningOfSpeech() {}
        override fun onRmsChanged(rmsdB: Float) {}
        override fun onBufferReceived(buffer: ByteArray?) {}
        override fun onEndOfSpeech() {}

        override fun onError(error: Int) {
            isListening = false
            // SpeechRecognizer errors like NO_MATCH or SPEECH_TIMEOUT are normal during idle listening
            val delay = when (error) {
                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> 800L
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> 5000L
                else -> 400L
            }
            scheduleRestart(delay)
        }

        override fun onResults(results: Bundle?) {
            isListening = false
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            if (!matches.isNullOrEmpty()) {
                val detectedText = matches[0]
                Log.d(TAG, "Speech heard: '$detectedText'")
                if (checkWakeWordTrigger(detectedText)) {
                    handleWakeWordTriggered(detectedText)
                    return
                }
            }
            scheduleRestart(250)
        }

        override fun onPartialResults(partialResults: Bundle?) {
            val partialMatches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            if (!partialMatches.isNullOrEmpty()) {
                val partialText = partialMatches[0]
                if (checkWakeWordTrigger(partialText)) {
                    speechRecognizer?.stopListening()
                    isListening = false
                    handleWakeWordTriggered(partialText)
                }
            }
        }

        override fun onEvent(eventType: Int, params: Bundle?) {}
    }

    private fun checkWakeWordTrigger(text: String): Boolean {
        val lower = text.lowercase(Locale.ROOT).trim()
        val triggers = listOf(
            "hey myra", "hi myra", "hello myra", "ok myra", "okay myra",
            "मायरा सुनो", "हे मायरा", "सुनो मायरा", "नमस्ते मायरा", "हेलो मायरा", "हाय मायरा",
            "suno myra", "namaste myra"
        )
        return triggers.any { lower.contains(it) }
    }

    private fun handleWakeWordTriggered(rawSpeech: String) {
        Log.i(TAG, "HOTWORD DETECTED! Waking screen and launching MYRA...")

        // 1. Vibrate feedback
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vm.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 70, 40, 90), -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(longArrayOf(0, 70, 40, 90), -1)
            }
        } catch (_: Exception) {}

        // 2. Turn screen on and wake up device (even from lock screen / screen off)
        try {
            val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
            @Suppress("DEPRECATION")
            val screenWakeLock = pm.newWakeLock(
                PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP or PowerManager.ON_AFTER_RELEASE,
                "myra:wake_screen_lock"
            )
            screenWakeLock.acquire(10000) // 10 seconds
        } catch (e: Exception) {
            Log.w(TAG, "Could not turn screen on with wake lock", e)
        }

        // 3. Extract any follow-up command spoken in the same breath (e.g., "Hey MYRA torch on karo" -> "torch on karo")
        var followUpCommand = ""
        val triggers = listOf(
            "hey myra", "hi myra", "hello myra", "ok myra", "okay myra",
            "मायरा सुनो", "हे मायरा", "सुनो मायरा", "नमस्ते मायरा", "हेलो मायरा", "हाय मायरा",
            "suno myra", "namaste myra"
        )
        val lower = rawSpeech.lowercase(Locale.ROOT)
        for (trig in triggers) {
            val idx = lower.indexOf(trig)
            if (idx != -1) {
                followUpCommand = rawSpeech.substring(idx + trig.length).trim(*" ,.!?".toCharArray())
                break
            }
        }

        // 4. Launch MainActivity with lockscreen bypass & voice auto-start flags
        val launchIntent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            putExtra(EXTRA_AUTO_START_VOICE, true)
            putExtra(EXTRA_TRIGGER_SOURCE, "wake_word")
            if (followUpCommand.isNotBlank()) {
                putExtra(EXTRA_FOLLOW_UP_COMMAND, followUpCommand)
            }
        }

        try {
            startActivity(launchIntent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start MainActivity from wake word service", e)
        }

        // Restart listener after a delay to allow the active session to complete
        scheduleRestart(6000)
    }

    override fun onDestroy() {
        super.onDestroy()
        isDestroyed = true
        isRunning = false
        handler.removeCallbacksAndMessages(null)
        try {
            speechRecognizer?.destroy()
            speechRecognizer = null
        } catch (_: Exception) {}

        try {
            wakeLock?.let {
                if (it.isHeld) it.release()
            }
        } catch (_: Exception) {}
        Log.i(TAG, "MyraWakeWordService destroyed")
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_STOP_SERVICE = "com.myra.assistant.ACTION_STOP_WAKE_WORD"
        const val EXTRA_AUTO_START_VOICE = "AUTO_START_VOICE"
        const val EXTRA_TRIGGER_SOURCE = "TRIGGER_SOURCE"
        const val EXTRA_FOLLOW_UP_COMMAND = "FOLLOW_UP_COMMAND"

        var isRunning: Boolean = false
            private set

        fun start(context: Context) {
            if (androidx.core.content.ContextCompat.checkSelfPermission(
                    context, android.Manifest.permission.RECORD_AUDIO
                ) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                Log.w("MYRA_WakeWordService", "Not starting: RECORD_AUDIO permission has not been granted.")
                return
            }
            val intent = Intent(context, MyraWakeWordService::class.java)
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                Log.w("MYRA_WakeWordService", "Unable to start microphone foreground service from this app state.", e)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, MyraWakeWordService::class.java))
        }
    }
}
