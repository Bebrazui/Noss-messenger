package com.nostr.expressive.shared.protocol

import com.nostr.expressive.shared.crypto.PlatformCrypto
import com.nostr.expressive.shared.crypto.toHex
import com.nostr.expressive.shared.model.NostrEvent
import com.nostr.expressive.shared.model.NostrKinds
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class ChannelMetadata(
    val name: String,
    val about: String = "",
    val picture: String? = null
)

object Nip28 {
    private val json = Json { ignoreUnknownKeys = true }
    const val APP_TAG = "nostr-expressive"

    /**
     * NIP-28 Kind 40: Create public channel with application identifier tag
     */
    fun buildCreateChannelEvent(
        name: String,
        about: String,
        picture: String?,
        myPubkey: String
    ): NostrEvent {
        val metadata = ChannelMetadata(name = name, about = about, picture = picture)
        val content = json.encodeToString(metadata)
        val now = PlatformCrypto.currentTimeSeconds()

        // Tag event with our app identifier so we can filter out public relay spam
        val tags = listOf(
            listOf("client", APP_TAG)
        )

        val eventWithoutId = NostrEvent(
            pubkey = myPubkey,
            created_at = now,
            kind = NostrKinds.CHANNEL_CREATE,
            tags = tags,
            content = content,
            sig = "sig_nip28_placeholder"
        )

        val serialized = "${eventWithoutId.pubkey}:${eventWithoutId.created_at}:${eventWithoutId.kind}:${eventWithoutId.content}:${APP_TAG}"
        val id = PlatformCrypto.sha256(serialized.encodeToByteArray()).toHex()
        return eventWithoutId.copy(id = id)
    }

    /**
     * NIP-28 Kind 42: Post message to public channel
     */
    fun buildChannelMessageEvent(
        channelId: String,
        text: String,
        myPubkey: String
    ): NostrEvent {
        val now = PlatformCrypto.currentTimeSeconds()
        val tags = listOf(
            listOf("e", channelId, "", "root"),
            listOf("client", APP_TAG)
        )

        val eventWithoutId = NostrEvent(
            pubkey = myPubkey,
            created_at = now,
            kind = NostrKinds.CHANNEL_MESSAGE,
            tags = tags,
            content = text,
            sig = "sig_nip28_placeholder"
        )

        val serialized = "${eventWithoutId.pubkey}:${eventWithoutId.created_at}:${eventWithoutId.kind}:${eventWithoutId.content}:${channelId}"
        val id = PlatformCrypto.sha256(serialized.encodeToByteArray()).toHex()
        return eventWithoutId.copy(id = id)
    }

    /**
     * Parse Kind 40 metadata
     */
    fun parseMetadata(content: String): ChannelMetadata {
        return try {
            json.decodeFromString<ChannelMetadata>(content)
        } catch (_: Exception) {
            ChannelMetadata(name = "Unknown Channel", about = content)
        }
    }

    /**
     * Verify if event belongs to our application ecosystem (Anti-Spam filter)
     */
    fun isAppChannel(event: NostrEvent): Boolean {
        return event.tags.any { it.size >= 2 && it[0] == "client" && it[1] == APP_TAG }
    }
}
