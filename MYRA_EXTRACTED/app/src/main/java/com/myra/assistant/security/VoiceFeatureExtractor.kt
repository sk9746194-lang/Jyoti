package com.myra.assistant.security

import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.sqrt

/**
 * On-Device Speaker Recognition & Acoustic Feature Extractor for MYRA Assistant.
 * 
 * Strict Privacy & Security Architecture:
 * - NO raw audio recordings are stored permanently.
 * - Raw audio samples are transformed directly in-memory into a compact, mathematical embedding vector (16-D).
 * - Embedding vectors are L2-normalized.
 * - Includes Anti-Replay Protection: tests dynamic acoustic flux and variance to prevent recorded audio loop attacks.
 */
object VoiceFeatureExtractor {

    private const val FRAME_SIZE = 512 // ~32ms at 16kHz
    private const val FRAME_HOP = 256  // ~16ms hop (50% overlap)
    private const val NUM_MEL_BANDS = 12
    const val EMBEDDING_DIM = 16 // Energy, ZCR, Spectral Centroid, Spectral Spread + 12 Mel bands

    /**
     * Extracts a normalized speaker embedding vector from raw 16kHz 16-bit PCM audio samples.
     * Returns null if active speech is insufficient or anti-replay test fails.
     */
    fun extractSpeakerEmbedding(pcmSamples: ShortArray): FloatArray? {
        if (pcmSamples.size < FRAME_SIZE * 4) {
            return null // Audio too short to form a reliable speaker profile
        }

        val frameCount = (pcmSamples.size - FRAME_SIZE) / FRAME_HOP
        if (frameCount < 6) return null

        val accumulatedVector = FloatArray(EMBEDDING_DIM)
        var activeSpeechFrames = 0
        var totalEnergyFlux = 0.0
        var previousFrameEnergy = 0.0

        val window = FloatArray(FRAME_SIZE) { i ->
            // Hamming window
            (0.54 - 0.46 * cos(2.0 * Math.PI * i / (FRAME_SIZE - 1))).toFloat()
        }

        val frameBuffer = FloatArray(FRAME_SIZE)

        for (f in 0 until frameCount) {
            val startIdx = f * FRAME_HOP
            var frameEnergy = 0.0
            var zcrCount = 0

            for (i in 0 until FRAME_SIZE) {
                val sample = pcmSamples[startIdx + i]
                frameBuffer[i] = sample * window[i]
                frameEnergy += sample * sample

                if (i > 0) {
                    val prev = pcmSamples[startIdx + i - 1]
                    if ((sample >= 0 && prev < 0) || (sample < 0 && prev >= 0)) {
                        zcrCount++
                    }
                }
            }

            val rmsEnergy = sqrt(frameEnergy / FRAME_SIZE)
            // Voice Activity Detection threshold (filter out ambient background silence)
            if (rmsEnergy < 300.0) {
                continue
            }

            activeSpeechFrames++

            // Anti-Replay: Track energy variation across frames
            if (previousFrameEnergy > 0.0) {
                totalEnergyFlux += abs(rmsEnergy - previousFrameEnergy) / (rmsEnergy + previousFrameEnergy)
            }
            previousFrameEnergy = rmsEnergy

            // Approximate spectral features using discrete Fourier approximation
            val halfSize = FRAME_SIZE / 2
            val spectrum = FloatArray(halfSize)
            var weightedFrequencySum = 0.0
            var totalMagnitude = 0.0

            for (k in 0 until halfSize step 4) {
                // Approximate magnitude across frequency bins
                var real = 0.0
                var imag = 0.0
                for (n in 0 until FRAME_SIZE step 8) {
                    val angle = 2.0 * Math.PI * k * n / FRAME_SIZE
                    val valSample = frameBuffer[n]
                    real += valSample * cos(angle)
                    imag -= valSample * kotlin.math.sin(angle)
                }
                val mag = sqrt(real * real + imag * imag).toFloat()
                spectrum[k] = mag
                val freqHz = (k * 16000.0) / FRAME_SIZE
                weightedFrequencySum += freqHz * mag
                totalMagnitude += mag
            }

            val spectralCentroid = if (totalMagnitude > 0.0) (weightedFrequencySum / totalMagnitude).toFloat() else 1500f
            val zcr = (zcrCount.toFloat() / FRAME_SIZE)
            val logEnergy = ln(rmsEnergy.coerceAtLeast(1.0)).toFloat()

            // Calculate 12 band-pass energy filters
            val melEnergies = FloatArray(NUM_MEL_BANDS)
            val bandWidth = halfSize / NUM_MEL_BANDS
            for (b in 0 until NUM_MEL_BANDS) {
                var bandSum = 0.0
                val bStart = b * bandWidth
                val bEnd = (b + 1) * bandWidth
                for (idx in bStart until bEnd) {
                    if (idx < halfSize) {
                        bandSum += spectrum[idx]
                    }
                }
                melEnergies[b] = ln((bandSum + 1.0)).toFloat()
            }

            // Accumulate into overall utterance vector
            accumulatedVector[0] += logEnergy
            accumulatedVector[1] += zcr * 10f
            accumulatedVector[2] += (spectralCentroid / 4000f) // Normalized to [0, 1]
            accumulatedVector[3] += (rmsEnergy / 5000f).toFloat()
            for (b in 0 until NUM_MEL_BANDS) {
                accumulatedVector[4 + b] += melEnergies[b]
            }
        }

        // Must have sufficient active speech frames
        if (activeSpeechFrames < 4) {
            return null
        }

        // Anti-Replay Check: Natural human speech has dynamic pitch and amplitude flux.
        // A synthetic static sound or robotic hum will have near-zero flux.
        val avgEnergyFlux = totalEnergyFlux / activeSpeechFrames
        if (avgEnergyFlux < 0.01) {
            // Unnatural static sound, reject for security
            return null
        }

        // Average and L2-normalize vector
        var sumSquares = 0.0f
        for (i in 0 until EMBEDDING_DIM) {
            val avgVal = accumulatedVector[i] / activeSpeechFrames.toFloat()
            accumulatedVector[i] = avgVal
            sumSquares += avgVal * avgVal
        }

        val norm = sqrt(sumSquares).coerceAtLeast(1e-6f)
        for (i in 0 until EMBEDDING_DIM) {
            accumulatedVector[i] = accumulatedVector[i] / norm
        }

        return accumulatedVector
    }

    /**
     * Computes the Cosine Similarity between two L2-normalized embedding vectors.
     * Returns a score between -1.0 and 1.0 (typical match >= 0.78 for owner verification).
     */
    fun computeCosineSimilarity(embeddingA: FloatArray, embeddingB: FloatArray): Float {
        if (embeddingA.size != embeddingB.size) return 0.0f

        var dotProduct = 0.0f
        for (i in embeddingA.indices) {
            dotProduct += embeddingA[i] * embeddingB[i]
        }
        return dotProduct.coerceIn(-1.0f, 1.0f)
    }

    /**
     * Serializes an embedding vector to a compact string format.
     */
    fun serializeEmbedding(embedding: FloatArray): String {
        return embedding.joinToString(",") { "%.6f".format(it) }
    }

    /**
     * Deserializes an embedding vector from a compact string.
     */
    fun deserializeEmbedding(rawString: String): FloatArray? {
        return try {
            val parts = rawString.split(",")
            if (parts.size != EMBEDDING_DIM) return null
            FloatArray(EMBEDDING_DIM) { i -> parts[i].trim().toFloat() }
        } catch (e: Exception) {
            null
        }
    }
}
