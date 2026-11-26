package com.nostr.expressive.ui.picker

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import kotlinx.browser.document
import org.jetbrains.skia.Image
import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.Int8Array
import org.khronos.webgl.get
import org.w3c.dom.HTMLInputElement
import org.w3c.files.FileReader
import org.w3c.files.get

@Composable
actual fun rememberImagePicker(onImagePicked: (bytes: ByteArray, mimeType: String) -> Unit): () -> Unit {
    return {
        try {
            val input = document.createElement("input") as HTMLInputElement
            input.type = "file"
            input.accept = "image/*"
            input.setAttribute("style", "display: none; position: absolute; left: -9999px;")
            document.body?.appendChild(input)
            input.onchange = {
                val file = input.files?.get(0)
                if (file != null) {
                    val reader = FileReader()
                    reader.onload = {
                        val buffer = reader.result as? ArrayBuffer
                        if (buffer != null) {
                            val int8Array = Int8Array(buffer)
                            val bytes = ByteArray(int8Array.length) { idx -> int8Array[idx] }
                            val mime = file.type.ifBlank { "image/jpeg" }
                            onImagePicked(bytes, mime)
                        }
                        try { document.body?.removeChild(input) } catch (_: Exception) {}
                    }
                    reader.readAsArrayBuffer(file)
                } else {
                    try { document.body?.removeChild(input) } catch (_: Exception) {}
                }
            }
            input.click()
        } catch (e: Exception) {
            println("Wasm file picker error: ${e.message}")
        }
    }
}

actual fun ByteArray.toImageBitmap(): ImageBitmap? {
    return try {
        Image.makeFromEncoded(this).toComposeImageBitmap()
    } catch (_: Exception) {
        null
    }
}
