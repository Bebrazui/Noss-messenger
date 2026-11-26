package com.nostr.expressive.ui.picker

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageBitmap

@Composable
expect fun rememberImagePicker(onImagePicked: (bytes: ByteArray, mimeType: String) -> Unit): () -> Unit

expect fun ByteArray.toImageBitmap(): ImageBitmap?
