package com.myra.assistant.automation

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.myra.assistant.R
import java.util.UUID

/**
 * Manages persistent user automation recipes, evaluates triggers against device state,
 * and executes authorized actions with an audit log.
 */
class AutomationRecipeManager private constructor(private val context: Context) {

    private val prefs = context.getSharedPreferences("myra_automation_recipes", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val tag = "MYRA_Automation"

    companion object {
        private const val KEY_RECIPES = "recipes_json"
        private const val KEY_HISTORY = "history_json"
        private const val CHANNEL_ID = "myra_automation_channel"

        @Volatile
        private var instance: AutomationRecipeManager? = null

        fun getInstance(context: Context): AutomationRecipeManager {
            return instance ?: synchronized(this) {
                instance ?: AutomationRecipeManager(context.applicationContext).also { instance = it }
            }
        }
    }

    init {
        createNotificationChannel()
        ensureDefaultRecipes()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "MYRA Automations",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Triggers and recipes executed by MYRA Assistant"
            }
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            nm?.createNotificationChannel(channel)
        }
    }

    private fun ensureDefaultRecipes() {
        if (!prefs.contains(KEY_RECIPES)) {
            val defaults = listOf(
                AutomationRecipe(
                    id = "default_battery_low",
                    title = "Battery Below 15% Alert",
                    triggerType = TriggerType.BATTERY_BELOW,
                    triggerValue = "15",
                    actionType = ActionType.SPEAK_ALERT,
                    actionValue = "बैटरी 15% से कम है, कृपया फोन चार्जर से कनेक्ट करें।",
                    isEnabled = true
                ),
                AutomationRecipe(
                    id = "default_battery_full",
                    title = "Battery 100% Fully Charged Alert",
                    triggerType = TriggerType.BATTERY_CHARGED,
                    triggerValue = "100",
                    actionType = ActionType.SPEAK_ALERT,
                    actionValue = "बैटरी पूरी तरह 100% चार्ज हो चुकी है। कृपया चार्जर अनप्लग करें।",
                    isEnabled = true
                ),
                AutomationRecipe(
                    id = "default_boot_welcome",
                    title = "Boot Start Welcome",
                    triggerType = TriggerType.BOOT_COMPLETED,
                    triggerValue = "",
                    actionType = ActionType.SEND_NOTIFICATION,
                    actionValue = "MYRA AI Assistant सक्रिय है और आपकी सेवा के लिए तैयार है।",
                    isEnabled = true
                )
            )
            saveRecipesList(defaults)
        }
    }

    fun getRecipes(): List<AutomationRecipe> {
        val json = prefs.getString(KEY_RECIPES, null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<AutomationRecipe>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            Log.e(tag, "Error parsing recipes", e)
            emptyList()
        }
    }

    fun saveRecipe(recipe: AutomationRecipe) {
        val current = getRecipes().toMutableList()
        val index = current.indexOfFirst { it.id == recipe.id }
        if (index >= 0) {
            current[index] = recipe
        } else {
            current.add(recipe)
        }
        saveRecipesList(current)
    }

    fun deleteRecipe(id: String) {
        val current = getRecipes().filter { it.id != id }
        saveRecipesList(current)
    }

    fun toggleRecipe(id: String, isEnabled: Boolean) {
        val current = getRecipes().map {
            if (it.id == id) it.copy(isEnabled = isEnabled) else it
        }
        saveRecipesList(current)
    }

    private fun saveRecipesList(list: List<AutomationRecipe>) {
        val json = gson.toJson(list)
        prefs.edit().putString(KEY_RECIPES, json).apply()
    }

    fun getExecutionHistory(): List<RecipeExecutionLog> {
        val json = prefs.getString(KEY_HISTORY, null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<RecipeExecutionLog>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun logExecution(recipeTitle: String, success: Boolean, message: String) {
        val current = getExecutionHistory().toMutableList()
        val entry = RecipeExecutionLog(
            id = UUID.randomUUID().toString(),
            recipeTitle = recipeTitle,
            timestamp = System.currentTimeMillis(),
            success = success,
            message = message
        )
        current.add(0, entry)
        if (current.size > 50) {
            current.removeAt(current.lastIndex)
        }
        prefs.edit().putString(KEY_HISTORY, gson.toJson(current)).apply()
    }

    /**
     * Evaluates active recipes matching a battery drop or full charge event.
     */
    fun evaluateBatteryChange(percent: Int, isCharging: Boolean, onVoiceAlert: (String) -> Unit) {
        val recipes = getRecipes().filter { it.isEnabled }
        val now = System.currentTimeMillis()

        for (r in recipes) {
            // Rate limit: do not re-trigger same recipe within 10 minutes
            if (now - r.lastTriggered < 600_000L) continue

            var matched = false
            if (r.triggerType == TriggerType.BATTERY_BELOW) {
                val target = r.triggerValue.toIntOrNull() ?: 15
                if (!isCharging && percent <= target) {
                    matched = true
                }
            } else if (r.triggerType == TriggerType.BATTERY_CHARGED) {
                if (isCharging && percent >= 100) {
                    matched = true
                }
            }

            if (matched) {
                executeRecipeAction(r, onVoiceAlert)
                saveRecipe(r.copy(lastTriggered = now))
            }
        }
    }

    /**
     * Evaluates boot completed triggers.
     */
    fun evaluateBootCompleted() {
        val recipes = getRecipes().filter { it.isEnabled && it.triggerType == TriggerType.BOOT_COMPLETED }
        for (r in recipes) {
            executeRecipeAction(r) { /* no-op in background */ }
        }
    }

    private fun executeRecipeAction(recipe: AutomationRecipe, onVoiceAlert: (String) -> Unit) {
        Log.i(tag, "Executing automation recipe '${recipe.title}' -> ${recipe.actionType}")
        when (recipe.actionType) {
            ActionType.SPEAK_ALERT -> {
                onVoiceAlert(recipe.actionValue)
                sendSystemNotification("MYRA Automation", recipe.actionValue)
                logExecution(recipe.title, true, "Voice announcement: ${recipe.actionValue}")
            }
            ActionType.SEND_NOTIFICATION -> {
                sendSystemNotification(recipe.title, recipe.actionValue)
                logExecution(recipe.title, true, "Notification displayed")
            }
            ActionType.TOGGLE_TORCH -> {
                val turnOn = recipe.actionValue.equals("on", ignoreCase = true)
                val deviceCtrl = com.myra.assistant.tools.DeviceControlManager(context)
                deviceCtrl.toggleFlashlight(turnOn)
                logExecution(recipe.title, true, "Flashlight set to $turnOn")
            }
            ActionType.SET_VOLUME -> {
                val level = recipe.actionValue.toIntOrNull() ?: 50
                val deviceCtrl = com.myra.assistant.tools.DeviceControlManager(context)
                deviceCtrl.setVolume(level)
                logExecution(recipe.title, true, "Volume set to $level%")
            }
            ActionType.OPEN_APP -> {
                val appLauncher = com.myra.assistant.tools.AppLauncher(context)
                appLauncher.launchAppByName(recipe.actionValue)
                logExecution(recipe.title, true, "Launched ${recipe.actionValue}")
            }
            ActionType.LAUNCH_ASSISTANT -> {
                sendSystemNotification(recipe.title, "MYRA is active and ready")
                logExecution(recipe.title, true, "Assistant opened")
            }
        }
    }

    private fun sendSystemNotification(title: String, message: String) {
        try {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            val notif = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_orb)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .build()
            nm?.notify((System.currentTimeMillis() % 10000).toInt(), notif)
        } catch (e: Exception) {
            Log.e(tag, "Failed to post notification", e)
        }
    }
}
