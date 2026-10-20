package com.myra.assistant.connect

import android.content.Context

/**
 * Validates incoming remote execution requests against user-approved permissions.
 * Protects the recipient device from unauthorized actions.
 */
class DevicePermissionManager(private val secureStore: SecureDeviceStore) {

    fun validateAction(action: RemoteCommandAction): PermissionCheckResult {
        val permissions = secureStore.getDevicePermissions()

        return when (action) {
            RemoteCommandAction.OPEN_APP,
            RemoteCommandAction.OPEN_YOUTUBE,
            RemoteCommandAction.OPEN_CHROME,
            RemoteCommandAction.OPEN_CAMERA,
            RemoteCommandAction.OPEN_SETTINGS,
            RemoteCommandAction.START_NAVIGATION -> {
                if (permissions.openApps) {
                    PermissionCheckResult.Allowed
                } else {
                    PermissionCheckResult.Denied("Remote App Launching is disabled in MYRA Connect Settings on this phone.")
                }
            }
            RemoteCommandAction.PLAY_MUSIC -> {
                if (permissions.mediaControls) {
                    PermissionCheckResult.Allowed
                } else {
                    PermissionCheckResult.Denied("Remote Media Controls are disabled in MYRA Connect Settings on this phone.")
                }
            }
            RemoteCommandAction.TOGGLE_TORCH,
            RemoteCommandAction.ADJUST_VOLUME,
            RemoteCommandAction.CHECK_BATTERY,
            RemoteCommandAction.RING_DEVICE,
            RemoteCommandAction.LOCK_DEVICE -> {
                if (permissions.deviceCommands) {
                    PermissionCheckResult.Allowed
                } else {
                    PermissionCheckResult.Denied("Remote Device Commands are disabled in MYRA Connect Settings on this phone.")
                }
            }
            RemoteCommandAction.SEND_NOTIFICATION -> {
                if (permissions.notifications) {
                    PermissionCheckResult.Allowed
                } else {
                    PermissionCheckResult.Denied("Remote Notifications are disabled in MYRA Connect Settings on this phone.")
                }
            }
            RemoteCommandAction.PING -> {
                PermissionCheckResult.Allowed
            }
            else -> {
                PermissionCheckResult.Allowed
            }
        }
    }
}

sealed class PermissionCheckResult {
    object Allowed : PermissionCheckResult()
    data class Denied(val reason: String) : PermissionCheckResult()
}
