package com.myra.assistant.connect

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.util.Log

/**
 * Transparent Activity that requests explicit user consent for Android 15 MediaProjection screen sharing.
 * Starts MyraConnectScreenShareService with the resulting projection token.
 */
class ScreenCaptureConsentActivity : Activity() {

    private val tag = "MYRA_CaptureConsent"
    private val requestMediaProjection = 8821

    private var targetIp: String = "127.0.0.1"
    private var targetPort: Int = 9876
    private var authToken: String = ""

    companion object {
        fun launchConsent(context: Context, targetIp: String, targetPort: Int = 9876, authToken: String = "") {
            val intent = Intent(context, ScreenCaptureConsentActivity::class.java).apply {
                putExtra(MyraConnectScreenShareService.EXTRA_TARGET_IP, targetIp)
                putExtra(MyraConnectScreenShareService.EXTRA_TARGET_PORT, targetPort)
                putExtra(MyraConnectScreenShareService.EXTRA_AUTH_TOKEN, authToken)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            context.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        targetIp = intent.getStringExtra(MyraConnectScreenShareService.EXTRA_TARGET_IP) ?: "127.0.0.1"
        targetPort = intent.getIntExtra(MyraConnectScreenShareService.EXTRA_TARGET_PORT, 9876)
        authToken = intent.getStringExtra(MyraConnectScreenShareService.EXTRA_AUTH_TOKEN) ?: ""

        val mpManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as? MediaProjectionManager
        if (mpManager != null) {
            val captureIntent = mpManager.createScreenCaptureIntent()
            startActivityForResult(captureIntent, requestMediaProjection)
        } else {
            finish()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == requestMediaProjection) {
            if (resultCode == RESULT_OK && data != null) {
                Log.d(tag, "Screen capture consent granted. Launching MyraConnectScreenShareService.")
                val serviceIntent = Intent(this, MyraConnectScreenShareService::class.java).apply {
                    action = MyraConnectScreenShareService.ACTION_START
                    putExtra(MyraConnectScreenShareService.EXTRA_RESULT_CODE, resultCode)
                    putExtra(MyraConnectScreenShareService.EXTRA_DATA, data)
                    putExtra(MyraConnectScreenShareService.EXTRA_TARGET_IP, targetIp)
                    putExtra(MyraConnectScreenShareService.EXTRA_TARGET_PORT, targetPort)
                    putExtra(MyraConnectScreenShareService.EXTRA_AUTH_TOKEN, authToken)
                }
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    startForegroundService(serviceIntent)
                } else {
                    startService(serviceIntent)
                }
            } else {
                Log.w(tag, "Screen capture consent denied by user.")
            }
            finish()
        }
    }
}
