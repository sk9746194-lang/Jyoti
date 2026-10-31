package com.myra.assistant.connect

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.UUID

/**
 * Secure persistent store for MYRA Connect paired devices, authentication tokens,
 * and recipient permission preferences.
 */
class SecureDeviceStore(context: Context) {

    private val appContext = context.applicationContext
    private val prefs: SharedPreferences = appContext.getSharedPreferences("myra_connect_secure", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val encryptedPrefs: SharedPreferences? by lazy {
        try {
            val masterKey = MasterKey.Builder(appContext)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            EncryptedSharedPreferences.create(
                appContext,
                "myra_connect_credentials",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (error: Exception) {
            Log.e("MYRA_ConnectSecurity", "Encrypted pairing storage unavailable; paired credentials are disabled.", error)
            null
        }
    }

    init {
        val legacy = prefs.getString("paired_devices_list", null)
        if (!legacy.isNullOrBlank()) {
            val migrated = encryptedPrefs?.edit()?.putString("paired_devices_list", legacy)?.commit() == true
            // Never retain pairing tokens in plaintext preferences, even if migration fails.
            prefs.edit().remove("paired_devices_list").apply()
            if (!migrated) {
                Log.e("MYRA_ConnectSecurity", "Legacy paired-device credentials could not be encrypted and were discarded. Re-pair devices after secure storage is available.")
            }
        }
    }

    val isCredentialStorageAvailable: Boolean
        get() = encryptedPrefs != null

    // Unique Persistent ID for this phone
    val thisDeviceId: String
        get() {
            var id = prefs.getString("this_device_id", null)
            if (id == null) {
                id = UUID.randomUUID().toString()
                prefs.edit().putString("this_device_id", id).apply()
            }
            return id
        }

    // Friendly Device Name (e.g. "Primary Phone", "Home Phone")
    var thisDeviceName: String
        get() {
            return prefs.getString("this_device_name", null) ?: "${Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${Build.MODEL}"
        }
        set(value) {
            prefs.edit().putString("this_device_name", value).apply()
        }

    // Local TCP Server Port
    var serverPort: Int
        get() = prefs.getInt("server_port", 9876)
        set(value) = prefs.edit().putInt("server_port", value).apply()

    // --- Paired Devices Management ---

    fun getPairedDevices(): List<PairedDevice> {
        val json = encryptedPrefs?.getString("paired_devices_list", null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<PairedDevice>>() {}.type
            gson.fromJson<List<PairedDevice>>(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun savePairedDevice(device: PairedDevice): Boolean {
        val current = getPairedDevices().toMutableList()
        val index = current.indexOfFirst { it.id == device.id }
        if (index >= 0) {
            current[index] = device
        } else {
            current.add(device)
        }
        return saveDevicesList(current)
    }

    fun getDevice(id: String): PairedDevice? {
        return getPairedDevices().find { it.id == id }
    }

    fun getDeviceByToken(token: String): PairedDevice? {
        if (token.isBlank()) return null
        return getPairedDevices().find { it.authToken == token }
    }

    fun renameDevice(id: String, newName: String) {
        val current = getPairedDevices().map {
            if (it.id == id) it.copy(name = newName) else it
        }
        saveDevicesList(current)
    }

    fun updateDeviceStatus(id: String, status: ConnectionStatus) {
        val current = getPairedDevices().map {
            if (it.id == id) it.copy(status = status, lastConnectedTime = System.currentTimeMillis()) else it
        }
        saveDevicesList(current)
    }

    fun removeDevice(id: String) {
        // Complete credential invalidation
        val current = getPairedDevices().filter { it.id != id }
        saveDevicesList(current)
    }

    private fun saveDevicesList(list: List<PairedDevice>): Boolean {
        val json = gson.toJson(list)
        return encryptedPrefs?.edit()?.putString("paired_devices_list", json)?.commit() == true
    }

    // --- Recipient Device Permissions ---

    fun getDevicePermissions(): DevicePermissions {
        val json = prefs.getString("device_permissions", null) ?: return DevicePermissions()
        return try {
            gson.fromJson(json, DevicePermissions::class.java) ?: DevicePermissions()
        } catch (e: Exception) {
            DevicePermissions()
        }
    }

    fun saveDevicePermissions(permissions: DevicePermissions) {
        val json = gson.toJson(permissions)
        prefs.edit().putString("device_permissions", json).apply()
    }
}
