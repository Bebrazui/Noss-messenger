package com.nostr.expressive.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nostr.expressive.shared.media.BlossomMediaManager
import com.nostr.expressive.shared.repository.MessengerRepository
import com.nostr.expressive.shared.repository.UsernameCheckResult
import com.nostr.expressive.ui.components.ArrowBackIcon
import com.nostr.expressive.ui.components.CheckVectorIcon
import com.nostr.expressive.ui.components.CrossVectorIcon
import com.nostr.expressive.ui.picker.rememberImagePicker
import com.nostr.expressive.ui.picker.toImageBitmap
import com.nostr.expressive.ui.theme.ExpressiveCookie9Shape
import com.nostr.expressive.ui.theme.FloatingPillShape
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import nostr_expressive_messenger.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource

@Composable
fun ProfileSetupScreen(
    repository: MessengerRepository,
    onComplete: () -> Unit,
    onBack: (() -> Unit)? = null
) {
    val coroutineScope = rememberCoroutineScope()
    val currentProfile by repository.userProfile.collectAsState()

    var username by remember {
        mutableStateOf(if (currentProfile.name == "nostr_user") "" else currentProfile.name)
    }
    var displayName by remember {
        mutableStateOf(if (currentProfile.displayName == "Anonymous Nostr") "" else currentProfile.displayName)
    }
    var about by remember {
        mutableStateOf(if (currentProfile.about.startsWith("Hardware")) "" else currentProfile.about)
    }
    var pictureUrl by remember { mutableStateOf(currentProfile.picture ?: "") }
    var bannerUrl by remember { mutableStateOf(currentProfile.banner ?: "") }

    var avatarBytes by remember { mutableStateOf<ByteArray?>(repository.localAvatarBytes.value) }
    var bannerBytes by remember { mutableStateOf<ByteArray?>(repository.localBannerBytes.value) }
    var avatarMime by remember { mutableStateOf("image/jpeg") }
    var bannerMime by remember { mutableStateOf("image/jpeg") }

    val isEditFromProfile = onBack != null
    val avatarPicker = rememberImagePicker { bytes, mime ->
        avatarBytes = bytes
        avatarMime = mime
    }
    val bannerPicker = rememberImagePicker { bytes, mime ->
        bannerBytes = bytes
        bannerMime = mime
    }

    var checkState by remember { mutableStateOf<UsernameCheckResult>(UsernameCheckResult.Idle) }
    var isSaving by remember { mutableStateOf(false) }

    // Live debounced handle availability verification
    LaunchedEffect(username) {
        val clean = username.trim().removePrefix("@").lowercase()
        if (clean.isBlank()) {
            checkState = UsernameCheckResult.Idle
            return@LaunchedEffect
        }
        if (clean.length < 3) {
            checkState = UsernameCheckResult.Invalid("Минимум 3 символа")
            return@LaunchedEffect
        }
        checkState = UsernameCheckResult.Checking
        delay(350L)
        checkState = repository.checkUsernameAvailability(clean)
    }

    val isCleanCurrentName = username.trim().removePrefix("@").equals(currentProfile.name.trim(), ignoreCase = true)
    val isUsernameValid = checkState is UsernameCheckResult.Available || (isCleanCurrentName && currentProfile.name.isNotBlank())
    val isFormValid = displayName.isNotBlank() && isUsernameValid

    // Tonal field styling according to M3 Expressive
    val tonalFieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = MaterialTheme.colorScheme.primary,
        unfocusedBorderColor = Color.Transparent,
        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        focusedTextColor = MaterialTheme.colorScheme.onSurface,
        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 480.dp)
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Top Navigation bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            ArrowBackIcon(tint = MaterialTheme.colorScheme.onSurface)
                        }
                    } else {
                        Spacer(modifier = Modifier.width(24.dp))
                    }
                }

                Text(
                    text = if (isEditFromProfile) "Редактирование профиля" else stringResource(Res.string.setup_screen_title),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (isEditFromProfile) {
                    val bannerBitmap = remember(bannerBytes) { bannerBytes?.toImageBitmap() }
                    val avatarBitmap = remember(avatarBytes) { avatarBytes?.toImageBitmap() }

                    // Banner & Avatar Customization Block
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0xFF1E2124))
                            .clickable { bannerPicker() }
                    ) {
                        if (bannerBitmap != null) {
                            Image(
                                bitmap = bannerBitmap,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        Surface(
                            shape = CircleShape,
                            color = Color.Black.copy(alpha = 0.65f),
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(10.dp)
                        ) {
                            Text(
                                text = "Сменить баннер",
                                fontSize = 11.sp,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }

                        // Avatar overlapping inside
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(start = 16.dp, bottom = 12.dp)
                                .size(68.dp)
                                .clip(ExpressiveCookie9Shape)
                                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                .border(3.dp, MaterialTheme.colorScheme.background, ExpressiveCookie9Shape)
                                .clickable { avatarPicker() },
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
                                    text = (displayName.ifBlank { username }).take(2).uppercase().ifBlank { "ME" },
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    // Optional direct URL for banner
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "URL баннера (опционально)",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedTextField(
                            value = bannerUrl,
                            onValueChange = { bannerUrl = it },
                            placeholder = { Text("https://... или выберите файл выше") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = tonalFieldColors
                        )
                    }
                }

                // Display Name field
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = stringResource(Res.string.setup_name_label),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = displayName,
                        onValueChange = { displayName = it },
                        placeholder = { Text(stringResource(Res.string.setup_name_hint)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = tonalFieldColors
                    )
                }

                // Username (@handle) field with real-time verification
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = stringResource(Res.string.setup_handle_label),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = username,
                        onValueChange = { input ->
                            username = input.filter { it.isLetterOrDigit() || it == '_' || it == '.' }
                        },
                        placeholder = { Text(stringResource(Res.string.setup_handle_hint)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = tonalFieldColors,
                        trailingIcon = {
                            when (checkState) {
                                is UsernameCheckResult.Checking -> {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                is UsernameCheckResult.Available -> {
                                    CheckVectorIcon(tint = Color(0xFF56E09E), size = 18.dp)
                                }
                                is UsernameCheckResult.Taken, is UsernameCheckResult.Invalid -> {
                                    CrossVectorIcon(tint = Color(0xFFF28B82), size = 18.dp)
                                }
                                UsernameCheckResult.Idle -> {}
                            }
                        }
                    )

                    // Real-time verification status message
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(start = 4.dp)
                    ) {
                        when (val state = checkState) {
                            is UsernameCheckResult.Checking -> {
                                Text("Проверка...", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            is UsernameCheckResult.Available -> {
                                Text("@${state.username} свободен", fontSize = 12.sp, color = Color(0xFF56E09E))
                            }
                            is UsernameCheckResult.Taken -> {
                                Text("@${state.username} уже занят", fontSize = 12.sp, color = Color(0xFFF28B82))
                            }
                            is UsernameCheckResult.Invalid -> {
                                Text(state.reason, fontSize = 12.sp, color = Color(0xFFF28B82))
                            }
                            UsernameCheckResult.Idle -> {}
                        }
                    }
                }

                // Bio / Status (optional)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = stringResource(Res.string.setup_bio_label),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = about,
                        onValueChange = { about = it },
                        placeholder = { Text(stringResource(Res.string.setup_bio_hint)) },
                        minLines = 2,
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = tonalFieldColors
                    )
                }
            }

            // Bottom Primary Action: Готово
            Button(
                onClick = {
                    val cleanName = username.trim().removePrefix("@").lowercase()
                    isSaving = true
                    coroutineScope.launch {
                        try {
                            var finalBanner = bannerUrl.ifBlank { currentProfile.banner }
                            var finalPicture = pictureUrl.ifBlank { currentProfile.picture }

                            if (bannerBytes != null) {
                                repository.setLocalBannerBytes(bannerBytes)
                                try {
                                    finalBanner = BlossomMediaManager.uploadPublicMediaBlob(bannerBytes!!, bannerMime)
                                } catch (_: Throwable) {}
                            }
                            if (avatarBytes != null) {
                                repository.setLocalAvatarBytes(avatarBytes)
                                try {
                                    finalPicture = BlossomMediaManager.uploadPublicMediaBlob(avatarBytes!!, avatarMime)
                                } catch (_: Throwable) {}
                            }

                            repository.updateAndPublishProfile(
                                displayName = displayName.trim(),
                                name = cleanName,
                                about = about.trim(),
                                picture = finalPicture,
                                banner = finalBanner
                            )
                        } catch (t: Throwable) {
                            println("Error saving profile: ${t.message}")
                        } finally {
                            isSaving = false
                            onComplete()
                        }
                    }
                },
                enabled = isFormValid && !isSaving,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = FloatingPillShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                )
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text(
                        text = stringResource(Res.string.setup_save_btn),
                        color = if (isFormValid) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
