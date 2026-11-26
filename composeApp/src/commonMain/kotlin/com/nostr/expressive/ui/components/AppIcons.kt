package com.nostr.expressive.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawStyle
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun ArrowBackIcon(
    modifier: Modifier = Modifier,
    size: Dp = 22.dp,
    tint: Color = LocalContentColor.current
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val strokeWidth = 2.4f.dp.toPx()

        // Stem: from (0.8w, 0.5h) to (0.2w, 0.5h)
        drawLine(
            color = tint,
            start = Offset(w * 0.82f, h * 0.5f),
            end = Offset(w * 0.22f, h * 0.5f),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )

        // Chevron arms
        val path = Path().apply {
            moveTo(w * 0.50f, h * 0.22f)
            lineTo(w * 0.22f, h * 0.5f)
            lineTo(w * 0.50f, h * 0.78f)
        }
        drawPath(
            path = path,
            color = tint,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}

@Composable
fun SendAirplaneIcon(
    modifier: Modifier = Modifier,
    size: Dp = 20.dp,
    tint: Color = LocalContentColor.current
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height

        // Paper plane path
        val path = Path().apply {
            moveTo(w * 0.15f, h * 0.15f)
            lineTo(w * 0.88f, h * 0.50f)
            lineTo(w * 0.15f, h * 0.85f)
            lineTo(w * 0.35f, h * 0.50f)
            close()
        }
        drawPath(
            path = path,
            color = tint,
            style = Fill
        )
    }
}

@Composable
fun ZapBoltIcon(
    modifier: Modifier = Modifier,
    size: Dp = 16.dp,
    tint: Color = LocalContentColor.current
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height

        val path = Path().apply {
            moveTo(w * 0.55f, h * 0.08f)
            lineTo(w * 0.15f, h * 0.55f)
            lineTo(w * 0.48f, h * 0.55f)
            lineTo(w * 0.45f, h * 0.92f)
            lineTo(w * 0.85f, h * 0.45f)
            lineTo(w * 0.52f, h * 0.45f)
            close()
        }
        drawPath(
            path = path,
            color = tint,
            style = Fill
        )
    }
}

@Composable
fun PlusIcon(
    modifier: Modifier = Modifier,
    size: Dp = 20.dp,
    tint: Color = LocalContentColor.current
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val strokeWidth = 2.4f.dp.toPx()

        // Horizontal line
        drawLine(
            color = tint,
            start = Offset(w * 0.22f, h * 0.5f),
            end = Offset(w * 0.78f, h * 0.5f),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )

        // Vertical line
        drawLine(
            color = tint,
            start = Offset(w * 0.5f, h * 0.22f),
            end = Offset(w * 0.5f, h * 0.78f),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
    }
}

@Composable
fun SearchIcon(
    modifier: Modifier = Modifier,
    size: Dp = 18.dp,
    tint: Color = LocalContentColor.current
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val strokeWidth = 2.0f.dp.toPx()

        // Glass circle
        drawCircle(
            color = tint,
            radius = w * 0.32f,
            center = Offset(w * 0.42f, h * 0.42f),
            style = Stroke(width = strokeWidth)
        )

        // Handle
        drawLine(
            color = tint,
            start = Offset(w * 0.65f, h * 0.65f),
            end = Offset(w * 0.88f, h * 0.88f),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
    }
}

@Composable
fun CheckVectorIcon(
    modifier: Modifier = Modifier,
    size: Dp = 16.dp,
    tint: Color = LocalContentColor.current
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val strokeWidth = 2.0f.dp.toPx()

        val path = Path().apply {
            moveTo(w * 0.18f, h * 0.52f)
            lineTo(w * 0.42f, h * 0.76f)
            lineTo(w * 0.82f, h * 0.26f)
        }
        drawPath(
            path = path,
            color = tint,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}

@Composable
fun CrossVectorIcon(
    modifier: Modifier = Modifier,
    size: Dp = 16.dp,
    tint: Color = LocalContentColor.current
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val strokeWidth = 2.0f.dp.toPx()

        drawLine(
            color = tint,
            start = Offset(w * 0.25f, h * 0.25f),
            end = Offset(w * 0.75f, h * 0.75f),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
        drawLine(
            color = tint,
            start = Offset(w * 0.75f, h * 0.25f),
            end = Offset(w * 0.25f, h * 0.75f),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
    }
}

@Composable
fun CameraVectorIcon(
    modifier: Modifier = Modifier,
    size: Dp = 18.dp,
    tint: Color = LocalContentColor.current
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val strokeWidth = 1.8f.dp.toPx()

        // Camera body
        val bodyPath = Path().apply {
            moveTo(w * 0.15f, h * 0.35f)
            lineTo(w * 0.32f, h * 0.35f)
            lineTo(w * 0.38f, h * 0.22f)
            lineTo(w * 0.62f, h * 0.22f)
            lineTo(w * 0.68f, h * 0.35f)
            lineTo(w * 0.85f, h * 0.35f)
            lineTo(w * 0.85f, h * 0.82f)
            lineTo(w * 0.15f, h * 0.82f)
            close()
        }
        drawPath(
            path = bodyPath,
            color = tint,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // Lens circle
        drawCircle(
            color = tint,
            radius = w * 0.16f,
            center = Offset(w * 0.5f, h * 0.58f),
            style = Stroke(width = strokeWidth)
        )
    }
}

/**
 * Authentic Geometric Nostr 'N' Logo with proper layer ordering (no sliced pillars)
 */
@Composable
fun NostrLogo(
    modifier: Modifier = Modifier,
    size: Dp = 96.dp,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    cutoutColor: Color = MaterialTheme.colorScheme.background
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height

        withTransform({
            scale(w / 273f, h / 301.393f, pivot = Offset.Zero)
        }) {
            // 1. Diagonal bar FIRST (behind cutouts and pillars)
            withTransform({
                translate(17.682f, 43.787f)
                rotate(-34.799f, pivot = Offset.Zero)
            }) {
                drawRoundRect(
                    color = tint,
                    topLeft = Offset.Zero,
                    size = Size(76.722f, 305f),
                    cornerRadius = CornerRadius(10f, 10f)
                )
            }

            // 2. Cutouts SECOND (slices only the diagonal, never the pillars)
            drawRect(
                color = cutoutColor,
                topLeft = Offset(0f, 98.087f),
                size = Size(273f, 25f)
            )
            drawRect(
                color = cutoutColor,
                topLeft = Offset(0f, 178.217f),
                size = Size(273f, 25f)
            )

            // 3. Left vertical bar THIRD (on top of cutouts, solid & rounded)
            drawRoundRect(
                color = tint,
                topLeft = Offset(6f, 3.958f),
                size = Size(78.708f, 293.387f),
                cornerRadius = CornerRadius(14f, 14f)
            )

            // 4. Right vertical bar FOURTH (on top of cutouts, solid & rounded)
            drawRoundRect(
                color = tint,
                topLeft = Offset(192.932f, 3.958f),
                size = Size(77.068f, 287.516f),
                cornerRadius = CornerRadius(14f, 14f)
            )
        }
    }
}
