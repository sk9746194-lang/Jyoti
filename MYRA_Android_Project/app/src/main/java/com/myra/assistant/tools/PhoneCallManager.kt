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
}
