package com.myra.assistant.services

import android.content.Intent
import android.speech.RecognitionService

/**
 * Android RecognitionService bound to MyraVoiceInteractionService.
 */
class MyraRecognitionService : RecognitionService() {

    override fun onStartListening(recognizerIntent: Intent?, listener: Callback?) {
        // Recognition start listener
    }

    override fun onCancel(listener: Callback?) {
        // Cancel listener
    }

    override fun onStopListening(listener: Callback?) {
        // Stop listener
    }
}
