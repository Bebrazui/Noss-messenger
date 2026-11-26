package com.nostr.expressive.ui.picker

import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun rememberImagePicker(onImagePicked: (bytes: ByteArray, mimeType: String) -> Unit): () -> Unit {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val bytes = inputStream?.use { it.readBytes() }
                val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"
                if (bytes != null && bytes.isNotEmpty()) {
                    onImagePicked(bytes, mimeType)
                }
            } catch (e: Exception) {
                println("Failed to read image from uri: ${e.message}")
            }
        }
    }
    return { launcher.launch("image/*") }
}

actual fun ByteArray.toImageBitmap(): ImageBitmap? {
    return try {
        BitmapFactory.decodeByteArray(this, 0, this.size)?.asImageBitmap()
    } catch (_: Exception) {
        null
    }
}
