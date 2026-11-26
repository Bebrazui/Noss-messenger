package com.nostr.expressive.shared.media

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.readRawBytes

object ImageLoader {
    private val client = HttpClient()
    private val memoryCache = mutableMapOf<String, ByteArray>()

    suspend fun loadImageBytes(url: String): ByteArray? {
        val clean = url.trim()
        if (clean.isBlank()) return null
        memoryCache[clean]?.let { return it }

        return try {
            val response = client.get(clean)
            val bytes = response.readRawBytes()
            if (bytes.isNotEmpty()) {
                memoryCache[clean] = bytes
                bytes
            } else null
        } catch (_: Exception) {
            null
        }
    }
}
