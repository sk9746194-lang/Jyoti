package com.myra.assistant.tools

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract
import android.provider.Settings
import android.util.Log
import androidx.core.content.ContextCompat
import com.myra.assistant.services.MyraAccessibilityService
import java.net.URLEncoder

/**
 * Real WhatsApp Automation Manager for MYRA ULTRA.
 * Verifies installation, resolves contact phone numbers, enforces recipient verification,
 * and coordinates with MyraAccessibilityService for hands-free message entry and execution.
 */
class WhatsAppAutomation(private val context: Context) {

    private val tag = "MYRA_WhatsApp"

    /**
     * Checks if WhatsApp (Standard or Business) is installed on device.
     */
    fun isInstalled(): Boolean {
        return isWhatsAppInstalled(context.packageManager)
    }

    /**
     * Checks if MYRA's Accessibility Service is currently active and granted by user.
     */
    fun isAccessibilityActive(): Boolean {
        return MyraAccessibilityService.isAccessibilityEnabled(context)
    }

    /**
     * Opens Android Accessibility Settings so the user can explicitly enable MYRA.
     */
    fun openAccessibilitySettings() {
        try {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(tag, "Could not open Accessibility Settings", e)
        }
    }

    /**
     * Main entry point for voice commands like "Rahul ko WhatsApp message bhejo hello".
     *
     * @param target Contact name or phone number
     * @param messageText Message payload to dispatch
     * @param autoSend Whether to automatically tap send if Accessibility is granted
     */
    fun handleWhatsAppCommand(
        target: String,
        messageText: String = "",
        autoSend: Boolean = false,
        onAsyncResult: ((CommandResult) -> Unit)? = null
    ): CommandResult {
        val pm = context.packageManager
        if (!isInstalled()) {
            Log.w(tag, "WhatsApp is not installed on device.")
            return CommandResult.notInstalled(
                "WhatsApp इस डिवाइस पर इंस्टॉल नहीं है।",
                target = "whatsapp"
            )
        }

        val trimmedTarget = target.trim()
        val trimmedMessage = messageText.trim()

        // 1. Generic open: "WhatsApp kholo"
        if (trimmedTarget.isEmpty() || trimmedTarget.equals("whatsapp", ignoreCase = true)) {
            val launchIntent = pm.getLaunchIntentForPackage("com.whatsapp")
                ?: pm.getLaunchIntentForPackage("com.whatsapp.w4b")
            return if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launchIntent)
                Log.d(tag, "Opened WhatsApp successfully.")
                CommandResult.success("WhatsApp खोल दिया गया है।", target = "whatsapp")
            } else {
                CommandResult.failed("WhatsApp खोलने में असमर्थ।", target = "whatsapp")
            }
        }

        // 2. Check if target is already a phone number
        val digitsOnly = trimmedTarget.filter { it.isDigit() || it == '+' }
        val resolvedPhone = if (digitsOnly.length >= 7) {
            digitsOnly
        } else {
            // Lookup contact in local address book
            lookupContactNumber(trimmedTarget)
        }

        val accessibilityEnabled = isAccessibilityActive()
        val accessibilityService = MyraAccessibilityService.instance

        // If Accessibility is active and messageText is provided, enqueue automation task
        if (accessibilityEnabled && accessibilityService != null && trimmedMessage.isNotEmpty()) {
            accessibilityService.enqueueWhatsAppTask(
                MyraAccessibilityService.AutomationRequest(
                    targetContact = trimmedTarget,
                    message = trimmedMessage,
                    autoSend = autoSend,
                    onComplete = { success, msg ->
                        Log.i(tag, "WhatsApp Automation completed: success=$success, msg='$msg'")
                        val res = if (success) {
                            CommandResult.success(msg, target = trimmedTarget)
                        } else {
                            CommandResult.failed(msg, target = trimmedTarget)
                        }
                        onAsyncResult?.invoke(res)
                    }
                )
            )
        }

        // 3. Dispatch chat via URL or Intent
        return if (resolvedPhone != null) {
            launchWhatsAppDirect(resolvedPhone, trimmedMessage, trimmedTarget, accessibilityEnabled)
        } else {
            launchWhatsAppSearch(trimmedTarget, trimmedMessage, accessibilityEnabled)
        }
    }

    private fun launchWhatsAppDirect(
        phoneNumber: String,
        message: String,
        displayName: String,
        accessibilityEnabled: Boolean
    ): CommandResult {
        return try {
            val encodedMsg = if (message.isNotEmpty()) URLEncoder.encode(message, "UTF-8") else ""
            var cleanPhone = phoneNumber.replace(" ", "").replace("-", "")

            // Ensure country code formatting if missing (default to +91 India for Hindi assistant)
            if (!cleanPhone.startsWith("+") && cleanPhone.length == 10) {
                cleanPhone = "91$cleanPhone"
            } else if (cleanPhone.startsWith("+")) {
                cleanPhone = cleanPhone.substring(1)
            }

            val url = "https://api.whatsapp.com/send?phone=$cleanPhone&text=$encodedMsg"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                setPackage(getPreferredWhatsAppPackage())
            }

            context.startActivity(intent)
            Log.d(tag, "Launched WhatsApp chat with $displayName ($cleanPhone)")

            val statusMsg = if (accessibilityEnabled) {
                "$displayName के लिए WhatsApp चैट खोली जा रही है और संदेश तैयार किया जा रहा है..."
            } else {
                "$displayName के लिए WhatsApp चैट खोल दी गई है। संदेश भेजने के लिए Send दबाएं। (ऑटो-सेंड के लिए Accessibility ऑन करें)"
            }

            CommandResult.success(statusMsg, target = displayName, detail = cleanPhone)
        } catch (e: Exception) {
            Log.e(tag, "Failed to launch WhatsApp direct chat", e)
            CommandResult.failed("WhatsApp चैट खोलने में समस्या आई: ${e.message ?: "त्रुटि"}", target = displayName)
        }
    }

    private fun launchWhatsAppSearch(
        contactQuery: String,
        message: String,
        accessibilityEnabled: Boolean
    ): CommandResult {
        val pm = context.packageManager
        val launchIntent = pm.getLaunchIntentForPackage("com.whatsapp")
            ?: pm.getLaunchIntentForPackage("com.whatsapp.w4b")

        return if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(launchIntent)
            Log.d(tag, "Opened WhatsApp main screen for contact query '$contactQuery'")

            val statusMsg = if (accessibilityEnabled) {
                "WhatsApp खोल दिया गया है। '$contactQuery' को खोजा जा रहा है..."
            } else {
                "संपर्क '$contactQuery' सीधे नहीं मिला, इसलिए WhatsApp खोल दिया है। संदेश भेजने के लिए संपर्क चुनें।"
            }

            CommandResult.success(statusMsg, target = contactQuery)
        } else {
            CommandResult.failed("WhatsApp खोलने में समस्या आई।", target = contactQuery)
        }
    }

    private fun getPreferredWhatsAppPackage(): String {
        val pm = context.packageManager
        return try {
            pm.getPackageInfo("com.whatsapp", 0)
            "com.whatsapp"
        } catch (e: Exception) {
            "com.whatsapp.w4b"
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
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.NUMBER,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME
        )
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
            Log.e(tag, "Error looking up contact '$nameQuery'", e)
            null
        }
    }
}
