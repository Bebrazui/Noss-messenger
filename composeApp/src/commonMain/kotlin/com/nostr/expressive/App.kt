package com.nostr.expressive

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import nostr_expressive_messenger.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import com.nostr.expressive.ui.picker.toImageBitmap
import com.nostr.expressive.shared.crypto.Nip19
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nostr.expressive.shared.crypto.KeyManager
import com.nostr.expressive.shared.crypto.NostrIdentity
import com.nostr.expressive.shared.crypto.PlatformCrypto
import com.nostr.expressive.shared.repository.ChatMessage
import com.nostr.expressive.shared.repository.ChatSummary
import com.nostr.expressive.shared.repository.MessengerRepository
import com.nostr.expressive.shared.repository.PublicChannel
import com.nostr.expressive.shared.repository.UsernameCheckResult
import com.nostr.expressive.ui.components.ArrowBackIcon
import com.nostr.expressive.ui.components.CameraVectorIcon
import com.nostr.expressive.ui.components.CheckVectorIcon
import com.nostr.expressive.ui.components.CrossVectorIcon
import com.nostr.expressive.ui.components.ExpressiveFloatingToolbar
import com.nostr.expressive.ui.components.ExpressiveMessageBubble
import com.nostr.expressive.ui.components.ExpressiveSplitButton
import com.nostr.expressive.ui.components.NostrLogo
import com.nostr.expressive.ui.components.NossAvatar
import com.nostr.expressive.ui.components.PlusIcon
import com.nostr.expressive.ui.components.SearchIcon
import com.nostr.expressive.ui.components.SettingsGearIcon
import com.nostr.expressive.ui.navigation.PlatformBackHandler
import com.nostr.expressive.ui.picker.rememberImagePicker
import com.nostr.expressive.ui.screens.ProfileSetupScreen
import com.nostr.expressive.ui.theme.ExpressiveCookie9Shape
import com.nostr.expressive.ui.theme.FloatingPillShape
import com.nostr.expressive.ui.theme.InboundBubbleShape
import com.nostr.expressive.ui.theme.NostrExpressiveTheme
import com.nostr.expressive.ui.theme.OutboundBubbleShape
import kotlinx.coroutines.delay

enum class Screen {
    ONBOARDING,
    PROFILE_SETUP,
    CHATS,
    PROFILE,
    SETTINGS,
    CHAT_DETAIL,
    CHANNEL_DETAIL
}

@Composable
fun App() {
    NostrExpressiveTheme {
        var currentIdentity by remember { mutableStateOf(KeyManager.getSavedIdentity()) }
        var pendingNewIdentity by remember { mutableStateOf<NostrIdentity?>(null) }
        var isImportedSession by remember { mutableStateOf(false) }

        if (pendingNewIdentity != null) {
            KeyBackupScreen(
                identity = pendingNewIdentity!!,
                onConfirmed = {
                    currentIdentity = pendingNewIdentity
                    pendingNewIdentity = null
                }
            )
        } else if (currentIdentity == null) {
            OnboardingScreen(onIdentityReady = { identity, isNew ->
                if (isNew) {
                    pendingNewIdentity = identity
                } else {
                    isImportedSession = true
                    currentIdentity = identity
                }
            })
        } else {
            val identity = currentIdentity!!
            val repository = remember(identity.pubkeyHex) {
                val sessionKey = PlatformCrypto.sha256((identity.privkeyHex + "_SESSION_V1").encodeToByteArray())
                val dbKey = PlatformCrypto.sha256((identity.privkeyHex + "_DATABASE_ENC_KEY_V1").encodeToByteArray())
                MessengerRepository(
                    myPubkey = identity.pubkeyHex,
                    myPrivkeyHex = identity.privkeyHex,
                    sessionKey = sessionKey,
                    dbKey = dbKey
                )
            }

            LaunchedEffect(identity.pubkeyHex) {
                repository.markProfileConfigured()
            }

            MainAppContent(
                identity = identity,
                repository = repository,
                onLogout = {
                    KeyManager.clearIdentity()
                    currentIdentity = null
                    isImportedSession = false
                }
            )
        }
    }
}

