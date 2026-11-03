package com.myra.assistant.connect

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.IBinder
import android.util.Base64
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.gson.Gson
import kotlinx.coroutines.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Android 15 Compliant MediaProjection Foreground Service.
 * Securely captures screen frames, compresses to JPEG, and streams them
 * to the connected primary phone across the local hotspot.
 * Enforces explicit consent and persistent notification indicator.
 */
class MyraConnectScreenShareService : Service() {

    private val tag = "MYRA_ScreenShare"
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val gson = Gson()
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(3, TimeUnit.SECONDS)
        .writeTimeout(3, TimeUnit.SECONDS)
        .readTimeout(3, TimeUnit.SECONDS)
        .build()

    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private var frameIndex = 0L

    private var targetIp: String = "127.0.0.1"
    private var targetPort: Int = 9876
    private var authToken: String = ""

    private val isStreaming = AtomicBoolean(false)
    private var streamJob: Job? = null

    companion object {
        const val CHANNEL_ID = "myra_screen_share_channel"
        const val NOTIFICATION_ID = 9021

        const val ACTION_START = "com.myra.assistant.connect.START_STREAM"
        const val ACTION_STOP = "com.myra.assistant.connect.STOP_STREAM"

        const val EXTRA_RESULT_CODE = "extra_result_code"
        const val EXTRA_DATA = "extra_data"
        const val EXTRA_TARGET_IP = "extra_target_ip"
        const val EXTRA_TARGET_PORT = "extra_target_port"
        const val EXTRA_AUTH_TOKEN = "extra_auth_token"

        @Volatile
        var isRunning: Boolean = false
            private set

        fun stop(context: Context) {
            val intent = Intent(context, MyraConnectScreenShareService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, Activity.RESULT_CANCELED)
                val data = intent.getParcelableExtra<Intent>(EXTRA_DATA)
                targetIp = intent.getStringExtra(EXTRA_TARGET_IP) ?: "127.0.0.1"
                targetPort = intent.getIntExtra(EXTRA_TARGET_PORT, 9876)
                authToken = intent.getStringExtra(EXTRA_AUTH_TOKEN) ?: ""

                if (resultCode == Activity.RESULT_OK && data != null) {
                    startForegroundWithNotification()
                    startProjection(resultCode, data)
                } else {
                    stopSelf()
                }
            }
            ACTION_STOP -> {
                stopStream()
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "MYRA Screen Sharing",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Active when MYRA Connect is sharing screen to paired phone"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun startForegroundWithNotification() {
        val stopIntent = Intent(this, MyraConnectScreenShareService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("MYRA Connect • Screen Streaming Active")
            .setContentText("Screen is being streamed to paired device ($targetIp)")
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .setOngoing(true)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop Sharing", stopPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
        isRunning = true
    }

    private fun startProjection(resultCode: Int, data: Intent) {
        val mpManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as? MediaProjectionManager
        mediaProjection = mpManager?.getMediaProjection(resultCode, data)

        val dm = resources.displayMetrics
        val width = (dm.widthPixels / 2).coerceAtLeast(360) // Scale down for lower latency over Wi-Fi
        val height = (dm.heightPixels / 2).coerceAtLeast(640)
        val density = dm.densityDpi

        imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)

        virtualDisplay = mediaProjection?.createVirtualDisplay(
            "MYRA-VirtualDisplay",
            width,
            height,
            density,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader?.surface,
            null,
            null
        )

        isStreaming.set(true)
        startFrameCaptureLoop(width, height)
    }

    private fun startFrameCaptureLoop(width: Int, height: Int) {
        streamJob = scope.launch {
            var lastFrameTime = 0L
            val frameIntervalMs = 70L // ~14-15 FPS optimal for local Wi-Fi hotspot bandwidth

            while (isActive && isStreaming.get()) {
                val now = System.currentTimeMillis()
                if (now - lastFrameTime >= frameIntervalMs) {
                    val image = try {
                        imageReader?.acquireLatestImage()
                    } catch (_: Exception) {
                        null
                    }

                    if (image != null) {
                        try {
                            val planes = image.planes
                            val buffer = planes[0].buffer
                            val pixelStride = planes[0].pixelStride
                            val rowStride = planes[0].rowStride
                            val rowPadding = rowStride - pixelStride * width

                            val bitmap = Bitmap.createBitmap(
                                width + rowPadding / pixelStride,
                                height,
                                Bitmap.Config.ARGB_8888
                            )
                            bitmap.copyPixelsFromBuffer(buffer)

                            val cropped = if (rowPadding > 0) {
                                Bitmap.createBitmap(bitmap, 0, 0, width, height)
                            } else {
                                bitmap
                            }

                            val outStream = ByteArrayOutputStream()
                            cropped.compress(Bitmap.CompressFormat.JPEG, 60, outStream)
                            val jpegBytes = outStream.toByteArray()
                            val b64 = Base64.encodeToString(jpegBytes, Base64.NO_WRAP)

                            frameIndex++
                            val packet = ScreenFramePacket(
                                frameIndex = frameIndex,
                                timestamp = now,
                                width = width,
                                height = height,
                                rotation = 0,
                                jpegBase64 = b64
                            )

                            transmitFrame(packet)
                            lastFrameTime = now
                        } catch (e: Exception) {
                            Log.e(tag, "Frame processing error", e)
                        } finally {
                            image.close()
                        }
                    }
                }
                delay(20)
            }
        }
    }

    private fun transmitFrame(packet: ScreenFramePacket) {
        try {
            val url = "http://$targetIp:$targetPort/screen/frame"
            val json = gson.toJson(packet)
            val body = json.toRequestBody(jsonMediaType)
            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $authToken")
                .post(body)
                .build()

            httpClient.newCall(request).execute().close()
        } catch (_: Exception) {}
    }

    private fun stopStream() {
        isStreaming.set(false)
        streamJob?.cancel()
        streamJob = null

        try {
            virtualDisplay?.release()
        } catch (_: Exception) {}
        virtualDisplay = null

        try {
            imageReader?.close()
        } catch (_: Exception) {}
        imageReader = null

        try {
            mediaProjection?.stop()
        } catch (_: Exception) {}
        mediaProjection = null

        isRunning = false
    }

    override fun onDestroy() {
        stopStream()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
