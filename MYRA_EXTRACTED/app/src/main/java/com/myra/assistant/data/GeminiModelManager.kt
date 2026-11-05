package com.myra.assistant.data

import android.content.Context
import android.util.Log

/**
 * Centralized Gemini AI Model Manager for MYRA Assistant.
 * Implements Smart Gemini Model Routing:
 * - A. Normal Questions -> Low-cost/Free-Tier Gemini Flash (gemini-3.8-flash)
 * - B. Complex Questions -> Reasoning Model (gemini-2.5-pro / high-thinking)
 * - C. Photos -> Multimodal Model for camera/gallery, OCR, documents, diagrams
 * - D. Voice -> Gemini Live (gemini-3.8-live) & Gemini TTS (gemini-3.8-flash-lite-tts)
 * - Enforces Free API & Paid Usage Protection: Paid API usage is blocked unless explicitly enabled.
 */
object GeminiModelManager {
    private const val TAG = "MYRA_ModelManager"

    // Model identifiers (aligned with modern @google/genai specifications)
    const val MODEL_FLASH = "gemini-3.8-flash"
    const val MODEL_FLASH_LITE = "gemini-3.5-flash-lite"
    const val MODEL_REASONING = "gemini-2.5-pro"
    const val MODEL_MULTIMODAL = "gemini-3.8-flash"
    const val MODEL_LIVE = "gemini-3.8-live"
    const val MODEL_TTS = "gemini-3.8-flash-lite-tts"

    enum class QueryCategory {
        NORMAL_CONVERSATION,
        COMPLEX_REASONING,
        MULTIMODAL_IMAGE,
        VOICE_REALTIME
    }

    /**
     * Intelligently classifies query and selects best Gemini model.
     * Respects paid usage protection setting.
     */
    fun selectModelForQuery(
        query: String,
        hasImages: Boolean,
        isVoiceLive: Boolean,
        allowPaidUsage: Boolean = false
    ): String {
        // 1. Multimodal image query
        if (hasImages) {
            Log.d(TAG, "Routing to Multimodal model: $MODEL_MULTIMODAL")
            return MODEL_MULTIMODAL
        }

        // 2. Realtime voice session
        if (isVoiceLive) {
            Log.d(TAG, "Routing to Gemini Live model: $MODEL_LIVE")
            return MODEL_LIVE
        }

        // 3. Complex mathematics, coding, analysis, or deep logic
        val lower = query.lowercase()
        val isComplex = lower.contains("calculate") ||
                lower.contains("formula") ||
                lower.contains("solve") ||
                lower.contains("derivative") ||
                lower.contains("integral") ||
                lower.contains("math") ||
                lower.contains("गणित") ||
                lower.contains("algorithm") ||
                lower.contains("code") ||
                lower.contains("function") ||
                lower.contains("debug") ||
                lower.contains("program") ||
                lower.contains("proof") ||
                lower.contains("explain step by step") ||
                lower.contains("विस्तार से समझाओ")

        if (isComplex) {
            // If paid usage is enabled and user selected reasoning, route to reasoning model
            if (allowPaidUsage) {
                Log.d(TAG, "Routing complex query to Reasoning model: $MODEL_REASONING")
                return MODEL_REASONING
            } else {
                // Free tier safe reasoning on gemini-3.8-flash with thinking
                Log.d(TAG, "Routing complex query to free-tier thinking on: $MODEL_FLASH")
                return MODEL_FLASH
            }
        }

        // 4. Default: High-speed everyday conversational Flash model
        return MODEL_FLASH
    }

    /**
     * Checks if a model name is considered a paid-tier model requiring explicit confirmation.
     */
    fun isPaidModel(modelName: String): Boolean {
        val paidModels = listOf(
            "gemini-4-argon",
            "gemini-2.5-pro",
            "gemini-3-pro-image",
            "gemini-nano-banana-2.1",
            "gemini-omni-1.1-flash",
            "lyria-3-clip-preview",
            "lyria-3-pro-preview"
        )
        return paidModels.any { modelName.contains(it, ignoreCase = true) }
    }
}
