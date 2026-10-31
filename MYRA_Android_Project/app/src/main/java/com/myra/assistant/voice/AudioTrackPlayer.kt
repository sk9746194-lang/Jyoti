package com.myra.assistant.voice

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AudioTrackPlayer(private val sampleRate: Int = 24000) {
    private var audioTrack: AudioTrack? = null
    private val trackLock = Any()

    private fun getOrCreateTrack(): AudioTrack? {
        synchronized(trackLock) {
            if (audioTrack != null && audioTrack?.state == AudioTrack.STATE_INITIALIZED) {
                if (audioTrack?.playState != AudioTrack.PLAYSTATE_PLAYING) {
                    try { audioTrack?.play() } catch (_: Exception) {}
                }
                return audioTrack
            }
            return try {
                val minBufferSize = AudioTrack.getMinBufferSize(
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                )
                val bufferSize = if (minBufferSize > 0) Math.max(minBufferSize * 2, 4096) else 4096
                val track = AudioTrack.Builder()
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
                    .setBufferSizeInBytes(bufferSize)
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build()

                track.play()
                audioTrack = track
                track
            } catch (e: Exception) {
                Log.e("MYRA_AudioTrack", "Error creating AudioTrack: ${e.message}")
                null
            }
        }
    }

    suspend fun writeChunk(pcmData: ByteArray) = withContext(Dispatchers.IO) {
        try {
            getOrCreateTrack()?.write(pcmData, 0, pcmData.size)
        } catch (e: Exception) {
            Log.w("MYRA_AudioTrack", "Error writing chunk: ${e.message}")
        }
    }

    fun playPcm(pcmData: ByteArray) {
        try {
            getOrCreateTrack()?.write(pcmData, 0, pcmData.size)
        } catch (e: Exception) {}
    }

    fun stop() {
        synchronized(trackLock) {
            try {
                audioTrack?.pause()
                audioTrack?.flush()
            } catch (e: Exception) {}
        }
    }

    fun release() {
        synchronized(trackLock) {
            try {
                audioTrack?.stop()
                audioTrack?.release()
                audioTrack = null
            } catch (e: Exception) {}
        }
    }
}
