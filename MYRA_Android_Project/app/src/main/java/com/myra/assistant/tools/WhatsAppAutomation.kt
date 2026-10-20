package com.myra.assistant.tools

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract
import android.util.Log
import androidx.core.content.ContextCompat
import java.net.URLEncoder

/**
 * Real WhatsApp Automation Manager.
 * Checks installation, resolves contact phone numbers, and triggers real WhatsApp Intents.
 */
class WhatsAppAutomation(private val context: Context) {

    private val tag = "MYRA_WhatsApp"

    fun handleWhatsAppCommand(target: String, messageText: String = ""): CommandResult {
        val pm = context.packageManager
        val isInstalled = isWhatsAppInstalled(pm)

        if (!isInstalled) {
            Log.w(tag, "MYRA_RESULT: NOT_INSTALLED, WhatsApp is not installed")
            return CommandResult.notInstalled(
                "WhatsApp इस फोन में installed नहीं है।",
                target = "whatsapp"
            )
        }

        val trimmedTarget = target.trim()

        // If target is empty or generic, simply open WhatsApp
        if (trimmedTarget.isEmpty() || trimmedTarget.equals("whatsapp", ignoreCase = true)) {
            val launchIntent = pm.getLaunchIntentForPackage("com.whatsapp") 
                ?: pm.getLaunchIntentForPackage("com.whatsapp.w4b")
            return if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launchIntent)
                Log.d(tag, "MYRA_RESULT: SUCCESS, opened WhatsApp app")
                CommandResult.success("WhatsApp खोल दिया।", target = "whatsapp")
            } else {
                CommandResult.failed("WhatsApp खोलने में असमर्थ।", target = "whatsapp")
            }
        }

        // Check if target is a direct phone number
        val digits = trimmedTarget.filter { it.isDigit() || it == '+' }
        if (digits.length >= 7) {
            return launchWhatsAppChat(digits, messageText, trimmedTarget)
        }

        // Try resolving contact name from device contacts
        val resolvedNumber = lookupContactNumber(trimmedTarget)
        if (resolvedNumber != null) {
            return launchWhatsAppChat(resolvedNumber, messageText, trimmedTarget)
        }

        // If contact name couldn't be resolved, open WhatsApp with search or main screen
        val launchIntent = pm.getLaunchIntentForPackage("com.whatsapp")
        return if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(launchIntent)
            Log.d(tag, "MYRA_RESULT: SUCCESS (partial), opened WhatsApp since contact not found")
            CommandResult.success(
                "WhatsApp में '$trimmedTarget' नहीं मिला, इसलिए WhatsApp खोल दिया है।",
                target = trimmedTarget
            )
        } else {
            CommandResult.failed("WhatsApp खोलने में समस्या आई।", target = trimmedTarget)
        }
    }

    private fun launchWhatsAppChat(phoneNumber: String, message: String, displayName: String): CommandResult {
        return try {
            val encodedMsg = if (message.isNotEmpty()) URLEncoder.encode(message, "UTF-8") else ""
            val cleanPhone = phoneNumber.replace(" ", "").replace("-", "")
            val url = "https://api.whatsapp.com/send?phone=$cleanPhone&text=$encodedMsg"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            Log.d(tag, "MYRA_RESULT: SUCCESS, launched WhatsApp chat with $displayName ($cleanPhone)")
            CommandResult.success(
                "$displayName के लिए WhatsApp चैट खोल दी गई है।",
                target = displayName,
                detail = cleanPhone
            )
        } catch (e: Exception) {
            Log.e(tag, "Failed to launch WhatsApp chat", e)
            CommandResult.failed("WhatsApp चैट खोलने में समस्या आई।", target = displayName, detail = e.message ?: "")
        }
    }

    private fun isWhatsAppInstalled(pm: PackageManager): Boolean {
        return try {
            pm.getPackageInfo("com.whatsapp", 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            try {
                pm.getPackageInfo("com.whatsapp.w4b", 0)
                true
            } catch (e2: PackageManager.NameNotFoundException) {
                false
            }
        }
    }

    private fun lookupContactNumber(nameQuery: String): String? {
        val hasPerm = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED
        if (!hasPerm) return null

        val resolver = context.contentResolver
        val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
        val projection = arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER)
        val selection = "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?"
        val selectionArgs = arrayOf("%$nameQuery%")

        return try {
            resolver.query(uri, projection, selection, selectionArgs, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                    if (idx >= 0) cursor.getString(idx) else null
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            null
        }
    }
}
