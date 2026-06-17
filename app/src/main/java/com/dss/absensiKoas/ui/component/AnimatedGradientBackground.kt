package com.dss.absensiKoas.ui.component

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.dss.absensiKoas.ui.theme.*

/**
 * Background gradient animasi — seperti langit malam yang hidup.
 * Orb-orb cahaya bergerak perlahan untuk kesan depth.
 */
@Composable
fun AnimatedGradientBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "bg")

    val orb1x by infiniteTransition.animateFloat(
        initialValue = 0.1f, targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            tween(8000, easing = EaseInOutSine), RepeatMode.Reverse
        ), label = "o1x"
    )
    val orb1y by infiniteTransition.animateFloat(
        initialValue = 0.1f, targetValue = 0.5f,
        animationSpec = infiniteRepeatable(
            tween(7000, easing = EaseInOutSine), RepeatMode.Reverse
        ), label = "o1y"
    )
    val orb2x by infiniteTransition.animateFloat(
        initialValue = 0.9f, targetValue = 0.2f,
        animationSpec = infiniteRepeatable(
            tween(9000, easing = EaseInOutSine), RepeatMode.Reverse
        ), label = "o2x"
    )
    val orb2y by infiniteTransition.animateFloat(
        initialValue = 0.8f, targetValue = 0.3f,
        animationSpec = infiniteRepeatable(
            tween(6500, easing = EaseInOutSine), RepeatMode.Reverse
        ), label = "o2y"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .drawBehind {
                // Base gradient
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(GradientStart, GradientMid, GradientStart)
                    )
                )
                // Orb 1 — cyan glow
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            AccentCyan.copy(alpha = 0.25f),
                            Color.Transparent
                        ),
                        center = Offset(size.width * orb1x, size.height * orb1y),
                        radius = size.width * 0.55f
                    ),
                    radius = size.width * 0.55f,
                    center = Offset(size.width * orb1x, size.height * orb1y)
                )
                // Orb 2 — blue glow
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            PrimaryLight.copy(alpha = 0.3f),
                            Color.Transparent
                        ),
                        center = Offset(size.width * orb2x, size.height * orb2y),
                        radius = size.width * 0.5f
                    ),
                    radius = size.width * 0.5f,
                    center = Offset(size.width * orb2x, size.height * orb2y)
                )
            }
    ) {
        content()
    }
}

/**
 * Card dengan efek frosted glass (kaca buram).
 * Tampilan layered dan modern.
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 20.dp,
    alpha: Float = 0.15f,
    borderAlpha: Float = 0.3f,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(Color.White.copy(alpha = alpha))
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = borderAlpha),
                        Color.White.copy(alpha = 0.05f)
                    )
                ),
                shape = RoundedCornerShape(cornerRadius)
            )
    ) {
        Column(modifier = Modifier.fillMaxWidth(), content = content)
    }
}

/**
 * Gradient button yang lebih mewah dari Button biasa.
 */
@Composable
fun GradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    gradientColors: List<Color> = listOf(PrimaryLight, AccentCyan),
    icon: @Composable (() -> Unit)? = null
) {
    val scale by animateFloatAsState(
        targetValue = if (enabled) 1f else 0.97f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "btn_scale"
    )

    Box(
        modifier = modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(16.dp))
            .background(
                brush = if (enabled)
                    Brush.horizontalGradient(gradientColors)
                else
                    Brush.horizontalGradient(listOf(Color(0xFF334155), Color(0xFF1E293B)))
            )
            .clickable(enabled = enabled && !isLoading, onClick = onClick)
            .padding(vertical = 16.dp, horizontal = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                color = Color.White,
                strokeWidth = 2.5.dp
            )
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                icon?.invoke()
                if (icon != null) Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = text,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (enabled) Color.White else Color(0xFF64748B)
                )
            }
        }
    }
}

/**
 * Pulse animation untuk tombol atau icon penting.
 */
@Composable
fun PulsingDot(
    color: Color = AccentGreen,
    size: Dp = 10.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.8f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "pulse_scale"
    )
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.8f, targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "pulse_alpha"
    )
    Box(contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(size * 2.5f)
                .graphicsLayer { scaleX = scale; scaleY = scale; this.alpha = alpha }
                .background(color.copy(alpha = 0.3f), shape = androidx.compose.foundation.shape.CircleShape)
        )
        Box(
            modifier = Modifier
                .size(size)
                .background(color, shape = androidx.compose.foundation.shape.CircleShape)
        )
    }
}

/**
 * Shimmer loading effect.
 */
@Composable
fun ShimmerBox(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 12.dp
) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val shimmerX by transition.animateFloat(
        initialValue = -1f, targetValue = 2f,
        animationSpec = infiniteRepeatable(tween(1400, easing = LinearEasing)),
        label = "shimmer_x"
    )
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        Color(0xFF1E293B),
                        Color(0xFF334155),
                        Color(0xFF1E293B)
                    ),
                    startX = shimmerX * 500f,
                    endX   = shimmerX * 500f + 400f
                )
            )
    )
}

/**
 * Status badge — chip berwarna sesuai status absen.
 */
@Composable
fun StatusBadge(status: String) {
    val (color, textColor) = when (status.uppercase()) {
        "HADIR"       -> Pair(AccentGreen.copy(alpha = 0.2f), AccentGreen)
        "TERLAMBAT"   -> Pair(AccentAmber.copy(alpha = 0.2f), AccentAmber)
        "PULANG_AWAL" -> Pair(AccentAmber.copy(alpha = 0.2f), AccentAmber)
        "ALPA"        -> Pair(AccentRed.copy(alpha = 0.2f), AccentRed)
        "IZIN","SAKIT"-> Pair(InfoBlue.copy(alpha = 0.2f), InfoBlue)
        else          -> Pair(Color(0x22FFFFFF), TextSecondary)
    }
    val label = when (status.uppercase()) {
        "HADIR"       -> "✓ Hadir"
        "TERLAMBAT"   -> "⚠ Terlambat"
        "PULANG_AWAL" -> "⚠ Pulang Awal"
        "ALPA"        -> "✗ Alpa"
        "IZIN"        -> "📋 Izin"
        "SAKIT"       -> "🏥 Sakit"
        else          -> status
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(color)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = textColor)
    }
}

private val EaseInOutSine = CubicBezierEasing(0.37f, 0f, 0.63f, 1f)
