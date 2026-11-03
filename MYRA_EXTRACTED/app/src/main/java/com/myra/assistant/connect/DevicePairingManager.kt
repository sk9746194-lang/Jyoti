package com.myra.assistant.connect

import android.content.Context
import android.net.Uri
import android.util.Log
import java.security.SecureRandom
import java.util.UUID

/**
 * Manages mutual authentication and pairing handshake between two Android devices.
 * Requires explicit user confirmation on both phones.
 */
class DevicePairingManager(
    private val context: Context,
    private val secureStore: SecureDeviceStore
) {
    private val tag = "MYRA_Pairing"
    private val secureRandom = SecureRandom()

    // Current active 6-digit code for this device in "Pairing Receiver" mode
    var activePairingCode: String? = null
        private set
    var activePairingSecret: String? = null
        private set

    /**
     * Prepares this phone to receive a pairing request.
     * Returns a 6-digit PIN and a companion QR payload.
     */
    fun startHostingPairing(localIp: String): PairingHostInfo {
        val code = String.format("%06d", 100000 + secureRandom.nextInt(900000))
        val secret = UUID.randomUUID().toString()
        activePairingCode = code
        activePairingSecret = secret

        val qrData = "myra-connect://pair?" +
                "id=${Uri.encode(secureStore.thisDeviceId)}" +
                "&name=${Uri.encode(secureStore.thisDeviceName)}" +
                "&ip=${Uri.encode(localIp)}" +
                "&port=${secureStore.serverPort}" +
                "&code=$code" +
                "&secret=$secret"

        Log.d(tag, "Hosting pairing initiated. Code: $code, IP: $localIp")
        return PairingHostInfo(
            pairingCode = code,
            qrPayload = qrData,
            deviceId = secureStore.thisDeviceId,
            deviceName = secureStore.thisDeviceName,
            ipAddress = localIp,
            port = secureStore.serverPort
        )
    }

    /**
     * Validates incoming pairing request against active hosted code.
     */
    fun validateIncomingPairing(request: PairingRequest): PairingResponse {
        val currentCode = activePairingCode
        val currentSecret = activePairingSecret

        if (currentCode == null || currentSecret == null) {
            return PairingResponse(
                success = false,
                errorMessage = "This device is not in pairing mode."
            )
        }

        if (request.pairingCode != currentCode) {
            Log.w(tag, "Pairing code mismatch: expected $currentCode, got ${request.pairingCode}")
            return PairingResponse(
                success = false,
                errorMessage = "Invalid 6-digit pairing code."
            )
        }

        // Generate strong session authentication token
        val authToken = "myra_auth_" + UUID.randomUUID().toString().replace("-", "")

        // Register paired device
        val newDevice = PairedDevice(
            id = request.deviceId,
            name = request.deviceName,
            ipAddress = request.ipAddress,
            port = request.port,
            authToken = authToken,
            status = ConnectionStatus.CONNECTED,
            connectionType = ConnectionType.WIFI_LOCAL,
            lastConnectedTime = System.currentTimeMillis()
        )
        secureStore.savePairedDevice(newDevice)

        // Clear one-time pairing code
        activePairingCode = null
        activePairingSecret = null

        Log.d(tag, "Device successfully paired: ${request.deviceName} (${request.deviceId})")

        return PairingResponse(
            success = true,
            authToken = authToken,
            deviceName = secureStore.thisDeviceName
        )
    }

    /**
     * Parses a QR code string scanned from another phone.
     */
    fun parseQrPayload(rawQr: String): PairingRequest? {
        return try {
            val uri = Uri.parse(rawQr)
            if (uri.scheme != "myra-connect" || uri.host != "pair") return null

            val id = uri.getQueryParameter("id") ?: return null
            val name = uri.getQueryParameter("name") ?: "Remote Phone"
            val ip = uri.getQueryParameter("ip") ?: return null
            val port = uri.getQueryParameter("port")?.toIntOrNull() ?: 9876
            val code = uri.getQueryParameter("code") ?: return null
            val secret = uri.getQueryParameter("secret") ?: ""

            PairingRequest(
                deviceId = id,
                deviceName = name,
                ipAddress = ip,
                port = port,
                pairingCode = code,
                sharedSecret = secret
            )
        } catch (e: Exception) {
            Log.e(tag, "Failed to parse QR payload: $rawQr", e)
            null
        }
    }

    fun stopHostingPairing() {
        activePairingCode = null
        activePairingSecret = null
    }
}

data class PairingHostInfo(
    val pairingCode: String,
    val qrPayload: String,
    val deviceId: String,
    val deviceName: String,
    val ipAddress: String,
    val port: Int
)
