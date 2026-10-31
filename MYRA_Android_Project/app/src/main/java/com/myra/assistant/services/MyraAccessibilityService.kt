package com.myra.assistant.services

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.text.TextUtils
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityManager
import android.view.accessibility.AccessibilityNodeInfo

/**
 * MYRA Accessibility Service for WhatsApp Automation and Hands-Free Assistance.
 * Safely navigates WhatsApp UI, resolves and verifies recipients, pre-fills messages,
 * and handles send execution under explicit user authorization.
 */
class MyraAccessibilityService : AccessibilityService() {

    data class AutomationRequest(
        val targetContact: String,
        val message: String,
        val autoSend: Boolean,
        val createdAt: Long = System.currentTimeMillis(),
        val onComplete: (Boolean, String) -> Unit
    )

    private val handler = Handler(Looper.getMainLooper())
    private var pendingRequest: AutomationRequest? = null
    private var isProcessing = false
    private var stepAttemptCount = 0

    companion object {
        private const val TAG = "MYRA_Accessibility"
        var instance: MyraAccessibilityService? = null
            private set

        fun isRunning(): Boolean = instance != null

        /**
         * Checks whether the user has enabled MyraAccessibilityService in Android Settings.
         */
        fun isAccessibilityEnabled(context: Context): Boolean {
            val expectedComponentName = "${context.packageName}/${MyraAccessibilityService::class.java.name}"
            val enabledServicesSetting = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: return false

            val colonSplitter = TextUtils.SimpleStringSplitter(':')
            colonSplitter.setString(enabledServicesSetting)

            while (colonSplitter.hasNext()) {
                val componentName = colonSplitter.next()
                if (componentName.equals(expectedComponentName, ignoreCase = true) ||
                    componentName.contains("MyraAccessibilityService", ignoreCase = true)
                ) {
                    return true
                }
            }

            // Fallback via AccessibilityManager
            val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager
            val runningServices = am?.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_GENERIC)
            return runningServices?.any { it.resolveInfo.serviceInfo.name == MyraAccessibilityService::class.java.name } ?: false
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        Log.i(TAG, "MyraAccessibilityService connected and ready.")
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        pendingRequest = null
        Log.i(TAG, "MyraAccessibilityService destroyed.")
    }

    override fun onInterrupt() {
        Log.w(TAG, "MyraAccessibilityService interrupted.")
        pendingRequest = null
        isProcessing = false
    }

    /**
     * Queues a WhatsApp automation request from WhatsAppAutomation or voice dispatcher.
     */
    fun enqueueWhatsAppTask(request: AutomationRequest) {
        this.pendingRequest = request
        this.isProcessing = false
        this.stepAttemptCount = 0

        Log.i(TAG, "Enqueued WhatsApp automation task for contact: '${request.targetContact}', autoSend: ${request.autoSend}")

        // 8-second safety timeout: if WhatsApp UI is changed or stuck, abort gracefully
        handler.postDelayed({
            pendingRequest?.let { current ->
                if (current == request) {
                    Log.w(TAG, "Automation request timed out after 8s.")
                    current.onComplete(
                        false,
                        "WhatsApp ऑटोमेशन टाइमआउट: चैट स्क्रीन खुल गई है, कृपया संदेश खुद जांचें और भेजें।"
                    )
                    pendingRequest = null
                    isProcessing = false
                }
            }
        }, 8000)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        val current = pendingRequest ?: return
        val pkg = event.packageName?.toString() ?: ""

        if (!pkg.startsWith("com.whatsapp")) return

        val root = rootInActiveWindow ?: return

        try {
            processWhatsAppEvent(root, current)
        } catch (e: Exception) {
            Log.e(TAG, "Error processing WhatsApp accessibility event", e)
        }
    }

    private fun processWhatsAppEvent(root: AccessibilityNodeInfo, request: AutomationRequest) {
        if (isProcessing) return
        isProcessing = true
        stepAttemptCount++

        // Step 1: Check Recipient Safety & Verification
        val currentChatTitle = findChatTitle(root)
        if (currentChatTitle != null && request.targetContact.isNotBlank()) {
            val matches = verifyRecipientMatch(currentChatTitle, request.targetContact)
            if (!matches) {
                Log.w(TAG, "Recipient mismatch detected! Current chat: '$currentChatTitle', Expected: '${request.targetContact}'")
                // Do not auto-send if there's any uncertainty about the recipient
                if (request.autoSend) {
                    pendingRequest = null
                    isProcessing = false
                    request.onComplete(
                        false,
                        "सुरक्षा जांच: वर्तमान चैट '$currentChatTitle' लक्षित संपर्क '${request.targetContact}' से मेल नहीं खाती। सुरक्षा कारणों से संदेश नहीं भेजा गया।"
                    )
                    return
                }
            }
        }

        // Step 2: Locate the WhatsApp message input field
        val inputNode = findMessageInputField(root)

        if (inputNode != null) {
            val currentText = inputNode.text?.toString() ?: ""

            // Fill message if empty or different
            if (request.message.isNotBlank() && !currentText.contains(request.message)) {
                val arguments = Bundle().apply {
                    putCharSequence(
                        AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                        request.message
                    )
                }
                val textSet = inputNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
                Log.d(TAG, "Message text set into input field: $textSet")
            }

            // Step 3: Handle Send Button
            if (request.autoSend) {
                // Find Send button
                handler.postDelayed({
                    val activeRoot = rootInActiveWindow ?: root
                    val sendButton = findSendButton(activeRoot)

                    if (sendButton != null) {
                        val clicked = sendButton.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                        Log.i(TAG, "Send button clicked: $clicked")

                        // Verify message cleared
                        handler.postDelayed({
                            val verifyRoot = rootInActiveWindow
                            val recheckInput = if (verifyRoot != null) findMessageInputField(verifyRoot) else null
                            val recheckText = recheckInput?.text?.toString() ?: ""

                            val isSent = recheckText.isBlank() || !recheckText.contains(request.message)

                            if (isSent) {
                                pendingRequest = null
                                isProcessing = false
                                request.onComplete(
                                    true,
                                    "${request.targetContact.ifBlank { "WhatsApp" }} को संदेश सफलतापूर्वक भेज दिया गया है।"
                                )
                            } else {
                                pendingRequest = null
                                isProcessing = false
                                request.onComplete(
                                    true,
                                    "संदेश WhatsApp में टाइप कर दिया गया है। भेजने के लिए Send बटन दबाएं।"
                                )
                            }
                        }, 500)
                    } else {
                        Log.w(TAG, "Send button not found or disabled.")
                        pendingRequest = null
                        isProcessing = false
                        request.onComplete(
                            true,
                            "संदेश WhatsApp में लिख दिया गया है। कृपया Send बटन दबाकर भेजें।"
                        )
                    }
                }, 300)
            } else {
                // Pre-filled only, user manual confirmation requested
                pendingRequest = null
                isProcessing = false
                request.onComplete(
                    true,
                    "${request.targetContact.ifBlank { "WhatsApp" }} के लिए संदेश तैयार कर दिया गया है। पुष्टि के लिए Send दबाएं।"
                )
            }
        } else {
            isProcessing = false
        }
    }

