package com.nostr.expressive.shared.repository

import com.nostr.expressive.shared.crypto.DoubleEncryptionEngine
import com.nostr.expressive.shared.crypto.Nip19
import com.nostr.expressive.shared.crypto.PlatformCrypto
import com.nostr.expressive.shared.crypto.toHex
import com.nostr.expressive.shared.database.LocalDatabase
import com.nostr.expressive.shared.media.BlossomMediaManager
import com.nostr.expressive.shared.model.NostrKinds
import com.nostr.expressive.shared.network.NostrRelayPool
import com.nostr.expressive.shared.protocol.Nip01Profile
import com.nostr.expressive.shared.protocol.Nip28
import com.nostr.expressive.shared.protocol.ProfileMetadata
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

enum class MessageDeliveryStatus {
    SENDING,
    SENT,
    DELIVERED,
    READ,
    FAILED
}

@Serializable
data class ChatMessage(
    val id: String,
    val chatId: String,
    val senderPubkey: String,
    val text: String,
    val timestamp: Long,
    val status: MessageDeliveryStatus,
    val isOutgoing: Boolean
)

@Serializable
data class ChatSummary(
    val chatId: String,
    val peerPubkey: String,
    val peerDisplayName: String,
    val peerAvatarUrl: String?,
    val lastMessage: String,
    val lastTimestamp: Long,
    val unreadCount: Int = 0
)

@Serializable
data class PublicChannel(
    val id: String,
    val name: String,
    val about: String,
    val pictureUrl: String? = null,
    val creatorPubkey: String
)

@Serializable
data class UserProfile(
    val pubkey: String,
    val name: String,
    val displayName: String,
    val about: String,
    val picture: String? = null,
    val banner: String? = null,
    val nip05: String? = null,
    val lud16: String? = null
)

@Serializable
data class NossUser(
    val pubkeyHex: String,
    val npub: String,
    val name: String,
    val displayName: String,
    val about: String = "",
    val picture: String? = null
)

sealed class UsernameCheckResult {
    object Idle : UsernameCheckResult()
    object Checking : UsernameCheckResult()
    data class Available(val username: String) : UsernameCheckResult()
    data class Taken(val username: String, val ownerPubkey: String) : UsernameCheckResult()
    data class Invalid(val reason: String) : UsernameCheckResult()
}

