package com.myra.assistant.data

enum class AssistantState {
    IDLE,
    LISTENING,
    THINKING,
    SPEAKING,
    ERROR,
    WAKE_WORD
}

enum class VoiceEngineMode {
    GEMINI_LIVE,
    GEMINI_TTS,
    FALLBACK_TTS
}

enum class PersonalityMode {
    FRIENDLY_ASSISTANT,
    ROMANTIC_GF,
    PROFESSIONAL
}

data class VoiceStatus(
    val activeMode: VoiceEngineMode = VoiceEngineMode.GEMINI_LIVE,
    val voiceName: String = "Kore (Female Indian Voice)",
    val sampleRateIn: Int = 16000,
    val sampleRateOut: Int = 24000,
    val fallbackActive: Boolean = false,
    val fallbackReason: String? = null
)

data class AssistantResponse(
    val text: String,
    val action: String = "general_chat",
    val actionParams: Map<String, Any> = emptyMap(),
    val category: String = "General"
)
