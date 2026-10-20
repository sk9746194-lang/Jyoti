package com.myra.assistant.connect

import com.google.gson.annotations.SerializedName

/**
 * Connection Status of a Paired Remote Phone.
 */
enum class ConnectionStatus {
    CONNECTED,
    CONNECTING,
    DISCONNECTED,
    PAIRING_REQUIRED
}

/**
 * Physical or Network Transport type between phones.
 */
enum class ConnectionType {
    WIFI_LOCAL,
    HOTSPOT,
    BLUETOOTH,
    INTERNET_SECURE
}

/**
 * Stored representation of a paired Android device.
 */
data class PairedDevice(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("ipAddress") val ipAddress: String,
    @SerializedName("port") val port: Int = 9876,
    @SerializedName("authToken") val authToken: String,
    @SerializedName("status") val status: ConnectionStatus = ConnectionStatus.DISCONNECTED,
    @SerializedName("connectionType") val connectionType: ConnectionType = ConnectionType.WIFI_LOCAL,
    @SerializedName("lastConnectedTime") val lastConnectedTime: Long = System.currentTimeMillis(),
    @SerializedName("isHost") val isHost: Boolean = false
)

/**
 * Granular User-Controlled Permissions on the Companion / Receiving phone.
 */
data class DevicePermissions(
    @SerializedName("openApps") val openApps: Boolean = true,
    @SerializedName("mediaControls") val mediaControls: Boolean = true,
    @SerializedName("deviceCommands") val deviceCommands: Boolean = true,
    @SerializedName("notifications") val notifications: Boolean = false,
    @SerializedName("files") val files: Boolean = false,
    @SerializedName("contactsCalls") val contactsCalls: Boolean = false
)

/**
 * Supported Remote Command Actions.
 */
enum class RemoteCommandAction {
    OPEN_APP,
    PLAY_MUSIC,
    TOGGLE_TORCH,
    ADJUST_VOLUME,
    CHECK_BATTERY,
    LOCK_DEVICE,
    PING,
    // Dashboard & Diagnostics
    GET_DEVICE_INFO,
    GET_INSTALLED_APPS,
    GET_SHARED_FILES,
    READ_SHARED_FILE,
    GET_DEVICE_HEALTH,
    GET_ERROR_LOGS,
    GET_SHARED_PERMISSIONS,
    UPDATE_SHARED_PERMISSIONS,
    // Approved Remote Actions
    RING_DEVICE,
    SEND_NOTIFICATION,
    START_NAVIGATION,
    OPEN_CHROME,
    OPEN_CAMERA,
    OPEN_YOUTUBE,
    OPEN_SETTINGS
}

/**
 * Granular Shared Information Privacy Settings on the Remote Companion Phone.
 * Every category can be independently enabled or disabled by the user.
 */
data class SharedInformationSettings(
    @SerializedName("allowDeviceInfo") val allowDeviceInfo: Boolean = true,
    @SerializedName("allowInstalledApps") val allowInstalledApps: Boolean = true,
    @SerializedName("allowSharedFiles") val allowSharedFiles: Boolean = true,
    @SerializedName("allowDiagnostics") val allowDiagnostics: Boolean = true
)

/**
 * Comprehensive User-Approved Diagnostic Information for Remote Phone Dashboard.
 */
data class RemoteDeviceInfo(
    @SerializedName("deviceName") val deviceName: String,
    @SerializedName("manufacturer") val manufacturer: String,
    @SerializedName("model") val model: String,
    @SerializedName("androidVersion") val androidVersion: String,
    @SerializedName("sdkInt") val sdkInt: Int,
    @SerializedName("appVersion") val appVersion: String,
    @SerializedName("availableStorageBytes") val availableStorageBytes: Long,
    @SerializedName("totalStorageBytes") val totalStorageBytes: Long,
    @SerializedName("usedStorageBytes") val usedStorageBytes: Long,
    @SerializedName("batteryPercentage") val batteryPercentage: Int,
    @SerializedName("isCharging") val isCharging: Boolean,
    @SerializedName("chargingType") val chargingType: String,
    @SerializedName("networkType") val networkType: String,
    @SerializedName("wifiSsid") val wifiSsid: String?,
    @SerializedName("isBluetoothEnabled") val isBluetoothEnabled: Boolean,
    @SerializedName("availableRamBytes") val availableRamBytes: Long,
    @SerializedName("totalRamBytes") val totalRamBytes: Long,
    @SerializedName("lastConnectedTime") val lastConnectedTime: Long,
    @SerializedName("myraServiceStatus") val myraServiceStatus: String
)

/**
 * User-Approved Installed Application on Remote Phone.
 */
