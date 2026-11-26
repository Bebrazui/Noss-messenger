package com.nostr.expressive.shared.crypto

import com.nostr.expressive.shared.storage.PlatformStorage
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class NostrIdentity(
    val privkeyHex: String,
    val pubkeyHex: String,
    val nsec: String,
    val npub: String
)

object KeyManager {
    private const val KEY_IDENTITY = "saved_nostr_identity"
    private val json = Json { ignoreUnknownKeys = true }

    fun getSavedIdentity(): NostrIdentity? {
        val raw = PlatformStorage.getString(KEY_IDENTITY) ?: return null
        return try {
            json.decodeFromString<NostrIdentity>(raw)
        } catch (e: Exception) {
            null
        }
    }

    fun generateNewIdentity(): NostrIdentity {
        val privBytes = PlatformCrypto.randomBytes(32)
        val privHex = privBytes.toHex()
        
        // Derive 32-byte BIP-340 secp256k1 x-only public key
        val pubHex = Secp256k1.getPublicKey(privHex)

        val nsec = Nip19.toNsec(privHex)
        val npub = Nip19.toNpub(pubHex)

        val identity = NostrIdentity(
            privkeyHex = privHex,
            pubkeyHex = pubHex,
            nsec = nsec,
            npub = npub
        )
        saveIdentity(identity)
        return identity
    }

    fun importIdentity(nsecOrHex: String): NostrIdentity {
        val trimmed = nsecOrHex.trim()
        val privHex = if (trimmed.startsWith("nsec1")) {
            Nip19.parseNsec(trimmed)
        } else {
            trimmed
        }

        require(privHex.length == 64 && privHex.all { it in "0123456789abcdefABCDEF" }) {
            "Invalid private key: must be a 64-character hex string or valid nsec1... address"
        }

        val cleanPrivHex = privHex.lowercase()
        val pubHex = Secp256k1.getPublicKey(cleanPrivHex)

        val identity = NostrIdentity(
            privkeyHex = cleanPrivHex,
            pubkeyHex = pubHex,
            nsec = Nip19.toNsec(cleanPrivHex),
            npub = Nip19.toNpub(pubHex)
        )
        saveIdentity(identity)
        return identity
    }

    fun saveIdentity(identity: NostrIdentity) {
        val serialized = json.encodeToString(identity)
        PlatformStorage.saveString(KEY_IDENTITY, serialized)
    }

    fun clearIdentity() {
        PlatformStorage.remove(KEY_IDENTITY)
    }
}
