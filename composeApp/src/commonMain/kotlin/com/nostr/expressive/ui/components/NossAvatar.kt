package com.nostr.expressive.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nostr.expressive.shared.media.ImageLoader
import com.nostr.expressive.ui.picker.toImageBitmap

@Composable
fun NossAvatar(
    avatarUrl: String?,
    displayName: String,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    fallbackBg: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
    textColor: Color = MaterialTheme.colorScheme.primary
) {
    var imageBitmap by remember(avatarUrl) { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(avatarUrl) {
        if (!avatarUrl.isNullOrBlank() && (avatarUrl.startsWith("http://") || avatarUrl.startsWith("https://"))) {
            val bytes = ImageLoader.loadImageBytes(avatarUrl)
            if (bytes != null) {
                imageBitmap = bytes.toImageBitmap()
            }
        } else {
            imageBitmap = null
        }
    }

    val currentBitmap = imageBitmap
    if (currentBitmap != null) {
        Image(
            bitmap = currentBitmap,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier
                .size(size)
                .clip(CircleShape)
        )
    } else {
        Box(
            modifier = modifier
                .size(size)
                .clip(CircleShape)
                .background(fallbackBg),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = displayName.trim().take(2).uppercase().ifBlank { "?" },
                color = textColor,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
