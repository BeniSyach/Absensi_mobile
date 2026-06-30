package com.dss.absensiKoas.ui.screen.absen

import android.Manifest
import android.util.Log
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.dss.absensiKoas.ui.component.*
import com.dss.absensiKoas.ui.theme.*
import com.google.accompanist.permissions.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.*
import com.google.maps.android.compose.*
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dss.absensiKoas.data.model.ShiftResponse

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun AbsenScreen(
    jenisAbsen: JenisAbsen,
    onAbsenSukses: () -> Unit,
    viewModel: AbsenViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showCamera by remember { mutableStateOf(false) }
    var visible by remember { mutableStateOf(false) }

    val permissionsState = rememberMultiplePermissionsState(
        listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.CAMERA)
    )

    // Saat pertama buka: minta izin, set visible untuk animasi
    LaunchedEffect(Unit) {
        visible = true
        if (!permissionsState.allPermissionsGranted) {
            permissionsState.launchMultiplePermissionRequest()
        }
    }

    LaunchedEffect(Unit) {
        Log.d(
            "SHIFT",
            "MainScreen = ${state.shiftDipilih}"
        )
    }

    // Layar kamera
    if (showCamera) {
        CameraCaptureScreen(
            onPhotoTaken = { file ->
                viewModel.onFotoDidambil(file)
                showCamera = false
            },
            onDismiss = { showCamera = false }
        )
        return
    }

    // Layar izin belum diberikan
    if (!permissionsState.allPermissionsGranted) {
        PermissionScreen(onRequest = { permissionsState.launchMultiplePermissionRequest() })
        return
    }

    // Routing berdasarkan step ViewModel
    when (state.step) {
        AbsenStep.SUKSES -> {
            state.absenResult?.let { result ->
                AbsenSuccessOverlay(
                    result  = result,
                    jenisAbsen = jenisAbsen,
                    onDismiss = {
                        viewModel.resetAbsenResult()
                        onAbsenSukses()
                    }
                )
            }
        }

        // Step pilih shift — HANYA untuk absen masuk
        // Absen pulang langsung ke step lokasi (shift diambil dari absen masuk)
        AbsenStep.PILIH_SHIFT -> {
            if (jenisAbsen == JenisAbsen.MASUK) {
                ShiftPickerScreen(
                    state   = state,
                    visible = visible,
                    onPilih = { shift -> viewModel.pilihShift(shift) },
                    onLanjut = { viewModel.lanjutDariPilihShift(jenisAbsen) },
                    onRetry = { viewModel.muatDaftarShift() }
                )
            } else {
                Log.d(
                    "SHIFT",
                    "MainScreen = ${state.shiftDipilih}"
                )
                // Pulang: langsung ke lokasi
                LaunchedEffect(Unit) { viewModel.mulaiAbsenPulang() }
                LoadingFullScreen("Menyiapkan absen pulang...")
            }
        }

        // Step lokasi, foto, konfirmasi, submit — screen utama
        else -> {

            AbsenMainScreen(
                jenisAbsen  = jenisAbsen,
                state       = state,
                visible     = visible,
                onRefreshLokasi = { viewModel.ambilLokasi() },
                onLanjutFoto    = { viewModel.lanjutKeAmbilFoto() },
                onAmbilFoto     = { showCamera = true },
                onUlangiAmbilFoto = { viewModel.ulangiAmbilFoto() },
                onKembali        = { viewModel.kembaliKeLokasi() },
                onCatatanChange  = { viewModel.setCatatan(it) },
                onSubmitMasuk    = { viewModel.submitAbsenMasuk() },
                onSubmitPulang   = { viewModel.submitAbsenPulang() }
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// STEP 1 — SHIFT PICKER SCREEN
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ShiftPickerScreen(
    state: AbsenUiState,
    visible: Boolean,
    onPilih: (ShiftResponse) -> Unit,
    onLanjut: () -> Unit,
    onRetry: () -> Unit
) {
    AnimatedGradientBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
        ) {
            // Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(Color.Black.copy(0.3f), Color.Transparent)))
                    .statusBarsPadding()
                    .padding(20.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .background(Brush.linearGradient(listOf(PrimaryLight, AccentCyan)), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Login, null, tint = Color.White, modifier = Modifier.size(22.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            "Pilih Shift Hari Ini",
                            style = MaterialTheme.typography.headlineSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            LocalDateTime.now().format(DateTimeFormatter.ofPattern("EEEE, dd MMM yyyy")),
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(500)) + slideInVertically(tween(500)) { it / 3 }
            ) {
                GlassCard(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    cornerRadius = 16.dp
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Info, null, tint = AccentCyan, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            "Pilih shift yang sesuai jadwal jaga Anda hari ini. " +
                                    "Shift malam yang melewati tengah malam ditandai 🌙.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Daftar shift
            when {
                state.isLoadingShift -> {
                    repeat(3) {
                        ShimmerBox(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(90.dp)
                                .padding(horizontal = 20.dp, vertical = 6.dp)
                        )
                    }
                }

                state.daftarShift.isEmpty() -> {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Outlined.Schedule, null, tint = TextSecondary,
                            modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Tidak ada shift tersedia", color = TextSecondary,
                            style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.height(16.dp))
                        GradientButton("Coba Lagi", onClick = onRetry,
                            modifier = Modifier.fillMaxWidth(0.6f))
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(state.daftarShift) { shift ->
                            ShiftItemCard(
                                shift    = shift,
                                dipilih  = state.shiftDipilih?.id == shift.id,
                                onClick  = { onPilih(shift) }
                            )
                        }
                    }
                }
            }

            // Error message
            state.errorMessage?.let { error ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(ErrorRed.copy(0.15f))
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.ErrorOutline, null, tint = ErrorRed,
                        modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(error, style = MaterialTheme.typography.bodySmall, color = ErrorRed)
                }
            }

            // Tombol lanjut
            Spacer(modifier = Modifier.height(12.dp))
            GradientButton(
                text = "Lanjut Absen Masuk",
                onClick = onLanjut,
                enabled = state.shiftDipilih != null,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .height(56.dp),
                icon = {
                    Icon(Icons.Default.ArrowForward, null, tint = Color.White,
                        modifier = Modifier.size(20.dp))
                }
            )
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// SHIFT ITEM CARD — tampilan satu shift di daftar pilihan
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ShiftItemCard(
    shift: ShiftResponse,
    dipilih: Boolean,
    onClick: () -> Unit
) {
    val borderColor by animateColorAsState(
        targetValue = if (dipilih) AccentCyan else Color.White.copy(0.1f),
        animationSpec = tween(200),
        label = "border"
    )
    val bgColor by animateColorAsState(
        targetValue = if (dipilih) AccentCyan.copy(0.12f) else Color.White.copy(0.05f),
        animationSpec = tween(200),
        label = "bg"
    )
    val scale by animateFloatAsState(
        targetValue = if (dipilih) 1.02f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "scale"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(16.dp))
            .background(bgColor)
            .border(
                width = if (dipilih) 1.5.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {

            // Emoji & radio indicator
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        if (dipilih) AccentCyan.copy(0.2f) else Color.White.copy(0.08f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    shift.emoji(),
                    style = MaterialTheme.typography.headlineSmall
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        shift.nama,
                        style = MaterialTheme.typography.titleMedium,
                        color = if (dipilih) AccentCyan else Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    // Badge lintas hari
                    if (shift.lintasHari == true) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(AccentAmber.copy(0.2f))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                "🌙 Lintas Hari",
                                style = MaterialTheme.typography.labelSmall,
                                color = AccentAmber,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Jam masuk → jam pulang
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Schedule, null, tint = TextSecondary,
                        modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        "${shift.jamMasuk}  →  ${shift.jamPulang}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Toleransi
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Toleransi terlambat: ${shift.toleransiTerlambat ?: 15} menit",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary.copy(alpha = 0.7f)
                )
            }

            // Checkmark jika dipilih
            AnimatedVisibility(
                visible = dipilih,
                enter = fadeIn() + scaleIn(),
                exit  = fadeOut() + scaleOut()
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(AccentCyan, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Check, null, tint = Color.White,
                        modifier = Modifier.size(16.dp))
                }
            }

            if (!dipilih) Spacer(modifier = Modifier.width(28.dp))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// STEP 2-4 — SCREEN UTAMA (Lokasi, Foto, Konfirmasi)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun AbsenMainScreen(
    jenisAbsen: JenisAbsen,
    state: AbsenUiState,
    visible: Boolean,
    onRefreshLokasi: () -> Unit,
    onLanjutFoto: () -> Unit,
    onAmbilFoto: () -> Unit,
    onUlangiAmbilFoto: () -> Unit,
    onKembali: () -> Unit,
    onCatatanChange: (String) -> Unit,
    onSubmitMasuk: () -> Unit,
    onSubmitPulang: () -> Unit
) {
    AnimatedGradientBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
        ) {
            // ── Header ──
            AbsenHeader(jenisAbsen = jenisAbsen, visible = visible, state = state)

            // ── Shift yang dipilih (hanya masuk) ──
            if (jenisAbsen == JenisAbsen.MASUK && state.shiftDipilih != null) {
                AnimatedVisibility(visible = visible,
                    enter = fadeIn(tween(400, 150)) + slideInVertically(tween(400, 150)) { it / 3 }) {
                    ShiftDipilihBadge(shift = state.shiftDipilih)
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // ── Maps & Lokasi ──
            AnimatedVisibility(visible = visible,
                enter = fadeIn(tween(600, 200)) + slideInVertically(tween(600, 200)) { it / 3 }) {
                MapsLocationCard(
                    uiState   = state,
                    onRefresh = onRefreshLokasi
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ── Tombol Lanjut ke Foto (hanya di step AMBIL_LOKASI) ──
            if (state.step == AbsenStep.AMBIL_LOKASI) {
                AnimatedVisibility(visible = visible,
                    enter = fadeIn(tween(400, 350))) {
                    GradientButton(
                        text    = "Lanjut Ambil Foto Selfie",
                        onClick = onLanjutFoto,
                        enabled = state.lokasiSaatIni != null
                                && !state.mockLocationTerdeteksi
                                && state.dalamRadius != false,
                        gradientColors = if (state.dalamRadius == false || state.mockLocationTerdeteksi)
                            listOf(Color(0xFF374151), Color(0xFF1F2937))
                        else
                            listOf(PrimaryLight, AccentCyan),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                            .height(52.dp),
                        icon = {
                            Icon(Icons.Default.CameraAlt, null, tint = Color.White,
                                modifier = Modifier.size(20.dp))
                        }
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // ── Radius Status Bar ──
            if (state.lokasiSaatIni != null) {
                AnimatedVisibility(visible = visible, enter = fadeIn(tween(400, 400))) {
                    RadiusStatusBar(uiState = state)
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // ── Foto Card (tampil di step AMBIL_FOTO / KONFIRMASI / SUBMIT) ──
            if (state.step in listOf(
                    AbsenStep.AMBIL_FOTO,
                    AbsenStep.KONFIRMASI,
                    AbsenStep.SUBMIT
                )) {
                AnimatedVisibility(visible = visible,
                    enter = fadeIn(tween(400, 200)) + slideInVertically(tween(400, 200)) { it / 3 }) {
                    FotoCaptureCard(
                        fotoFile    = state.fotoFile,
                        onAmbilFoto = onAmbilFoto,
                        onUlangi    = onUlangiAmbilFoto
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // ── Catatan (hanya di konfirmasi) ──
            if (state.step == AbsenStep.KONFIRMASI) {
                AnimatedVisibility(visible = visible,
                    enter = fadeIn(tween(400, 250)) + slideInVertically(tween(400, 250)) { it / 3 }) {
                    GlassCard(
                        modifier      = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                        cornerRadius  = 18.dp
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Catatan (opsional)",
                                style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value         = state.catatan,
                                onValueChange = onCatatanChange,
                                placeholder   = { Text("Tambahkan catatan...", color = TextSecondary) },
                                modifier      = Modifier.fillMaxWidth(),
                                minLines = 2, maxLines = 3,
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor      = Color.White,
                                    unfocusedTextColor    = Color.White,
                                    focusedBorderColor    = AccentCyan,
                                    unfocusedBorderColor  = Color.White.copy(0.2f),
                                    focusedContainerColor = Color.White.copy(0.07f),
                                    unfocusedContainerColor = Color.White.copy(0.04f),
                                    cursorColor           = AccentCyan
                                )
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // ── Error ──
            state.errorMessage?.let { error ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(ErrorRed.copy(0.15f))
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.ErrorOutline, null, tint = ErrorRed,
                        modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(error, style = MaterialTheme.typography.bodySmall, color = ErrorRed)
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // ── Tombol Submit (hanya di KONFIRMASI atau SUBMIT) ──
            if (state.step in listOf(AbsenStep.KONFIRMASI, AbsenStep.SUBMIT)) {
                val gradientColors = if (jenisAbsen == JenisAbsen.MASUK)
                    listOf(PrimaryLight, AccentCyan)
                else
                    listOf(Color(0xFF7C3AED), Color(0xFFDB2777))

                AnimatedVisibility(visible = visible,
                    enter = fadeIn(tween(400, 300)) + slideInVertically(tween(400, 300)) { it }) {
                    Column {
                        GradientButton(
                            text      = if (jenisAbsen == JenisAbsen.MASUK)
                                "Konfirmasi Absen Masuk" else "Konfirmasi Absen Pulang",
                            onClick   = if (jenisAbsen == JenisAbsen.MASUK) onSubmitMasuk else onSubmitPulang,
                            enabled   = !state.isSubmitting,
                            isLoading = state.isSubmitting,
                            gradientColors = gradientColors,
                            modifier  = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp)
                                .height(56.dp),
                            icon = {
                                Icon(
                                    if (jenisAbsen == JenisAbsen.MASUK)
                                        Icons.Default.Login else Icons.Default.Logout,
                                    null, tint = Color.White, modifier = Modifier.size(22.dp)
                                )
                            }
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Tombol kembali ke lokasi
                        TextButton(
                            onClick  = onKembali,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.ArrowBack, null,
                                tint = TextSecondary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Ambil Ulang Lokasi",
                                style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        }
                    }
                }
            }

            // Hint ketika foto belum diambil di step AMBIL_FOTO
            if (state.step == AbsenStep.AMBIL_FOTO && state.fotoFile == null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Tap area foto di atas untuk membuka kamera",
                    style     = MaterialTheme.typography.bodySmall,
                    color     = TextSecondary,
                    textAlign = TextAlign.Center,
                    modifier  = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// SHIFT DIPILIH BADGE — tampil setelah shift dipilih
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ShiftDipilihBadge(shift: ShiftResponse) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(AccentCyan.copy(0.12f))
            .border(1.dp, AccentCyan.copy(0.4f), RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(shift.emoji(), style = MaterialTheme.typography.titleSmall)
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "${shift.nama} · ${shift.jamMasuk} – ${shift.jamPulang}",
                style     = MaterialTheme.typography.bodyMedium,
                color     = AccentCyan,
                fontWeight = FontWeight.Bold
            )
            if (shift.lintasHari == true) {
                Text("Shift lintas hari — absen pulang bisa esok hari",
                    style = MaterialTheme.typography.bodySmall,
                    color = AccentAmber)
            }
        }
        Icon(Icons.Default.CheckCircle, null, tint = AccentCyan,
            modifier = Modifier.size(20.dp))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// HEADER
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun AbsenHeader(
    jenisAbsen: JenisAbsen,
    visible: Boolean,
    state: AbsenUiState
) {
    val alpha by animateFloatAsState(
        targetValue    = if (visible) 1f else 0f,
        animationSpec  = tween(600),
        label          = "header_alpha"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(Color.Black.copy(0.3f), Color.Transparent)))
            .statusBarsPadding()
            .padding(20.dp)
            .graphicsLayer { this.alpha = alpha }
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(
                        Brush.linearGradient(
                            if (jenisAbsen == JenisAbsen.MASUK)
                                listOf(PrimaryLight, AccentCyan)
                            else
                                listOf(Color(0xFF7C3AED), Color(0xFFDB2777))
                        ),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (jenisAbsen == JenisAbsen.MASUK) Icons.Default.Login else Icons.Default.Logout,
                    null, tint = Color.White, modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(
                    if (jenisAbsen == JenisAbsen.MASUK) "Absen Masuk" else "Absen Pulang",
                    style      = MaterialTheme.typography.headlineSmall,
                    color      = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    LocalDateTime.now()
                        .format(DateTimeFormatter.ofPattern("HH:mm — dd MMM yyyy")),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }

            // Indikator step di kanan
            Spacer(modifier = Modifier.weight(1f))
            StepIndicator(step = state.step, jenisAbsen = jenisAbsen)
        }
    }
}

@Composable
private fun StepIndicator(step: AbsenStep, jenisAbsen: JenisAbsen) {
    val steps = if (jenisAbsen == JenisAbsen.MASUK)
        listOf(AbsenStep.PILIH_SHIFT, AbsenStep.AMBIL_LOKASI, AbsenStep.AMBIL_FOTO, AbsenStep.KONFIRMASI)
    else
        listOf(AbsenStep.AMBIL_LOKASI, AbsenStep.AMBIL_FOTO, AbsenStep.KONFIRMASI)

    val currentIndex = steps.indexOf(step).coerceAtLeast(0)

    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        steps.forEachIndexed { index, _ ->
            val done   = index < currentIndex
            val active = index == currentIndex
            Box(
                modifier = Modifier
                    .width(if (active) 20.dp else 8.dp)
                    .height(8.dp)
                    .clip(RoundedCornerShape(50))
                    .background(
                        when {
                            done   -> AccentGreen.copy(0.8f)
                            active -> AccentCyan
                            else   -> Color.White.copy(0.2f)
                        }
                    )
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// MAPS LOCATION CARD
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun MapsLocationCard(
    uiState: AbsenUiState,
    onRefresh: () -> Unit
) {
    val lokasi = uiState.lokasiSaatIni

    GlassCard(
        modifier     = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        cornerRadius = 20.dp
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(
                                if (uiState.mockLocationTerdeteksi) ErrorRed.copy(0.2f)
                                else AccentCyan.copy(0.2f),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            if (uiState.mockLocationTerdeteksi)
                                Icons.Default.LocationOff else Icons.Default.MyLocation,
                            null,
                            tint = if (uiState.mockLocationTerdeteksi) ErrorRed else AccentCyan,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text("Lokasi GPS",
                            style = MaterialTheme.typography.titleSmall,
                            color = Color.White, fontWeight = FontWeight.Bold)
                        Text(
                            when {
                                uiState.isLoadingLokasi        -> "Mencari sinyal GPS..."
                                uiState.mockLocationTerdeteksi -> "⚠ Mock Location terdeteksi!"
                                lokasi != null -> "Akurasi: ${lokasi.accuracy.toInt()}m"
                                else           -> "Lokasi belum tersedia"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = when {
                                uiState.mockLocationTerdeteksi -> ErrorRed
                                lokasi != null -> AccentGreen
                                else           -> TextSecondary
                            }
                        )
                    }
                }
                IconButton(onClick = onRefresh, modifier = Modifier.size(36.dp)) {
                    if (uiState.isLoadingLokasi) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp),
                            color = AccentCyan, strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.Refresh, null, tint = TextSecondary)
                    }
                }
            }

            // Google Maps
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp))
            ) {
                if (lokasi != null) {
                    val userPos     = LatLng(lokasi.latitude, lokasi.longitude)
                    val cameraState = rememberCameraPositionState {
                        position = CameraPosition.fromLatLngZoom(userPos, 17f)
                    }

                    LaunchedEffect(lokasi) {
                        cameraState.animate(CameraUpdateFactory.newLatLngZoom(userPos, 17f), 1000)
                    }

                    GoogleMap(
                        modifier = Modifier.fillMaxSize(),
                        cameraPositionState = cameraState,
                        properties = MapProperties(isMyLocationEnabled = false),
                        uiSettings = MapUiSettings(
                            zoomControlsEnabled = false,
                            compassEnabled      = false,
                            scrollGesturesEnabled = true,
                            zoomGesturesEnabled   = true
                        )
                    ) {
                        Marker(
                            state   = MarkerState(position = userPos),
                            title   = "Posisi Anda",
                            snippet = "${String.format("%.6f", lokasi.latitude)}, ${String.format("%.6f", lokasi.longitude)}",
                            icon    = BitmapDescriptorFactory.defaultMarker(
                                if (uiState.mockLocationTerdeteksi)
                                    BitmapDescriptorFactory.HUE_RED
                                else
                                    BitmapDescriptorFactory.HUE_AZURE
                            )
                        )
                        Circle(
                            center      = userPos,
                            radius      = lokasi.accuracy.toDouble(),
                            fillColor   = AccentCyan.copy(alpha = 0.1f),
                            strokeColor = AccentCyan.copy(alpha = 0.5f),
                            strokeWidth = 2f
                        )
                    }

                    // Koordinat overlay
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(10.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Black.copy(0.65f))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            "${String.format("%.5f", lokasi.latitude)}, ${String.format("%.5f", lokasi.longitude)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White
                        )
                    }

                    // Badge fake GPS
                    if (uiState.mockLocationTerdeteksi) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(10.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(ErrorRed.copy(0.85f))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Warning, null, tint = Color.White,
                                    modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("FAKE GPS TERDETEKSI",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize().background(CardDark),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            if (uiState.isLoadingLokasi) {
                                CircularProgressIndicator(color = AccentCyan)
                                Spacer(modifier = Modifier.height(12.dp))
                                Text("Mencari lokasi GPS...", color = TextSecondary,
                                    style = MaterialTheme.typography.bodySmall)
                            } else {
                                Icon(Icons.Outlined.LocationOff, null, tint = TextSecondary,
                                    modifier = Modifier.size(40.dp))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Tekan refresh untuk mengambil lokasi",
                                    color = TextSecondary,
                                    style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// FOTO CAPTURE CARD
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun FotoCaptureCard(
    fotoFile: File?,
    onAmbilFoto: () -> Unit,
    onUlangi: () -> Unit = onAmbilFoto
) {
    GlassCard(
        modifier     = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        cornerRadius = 20.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.CameraAlt, null, tint = AccentCyan,
                    modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Foto Selfie",
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White, fontWeight = FontWeight.Bold)
                if (fotoFile != null) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(AccentGreen.copy(0.2f))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text("✓ Siap", style = MaterialTheme.typography.labelSmall,
                            color = AccentGreen)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(CardDark)
                    .clickable(onClick = onAmbilFoto),
                contentAlignment = Alignment.Center
            ) {
                if (fotoFile != null) {
                    AsyncImage(
                        model            = fotoFile,
                        contentDescription = "Foto selfie",
                        modifier         = Modifier.fillMaxSize(),
                        contentScale     = ContentScale.Crop
                    )
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(12.dp)
                            .clip(RoundedCornerShape(50))
                            .background(Color.Black.copy(0.65f))
                            .clickable(onClick = onUlangi)
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CameraAlt, null, tint = Color.White,
                                modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Ulangi",
                                style = MaterialTheme.typography.labelSmall, color = Color.White)
                        }
                    }
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(60.dp)
                                .background(Brush.linearGradient(listOf(PrimaryLight, AccentCyan)),
                                    CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.CameraAlt, null, tint = Color.White,
                                modifier = Modifier.size(28.dp))
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Tap untuk ambil foto selfie",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White, fontWeight = FontWeight.Medium)
                        Text("Gunakan kamera depan — wajah harus terlihat jelas",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary, textAlign = TextAlign.Center)
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// RADIUS STATUS BAR
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun RadiusStatusBar(uiState: AbsenUiState) {
    val jarak       = uiState.jarakKeKantor
    val radius      = uiState.radiusKantor.toFloat()
    val dalamRadius = uiState.dalamRadius
    val isMock      = uiState.mockLocationTerdeteksi

    val (bgColor, borderColor, iconColor, statusColor) = when {
        isMock               -> listOf(ErrorRed.copy(0.12f), ErrorRed.copy(0.5f), ErrorRed, ErrorRed)
        dalamRadius == false -> listOf(AccentAmber.copy(0.12f), AccentAmber.copy(0.5f), AccentAmber, AccentAmber)
        dalamRadius == true  -> listOf(AccentGreen.copy(0.12f), AccentGreen.copy(0.5f), AccentGreen, AccentGreen)
        else                 -> listOf(Color.White.copy(0.05f), Color.White.copy(0.15f), TextSecondary, TextSecondary)
    }

    val progressFraction = if (jarak != null && radius > 0) (jarak / radius).coerceIn(0f, 1f) else 0f
    val animatedProgress by animateFloatAsState(
        targetValue   = progressFraction,
        animationSpec = tween(800, easing = CubicBezierEasing(0.25f, 1f, 0.5f, 1f)),
        label         = "radius_progress"
    )

    GlassCard(
        modifier     = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        cornerRadius = 18.dp, alpha = 0.1f
    ) {
        Column(
            modifier = Modifier
                .border(1.dp, borderColor, RoundedCornerShape(18.dp))
                .background(bgColor, RoundedCornerShape(18.dp))
                .padding(16.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(iconColor.copy(0.2f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        when {
                            isMock               -> Icons.Default.GpsOff
                            dalamRadius == false -> Icons.Default.LocationOff
                            dalamRadius == true  -> Icons.Default.LocationOn
                            else                 -> Icons.Default.MyLocation
                        },
                        null, tint = iconColor, modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = when {
                            isMock               -> "Fake GPS Terdeteksi"
                            dalamRadius == false -> "Di Luar Radius Kantor"
                            dalamRadius == true  -> "Dalam Radius Kantor"
                            else                 -> "Memeriksa Radius..."
                        },
                        style = MaterialTheme.typography.titleSmall,
                        color = statusColor, fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = when {
                            isMock    -> "Nonaktifkan fake GPS dan refresh lokasi"
                            jarak != null -> {
                                val j = if (jarak >= 1000) String.format("%.1f km", jarak/1000) else "${jarak.toInt()} m"
                                val r = if (radius >= 1000) String.format("%.1f km", radius/1000) else "${radius.toInt()} m"
                                "$j dari kantor • radius $r"
                            }
                            else -> uiState.namaOpd ?: "Memuat data kantor..."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = statusColor.copy(alpha = 0.8f)
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(statusColor.copy(0.2f))
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = when {
                            isMock || dalamRadius == false -> "DITOLAK"
                            dalamRadius == true            -> "OK"
                            else                           -> "..."
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = statusColor, fontWeight = FontWeight.Black
                    )
                }
            }

            if (jarak != null && !isMock) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Posisi Anda", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    Text(
                        if (dalamRadius == true) "✓ Dalam radius" else "Batas radius",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (dalamRadius == true) AccentGreen else AccentAmber
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth().height(8.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Color.White.copy(0.1f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(animatedProgress)
                            .background(
                                Brush.horizontalGradient(
                                    if (dalamRadius == true)
                                        listOf(AccentGreen, AccentGreen.copy(0.7f))
                                    else
                                        listOf(AccentGreen, AccentAmber, ErrorRed)
                                ), RoundedCornerShape(50)
                            )
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("0 m", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    Text(
                        if (radius >= 1000) String.format("%.0f km", radius/1000) else "${radius.toInt()} m",
                        style = MaterialTheme.typography.labelSmall, color = TextSecondary
                    )
                }
            }

            uiState.pesanLokasi?.let { pesan ->
                if (dalamRadius == false || isMock) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(statusColor.copy(0.1f))
                            .padding(10.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(Icons.Default.Info, null, tint = statusColor,
                            modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(pesan, style = MaterialTheme.typography.bodySmall, color = statusColor)
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// SUKSES OVERLAY
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun AbsenSuccessOverlay(
    result: com.dss.absensiKoas.data.model.AbsenResponse,
    jenisAbsen: JenisAbsen,
    onDismiss: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue   = 1f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMediumLow),
        label         = "success_scale"
    )
    val isMock  = result.mockLocationDetected == true
    val isValid = result.lokasiValid == true

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.8f)),
        contentAlignment = Alignment.Center
    ) {
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp)
                .graphicsLayer { scaleX = scale; scaleY = scale },
            cornerRadius = 28.dp, alpha = 0.2f
        ) {
            Column(
                modifier = Modifier.padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(90.dp)
                        .background(
                            Brush.radialGradient(
                                if (isMock) listOf(ErrorRed.copy(0.3f), Color.Transparent)
                                else        listOf(AccentGreen.copy(0.3f), Color.Transparent)
                            ), CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .background(
                                if (isMock) ErrorRed.copy(0.2f) else AccentGreen.copy(0.2f),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            if (isMock) Icons.Default.Warning else Icons.Default.CheckCircle,
                            null,
                            tint = if (isMock) ErrorRed else AccentGreen,
                            modifier = Modifier.size(38.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    if (jenisAbsen == JenisAbsen.MASUK) "Absen Masuk Berhasil!" else "Absen Pulang Berhasil!",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White, fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                // Info shift
                result.shiftNama?.let { nama ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(AccentCyan.copy(0.15f))
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            "Shift: $nama${if (result.shiftLintasHari == true) " 🌙" else ""}",
                            style = MaterialTheme.typography.labelMedium,
                            color = AccentCyan, fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                listOfNotNull(
                    "Status" to result.status,
                    result.jarakDariKantor?.let { "Jarak dari kantor" to "${it.toInt()} meter" },
                    result.durasiKerjaMenit?.let { "Durasi kerja" to "${it/60} jam ${it%60} menit" },
                    "Lokasi valid" to if (isValid) "✓ Ya" else "✗ Di luar radius"
                ).forEach { (label, value) ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(label, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        Text(value, style = MaterialTheme.typography.bodySmall,
                            color = Color.White, fontWeight = FontWeight.SemiBold)
                    }
                }

                result.pesan?.let {
                    Spacer(modifier = Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background((if (isMock) ErrorRed else AccentGreen).copy(0.15f))
                            .padding(12.dp)
                    ) {
                        Text(it, style = MaterialTheme.typography.bodySmall,
                            color = if (isMock) ErrorRed else AccentGreen,
                            textAlign = TextAlign.Center)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                GradientButton(
                    text    = "Selesai",
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    gradientColors = if (isMock)
                        listOf(ErrorRed, Color(0xFFB91C1C))
                    else
                        listOf(AccentGreen, Color(0xFF059669))
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// HELPER COMPOSABLES
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun LoadingFullScreen(message: String) {
    AnimatedGradientBackground {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(color = AccentCyan)
                Spacer(modifier = Modifier.height(16.dp))
                Text(message, color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun PermissionScreen(onRequest: () -> Unit) {
    AnimatedGradientBackground {
        Column(
            modifier = Modifier.fillMaxSize().padding(40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier.size(90.dp).background(ErrorRed.copy(0.2f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.LocationOff, null, tint = ErrorRed, modifier = Modifier.size(48.dp))
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text("Izin Diperlukan",
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                "Aplikasi membutuhkan akses Lokasi GPS dan Kamera untuk proses absensi yang valid.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary, textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(32.dp))
            GradientButton("Berikan Izin", onClick = onRequest, modifier = Modifier.fillMaxWidth())
        }
    }
}