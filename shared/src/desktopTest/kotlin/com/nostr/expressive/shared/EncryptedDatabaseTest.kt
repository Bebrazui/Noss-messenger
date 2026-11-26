package com.nostr.expressive.shared

import com.nostr.expressive.shared.crypto.PlatformCrypto
import com.nostr.expressive.shared.database.DatabaseStorage
import com.nostr.expressive.shared.database.LocalDatabase
import com.nostr.expressive.shared.repository.ChatMessage
import com.nostr.expressive.shared.repository.ChatSummary
import com.nostr.expressive.shared.repository.MessageDeliveryStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EncryptedDatabaseTest {

    @Test
    fun testDatabaseHardwareEncryptionOnDisk() {
        val dbKey = PlatformCrypto.randomBytes(32)
        val wrongKey = PlatformCrypto.randomBytes(32)

        val db = LocalDatabase(dbKey)
        val chat = ChatSummary(
            chatId = "test_chat_1",
            peerPubkey = "npub1test00000000000000000000000000000000000000000000000000",
            peerDisplayName = "Bob",
            peerAvatarUrl = null,
            lastMessage = "Secret classified text",
            lastTimestamp = 123456789L,
            unreadCount = 0
        )
        val msg = ChatMessage(
            id = "msg_test_1",
            chatId = "test_chat_1",
            senderPubkey = "npub1me",
            text = "Super top-secret unreadable database payload",
            timestamp = 123456789L,
            status = MessageDeliveryStatus.SENT,
            isOutgoing = true
        )

        db.saveChat(chat)
        db.insertMessage(msg)

        // Verify the raw bytes stored on disk are encrypted (do not contain plaintext)
        val rawEncryptedBytes = DatabaseStorage.loadEncryptedBytes()
        checkNotNull(rawEncryptedBytes)
        val rawString = rawEncryptedBytes.decodeToString()
        assertFalse(rawString.contains("Super top-secret"))
        assertFalse(rawString.contains("Bob"))

        // Re-open with the correct key - data must be recovered
        val reloadedDb = LocalDatabase(dbKey)
        val chats = reloadedDb.getChats()
        val messages = reloadedDb.getMessages("test_chat_1")

        assertEquals(1, chats.size)
        assertEquals("Bob", chats[0].peerDisplayName)
        assertEquals(1, messages.size)
        assertEquals("Super top-secret unreadable database payload", messages[0].text)

        // Attempt open with wrong key - must NOT crash, but must fail decryption and not expose data
        val tamperedDb = LocalDatabase(wrongKey)
        assertTrue(tamperedDb.getChats().isEmpty())
    }
}
