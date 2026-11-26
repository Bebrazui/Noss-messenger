package com.nostr.expressive.shared.database

import java.io.File

actual object DatabaseStorage {
    private val dbDir: File by lazy {
        val home = System.getProperty("user.home")
        val dir = File(home, ".nostr_expressive")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        dir
    }

    private val dbFile: File by lazy {
        File(dbDir, "messenger_db.enc")
    }

    actual fun saveEncryptedBytes(bytes: ByteArray) {
        try {
            val tempFile = File(dbDir, "messenger_db.enc.tmp")
            tempFile.writeBytes(bytes)
            if (dbFile.exists()) {
                dbFile.delete()
            }
            tempFile.renameTo(dbFile)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    actual fun loadEncryptedBytes(): ByteArray? {
        return try {
            if (dbFile.exists()) {
                dbFile.readBytes()
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
}
