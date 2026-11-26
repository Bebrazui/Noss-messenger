package com.nostr.expressive.ui.picker

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import org.jetbrains.skia.Image
import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import javax.swing.SwingUtilities

@Composable
actual fun rememberImagePicker(onImagePicked: (bytes: ByteArray, mimeType: String) -> Unit): () -> Unit {
    return {
        SwingUtilities.invokeLater {
            try {
                val dialog = FileDialog(null as Frame?, "Select Image", FileDialog.LOAD)
                dialog.setFilenameFilter { _, name ->
                    val lower = name.lowercase()
                    lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".webp")
                }
                dialog.isVisible = true
                val file = dialog.file
                val dir = dialog.directory
                if (file != null && dir != null) {
                    val target = File(dir, file)
                    if (target.exists()) {
                        val bytes = target.readBytes()
                        val mime = when {
                            file.endsWith(".png", true) -> "image/png"
                            file.endsWith(".webp", true) -> "image/webp"
                            else -> "image/jpeg"
                        }
                        onImagePicked(bytes, mime)
                    }
                }
            } catch (e: Exception) {
                println("Desktop file picker error: ${e.message}")
            }
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
