package com.myra.assistant.tools

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract
import android.util.Log
import androidx.core.content.ContextCompat

/**
 * Real Android Telephony & Contact Resolution Manager.
 * Checks permissions, resolves real contact numbers, and issues actual Call/Dial Intents.
 */
class PhoneCallManager(private val context: Context) {

    private val tag = "MYRA_PhoneCall"

    /**
     * Resolves target (contact name or number), checks permissions, and initiates real call/dial.
     */
    fun handleCallCommand(target: String): CommandResult {
        val query = target.trim()
        Log.d(tag, "MYRA_ACTION: CALL_PHONE target='$query'")

        if (query.isEmpty()) {
            return openDialer()
        }

        // If target contains mostly digits, treat as direct phone number
        val digitsOnly = query.filter { it.isDigit() || it == '+' }
        if (digitsOnly.length >= 5 && digitsOnly.length > query.length / 2) {
            return initiateCallToNumber(digitsOnly, "नंबर $digitsOnly")
        }

        // Otherwise resolve contact name from Android Contacts
        val hasContactsPerm = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasContactsPerm) {
            Log.w(tag, "READ_CONTACTS permission not granted")
            return CommandResult.permissionRequired(
                "कॉन्टैक्ट खोजने के लिए Contacts अनुमति (Permission) की आवश्यकता है।",
                target = query
            )
        }

        val resolved = lookupContactNumber(query)
        if (resolved == null) {
            Log.w(tag, "Contact not found for query: $query")
            return CommandResult.notFound(
                "फोन में '$query' नाम का कोई कॉन्टैक्ट नहीं मिला।",
                target = query
            )
        }

