package com.dss.absensiKoas.ui.screen.home

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.dss.absensiKoas.BuildConfig
import com.dss.absensiKoas.ui.component.*
import com.dss.absensiKoas.ui.theme.*
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

@Composable
fun HomeScreen(
    onAbsenMasuk: () -> Unit,
    onAbsenPulang: () -> Unit,
    onRiwayat: () -> Unit,
    onProfil: () -> Unit,
    onLogout: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showLogoutDialog by remember { mutableStateOf(false) }
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { visible = true }
    LaunchedEffect(uiState.logoutSuccess) { if (uiState.logoutSuccess) onLogout() }

    if (showLogoutDialog) {
        LogoutConfirmDialog(
            onConfirm = { viewModel.logout() },
            onDismiss = { showLogoutDialog = false }
        )
    }

    AnimatedGradientBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
        ) {
            // ===== HEADER =====
            HomeHeader(
                profil = uiState.profil,
                visible = visible,
                onLogout = { showLogoutDialog = true },
                onProfil = onProfil,
                onRefresh = { viewModel.muatData() }
            )

            // ===== DATE & CLOCK =====
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(600, 200)) + slideInVertically(tween(600, 200)) { it / 3 }
            ) {
                DateClockCard()
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ===== STATUS HARI INI =====
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(600, 300)) + slideInVertically(tween(600, 300)) { it / 3 }
            ) {
                StatusTodayCard(uiState = uiState)
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ===== TOMBOL ABSEN =====
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(600, 400)) + slideInVertically(tween(600, 400)) { it / 3 }
            ) {
                AbsenButtonsSection(
                    sudahMasuk  = uiState.statusHariIni?.sudahAbsenMasuk == true,
                    sudahPulang = uiState.statusHariIni?.sudahAbsenPulang == true,
                    onAbsenMasuk  = onAbsenMasuk,
                    onAbsenPulang = onAbsenPulang
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ===== QUICK STATS =====
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(600, 500)) + slideInVertically(tween(600, 500)) { it / 3 }
            ) {
                QuickStatsRow(uiState = uiState, onRiwayat = onRiwayat)
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ===== SHIFT INFO =====
            uiState.profil?.shiftAktif?.let { shift ->
                AnimatedVisibility(
                    visible = visible,
                    enter = fadeIn(tween(600, 600)) + slideInVertically(tween(600, 600)) { it / 3 }
                ) {
                    ShiftInfoCard(shift = shift)
                }
                Spacer(modifier = Modifier.height(20.dp))
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun HomeHeader(
    profil: com.dss.absensiKoas.data.model.UserDetailResponse?,
    visible: Boolean,
    onLogout: () -> Unit,
    onProfil: () -> Unit,
    onRefresh: () -> Unit
) {
    val headerAlpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(600),
        label = "header_alpha"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(Color.Black.copy(alpha = 0.3f), Color.Transparent)
                )
            )
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .graphicsLayer { alpha = headerAlpha }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar foto profil
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .border(2.dp, Brush.linearGradient(listOf(PrimaryLight, AccentCyan)), CircleShape)
                    .clickable(onClick = onProfil)
            ) {
                val fotoUrl = profil?.fotoProfil?.let { "${BuildConfig.BASE_URL}api/v1/foto/$it" }
                if (fotoUrl != null) {
                    AsyncImage(
                        model = fotoUrl,
                        contentDescription = "Foto profil",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize()
                            .background(Brush.linearGradient(listOf(PrimaryLight, AccentCyan))),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = profil?.namaLengkap?.take(1)?.uppercase() ?: "A",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = getGreeting(),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                Text(
                    text = profil?.namaLengkap ?: "Memuat...",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                Text(
                    text = profil?.opd?.nama ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = AccentCyan,
                    maxLines = 1
                )
            }

            IconButton(onClick = onRefresh) {
                Icon(Icons.Default.Refresh, contentDescription = null, tint = TextSecondary)
            }
            IconButton(onClick = onLogout) {
                Icon(Icons.Default.Logout, contentDescription = null, tint = TextSecondary)
            }
        }
    }
}

