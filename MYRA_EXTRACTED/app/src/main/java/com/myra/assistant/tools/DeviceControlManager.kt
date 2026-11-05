package com.myra.assistant.tools

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.os.BatteryManager
import android.os.Build
import android.provider.AlarmClock
import android.provider.CalendarContract
import android.provider.Settings
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

    /**
     * Creates an alarm on Android clock app using AlarmClock.ACTION_SET_ALARM.
     */
    fun setAlarm(hour: Int, minute: Int, message: String = "MYRA Alarm"): CommandResult {
        Log.d(tag, "MYRA_ACTION: SET_ALARM hour=$hour, minute=$minute, msg='$message'")
        return try {
            val validHour = hour.coerceIn(0, 23)
            val validMinute = minute.coerceIn(0, 59)
            val timeLabel = String.format("%02d:%02d", validHour, validMinute)

            val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
                putExtra(AlarmClock.EXTRA_HOUR, validHour)
                putExtra(AlarmClock.EXTRA_MINUTES, validMinute)
                putExtra(AlarmClock.EXTRA_MESSAGE, message.ifBlank { "MYRA Alarm" })
                putExtra(AlarmClock.EXTRA_SKIP_UI, false)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }

            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
                Log.d(tag, "MYRA_RESULT: SUCCESS, alarm set for $timeLabel")
                CommandResult.success(
                    "$timeLabel बजे का अलार्म सेट कर दिया गया है ($message)।",
                    target = "alarm",
                    detail = "$timeLabel • $message"
                )
            } else {
                Log.w(tag, "No Clock application found to handle ACTION_SET_ALARM")
                CommandResult.notInstalled(
                    "फोन में Clock ऐप नहीं मिला।",
                    target = "alarm"
                )
            }
        } catch (e: Exception) {
            Log.e(tag, "Failed to set alarm", e)
            CommandResult.failed(
                "अलार्म सेट करने में समस्या आई: ${e.localizedMessage ?: "Unknown error"}",
                target = "alarm"
            )
        }
    }

    /**
     * Creates a countdown timer using AlarmClock.ACTION_SET_TIMER.
     */
    fun setTimer(seconds: Int, message: String = "MYRA Timer"): CommandResult {
        Log.d(tag, "MYRA_ACTION: SET_TIMER seconds=$seconds, msg='$message'")
        return try {
            val lengthSec = seconds.coerceAtLeast(1)
            val intent = Intent(AlarmClock.ACTION_SET_TIMER).apply {
                putExtra(AlarmClock.EXTRA_LENGTH, lengthSec)
                putExtra(AlarmClock.EXTRA_MESSAGE, message.ifBlank { "MYRA Timer" })
                putExtra(AlarmClock.EXTRA_SKIP_UI, false)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }

            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
                val minStr = if (lengthSec >= 60) "${lengthSec / 60} मिनट " else ""
                val secStr = if (lengthSec % 60 != 0 || lengthSec < 60) "${lengthSec % 60} सेकंड" else ""
                CommandResult.success(
                    "$minStr$secStr का टाइमर शुरू कर दिया गया है।",
                    target = "timer",
                    detail = "$lengthSec seconds • $message"
                )
            } else {
                CommandResult.notInstalled("घड़ी (Timer) ऐप नहीं मिला।", target = "timer")
            }
        } catch (e: Exception) {
            Log.e(tag, "Failed to set timer", e)
            CommandResult.failed("टाइमर सेट करने में समस्या आई: ${e.message}", target = "timer")
        }
    }

    /**
     * Creates a Calendar event via official CalendarContract.Events intent.
     * Safely presents confirmation in the user's default Calendar app.
     */
    fun addCalendarEvent(
        title: String,
        location: String = "",
        description: String = "",
        beginTimeMillis: Long = 0L,
        endTimeMillis: Long = 0L
    ): CommandResult {
        Log.d(tag, "MYRA_ACTION: ADD_CALENDAR_EVENT title='$title'")
        return try {
            val intent = Intent(Intent.ACTION_INSERT).apply {
                data = CalendarContract.Events.CONTENT_URI
                putExtra(CalendarContract.Events.TITLE, title.ifBlank { "New Event" })
                if (location.isNotBlank()) putExtra(CalendarContract.Events.EVENT_LOCATION, location)
                if (description.isNotBlank()) putExtra(CalendarContract.Events.DESCRIPTION, description)
                if (beginTimeMillis > 0L) putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, beginTimeMillis)
                if (endTimeMillis > 0L) putExtra(CalendarContract.EXTRA_EVENT_END_TIME, endTimeMillis)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }

            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
                CommandResult.success(
                    "कैलेंडर में '$title' इवेंट बनाने का पेज खोल दिया है। पुष्टि करके सेव करें।",
                    target = "calendar",
                    detail = title
                )
            } else {
                CommandResult.notInstalled("कैलेंडर ऐप नहीं मिला।", target = "calendar")
            }
        } catch (e: Exception) {
            Log.e(tag, "Failed to add calendar event", e)
            CommandResult.failed("कैलेंडर इवेंट खोलने में समस्या आई: ${e.message}", target = "calendar")
        }
    }

    /**
     * Opens Android Wi-Fi Settings Panel (Slice on Android 10+, Activity on older).
     * Complies with modern Android privacy/security policies prohibiting background Wi-Fi toggling.
     */
    fun openWifiSettings(): CommandResult {
        Log.d(tag, "MYRA_ACTION: OPEN_WIFI_SETTINGS")
        return try {
            val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                Intent(Settings.Panel.ACTION_WIFI).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
            } else {
                Intent(Settings.ACTION_WIFI_SETTINGS).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
            }
            context.startActivity(intent)
            CommandResult.success("Wi-Fi सेटिंग्स पैनल खोल दिया गया है।", target = "wifi_panel")
        } catch (e: Exception) {
            Log.e(tag, "Failed to open Wi-Fi settings", e)
            try {
                val fallbackIntent = Intent(Settings.ACTION_WIFI_SETTINGS).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
                context.startActivity(fallbackIntent)
                CommandResult.success("Wi-Fi सेटिंग्स खोल दी गई हैं।", target = "wifi_settings")
            } catch (e2: Exception) {
                CommandResult.failed("Wi-Fi सेटिंग्स नहीं खुल सकीं।", target = "wifi")
            }
        }
    }

    /**
     * Opens Android Bluetooth Settings Panel.
     * Complies with modern Android restrictions on background Bluetooth toggling.
     */
    fun openBluetoothSettings(): CommandResult {
        Log.d(tag, "MYRA_ACTION: OPEN_BLUETOOTH_SETTINGS")
        return try {
            val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            CommandResult.success("ब्लूटूथ सेटिंग्स खोल दी गई हैं।", target = "bluetooth_settings")
        } catch (e: Exception) {
            Log.e(tag, "Failed to open Bluetooth settings", e)
            CommandResult.failed("ब्लूटूथ सेटिंग्स नहीं खुल सकीं।", target = "bluetooth")
        }
    }
}