data class RemoteInstalledApp(
    @SerializedName("appName") val appName: String,
    @SerializedName("packageName") val packageName: String,
    @SerializedName("versionName") val versionName: String,
    @SerializedName("versionCode") val versionCode: Long,
    @SerializedName("installedDate") val installedDate: Long,
    @SerializedName("updatedDate") val updatedDate: Long,
    @SerializedName("isSystemApp") val isSystemApp: Boolean
)

/**
 * User-Approved Shared File from MediaStore / Storage Access Framework (SAF).
 */
data class RemoteSharedFile(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("path") val path: String,
    @SerializedName("category") val category: String, // Documents, Pictures, Videos, Downloads, Music
    @SerializedName("mimeType") val mimeType: String,
    @SerializedName("sizeBytes") val sizeBytes: Long,
    @SerializedName("lastModified") val lastModified: Long,
    @SerializedName("isDirectory") val isDirectory: Boolean,
    @SerializedName("fileContentBase64") val fileContentBase64: String? = null
)

/**
 * Diagnostic Health Issue identified on the Remote Device.
 */
data class RemoteHealthIssue(
    @SerializedName("id") val id: String,
    @SerializedName("severity") val severity: String, // CRITICAL, WARNING, INFO
    @SerializedName("title") val title: String,
    @SerializedName("description") val description: String,
    @SerializedName("cause") val cause: String,
    @SerializedName("userActionRequired") val userActionRequired: String,
    @SerializedName("fixInstructions") val fixInstructions: String,
    @SerializedName("settingAction") val settingAction: String? = null
)

/**
 * Overall Health Diagnostic Report of Remote Phone.
 */
data class RemoteHealthReport(
    @SerializedName("overallStatus") val overallStatus: String, // OPTIMAL, WARNING, ACTION_REQUIRED
    @SerializedName("storageStatus") val storageStatus: String,
    @SerializedName("batteryStatus") val batteryStatus: String,
    @SerializedName("networkStatus") val networkStatus: String,
    @SerializedName("serviceStatus") val serviceStatus: String,
    @SerializedName("permissionStatus") val permissionStatus: String,
    @SerializedName("issues") val issues: List<RemoteHealthIssue>,
    @SerializedName("timestamp") val timestamp: Long = System.currentTimeMillis()
)

/**
 * Sanitized Safe Diagnostic Error Log entry (NO sensitive tokens, passwords or chats).
 */
data class RemoteSafeLog(
    @SerializedName("id") val id: String,
    @SerializedName("timestamp") val timestamp: Long,
    @SerializedName("level") val level: String, // ERROR, WARN, INFO
    @SerializedName("tag") val tag: String,
    @SerializedName("message") val message: String
)

/**
 * Payload sent from Main Phone to Remote Phone.
 */
data class RemoteCommand(
    @SerializedName("commandId") val commandId: String,
    @SerializedName("action") val action: RemoteCommandAction,
    @SerializedName("target") val target: String = "",
    @SerializedName("payload") val payload: String = "",
    @SerializedName("authToken") val authToken: String,
    @SerializedName("timestamp") val timestamp: Long = System.currentTimeMillis()
)

/**
 * Detailed Status of Remote Command Execution.
 */
enum class RemoteResultStatus {
    SUCCESS,
    FAILED,
    NOT_CONNECTED,
    DEVICE_OFFLINE,
    PERMISSION_REQUIRED,
    APP_NOT_INSTALLED,
    UNSUPPORTED,
    NOT_SUPPORTED
}

/**
 * Result returned from Remote Phone to Main Phone.
 */
data class RemoteCommandResult(
    @SerializedName("commandId") val commandId: String,
    @SerializedName("status") val status: RemoteResultStatus,
    @SerializedName("message") val message: String,
    @SerializedName("payload") val payload: String = "",
    @SerializedName("timestamp") val timestamp: Long = System.currentTimeMillis()
)

/**
 * Pairing Request payload exchanged during QR code or 6-digit PIN handshake.
 */
data class PairingRequest(
    @SerializedName("deviceId") val deviceId: String,
    @SerializedName("deviceName") val deviceName: String,
    @SerializedName("ipAddress") val ipAddress: String,
    @SerializedName("port") val port: Int = 9876,
    @SerializedName("pairingCode") val pairingCode: String,
    @SerializedName("sharedSecret") val sharedSecret: String
)

/**
 * Pairing Response payload after mutual confirmation.
 */
data class PairingResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("authToken") val authToken: String = "",
    @SerializedName("deviceName") val deviceName: String = "",
    @SerializedName("errorMessage") val errorMessage: String? = null
)
