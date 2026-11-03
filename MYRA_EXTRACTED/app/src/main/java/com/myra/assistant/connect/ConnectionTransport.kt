package com.myra.assistant.connect

import android.content.Context
import android.net.wifi.WifiManager
import android.os.Environment
import android.util.Base64
import android.util.Log
import com.google.gson.Gson
import kotlinx.coroutines.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.*
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * Native Android Networking Transport for MYRA Connect.
 * Hosts an embedded authenticated server for receiving commands, screen frames,
 * remote touch gestures, chunked file transfers, and pairing,
 * and an OkHttp client for transmitting commands across local Wi-Fi / Hotspot.
 */
class ConnectionTransport(
    private val context: Context,
    private val secureStore: SecureDeviceStore,
    private val pairingManager: DevicePairingManager,
    private val commandExecutor: RemoteCommandExecutor
) {
    private val tag = "MYRA_Transport"
    private val gson = Gson()
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .build()

    private var serverSocket: ServerSocket? = null
    private var serverJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    data class IncomingTransfer(
        val header: FileTransferHeader,
        val tempFile: File,
        val fos: FileOutputStream,
        var bytesWritten: Long = 0L
    )

    private val incomingTransfers = ConcurrentHashMap<String, IncomingTransfer>()

    // Callbacks
    var onDeviceStatusChanged: ((String, ConnectionStatus) -> Unit)? = null
    var onScreenFrameReceived: ((ScreenFramePacket) -> Unit)? = null
    var onRemoteGestureReceived: ((RemoteGesturePayload) -> Unit)? = null
    var onFileTransferProgress: ((FileTransferProgress) -> Unit)? = null
    var onFileTransferCompleted: ((String, String) -> Unit)? = null

    /**
     * Finds local Wi-Fi / Hotspot IPv4 address.
     */
    fun getLocalIpAddress(): String {
        try {
            // First check Wi-Fi Manager
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            val wifiIp = wifiManager?.connectionInfo?.ipAddress ?: 0
            if (wifiIp != 0) {
                return String.format(
                    "%d.%d.%d.%d",
                    wifiIp and 0xff,
                    wifiIp shr 8 and 0xff,
                    wifiIp shr 16 and 0xff,
                    wifiIp shr 24 and 0xff
                )
            }

            // Fallback: iterate network interfaces (covers Wi-Fi direct, hotspot, USB tethering)
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val intf = interfaces.nextElement()
                if (intf.isLoopback || !intf.isUp) continue
                val addresses = intf.inetAddresses
                while (addresses.hasMoreElements()) {
                    val addr = addresses.nextElement()
                    if (!addr.isLoopbackAddress && addr is Inet4Address) {
                        return addr.hostAddress ?: "127.0.0.1"
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "Failed to resolve local IP address", e)
        }
        return "127.0.0.1"
    }

    /**
     * Starts listening on local TCP ServerSocket.
     */
    fun startServer(port: Int = secureStore.serverPort) {
        if (serverJob?.isActive == true) return

        serverJob = scope.launch {
            try {
                serverSocket = ServerSocket(port)
                Log.d(tag, "MYRA Connect Server started on port $port (IP: ${getLocalIpAddress()})")

                while (isActive) {
                    val client = try {
                        serverSocket?.accept() ?: break
                    } catch (e: Exception) {
                        break
                    }
                    launch { handleClientSocket(client) }
                }
            } catch (e: Exception) {
                Log.e(tag, "Server error on port $port", e)
            } finally {
                stopServer()
            }
        }
    }

    fun stopServer() {
        try {
            serverSocket?.close()
        } catch (_: Exception) {}
        serverSocket = null
        serverJob?.cancel()
        serverJob = null
    }

    /**
     * Handles incoming connection (pairing, ping, commands, screen frames, gestures, file transfers).
     */
    private fun handleClientSocket(socket: Socket) {
        try {
            socket.use { s ->
                val reader = BufferedReader(InputStreamReader(s.getInputStream()))
                val writer = OutputStreamWriter(s.getOutputStream())

                val firstLine = reader.readLine() ?: return
                val parts = firstLine.split(" ")
                if (parts.size < 2) return

                val method = parts[0]
                val path = parts[1]

                // Read HTTP Headers to find Content-Length and Auth Token
                var contentLength = 0
                var authTokenHeader = ""
                var line: String? = reader.readLine()
                while (!line.isNullOrBlank()) {
                    if (line.startsWith("Content-Length:", ignoreCase = true)) {
                        contentLength = line.substringAfter(":").trim().toIntOrNull() ?: 0
                    } else if (line.startsWith("Authorization:", ignoreCase = true) || line.startsWith("X-Auth-Token:", ignoreCase = true)) {
                        authTokenHeader = line.substringAfter(":").trim().removePrefix("Bearer ").trim()
                    }
                    line = reader.readLine()
                }

                // Read Request Body
                val body = if (contentLength > 0) {
                    val buffer = CharArray(contentLength)
                    var readTotal = 0
                    while (readTotal < contentLength) {
                        val count = reader.read(buffer, readTotal, contentLength - readTotal)
                        if (count == -1) break
                        readTotal += count
                    }
                    String(buffer, 0, readTotal)
                } else ""

                // Route request
                val responseJson: String = when {
                    path == "/ping" -> {
                        gson.toJson(mapOf("status" to "pong", "device" to secureStore.thisDeviceName))
                    }
                    path == "/pair" && method == "POST" -> {
                        val req = gson.fromJson(body, PairingRequest::class.java)
                        val resp = pairingManager.validateIncomingPairing(req)
                        gson.toJson(resp)
                    }
                    path == "/command" && method == "POST" -> {
                        val cmd = gson.fromJson(body, RemoteCommand::class.java)
                        val knownDevice = secureStore.getDeviceByToken(cmd.authToken)
                        if (knownDevice == null) {
                            Log.w(tag, "Rejected unauthenticated command with token: ${cmd.authToken}")
                            val rejected = RemoteCommandResult(
                                commandId = cmd.commandId,
                                status = RemoteResultStatus.PERMISSION_REQUIRED,
                                message = "Unauthenticated: Device is not paired with this phone."
                            )
                            gson.toJson(rejected)
                        } else {
                            val result = commandExecutor.execute(cmd)
                            gson.toJson(result)
                        }
                    }
                    path == "/screen/frame" && method == "POST" -> {
                        try {
                            val packet = gson.fromJson(body, ScreenFramePacket::class.java)
                            onScreenFrameReceived?.invoke(packet)
                            gson.toJson(mapOf("status" to "ok", "frameIndex" to packet.frameIndex))
                        } catch (e: Exception) {
                            gson.toJson(mapOf("status" to "error", "message" to (e.message ?: "Invalid frame")))
                        }
                    }
                    path == "/gesture" && method == "POST" -> {
                        try {
                            val gesture = gson.fromJson(body, RemoteGesturePayload::class.java)
                            onRemoteGestureReceived?.invoke(gesture)
                            // Forward directly to AccessibilityService if running
                            val dm = context.resources.displayMetrics
                            val executed = when (gesture.type) {
                                "tap" -> {
                                    val pxX = (gesture.x * dm.widthPixels).coerceIn(0f, dm.widthPixels.toFloat())
                                    val pxY = (gesture.y * dm.heightPixels).coerceIn(0f, dm.heightPixels.toFloat())
                                    com.myra.assistant.services.MyraAccessibilityService.instance?.performTap(pxX, pxY) ?: false
                                }
                                "swipe", "scroll" -> {
                                    val pxX1 = (gesture.x * dm.widthPixels).coerceIn(0f, dm.widthPixels.toFloat())
                                    val pxY1 = (gesture.y * dm.heightPixels).coerceIn(0f, dm.heightPixels.toFloat())
                                    val pxX2 = (gesture.x2 * dm.widthPixels).coerceIn(0f, dm.widthPixels.toFloat())
                                    val pxY2 = (gesture.y2 * dm.heightPixels).coerceIn(0f, dm.heightPixels.toFloat())
                                    com.myra.assistant.services.MyraAccessibilityService.instance?.performSwipe(pxX1, pxY1, pxX2, pxY2, gesture.durationMs) ?: false
                                }
                                "back" -> com.myra.assistant.services.MyraAccessibilityService.instance?.performBackGesture() ?: false
                                "home" -> com.myra.assistant.services.MyraAccessibilityService.instance?.performHomeGesture() ?: false
                                else -> false
                            }
                            gson.toJson(mapOf("status" to "ok", "executed" to executed))
                        } catch (e: Exception) {
                            gson.toJson(mapOf("status" to "error", "message" to e.message))
                        }
                    }
                    path == "/file/init" && method == "POST" -> {
                        try {
                            val header = gson.fromJson(body, FileTransferHeader::class.java)
                            val cacheDir = File(context.cacheDir, "myra_transfers").apply { mkdirs() }
                            val tempFile = File(cacheDir, "transfer_${header.transferId}.tmp")
                            if (tempFile.exists()) tempFile.delete()
                            val fos = FileOutputStream(tempFile)
                            incomingTransfers[header.transferId] = IncomingTransfer(header, tempFile, fos)

                            onFileTransferProgress?.invoke(
                                FileTransferProgress(
                                    transferId = header.transferId,
                                    fileName = header.fileName,
                                    bytesTransferred = 0,
                                    totalBytes = header.fileSize,
                                    isIncoming = true,
                                    status = "transferring"
                                )
                            )
                            gson.toJson(mapOf("status" to "accepted", "transferId" to header.transferId))
                        } catch (e: Exception) {
                            gson.toJson(mapOf("status" to "error", "message" to e.message))
                        }
                    }
                    path == "/file/chunk" && method == "POST" -> {
                        try {
                            val chunk = gson.fromJson(body, FileChunkPayload::class.java)
                            val incoming = incomingTransfers[chunk.transferId]
                            if (incoming == null) {
                                gson.toJson(mapOf("status" to "error", "message" to "Transfer not initialized"))
                            } else {
                                val bytes = Base64.decode(chunk.dataBase64, Base64.NO_WRAP)
                                incoming.fos.write(bytes)
                                incoming.bytesWritten += bytes.size

                                onFileTransferProgress?.invoke(
                                    FileTransferProgress(
                                        transferId = chunk.transferId,
                                        fileName = incoming.header.fileName,
                                        bytesTransferred = incoming.bytesWritten,
                                        totalBytes = incoming.header.fileSize,
                                        isIncoming = true,
                                        status = "transferring"
                                    )
                                )
                                gson.toJson(mapOf("status" to "ack", "chunkIndex" to chunk.chunkIndex))
                            }
                        } catch (e: Exception) {
                            gson.toJson(mapOf("status" to "error", "message" to e.message))
                        }
                    }
                    path == "/file/finalize" && method == "POST" -> {
                        try {
                            val finalizeData = gson.fromJson(body, Map::class.java)
                            val transferId = finalizeData["transferId"] as? String ?: ""
                            val expectedSha256 = finalizeData["sha256"] as? String ?: ""

                            val incoming = incomingTransfers.remove(transferId)
                            if (incoming == null) {
                                gson.toJson(mapOf("status" to "error", "message" to "Transfer session expired"))
                            } else {
                                incoming.fos.flush()
                                incoming.fos.close()

                                val actualSha256 = FileInputStream(incoming.tempFile).use { fis ->
                                    CryptoChannel.calculateSha256(fis)
                                }

                                val verified = actualSha256.equals(expectedSha256, ignoreCase = true)
                                if (verified) {
                                    val destDir = File(
                                        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                                        "MYRA_Transfers"
                                    ).apply { mkdirs() }
                                    val safeFileName = incoming.header.fileName.replace("[^a-zA-Z0-9._-]".toRegex(), "_")
                                    val destFile = File(destDir, safeFileName)
                                    incoming.tempFile.copyTo(destFile, overwrite = true)
                                    incoming.tempFile.delete()

                                    onFileTransferProgress?.invoke(
                                        FileTransferProgress(
                                            transferId = transferId,
                                            fileName = incoming.header.fileName,
                                            bytesTransferred = incoming.bytesWritten,
                                            totalBytes = incoming.header.fileSize,
                                            isIncoming = true,
                                            status = "completed",
                                            checksumVerified = true
                                        )
                                    )
                                    onFileTransferCompleted?.invoke(transferId, destFile.absolutePath)

                                    gson.toJson(mapOf("status" to "completed", "verified" to true, "path" to destFile.absolutePath))
                                } else {
                                    incoming.tempFile.delete()
                                    onFileTransferProgress?.invoke(
                                        FileTransferProgress(
                                            transferId = transferId,
                                            fileName = incoming.header.fileName,
                                            bytesTransferred = incoming.bytesWritten,
                                            totalBytes = incoming.header.fileSize,
                                            isIncoming = true,
                                            status = "failed",
                                            checksumVerified = false,
                                            errorMessage = "SHA-256 Checksum mismatch"
                                        )
                                    )
                                    gson.toJson(mapOf("status" to "failed", "error" to "Checksum mismatch"))
                                }
                            }
                        } catch (e: Exception) {
                            gson.toJson(mapOf("status" to "error", "message" to e.message))
                        }
                    }
                    path == "/file/cancel" && method == "POST" -> {
                        try {
                            val cancelData = gson.fromJson(body, Map::class.java)
                            val transferId = cancelData["transferId"] as? String ?: ""
                            val incoming = incomingTransfers.remove(transferId)
                            incoming?.let {
                                try { it.fos.close() } catch (_: Exception) {}
                                try { it.tempFile.delete() } catch (_: Exception) {}
                            }
                            gson.toJson(mapOf("status" to "cancelled"))
                        } catch (e: Exception) {
                            gson.toJson(mapOf("status" to "error", "message" to e.message))
                        }
                    }
                    else -> {
                        gson.toJson(mapOf("error" to "NotFound"))
                    }
                }

                // Send HTTP 200 Response
                val bytes = responseJson.toByteArray(Charsets.UTF_8)
                writer.write("HTTP/1.1 200 OK\r\n")
                writer.write("Content-Type: application/json; charset=utf-8\r\n")
                writer.write("Content-Length: ${bytes.size}\r\n")
                writer.write("Connection: close\r\n\r\n")
                writer.flush()
                s.getOutputStream().write(bytes)
                s.getOutputStream().flush()
            }
        } catch (e: Exception) {
            Log.e(tag, "Error handling client connection", e)
        }
    }

    // --- Outgoing Client Requests ---

    suspend fun sendPairingRequest(targetIp: String, port: Int, request: PairingRequest): PairingResponse = withContext(Dispatchers.IO) {
        val url = "http://$targetIp:$port/pair"
        val json = gson.toJson(request)
        val body = json.toRequestBody(jsonMediaType)
        val httpRequest = Request.Builder().url(url).post(body).build()

        try {
            httpClient.newCall(httpRequest).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext PairingResponse(success = false, errorMessage = "HTTP error ${response.code}")
                }
                val respBody = response.body?.string() ?: ""
                gson.fromJson(respBody, PairingResponse::class.java)
            }
        } catch (e: Exception) {
            Log.e(tag, "Failed to connect to $url for pairing", e)
            PairingResponse(success = false, errorMessage = "Connection failed: ${e.localizedMessage}")
        }
    }

    suspend fun sendRemoteCommand(device: PairedDevice, command: RemoteCommand): RemoteCommandResult = withContext(Dispatchers.IO) {
        val url = "http://${device.ipAddress}:${device.port}/command"
        val json = gson.toJson(command)
        val body = json.toRequestBody(jsonMediaType)
        val httpRequest = Request.Builder().url(url).post(body).build()

        try {
            httpClient.newCall(httpRequest).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext RemoteCommandResult(
                        commandId = command.commandId,
                        status = RemoteResultStatus.FAILED,
                        message = "Remote phone responded with HTTP ${response.code}"
                    )
                }
                val respBody = response.body?.string() ?: ""
                gson.fromJson(respBody, RemoteCommandResult::class.java)
            }
        } catch (e: Exception) {
            Log.e(tag, "Failed to send command to ${device.name} ($url)", e)
            RemoteCommandResult(
                commandId = command.commandId,
                status = RemoteResultStatus.NOT_CONNECTED,
                message = "${device.name} से संपर्क नहीं हो पाया।"
            )
        }
    }

    suspend fun sendScreenFrame(device: PairedDevice, packet: ScreenFramePacket): Boolean = withContext(Dispatchers.IO) {
        val url = "http://${device.ipAddress}:${device.port}/screen/frame"
        val json = gson.toJson(packet)
        val body = json.toRequestBody(jsonMediaType)
        val httpRequest = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer ${device.authToken}")
            .post(body)
            .build()

        try {
            httpClient.newCall(httpRequest).execute().use { it.isSuccessful }
        } catch (_: Exception) {
            false
        }
    }

    suspend fun sendRemoteGesture(device: PairedDevice, gesture: RemoteGesturePayload): Boolean = withContext(Dispatchers.IO) {
        val url = "http://${device.ipAddress}:${device.port}/gesture"
        val json = gson.toJson(gesture)
        val body = json.toRequestBody(jsonMediaType)
        val httpRequest = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer ${device.authToken}")
            .post(body)
            .build()

        try {
            httpClient.newCall(httpRequest).execute().use { it.isSuccessful }
        } catch (_: Exception) {
            false
        }
    }

    suspend fun initiateFileTransfer(device: PairedDevice, header: FileTransferHeader): Boolean = withContext(Dispatchers.IO) {
        val url = "http://${device.ipAddress}:${device.port}/file/init"
        val json = gson.toJson(header)
        val body = json.toRequestBody(jsonMediaType)
        val httpRequest = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer ${device.authToken}")
            .post(body)
            .build()

        try {
            httpClient.newCall(httpRequest).execute().use { it.isSuccessful }
        } catch (_: Exception) {
            false
        }
    }

    suspend fun sendFileChunk(device: PairedDevice, chunk: FileChunkPayload): Boolean = withContext(Dispatchers.IO) {
        val url = "http://${device.ipAddress}:${device.port}/file/chunk"
        val json = gson.toJson(chunk)
        val body = json.toRequestBody(jsonMediaType)
        val httpRequest = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer ${device.authToken}")
            .post(body)
            .build()

        try {
            httpClient.newCall(httpRequest).execute().use { it.isSuccessful }
        } catch (_: Exception) {
            false
        }
    }

    suspend fun finalizeFileTransfer(device: PairedDevice, transferId: String, sha256: String): Boolean = withContext(Dispatchers.IO) {
        val url = "http://${device.ipAddress}:${device.port}/file/finalize"
        val json = gson.toJson(mapOf("transferId" to transferId, "sha256" to sha256))
        val body = json.toRequestBody(jsonMediaType)
        val httpRequest = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer ${device.authToken}")
            .post(body)
            .build()

        try {
            httpClient.newCall(httpRequest).execute().use { it.isSuccessful }
        } catch (_: Exception) {
            false
        }
    }

    suspend fun cancelFileTransfer(device: PairedDevice, transferId: String): Boolean = withContext(Dispatchers.IO) {
        val url = "http://${device.ipAddress}:${device.port}/file/cancel"
        val json = gson.toJson(mapOf("transferId" to transferId))
        val body = json.toRequestBody(jsonMediaType)
        val httpRequest = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer ${device.authToken}")
            .post(body)
            .build()

        try {
            httpClient.newCall(httpRequest).execute().use { it.isSuccessful }
        } catch (_: Exception) {
            false
        }
    }

    suspend fun pingDevice(device: PairedDevice): Boolean = withContext(Dispatchers.IO) {
        val url = "http://${device.ipAddress}:${device.port}/ping"
        val httpRequest = Request.Builder().url(url).get().build()

        try {
            httpClient.newCall(httpRequest).execute().use { response ->
                response.isSuccessful
            }
        } catch (e: Exception) {
            false
        }
    }
}
