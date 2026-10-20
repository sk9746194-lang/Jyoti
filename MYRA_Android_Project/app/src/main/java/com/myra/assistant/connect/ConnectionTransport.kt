package com.myra.assistant.connect

import android.content.Context
import android.net.wifi.WifiManager
import android.util.Log
import com.google.gson.Gson
import kotlinx.coroutines.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.TimeUnit

/**
 * Native Android Networking Transport for MYRA Connect.
 * Hosts an embedded authenticated server for receiving commands and pairing,
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
        .readTimeout(8, TimeUnit.SECONDS)
        .writeTimeout(5, TimeUnit.SECONDS)
        .build()

    private var serverSocket: ServerSocket? = null
    private var serverJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // Callbacks
    var onDeviceStatusChanged: ((String, ConnectionStatus) -> Unit)? = null

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
     * Handles incoming connection (pairing, ping, or remote command execution).
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

                // Read HTTP Headers to find Content-Length
                var contentLength = 0
                var line: String? = reader.readLine()
                while (!line.isNullOrBlank()) {
                    if (line.startsWith("Content-Length:", ignoreCase = true)) {
                        contentLength = line.substringAfter(":").trim().toIntOrNull() ?: 0
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
                        // Security check: Verify caller token
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
