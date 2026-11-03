package com.myra.assistant.automation

enum class TriggerType(val displayName: String) {
    BATTERY_BELOW("Battery Drops Below"),
    BATTERY_CHARGED("Battery Fully Charged"),
    BOOT_COMPLETED("Phone Boot Completed"),
    TIME_SCHEDULE("Daily Time Schedule"),
    HEADPHONES_CONNECTED("Headphones Connected")
}

enum class ActionType(val displayName: String) {
    SPEAK_ALERT("Voice Announcement"),
    SEND_NOTIFICATION("System Notification"),
    TOGGLE_TORCH("Toggle Flashlight"),
    SET_VOLUME("Set Media Volume"),
    OPEN_APP("Launch App"),
    LAUNCH_ASSISTANT("Open MYRA Voice")
}

data class AutomationRecipe(
    val id: String,
    val title: String,
    val triggerType: TriggerType,
    val triggerValue: String,
    val actionType: ActionType,
    val actionValue: String,
    val isEnabled: Boolean = true,
    val requireBiometricConfirm: Boolean = false,
    val lastTriggered: Long = 0L
)

data class RecipeExecutionLog(
    val id: String,
    val recipeTitle: String,
    val timestamp: Long,
    val success: Boolean,
    val message: String
)
