package com.myra.assistant.voice

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AudioTrackPlayer(private val sampleRate: Int = 24000) {
    private var audioTrack: AudioTrack? = null

    init {
        val minBufferSize = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )

        audioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANT)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(minBufferSize * 2)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()

        audioTrack?.play()
    }

    suspend fun writeChunk(pcmData: ByteArray) = withContext(Dispatchers.IO) {
        audioTrack?.write(pcmData, 0, pcmData.size)
    }

    fun playPcm(pcmData: ByteArray) {
        try {
            audioTrack?.write(pcmData, 0, pcmData.size)
        } catch (e: Exception) {}
    }

    fun stop() {
        try {
            audioTrack?.pause()
            audioTrack?.flush()
        } catch (e: Exception) {}
    }

    fun release() {
        try {
            audioTrack?.stop()
            audioTrack?.release()
            audioTrack = null
        } catch (e: Exception) {}
    }
}
