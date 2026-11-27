package com.nostr.expressive.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.nostr.expressive.shared.repository.ChatMessage
import com.nostr.expressive.shared.repository.ChatSummary
import com.nostr.expressive.ui.theme.ExpressiveCookie9Shape
import com.nostr.expressive.ui.theme.FloatingPillShape
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * M3 Expressive Spring Bouncy Swipe for Chat Messages.
 * Allows elastic rubber-band pull of message bubbles with spring bounce-back.
 */
@Composable
fun BouncySpringSwipeMessage(
    message: ChatMessage,
    onLongPress: () -> Unit,
    onQuickAction: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val dragOffset = remember { Animatable(0f) }
    val isOutbound = message.isOutgoing

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(message.id) {
                detectHorizontalDragGestures(
                    onHorizontalDrag = { _, dragAmount ->
                        coroutineScope.launch {
                            // Apply rubber-band resistance damping
                            val current = dragOffset.value
                            val damped = dragAmount * 0.55f
                            // Outbound messages drag left, inbound messages drag right
                            val target = if (isOutbound) {
                                (current + damped).coerceIn(-130f, 20f)
                            } else {
                                (current + damped).coerceIn(-20f, 130f)
                            }
                            dragOffset.snapTo(target)
                        }
                    },
                    onDragEnd = {
                        coroutineScope.launch {
                            if (abs(dragOffset.value) > 75f) {
                                onQuickAction?.invoke()
                            }
                            // Bouncy spring return animation
                            dragOffset.animateTo(
                                targetValue = 0f,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessMedium
                                )
                            )
                        }
                    },
                    onDragCancel = {
                        coroutineScope.launch {
                            dragOffset.animateTo(
                                targetValue = 0f,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessMedium
                                )
                            )
                        }
                    }
                )
            }
            .pointerInput(message.id) {
                detectTapGestures(
                    onLongPress = { onLongPress() }
                )
            }
    ) {
        // Behind the bubble: spring reveal action badge
        val progress = (abs(dragOffset.value) / 70f).coerceIn(0f, 1f)
        if (progress > 0.05f) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .padding(horizontal = 24.dp),
                contentAlignment = if (isOutbound) Alignment.CenterEnd else Alignment.CenterStart
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = progress),
                    modifier = Modifier
                        .size(36.dp)
                        .scale(0.5f + progress * 0.5f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = if (isOutbound) "📋" else "↩️",
                            fontSize = 15.sp
                        )
                    }
                }
            }
        }

        // Foreground Message Bubble displaced by spring dragOffset
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(dragOffset.value.roundToInt(), 0) }
        ) {
            content()
        }
    }
}

/**
 * M3 Expressive Floating Context Menu for Chat Messages.
 * Renders an elevated floating popover pill directly over the focused message.
 */
