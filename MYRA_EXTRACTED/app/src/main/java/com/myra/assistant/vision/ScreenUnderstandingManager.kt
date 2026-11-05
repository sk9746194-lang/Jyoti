package com.myra.assistant.vision

import android.content.Context
import android.util.Log
import com.myra.assistant.data.GeminiApiClient
import com.myra.assistant.data.PreferencesManager
import com.myra.assistant.services.MyraAccessibilityService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Screen Understanding Engine for MYRA Assistant.
 * Safely extracts visible screen hierarchy via MyraAccessibilityService (privacy-compliant,
 * password-sanitized) and feeds context into Gemini models for conversational reasoning,
 * translation, summarization, and task guidance.
 */
class ScreenUnderstandingManager(private val context: Context) {

    private val tag = "MYRA_ScreenUnderstanding"

    fun isAccessibilityReady(): Boolean {
        return MyraAccessibilityService.isRunning()
    }

    /**
     * Extracts screen text and prompts Gemini to understand, summarize, or answer questions
     * about the currently visible application on user's phone.
     */
    suspend fun analyzeCurrentScreen(userPrompt: String = "Explain what is on my screen right now in brief Hindi/English."): Result<String> = withContext(Dispatchers.IO) {
        val service = MyraAccessibilityService.instance
        if (service == null) {
            Log.w(tag, "MyraAccessibilityService is not connected")
            return@withContext Result.failure(
                IllegalStateException("स्क्रीन समझने के लिए MYRA Accessibility Service चालू होनी चाहिए। कृपया Android सेटिंग्स में इसे चालू करें।")
            )
        }

        val rawText = service.extractCurrentScreenText()
        if (rawText.isBlank()) {
            Log.w(tag, "No readable text detected on screen")
            return@withContext Result.failure(
                IllegalStateException("स्क्रीन पर कोई पढ़ने योग्य टेक्स्ट नहीं मिला। (सुरक्षित बैंकिंग या प्रोटेक्टेड स्क्रीन पढ़ी नहीं जा सकतीं)")
            )
        }

        // Limit length to 4000 characters to stay within token limits and maintain latency
        val truncatedText = if (rawText.length > 4000) rawText.take(4000) + "\n...[Content Truncated]" else rawText

        val prefs = PreferencesManager(context)
        if (!prefs.hasConfiguredApiKey) {
            return@withContext Result.success("स्क्रीन पर यह सामग्री मिली:\n\n${truncatedText.take(300)}...\n\n(विस्तृत AI विश्लेषण के लिए Google Gemini API Key दर्ज करें)")
        }

        val fullPrompt = """
            Here is the text extracted from the user's active Android phone screen:
            ---
            $truncatedText
            ---
            User Question / Task: $userPrompt
            
            Please provide a clear, helpful, and concise response in the user's language (Hindi, Hinglish, or English). If they asked to summarize, give key bullet points.
        """.trimIndent()

        GeminiApiClient.generateResponse(
            apiKey = prefs.geminiApiKey,
            model = prefs.selectedGeminiModel,
            userPrompt = fullPrompt,
            systemInstruction = "You are MYRA Screen Assistant. Help the user understand, navigate, or summarize what is displayed on their Android phone screen with precision and clarity."
        )
    }
}
