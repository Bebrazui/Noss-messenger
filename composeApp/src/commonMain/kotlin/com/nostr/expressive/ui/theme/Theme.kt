package com.nostr.expressive.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// Deep Expressive Graphite Palette (0% blue parasitic tint)
val ExpressiveSurface = Color(0xFF131314)
val ExpressiveSurfaceContainerLow = Color(0xFF1E1F20)
val ExpressiveSurfaceContainer = Color(0xFF282A2C)
val ExpressiveSurfaceContainerHigh = Color(0xFF333538)
val ExpressivePrimary = Color(0xFFA8C7FA)
val ExpressiveOnPrimary = Color(0xFF042E61)
val ExpressivePrimaryContainer = Color(0xFF0842A0)
val ExpressiveOnPrimaryContainer = Color(0xFFD3E3FD)

val ExpressiveSecondary = Color(0xFF7FCFFF)
val ExpressiveTertiary = Color(0xFF6DD58C) // Zap / Accent green

val DarkExpressiveColorScheme: ColorScheme = darkColorScheme(
    primary = ExpressivePrimary,
    onPrimary = ExpressiveOnPrimary,
    primaryContainer = ExpressivePrimaryContainer,
    onPrimaryContainer = ExpressiveOnPrimaryContainer,
    secondary = ExpressiveSecondary,
    tertiary = ExpressiveTertiary,
    background = ExpressiveSurface,
    surface = ExpressiveSurface,
    surfaceContainerLow = ExpressiveSurfaceContainerLow,
    surfaceContainer = ExpressiveSurfaceContainer,
    surfaceContainerHigh = ExpressiveSurfaceContainerHigh,
    onSurface = Color(0xFFE3E3E3),
    onSurfaceVariant = Color(0xFFC4C7C5)
)

// Expressive Shapes with asymmetric message bubbles
val ExpressiveShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

val InboundBubbleShape = RoundedCornerShape(
    topStart = 4.dp,
    topEnd = 20.dp,
    bottomEnd = 20.dp,
    bottomStart = 20.dp
)

val OutboundBubbleShape = RoundedCornerShape(
    topStart = 20.dp,
    topEnd = 4.dp,
    bottomEnd = 20.dp,
    bottomStart = 20.dp
)

val FloatingPillShape = RoundedCornerShape(50)

@Composable
fun NostrExpressiveTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkExpressiveColorScheme,
        shapes = ExpressiveShapes,
        content = content
    )
}
