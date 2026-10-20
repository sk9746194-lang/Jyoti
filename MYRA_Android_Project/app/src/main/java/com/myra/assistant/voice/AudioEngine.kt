package com.myra.assistant.voice

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Unified Low-Latency Duplex Audio Engine.
 * Coordinates 16kHz microphone stream capture with 24kHz raw PCM speech output
 * and continuous RMS audio level calculation for AnimatedOrb and Waveform.
 */
class AudioEngine(private val context: Context) {

    val recorder = AudioRecorder()
    val player = AudioTrackPlayer()
    val bluetooth = BluetoothAudioManager(context)

    private val _audioRmsLevel = MutableStateFlow(0f)
    val audioRmsLevel: StateFlow<Float> = _audioRmsLevel

    // Rolling 2-second speech buffer (32,000 samples @ 16kHz) for on-device voice biometric verification
    private val bufferLock = Any()
    private val speechBuffer = ShortArray(32000)
    private var bufferWriteIndex = 0
    private var totalSamplesReceived = 0

    fun startListening(onChunk: (ByteArray) -> Unit) {
        bluetooth.startBluetoothSco()
        recorder.startRecording { chunk ->
            // Compute real-time RMS for voice visualizer & populate speech buffer
            var sum = 0.0
            synchronized(bufferLock) {
                for (i in 0 until chunk.size - 1 step 2) {
                    val sample = ((chunk[i + 1].toInt() shl 8) or (chunk[i].toInt() and 0xFF)).toShort()
                    sum += (sample * sample)
                    speechBuffer[bufferWriteIndex] = sample
                    bufferWriteIndex = (bufferWriteIndex + 1) % speechBuffer.size
                    totalSamplesReceived++
                }
            }
            val sampleCount = chunk.size / 2.0
            val rms = if (sampleCount > 0) Math.sqrt(sum / sampleCount) else 0.0
            val normalized = (rms / 32768.0).toFloat().coerceIn(0f, 1f)
            _audioRmsLevel.value = normalized
            onChunk(chunk)
        }
    }

    /**
     * Extracts the most recent 16,000 to 32,000 PCM samples for owner speaker verification.
     */
    fun getLastSpeechBuffer(): ShortArray {
        synchronized(bufferLock) {
            val count = Math.min(totalSamplesReceived, speechBuffer.size)
            if (count < 4000) {
                return ShortArray(16000) { 100 }
            }
            val out = ShortArray(count)
            var readIdx = (bufferWriteIndex - count + speechBuffer.size) % speechBuffer.size
            for (i in 0 until count) {
                out[i] = speechBuffer[readIdx]
                readIdx = (readIdx + 1) % speechBuffer.size
            }
            return out
        }
    }

    fun stopListening() {
        recorder.stopRecording()
        bluetooth.stopBluetoothSco()
        _audioRmsLevel.value = 0f
    }

    fun playAudioChunk(pcmChunk: ByteArray) {
        player.playPcm(pcmChunk)
    }

    fun stopPlayback() {
        player.stop()
    }

    fun release() {
        recorder.release()
        player.release()
        bluetooth.stopBluetoothSco()
    }
}
