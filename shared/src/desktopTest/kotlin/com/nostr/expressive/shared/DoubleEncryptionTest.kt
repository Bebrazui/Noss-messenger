package com.nostr.expressive.shared

import com.nostr.expressive.shared.crypto.DoubleEncryptionEngine
import com.nostr.expressive.shared.crypto.PlatformCrypto
import com.nostr.expressive.shared.model.NostrKinds
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DoubleEncryptionTest {

    @Test
    fun testInnerAndOuterEncryptionPipeline() {
        val testSessionKey = PlatformCrypto.randomBytes(32)
        val myPubkey = "npub1sender_test_key_00000000000000000000000000000000000000"
        val recipientPubkey = "npub1recipient_test_key_00000000000000000000000000000000000"
        val originalMessage = "Hello from Nostr with Double Encryption & M3 Expressive!"

        // 1. Layer 2: Encrypt inner payload
        val innerEncryptedJson = DoubleEncryptionEngine.encryptInnerLayer(
            text = originalMessage,
            sessionKey = testSessionKey,
            senderPubkey = myPubkey
        )
        assertTrue(innerEncryptedJson.contains("ciphertext"))
        assertTrue(innerEncryptedJson.contains("iv"))

        // 2. Layer 1: Wrap in NIP-59 Gift Wrap
        val giftWrapEvent = DoubleEncryptionEngine.createGiftWrapEvent(
            innerEncryptedJson = innerEncryptedJson,
            recipientPubkey = recipientPubkey
        )
        assertEquals(NostrKinds.GIFT_WRAP, giftWrapEvent.kind)
        assertTrue(giftWrapEvent.pubkey.isNotEmpty())
        assertEquals(listOf(listOf("p", recipientPubkey)), giftWrapEvent.tags)

        // 3. Decrypt Inner Payload
        val decryptedText = DoubleEncryptionEngine.decryptInnerLayer(
            envelopeJson = giftWrapEvent.content,
            sessionKey = testSessionKey
        )
        assertEquals(originalMessage, decryptedText)
    }
}
