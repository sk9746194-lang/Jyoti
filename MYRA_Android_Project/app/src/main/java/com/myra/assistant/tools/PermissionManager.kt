package com.myra.assistant.tools

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

class PermissionManager(private val context: Context) {

    val requiredPermissions = mutableListOf(
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.INTERNET,
        Manifest.permission.CALL_PHONE,
        Manifest.permission.READ_CONTACTS,
        Manifest.permission.SEND_SMS,
        Manifest.permission.CAMERA
    ).apply {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.POST_NOTIFICATIONS)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            add(Manifest.permission.BLUETOOTH_CONNECT)
        }
    }

    fun hasRecordAudio(): Boolean = isGranted(Manifest.permission.RECORD_AUDIO)
    fun hasCamera(): Boolean = isGranted(Manifest.permission.CAMERA)
    fun hasCallPhone(): Boolean = isGranted(Manifest.permission.CALL_PHONE)
    fun hasContacts(): Boolean = isGranted(Manifest.permission.READ_CONTACTS)

    fun checkMissingPermissions(): List<String> {
        return requiredPermissions.filter { !isGranted(it) }
    }

    private fun isGranted(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }
}
