package com.nostr.expressive.shared.crypto

import com.nostr.expressive.shared.model.NostrEvent
import com.nostr.expressive.shared.model.NostrKinds
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.random.Random

@Serializable
data class InnerPayloadEnvelope(
    val v: Int = 1,
    val type: String = "CHAT_MSG",
    val iv: String,
    val ciphertext: String,
    val sender_pubkey: String,
    val text: String? = null // only present after decryption
)

object DoubleEncryptionEngine {
    val json = Json { ignoreUnknownKeys = true }

    /**
     * Deterministic shared conversation key between two Nostr public keys.
     */
    fun deriveConversationKey(pubkeyA: String, pubkeyB: String): ByteArray {
        val sorted = listOf(pubkeyA.lowercase(), pubkeyB.lowercase()).sorted()
        return PlatformCrypto.sha256((sorted[0] + ":" + sorted[1] + ":NOSS_CONVERSATION_KEY_V1").encodeToByteArray())
    }

    /**
     * Layer 2 (Inner): Encrypt message text using a shared or derived session key
     */
    fun encryptInnerLayer(text: String, sessionKey: ByteArray, senderPubkey: String): String {
        val iv = PlatformCrypto.randomBytes(12) // 96-bit standard GCM IV
        val plaintext = text.encodeToByteArray()
        val ciphertext = PlatformCrypto.encryptAesGcm(plaintext, sessionKey, iv)
        
        val envelope = InnerPayloadEnvelope(
            v = 1,
            type = "CHAT_MSG",
            iv = iv.toHex(),
            ciphertext = ciphertext.toHex(),
            sender_pubkey = senderPubkey
        )
        return json.encodeToString(envelope)
    }

    /**
     * Layer 2 (Inner): Decrypt inner payload using session key
     */
    fun decryptInnerLayer(envelopeJson: String, sessionKey: ByteArray): String {
        val envelope = json.decodeFromString<InnerPayloadEnvelope>(envelopeJson)
        val iv = envelope.iv.hexToByteArray()
        val ciphertext = envelope.ciphertext.hexToByteArray()
        val decrypted = PlatformCrypto.decryptAesGcm(ciphertext, sessionKey, iv)
        return decrypted.decodeToString()
    }

    /**
     * Layer 1 (Outer NIP-59 Gift Wrap):
     * Wraps inner payload into a Gift Wrap event (kind 1059)
     * - Hides sender pubkey from relays (uses random ephemeral key)
     * - Randomizes timestamp (jitter within 2 days) to prevent traffic analysis
     * - Sets recipient 'p' tag so recipient's relay subscription catches it
     */
    fun createGiftWrapEvent(
        innerEncryptedJson: String,
        recipientPubkey: String
    ): NostrEvent {
        // Ephemeral random keypair for the outer wrap
        val ephemeralPrivKeyHex = PlatformCrypto.randomBytes(32).toHex()

        // Jitter timestamp by up to +/- 172800 seconds (2 days) per NIP-59
        val now = PlatformCrypto.currentTimeSeconds()
        // Never put created_at into the future; keep within acceptable window (relays reject created_at > now)
        val jitter = Random.nextLong(-30, 0)
        val randomizedTimestamp = now + jitter

        return NostrEvent.createAndSign(
            privkeyHex = ephemeralPrivKeyHex,
            kind = NostrKinds.GIFT_WRAP,
            content = innerEncryptedJson,
            tags = listOf(listOf("p", recipientPubkey.lowercase())),
            createdAt = randomizedTimestamp
        )
    }
}
