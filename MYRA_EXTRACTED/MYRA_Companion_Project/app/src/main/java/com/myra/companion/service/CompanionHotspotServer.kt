package com.myra.companion.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.RingtoneManager
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.os.Build
import android.os.Environment
import android.os.IBinder
import android.util.Base64
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.gson.Gson
import com.myra.companion.CompanionCaptureConsentActivity
import kotlinx.coroutines.*
import java.io.*
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import java.security.MessageDigest
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Foreground Service hosting the secure HTTP socket server & mDNS broadcaster on the Companion phone.
 */
class CompanionHotspotServerService : Service() {

    private val tag = "MYRA_CompServer"
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val gson = Gson()

    private var serverSocket: ServerSocket? = null
    private var serverJob: Job? = null
    private var nsdManager: NsdManager? = null
    private var nsdRegistrationListener: NsdManager.RegistrationListener? = null

    // In-memory store for authorized pairing
    companion object {
        var hostPairingPin: String = "492815"
        var authorizedAuthToken: String = ""
        var authorizedPrimaryDeviceName: String = "Primary MYRA Phone"

        const val CHANNEL_ID = "myra_companion_server_channel"
        const val NOTIFICATION_ID = 9103
    }

    data class IncomingTransfer(
        val transferId: String,
        val fileName: String,
        val fileSize: Long,
        val expectedSha256: String,
        val tempFile: File,
        val fos: FileOutputStream,
        var bytesWritten: Long = 0L
    )
    private val incomingTransfers = ConcurrentHashMap<String, IncomingTransfer>()

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForegroundService()
        startHotspotServer(9876)
        registerNsd(9876)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "MYRA Companion Server",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Runs the background connection server for phone interconnect"
            }
            getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
        }
    }

    private fun startForegroundService() {
        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("MYRA Companion Ready")
            .setContentText("Listening on Hotspot port 9876 • PIN: $hostPairingPin")
            .setSmallIcon(android.R.drawable.stat_sys_data_bluetooth)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    fun getLocalIpAddress(): String {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val intf = interfaces.nextElement()
                if (intf.isLoopback || !intf.isUp) continue
                val addrs = intf.inetAddresses
                while (addrs.hasMoreElements()) {
                    val addr = addrs.nextElement()
                    if (!addr.isLoopbackAddress && addr is Inet4Address) {
                        return addr.hostAddress ?: "127.0.0.1"
                    }
                }
            }
        } catch (_: Exception) {}
        return "127.0.0.1"
    }

    private fun registerNsd(port: Int) {
        nsdManager = getSystemService(Context.NSD_SERVICE) as? NsdManager
        val serviceInfo = NsdServiceInfo().apply {
            serviceName = "MYRA-Companion-${Build.MODEL.replace(" ", "-")}"
            serviceType = "_myraconnect._tcp."
            setPort(port)
        }
        nsdRegistrationListener = object : NsdManager.RegistrationListener {
            override fun onServiceRegistered(serviceInfo: NsdServiceInfo?) {
                Log.d(tag, "NSD registered: ${serviceInfo?.serviceName}")
            }
            override fun onRegistrationFailed(serviceInfo: NsdServiceInfo?, errorCode: Int) {}
            override fun onServiceUnregistered(serviceInfo: NsdServiceInfo?) {}
            override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo?, errorCode: Int) {}
        }
        try {
            nsdManager?.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, nsdRegistrationListener)
        } catch (e: Exception) {
            Log.e(tag, "NSD register failed", e)
        }
    }

    private fun startHotspotServer(port: Int) {
        serverJob = scope.launch {
            try {
                serverSocket = ServerSocket(port)
                Log.d(tag, "Companion Server listening on port $port")
                while (isActive) {
                    val client = try { serverSocket?.accept() ?: break } catch (_: Exception) { break }
                    launch { handleSocket(client) }
                }
            } catch (e: Exception) {
                Log.e(tag, "Companion ServerSocket error", e)
            }
        }
    }

    private fun handleSocket(socket: Socket) {
        try {
            socket.use { s ->
                val reader = BufferedReader(InputStreamReader(s.getInputStream()))
                val writer = OutputStreamWriter(s.getOutputStream())

                val firstLine = reader.readLine() ?: return
                val parts = firstLine.split(" ")
                if (parts.size < 2) return
                val method = parts[0]
                val path = parts[1]

                var contentLength = 0
                var authTokenHeader = ""
                var line: String? = reader.readLine()
                while (!line.isNullOrBlank()) {
                    if (line.startsWith("Content-Length:", ignoreCase = true)) {
                        contentLength = line.substringAfter(":").trim().toIntOrNull() ?: 0
                    } else if (line.startsWith("Authorization:", ignoreCase = true)) {
                        authTokenHeader = line.substringAfter(":").trim().removePrefix("Bearer ").trim()
                    }
                    line = reader.readLine()
                }

                val body = if (contentLength > 0) {
                    val buf = CharArray(contentLength)
                    var readTotal = 0
                    while (readTotal < contentLength) {
                        val c = reader.read(buf, readTotal, contentLength - readTotal)
                        if (c == -1) break
                        readTotal += c
                    }
                    String(buf, 0, readTotal)
                } else ""

                val responseJson: String = when {
                    path == "/ping" -> {
                        gson.toJson(mapOf("status" to "pong", "device" to "MYRA Companion (${Build.MODEL})"))
                    }
                    path == "/pair" && method == "POST" -> {
                        val req = gson.fromJson(body, Map::class.java)
                        val code = req["pairingCode"] as? String ?: ""
                        if (code == hostPairingPin) {
                            val token = UUID.randomUUID().toString()
                            authorizedAuthToken = token
                            authorizedPrimaryDeviceName = req["deviceName"] as? String ?: "Primary Phone"
                            gson.toJson(mapOf("success" to true, "authToken" to token, "deviceName" to "MYRA Companion (${Build.MODEL})"))
                        } else {
                            gson.toJson(mapOf("success" to false, "errorMessage" to "PIN Mismatch"))
                        }
                    }
                    path == "/command" && method == "POST" -> {
                        val cmd = gson.fromJson(body, Map::class.java)
                        val action = cmd["action"] as? String ?: ""
                        val target = cmd["target"] as? String ?: ""
                        val token = cmd["authToken"] as? String ?: authTokenHeader

                        if (authorizedAuthToken.isNotBlank() && token != authorizedAuthToken) {
                            gson.toJson(mapOf("status" to "PERMISSION_REQUIRED", "message" to "Unauthenticated"))
                        } else {
                            when (action) {
                                "START_SCREEN_STREAM" -> {
                                    val callerIp = s.inetAddress.hostAddress ?: "127.0.0.1"
                                    CompanionCaptureConsentActivity.launchConsent(this@CompanionHotspotServerService, callerIp, 9876, authorizedAuthToken)
                                    gson.toJson(mapOf("status" to "SUCCESS", "message" to "Screen capture initiated"))
                                }
                                "STOP_SCREEN_STREAM" -> {
                                    CompanionScreenCaptureService.stop(this@CompanionHotspotServerService)
                                    gson.toJson(mapOf("status" to "SUCCESS", "message" to "Screen capture stopped"))
                                }
                                "RING_DEVICE" -> {
                                    val ringUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                                        ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                                    RingtoneManager.getRingtone(this@CompanionHotspotServerService, ringUri)?.play()
                                    gson.toJson(mapOf("status" to "SUCCESS", "message" to "Ringer ringing"))
                                }
                                "LOCK_DEVICE" -> {
                                    val ok = CompanionAccessibilityService.instance?.performLockGesture() ?: false
                                    gson.toJson(mapOf("status" to if (ok) "SUCCESS" else "FAILED", "message" to "Screen locked"))
                                }
                                else -> {
                                    gson.toJson(mapOf("status" to "SUCCESS", "message" to "Action $action received"))
                                }
                            }
                        }
                    }
                    path == "/gesture" && method == "POST" -> {
                        val gesture = gson.fromJson(body, Map::class.java)
                        val type = gesture["type"] as? String ?: "tap"
                        val dm = resources.displayMetrics
                        val executed = when (type) {
                            "tap" -> {
                                val x = ((gesture["x"] as? Number)?.toFloat() ?: 0f) * dm.widthPixels
                                val y = ((gesture["y"] as? Number)?.toFloat() ?: 0f) * dm.heightPixels
                                CompanionAccessibilityService.instance?.performTap(x, y) ?: false
                            }
                            "swipe", "scroll" -> {
                                val x1 = ((gesture["x"] as? Number)?.toFloat() ?: 0f) * dm.widthPixels
                                val y1 = ((gesture["y"] as? Number)?.toFloat() ?: 0f) * dm.heightPixels
                                val x2 = ((gesture["x2"] as? Number)?.toFloat() ?: 0f) * dm.widthPixels
                                val y2 = ((gesture["y2"] as? Number)?.toFloat() ?: 0f) * dm.heightPixels
                                val dur = (gesture["durationMs"] as? Number)?.toLong() ?: 200L
                                CompanionAccessibilityService.instance?.performSwipe(x1, y1, x2, y2, dur) ?: false
                            }
                            "back" -> CompanionAccessibilityService.instance?.performBackGesture() ?: false
                            "home" -> CompanionAccessibilityService.instance?.performHomeGesture() ?: false
                            else -> false
                        }
                        gson.toJson(mapOf("status" to "ok", "executed" to executed))
                    }
                    path == "/file/init" && method == "POST" -> {
                        val header = gson.fromJson(body, Map::class.java)
                        val transferId = header["transferId"] as? String ?: UUID.randomUUID().toString()
                        val fileName = header["fileName"] as? String ?: "file.bin"
                        val fileSize = (header["fileSize"] as? Number)?.toLong() ?: 0L
                        val sha256 = header["sha256"] as? String ?: ""

                        val cacheDir = File(cacheDir, "myra_companion_transfers").apply { mkdirs() }
                        val tempFile = File(cacheDir, "transfer_$transferId.tmp")
                        if (tempFile.exists()) tempFile.delete()
                        val fos = FileOutputStream(tempFile)
                        incomingTransfers[transferId] = IncomingTransfer(transferId, fileName, fileSize, sha256, tempFile, fos)
                        gson.toJson(mapOf("status" to "accepted", "transferId" to transferId))
                    }
                    path == "/file/chunk" && method == "POST" -> {
                        val chunk = gson.fromJson(body, Map::class.java)
                        val transferId = chunk["transferId"] as? String ?: ""
                        val dataB64 = chunk["dataBase64"] as? String ?: ""
                        val incoming = incomingTransfers[transferId]
                        if (incoming != null) {
                            val bytes = Base64.decode(dataB64, Base64.NO_WRAP)
                            incoming.fos.write(bytes)
                            incoming.bytesWritten += bytes.size
                            gson.toJson(mapOf("status" to "ack"))
                        } else {
                            gson.toJson(mapOf("status" to "error", "message" to "Transfer not found"))
                        }
                    }
                    path == "/file/finalize" && method == "POST" -> {
                        val fin = gson.fromJson(body, Map::class.java)
                        val transferId = fin["transferId"] as? String ?: ""
                        val incoming = incomingTransfers.remove(transferId)
                        if (incoming != null) {
                            incoming.fos.flush()
                            incoming.fos.close()

                            val md = MessageDigest.getInstance("SHA-256")
                            FileInputStream(incoming.tempFile).use { fis ->
                                val buf = ByteArray(8192)
                                var read: Int
                                while (fis.read(buf).also { read = it } != -1) {
                                    md.update(buf, 0, read)
                                }
                            }
                            val actualHash = md.digest().joinToString("") { "%02x".format(it) }

                            if (actualHash.equals(incoming.expectedSha256, ignoreCase = true)) {
                                val destDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "MYRA_Transfers").apply { mkdirs() }
                                val destFile = File(destDir, incoming.fileName.replace("[^a-zA-Z0-9._-]".toRegex(), "_"))
                                incoming.tempFile.copyTo(destFile, overwrite = true)
                                incoming.tempFile.delete()
                                gson.toJson(mapOf("status" to "completed", "verified" to true, "path" to destFile.absolutePath))
                            } else {
                                incoming.tempFile.delete()
                                gson.toJson(mapOf("status" to "failed", "error" to "SHA-256 Mismatch"))
                            }
                        } else {
                            gson.toJson(mapOf("status" to "error", "message" to "Session expired"))
                        }
                    }
                    else -> gson.toJson(mapOf("error" to "NotFound"))
                }

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
            Log.e(tag, "Client socket handling error", e)
        }
    }

    override fun onDestroy() {
        try { serverSocket?.close() } catch (_: Exception) {}
        try { nsdManager?.unregisterService(nsdRegistrationListener) } catch (_: Exception) {}
        serverJob?.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
