package com.myra.assistant.voice

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log

class AudioRecorder(
    private val sampleRate: Int = 16000,
    private val onChunkRecorded: ((ByteArray, Float) -> Unit)? = null
) {
    private var audioRecord: AudioRecord? = null
    @Volatile
    private var isRecording = false

    @SuppressLint("MissingPermission")
    fun startRecording(onChunk: ((ByteArray) -> Unit)? = null) {
        if (isRecording) return

        val minBufferSize = AudioRecord.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        val bufferSize = if (minBufferSize > 0) Math.max(minBufferSize * 2, 4096) else 4096

        try {
            val record = AudioRecord(
                MediaRecorder.AudioSource.VOICE_RECOGNITION,
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize
            )

            if (record.state != AudioRecord.STATE_INITIALIZED) {
                Log.e("MYRA_Audio", "AudioRecord initialization failed (state: ${record.state})")
                record.release()
                return
            }

            audioRecord = record
            record.startRecording()
            isRecording = true

            Thread({
                val buffer = ByteArray(bufferSize)
                while (isRecording) {
                    val read = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                    if (read > 0) {
                        val chunk = buffer.copyOf(read)
                        // Calculate RMS audio level (16-bit signed PCM little-endian)
                        var sum = 0.0
                        for (i in 0 until read - 1 step 2) {
                            val sample = ((chunk[i + 1].toInt() shl 8) or (chunk[i].toInt() and 0xFF)).toShort()
                            sum += sample * sample
                        }
                        val sampleCount = read / 2.0
                        val rms = if (sampleCount > 0) Math.sqrt(sum / sampleCount) else 0.0
                        val level = (rms / 32768.0).toFloat().coerceIn(0f, 1f)

                        onChunkRecorded?.invoke(chunk, level)
                        onChunk?.invoke(chunk)
                    }
                }
            }, "MYRA-AudioRecorder-Thread").start()
        } catch (e: Exception) {
            Log.e("MYRA_Audio", "Failed to start AudioRecord: ${e.message}")
            stopRecording()
        }
    }

    fun stopRecording() {
        isRecording = false
        try {
            if (audioRecord?.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                audioRecord?.stop()
            }
            audioRecord?.release()
            audioRecord = null
        } catch (e: Exception) {
            Log.w("MYRA_Audio", "Error stopping AudioRecord: ${e.message}")
        }
    }

    fun release() {
        stopRecording()
    }
}
