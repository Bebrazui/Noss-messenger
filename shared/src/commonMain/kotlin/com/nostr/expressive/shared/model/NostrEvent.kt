package com.nostr.expressive.shared.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class NostrEvent(
    val id: String = "",
    val pubkey: String = "",
    val created_at: Long = 0L,
    val kind: Int = 1,
    val tags: List<List<String>> = emptyList(),
    val content: String = "",
    val sig: String = ""
)

@Serializable
data class NostrFilter(
    val ids: List<String>? = null,
    val authors: List<String>? = null,
    val kinds: List<Int>? = null,
    val since: Long? = null,
    val until: Long? = null,
    val limit: Int? = null
)

/**
 * Kind constants for Nostr
 */
object NostrKinds {
    const val METADATA = 0          // NIP-01: User Profile (name, about, picture, etc.)
    const val TEXT_NOTE = 1         // NIP-01: Public short note
    const val SEAL = 13             // NIP-59: Sealed Rumor
    const val CHAT_MESSAGE = 14     // NIP-17: Direct chat rumor
    const val CHANNEL_CREATE = 40   // NIP-28: Public Channel Creation
    const val CHANNEL_META = 41     // NIP-28: Public Channel Metadata
    const val CHANNEL_MESSAGE = 42  // NIP-28: Public Channel Message
    const val GIFT_WRAP = 1059      // NIP-59: Gift Wrap (ephemeral key encryption)
}
