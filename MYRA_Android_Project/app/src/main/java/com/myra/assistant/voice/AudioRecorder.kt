package com.myra.assistant.voice

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder

class AudioRecorder(
    private val sampleRate: Int = 16000,
    private val onChunkRecorded: ((ByteArray, Float) -> Unit)? = null
) {
    private var audioRecord: AudioRecord? = null
    private var isRecording = false

    @SuppressLint("MissingPermission")
    fun startRecording(onChunk: ((ByteArray) -> Unit)? = null) {
        val bufferSize = AudioRecord.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )

        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.VOICE_RECOGNITION,
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufferSize
        )

        audioRecord?.startRecording()
        isRecording = true

        Thread {
            val buffer = ByteArray(bufferSize)
            while (isRecording) {
                val read = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                if (read > 0) {
                    val chunk = buffer.copyOf(read)
                    // Calculate RMS audio level
                    var sum = 0.0
                    for (i in 0 until read step 2) {
                        val sample = (chunk[i].toInt() and 0xFF) or (chunk[i + 1].toInt() shl 8)
                        sum += sample * sample
                    }
                    val rms = Math.sqrt(sum / (read / 2.0))
                    val level = Math.min(1.0f, (rms / 8000.0f).toFloat())
                    
                    onChunkRecorded?.invoke(chunk, level)
                    onChunk?.invoke(chunk)
                }
            }
        }.start()
    }

    fun stopRecording() {
        isRecording = false
        try {
            audioRecord?.stop()
            audioRecord?.release()
            audioRecord = null
        } catch (e: Exception) {}
    }

    fun release() {
        stopRecording()
    }
}
