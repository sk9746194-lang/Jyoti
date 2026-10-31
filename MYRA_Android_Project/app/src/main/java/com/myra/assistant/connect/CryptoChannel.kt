package com.myra.assistant.connect

import android.util.Base64
import java.io.InputStream
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Provides authenticated, tamper-resistant encryption for local phone-to-phone communication
 * using AES-256-GCM. Ensures all screen frames, remote touch commands, and file chunks
 * are encrypted and verified over local Wi-Fi / Hotspot sockets.
 */
object CryptoChannel {

    private const val GCM_IV_LENGTH = 12 // 96 bits recommended for GCM
    private const val GCM_TAG_LENGTH = 128 // 128-bit authentication tag
    private val secureRandom = SecureRandom()

    /**
     * Derives a deterministic 256-bit AES key from the mutual authentication token and shared secret.
     */
    fun deriveKey(authToken: String, sharedSecret: String = "myra_hotspot_salt"): SecretKeySpec {
        val digest = MessageDigest.getInstance("SHA-256")
        val combined = "$authToken:$sharedSecret".toByteArray(Charsets.UTF_8)
        val keyBytes = digest.digest(combined)
        return SecretKeySpec(keyBytes, "AES")
    }

    /**
     * Encrypts plaintext string using AES-256-GCM.
     * Returns a colon-separated string: "ivBase64:ciphertextBase64"
     */
    fun encrypt(plainText: String, secretKey: SecretKeySpec): String {
        val iv = ByteArray(GCM_IV_LENGTH)
        secureRandom.nextBytes(iv)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, spec)

        val cipherText = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
        val ivB64 = Base64.encodeToString(iv, Base64.NO_WRAP)
        val dataB64 = Base64.encodeToString(cipherText, Base64.NO_WRAP)

        return "$ivB64:$dataB64"
    }

    /**
     * Decrypts an authenticated AES-256-GCM envelope.
     * Throws an exception if tampered or key is incorrect.
     */
    fun decrypt(encryptedEnvelope: String, secretKey: SecretKeySpec): String {
        val parts = encryptedEnvelope.split(":")
        if (parts.size != 2) throw IllegalArgumentException("Malformed encrypted envelope")

        val iv = Base64.decode(parts[0], Base64.NO_WRAP)
        val cipherBytes = Base64.decode(parts[1], Base64.NO_WRAP)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)

        val plainBytes = cipher.doFinal(cipherBytes)
        return String(plainBytes, Charsets.UTF_8)
    }

    /**
     * Computes the SHA-256 checksum of an input stream for file transfer integrity verification.
     */
    fun calculateSha256(inputStream: InputStream): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(8192)
        var bytesRead: Int
        while (inputStream.read(buffer).also { bytesRead = it } != -1) {
            digest.update(buffer, 0, bytesRead)
        }
        val hashBytes = digest.digest()
        return hashBytes.joinToString("") { "%02x".format(it) }
    }
}
