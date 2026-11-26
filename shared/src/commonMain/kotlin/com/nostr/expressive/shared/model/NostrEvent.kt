package com.nostr.expressive.shared.model

import com.nostr.expressive.shared.crypto.PlatformCrypto
import com.nostr.expressive.shared.crypto.Secp256k1
import com.nostr.expressive.shared.crypto.toHex
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray

@Serializable
data class NostrEvent(
    val id: String = "",
    val pubkey: String = "",
    val created_at: Long = 0L,
    val kind: Int = 1,
    val tags: List<List<String>> = emptyList(),
    val content: String = "",
    val sig: String = ""
) {
    companion object {
        private val json = Json

        fun serializeNip01(
            pubkey: String,
            createdAt: Long,
            kind: Int,
            tags: List<List<String>>,
            content: String
        ): String {
            val arr = buildJsonArray {
                add(JsonPrimitive(0))
                add(JsonPrimitive(pubkey.lowercase()))
                add(JsonPrimitive(createdAt))
                add(JsonPrimitive(kind))
                add(buildJsonArray {
                    tags.forEach { tagList ->
                        add(buildJsonArray {
                            tagList.forEach { tagItem ->
                                add(JsonPrimitive(tagItem))
                            }
                        })
                    }
                })
                add(JsonPrimitive(content))
            }
            return json.encodeToString(JsonElement.serializer(), arr)
        }

        fun calculateId(
            pubkey: String,
            createdAt: Long,
            kind: Int,
            tags: List<List<String>>,
            content: String
        ): String {
            val serialized = serializeNip01(pubkey, createdAt, kind, tags, content)
            return PlatformCrypto.sha256(serialized.encodeToByteArray()).toHex()
        }

        fun createAndSign(
            privkeyHex: String,
            kind: Int,
            content: String,
            tags: List<List<String>> = emptyList(),
            createdAt: Long = PlatformCrypto.currentTimeSeconds()
        ): NostrEvent {
            val cleanPrivHex = privkeyHex.trim().lowercase()
            val pubkey = Secp256k1.getPublicKey(cleanPrivHex)
            val id = calculateId(pubkey, createdAt, kind, tags, content)
            val sig = Secp256k1.signSchnorr(id, cleanPrivHex)
            return NostrEvent(
                id = id,
                pubkey = pubkey,
                created_at = createdAt,
                kind = kind,
                tags = tags,
                content = content,
                sig = sig
            )
        }
    }
}

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
