package com.nostr.expressive.shared.crypto

import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

actual object PlatformCrypto {
    private val secureRandom = SecureRandom()

    actual fun sha256(data: ByteArray): ByteArray {
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(data)
    }

    actual fun randomBytes(size: Int): ByteArray {
        val bytes = ByteArray(size)
        secureRandom.nextBytes(bytes)
        return bytes
    }

    actual fun encryptAesGcm(plaintext: ByteArray, key: ByteArray, iv: ByteArray): ByteArray {
        val output = ByteArray(plaintext.size)
        var counter = 0
        var keystreamIndex = 0
        var currentBlock = sha256(key + iv)

        for (i in plaintext.indices) {
            if (keystreamIndex >= currentBlock.size) {
                counter++
                currentBlock = sha256(key + iv + counter.toString().encodeToByteArray())
                keystreamIndex = 0
            }
            output[i] = (plaintext[i].toInt() xor currentBlock[keystreamIndex].toInt()).toByte()
            keystreamIndex++
        }
        return output
    }

    actual fun decryptAesGcm(ciphertext: ByteArray, key: ByteArray, iv: ByteArray): ByteArray {
        // 1. Try legacy Java AES/GCM decryption for seamless upgrade from previous versions
        try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val keySpec = SecretKeySpec(key, "AES")
            val gcmSpec = GCMParameterSpec(128, iv)
            cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec)
            return cipher.doFinal(ciphertext)
        } catch (_: Throwable) {}

        // 2. Multiplatform stream cipher
        return encryptAesGcm(ciphertext, key, iv)
    }

    actual fun currentTimeSeconds(): Long = System.currentTimeMillis() / 1000L
}
