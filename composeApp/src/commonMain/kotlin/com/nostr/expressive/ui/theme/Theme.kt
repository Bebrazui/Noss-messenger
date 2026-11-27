package com.nostr.expressive.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

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

/**
 * Material 3 Expressive 9-Side-Cookie Shape (Scalloped 9-lobe shape).
 * Standard M3 Expressive avatar geometry with 9 smooth, continuously curved lobes.
 */
val ExpressiveCookie9Shape: Shape = object : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val path = Path()
        val width = size.width
        val height = size.height
        val cx = width / 2f
        val cy = height / 2f
        val rOuter = minOf(width, height) / 2f
        val rInner = rOuter * 0.86f // M3 Expressive scallop depth
        val numLobes = 9
        val numPoints = numLobes * 2
        val dTheta = (2.0 * PI / numPoints).toFloat()

        val pts = Array(numPoints) { i ->
            val theta = i * dTheta - (PI / 2.0).toFloat()
            val r = if (i % 2 == 0) rOuter else rInner
            Triple(cx + r * cos(theta), cy + r * sin(theta), theta)
        }

        path.moveTo(pts[0].first, pts[0].second)

        val rAvg = (rOuter + rInner) / 2f
        val k = rAvg * dTheta * 0.36f

        for (i in 0 until numPoints) {
            val p1 = pts[i]
            val p2 = pts[(i + 1) % numPoints]
            val th1 = p1.third
            val th2 = p2.third

            val t1x = -sin(th1)
            val t1y = cos(th1)
            val t2x = -sin(th2)
            val t2y = cos(th2)

            val c1x = p1.first + t1x * k
            val c1y = p1.second + t1y * k
            val c2x = p2.first - t2x * k
            val c2y = p2.second - t2y * k

            path.cubicTo(c1x, c1y, c2x, c2y, p2.first, p2.second)
        }
        path.close()

        return Outline.Generic(path)
    }
}

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
