package com.nostr.expressive.shared.database

import com.nostr.expressive.shared.crypto.PlatformCrypto
import com.nostr.expressive.shared.crypto.hexToByteArray
import com.nostr.expressive.shared.crypto.toHex
import com.nostr.expressive.shared.repository.ChatMessage
import com.nostr.expressive.shared.repository.ChatSummary
import com.nostr.expressive.shared.repository.MessageDeliveryStatus
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class DatabaseState(
    val chats: List<ChatSummary> = emptyList(),
    val messages: Map<String, List<ChatMessage>> = emptyMap(),
    val channels: List<com.nostr.expressive.shared.repository.PublicChannel> = emptyList(),
    val channelMessages: Map<String, List<ChatMessage>> = emptyMap(),
    val userProfile: com.nostr.expressive.shared.repository.UserProfile? = null,
    val isProfileConfigured: Boolean = false,
    val lastSyncTimestamp: Long = 0L,
    val avatarBytesHex: String? = null,
    val bannerBytesHex: String? = null
)

class LocalDatabase(
    private val encryptionKey: ByteArray
) {
    private val json = Json { ignoreUnknownKeys = true }
    private var state: DatabaseState

    init {
        val encryptedData = DatabaseStorage.loadEncryptedBytes()
        state = if (encryptedData != null && encryptedData.size > 28) {
            try {
                val iv = encryptedData.copyOfRange(0, 12)
                val ciphertext = encryptedData.copyOfRange(12, encryptedData.size)
                val decrypted = PlatformCrypto.decryptAesGcm(ciphertext, encryptionKey, iv)
                json.decodeFromString<DatabaseState>(decrypted.decodeToString())
            } catch (e: Exception) {
                // Decryption failed or tampered data - fallback to fresh state
                println("Failed to decrypt local database (invalid key or corrupted file): ${e.message}")
                DatabaseState()
            }
        } else {
            DatabaseState()
        }
        if (state.isProfileConfigured) {
            persist()
        }
    }

    fun getChats(): List<ChatSummary> = state.chats

    fun getMessages(chatId: String): List<ChatMessage> = state.messages[chatId] ?: emptyList()

    fun getAllMessages(): Map<String, List<ChatMessage>> = state.messages

    fun saveChat(chat: ChatSummary) {
        val filtered = state.chats.filter { it.chatId != chat.chatId }
        state = state.copy(chats = listOf(chat) + filtered)
        persist()
    }

    fun insertMessage(message: ChatMessage) {
        val currentList = state.messages[message.chatId] ?: emptyList()
        // Deduplicate by message ID
        if (currentList.none { it.id == message.id }) {
            val updatedList = currentList + message
            state = state.copy(messages = state.messages + (message.chatId to updatedList))
            persist()
        }
    }

    fun updateMessageStatus(chatId: String, messageId: String, status: MessageDeliveryStatus) {
        val currentList = state.messages[chatId] ?: return
        val updatedList = currentList.map { msg ->
            if (msg.id == messageId) msg.copy(status = status) else msg
        }
        state = state.copy(messages = state.messages + (chatId to updatedList))
        persist()
    }

    fun clearChat(chatId: String) {
        state = state.copy(messages = state.messages + (chatId to emptyList()))
        val updatedChats = state.chats.map {
            if (it.chatId == chatId) it.copy(lastMessage = "", unreadCount = 0) else it
        }
        state = state.copy(chats = updatedChats)
        persist()
    }

    fun deleteChat(chatId: String) {
        state = state.copy(
            chats = state.chats.filter { it.chatId != chatId },
            messages = state.messages - chatId
        )
        persist()
    }

    fun deleteMessage(chatId: String, messageId: String) {
        val currentList = state.messages[chatId] ?: return
        val updatedList = currentList.filter { it.id != messageId }
        val lastMsg = updatedList.lastOrNull()?.let {
            if (it.text.startsWith("nostr:media:")) "📷 Файл" else it.text
        } ?: ""
        val lastTs = updatedList.lastOrNull()?.timestamp ?: 0L
        state = state.copy(
            messages = state.messages + (chatId to updatedList),
            chats = state.chats.map {
                if (it.chatId == chatId) it.copy(lastMessage = lastMsg, lastTimestamp = if (lastTs > 0) lastTs else it.lastTimestamp) else it
            }
        )
        persist()
    }

    fun getChannels(): List<com.nostr.expressive.shared.repository.PublicChannel> = state.channels

    fun getChannelMessages(channelId: String): List<ChatMessage> = state.channelMessages[channelId] ?: emptyList()

    fun getAllChannelMessages(): Map<String, List<ChatMessage>> = state.channelMessages

    fun saveChannel(channel: com.nostr.expressive.shared.repository.PublicChannel) {
        val filtered = state.channels.filter { it.id != channel.id }
        state = state.copy(channels = listOf(channel) + filtered)
        persist()
    }

    fun insertChannelMessage(channelId: String, message: ChatMessage) {
        val currentList = state.channelMessages[channelId] ?: emptyList()
        if (currentList.none { it.id == message.id }) {
            val updatedList = currentList + message
            state = state.copy(channelMessages = state.channelMessages + (channelId to updatedList))
            persist()
        }
    }

    fun getLastSyncTimestamp(): Long = state.lastSyncTimestamp

    fun setLastSyncTimestamp(ts: Long) {
        state = state.copy(lastSyncTimestamp = ts)
        persist()
    }

    fun getUserProfile(): com.nostr.expressive.shared.repository.UserProfile? = state.userProfile

    fun getAvatarBytes(): ByteArray? = state.avatarBytesHex?.let { try { it.hexToByteArray() } catch (_: Throwable) { null } }
    fun getBannerBytes(): ByteArray? = state.bannerBytesHex?.let { try { it.hexToByteArray() } catch (_: Throwable) { null } }

    fun saveMediaBytes(avatar: ByteArray? = null, banner: ByteArray? = null) {
        state = state.copy(
            avatarBytesHex = avatar?.toHex() ?: state.avatarBytesHex,
            bannerBytesHex = banner?.toHex() ?: state.bannerBytesHex
        )
        persist()
    }

    fun isProfileConfigured(): Boolean = state.isProfileConfigured

    fun saveUserProfile(profile: com.nostr.expressive.shared.repository.UserProfile, isConfigured: Boolean = true) {
        state = state.copy(userProfile = profile, isProfileConfigured = isConfigured)
        persist()
    }

    private fun persist() {
        try {
            val serialized = json.encodeToString(state).encodeToByteArray()
            val iv = PlatformCrypto.randomBytes(12)
            val ciphertext = PlatformCrypto.encryptAesGcm(serialized, encryptionKey, iv)
            val payload = iv + ciphertext
            DatabaseStorage.saveEncryptedBytes(payload)
        } catch (e: Exception) {
            println("Error persisting encrypted database: ${e.message}")
        }
    }
}
