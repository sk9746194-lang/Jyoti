package com.myra.assistant.tools

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.os.BatteryManager
import android.util.Log

/**
 * Real Hardware & System Settings Controller for MYRA Assistant.
 * Uses CameraManager for Flashlight, AudioManager for Volume, and BatteryManager for Charge.
 * Returns honest CommandResults.
 */
class DeviceControlManager(private val context: Context) {

    private val tag = "MYRA_DeviceControl"

    /**
     * Real Hardware Torch Control using CameraManager setTorchMode.
     * Iterates available camera IDs to find one with a physical flash unit.
     */
    fun toggleFlashlight(turnOn: Boolean): CommandResult {
        Log.d(tag, "MYRA_ACTION: TOGGLE_TORCH state=$turnOn")
        return try {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
            if (cameraManager == null) {
                Log.e(tag, "CameraManager service not available")
                return CommandResult.failed("Camera सेवा उपलब्ध नहीं है।", target = "torch")
            }

            var torchCameraId: String? = null
            for (id in cameraManager.cameraIdList) {
                val characteristics = cameraManager.getCameraCharacteristics(id)
                val hasFlash = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) ?: false
                if (hasFlash) {
                    torchCameraId = id
                    break
                }
            }

            if (torchCameraId == null) {
                Log.w(tag, "No camera with physical flash unit found")
                return CommandResult.notInstalled("इस डिवाइस में टॉर्च / फ्लैशलाइट हार्डवेयर नहीं मिला।", target = "torch")
            }

            cameraManager.setTorchMode(torchCameraId, turnOn)
            val actionName = if (turnOn) "चालू" else "बंद"
            val stateCode = if (turnOn) "ON" else "OFF"
            Log.d(tag, "MYRA_RESULT: SUCCESS, torch set to $stateCode on camera $torchCameraId")
            CommandResult.success("टॉर्च $actionName कर दी गई है।", target = "torch_$stateCode", detail = "cameraId: $torchCameraId")
        } catch (e: Exception) {
            Log.e(tag, "Failed to toggle flashlight", e)
            val actionName = if (turnOn) "चालू" else "बंद"
            CommandResult.failed("टॉर्च $actionName नहीं हो पाई। कृपया कैमरा अनुमति चेक करें।", target = "torch", detail = e.message ?: "")
        }
    }

    /**
     * Real battery percentage from Android BatteryManager.
     */
    fun getBatteryPercentage(): CommandResult {
        Log.d(tag, "MYRA_ACTION: CHECK_BATTERY")
        return try {
            val ifilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            val batteryStatus = context.registerReceiver(null, ifilter)
            val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
            val percent = if (level >= 0 && scale > 0) ((level / scale.toFloat()) * 100).toInt() else -1

            if (percent >= 0) {
                Log.d(tag, "MYRA_RESULT: SUCCESS, battery is $percent%")
                CommandResult.success("आपके फोन की बैटरी $percent% है।", target = "battery", detail = "$percent%")
            } else {
                Log.w(tag, "Battery level could not be read")
                CommandResult.failed("बैटरी लेवल नहीं पढ़ पाए।", target = "battery")
            }
        } catch (e: Exception) {
            Log.e(tag, "Battery check error", e)
            CommandResult.failed("बैटरी की जानकारी प्राप्त करने में समस्या आई।", target = "battery")
        }
    }

    /**
     * Real Volume Control using Android AudioManager.
     */
    fun setVolume(percent: Int): CommandResult {
        Log.d(tag, "MYRA_ACTION: ADJUST_VOLUME percent=$percent")
        return try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            if (audioManager == null) {
                return CommandResult.failed("Audio सेवा उपलब्ध नहीं है।", target = "volume")
            }
            val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            val clamped = percent.coerceIn(0, 100)
            val targetLevel = ((clamped / 100f) * max).toInt().coerceIn(0, max)
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, targetLevel, AudioManager.FLAG_SHOW_UI)
            Log.d(tag, "MYRA_RESULT: SUCCESS, volume set to $targetLevel / $max ($clamped%)")
            CommandResult.success("Media volume $clamped% पर सेट कर दी गई है।", target = "volume", detail = "$clamped%")
        } catch (e: Exception) {
            Log.e(tag, "Volume adjustment failed", e)
            CommandResult.failed("Volume बदलने में समस्या आई।", target = "volume", detail = e.message ?: "")
        }
    }

    /**
     * Lock screen display.
     */
    fun lockScreen(): CommandResult {
        Log.d(tag, "MYRA_ACTION: LOCK_DEVICE")
        return try {
            val intent = Intent(context, com.myra.assistant.ui.LockScreenOverlayActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            Log.d(tag, "MYRA_RESULT: SUCCESS, lock overlay launched")
            CommandResult.success("फोन लॉक स्क्रीन पर लगा दिया गया है।", target = "lock")
        } catch (e: Exception) {
            Log.e(tag, "Lock screen overlay failed", e)
            CommandResult.failed("स्क्रीन लॉक नहीं हो पाई।", target = "lock", detail = e.message ?: "")
        }
    }

    /**
     * Real Hardware Haptic Feedback Vibration sequence.
     * Uses Android Vibrator service with VibrationEffect.
     */
    fun triggerHapticFeedback(pattern: String = "speech_start"): CommandResult {
        Log.d(tag, "MYRA_ACTION: TRIGGER_HAPTIC pattern=$pattern")
        return try {
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? android.os.Vibrator
            if (vibrator != null && vibrator.hasVibrator()) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    val timings = if (pattern == "command_confirm") {
                        longArrayOf(0, 20, 40, 25)
                    } else {
                        longArrayOf(0, 15, 30, 10)
                    }
                    val amplitudes = if (pattern == "command_confirm") {
                        intArrayOf(0, 200, 0, 255)
                    } else {
                        intArrayOf(0, 150, 0, 180)
                    }
                    val effect = android.os.VibrationEffect.createWaveform(timings, amplitudes, -1)
                    vibrator.vibrate(effect)
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(longArrayOf(0, 20, 40, 25), -1)
                }
                CommandResult.success("Haptic feedback executed", target = "haptic")
            } else {
                CommandResult.failed("Vibrator not available", target = "haptic")
            }
        } catch (e: Exception) {
            Log.e(tag, "Haptic feedback failed", e)
            CommandResult.failed("Haptic error: ${e.message}", target = "haptic")
        }
    }
}

