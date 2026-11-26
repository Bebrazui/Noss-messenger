package com.nostr.expressive.shared.database

expect object DatabaseStorage {
    fun saveEncryptedBytes(bytes: ByteArray)
    fun loadEncryptedBytes(): ByteArray?
}