@Composable
fun ExpressiveMessageFloatingMenu(
    visible: Boolean,
    message: ChatMessage?,
    onDismiss: () -> Unit,
    onCopy: () -> Unit,
    onDelete: () -> Unit,
    onReply: () -> Unit = {}
) {
    if (!visible || message == null) return

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.65f))
                .clickable { onDismiss() }
                .padding(horizontal = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = false) {} // Prevent click through
            ) {
                // Floating Action Pill (Expressive M3 Quick Actions)
                Surface(
                    shape = FloatingPillShape,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    shadowElevation = 8.dp,
                    tonalElevation = 6.dp,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        TextButton(
                            onClick = {
                                onCopy()
                                onDismiss()
                            },
                            shape = FloatingPillShape
                        ) {
                            Text("📋 Копировать", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Medium)
                        }

                        TextButton(
                            onClick = {
                                onReply()
                                onDismiss()
                            },
                            shape = FloatingPillShape
                        ) {
                            Text("↩️ Ответить", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
                        }

                        TextButton(
                            onClick = {
                                onDelete()
                                onDismiss()
                            },
                            shape = FloatingPillShape,
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("🗑️ Удалить", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                // Focused message preview card
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (message.isOutgoing) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest,
                    tonalElevation = 4.dp,
                    modifier = Modifier.widthIn(max = 380.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        val preview = if (message.text.startsWith("nostr:media:")) "📷 [Прикрепленный файл]" else message.text
                        Text(
                            text = preview,
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (message.isOutgoing) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

/**
 * M3 Expressive Spring Bouncy Swipe for Chat List Item.
 * Supports elastic pull to reveal quick delete and clear action buttons.
 */
@Composable
fun BouncySpringSwipeChat(
    chat: ChatSummary,
    onClick: () -> Unit,
    onClearChat: () -> Unit,
    onDeleteChat: () -> Unit,
    onLongPress: () -> Unit,
    content: @Composable () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val dragOffset = remember { Animatable(0f) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(chat.chatId) {
                detectHorizontalDragGestures(
                    onHorizontalDrag = { _, dragAmount ->
                        coroutineScope.launch {
                            val current = dragOffset.value
                            // Drag left reveals actions with rubber-band damping
                            val target = (current + dragAmount * 0.55f).coerceIn(-150f, 15f)
                            dragOffset.snapTo(target)
                        }
                    },
                    onDragEnd = {
                        coroutineScope.launch {
                            // If dragged far enough, settle at action width, else spring return
                            val target = if (dragOffset.value < -90f) -120f else 0f
                            dragOffset.animateTo(
                                targetValue = target,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessMedium
                                )
                            )
                        }
                    },
                    onDragCancel = {
                        coroutineScope.launch {
                            dragOffset.animateTo(0f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium))
                        }
                    }
                )
            }
            .pointerInput(chat.chatId) {
                detectTapGestures(
                    onTap = {
                        if (dragOffset.value < -20f) {
                            coroutineScope.launch {
                                dragOffset.animateTo(0f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium))
                            }
                        } else {
                            onClick()
                        }
                    },
                    onLongPress = { onLongPress() }
                )
            }
    ) {
        // Background Action buttons revealed on spring swipe
        Row(
            modifier = Modifier
                .matchParentSize()
                .padding(end = 20.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    coroutineScope.launch { dragOffset.animateTo(0f) }
                    onClearChat()
                },
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            ) {
                Text("🧹", fontSize = 18.sp)
            }

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = {
                    coroutineScope.launch { dragOffset.animateTo(0f) }
                    onDeleteChat()
                },
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.errorContainer)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Foreground item container displaced by spring dragOffset
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(dragOffset.value.roundToInt(), 0) }
        ) {
            content()
        }
    }
}

/**
 * M3 Expressive Centered Context Card for Chats.
 * Instead of generic bottom sheets, displays an expressive centered modal card.
 */
@Composable
fun ExpressiveChatContextCard(
    visible: Boolean,
    chat: ChatSummary?,
    onDismiss: () -> Unit,
    onClearChat: () -> Unit,
    onDeleteChat: () -> Unit
) {
    if (!visible || chat == null) return

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 6.dp,
            shadowElevation = 12.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Large 9-side cookie avatar
                NossAvatar(
                    avatarUrl = chat.peerAvatarUrl,
                    displayName = chat.peerDisplayName,
                    size = 64.dp,
                    shape = ExpressiveCookie9Shape
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = chat.peerDisplayName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = "Действия с диалогом",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(22.dp))

                // Actions with M3 Expressive connected surfaces
                Surface(
                    shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 4.dp, bottomEnd = 4.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onDismiss()
                            onClearChat()
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 15.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🧹", fontSize = 18.sp)
                        Spacer(modifier = Modifier.width(14.dp))
                        Text(
                            text = "Очистить историю сообщений",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Spacer(modifier = Modifier.height(3.dp))

                Surface(
                    shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 18.dp, bottomEnd = 18.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onDismiss()
                            onDeleteChat()
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 15.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Text(
                            text = "Удалить чат",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                TextButton(
                    onClick = onDismiss,
                    shape = FloatingPillShape,
                    modifier = Modifier.fillMaxWidth().height(44.dp)
                ) {
                    Text("Закрыть", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

/**
 * M3 Expressive Centered Confirmation Dialog.
 * Compact expressive modal for delete/clear actions instead of stretched bottom sheets.
 */
@Composable
fun ExpressiveConfirmDialog(
    visible: Boolean,
    title: String,
    message: String,
    confirmText: String = "Удалить",
    cancelText: String = "Отмена",
    isDestructive: Boolean = true,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    if (!visible) return

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 6.dp,
            shadowElevation = 12.dp,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = FloatingPillShape,
                        modifier = Modifier.weight(1f).height(46.dp)
                    ) {
                        Text(cancelText, color = MaterialTheme.colorScheme.onSurface)
                    }

                    Button(
                        onClick = {
                            onDismiss()
                            onConfirm()
                        },
                        shape = FloatingPillShape,
                        colors = if (isDestructive) {
                            ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = MaterialTheme.colorScheme.onError
                            )
                        } else {
                            ButtonDefaults.buttonColors()
                        },
                        modifier = Modifier.weight(1f).height(46.dp)
                    ) {
                        Text(confirmText, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
