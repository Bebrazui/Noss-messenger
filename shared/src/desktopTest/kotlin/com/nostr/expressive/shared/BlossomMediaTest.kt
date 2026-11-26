package com.nostr.expressive.shared

import com.nostr.expressive.shared.media.BlossomMediaManager
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BlossomMediaTest {

    @Test
    fun testClientSideMediaEncryptionAndDecryption() {
        val sampleImageText = "PNG_OR_WEBP_BINARY_DATA_ENCRYPTED_FOR_BLOSSOM_STORAGE"
        val rawBytes = sampleImageText.encodeToByteArray()

        // 1. Encrypt raw media bytes locally
        val (ciphertext, attachmentDraft) = BlossomMediaManager.encryptMediaBytes(rawBytes, "image/webp")
        val attachment = attachmentDraft.copy(url = "https://blossom.nostr.build/test_blob")

        // Verify ciphertext does not contain plaintext data
        assertFalse(ciphertext.decodeToString().contains(sampleImageText))
        assertEquals(rawBytes.size.toLong(), attachment.sizeBytes)

        // 2. Format as chat message payload
        val formattedMessage = BlossomMediaManager.formatMediaMessage(attachment)
        assertTrue(BlossomMediaManager.isMediaMessage(formattedMessage))

        // 3. Parse back on receiver end
        val parsedAttachment = BlossomMediaManager.parseMediaAttachment(formattedMessage)
        checkNotNull(parsedAttachment)
        assertEquals(attachment.url, parsedAttachment.url)
        assertEquals(attachment.keyHex, parsedAttachment.keyHex)

        // 4. Decrypt received blob using attachment key and IV
        val decryptedBytes = BlossomMediaManager.decryptMediaBytes(
            ciphertext = ciphertext,
            keyHex = parsedAttachment.keyHex,
            ivHex = parsedAttachment.ivHex
        )
        assertEquals(sampleImageText, decryptedBytes.decodeToString())
    }
}
