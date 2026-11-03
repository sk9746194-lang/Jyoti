package com.myra.companion

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.util.Log
import com.myra.companion.service.CompanionScreenCaptureService

/**
 * Transparent Activity that displays the system MediaProjection consent dialog on Android 15.
 */
class CompanionCaptureConsentActivity : Activity() {

    private val tag = "MYRA_CompConsent"
    private val requestMediaProjection = 8822

    private var targetIp: String = "127.0.0.1"
    private var targetPort: Int = 9876
    private var authToken: String = ""

    companion object {
        fun launchConsent(context: Context, targetIp: String, targetPort: Int = 9876, authToken: String = "") {
            val intent = Intent(context, CompanionCaptureConsentActivity::class.java).apply {
                putExtra(CompanionScreenCaptureService.EXTRA_TARGET_IP, targetIp)
                putExtra(CompanionScreenCaptureService.EXTRA_TARGET_PORT, targetPort)
                putExtra(CompanionScreenCaptureService.EXTRA_AUTH_TOKEN, authToken)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            context.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        targetIp = intent.getStringExtra(CompanionScreenCaptureService.EXTRA_TARGET_IP) ?: "127.0.0.1"
        targetPort = intent.getIntExtra(CompanionScreenCaptureService.EXTRA_TARGET_PORT, 9876)
        authToken = intent.getStringExtra(CompanionScreenCaptureService.EXTRA_AUTH_TOKEN) ?: ""

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
                Log.d(tag, "Screen capture consent granted. Launching CompanionScreenCaptureService.")
                val serviceIntent = Intent(this, CompanionScreenCaptureService::class.java).apply {
                    action = CompanionScreenCaptureService.ACTION_START
                    putExtra(CompanionScreenCaptureService.EXTRA_RESULT_CODE, resultCode)
                    putExtra(CompanionScreenCaptureService.EXTRA_DATA, data)
                    putExtra(CompanionScreenCaptureService.EXTRA_TARGET_IP, targetIp)
                    putExtra(CompanionScreenCaptureService.EXTRA_TARGET_PORT, targetPort)
                    putExtra(CompanionScreenCaptureService.EXTRA_AUTH_TOKEN, authToken)
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