        return initiateCallToNumber(resolved.second, resolved.first)
    }

    private fun initiateCallToNumber(number: String, displayName: String): CommandResult {
        val hasCallPerm = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CALL_PHONE
        ) == PackageManager.PERMISSION_GRANTED

        return if (hasCallPerm) {
            try {
                val callIntent = Intent(Intent.ACTION_CALL).apply {
                    data = Uri.parse("tel:$number")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(callIntent)
                Log.d(tag, "MYRA_RESULT: SUCCESS, ACTION_CALL to $number")
                CommandResult.success(
                    "$displayName को कॉल लगाई जा रही है...",
                    target = displayName,
                    detail = number
                )
            } catch (e: Exception) {
                Log.e(tag, "ACTION_CALL failed", e)
                fallbackToDialer(number, displayName)
            }
        } else {
            Log.w(tag, "CALL_PHONE permission not granted, falling back to ACTION_DIAL")
            fallbackToDialer(number, displayName)
        }
    }

    private fun fallbackToDialer(number: String, displayName: String): CommandResult {
        return try {
            val dialIntent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:$number")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(dialIntent)
            Log.d(tag, "MYRA_RESULT: PERMISSION_REQUIRED, ACTION_DIAL fallback to $number")
            CommandResult.permissionRequired(
                "डायलर में $displayName ($number) का नंबर लगा दिया है। कॉल बटन दबाएं।",
                target = displayName,
                detail = number
            )
        } catch (e: Exception) {
            Log.e(tag, "ACTION_DIAL failed", e)
            CommandResult.failed(
                "कॉल लगाने में समस्या आई।",
                target = displayName,
                detail = e.message ?: ""
            )
        }
    }

    private fun openDialer(): CommandResult {
        return try {
            val dialIntent = Intent(Intent.ACTION_DIAL).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(dialIntent)
            Log.d(tag, "MYRA_RESULT: SUCCESS, opened dialer")
            CommandResult.success("Phone dialer खोल दिया।", target = "dialer")
        } catch (e: Exception) {
            Log.e(tag, "Failed to open dialer", e)
            CommandResult.failed("Phone dialer नहीं खुला।", target = "dialer", detail = e.message ?: "")
        }
    }

    private fun lookupContactNumber(nameQuery: String): Pair<String, String>? {
        val resolver = context.contentResolver
        val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER
        )
        val selection = "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?"
        val selectionArgs = arrayOf("%$nameQuery%")

        return try {
            resolver.query(uri, projection, selection, selectionArgs, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                    val numIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                    val name = if (nameIdx >= 0) cursor.getString(nameIdx) else nameQuery
                    val number = if (numIdx >= 0) cursor.getString(numIdx) else null
                    if (number != null) Pair(name, number) else null
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "Error searching contacts", e)
            null
        }
    }

    /**
     * Handles SMS composition and sending.
     * Complies strictly with Android security guidelines:
     * - Uses Intent.ACTION_SENDTO / smsto: to display confirmation in user's default SMS app.
     * - If directSendConfirmed is requested and SEND_SMS permission is granted, can send directly.
     */
    fun handleSmsCommand(target: String, messageText: String, directSendConfirmed: Boolean = false): CommandResult {
        val query = target.trim()
        val text = messageText.trim()
        Log.d(tag, "MYRA_ACTION: SEND_SMS target='$query', direct=$directSendConfirmed")

        if (query.isEmpty() && text.isEmpty()) {
            return openSmsApp()
        }

        // 1. Resolve phone number
        var resolvedNumber: String? = null
        var displayName = query

        val digitsOnly = query.filter { it.isDigit() || it == '+' }
        if (digitsOnly.length >= 5 && digitsOnly.length > query.length / 2) {
            resolvedNumber = digitsOnly
            displayName = "नंबर $digitsOnly"
        } else if (query.isNotBlank()) {
            val contact = lookupContactNumber(query)
            if (contact != null) {
                displayName = contact.first
                resolvedNumber = contact.second
            } else {
                Log.w(tag, "Contact not found for SMS query: $query")
                return CommandResult.notFound(
                    "'$query' नाम का कोई कॉन्टैक्ट नहीं मिला।",
                    target = query
                )
            }
        }

        if (resolvedNumber == null) {
            return openSmsApp()
        }

        // 2. Direct send branch if explicitly confirmed and permitted
        val hasSmsPerm = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.SEND_SMS
        ) == PackageManager.PERMISSION_GRANTED

        if (directSendConfirmed && hasSmsPerm) {
            return try {
                val smsManager: android.telephony.SmsManager = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                    context.getSystemService(android.telephony.SmsManager::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    android.telephony.SmsManager.getDefault()
                }
                smsManager.sendTextMessage(resolvedNumber, null, text, null, null)
                Log.d(tag, "MYRA_RESULT: SUCCESS, direct SMS sent to $resolvedNumber")
                CommandResult.success(
                    "$displayName को SMS भेज दिया गया है: \"$text\"",
                    target = displayName,
                    detail = "$resolvedNumber • $text"
                )
            } catch (e: Exception) {
                Log.e(tag, "Direct SMS sending failed", e)
                fallbackToSmsCompose(resolvedNumber, displayName, text)
            }
        }

        // 3. Safe Default / Official Android Compose Intent
        return fallbackToSmsCompose(resolvedNumber, displayName, text)
    }

    private fun fallbackToSmsCompose(number: String, displayName: String, text: String): CommandResult {
        return try {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("smsto:$number")
                putExtra("sms_body", text)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            CommandResult.success(
                "$displayName को SMS भेजने के लिए मैसेज तैयार कर दिया है। भेजने के लिए Send दबाएं।",
                target = displayName,
                detail = "$number • $text"
            )
        } catch (e: Exception) {
            Log.e(tag, "SMS Compose intent failed", e)
            CommandResult.failed("SMS ऐप खोलने में समस्या आई: ${e.message}", target = displayName)
        }
    }

    private fun openSmsApp(): CommandResult {
        return try {
            val intent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_APP_MESSAGING)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            CommandResult.success("मैसेज ऐप खोल दिया गया है।", target = "sms")
        } catch (e: Exception) {
            CommandResult.failed("मैसेज ऐप नहीं खुला।", target = "sms")
        }
    }
}
