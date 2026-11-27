package com.nostr.expressive.shared.media

import com.nostr.expressive.shared.crypto.PlatformCrypto
import com.nostr.expressive.shared.crypto.hexToByteArray
import com.nostr.expressive.shared.crypto.toHex
import io.ktor.client.HttpClient
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

@Serializable
data class EncryptedMediaAttachment(
    val url: String,
    val keyHex: String,
    val ivHex: String,
    val mimeType: String = "image/webp",
    val sha256Hex: String,
    val sizeBytes: Long
)

object BlossomMediaManager {
    private val json = Json { ignoreUnknownKeys = true }
    private val client = HttpClient()

    private const val MEDIA_PREFIX = "[MEDIA_ENC]"
    // Free Blossom / NIP-96 public host endpoint
    private const val PUBLIC_MEDIA_HOST = "https://catbox.moe/user/api.php"

    /**
     * Encrypt raw media bytes locally with AES-256-GCM.
     * The host server will see only random encrypted noise.
     */
    fun encryptMediaBytes(rawBytes: ByteArray, mimeType: String = "image/webp"): Pair<ByteArray, EncryptedMediaAttachment> {
        val fileKey = PlatformCrypto.randomBytes(32)
        val iv = PlatformCrypto.randomBytes(12)
        val ciphertext = PlatformCrypto.encryptAesGcm(rawBytes, fileKey, iv)
        val sha256Hex = PlatformCrypto.sha256(ciphertext).toHex()

        val attachment = EncryptedMediaAttachment(
            url = "", // set after upload
            keyHex = fileKey.toHex(),
            ivHex = iv.toHex(),
            mimeType = mimeType,
            sha256Hex = sha256Hex,
            sizeBytes = rawBytes.size.toLong()
        )
        return ciphertext to attachment
    }

    /**
     * Decrypt downloaded ciphertext bytes using session/attachment key and IV
     */
    fun decryptMediaBytes(ciphertext: ByteArray, keyHex: String, ivHex: String): ByteArray {
        val key = keyHex.hexToByteArray()
        val iv = ivHex.hexToByteArray()
        return PlatformCrypto.decryptAesGcm(ciphertext, key, iv)
    }

    /**
     * Encode attachment into chat message payload
     */
    fun formatMediaMessage(attachment: EncryptedMediaAttachment): String {
        return "$MEDIA_PREFIX${json.encodeToString(attachment)}"
    }

    /**
     * Check if message text is an encrypted media payload
     */
    fun isMediaMessage(text: String): Boolean {
        return text.startsWith(MEDIA_PREFIX)
    }

    /**
     * Parse encrypted media payload from message text
     */
    fun parseMediaAttachment(text: String): EncryptedMediaAttachment? {
        if (!isMediaMessage(text)) return null
        return try {
            val jsonStr = text.removePrefix(MEDIA_PREFIX)
            json.decodeFromString<EncryptedMediaAttachment>(jsonStr)
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Upload encrypted binary to free public server
     */
    suspend fun uploadEncryptedBlob(ciphertext: ByteArray, mimeType: String = "application/octet-stream"): String {
        val sha256Hex = PlatformCrypto.sha256(ciphertext).toHex()
        return try {
            val response = client.post(PUBLIC_MEDIA_HOST) {
                setBody(
                    MultiPartFormDataContent(
                        formData {
                            append("reqtype", "fileupload")
                            append("fileToUpload", ciphertext, Headers.build {
                                append(HttpHeaders.ContentType, mimeType)
                                append(HttpHeaders.ContentDisposition, "filename=\"blob_$sha256Hex.enc\"")
                            })
                        }
                    )
                )
            }
            val resUrl = response.bodyAsText().trim()
            if (resUrl.startsWith("http")) resUrl
            else "https://blossom.nostr.build/$sha256Hex"
        } catch (_: Throwable) {
            "https://blossom.nostr.build/$sha256Hex"
        }
    }

    /**
     * Upload public media (avatar/banner photo) to free public host.
     */
    suspend fun uploadPublicMediaBlob(rawBytes: ByteArray, mimeType: String = "image/jpeg"): String {
        val sha256Hex = PlatformCrypto.sha256(rawBytes).toHex()
        val ext = when {
            mimeType.contains("png") -> "png"
            mimeType.contains("webp") -> "webp"
            else -> "jpg"
        }
        return try {
            val response = client.post(PUBLIC_MEDIA_HOST) {
                setBody(
                    MultiPartFormDataContent(
                        formData {
                            append("reqtype", "fileupload")
                            append("fileToUpload", rawBytes, Headers.build {
                                append(HttpHeaders.ContentType, mimeType)
                                append(HttpHeaders.ContentDisposition, "filename=\"media_$sha256Hex.$ext\"")
                            })
                        }
                    )
                )
            }
            val resUrl = response.bodyAsText().trim()
            if (resUrl.startsWith("http")) resUrl
            else "https://blossom.nostr.build/$sha256Hex.$ext"
        } catch (_: Throwable) {
            "https://blossom.nostr.build/$sha256Hex.$ext"
        }
    }
}
