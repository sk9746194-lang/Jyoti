package com.myra.assistant.connect

import android.content.Context
import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.*
import java.util.UUID

/**
 * Main Singleton Coordinator for MYRA Connect — Phone-to-Phone Interconnectivity.
 * Manages device pairing, background communication server, permissions, and remote command routing.
 */
class MyraConnectManager private constructor(private val context: Context) {

    private val tag = "MYRA_ConnectManager"
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    val secureStore = SecureDeviceStore(context)
    val pairingManager = DevicePairingManager(context, secureStore)
    val permissionManager = DevicePermissionManager(secureStore)
    val commandExecutor = RemoteCommandExecutor(context, permissionManager)
    val transport = ConnectionTransport(context, secureStore, pairingManager, commandExecutor)

    // Reactive Compose State for UI
    private val _pairedDevices = mutableStateOf<List<PairedDevice>>(emptyList())
    val pairedDevices: State<List<PairedDevice>> = _pairedDevices

    private val _localIp = mutableStateOf("127.0.0.1")
    val localIp: State<String> = _localIp

    init {
        refreshDevices()
        refreshLocalIp()
        transport.startServer()
    }

    companion object {
        @Volatile
        private var instance: MyraConnectManager? = null

        fun getInstance(context: Context): MyraConnectManager {
            return instance ?: synchronized(this) {
                instance ?: MyraConnectManager(context.applicationContext).also { instance = it }
            }
        }
    }

    fun refreshLocalIp() {
        _localIp.value = transport.getLocalIpAddress()
    }

    fun refreshDevices() {
        _pairedDevices.value = secureStore.getPairedDevices()
    }

    /**
     * Checks online connectivity status of all paired devices.
     */
    fun checkAllDevicesStatus() {
        scope.launch(Dispatchers.IO) {
            val list = secureStore.getPairedDevices()
            val updated = list.map { dev ->
                val isOnline = transport.pingDevice(dev)
                val status = if (isOnline) ConnectionStatus.CONNECTED else ConnectionStatus.DISCONNECTED
                secureStore.updateDeviceStatus(dev.id, status)
                dev.copy(status = status)
            }
            withContext(Dispatchers.Main) {
                _pairedDevices.value = updated
            }
        }
    }

    /**
     * Starts Pairing Mode on this device (generates 6-digit PIN and QR payload).
     */
    fun hostPairing(): PairingHostInfo {
        refreshLocalIp()
        return pairingManager.startHostingPairing(_localIp.value)
    }

    fun stopHostingPairing() {
        pairingManager.stopHostingPairing()
    }

    /**
     * Connects to a remote hosting phone using its IP, Port, and 6-digit code.
     */
    fun pairWithRemoteDevice(
        targetIp: String,
        port: Int,
        sixDigitCode: String,
        onComplete: (Boolean, String?) -> Unit
    ) {
        scope.launch {
            val request = PairingRequest(
                deviceId = secureStore.thisDeviceId,
                deviceName = secureStore.thisDeviceName,
                ipAddress = _localIp.value,
                port = secureStore.serverPort,
                pairingCode = sixDigitCode.trim(),
                sharedSecret = UUID.randomUUID().toString()
            )

            val response = transport.sendPairingRequest(targetIp.trim(), port, request)
            if (response.success && response.authToken.isNotBlank()) {
                val newDevice = PairedDevice(
                    id = "dev_${System.currentTimeMillis()}",
                    name = response.deviceName.ifBlank { "Home Phone" },
                    ipAddress = targetIp.trim(),
                    port = port,
                    authToken = response.authToken,
                    status = ConnectionStatus.CONNECTED,
                    connectionType = ConnectionType.WIFI_LOCAL,
                    lastConnectedTime = System.currentTimeMillis()
                )
                secureStore.savePairedDevice(newDevice)
                refreshDevices()
                onComplete(true, null)
            } else {
                onComplete(false, response.errorMessage ?: "Pairing failed")
            }
        }
    }

    fun removeDevice(deviceId: String) {
        secureStore.removeDevice(deviceId)
        refreshDevices()
    }

    fun renameDevice(deviceId: String, newName: String) {
        secureStore.renameDevice(deviceId, newName)
        refreshDevices()
    }

    fun disconnectDevice(deviceId: String) {
        secureStore.updateDeviceStatus(deviceId, ConnectionStatus.DISCONNECTED)
        refreshDevices()
    }