@Composable
private fun DateClockCard() {
    val now = LocalDateTime.now()
    var currentTime by remember { mutableStateOf(LocalDateTime.now()) }

    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(1000)
            currentTime = LocalDateTime.now()
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = currentTime.format(DateTimeFormatter.ofPattern("HH:mm:ss")),
                style = MaterialTheme.typography.headlineLarge,
                color = Color.White,
                fontWeight = FontWeight.Black
            )
            Text(
                text = currentTime.format(
                    DateTimeFormatter.ofPattern("EEEE, dd MMMM yyyy", Locale("id", "ID"))
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        }

        // Live indicator
        Row(verticalAlignment = Alignment.CenterVertically) {
            PulsingDot(color = AccentGreen, size = 8.dp)
            Spacer(modifier = Modifier.width(6.dp))
            Text("LIVE", style = MaterialTheme.typography.labelSmall, color = AccentGreen, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun StatusTodayCard(uiState: HomeUiState) {
    val status = uiState.statusHariIni

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        cornerRadius = 20.dp
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Status Absensi Hari Ini",
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Icon(Icons.Outlined.CalendarToday, contentDescription = null,
                    tint = AccentCyan, modifier = Modifier.size(18.dp))
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                StatusTimeBlock(
                    label = "Jam Masuk",
                    icon = Icons.Default.Login,
                    iconColor = AccentGreen,
                    time = status?.waktuMasuk?.let { formatJam(it) },
                    statusText = status?.statusMasuk,
                    done = status?.sudahAbsenMasuk == true,
                    modifier = Modifier.weight(1f)
                )

                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(70.dp)
                        .background(Color.White.copy(alpha = 0.15f))
                        .align(Alignment.CenterVertically)
                )

                StatusTimeBlock(
                    label = "Jam Pulang",
                    icon = Icons.Default.Logout,
                    iconColor = AccentAmber,
                    time = status?.waktuPulang?.let { formatJam(it) },
                    statusText = status?.statusPulang,
                    done = status?.sudahAbsenPulang == true,
                    modifier = Modifier.weight(1f)
                )
            }

            status?.durasiKerjaMenit?.let {
                Spacer(modifier = Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(AccentGreen.copy(alpha = 0.15f))
                        .padding(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Timer, null, tint = AccentGreen, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Durasi kerja: ${it / 60} jam ${it % 60} menit",
                            style = MaterialTheme.typography.bodySmall,
                            color = AccentGreen,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusTimeBlock(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    time: String?,
    statusText: String?,
    done: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(horizontal = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(iconColor.copy(alpha = if (done) 0.2f else 0.08f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                if (done) Icons.Default.CheckCircle else icon,
                contentDescription = null,
                tint = if (done) iconColor else TextSecondary,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
        Text(
            text = time ?: "--:--",
            style = MaterialTheme.typography.titleMedium,
            color = if (done) Color.White else TextSecondary,
            fontWeight = FontWeight.Bold
        )
        statusText?.let { StatusBadge(it) }
    }
}

@Composable
private fun AbsenButtonsSection(
    sudahMasuk: Boolean,
    sudahPulang: Boolean,
    onAbsenMasuk: () -> Unit,
    onAbsenPulang: () -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
        Text(
            "Aksi Absensi",
            style = MaterialTheme.typography.titleSmall,
            color = TextSecondary,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            AbsenActionCard(
                label = if (sudahMasuk) "Sudah Masuk" else "Absen Masuk",
                subLabel = if (sudahMasuk) "✓ Tercatat" else "Tap untuk absen",
                icon = if (sudahMasuk) Icons.Default.CheckCircle else Icons.Default.Login,
                gradient = if (sudahMasuk)
                    listOf(Color(0xFF134E4A), Color(0xFF065F46))
                else
                    listOf(PrimaryLight, Color(0xFF1D4ED8)),
                enabled = !sudahMasuk,
                modifier = Modifier.weight(1f),
                onClick = onAbsenMasuk
            )
            AbsenActionCard(
                label = if (sudahPulang) "Sudah Pulang" else "Absen Pulang",
                subLabel = when {
                    sudahPulang  -> "✓ Tercatat"
                    !sudahMasuk  -> "Absen masuk dulu"
                    else         -> "Tap untuk absen"
                },
                icon = if (sudahPulang) Icons.Default.CheckCircle else Icons.Default.Logout,
                gradient = if (sudahPulang)
                    listOf(Color(0xFF134E4A), Color(0xFF065F46))
                else
                    listOf(Color(0xFF7C3AED), Color(0xFF6D28D9)),
                enabled = sudahMasuk && !sudahPulang,
                modifier = Modifier.weight(1f),
                onClick = onAbsenPulang
            )
        }
    }
}

@Composable
private fun AbsenActionCard(
    label: String,
    subLabel: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    gradient: List<Color>,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (enabled) 1f else 0.97f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "card_scale"
    )
    Box(
        modifier = modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .aspectRatio(0.85f)
            .clip(RoundedCornerShape(20.dp))
            .background(
                if (enabled) Brush.linearGradient(gradient)
                else Brush.linearGradient(listOf(Color(0xFF1E293B), Color(0xFF0F172A)))
            )
            .border(
                1.dp,
                if (enabled) Brush.linearGradient(gradient.map { it.copy(alpha = 0.5f) })
                else Brush.linearGradient(listOf(Color.White.copy(0.05f), Color.Transparent)),
                RoundedCornerShape(20.dp)
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(Color.White.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = Color.White, modifier = Modifier.size(24.dp))
            }
            Column {
                Text(label, style = MaterialTheme.typography.titleSmall, color = Color.White, fontWeight = FontWeight.Bold)
                Text(subLabel, style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = if (enabled) 0.75f else 0.4f))
            }
        }
    }
}

@Composable
private fun QuickStatsRow(uiState: HomeUiState, onRiwayat: () -> Unit) {
    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Ringkasan", style = MaterialTheme.typography.titleSmall, color = TextSecondary)
            TextButton(onClick = onRiwayat) {
                Text("Lihat Semua", style = MaterialTheme.typography.labelMedium, color = AccentCyan)
                Icon(Icons.Default.ChevronRight, null, tint = AccentCyan, modifier = Modifier.size(16.dp))
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatCard("OPD", uiState.profil?.opd?.kode ?: "-", Icons.Outlined.Business, AccentCyan, Modifier.weight(1f))
            StatCard("Role", uiState.profil?.role?.removePrefix("ROLE_") ?: "-", Icons.Outlined.Badge, AccentAmber, Modifier.weight(1f))
            StatCard("NIP", "•••••", Icons.Outlined.Numbers, AccentGreen, Modifier.weight(1f))
        }
    }
}

@Composable
private fun StatCard(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    GlassCard(modifier = modifier, cornerRadius = 16.dp) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.height(6.dp))
            Text(value, style = MaterialTheme.typography.labelLarge, color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1)
            Text(label, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
        }
    }
}

