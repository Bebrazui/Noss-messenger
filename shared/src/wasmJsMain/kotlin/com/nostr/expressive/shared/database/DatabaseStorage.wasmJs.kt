package com.nostr.expressive.shared.database

import com.nostr.expressive.shared.crypto.hexToByteArray
import com.nostr.expressive.shared.crypto.toHex
import com.nostr.expressive.shared.storage.PlatformStorage

actual object DatabaseStorage {
    private const val KEY_DB_WASM = "nostr_database_wasm_enc_hex"

    actual fun saveEncryptedBytes(bytes: ByteArray) {
        PlatformStorage.saveString(KEY_DB_WASM, bytes.toHex())
    }

    actual fun loadEncryptedBytes(): ByteArray? {
        val hex = PlatformStorage.getString(KEY_DB_WASM) ?: return null
        return try {
            hex.hexToByteArray()
        } catch (_: Exception) {
            null
        }
    }
}
