package com.myra.companion.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.util.Log
import android.view.accessibility.AccessibilityEvent

/**
 * Android Accessibility Service for MYRA Companion.
 * Receives remote touch and navigation gestures dispatched from the Primary Phone
 * over the local hotspot network and dispatches them on this Android 15 device.
 */
class CompanionAccessibilityService : AccessibilityService() {

    private val tag = "MYRA_CompAccessibility"

    companion object {
        @Volatile
        var instance: CompanionAccessibilityService? = null
            private set

        fun isRunning(): Boolean = instance != null
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        Log.d(tag, "Companion Accessibility Service connected and active")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Passive monitoring
    }

    override fun onInterrupt() {
        Log.w(tag, "Companion Accessibility Service interrupted")
    }

    override fun onDestroy() {
        instance = null
        super.onDestroy()
        Log.d(tag, "Companion Accessibility Service destroyed")
    }

    /**
     * Executes a tap gesture at the specified screen pixel coordinates.
     */
    fun performTap(x: Float, y: Float): Boolean {
        val path = Path().apply {
            moveTo(x, y)
        }
        val stroke = GestureDescription.StrokeDescription(path, 0, 50)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()
        return dispatchGesture(gesture, null, null)
    }

    /**
     * Executes a swipe or scroll gesture from (x1, y1) to (x2, y2).
     */
    fun performSwipe(x1: Float, y1: Float, x2: Float, y2: Float, durationMs: Long = 200): Boolean {
        val path = Path().apply {
            moveTo(x1, y1)
            lineTo(x2, y2)
        }
        val stroke = GestureDescription.StrokeDescription(path, 0, durationMs.coerceIn(50, 1000))
        val gesture = GestureDescription.Builder().addStroke(stroke).build()
        return dispatchGesture(gesture, null, null)
    }

    fun performBackGesture(): Boolean {
        return performGlobalAction(GLOBAL_ACTION_BACK)
    }

    fun performHomeGesture(): Boolean {
        return performGlobalAction(GLOBAL_ACTION_HOME)
    }

    fun performLockGesture(): Boolean {
        return performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
    }
}
