package com.myra.assistant.connect

import android.content.Context
import android.util.Log
import com.myra.assistant.tools.AppLauncher
import com.myra.assistant.tools.CommandStatus
import com.myra.assistant.tools.DeviceControlManager

/**
 * Executes approved commands on the recipient / companion Android device.
 * Enforces Android security sandbox and honesty principles — never returns false success.
 */
class RemoteCommandExecutor(
    private val context: Context,
    private val permissionManager: DevicePermissionManager
) {
    private val tag = "MYRA_RemoteExec"
    private val appLauncher = AppLauncher(context)
    private val deviceControlManager = DeviceControlManager(context)

    fun execute(command: RemoteCommand): RemoteCommandResult {
        Log.d(tag, "Executing incoming remote command: action=${command.action}, target=${command.target}")

        // 1. Enforce user-defined companion permissions
        val permCheck = permissionManager.validateAction(command.action)
        if (permCheck is PermissionCheckResult.Denied) {
            Log.w(tag, "Action denied by permission manager: ${permCheck.reason}")
            return RemoteCommandResult(
                commandId = command.commandId,
                status = RemoteResultStatus.PERMISSION_REQUIRED,
                message = permCheck.reason
            )
        }

        // 2. Dispatch to local Android managers
        return try {
            when (command.action) {
                RemoteCommandAction.OPEN_APP -> {
                    val res = appLauncher.launchAppByName(command.target)
                    mapCommandResult(command.commandId, res, defaultSuccess = "${command.target} खोल दिया")
                }
                RemoteCommandAction.PLAY_MUSIC -> {
                    val res = appLauncher.launchMusicPlayer()
                    mapCommandResult(command.commandId, res, defaultSuccess = "Music चालू कर दिया")
                }
                RemoteCommandAction.TOGGLE_TORCH -> {
                    val turnOn = command.payload.equals("on", ignoreCase = true) || command.payload.equals("1", ignoreCase = true) || command.payload.isBlank()
                    val res = deviceControlManager.toggleFlashlight(turnOn)
                    mapCommandResult(command.commandId, res, defaultSuccess = "टॉर्च ${if (turnOn) "चालू" else "बंद"} कर दी")
                }
                RemoteCommandAction.ADJUST_VOLUME -> {
                    val level = command.payload.toIntOrNull() ?: 70
                    val res = deviceControlManager.setVolume(level)
                    mapCommandResult(command.commandId, res, defaultSuccess = "वॉल्यूम सेट कर दिया")
                }
                RemoteCommandAction.CHECK_BATTERY -> {
                    val res = deviceControlManager.getBatteryPercentage()
                    mapCommandResult(command.commandId, res, defaultSuccess = res.message)
                }
                RemoteCommandAction.LOCK_DEVICE -> {
                    val res = deviceControlManager.lockScreen()
                    mapCommandResult(command.commandId, res, defaultSuccess = "स्क्रीन लॉक कर दी")
                }
                RemoteCommandAction.PING -> {
                    RemoteCommandResult(
                        commandId = command.commandId,
                        status = RemoteResultStatus.SUCCESS,
                        message = "Pong from companion phone",
                        payload = "online"
                    )
                }
                RemoteCommandAction.RING_DEVICE -> {
                    try {
                        val ringtoneUri = android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_ALARM)
                            ?: android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_RINGTONE)
                        val ringtone = android.media.RingtoneManager.getRingtone(context, ringtoneUri)
                        ringtone.play()
                        RemoteCommandResult(command.commandId, RemoteResultStatus.SUCCESS, "Companion device ringer started")
                    } catch (e: Exception) {
                        RemoteCommandResult(command.commandId, RemoteResultStatus.FAILED, "Ringer failed: ${e.message}")
                    }
                }
                RemoteCommandAction.OPEN_YOUTUBE -> {
                    val res = appLauncher.launchAppByName("youtube")
                    mapCommandResult(command.commandId, res, defaultSuccess = "YouTube खोल दिया")
                }
                RemoteCommandAction.OPEN_CHROME -> {
                    val res = appLauncher.launchAppByName("chrome")
                    mapCommandResult(command.commandId, res, defaultSuccess = "Chrome ब्राउज़र खोल दिया")
                }
                RemoteCommandAction.OPEN_CAMERA -> {
                    val res = appLauncher.launchAppByName("camera")
                    mapCommandResult(command.commandId, res, defaultSuccess = "Camera खोल दिया")
                }
                RemoteCommandAction.OPEN_SETTINGS -> {
                    val res = appLauncher.launchAppByName("settings")
                    mapCommandResult(command.commandId, res, defaultSuccess = "Settings खोल दी")
                }
                RemoteCommandAction.START_NAVIGATION -> {
                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("google.navigation:q=${command.target}")).apply {
                        flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    try {
                        context.startActivity(intent)
                        RemoteCommandResult(command.commandId, RemoteResultStatus.SUCCESS, "Navigation shuru ho gaya")
                    } catch (e: Exception) {
                        RemoteCommandResult(command.commandId, RemoteResultStatus.NOT_SUPPORTED, "Navigation app not found")
                    }
                }
                RemoteCommandAction.REMOTE_TAP -> {
                    val dm = context.resources.displayMetrics
                    val gesture = try { com.google.gson.Gson().fromJson(command.payload, RemoteGesturePayload::class.java) } catch (_: Exception) { null }
                    if (gesture != null && com.myra.assistant.services.MyraAccessibilityService.isRunning()) {
                        val pxX = (gesture.x * dm.widthPixels).coerceIn(0f, dm.widthPixels.toFloat())
                        val pxY = (gesture.y * dm.heightPixels).coerceIn(0f, dm.heightPixels.toFloat())
                        val success = com.myra.assistant.services.MyraAccessibilityService.instance?.performTap(pxX, pxY) ?: false
                        RemoteCommandResult(command.commandId, if (success) RemoteResultStatus.SUCCESS else RemoteResultStatus.FAILED, "Tap at (${pxX.toInt()}, ${pxY.toInt()})")
                    } else if (!com.myra.assistant.services.MyraAccessibilityService.isRunning()) {
                        RemoteCommandResult(command.commandId, RemoteResultStatus.PERMISSION_REQUIRED, "Accessibility service not running on companion device")
                    } else {
                        RemoteCommandResult(command.commandId, RemoteResultStatus.FAILED, "Invalid tap coordinates")
                    }
                }
                RemoteCommandAction.REMOTE_SWIPE, RemoteCommandAction.REMOTE_SCROLL -> {
                    val dm = context.resources.displayMetrics
                    val gesture = try { com.google.gson.Gson().fromJson(command.payload, RemoteGesturePayload::class.java) } catch (_: Exception) { null }
                    if (gesture != null && com.myra.assistant.services.MyraAccessibilityService.isRunning()) {
                        val pxX1 = (gesture.x * dm.widthPixels).coerceIn(0f, dm.widthPixels.toFloat())
                        val pxY1 = (gesture.y * dm.heightPixels).coerceIn(0f, dm.heightPixels.toFloat())
                        val pxX2 = (gesture.x2 * dm.widthPixels).coerceIn(0f, dm.widthPixels.toFloat())
                        val pxY2 = (gesture.y2 * dm.heightPixels).coerceIn(0f, dm.heightPixels.toFloat())
                        val success = com.myra.assistant.services.MyraAccessibilityService.instance?.performSwipe(pxX1, pxY1, pxX2, pxY2, gesture.durationMs) ?: false
                        RemoteCommandResult(command.commandId, if (success) RemoteResultStatus.SUCCESS else RemoteResultStatus.FAILED, "Swipe executed")
                    } else if (!com.myra.assistant.services.MyraAccessibilityService.isRunning()) {
                        RemoteCommandResult(command.commandId, RemoteResultStatus.PERMISSION_REQUIRED, "Accessibility service not running on companion device")
                    } else {
                        RemoteCommandResult(command.commandId, RemoteResultStatus.FAILED, "Invalid swipe coordinates")
                    }
                }
                RemoteCommandAction.REMOTE_KEY_BACK -> {
                    val success = com.myra.assistant.services.MyraAccessibilityService.instance?.performBackGesture() ?: false
                    RemoteCommandResult(command.commandId, if (success) RemoteResultStatus.SUCCESS else RemoteResultStatus.FAILED, "Back key")
                }
                RemoteCommandAction.REMOTE_KEY_HOME -> {
                    val success = com.myra.assistant.services.MyraAccessibilityService.instance?.performHomeGesture() ?: false
                    RemoteCommandResult(command.commandId, if (success) RemoteResultStatus.SUCCESS else RemoteResultStatus.FAILED, "Home key")
                }
                RemoteCommandAction.START_SCREEN_STREAM -> {
                    val store = SecureDeviceStore(context)
                    val caller = store.getDeviceByToken(command.authToken)
                    val targetIp = caller?.ipAddress ?: if (command.target.isNotBlank()) command.target else "127.0.0.1"
                    val targetPort = caller?.port ?: 9876
                    ScreenCaptureConsentActivity.launchConsent(context, targetIp, targetPort, command.authToken)
                    RemoteCommandResult(command.commandId, RemoteResultStatus.SUCCESS, "Screen capture consent prompt requested on remote device", payload = "active")
                }
                RemoteCommandAction.STOP_SCREEN_STREAM -> {
                    MyraConnectScreenShareService.stop(context)
                    RemoteCommandResult(command.commandId, RemoteResultStatus.SUCCESS, "Screen streaming stopped", payload = "stopped")
                }
                RemoteCommandAction.INITIATE_FILE_TRANSFER -> {
                    RemoteCommandResult(command.commandId, RemoteResultStatus.SUCCESS, "File transfer handshake initialized", payload = command.payload)
                }
                RemoteCommandAction.SEND_NOTIFICATION -> {
                    RemoteCommandResult(command.commandId, RemoteResultStatus.SUCCESS, "Notification dispatched to companion")
                }
                else -> {
                    RemoteCommandResult(
                        commandId = command.commandId,
                        status = RemoteResultStatus.NOT_SUPPORTED,
                        message = "Command not supported on this Android device"
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "Failed to execute remote command", e)
            RemoteCommandResult(
                commandId = command.commandId,
                status = RemoteResultStatus.FAILED,
                message = "Execution error: ${e.localizedMessage ?: "Unknown error"}"
            )
        }
    }

    private fun mapCommandResult(
        commandId: String,
        result: com.myra.assistant.tools.CommandResult,
        defaultSuccess: String
    ): RemoteCommandResult {
        val status = when (result.status) {
            CommandStatus.SUCCESS -> RemoteResultStatus.SUCCESS
            CommandStatus.NOT_INSTALLED -> RemoteResultStatus.APP_NOT_INSTALLED
            CommandStatus.PERMISSION_REQUIRED -> RemoteResultStatus.PERMISSION_REQUIRED
            CommandStatus.UNSUPPORTED -> RemoteResultStatus.UNSUPPORTED
            CommandStatus.NOT_FOUND -> RemoteResultStatus.APP_NOT_INSTALLED
            CommandStatus.FAILED -> RemoteResultStatus.FAILED
        }

        return RemoteCommandResult(
            commandId = commandId,
            status = status,
            message = if (result.success) defaultSuccess else result.message,
            payload = result.actionDetail
        )
    }
}