@Composable
private fun ShiftInfoCard(shift: com.dss.absensiKoas.data.model.ShiftResponse) {
    GlassCard(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        cornerRadius = 20.dp
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Shift Aktif", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                    Text(shift.nama, style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.Bold)
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(AccentCyan.copy(alpha = 0.15f))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Schedule, null, tint = AccentCyan, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("${shift.jamMasuk} – ${shift.jamPulang}",
                            style = MaterialTheme.typography.labelMedium, color = AccentCyan, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            shift.hariKerja?.let { hari ->
                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("Sen","Sel","Rab","Kam","Jum","Sab","Min").forEachIndexed { idx, nama ->
                        val dayName = listOf("MONDAY","TUESDAY","WEDNESDAY","THURSDAY","FRIDAY","SATURDAY","SUNDAY")[idx]
                        val active = hari.contains(dayName)
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(
                                    if (active) AccentCyan.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.05f),
                                    CircleShape
                                )
                                .border(1.dp, if (active) AccentCyan.copy(0.5f) else Color.Transparent, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(nama, style = MaterialTheme.typography.labelSmall,
                                color = if (active) AccentCyan else TextSecondary,
                                fontWeight = if (active) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LogoutConfirmDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardDark,
        icon = { Icon(Icons.Outlined.Logout, null, tint = AccentRed) },
        title = { Text("Konfirmasi Logout", color = Color.White) },
        text = { Text("Yakin ingin keluar dari aplikasi?", color = TextSecondary) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = AccentRed)
            ) { Text("Ya, Keluar") }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Batal", color = TextSecondary) }
        }
    )
}

private fun getGreeting(): String {
    val hour = LocalDateTime.now().hour
    return when {
        hour < 11 -> "Selamat Pagi 🌅"
        hour < 15 -> "Selamat Siang ☀️"
        hour < 18 -> "Selamat Sore 🌤️"
        else      -> "Selamat Malam 🌙"
    }
}

private fun formatJam(dt: String): String = try {
    LocalDateTime.parse(dt).format(DateTimeFormatter.ofPattern("HH:mm"))
} catch (e: Exception) { dt.takeLast(8).take(5) }