package com.myra.assistant.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import java.util.Locale

/**
 * Production-ready Real-Time Hands-Free Speech Engine for MYRA ULTRA.
 * Supports:
 * 1. Low-latency speech recognition with real-time RMS audio levels (Orb/Waveform).
 * 2. Text-to-Speech synthesis with UtteranceProgressListener.
 * 3. Automatic conversational turn-taking (re-listens immediately after speaking finishes).
 * 4. Resilient error recovery on silence or timeouts without freezing.
 * 5. Explicit start/stop controls for safe exit.
 */
class SystemSpeechEngine(
    private val context: Context,
    private val onResult: (String) -> Unit,
    private val onPartial: (String) -> Unit = {},
    private val onRmsChanged: (Float) -> Unit = {},
    private val onListeningStateChanged: (Boolean) -> Unit = {},
    private val onSpeakingStateChanged: (Boolean) -> Unit = {},
    private val onError: (Int, String) -> Unit = { _, _ -> }
) : TextToSpeech.OnInitListener {

    private val tag = "MYRA_SpeechEngine"
    private val mainHandler = Handler(Looper.getMainLooper())

    private var tts: TextToSpeech? = null
    private var recognizer: SpeechRecognizer? = null

    var isTtsReady = false
        private set

    var isListening = false
        private set

    var isSpeaking = false
        private set

    // When true, automatically resumes listening after assistant finishes speaking
    var isContinuousHandsFree = true

    private var isDestroyed = false
    private var activeUtteranceId: String? = null

    private val restartListeningRunnable = Runnable {
        if (!isDestroyed && isContinuousHandsFree && !isSpeaking) {
            startListeningInternal()
        }
    }

    init {
        mainHandler.post {
            initSpeechRecognizer()
            initTts()
        }
    }

    private fun initSpeechRecognizer() {
        try {
            if (SpeechRecognizer.isRecognitionAvailable(context)) {
                recognizer?.destroy()
                recognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                    setRecognitionListener(createRecognitionListener())
                }
                Log.i(tag, "SpeechRecognizer initialized successfully")
            } else {
                Log.e(tag, "Speech recognition is not available on this device")
                onError(-1, "Speech recognition unavailable on device")
            }
        } catch (e: Exception) {
            Log.e(tag, "Failed to create SpeechRecognizer: ${e.message}")
            onError(-1, e.message ?: "Failed to initialize SpeechRecognizer")
        }
    }

    private fun initTts() {
        tts = TextToSpeech(context, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            // 1. Prefer Hindi (India) locale for authentic pronunciation
            val hiLocale = Locale("hi", "IN")
            val langResult = tts?.setLanguage(hiLocale)
            val isHindiSupported = langResult != TextToSpeech.LANG_MISSING_DATA && langResult != TextToSpeech.LANG_NOT_SUPPORTED

            if (isHindiSupported) {
                // 2. Select the highest-quality natural Hindi voice from available Android TTS voices
                try {
                    val availableVoices = tts?.voices
                    if (!availableVoices.isNullOrEmpty()) {
                        val hindiVoices = availableVoices.filter { v ->
                            v.locale.language.equals("hi", ignoreCase = true)
                        }

                        if (hindiVoices.isNotEmpty()) {
                            // Prefer high-quality neural female voices (e.g. hi-in-x-hie or female tags)
                            val bestVoice = hindiVoices.firstOrNull { v ->
                                (v.name.contains("female", ignoreCase = true) || v.name.contains("hie", ignoreCase = true) || v.name.contains("hic", ignoreCase = true)) &&
                                !v.isNetworkConnectionRequired
                            } ?: hindiVoices.firstOrNull { v ->
                                v.quality >= android.speech.tts.Voice.QUALITY_HIGH
                            } ?: hindiVoices.firstOrNull { v ->
                                !v.isNetworkConnectionRequired
                            } ?: hindiVoices.first()

                            tts?.voice = bestVoice
                            Log.i(tag, "Selected optimal Hindi TTS voice: ${bestVoice.name} (quality: ${bestVoice.quality})")
                        }
                    }
                } catch (e: Exception) {
                    Log.w(tag, "Voice filtering notice: ${e.message}")
                }
                Log.i(tag, "Hindi (hi-IN) TTS language loaded successfully")
            } else {
                // 3. Fallback to Indian English (en-IN) instead of US English to preserve Indian phonetics
                Log.w(tag, "Hindi TTS voice data missing or not installed; falling back to Indian English (en-IN)")
                val inEngLocale = Locale("en", "IN")
                val inEngResult = tts?.setLanguage(inEngLocale)
                if (inEngResult != TextToSpeech.LANG_MISSING_DATA && inEngResult != TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.i(tag, "Indian English (en-IN) TTS loaded as phonetic fallback")
                } else {
                    Log.w(tag, "Indian English unavailable; falling back to default locale")
                    tts?.language = Locale.ENGLISH
                }
            }

            // Natural conversational rate and pitch tuning to avoid robotic monotone
            tts?.setPitch(1.03f)
            tts?.setSpeechRate(0.98f)

            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    isSpeaking = true
                    mainHandler.post {
                        onSpeakingStateChanged(true)
                    }
                }

                override fun onDone(utteranceId: String?) {
                    isSpeaking = false
                    mainHandler.post {
                        onSpeakingStateChanged(false)
                        // Conversational Turn-Taking: If in continuous hands-free mode,
                        // resume listening automatically after a 350ms buffer (avoiding TTS speaker echo)
                        if (isContinuousHandsFree && !isDestroyed) {
                            Log.d(tag, "TTS completed; scheduling auto-listen turn-taking")
                            scheduleListeningRestart(350)
                        }
                    }
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    isSpeaking = false
                    mainHandler.post {
                        onSpeakingStateChanged(false)
                        if (isContinuousHandsFree && !isDestroyed) {
                            scheduleListeningRestart(500)
                        }
                    }
                }

                override fun onError(utteranceId: String?, errorCode: Int) {
                    isSpeaking = false
                    mainHandler.post {
                        onSpeakingStateChanged(false)
                        Log.w(tag, "TTS error code $errorCode for utterance $utteranceId")
                        if (isContinuousHandsFree && !isDestroyed) {
                            scheduleListeningRestart(500)
                        }
                    }
                }
            })

            isTtsReady = true
        } else {
            Log.e(tag, "TextToSpeech initialization failed with status $status")
            onError(status, "TextToSpeech init failed")
        }
    }

    /**
     * Sanitizes raw conversational text so Android TTS speaks smoothly without spelling out
     * formatting symbols, asterisks, brackets, or emojis that cause robotic pauses.
     */
    private fun sanitizeForSpeech(raw: String): String {
        return raw
            // Remove markdown bold/italic formatting
            .replace(Regex("[*_#`~>]"), "")
            // Remove emojis and special symbol glyphs that TTS attempts to read aloud
            .replace(Regex("[\\p{So}\\p{Cn}]"), "")
            // Normalize multiple whitespaces
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    /**
     * Speaks the assistant response text aloud.
     * Automatically coordinates listening pause during speech and resume upon finish.
     */
    fun speak(text: String, rate: Float = 0.98f, pitch: Float = 1.03f) {
        val cleanSpeech = sanitizeForSpeech(text)
        if (cleanSpeech.isBlank()) return

        mainHandler.post {
            // Stop active listening so mic does not transcribe assistant's own voice
            stopListeningInternal()

            if (!isTtsReady || tts == null) {
                Log.w(tag, "TTS not ready yet; attempting lazy initialization")
                return@post
            }

            try {
                tts?.setSpeechRate(rate.coerceIn(0.5f, 2.0f))
                tts?.setPitch(pitch.coerceIn(0.5f, 2.0f))

                val utteranceId = "myra_utterance_${System.currentTimeMillis()}"
                activeUtteranceId = utteranceId

                val params = Bundle().apply {
                    putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId)
                }

                isSpeaking = true
                onSpeakingStateChanged(true)

                val result = tts?.speak(cleanSpeech, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
                if (result != TextToSpeech.SUCCESS) {
                    Log.e(tag, "tts.speak returned error code: $result")
                    isSpeaking = false
                    onSpeakingStateChanged(false)
                    if (isContinuousHandsFree) {
                        scheduleListeningRestart(600)
                    }
                }
            } catch (e: Exception) {
                Log.e(tag, "Exception during TTS speak: ${e.message}")
                isSpeaking = false
                onSpeakingStateChanged(false)
                if (isContinuousHandsFree) {
                    scheduleListeningRestart(600)
                }
            }
        }
    }

    /**
     * Starts listening for user voice commands.
     */
    fun startListening(continuous: Boolean = true) {
        this.isContinuousHandsFree = continuous
        mainHandler.post {
            startListeningInternal()
        }
    }

    private fun startListeningInternal() {
        if (isDestroyed || isSpeaking) return

        mainHandler.removeCallbacks(restartListeningRunnable)

        try {
            if (recognizer == null) {
                initSpeechRecognizer()
            }

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "hi-IN")
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
            }

            recognizer?.startListening(intent)
            isListening = true
            onListeningStateChanged(true)
            Log.d(tag, "SpeechRecognizer started listening")
        } catch (e: Exception) {
            Log.e(tag, "startListening error: ${e.message}")
            isListening = false
            onListeningStateChanged(false)
            if (isContinuousHandsFree) {
                scheduleListeningRestart(1000)
            }
        }
    }

    /**
     * Stops listening and pauses auto-listening loop.
     */
    fun stopListening() {
        isContinuousHandsFree = false
        mainHandler.post {
            stopListeningInternal()
        }
    }

    private fun stopListeningInternal() {
        mainHandler.removeCallbacks(restartListeningRunnable)
        try {
            recognizer?.stopListening()
            recognizer?.cancel()
        } catch (_: Exception) {}
        isListening = false
        onListeningStateChanged(false)
        onRmsChanged(0f)
    }

    /**
     * Stops both speech synthesis and recognition immediately.
     */
    fun stopAll() {
        isContinuousHandsFree = false
        mainHandler.removeCallbacks(restartListeningRunnable)
        try {
            tts?.stop()
        } catch (_: Exception) {}
        isSpeaking = false
        onSpeakingStateChanged(false)
        stopListeningInternal()
    }

    /**
     * Stops speech synthesis immediately.
     */
    fun stopSpeaking() {
        try {
            tts?.stop()
        } catch (_: Exception) {}
        isSpeaking = false
        onSpeakingStateChanged(false)
    }

    private fun scheduleListeningRestart(delayMs: Long) {
        mainHandler.removeCallbacks(restartListeningRunnable)
        if (!isDestroyed && isContinuousHandsFree && !isSpeaking) {
            mainHandler.postDelayed(restartListeningRunnable, delayMs)
        }
    }

    private fun createRecognitionListener() = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            isListening = true
            onListeningStateChanged(true)
        }

        override fun onBeginningOfSpeech() {
            Log.d(tag, "User began speaking")
        }

        override fun onRmsChanged(rmsdB: Float) {
            // Normalize Android RMS dB (-2.0f to +10.0f) to 0.0f..1.0f for real Orb & Waveform animation
            val normalized = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
            onRmsChanged(normalized)
        }

        override fun onBufferReceived(buffer: ByteArray?) {}

        override fun onEndOfSpeech() {
            Log.d(tag, "User finished speech utterance")
            onRmsChanged(0f)
        }

        override fun onError(error: Int) {
            isListening = false
            onListeningStateChanged(false)
            onRmsChanged(0f)

            val errorMsg = when (error) {
                SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                SpeechRecognizer.ERROR_CLIENT -> "Client-side error"
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission required"
                SpeechRecognizer.ERROR_NETWORK -> "Network connection error"
                SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
                SpeechRecognizer.ERROR_NO_MATCH -> "No speech recognized"
                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Speech recognizer busy"
                SpeechRecognizer.ERROR_SERVER -> "Server recognition error"
                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Speech timeout (silence)"
                else -> "Recognition error ($error)"
            }

            Log.d(tag, "RecognitionListener onError: $errorMsg (code $error)")
            onError(error, errorMsg)

            // Resilient recovery for hands-free mode
            if (isContinuousHandsFree && !isDestroyed && !isSpeaking) {
                when (error) {
                    SpeechRecognizer.ERROR_NO_MATCH,
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> {
                        // User was silent or paused; seamlessly restart listening
                        scheduleListeningRestart(300)
                    }
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY,
                    SpeechRecognizer.ERROR_CLIENT -> {
                        // Reset recognizer instance on busy/client glitch
                        mainHandler.postDelayed({
                            if (!isDestroyed && isContinuousHandsFree) {
                                initSpeechRecognizer()
                                startListeningInternal()
                            }
                        }, 600)
                    }
                    SpeechRecognizer.ERROR_AUDIO -> {
                        scheduleListeningRestart(1000)
                    }
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> {
                        // Fatal permission error; stop auto-restarting
                        isContinuousHandsFree = false
                    }
                    else -> {
                        scheduleListeningRestart(800)
                    }
                }
            }
        }

        override fun onResults(results: Bundle?) {
            isListening = false
            onListeningStateChanged(false)
            onRmsChanged(0f)

            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            if (!matches.isNullOrEmpty()) {
                val recognizedText = matches[0]
                Log.i(tag, "Recognized user speech: \"$recognizedText\"")
                onResult(recognizedText)
            } else {
                Log.d(tag, "No recognition matches in results")
                if (isContinuousHandsFree && !isSpeaking) {
                    scheduleListeningRestart(300)
                }
            }
        }

        override fun onPartialResults(partialResults: Bundle?) {
            val partialMatches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            if (!partialMatches.isNullOrEmpty()) {
                val partial = partialMatches[0]
                onPartial(partial)
            }
        }

        override fun onEvent(eventType: Int, params: Bundle?) {}
    }

    fun destroy() {
        isDestroyed = true
        isContinuousHandsFree = false
        mainHandler.removeCallbacksAndMessages(null)
        try {
            tts?.stop()
            tts?.shutdown()
        } catch (_: Exception) {}
        try {
            recognizer?.destroy()
        } catch (_: Exception) {}
        tts = null
        recognizer = null
        isSpeaking = false
        isListening = false
        Log.i(tag, "SystemSpeechEngine destroyed cleanly")
    }
}
