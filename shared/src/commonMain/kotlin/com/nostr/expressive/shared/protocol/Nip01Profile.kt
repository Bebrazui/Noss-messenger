package com.nostr.expressive.shared.protocol

import com.nostr.expressive.shared.crypto.PlatformCrypto
import com.nostr.expressive.shared.crypto.toHex
import com.nostr.expressive.shared.model.NostrEvent
import com.nostr.expressive.shared.model.NostrKinds
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class ProfileMetadata(
    val name: String? = null,
    val display_name: String? = null,
    val about: String? = null,
    val picture: String? = null,
    val banner: String? = null,
    val nip05: String? = null,
    val lud16: String? = null
)

object Nip01Profile {
    private val json = Json { ignoreUnknownKeys = true }

    /**
     * NIP-01 Kind 0: Replaceable user metadata
     */
    fun buildProfileEvent(metadata: ProfileMetadata, myPubkey: String, privkeyHex: String = ""): NostrEvent {
        val content = json.encodeToString(metadata)
        val now = PlatformCrypto.currentTimeSeconds()

        return if (privkeyHex.isNotBlank()) {
            NostrEvent.createAndSign(
                privkeyHex = privkeyHex,
                kind = NostrKinds.METADATA,
                content = content,
                tags = emptyList(),
                createdAt = now
            )
        } else {
            val eventWithoutId = NostrEvent(
                pubkey = myPubkey,
                created_at = now,
                kind = NostrKinds.METADATA,
                tags = emptyList(),
                content = content,
                sig = ""
            )
            val serialized = "${eventWithoutId.pubkey}:${eventWithoutId.created_at}:${eventWithoutId.kind}:${eventWithoutId.content}"
            val id = PlatformCrypto.sha256(serialized.encodeToByteArray()).toHex()
            eventWithoutId.copy(id = id)
        }
    }

    /**
     * Parse Kind 0 content
     */
    fun parseMetadata(content: String): ProfileMetadata {
        return try {
            json.decodeFromString<ProfileMetadata>(content)
        } catch (_: Exception) {
            ProfileMetadata()
        }
    }
}
