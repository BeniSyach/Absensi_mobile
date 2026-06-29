package com.dss.absensiKoas.ui.screen.login

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import com.dss.absensiKoas.ui.component.*
import com.dss.absensiKoas.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onNavigateToRegistrasi: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val state by viewModel.loginState.collectAsState()
    var passwordVisible by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    LaunchedEffect(state.loginSuccess) {
        if (state.loginSuccess) { onLoginSuccess(); viewModel.resetLoginSuccess() }
    }

    // Header slide-in animation
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    val headerOffset by animateFloatAsState(
        targetValue = if (visible) 0f else -60f,
        animationSpec = tween(700, easing = CubicBezierEasing(0.215f, 0.61f, 0.355f, 1f)),
        label = "header"
    )
    val formAlpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(800, delayMillis = 300),
        label = "form_alpha"
    )
    val formOffset by animateFloatAsState(
        targetValue = if (visible) 0f else 50f,
        animationSpec = tween(700, delayMillis = 300, easing = CubicBezierEasing(0.215f, 0.61f, 0.355f, 1f)),
        label = "form_offset"
    )

    AnimatedGradientBackground {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(80.dp))

                // === Logo & Judul ===
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.graphicsLayer {
                        translationY = headerOffset
                        alpha = if (visible) 1f else 0f
                    }
                ) {
                    Box(
                        modifier = Modifier
                            .size(88.dp)
                            .background(
                                Brush.linearGradient(listOf(PrimaryLight, AccentCyan)),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Fingerprint,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        "Selamat Datang",
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Masuk ke akun absensi Anda",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }

                Spacer(modifier = Modifier.height(44.dp))

                // === Form Card ===
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            translationY = formOffset
                            alpha = formAlpha
                        },
                    cornerRadius = 24.dp,
                    alpha = 0.12f
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {

                        // Username field
                        ModernTextField(
                            value = state.username,
                            onValueChange = viewModel::onUsernameChange,
                            label = "Username / NIP",
                            leadingIcon = Icons.Outlined.Person,
                            keyboardOptions = KeyboardOptions(
                                imeAction = ImeAction.Next
                            ),
                            keyboardActions = KeyboardActions(
                                onNext = { focusManager.moveFocus(FocusDirection.Down) }
                            )
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Password field
                        ModernTextField(
                            value = state.password,
                            onValueChange = viewModel::onPasswordChange,
                            label = "Password",
                            leadingIcon = Icons.Outlined.Lock,
                            isPassword = true,
                            passwordVisible = passwordVisible,
                            onPasswordToggle = { passwordVisible = !passwordVisible },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = { focusManager.clearFocus(); viewModel.login() }
                            )
                        )

                        // Error
                        state.errorMessage?.let { error ->
                            Spacer(modifier = Modifier.height(14.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(ErrorRed.copy(alpha = 0.15f))
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.ErrorOutline,
                                    contentDescription = null,
                                    tint = ErrorRed,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    error,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = ErrorRed
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Login Button
                        GradientButton(
                            text = "Masuk",
                            onClick = { focusManager.clearFocus(); viewModel.login() },
                            enabled = !state.isLoading,
                            isLoading = state.isLoading,
                            modifier = Modifier.fillMaxWidth(),
                            icon = {
                                Icon(Icons.Default.Login, contentDescription = null,
                                    tint = Color.White, modifier = Modifier.size(20.dp))
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))


            }
        }
    }
}

@Composable
fun ModernTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    leadingIcon: androidx.compose.ui.graphics.vector.ImageVector,
    isPassword: Boolean = false,
    passwordVisible: Boolean = false,
    onPasswordToggle: (() -> Unit)? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    modifier: Modifier = Modifier
) {
    val focused = remember { mutableStateOf(false) }
    val borderColor by animateColorAsState(
        targetValue = if (focused.value) AccentCyan else Color.White.copy(alpha = 0.2f),
        label = "border"
    )

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, style = MaterialTheme.typography.bodyMedium) },
        leadingIcon = {
            Icon(
                leadingIcon,
                contentDescription = null,
                tint = if (focused.value) AccentCyan else TextSecondary,
                modifier = Modifier.size(20.dp)
            )
        },
        trailingIcon = if (isPassword) ({
            IconButton(onClick = { onPasswordToggle?.invoke() }) {
                Icon(
                    if (passwordVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                    contentDescription = null,
                    tint = TextSecondary
                )
            }
        }) else null,
        visualTransformation = if (isPassword && !passwordVisible)
            PasswordVisualTransformation() else VisualTransformation.None,
        singleLine = true,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        modifier = modifier
            .fillMaxWidth()
            .onFocusChanged { focused.value = it.isFocused },
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor       = Color.White,
            unfocusedTextColor     = Color.White,
            focusedBorderColor     = AccentCyan,
            unfocusedBorderColor   = Color.White.copy(alpha = 0.25f),
            focusedLabelColor      = AccentCyan,
            unfocusedLabelColor    = TextSecondary,
            cursorColor            = AccentCyan,
            focusedContainerColor  = Color.White.copy(alpha = 0.07f),
            unfocusedContainerColor= Color.White.copy(alpha = 0.04f)
        )
    )
}

private fun Modifier.onFocusChanged(block: (androidx.compose.ui.focus.FocusState) -> Unit) =
    this.then(Modifier.focusable())