    /**
     * Resolves the current chat recipient's name from WhatsApp header bar.
     */
    private fun findChatTitle(root: AccessibilityNodeInfo): String? {
        // WhatsApp standard ID for conversation contact name
        val titleNodes = root.findAccessibilityNodeInfosByViewId("com.whatsapp:id/conversation_contact_name")
        if (!titleNodes.isNullOrEmpty()) {
            return titleNodes[0].text?.toString()
        }

        // WhatsApp Business alternative
        val w4bTitleNodes = root.findAccessibilityNodeInfosByViewId("com.whatsapp.w4b:id/conversation_contact_name")
        if (!w4bTitleNodes.isNullOrEmpty()) {
            return w4bTitleNodes[0].text?.toString()
        }

        // Fallback: search nodes near top toolbar
        val actionBars = root.findAccessibilityNodeInfosByViewId("com.whatsapp:id/action_bar")
        if (!actionBars.isNullOrEmpty()) {
            for (i in 0 until actionBars[0].childCount) {
                val child = actionBars[0].getChild(i)
                val text = child?.text?.toString()
                if (!text.isNullOrBlank() && text != "WhatsApp" && !text.contains("online", ignoreCase = true)) {
                    return text
                }
            }
        }
        return null
    }

    /**
     * Verifies that the recipient name in the chat view matches the requested voice target.
     */
    private fun verifyRecipientMatch(actual: String, requested: String): Boolean {
        val a = actual.trim().lowercase()
        val r = requested.trim().lowercase()
        if (a == r) return true
        if (a.contains(r) || r.contains(a)) return true

        // Clean common honorifics
        val cleanA = a.replace("ji", "").replace("bhai", "").trim()
        val cleanR = r.replace("ji", "").replace("bhai", "").replace("ko", "").trim()
        return cleanA == cleanR || cleanA.contains(cleanR) || cleanR.contains(cleanA)
    }

    /**
     * Locates the WhatsApp text entry box across standard WhatsApp and WhatsApp Business.
     */
    private fun findMessageInputField(root: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        val entryIds = listOf(
            "com.whatsapp:id/entry",
            "com.whatsapp.w4b:id/entry",
            "com.whatsapp:id/input_edit_text",
            "com.whatsapp.w4b:id/input_edit_text"
        )

        for (id in entryIds) {
            val nodes = root.findAccessibilityNodeInfosByViewId(id)
            if (!nodes.isNullOrEmpty()) {
                return nodes[0]
            }
        }

        // Fallback by class name
        return findNodeByClass(root, "android.widget.EditText")
    }

    /**
     * Locates the Send button in WhatsApp.
     */
    private fun findSendButton(root: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        val sendIds = listOf(
            "com.whatsapp:id/send",
            "com.whatsapp.w4b:id/send"
        )

        for (id in sendIds) {
            val nodes = root.findAccessibilityNodeInfosByViewId(id)
            if (!nodes.isNullOrEmpty()) {
                val btn = nodes[0]
                if (btn.isClickable || btn.isEnabled) return btn
            }
        }

        // Search by content description in Hindi and English
        val descriptors = listOf("Send", "send", "भेजें", "संदेश भेजें")
        for (desc in descriptors) {
            val byText = root.findAccessibilityNodeInfosByText(desc)
            if (!byText.isNullOrEmpty()) {
                for (node in byText) {
                    if (node.isClickable) return node
                    val parent = node.parent
                    if (parent != null && parent.isClickable) return parent
                }
            }
        }

        return null
    }

    private fun findNodeByClass(node: AccessibilityNodeInfo, className: String): AccessibilityNodeInfo? {
        if (node.className?.toString() == className) {
            return node
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val found = findNodeByClass(child, className)
            if (found != null) return found
        }
        return null
    }

    fun performBackGesture() {
        performGlobalAction(GLOBAL_ACTION_BACK)
    }

    fun performHomeGesture() {
        performGlobalAction(GLOBAL_ACTION_HOME)
    }

    fun performLockGesture() {
        performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
    }
}