class MessengerRepository(
    val myPubkey: String,
    val myPrivkeyHex: String = "",
    private val sessionKey: ByteArray,
    dbKey: ByteArray,
    private val relayPool: NostrRelayPool = NostrRelayPool()
) {
    private val localDb = LocalDatabase(dbKey)
    private val scope = CoroutineScope(Dispatchers.Default + Job())

    private val _chats = MutableStateFlow<List<ChatSummary>>(emptyList())
    val chats: StateFlow<List<ChatSummary>> = _chats.asStateFlow()

    private val _messages = MutableStateFlow<Map<String, List<ChatMessage>>>(emptyMap())
    val messages: StateFlow<Map<String, List<ChatMessage>>> = _messages.asStateFlow()

    private val _channels = MutableStateFlow<List<PublicChannel>>(emptyList())
    val channels: StateFlow<List<PublicChannel>> = _channels.asStateFlow()

    private val _channelMessages = MutableStateFlow<Map<String, List<ChatMessage>>>(emptyMap())
    val channelMessages: StateFlow<Map<String, List<ChatMessage>>> = _channelMessages.asStateFlow()

    private val _isProfileConfigured = MutableStateFlow(localDb.isProfileConfigured())
    val isProfileConfigured: StateFlow<Boolean> = _isProfileConfigured.asStateFlow()

    private val knownUsernames = mutableMapOf<String, String>() // lowercase name -> pubkeyHex

    private val _communityUsers = MutableStateFlow<List<NossUser>>(emptyList())
    val communityUsers: StateFlow<List<NossUser>> = _communityUsers.asStateFlow()

    fun searchNossUsers(query: String): List<NossUser> {
        val clean = query.trim().removePrefix("@").lowercase()
        if (clean.isBlank()) return emptyList()

        // If query is an npub1... or 64-char hex pubkey, resolve and fetch immediately
        if (clean.startsWith("npub1") || (clean.length == 64 && clean.all { it in "0123456789abcdef" })) {
            val pubkeyHex = if (clean.startsWith("npub1")) {
                try { Nip19.parseNpub(clean).lowercase() } catch (_: Exception) { clean }
            } else clean

            fetchUserProfile(pubkeyHex)
            val existing = _communityUsers.value.find { it.pubkeyHex.equals(pubkeyHex, ignoreCase = true) }
            if (existing != null) return listOf(existing)

            val npub = try { Nip19.toNpub(pubkeyHex) } catch (_: Exception) { pubkeyHex }
            val placeholder = NossUser(
                pubkeyHex = pubkeyHex,
                npub = npub,
                name = "user_${pubkeyHex.take(6)}",
                displayName = "User ${pubkeyHex.take(6)}",
                about = "Nostr User"
            )
            if (_communityUsers.value.none { it.pubkeyHex.equals(pubkeyHex, ignoreCase = true) }) {
                _communityUsers.value = _communityUsers.value + placeholder
            }
            return listOf(placeholder)
        }

        // Trigger NIP-50 search on relays
        val searchSubId = "search_${clean.take(8)}"
        relayPool.subscribe(searchSubId, listOf(NostrKinds.METADATA), search = clean, limit = 20)

        return _communityUsers.value.filter { user ->
            user.name.lowercase().contains(clean) ||
            user.displayName.lowercase().contains(clean) ||
            user.npub.lowercase().contains(clean) ||
            user.pubkeyHex.lowercase().contains(clean)
        }
    }

    private val _userProfile = MutableStateFlow(
        localDb.getUserProfile() ?: UserProfile(
            pubkey = myPubkey,
            name = "nostr_user",
            displayName = "Anonymous Nostr",
            about = "Hardware encrypted database + M3 Expressive"
        )
    )
    val userProfile: StateFlow<UserProfile> = _userProfile.asStateFlow()

    val localAvatarBytes = MutableStateFlow<ByteArray?>(localDb.getAvatarBytes())
    val localBannerBytes = MutableStateFlow<ByteArray?>(localDb.getBannerBytes())

    fun setLocalAvatarBytes(bytes: ByteArray?) {
        localAvatarBytes.value = bytes
        localDb.saveMediaBytes(avatar = bytes)
    }

    fun setLocalBannerBytes(bytes: ByteArray?) {
        localBannerBytes.value = bytes
        localDb.saveMediaBytes(banner = bytes)
    }

    init {
        // Load initial state from encrypted persistent local database (purging legacy mock peer_satoshi)
        val savedChats = localDb.getChats().filter { it.chatId != "peer_satoshi" }
        val savedMessages = localDb.getAllMessages().filterKeys { it != "peer_satoshi" }
        val savedChannels = localDb.getChannels()
        val savedChanMsgs = localDb.getAllChannelMessages()

        _chats.value = savedChats
        _messages.value = savedMessages

        val defaultChannels = listOf(
            PublicChannel(
                id = "chan_expressive_dev",
                name = "Nostr & Expressive UI",
                about = "Official public channel for discussion of decentralized messengers and M3 Expressive.",
                pictureUrl = null,
                creatorPubkey = myPubkey
            ),
            PublicChannel(
                id = "chan_bitcoin",
                name = "Bitcoin & Lightning",
                about = "Global censorship-resistant peer-to-peer digital currency channel.",
                pictureUrl = null,
                creatorPubkey = myPubkey
            ),
            PublicChannel(
                id = "chan_nostr_community",
                name = "Nostr Global Hub",
                about = "Decentralized social networking and zero-server communications.",
                pictureUrl = null,
                creatorPubkey = myPubkey
            )
        )

        val mergedChannels = if (savedChannels.size >= 3) savedChannels else (savedChannels + defaultChannels).distinctBy { it.id }
        _channels.value = mergedChannels
        _channelMessages.value = savedChanMsgs

        relayPool.start()
        relayPool.subscribe("sub_inbox", listOf(NostrKinds.GIFT_WRAP), pTags = listOf(myPubkey))
        relayPool.subscribe("sub_public_channels", listOf(NostrKinds.CHANNEL_CREATE))
        relayPool.subscribe("sub_metadata", listOf(NostrKinds.METADATA), limit = 50)

        // Request live profile metadata for all existing chats from relays
        savedChats.forEach { chat ->
            fetchUserProfile(chat.peerPubkey)
        }

        scope.launch {
            relayPool.eventsFlow.collect { event ->
                handleIncomingEvent(event)
            }
        }
    }

    fun fetchUserProfile(pubkeyHex: String) {
        val clean = pubkeyHex.trim().lowercase()
        if (clean.isBlank()) return
        relayPool.subscribe(
            subId = "sub_user_${clean.take(12)}",
            kinds = listOf(NostrKinds.METADATA),
            authors = listOf(clean),
            limit = 1
        )
    }

    fun updateAndPublishProfile(
        displayName: String,
        name: String,
        about: String,
        picture: String? = null,
        banner: String? = null,
        nip05: String? = null,
        lud16: String? = null
    ) {
        val meta = ProfileMetadata(
            name = name,
            display_name = displayName,
            about = about,
            picture = picture,
            banner = banner,
            nip05 = nip05,
            lud16 = lud16
        )
        val event = Nip01Profile.buildProfileEvent(meta, myPubkey, myPrivkeyHex)
        
        val updated = UserProfile(
            pubkey = myPubkey,
            name = name,
            displayName = displayName,
            about = about,
            picture = picture,
            banner = banner,
            nip05 = nip05,
            lud16 = lud16
        )

        // Update local state and persistent encrypted database
        _userProfile.value = updated
        localDb.saveUserProfile(updated, isConfigured = true)
        _isProfileConfigured.value = true
        knownUsernames[name.trim().lowercase()] = myPubkey

        // Broadcast Kind 0 with real Schnorr signature to all relays
        relayPool.broadcastEvent(event)
    }

    suspend fun checkUsernameAvailability(rawUsername: String): UsernameCheckResult {
        val clean = rawUsername.trim().removePrefix("@").lowercase()
        if (clean.length < 3) {
            return UsernameCheckResult.Invalid("At least 3 characters required")
        }
        if (clean.length > 20) {
            return UsernameCheckResult.Invalid("Max 20 characters allowed")
        }
        val validRegex = Regex("^[a-z0-9_.]+$")
        if (!clean.matches(validRegex)) {
            return UsernameCheckResult.Invalid("Letters, numbers, underscores and dots only")
        }

        // If it's already our current handle, it's available
        if (clean == _userProfile.value.name.trim().lowercase()) {
            return UsernameCheckResult.Available(clean)
        }

        // Check local cache
        val cachedOwner = knownUsernames[clean]
        if (cachedOwner != null && !cachedOwner.equals(myPubkey, ignoreCase = true)) {
            return UsernameCheckResult.Taken(clean, cachedOwner)
        }

        // Probe relays with search filter for this username
        val probeSubId = "probe_user_${PlatformCrypto.randomBytes(4).toHex()}"
        relayPool.subscribe(
            subId = probeSubId,
            kinds = listOf(NostrKinds.METADATA),
            search = clean,
            limit = 20
        )

        var takenBy: String? = null
        val probeJob = scope.launch {
            relayPool.eventsFlow.collect { event ->
                if (event.kind == NostrKinds.METADATA) {
                    try {
                        val meta = Nip01Profile.parseMetadata(event.content)
                        val candidateName = meta.name?.trim()?.lowercase()
                        if (candidateName != null) {
                            knownUsernames[candidateName] = event.pubkey
                            if (candidateName == clean && !event.pubkey.equals(myPubkey, ignoreCase = true)) {
                                takenBy = event.pubkey
                            }
                        }
                    } catch (_: Exception) {}
                }
            }
        }

        // Wait briefly for network probe responses
        delay(1200L)
        probeJob.cancel()
        relayPool.unsubscribe(probeSubId)

        val finalCheck = takenBy ?: knownUsernames[clean]
        return if (finalCheck != null && !finalCheck.equals(myPubkey, ignoreCase = true)) {
            UsernameCheckResult.Taken(clean, finalCheck)
        } else {
            UsernameCheckResult.Available(clean)
        }
    }

    fun sendMediaMessage(
        chatId: String,
        recipientPubkey: String,
        rawImageBytes: ByteArray,
        mimeType: String = "image/webp"
    ) {
        scope.launch {
            try {
                // 1. Encrypt image bytes locally with AES-256-GCM
                val (ciphertext, attachmentDraft) = BlossomMediaManager.encryptMediaBytes(rawImageBytes, mimeType)

                // 2. Upload encrypted blob to free Blossom / NIP-96 host
                val publicUrl = BlossomMediaManager.uploadEncryptedBlob(ciphertext, "application/octet-stream")
                val finalAttachment = attachmentDraft.copy(url = publicUrl)

                // 3. Format into structured message text
                val mediaPayload = BlossomMediaManager.formatMediaMessage(finalAttachment)

                // 4. Send encrypted payload as chat message
                sendMessage(chatId, recipientPubkey, mediaPayload)
            } catch (e: Exception) {
                println("[MessengerRepository] Failed to send encrypted media: ${e.message}")
            }
        }
    }

    fun createChannel(name: String, about: String, picture: String? = null): PublicChannel {
        val event = Nip28.buildCreateChannelEvent(name, about, picture, myPubkey)
        val channel = PublicChannel(
            id = event.id,
            name = name,
            about = about,
            pictureUrl = picture,
            creatorPubkey = myPubkey
        )

        localDb.saveChannel(channel)
        _channels.value = listOf(channel) + _channels.value.filter { it.id != channel.id }

        relayPool.broadcastEvent(event)
        subscribeToChannel(channel.id)
        return channel
    }

    fun subscribeToChannel(channelId: String) {
        relayPool.subscribe(
            subId = "sub_chan_${channelId.take(8)}",
            kinds = listOf(NostrKinds.CHANNEL_MESSAGE),
            eTags = listOf(channelId)
        )
    }

    fun sendChannelMessage(channelId: String, text: String) {
        val now = PlatformCrypto.currentTimeSeconds()
        val event = Nip28.buildChannelMessageEvent(channelId, text, myPubkey)

        val optimisticMsg = ChatMessage(
            id = event.id,
            chatId = channelId,
            senderPubkey = myPubkey,
            text = text,
            timestamp = now,
            status = MessageDeliveryStatus.SENT,
            isOutgoing = true
        )

        localDb.insertChannelMessage(channelId, optimisticMsg)
        val currentList = _channelMessages.value[channelId] ?: emptyList()
        _channelMessages.value = _channelMessages.value + (channelId to (currentList + optimisticMsg))

        relayPool.broadcastEvent(event)
    }

    fun startNewChat(peerPubkeyOrNpub: String, peerDisplayName: String): ChatSummary {
        val cleanInput = peerPubkeyOrNpub.trim()
        val pubkeyHex = if (cleanInput.startsWith("npub1")) {
            try {
                Nip19.parseNpub(cleanInput).lowercase()
            } catch (_: Exception) {
                cleanInput.lowercase()
            }
        } else {
            cleanInput.lowercase()
        }

        // Trigger live profile fetch from Nostr relays immediately
        fetchUserProfile(pubkeyHex)

        val chatId = "chat_${pubkeyHex.take(12)}"
        val existing = _chats.value.find { it.chatId == chatId || it.peerPubkey.equals(pubkeyHex, ignoreCase = true) }
        if (existing != null) {
            return existing
        }

        val cachedUser = _communityUsers.value.find { it.pubkeyHex.equals(pubkeyHex, ignoreCase = true) }
        val finalName = peerDisplayName.ifBlank {
            cachedUser?.displayName?.ifBlank { null }
                ?: cachedUser?.name?.ifBlank { null }
                ?: "User ${pubkeyHex.take(6)}"
        }
        val finalAvatar = cachedUser?.picture

        val newChat = ChatSummary(
            chatId = chatId,
            peerPubkey = pubkeyHex,
            peerDisplayName = finalName,
            peerAvatarUrl = finalAvatar,
            lastMessage = "Chat started",
            lastTimestamp = PlatformCrypto.currentTimeSeconds(),
            unreadCount = 0
        )
        localDb.saveChat(newChat)
        _chats.value = listOf(newChat) + _chats.value
        return newChat
    }

    fun sendMessage(chatId: String, recipientPubkey: String, text: String) {
        val now = PlatformCrypto.currentTimeSeconds()
        val tempId = "msg_${now}_${PlatformCrypto.randomBytes(4).toHex()}"

        val optimisticMsg = ChatMessage(
            id = tempId,
            chatId = chatId,
            senderPubkey = myPubkey,
            text = text,
            timestamp = now,
            status = MessageDeliveryStatus.SENDING,
            isOutgoing = true
        )

        localDb.insertMessage(optimisticMsg)
        val currentList = _messages.value[chatId] ?: emptyList()
        _messages.value = _messages.value + (chatId to (currentList + optimisticMsg))

        val summaryDisplay = if (BlossomMediaManager.isMediaMessage(text)) "📷 Image Attachment" else text
        updateChatSummary(chatId, summaryDisplay, now)

        scope.launch {
            try {
                val targetPubkey = if (recipientPubkey.trim().startsWith("npub1")) {
                    try { Nip19.parseNpub(recipientPubkey.trim()).lowercase() } catch (_: Exception) { recipientPubkey.trim().lowercase() }
                } else {
                    recipientPubkey.trim().lowercase()
                }

                val chatKey = DoubleEncryptionEngine.deriveConversationKey(myPubkey, targetPubkey)
                val innerEncrypted = DoubleEncryptionEngine.encryptInnerLayer(
                    text = text,
                    sessionKey = chatKey,
                    senderPubkey = myPubkey
                )

                val giftWrapEvent = DoubleEncryptionEngine.createGiftWrapEvent(
                    innerEncryptedJson = innerEncrypted,
                    recipientPubkey = targetPubkey
                )

                relayPool.broadcastEvent(giftWrapEvent)
                updateMessageStatus(chatId, tempId, MessageDeliveryStatus.SENT)
            } catch (e: Exception) {
                println("[MessengerRepository] Send error: ${e.message}")
                updateMessageStatus(chatId, tempId, MessageDeliveryStatus.FAILED)
            }
        }
    }

    fun clearChat(chatId: String) {
        localDb.clearChat(chatId)
        _messages.value = _messages.value + (chatId to emptyList())
        _chats.value = _chats.value.map {
            if (it.chatId == chatId) it.copy(lastMessage = "", unreadCount = 0) else it
        }
    }

    fun deleteChat(chatId: String) {
        localDb.deleteChat(chatId)
        _messages.value = _messages.value - chatId
        _chats.value = _chats.value.filter { it.chatId != chatId }
    }

    fun deleteMessage(chatId: String, messageId: String) {
        localDb.deleteMessage(chatId, messageId)
        val current = _messages.value[chatId] ?: emptyList()
        val updated = current.filter { it.id != messageId }
        _messages.value = _messages.value + (chatId to updated)
        val lastMsg = updated.lastOrNull()?.let {
            if (it.text.startsWith("nostr:media:")) "📷 Файл" else it.text
        } ?: ""
        _chats.value = _chats.value.map {
            if (it.chatId == chatId) it.copy(lastMessage = lastMsg) else it
        }
    }

    private fun updateMessageStatus(chatId: String, messageId: String, newStatus: MessageDeliveryStatus) {
        localDb.updateMessageStatus(chatId, messageId, newStatus)
        val list = _messages.value[chatId] ?: return
        val updated = list.map { msg ->
            if (msg.id == messageId) msg.copy(status = newStatus) else msg
        }
        _messages.value = _messages.value + (chatId to updated)
    }

    private fun updateChatSummary(chatId: String, lastText: String, timestamp: Long) {
        val currentChats = _chats.value
        val updated = currentChats.map { chat ->
            if (chat.chatId == chatId) {
                val upd = chat.copy(lastMessage = lastText, lastTimestamp = timestamp)
                localDb.saveChat(upd)
                upd
            } else chat
        }
        _chats.value = updated
    }

    private fun handleIncomingEvent(event: com.nostr.expressive.shared.model.NostrEvent) {
        when (event.kind) {
            NostrKinds.METADATA -> {
                // Incoming peer profile metadata
                try {
                    val meta = Nip01Profile.parseMetadata(event.content)
                    val senderHex = event.pubkey.lowercase()
                    meta.name?.trim()?.lowercase()?.let { knownUsernames[it] = senderHex }
                    val newDisplayName = meta.display_name?.ifBlank { null }
                        ?: meta.name?.ifBlank { null }
                        ?: "User ${senderHex.take(6)}"
                    val newAvatar = meta.picture?.ifBlank { null }

                    val nossUser = NossUser(
                        pubkeyHex = senderHex,
                        npub = com.nostr.expressive.shared.crypto.Nip19.toNpub(senderHex),
                        name = meta.name ?: "user",
                        displayName = newDisplayName,
                        about = meta.about ?: "",
                        picture = newAvatar
                    )
                    _communityUsers.value = (_communityUsers.value.filter { !it.pubkeyHex.equals(senderHex, ignoreCase = true) } + nossUser)
                    val chatId = "chat_${senderHex.take(12)}"
                    val existingChat = _chats.value.find { it.chatId == chatId || it.peerPubkey.equals(senderHex, ignoreCase = true) }
                    if (existingChat != null) {
                        val updated = existingChat.copy(
                            peerDisplayName = newDisplayName,
                            peerAvatarUrl = newAvatar ?: existingChat.peerAvatarUrl
                        )
                        localDb.saveChat(updated)
                        _chats.value = _chats.value.map { if (it.chatId == existingChat.chatId) updated else it }
                    }
                } catch (_: Exception) {}
            }
            NostrKinds.GIFT_WRAP -> {
                try {
                    // Check if this Gift Wrap is addressed to me (has #p tag == myPubkey)
                    val pTag = event.tags.firstOrNull { it.getOrNull(0) == "p" }?.getOrNull(1)
                    if (pTag != null && !pTag.equals(myPubkey, ignoreCase = true)) {
                        return
                    }

                    val envelope = DoubleEncryptionEngine.json.decodeFromString<com.nostr.expressive.shared.crypto.InnerPayloadEnvelope>(event.content)
                    val senderPubkey = envelope.sender_pubkey
                    if (senderPubkey.isBlank() || senderPubkey.equals(myPubkey, ignoreCase = true)) {
                        return
                    }

                    val chatKey = DoubleEncryptionEngine.deriveConversationKey(myPubkey, senderPubkey)
                    val decryptedText = DoubleEncryptionEngine.decryptInnerLayer(event.content, chatKey)

                    val chatId = "chat_${senderPubkey.take(12)}"
                    val now = PlatformCrypto.currentTimeSeconds()

                    val incomingMsg = ChatMessage(
                        id = event.id.ifBlank { "in_${now}_${PlatformCrypto.randomBytes(4).toHex()}" },
                        chatId = chatId,
                        senderPubkey = senderPubkey,
                        text = decryptedText,
                        timestamp = event.created_at.takeIf { it > 0 } ?: now,
                        status = MessageDeliveryStatus.DELIVERED,
                        isOutgoing = false
                    )

                    localDb.insertMessage(incomingMsg)
                    val currentList = _messages.value[chatId] ?: emptyList()
                    if (currentList.none { it.id == incomingMsg.id }) {
                        _messages.value = _messages.value + (chatId to (currentList + incomingMsg))
                        val summaryText = if (BlossomMediaManager.isMediaMessage(decryptedText)) "📷 Image Attachment" else decryptedText
                        updateChatSummary(chatId, summaryText, now)
                    }

                    if (_chats.value.none { it.chatId == chatId }) {
                        val senderName = knownUsernames.entries.find { it.value == senderPubkey }?.key ?: "User_${senderPubkey.take(6)}"
                        val newChat = ChatSummary(
                            chatId = chatId,
                            peerPubkey = senderPubkey,
                            peerDisplayName = senderName,
                            peerAvatarUrl = null,
                            lastMessage = if (BlossomMediaManager.isMediaMessage(decryptedText)) "📷 Image Attachment" else decryptedText,
                            lastTimestamp = now,
                            unreadCount = 1
                        )
                        localDb.saveChat(newChat)
                        _chats.value = listOf(newChat) + _chats.value
                    }
                } catch (e: Throwable) {
                    println("[MessengerRepository] Error decrypting incoming GIFT_WRAP: ${e.message}")
                }
            }
            NostrKinds.CHANNEL_CREATE -> {
                try {
                    // Strict anti-spam filter: only accept channels created by our app
                    if (!Nip28.isAppChannel(event) && event.pubkey != myPubkey) {
                        return
                    }

                    val meta = Nip28.parseMetadata(event.content)
                    val newChannel = PublicChannel(
                        id = event.id,
                        name = meta.name,
                        about = meta.about,
                        pictureUrl = meta.picture,
                        creatorPubkey = event.pubkey
                    )
                    localDb.saveChannel(newChannel)
                    val currentList = _channels.value
                    if (currentList.none { it.id == newChannel.id }) {
                        _channels.value = listOf(newChannel) + currentList
                    }
                } catch (_: Exception) {}
            }
            NostrKinds.CHANNEL_MESSAGE -> {
                try {
                    val channelIdTag = event.tags.firstOrNull { it.getOrNull(0) == "e" }?.getOrNull(1) ?: return
                    val isMine = event.pubkey == myPubkey
                    val msg = ChatMessage(
                        id = event.id,
                        chatId = channelIdTag,
                        senderPubkey = event.pubkey,
                        text = event.content,
                        timestamp = event.created_at,
                        status = MessageDeliveryStatus.DELIVERED,
                        isOutgoing = isMine
                    )

                    localDb.insertChannelMessage(channelIdTag, msg)
                    val currentList = _channelMessages.value[channelIdTag] ?: emptyList()
                    if (currentList.none { it.id == msg.id }) {
                        _channelMessages.value = _channelMessages.value + (channelIdTag to (currentList + msg))
                    }
                } catch (_: Exception) {}
            }
        }
    }

    fun close() {
        relayPool.close()
    }
}
