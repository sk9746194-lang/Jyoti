package com.myra.assistant.services

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent

class MyraAccessibilityService : AccessibilityService() {

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Intercept and automate UI elements on screen (e.g. WhatsApp send button)
    }

    override fun onInterrupt() {}

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