    /**
     * Executes a remote voice command parsed from natural Hindi/Hinglish speech.
     */
    suspend fun executeRemoteVoiceCommand(
        cleanedCommand: String,
        preferredDeviceName: String? = null
    ): RemoteCommandResult = withContext(Dispatchers.IO) {
        val devices = secureStore.getPairedDevices()
        if (devices.isEmpty()) {
            return@withContext RemoteCommandResult(
                commandId = UUID.randomUUID().toString(),
                status = RemoteResultStatus.NOT_CONNECTED,
                message = "कोई भी दूसरा फोन MYRA Connect से पेयर नहीं है।"
            )
        }

        // Select targeted device or first available connected device
        val targetDevice = if (!preferredDeviceName.isNullOrBlank()) {
            devices.find { it.name.contains(preferredDeviceName, ignoreCase = true) } ?: devices.first()
        } else {
            devices.find { it.status == ConnectionStatus.CONNECTED } ?: devices.first()
        }

        val remoteAction = parseRemoteAction(cleanedCommand)
        val command = RemoteCommand(
            commandId = UUID.randomUUID().toString(),
            action = remoteAction.first,
            target = remoteAction.second,
            payload = remoteAction.third,
            authToken = targetDevice.authToken
        )

        Log.d(tag, "Sending remote command to ${targetDevice.name} (${targetDevice.ipAddress}): action=${command.action}, target=${command.target}")
        val result = transport.sendRemoteCommand(targetDevice, command)

        // Update device status based on response
        val newStatus = if (result.status == RemoteResultStatus.NOT_CONNECTED) ConnectionStatus.DISCONNECTED else ConnectionStatus.CONNECTED
        secureStore.updateDeviceStatus(targetDevice.id, newStatus)
        refreshDevices()

        result
    }

    /**
     * Parses clean Hindi/Hinglish command into RemoteCommandAction, target, and payload.
     */
    private fun parseRemoteAction(input: String): Triple<RemoteCommandAction, String, String> {
        val lower = input.lowercase()

        return when {
            lower.contains("youtube music") -> Triple(RemoteCommandAction.PLAY_MUSIC, "youtube_music", "")
            lower.contains("youtube") -> Triple(RemoteCommandAction.OPEN_APP, "youtube", "")
            lower.contains("chrome") || lower.contains("browser") -> Triple(RemoteCommandAction.OPEN_APP, "chrome", "")
            lower.contains("camera") || lower.contains("कैमरा") -> Triple(RemoteCommandAction.OPEN_APP, "camera", "")
            lower.contains("whatsapp") -> Triple(RemoteCommandAction.OPEN_APP, "whatsapp", "")
            lower.contains("spotify") || lower.contains("music") || lower.contains("gana") || lower.contains("गाना") -> Triple(RemoteCommandAction.PLAY_MUSIC, "music", "")
            lower.contains("torch") || lower.contains("flashlight") || lower.contains("टॉर्च") -> {
                val turnOff = lower.contains("band") || lower.contains("off") || lower.contains("बंद")
                Triple(RemoteCommandAction.TOGGLE_TORCH, "torch", if (turnOff) "off" else "on")
            }
            lower.contains("battery") || lower.contains("charge") || lower.contains("बैटरी") -> Triple(RemoteCommandAction.CHECK_BATTERY, "battery", "")
            lower.contains("volume") || lower.contains("awaaz") || lower.contains("आवाज़") -> {
                val level = if (lower.contains("kam") || lower.contains("low") || lower.contains("कम")) "30" else "80"
                Triple(RemoteCommandAction.ADJUST_VOLUME, "volume", level)
            }
            lower.contains("lock") || lower.contains("लॉक") -> Triple(RemoteCommandAction.LOCK_DEVICE, "lock", "")
            lower.contains("gallery") || lower.contains("photo") || lower.contains("फोटो") -> Triple(RemoteCommandAction.OPEN_APP, "gallery", "")
            lower.contains("calculator") || lower.contains("कैलकुलेटर") -> Triple(RemoteCommandAction.OPEN_APP, "calculator", "")
            lower.contains("settings") || lower.contains("सेटिंग्स") -> Triple(RemoteCommandAction.OPEN_APP, "settings", "")
            else -> {
                // Extract generic app name
                val appName = extractAppName(lower)
                Triple(RemoteCommandAction.OPEN_APP, appName, "")
            }
        }
    }

    private fun extractAppName(lower: String): String {
        return lower
            .replace("दूसरे फोन पर", "")
            .replace("घर वाले फोन पर", "")
            .replace("दूसरे मोबाइल पर", "")
            .replace("remote phone", "")
            .replace("second phone", "")
            .replace("other phone", "")
            .replace("par", "")
            .replace("per", "")
            .replace("pe", "")
            .replace("kholey", "")
            .replace("kholo", "")
            .replace("chalao", "")
            .replace("open", "")
            .replace("launch", "")
            .replace("खोलो", "")
            .replace("चलाओ", "")
            .replace("पर", "")
            .trim()
            .ifBlank { "youtube" }
    }
}
