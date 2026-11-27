package com.nostr.expressive.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import nostr_expressive_messenger.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import com.nostr.expressive.shared.repository.ChatMessage
import com.nostr.expressive.shared.repository.MessageDeliveryStatus
import com.nostr.expressive.ui.picker.toImageBitmap
import com.nostr.expressive.ui.theme.ExpressiveCookie9Shape
import com.nostr.expressive.ui.theme.FloatingPillShape
import com.nostr.expressive.ui.theme.InboundBubbleShape
import com.nostr.expressive.ui.theme.OutboundBubbleShape

/**
 * Expressive Floating Toolbar (Bottom Pill Navigation: Chats + Profile + Action FAB)
 */
@Composable
fun ExpressiveFloatingToolbar(
    currentTab: Int,
    onTabSelected: (Int) -> Unit,
    onFabClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .padding(16.dp)
            .height(58.dp),
        shape = FloatingPillShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 6.dp,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Tab 0: Chats & Unified Search
            ToolbarTabItem(
                label = stringResource(Res.string.nav_chats),
                isSelected = currentTab == 0,
                onClick = { onTabSelected(0) }
            )

            // Tab 1: Profile
            ToolbarTabItem(
                label = stringResource(Res.string.nav_profile),
                isSelected = currentTab == 1,
                onClick = { onTabSelected(1) }
            )

            Spacer(modifier = Modifier.width(4.dp))

            // Vibrant Action Button (FAB inside Toolbar) with Vector Plus
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
                    .clickable { onFabClick() },
                contentAlignment = Alignment.Center
            ) {
                PlusIcon(
                    tint = MaterialTheme.colorScheme.onPrimary,
                    size = 18.dp
                )
            }
        }
    }
}

@Composable
private fun ToolbarTabItem(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(
                if (isSelected) MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)
                else Color.Transparent
            )
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelLarge
        )
    }
}

/**
 * Expressive Split Button for Message Input (Vector Paper Airplane + Vector Zap Bolt)
 */
@Composable
fun ExpressiveSplitButton(
    onSend: () -> Unit,
    onZap: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.height(42.dp),
        shape = FloatingPillShape,
        color = MaterialTheme.colorScheme.primary
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Main Send Button with Vector Airplane
            Box(
                modifier = Modifier
                    .clickable { onSend() }
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                SendAirplaneIcon(
                    tint = MaterialTheme.colorScheme.onPrimary,
                    size = 18.dp
                )
            }

            // Divider
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(20.dp)
                    .background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.25f))
            )

            // Zap action with Vector Bolt
            Box(
                modifier = Modifier
                    .clickable { onZap() }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                ZapBoltIcon(
                    tint = MaterialTheme.colorScheme.onPrimary,
                    size = 15.dp
                )
            }
        }
    }
}

/**
 * Expressive Message Bubble with Asymmetric Corners and Delivery Status
 */
@Composable
fun ExpressiveMessageBubble(
    message: ChatMessage,
    modifier: Modifier = Modifier
) {
    val isOutbound = message.isOutgoing
    val shape = if (isOutbound) OutboundBubbleShape else InboundBubbleShape
    val backgroundColor = if (isOutbound) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceContainerHigh
    }
    val textColor = if (isOutbound) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    val mediaAttachment = com.nostr.expressive.shared.media.BlossomMediaManager.parseMediaAttachment(message.text)
    var decryptedBitmap by remember(mediaAttachment?.url) { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(mediaAttachment) {
        if (mediaAttachment != null && mediaAttachment.url.isNotBlank()) {
            val encBytes = com.nostr.expressive.shared.media.ImageLoader.loadImageBytes(mediaAttachment.url)
            if (encBytes != null) {
                try {
                    val decBytes = com.nostr.expressive.shared.media.BlossomMediaManager.decryptMediaBytes(
                        encBytes,
                        mediaAttachment.keyHex,
                        mediaAttachment.ivHex
                    )
                    decryptedBitmap = decBytes.toImageBitmap()
                } catch (_: Exception) {}
            }
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = if (isOutbound) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            shape = shape,
            color = backgroundColor,
            tonalElevation = 2.dp
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                if (mediaAttachment != null) {
                    val currentDecrypted = decryptedBitmap
                    if (currentDecrypted != null) {
                        Image(
                            bitmap = currentDecrypted,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 260.dp)
                                .clip(RoundedCornerShape(12.dp))
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                    } else {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.background.copy(alpha = 0.35f),
                            modifier = Modifier.padding(bottom = 6.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(ExpressiveCookie9Shape)
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "ENC",
                                        fontSize = 10.sp,
                                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Column {
                                    Text(
                                        text = "Фото / Файл (AES-GCM)",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = textColor
                                    )
                                    Text(
                                        text = "${mediaAttachment.mimeType} · ${mediaAttachment.sha256Hex.take(8)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontSize = 11.sp,
                                        color = textColor.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        }
                    }
                } else {
                    Text(
                        text = message.text,
                        color = textColor,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }

                Row(
                    modifier = Modifier.align(Alignment.End),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (isOutbound) {
                        StatusIndicator(message.status)
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusIndicator(status: MessageDeliveryStatus) {
    when (status) {
        MessageDeliveryStatus.SENDING -> {
            CircularProgressIndicator(
                modifier = Modifier.size(10.dp),
                strokeWidth = 1.5.dp,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
            )
        }
        MessageDeliveryStatus.SENT -> {
            CanvasCheckmark(double = false, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f))
        }
        MessageDeliveryStatus.DELIVERED -> {
            CanvasCheckmark(double = true, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f))
        }
        MessageDeliveryStatus.READ -> {
            CanvasCheckmark(double = true, color = MaterialTheme.colorScheme.tertiary)
        }
        MessageDeliveryStatus.FAILED -> {
            Text(text = "!", fontSize = 11.sp, color = Color.Red)
        }
    }
}

@Composable
private fun CanvasCheckmark(double: Boolean, color: Color) {
    androidx.compose.foundation.Canvas(modifier = Modifier.size(if (double) 15.dp else 10.dp, 10.dp)) {
        val stroke = 1.5f.dp.toPx()
        // First checkmark
        val path1 = androidx.compose.ui.graphics.Path().apply {
            moveTo(size.width * 0.1f, size.height * 0.5f)
            lineTo(size.width * 0.4f, size.height * 0.85f)
            lineTo(size.width * 0.85f, size.height * 0.15f)
        }
        drawPath(
            path = path1,
            color = color,
            style = androidx.compose.ui.graphics.drawscope.Stroke(
                width = stroke,
                cap = androidx.compose.ui.graphics.StrokeCap.Round,
                join = androidx.compose.ui.graphics.StrokeJoin.Round
            )
        )
    }
}
