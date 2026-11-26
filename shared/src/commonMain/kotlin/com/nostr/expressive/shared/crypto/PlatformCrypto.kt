package com.nostr.expressive.shared.crypto

expect object PlatformCrypto {
    fun sha256(data: ByteArray): ByteArray
    fun randomBytes(size: Int): ByteArray
    fun encryptAesGcm(plaintext: ByteArray, key: ByteArray, iv: ByteArray): ByteArray
    fun decryptAesGcm(ciphertext: ByteArray, key: ByteArray, iv: ByteArray): ByteArray
    fun currentTimeSeconds(): Long
}

fun ByteArray.toHex(): String = joinToString("") { (it.toInt() and 0xFF).toString(16).padStart(2, '0') }

fun String.hexToByteArray(): ByteArray {
    check(length % 2 == 0) { "Must have an even length" }
    return chunked(2)
        .map { it.toInt(16).toByte() }
        .toByteArray()
}
