package com.nostr.expressive.shared.crypto

expect object Secp256k1 {
    /**
     * Derives a 32-byte (64 hex characters) x-only public key from a 32-byte private key hex.
     * Compliant with BIP-340 / Nostr NIP-01.
     */
    fun getPublicKey(privKeyHex: String): String

    /**
     * Computes a 64-byte (128 hex characters) Schnorr signature over a 32-byte message hash.
     * Compliant with BIP-340 / Nostr NIP-01.
     */
    fun signSchnorr(
        msgHashHex: String,
        privKeyHex: String,
        auxHex: String = "0000000000000000000000000000000000000000000000000000000000000000"
    ): String

    /**
     * Verifies a 64-byte Schnorr signature over a 32-byte message hash using a 32-byte public key.
     */
    fun verifySchnorr(msgHashHex: String, pubKeyHex: String, sigHex: String): Boolean
}
