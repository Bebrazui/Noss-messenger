package com.nostr.expressive.ui.navigation

import androidx.compose.runtime.Composable

@Composable
actual fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit) {
    // In web browser, back button can be hooked via window.onpopstate if needed
}
