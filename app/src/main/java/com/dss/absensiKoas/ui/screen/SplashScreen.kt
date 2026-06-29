package com.dss.absensiKoas.ui.screen

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dss.absensiKoas.ui.component.AnimatedGradientBackground
import com.dss.absensiKoas.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onFinished: () -> Unit) {
    var startAnim by remember { mutableStateOf(false) }

    // Logo scale: 0 → 1 dengan spring bounce
    val logoScale by animateFloatAsState(
        targetValue = if (startAnim) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMediumLow),
        label = "logo_scale"
    )
    // Logo alpha
    val logoAlpha by animateFloatAsState(
        targetValue = if (startAnim) 1f else 0f,
        animationSpec = tween(600),
        label = "logo_alpha"
    )
    // Text slide up
    val textOffset by animateFloatAsState(
        targetValue = if (startAnim) 0f else 40f,
        animationSpec = tween(700, delayMillis = 300, easing = EaseOutCubic),
        label = "text_offset"
    )
    val textAlpha by animateFloatAsState(
        targetValue = if (startAnim) 1f else 0f,
        animationSpec = tween(600, delayMillis = 300),
        label = "text_alpha"
    )
    // Progress bar
    val progressWidth by animateFloatAsState(
        targetValue = if (startAnim) 1f else 0f,
        animationSpec = tween(1800, delayMillis = 600, easing = EaseOutQuart),
        label = "progress"
    )

    var splashVisible by remember {
        mutableStateOf(true)
    }

    val splashAlpha by animateFloatAsState(
        targetValue = if (splashVisible) 1f else 0f,
        animationSpec = tween(600),
        label = "splash_alpha"
    )

    LaunchedEffect(Unit) {
        startAnim = true

        delay(4000)

        splashVisible = false

        delay(600)

        onFinished()
    }

    // Infinite ring pulse di belakang logo
    val infiniteTransition = rememberInfiniteTransition(label = "ring")
    val ring1 by infiniteTransition.animateFloat(
        initialValue = 0.6f, targetValue = 1.4f,
        animationSpec = infiniteRepeatable(tween(1800, easing = EaseOutCubic), RepeatMode.Restart),
        label = "ring1"
    )
    val ring1Alpha by infiniteTransition.animateFloat(
        initialValue = 0.5f, targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(1800, easing = EaseOutCubic), RepeatMode.Restart),
        label = "ring1a"
    )
    val ring2 by infiniteTransition.animateFloat(
        initialValue = 0.6f, targetValue = 1.4f,
        animationSpec = infiniteRepeatable(tween(1800, delayMillis = 600, easing = EaseOutCubic), RepeatMode.Restart),
        label = "ring2"
    )
    val ring2Alpha by infiniteTransition.animateFloat(
        initialValue = 0.4f, targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(1800, delayMillis = 600, easing = EaseOutCubic), RepeatMode.Restart),
        label = "ring2a"
    )

    AnimatedGradientBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    alpha = splashAlpha
                },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Logo dengan ring pulse
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center) {
                // Ring 1
                Box(
                    modifier = Modifier
                        .size(140.dp)
                        .graphicsLayer { scaleX = ring1; scaleY = ring1; alpha = ring1Alpha }
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(AccentCyan.copy(alpha = 0.3f), Color.Transparent)
                            ),
                            shape = androidx.compose.foundation.shape.CircleShape
                        )
                )
                // Ring 2
                Box(
                    modifier = Modifier
                        .size(140.dp)
                        .graphicsLayer { scaleX = ring2; scaleY = ring2; alpha = ring2Alpha }
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(PrimaryLight.copy(alpha = 0.25f), Color.Transparent)
                            ),
                            shape = androidx.compose.foundation.shape.CircleShape
                        )
                )
                // Logo box
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .graphicsLayer { scaleX = logoScale; scaleY = logoScale; alpha = logoAlpha }
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(PrimaryLight, AccentCyan)
                            ),
                            shape = androidx.compose.foundation.shape.CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Fingerprint,
                        contentDescription = null,
                        modifier = Modifier.size(52.dp),
                        tint = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // App name
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.graphicsLayer {
                    translationY = textOffset
                    alpha = textAlpha
                }
            ) {
                Text(
                    text = "ABSENSI",
                    style = MaterialTheme.typography.headlineLarge,
                    color = Color.White,
                    letterSpacing = 6.sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = "Smart Attendance System",
                    style = MaterialTheme.typography.bodyMedium,
                    color = AccentCyan,
                    letterSpacing = 2.sp
                )
            }

            Spacer(modifier = Modifier.height(60.dp))

            // Loading bar
            Box(
                modifier = Modifier
                    .graphicsLayer { alpha = textAlpha }
                    .width(200.dp)
                    .height(3.dp)
                    .background(Color.White.copy(alpha = 0.15f), androidx.compose.foundation.shape.CircleShape)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(progressWidth)
                        .background(
                            Brush.horizontalGradient(listOf(PrimaryLight, AccentCyan)),
                            androidx.compose.foundation.shape.CircleShape
                        )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Memuat sistem...",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                modifier = Modifier.graphicsLayer { alpha = textAlpha }
            )
        }
    }
}

private val EaseOutCubic  = CubicBezierEasing(0.215f, 0.61f, 0.355f, 1f)
private val EaseOutQuart  = CubicBezierEasing(0.25f, 1f, 0.5f, 1f)