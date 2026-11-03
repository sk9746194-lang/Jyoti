package com.myra.assistant.vision

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.hardware.camera2.*
import android.media.Image
import android.media.ImageReader
import android.os.Handler
import android.os.HandlerThread
import android.util.Base64
import android.util.Log
import androidx.core.content.ContextCompat
import com.myra.assistant.data.GeminiApiClient
import com.myra.assistant.data.PreferencesManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import kotlin.coroutines.resume

/**
 * Camera2-based Live Camera Vision Engine for MYRA Assistant.
 * Provides on-demand high-resolution frame capture and AI visual reasoning via Google Gemini Vision.
 * Strict lifecycle management prevents background camera execution, ensuring user privacy and battery safety.
 */
class CameraVisionManager(private val context: Context) {

    private val tag = "MYRA_CameraVision"

    private var cameraDevice: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null
    private var imageReader: ImageReader? = null
    private var backgroundThread: HandlerThread? = null
    private var backgroundHandler: Handler? = null

    private var isCapturing = false

    private fun startBackgroundThread() {
        if (backgroundThread == null) {
            backgroundThread = HandlerThread("MYRA_CameraBackground").apply { start() }
            backgroundHandler = Handler(backgroundThread!!.looper)
        }
    }

    private fun stopBackgroundThread() {
        backgroundThread?.quitSafely()
        try {
            backgroundThread?.join()
            backgroundThread = null
            backgroundHandler = null
        } catch (e: InterruptedException) {
            Log.e(tag, "Error stopping background thread", e)
        }
    }

    fun hasCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Captures a single high-definition snapshot frame from the back camera,
     * encodes it as JPEG Base64, and releases hardware immediately.
     */
    @SuppressLint("MissingPermission")
    suspend fun captureSnapshotBase64(): Result<String> = withContext(Dispatchers.IO) {
        if (!hasCameraPermission()) {
            return@withContext Result.failure(SecurityException("Camera permission not granted."))
        }

        startBackgroundThread()

        try {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
                ?: return@withContext Result.failure(IllegalStateException("CameraManager service unavailable."))

            // Prefer back-facing camera
            var targetCameraId: String? = null
            for (id in cameraManager.cameraIdList) {
                val chars = cameraManager.getCameraCharacteristics(id)
                val facing = chars.get(CameraCharacteristics.LENS_FACING)
                if (facing == CameraCharacteristics.LENS_FACING_BACK) {
                    targetCameraId = id
                    break
                }
            }
            if (targetCameraId == null && cameraManager.cameraIdList.isNotEmpty()) {
                targetCameraId = cameraManager.cameraIdList[0]
            }

            if (targetCameraId == null) {
                return@withContext Result.failure(IllegalStateException("No camera sensor found on device."))
            }

            val jpegBytes = suspendCancellableCoroutine<ByteArray?> { cont ->
                val reader = ImageReader.newInstance(1280, 720, ImageFormat.JPEG, 2)
                imageReader = reader

                reader.setOnImageAvailableListener({ ir ->
                    val image: Image? = ir.acquireLatestImage()
                    if (image != null) {
                        try {
                            val buffer: ByteBuffer = image.planes[0].buffer
                            val bytes = ByteArray(buffer.remaining())
                            buffer.get(bytes)
                            if (cont.isActive) cont.resume(bytes)
                        } catch (e: Exception) {
                            Log.e(tag, "Error acquiring camera image bytes", e)
                            if (cont.isActive) cont.resume(null)
                        } finally {
                            image.close()
                        }
                    }
                }, backgroundHandler)

                cameraManager.openCamera(targetCameraId, object : CameraDevice.StateCallback() {
                    override fun onOpened(camera: CameraDevice) {
                        cameraDevice = camera
                        try {
                            val surface = reader.surface
                            @Suppress("DEPRECATION")
                            camera.createCaptureSession(
                                listOf(surface),
                                object : CameraCaptureSession.StateCallback() {
                                    override fun onConfigured(session: CameraCaptureSession) {
                                        captureSession = session
                                        try {
                                            val captureBuilder = camera.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE).apply {
                                                addTarget(surface)
                                                set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
                                                set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH)
                                            }
                                            session.capture(captureBuilder.build(), null, backgroundHandler)
                                        } catch (e: Exception) {
                                            Log.e(tag, "Failed to send capture request", e)
                                            if (cont.isActive) cont.resume(null)
                                        }
                                    }

                                    override fun onConfigureFailed(session: CameraCaptureSession) {
                                        Log.e(tag, "Camera capture session configuration failed")
                                        if (cont.isActive) cont.resume(null)
                                    }
                                },
                                backgroundHandler
                            )
                        } catch (e: Exception) {
                            Log.e(tag, "Error setting up camera session", e)
                            if (cont.isActive) cont.resume(null)
                        }
                    }

                    override fun onDisconnected(camera: CameraDevice) {
                        camera.close()
                        cameraDevice = null
                    }

                    override fun onError(camera: CameraDevice, error: Int) {
                        camera.close()
                        cameraDevice = null
                        Log.e(tag, "CameraDevice open error: $error")
                        if (cont.isActive) cont.resume(null)
                    }
                }, backgroundHandler)

                cont.invokeOnCancellation {
                    releaseCamera()
                }
            }

            releaseCamera()

            if (jpegBytes != null && jpegBytes.isNotEmpty()) {
                val base64 = Base64.encodeToString(jpegBytes, Base64.NO_WRAP)
                Result.success(base64)
            } else {
                Result.failure(IOException("Failed to capture image frame from camera sensor."))
            }
        } catch (e: Exception) {
            releaseCamera()
            Log.e(tag, "Exception during camera snapshot", e)
            Result.failure(e)
        }
    }

    /**
     * Takes a snapshot and sends it immediately to Gemini Vision API with user question.
     */
    suspend fun inspectSceneWithGemini(userPrompt: String): Result<String> {
        val prefs = PreferencesManager(context)
        if (!prefs.hasConfiguredApiKey) {
            return Result.failure(IllegalStateException("Gemini API Key is not configured in MYRA Settings."))
        }

        val snapshotResult = captureSnapshotBase64()
        if (snapshotResult.isFailure) {
            return Result.failure(snapshotResult.exceptionOrNull() ?: Exception("Camera frame capture failed"))
        }

        val base64 = snapshotResult.getOrThrow()
        return GeminiApiClient.generateVisionAnalysis(
            apiKey = prefs.geminiApiKey,
            model = prefs.selectedGeminiModel,
            imageBase64 = base64,
            userPrompt = userPrompt
        )
    }

    /**
     * Safely closes hardware handles immediately after use.
     */
    fun releaseCamera() {
        try {
            captureSession?.close()
            captureSession = null
            cameraDevice?.close()
            cameraDevice = null
            imageReader?.close()
            imageReader = null
            stopBackgroundThread()
            Log.d(tag, "Camera hardware released successfully.")
        } catch (e: Exception) {
            Log.w(tag, "Error releasing camera: ${e.message}")
        }
    }
}