@Composable
fun KeyBackupScreen(
    identity: NostrIdentity,
    onConfirmed: () -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    var copied by remember { mutableStateOf(false) }

    val backupTitle = runCatching { stringResource(Res.string.backup_title) }.getOrNull()?.ifBlank { null } ?: "Ваш секретный ключ"
    val backupSubtitle = runCatching { stringResource(Res.string.backup_subtitle) }.getOrNull()?.ifBlank { null } ?: "Сохраните его сейчас. В Noss нет серверов, поэтому восстановить аккаунт без этого ключа невозможно."
    val copyBtnText = runCatching { stringResource(Res.string.backup_copy_btn) }.getOrNull()?.ifBlank { null } ?: "Скопировать секретный ключ"
    val continueBtnText = runCatching { stringResource(Res.string.backup_continue_btn) }.getOrNull()?.ifBlank { null } ?: "Я сохранил ключ"
    val secretKeyLabel = runCatching { stringResource(Res.string.profile_secret_key) }.getOrNull()?.ifBlank { null } ?: "Секретный ключ"
    val copiedLabel = runCatching { stringResource(Res.string.profile_copied) }.getOrNull()?.ifBlank { null } ?: "Скопировано"

    LaunchedEffect(copied) {
        if (copied) {
            delay(2000L)
            copied = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 440.dp)
                .fillMaxWidth()
                .fillMaxHeight()
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(20.dp))

                NostrLogo(
                    size = 72.dp,
                    tint = MaterialTheme.colorScheme.onSurface,
                    cutoutColor = MaterialTheme.colorScheme.background
                )

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = backupTitle,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = backupSubtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(28.dp))

                // Secret Key Container
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = secretKeyLabel,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            if (copied) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = copiedLabel,
                                        color = MaterialTheme.colorScheme.primary,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = identity.nsec,
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(identity.nsec))
                        copied = true
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = FloatingPillShape,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
                ) {
                    Text(
                        text = if (copied) "✓ Ключ скопирован" else copyBtnText,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Bottom Primary Confirmation
            Button(
                onClick = onConfirmed,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = FloatingPillShape,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text(
                    text = continueBtnText,
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
        }
    }
}

@Composable
fun OnboardingScreen(onIdentityReady: (NostrIdentity, Boolean) -> Unit) {
    var importMode by remember { mutableStateOf(false) }
    var nsecInput by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val welcomeTitle = runCatching { stringResource(Res.string.welcome_title) }.getOrNull()?.ifBlank { null } ?: "Привет, это Noss"
    val welcomeSubtitle = runCatching { stringResource(Res.string.welcome_subtitle) }.getOrNull()?.ifBlank { null } ?: "Децентрализованный приватный мессенджер без серверов"
    val welcomeStartBtn = runCatching { stringResource(Res.string.welcome_start_btn) }.getOrNull()?.ifBlank { null } ?: "Приступить"
    val welcomeHaveKeyBtn = runCatching { stringResource(Res.string.welcome_have_key_btn) }.getOrNull()?.ifBlank { null } ?: "У меня уже есть ключ (nsec)"
    val welcomeImportBtn = runCatching { stringResource(Res.string.welcome_import_btn) }.getOrNull()?.ifBlank { null } ?: "Войти по ключу"
    val welcomeNsecHint = runCatching { stringResource(Res.string.welcome_nsec_hint) }.getOrNull()?.ifBlank { null } ?: "nsec1... или hex приватный ключ"
    val welcomeBackBtn = runCatching { stringResource(Res.string.welcome_back_btn) }.getOrNull()?.ifBlank { null } ?: "Назад"

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 440.dp)
                .fillMaxWidth()
                .fillMaxHeight()
                .padding(horizontal = 28.dp, vertical = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Center Logo & Greeting
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                NostrLogo(
                    size = 110.dp,
                    tint = MaterialTheme.colorScheme.onSurface,
                    cutoutColor = MaterialTheme.colorScheme.background
                )

                Spacer(modifier = Modifier.height(28.dp))

                Text(
                    text = welcomeTitle,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = welcomeSubtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }

            // Bottom Actions
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (!importMode) {
                    Button(
                        onClick = {
                            val newId = KeyManager.generateNewIdentity()
                            onIdentityReady(newId, true)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        shape = FloatingPillShape,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text(
                            text = welcomeStartBtn,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    TextButton(
                        onClick = { importMode = true },
                        modifier = Modifier.fillMaxWidth().height(44.dp)
                    ) {
                        Text(
                            text = welcomeHaveKeyBtn,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                } else {
                    OutlinedTextField(
                        value = nsecInput,
                        onValueChange = {
                            nsecInput = it
                            errorMessage = null
                        },
                        placeholder = { Text(welcomeNsecHint) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = Color.Transparent,
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow
                        )
                    )

                    if (errorMessage != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(errorMessage!!, color = Color.Red, style = MaterialTheme.typography.bodySmall)
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = {
                            try {
                                val id = KeyManager.importIdentity(nsecInput.trim())
                                onIdentityReady(id, false)
                            } catch (e: Exception) {
                                errorMessage = "Некорректный ключ: ${e.message}"
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = FloatingPillShape,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text(
                            text = welcomeImportBtn,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    TextButton(onClick = { importMode = false }) {
                        Text(welcomeBackBtn, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
fun MainAppContent(
    identity: NostrIdentity,
    repository: MessengerRepository,
    onLogout: () -> Unit
) {
    var currentScreen by remember { mutableStateOf(Screen.CHATS) }
    var currentTab by remember { mutableStateOf(0) }
    var activeChatId by remember { mutableStateOf<String?>(null) }
    var activeChatTitle by remember { mutableStateOf("") }
    var activeChatPubkey by remember { mutableStateOf("") }

    var activeChannelId by remember { mutableStateOf<String?>(null) }
    var activeChannelName by remember { mutableStateOf("") }
    var activeChannelAbout by remember { mutableStateOf("") }

    var showActionPicker by remember { mutableStateOf(false) }
    var showNewChatDialog by remember { mutableStateOf(false) }
    var showNewChannelDialog by remember { mutableStateOf(false) }

    val chats by repository.chats.collectAsState()
    val channels by repository.channels.collectAsState()
    val allMessages by repository.messages.collectAsState()
    val allChannelMessages by repository.channelMessages.collectAsState()
    val profile by repository.userProfile.collectAsState()

    val isDetailView = currentScreen == Screen.CHAT_DETAIL || currentScreen == Screen.CHANNEL_DETAIL || currentScreen == Screen.PROFILE_SETUP || currentScreen == Screen.SETTINGS

    // Hardware back gesture handling for Android
    PlatformBackHandler(enabled = isDetailView) {
        currentScreen = when (currentScreen) {
            Screen.PROFILE_SETUP -> Screen.PROFILE
            Screen.SETTINGS -> Screen.PROFILE
            else -> Screen.CHATS
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        // Animated screen transitions (logical spatial direction)
        AnimatedContent(
            targetState = currentScreen,
            transitionSpec = {
                // Direction model:
                // 1. Drill-down forward (into chat, channel, setup, settings): right-to-left
                val isDrillDown = targetState == Screen.CHAT_DETAIL ||
                                  targetState == Screen.CHANNEL_DETAIL ||
                                  targetState == Screen.PROFILE_SETUP ||
                                  targetState == Screen.SETTINGS

                // 2. Tab switch rightwards (Chats [left] -> Profile [right]): right-to-left
                val isTabRight = initialState == Screen.CHATS && targetState == Screen.PROFILE

                if (isDrillDown || isTabRight) {
                    // Moving forward/rightwards: enters from RIGHT, exits to LEFT
                    (slideInHorizontally(tween(300, easing = FastOutSlowInEasing)) { width -> width } +
                     fadeIn(tween(220))).togetherWith(
                        slideOutHorizontally(tween(300, easing = FastOutSlowInEasing)) { width -> -width / 4 } +
                        fadeOut(tween(220))
                    )
                } else {
                    // Moving back/leftwards: enters from LEFT, exits to RIGHT
                    (slideInHorizontally(tween(300, easing = FastOutSlowInEasing)) { width -> -width / 4 } +
                     fadeIn(tween(220))).togetherWith(
                        slideOutHorizontally(tween(300, easing = FastOutSlowInEasing)) { width -> width } +
                        fadeOut(tween(220))
                    )
                }
            }
        ) { target ->
            when (target) {
                Screen.CHATS -> {
                    ChatListScreen(
                        chats = chats,
                        channels = channels,
                        repository = repository,
                        onChatClick = { chat ->
                            activeChatId = chat.chatId
                            activeChatTitle = chat.peerDisplayName
                            activeChatPubkey = chat.peerPubkey
                            currentScreen = Screen.CHAT_DETAIL
                        },
                        onUserClick = { user ->
                            val created = repository.startNewChat(user.pubkeyHex, user.displayName)
                            activeChatId = created.chatId
                            activeChatTitle = created.peerDisplayName
                            activeChatPubkey = created.peerPubkey
                            currentScreen = Screen.CHAT_DETAIL
                        },
                        onChannelClick = { channel ->
                            activeChannelId = channel.id
                            activeChannelName = channel.name
                            activeChannelAbout = channel.about
                            repository.subscribeToChannel(channel.id)
                            currentScreen = Screen.CHANNEL_DETAIL
                        },
                        onClearChat = { chat -> repository.clearChat(chat.chatId) },
                        onDeleteChat = { chat -> repository.deleteChat(chat.chatId) }
                    )
                }
                Screen.PROFILE -> {
                    ProfileScreen(
                        repository = repository,
                        npub = identity.npub,
                        displayName = profile.displayName,
                        name = profile.name,
                        about = profile.about,
                        picture = profile.picture,
                        banner = profile.banner,
                        onEditClick = { currentScreen = Screen.PROFILE_SETUP },
                        onSettingsClick = { currentScreen = Screen.SETTINGS }
                    )
                }
                Screen.SETTINGS -> {
                    SettingsScreen(
                        repository = repository,
                        npub = identity.npub,
                        nsec = identity.nsec,
                        onBack = { currentScreen = Screen.PROFILE },
                        onLogout = onLogout
                    )
                }
                Screen.PROFILE_SETUP -> {
                    ProfileSetupScreen(
                        repository = repository,
                        onComplete = { currentScreen = Screen.PROFILE },
                        onBack = { currentScreen = Screen.PROFILE }
                    )
                }
                Screen.CHAT_DETAIL -> {
                    activeChatId?.let { chatId ->
                        val messages = allMessages[chatId] ?: emptyList()
                        val currentChat = chats.find { it.chatId == chatId || it.peerPubkey.equals(activeChatPubkey, ignoreCase = true) }
                        val displayTitle = currentChat?.peerDisplayName?.ifBlank { null } ?: activeChatTitle
                        val chatImagePicker = rememberImagePicker { bytes, mime ->
                            repository.sendMediaMessage(chatId, activeChatPubkey, bytes, mime)
                        }
                        ChatDetailScreen(
                            title = displayTitle,
                            peerPubkey = activeChatPubkey,
                            messages = messages,
                            avatarUrl = currentChat?.peerAvatarUrl,
                            onBack = { currentScreen = Screen.CHATS },
                            onSendMessage = { text ->
                                repository.sendMessage(chatId, activeChatPubkey, text)
                            },
                            onSendMedia = {
                                chatImagePicker()
                            },
                            onClearChat = {
                                repository.clearChat(chatId)
                            },
                            onDeleteChat = {
                                repository.deleteChat(chatId)
                                currentScreen = Screen.CHATS
                            },
                            onDeleteMessage = { messageId ->
                                repository.deleteMessage(chatId, messageId)
                            }
                        )
                    }
                }
                Screen.CHANNEL_DETAIL -> {
                    activeChannelId?.let { chanId ->
                        val msgs = allChannelMessages[chanId] ?: emptyList()
                        ChannelDetailScreen(
                            channelId = chanId,
                            name = activeChannelName,
                            about = activeChannelAbout,
                            messages = msgs,
                            onBack = { currentScreen = Screen.CHATS },
                            onSendMessage = { text ->
                                repository.sendChannelMessage(chanId, text)
                            }
                        )
                    }
                }
                else -> {}
            }
        }

        // Floating Toolbar: 2 clean tabs (Chats & Profile) + Vibrant Action FAB
        if (!isDetailView) {
            ExpressiveFloatingToolbar(
                currentTab = currentTab,
                onTabSelected = { tabIndex ->
                    currentTab = tabIndex
                    currentScreen = if (tabIndex == 0) Screen.CHATS else Screen.PROFILE
                },
                onFabClick = {
                    showActionPicker = true
                },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }

        // M3 Expressive Action Sheet (New Chat vs Create Channel)
        ExpressiveModalBottomSheet(
            visible = showActionPicker,
            onDismissRequest = { showActionPicker = false }
        ) {
            Box(
                modifier = Modifier
                    .width(36.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
            )
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(Res.string.action_sheet_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            val actionChatTitle = stringResource(Res.string.action_new_chat)
            val actionChatDesc = stringResource(Res.string.action_new_chat_desc)
            val actionChanTitle = stringResource(Res.string.action_new_channel)
            val actionChanDesc = stringResource(Res.string.action_new_channel_desc)

            val actions = listOf(
                Triple(actionChatTitle, actionChatDesc, true),
                Triple(actionChanTitle, actionChanDesc, false)
            )

            actions.forEachIndexed { index, (title, subtitle, isChat) ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            showActionPicker = false
                            if (isChat) showNewChatDialog = true else showNewChannelDialog = true
                        }
                        .padding(vertical = 1.5.dp),
                    shape = getM3EConnectedShape(index, actions.size, cornerRadius = 20.dp, innerRadius = 4.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (isChat) "@" else "#",
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))
        }

        NewChatSheet(
            repository = repository,
            visible = showNewChatDialog,
            onDismiss = { showNewChatDialog = false },
            onConfirm = { npubInput, nameInput ->
                showNewChatDialog = false
                val created = repository.startNewChat(npubInput.trim(), nameInput.trim())
                activeChatId = created.chatId
                activeChatTitle = created.peerDisplayName
                activeChatPubkey = created.peerPubkey
                currentScreen = Screen.CHAT_DETAIL
            }
        )

        NewChannelSheet(
            visible = showNewChannelDialog,
            onDismiss = { showNewChannelDialog = false },
            onConfirm = { name, about ->
                showNewChannelDialog = false
                val newChan = repository.createChannel(name, about)
                activeChannelId = newChan.id
                activeChannelName = newChan.name
                activeChannelAbout = newChan.about
                currentScreen = Screen.CHANNEL_DETAIL
            }
        )
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun ExpressiveModalBottomSheet(
    visible: Boolean,
    onDismissRequest: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(220)),
        exit = fadeOut(animationSpec = tween(180))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.65f))
                .clickable { onDismissRequest() }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .clickable(enabled = false) {}
                    .animateEnterExit(
                        enter = slideInVertically(
                            initialOffsetY = { it },
                            animationSpec = tween(280, easing = FastOutSlowInEasing)
                        ),
                        exit = slideOutVertically(
                            targetOffsetY = { it },
                            animationSpec = tween(200, easing = FastOutSlowInEasing)
                        )
                    )
            ) {
                Surface(
                    shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        content = content
                    )
                }
            }
        }
    }
}

@Composable
fun NewChatSheet(
    repository: MessengerRepository,
    visible: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var npub by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    val communityUsers by repository.communityUsers.collectAsState()

    val cleanInput = npub.trim()
    val resolvedHex = remember(cleanInput) {
        if (cleanInput.startsWith("npub1")) {
            try { Nip19.parseNpub(cleanInput).lowercase() } catch (_: Exception) { null }
        } else if (cleanInput.length == 64 && cleanInput.all { it in "0123456789abcdefABCDEF" }) {
            cleanInput.lowercase()
        } else null
    }

    LaunchedEffect(resolvedHex) {
        if (resolvedHex != null) {
            repository.fetchUserProfile(resolvedHex)
        }
    }

    val matchedUser = remember(resolvedHex, communityUsers) {
        if (resolvedHex == null) null
        else communityUsers.find { it.pubkeyHex.equals(resolvedHex, ignoreCase = true) }
    }

    LaunchedEffect(matchedUser) {
        if (matchedUser != null && name.isBlank()) {
            name = matchedUser.displayName.ifBlank { matchedUser.name }
        }
    }

    ExpressiveModalBottomSheet(
        visible = visible,
        onDismissRequest = onDismiss
    ) {
        Box(
            modifier = Modifier
                .width(36.dp)
                .height(4.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
        )
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(Res.string.new_chat_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(14.dp))

        OutlinedTextField(
            value = npub,
            onValueChange = { npub = it },
            placeholder = { Text(stringResource(Res.string.new_chat_npub_hint)) },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = Color.Transparent,
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow
            )
        )

        if (matchedUser != null) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    NossAvatar(
                        avatarUrl = matchedUser.picture,
                        displayName = matchedUser.displayName.ifBlank { matchedUser.name },
                        size = 40.dp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = matchedUser.displayName.ifBlank { matchedUser.name },
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "@${matchedUser.name}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            placeholder = { Text(stringResource(Res.string.new_chat_name_hint)) },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = Color.Transparent,
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow
            )
        )

        Spacer(modifier = Modifier.height(20.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.weight(1f).height(48.dp)
            ) {
                Text(stringResource(Res.string.btn_cancel), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Button(
                onClick = {
                    if (cleanInput.isNotBlank()) {
                        val finalName = name.trim().ifBlank {
                            matchedUser?.displayName?.ifBlank { null }
                                ?: matchedUser?.name?.ifBlank { null }
                                ?: "User ${resolvedHex?.take(6) ?: cleanInput.takeLast(6)}"
                        }
                        onConfirm(cleanInput, finalName)
                        npub = ""
                        name = ""
                    }
                },
                enabled = cleanInput.isNotBlank(),
                shape = FloatingPillShape,
                modifier = Modifier.weight(2f).height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text(stringResource(Res.string.btn_start), color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.SemiBold)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun NewChannelSheet(
    visible: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var about by remember { mutableStateOf("") }

    ExpressiveModalBottomSheet(
        visible = visible,
        onDismissRequest = onDismiss
    ) {
        Box(
            modifier = Modifier
                .width(36.dp)
                .height(4.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
        )
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(Res.string.new_channel_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(14.dp))

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            placeholder = { Text(stringResource(Res.string.new_channel_name_hint)) },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = Color.Transparent,
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow
            )
        )

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = about,
            onValueChange = { about = it },
            placeholder = { Text(stringResource(Res.string.new_channel_about_hint)) },
            minLines = 2,
            maxLines = 3,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = Color.Transparent,
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow
            )
        )

        Spacer(modifier = Modifier.height(20.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.weight(1f).height(48.dp)
            ) {
                Text(stringResource(Res.string.btn_cancel), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onConfirm(name, about)
                    }
                },
                enabled = name.isNotBlank(),
                shape = FloatingPillShape,
                modifier = Modifier.weight(2f).height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text(stringResource(Res.string.btn_create), color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.SemiBold)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

/**
 * Material 3 Expressive connected card shape helper for grouped lists (Option Б).
 * Top item has rounded top, middle items are connected, bottom item has rounded bottom.
 */
fun getM3EConnectedShape(
    index: Int,
    totalCount: Int,
    cornerRadius: Dp = 20.dp,
    innerRadius: Dp = 4.dp
): Shape {
    return if (totalCount <= 1) {
        RoundedCornerShape(cornerRadius)
    } else if (index == 0) {
        RoundedCornerShape(
            topStart = cornerRadius,
            topEnd = cornerRadius,
            bottomStart = innerRadius,
            bottomEnd = innerRadius
        )
    } else if (index == totalCount - 1) {
        RoundedCornerShape(
            topStart = innerRadius,
            topEnd = innerRadius,
            bottomStart = cornerRadius,
            bottomEnd = cornerRadius
        )
    } else {
        RoundedCornerShape(innerRadius)
    }
}

/**
 * Unified Chat List with Search integrating Noss users, chats and verified public channels
 */
@Composable
fun ChatListScreen(
    chats: List<ChatSummary>,
    channels: List<PublicChannel>,
    repository: MessengerRepository,
    onChatClick: (ChatSummary) -> Unit,
    onUserClick: (com.nostr.expressive.shared.repository.NossUser) -> Unit,
    onChannelClick: (PublicChannel) -> Unit,
    onClearChat: (ChatSummary) -> Unit = {},
    onDeleteChat: (ChatSummary) -> Unit = {}
) {
    var searchQuery by remember { mutableStateOf("") }
    val communityUsers by repository.communityUsers.collectAsState()
    var selectedChatForActions by remember { mutableStateOf<ChatSummary?>(null) }

    LaunchedEffect(searchQuery) {
        if (searchQuery.isNotBlank()) {
            repository.searchNossUsers(searchQuery)
        }
    }

    val filteredChats = remember(chats, searchQuery) {
        if (searchQuery.isBlank()) chats
        else chats.filter {
            it.peerDisplayName.contains(searchQuery.trim(), ignoreCase = true)
        }
    }

    val foundUsers = remember(searchQuery, communityUsers) {
        val clean = searchQuery.trim().removePrefix("@").lowercase()
        if (clean.isBlank()) emptyList()
        else {
            communityUsers.filter { user ->
                user.name.lowercase().contains(clean) ||
                user.displayName.lowercase().contains(clean) ||
                user.npub.lowercase().contains(clean) ||
                user.pubkeyHex.lowercase().contains(clean)
            }
        }
    }

    val filteredChannels = remember(channels, searchQuery) {
        if (searchQuery.isBlank()) emptyList()
        else channels.filter {
            it.name.contains(searchQuery.trim().removePrefix("#"), ignoreCase = true) ||
            it.about.contains(searchQuery.trim(), ignoreCase = true)
        }
    }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.background)
                    .padding(horizontal = 16.dp)
            ) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(Res.string.chats_title),
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        fontSize = 32.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Unified Search Bar with generous expressive padding & height
                Surface(
                    shape = FloatingPillShape,
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SearchIcon(tint = MaterialTheme.colorScheme.onSurfaceVariant, size = 18.dp)
                        Spacer(modifier = Modifier.width(12.dp))
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            singleLine = true,
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            textStyle = TextStyle(
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 14.sp
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            decorationBox = { innerTextField ->
                                Box(contentAlignment = Alignment.CenterStart) {
                                    if (searchQuery.isEmpty()) {
                                        Text(
                                            text = stringResource(Res.string.search_hint),
                                            fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    innerTextField()
                                }
                            }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 96.dp)
        ) {
            if (searchQuery.isNotBlank()) {
                // Search Mode: Users section
                if (foundUsers.isNotEmpty()) {
                    item {
                        Text(
                            text = stringResource(Res.string.search_users_section).uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                        )
                    }
                    itemsIndexed(foundUsers) { index, user ->
                        NossUserListItem(
                            user = user,
                            shape = getM3EConnectedShape(index, foundUsers.size),
                            onClick = { onUserClick(user) }
                        )
                    }
                    item { Spacer(modifier = Modifier.height(10.dp)) }
                }

                // Search Mode: Public Channels section
                if (filteredChannels.isNotEmpty()) {
                    item {
                        Text(
                            text = stringResource(Res.string.channels_section).uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                        )
                    }
                    itemsIndexed(filteredChannels) { index, channel ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onChannelClick(channel) }
                                .padding(horizontal = 16.dp, vertical = 1.5.dp),
                            shape = getM3EConnectedShape(index, filteredChannels.size),
                            color = MaterialTheme.colorScheme.surfaceContainerLow
                        ) {
                            Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text("#", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleMedium)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(channel.name, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                                    if (channel.about.isNotBlank()) {
                                        Text(channel.about, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                                    }
                                }
                            }
                        }
                    }
                    item { Spacer(modifier = Modifier.height(10.dp)) }
                }

                // Search Mode: Matching active chats
                if (filteredChats.isNotEmpty()) {
                    item {
                        Text(
                            text = "АКТИВНЫЕ ДИАЛОГИ",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                        )
                    }
                    itemsIndexed(filteredChats) { index, chat ->
                        ChatListItem(
                            chat = chat,
                            shape = getM3EConnectedShape(index, filteredChats.size),
                            onClick = { onChatClick(chat) },
                            onLongClick = { selectedChatForActions = chat }
                        )
                    }
                }

                // Nothing found in search
                if (foundUsers.isEmpty() && filteredChannels.isEmpty() && filteredChats.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 80.dp, start = 24.dp, end = 24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = stringResource(Res.string.search_no_results),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Проверьте юзернейм @handle или название",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                }
            } else {
                // Default Mode: active chats list or empty placeholder
                if (filteredChats.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 100.dp, start = 24.dp, end = 24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = stringResource(Res.string.empty_chats_title),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = stringResource(Res.string.empty_chats_subtitle),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                } else {
                    itemsIndexed(filteredChats) { index, chat ->
                        ChatListItem(
                            chat = chat,
                            shape = getM3EConnectedShape(index, filteredChats.size),
                            onClick = { onChatClick(chat) },
                            onLongClick = { selectedChatForActions = chat }
                        )
                    }
                }
            }
        }
    }

    ExpressiveModalBottomSheet(
        visible = selectedChatForActions != null,
        onDismissRequest = { selectedChatForActions = null }
    ) {
        selectedChatForActions?.let { chat ->
            Box(
                modifier = Modifier
                    .width(36.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
            )
            Spacer(modifier = Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                NossAvatar(
                    avatarUrl = chat.peerAvatarUrl,
                    displayName = chat.peerDisplayName,
                    size = 40.dp
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(chat.peerDisplayName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Диалог", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onClearChat(chat)
                        selectedChatForActions = null
                    },
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stringResource(Res.string.chat_menu_clear), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onDeleteChat(chat)
                        selectedChatForActions = null
                    },
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(stringResource(Res.string.chat_menu_delete), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.error)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun NossUserListItem(
    user: com.nostr.expressive.shared.repository.NossUser,
    shape: Shape = RoundedCornerShape(18.dp),
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 1.5.dp),
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            NossAvatar(
                avatarUrl = user.picture,
                displayName = user.displayName.ifBlank { user.name },
                size = 44.dp
            )

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = user.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "@${user.name}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                if (user.about.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = user.about,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
fun ChatListItem(
    chat: ChatSummary,
    shape: Shape = RoundedCornerShape(18.dp),
    onClick: () -> Unit,
    onLongClick: () -> Unit = {}
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 1.5.dp)
            .clip(shape)
            .pointerInput(chat.chatId) {
                detectTapGestures(
                    onTap = { onClick() },
                    onLongPress = { onLongClick() }
                )
            },
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            NossAvatar(
                avatarUrl = chat.peerAvatarUrl,
                displayName = chat.peerDisplayName,
                size = 44.dp
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = chat.peerDisplayName,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = chat.lastMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }

            if (chat.unreadCount > 0) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = chat.unreadCount.toString(),
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        lineHeight = 12.sp,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatDetailScreen(
    title: String,
    peerPubkey: String,
    messages: List<ChatMessage>,
    onBack: () -> Unit,
    onSendMessage: (String) -> Unit,
    onSendMedia: () -> Unit,
    avatarUrl: String? = null,
    onClearChat: () -> Unit = {},
    onDeleteChat: () -> Unit = {},
    onDeleteMessage: (String) -> Unit = {}
) {
    var inputText by remember { mutableStateOf("") }
    var showChatMenu by remember { mutableStateOf(false) }
    var showClearConfirm by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var selectedMessageForAction by remember { mutableStateOf<ChatMessage?>(null) }
    val clipboardManager = LocalClipboardManager.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        NossAvatar(
                            avatarUrl = avatarUrl,
                            displayName = title,
                            size = 36.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(title, style = MaterialTheme.typography.titleMedium)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        ArrowBackIcon(tint = MaterialTheme.colorScheme.onSurface)
                    }
                },
                actions = {
                    IconButton(onClick = { showChatMenu = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Menu",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                )
            )
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Quick Attach Media Button
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                            .clickable { onSendMedia() },
                        contentAlignment = Alignment.Center
                    ) {
                        PlusIcon(size = 16.dp, tint = MaterialTheme.colorScheme.primary)
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    TextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        placeholder = { Text(stringResource(Res.string.chat_message_hint)) },
                        modifier = Modifier
                            .weight(1f)
                            .clip(FloatingPillShape),
                        colors = TextFieldDefaults.colors(
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        )
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    ExpressiveSplitButton(
                        onSend = {
                            if (inputText.isNotBlank()) {
                                onSendMessage(inputText)
                                inputText = ""
                            }
                        },
                        onZap = {
                            onSendMessage("[Zap: 21 sats]")
                        }
                    )
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            items(messages) { msg ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .pointerInput(msg.id) {
                            detectTapGestures(
                                onLongPress = {
                                    selectedMessageForAction = msg
                                }
                            )
                        }
                ) {
                    ExpressiveMessageBubble(message = msg)
                }
            }
        }
    }

    // Chat Actions Menu Sheet (Clear history, Delete chat)
    ExpressiveModalBottomSheet(
        visible = showChatMenu,
        onDismissRequest = { showChatMenu = false }
    ) {
        Box(
            modifier = Modifier
                .width(36.dp)
                .height(4.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
        )
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(18.dp))

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    showChatMenu = false
                    showClearConfirm = true
                },
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(Res.string.chat_menu_clear),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    showChatMenu = false
                    showDeleteConfirm = true
                },
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = stringResource(Res.string.chat_menu_delete),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }

    // Message Actions Sheet (Copy, Delete)
    ExpressiveModalBottomSheet(
        visible = selectedMessageForAction != null,
        onDismissRequest = { selectedMessageForAction = null }
    ) {
        selectedMessageForAction?.let { msg ->
            Box(
                modifier = Modifier
                    .width(36.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
            )
            Spacer(modifier = Modifier.height(16.dp))

            val preview = if (msg.text.startsWith("nostr:media:")) "📷 Файл" else msg.text
            Text(
                text = preview.take(120),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3
            )

            Spacer(modifier = Modifier.height(18.dp))

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        if (!msg.text.startsWith("nostr:media:")) {
                            clipboardManager.setText(AnnotatedString(msg.text))
                        }
                        selectedMessageForAction = null
                    },
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(Res.string.chat_menu_copy_msg),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onDeleteMessage(msg.id)
                        selectedMessageForAction = null
                    },
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = stringResource(Res.string.chat_menu_delete_msg),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Confirmation Sheet: Clear History
    ExpressiveModalBottomSheet(
        visible = showClearConfirm,
        onDismissRequest = { showClearConfirm = false }
    ) {
        Box(
            modifier = Modifier
                .width(36.dp)
                .height(4.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
        )
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(Res.string.chat_clear_confirm_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(Res.string.chat_clear_confirm_desc),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(20.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = { showClearConfirm = false },
                modifier = Modifier.weight(1f).height(48.dp),
                shape = FloatingPillShape,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
            ) {
                Text(stringResource(Res.string.btn_cancel), color = MaterialTheme.colorScheme.onSurface)
            }
            Button(
                onClick = {
                    showClearConfirm = false
                    onClearChat()
                },
                modifier = Modifier.weight(1f).height(48.dp),
                shape = FloatingPillShape,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text(stringResource(Res.string.chat_menu_clear), color = MaterialTheme.colorScheme.onError)
            }
        }
        Spacer(modifier = Modifier.height(20.dp))
    }

    // Confirmation Sheet: Delete Chat
    ExpressiveModalBottomSheet(
        visible = showDeleteConfirm,
        onDismissRequest = { showDeleteConfirm = false }
    ) {
        Box(
            modifier = Modifier
                .width(36.dp)
                .height(4.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
        )
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(Res.string.chat_delete_confirm_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(Res.string.chat_delete_confirm_desc),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(20.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = { showDeleteConfirm = false },
                modifier = Modifier.weight(1f).height(48.dp),
                shape = FloatingPillShape,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
            ) {
                Text(stringResource(Res.string.btn_cancel), color = MaterialTheme.colorScheme.onSurface)
            }
            Button(
                onClick = {
                    showDeleteConfirm = false
                    onDeleteChat()
                },
                modifier = Modifier.weight(1f).height(48.dp),
                shape = FloatingPillShape,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text(stringResource(Res.string.chat_menu_delete), color = MaterialTheme.colorScheme.onError)
            }
        }
        Spacer(modifier = Modifier.height(20.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChannelDetailScreen(
    channelId: String,
    name: String,
    about: String,
    messages: List<ChatMessage>,
    onBack: () -> Unit,
    onSendMessage: (String) -> Unit
) {
    var inputText by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("# $name", style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = if (about.isNotBlank()) about else stringResource(Res.string.channel_public_badge),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        ArrowBackIcon(tint = MaterialTheme.colorScheme.onSurface)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                )
            )
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        placeholder = { Text(stringResource(Res.string.channel_message_hint)) },
                        modifier = Modifier
                            .weight(1f)
                            .clip(FloatingPillShape),
                        colors = TextFieldDefaults.colors(
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        )
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            if (inputText.isNotBlank()) {
                                onSendMessage(inputText)
                                inputText = ""
                            }
                        },
                        shape = FloatingPillShape
                    ) {
                        Text(stringResource(Res.string.btn_send))
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            items(messages) { msg ->
                ChannelMessageItem(msg = msg)
            }
        }
    }
}

@Composable
fun ChannelMessageItem(msg: ChatMessage) {
    val isMine = msg.isOutgoing
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            shape = if (isMine) OutboundBubbleShape else InboundBubbleShape,
            color = if (isMine) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
                if (!isMine) {
                    Text(
                        text = "npub1..." + msg.senderPubkey.takeLast(6),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                }
                Text(
                    text = msg.text,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (isMine) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
fun ProfileScreen(
    repository: MessengerRepository,
    npub: String,
    displayName: String,
    name: String,
    about: String,
    picture: String? = null,
    banner: String? = null,
    onEditClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    var copied by remember { mutableStateOf(false) }

    val bannerBytes by repository.localBannerBytes.collectAsState()
    val avatarBytes by repository.localAvatarBytes.collectAsState()
    val bannerBitmap = remember(bannerBytes) { bannerBytes?.toImageBitmap() }
    val avatarBitmap = remember(avatarBytes) { avatarBytes?.toImageBitmap() }

    LaunchedEffect(copied) {
        if (copied) {
            delay(2000L)
            copied = false
        }
    }

    val shortenedNpub = remember(npub) {
        if (npub.length > 20) {
            "${npub.take(12)}...${npub.takeLast(8)}"
        } else npub
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 96.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Banner Header with smooth bottom fade gradient & Settings button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
                .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                .clickable { onEditClick() }
        ) {
            if (bannerBitmap != null) {
                Image(
                    bitmap = bannerBitmap,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.surfaceContainerHigh,
                                    MaterialTheme.colorScheme.surfaceContainerLowest
                                )
                            )
                        )
                )
            }

            // Smooth gradient transition from banner to profile background
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0.0f to Color.Transparent,
                            0.45f to Color.Transparent,
                            0.82f to MaterialTheme.colorScheme.background.copy(alpha = 0.75f),
                            1.0f to MaterialTheme.colorScheme.background
                        )
                    )
            )

            // Settings button in top-right of banner
            IconButton(
                onClick = onSettingsClick,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.35f))
            ) {
                SettingsGearIcon(
                    size = 20.dp,
                    tint = Color.White
                )
            }
        }

        // Avatar overlapping banner (clickable to edit) - enlarged to 112.dp with M3 Expressive 9-side cookie shape
        Box(
            modifier = Modifier
                .offset(y = (-56).dp)
                .size(112.dp)
                .clip(ExpressiveCookie9Shape)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .border(3.dp, MaterialTheme.colorScheme.background, ExpressiveCookie9Shape)
                .clickable { onEditClick() },
            contentAlignment = Alignment.Center
        ) {
            if (avatarBitmap != null) {
                Image(
                    bitmap = avatarBitmap,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Text(
                    text = (displayName.ifBlank { name }).take(2).uppercase().ifBlank { "ME" },
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .offset(y = (-36).dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = displayName.ifBlank { stringResource(Res.string.profile_default_name) },
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = if (name.isNotBlank()) "@$name" else "@${stringResource(Res.string.profile_default_handle)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )

            // Bio directly below handle (shown once)
            Spacer(modifier = Modifier.height(10.dp))
            if (about.isNotBlank()) {
                Text(
                    text = about,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
            } else {
                Text(
                    text = "Нет описания профиля",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            Button(
                onClick = onEditClick,
                shape = FloatingPillShape,
                modifier = Modifier.fillMaxWidth().height(46.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text(stringResource(Res.string.profile_edit_btn), color = MaterialTheme.colorScheme.onPrimary)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Public Key block (clickable to copy, shortened with ellipsis)
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        clipboardManager.setText(AnnotatedString(npub))
                        copied = true
                    }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(Res.string.profile_public_key),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = shortenedNpub,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    if (copied) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            modifier = Modifier.padding(start = 8.dp)
                        ) {
                            Text(
                                text = stringResource(Res.string.profile_copied),
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    repository: MessengerRepository,
    npub: String,
    nsec: String,
    onBack: () -> Unit,
    onLogout: () -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    var npubCopied by remember { mutableStateOf(false) }
    var nsecCopied by remember { mutableStateOf(false) }
    var showNsec by remember { mutableStateOf(false) }
    var showLogoutSafetySheet by remember { mutableStateOf(false) }

    val shortenedNpub = remember(npub) {
        if (npub.length > 20) "${npub.take(12)}...${npub.takeLast(8)}" else npub
    }

    LaunchedEffect(npubCopied) {
        if (npubCopied) {
            delay(2000L)
            npubCopied = false
        }
    }
    LaunchedEffect(nsecCopied) {
        if (nsecCopied) {
            delay(2000L)
            nsecCopied = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Настройки",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        ArrowBackIcon(tint = MaterialTheme.colorScheme.onSurface)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            // Section 1: Ключи и безопасность
            Text(
                text = "Ключи и безопасность",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
            )

            // Public Key card
            Surface(
                shape = getM3EConnectedShape(0, 2, cornerRadius = 18.dp, innerRadius = 4.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 1.5.dp)
                    .clickable {
                        clipboardManager.setText(AnnotatedString(npub))
                        npubCopied = true
                    }
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Публичный ключ (npub)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = shortenedNpub,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    if (npubCopied) {
                        Text(
                            text = "✓ Скопировано",
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Secret Key card
            Surface(
                shape = getM3EConnectedShape(1, 2, cornerRadius = 18.dp, innerRadius = 4.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                modifier = Modifier.fillMaxWidth().padding(vertical = 1.5.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Секретный ключ (nsec)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (nsecCopied) {
                                Text(
                                    text = "✓ Скопировано",
                                    color = MaterialTheme.colorScheme.primary,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(end = 8.dp)
                                )
                            }
                            Text(
                                text = if (showNsec) "Скрыть" else "Показать",
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .clickable { showNsec = !showNsec }
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (showNsec) "${nsec.take(12)}...${nsec.takeLast(8)}" else "••••••••••••••••••••",
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Нажмите для копирования nsec",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable {
                            clipboardManager.setText(AnnotatedString(nsec))
                            nsecCopied = true
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Section 2: Сеть Nostr
            Text(
                text = "Сеть Nostr",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
            )

            val relays = listOf("wss://relay.damus.io", "wss://relay.primal.net", "wss://nos.lol")
            relays.forEachIndexed { idx, relayUrl ->
                Surface(
                    shape = getM3EConnectedShape(idx, relays.size, cornerRadius = 18.dp, innerRadius = 4.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 1.5.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF4CAF50))
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = relayUrl,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Section 3: О приложении
            Text(
                text = "О приложении",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
            )

            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Noss Messenger v1.0.0",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Децентрализованный E2EE мессенджер на протоколе Nostr с шифрованием NIP-04/NIP-44.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Section 4: Выход из аккаунта
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showLogoutSafetySheet = true }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Выйти из аккаунта",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }

    // Safety Confirmation Bottom Sheet for Logout
    ExpressiveModalBottomSheet(
        visible = showLogoutSafetySheet,
        onDismissRequest = { showLogoutSafetySheet = false }
    ) {
        Box(
            modifier = Modifier
                .width(36.dp)
                .height(4.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
        )
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(Res.string.logout_dialog_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = stringResource(Res.string.logout_dialog_warning),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(18.dp))

        Button(
            onClick = {
                clipboardManager.setText(AnnotatedString(nsec))
                nsecCopied = true
            },
            shape = FloatingPillShape,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
        ) {
            Text(
                text = if (nsecCopied) "✓ Ключ скопирован" else stringResource(Res.string.logout_dialog_copy_nsec),
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = { showLogoutSafetySheet = false },
                modifier = Modifier.weight(1f).height(48.dp),
                shape = FloatingPillShape,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text(stringResource(Res.string.btn_cancel), color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.SemiBold)
            }

            TextButton(
                onClick = {
                    showLogoutSafetySheet = false
                    onLogout()
                },
                modifier = Modifier.weight(1f).height(48.dp)
            ) {
                Text(
                    text = stringResource(Res.string.logout_dialog_confirm),
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}
