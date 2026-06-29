package com.dss.absensiKoas.ui.screen.absen

import android.Manifest
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

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun AbsenScreen(
    jenisAbsen: JenisAbsen,
    onAbsenSukses: () -> Unit,
    viewModel: AbsenViewModel = hiltViewModel()
) {
    val uiState by viewModel.state.collectAsStateWithLifecycle()
    var showCamera by remember { mutableStateOf(false) }
    var fotoFile by remember { mutableStateOf<File?>(null) }
    var catatan by remember { mutableStateOf("") }
    var visible by remember { mutableStateOf(false) }

    val permissionsState = rememberMultiplePermissionsState(
        listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.CAMERA)
    )

    LaunchedEffect(Unit) {
        visible = true
        if (permissionsState.allPermissionsGranted) viewModel.ambilLokasi()
        else permissionsState.launchMultiplePermissionRequest()
    }
    LaunchedEffect(permissionsState.allPermissionsGranted) {
        if (permissionsState.allPermissionsGranted && uiState.lokasiSaatIni == null) {
            viewModel.ambilLokasi()
        }
    }

    if (showCamera) {
        CameraCaptureScreen(
            onPhotoTaken = { file -> fotoFile = file; showCamera = false },
            onDismiss = { showCamera = false }
        )
        return
    }

    if (!permissionsState.allPermissionsGranted) {
        PermissionScreen(onRequest = { permissionsState.launchMultiplePermissionRequest() })
        return
    }

    // Dialog sukses
    uiState.absenResult?.let { result ->
        AbsenSuccessOverlay(result = result, jenisAbsen = jenisAbsen) {
            viewModel.resetAbsenResult()
            onAbsenSukses()
        }
        return
    }

    AnimatedGradientBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
        ) {
            // ===== HEADER =====
            AbsenHeader(jenisAbsen = jenisAbsen, visible = visible)

            // ===== MAPS CARD =====
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(600, 200)) + slideInVertically(tween(600, 200)) { it / 3 }
            ) {
                MapsLocationCard(
                    uiState = uiState,
                    profil = null, // bisa diisi dari ViewModel jika perlu
                    onRefresh = { viewModel.ambilLokasi() }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ===== FOTO CARD =====
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(600, 350)) + slideInVertically(tween(600, 350)) { it / 3 }
            ) {
                FotoCaptureCard(
                    fotoFile = fotoFile,
                    onAmbilFoto = { showCamera = true }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ===== CATATAN =====
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(600, 450)) + slideInVertically(tween(600, 450)) { it / 3 }
            ) {
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    cornerRadius = 18.dp
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Catatan (opsional)", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = catatan,
                            onValueChange = { catatan = it },
                            placeholder = { Text("Tambahkan catatan...", color = TextSecondary) },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2, maxLines = 3,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor     = Color.White,
                                unfocusedTextColor   = Color.White,
                                focusedBorderColor   = AccentCyan,
                                unfocusedBorderColor = Color.White.copy(0.2f),
                                focusedContainerColor  = Color.White.copy(0.07f),
                                unfocusedContainerColor= Color.White.copy(0.04f),
                                cursorColor          = AccentCyan
                            )
                        )
                    }
                }
            }

            // ===== ERROR =====
            uiState.errorMessage?.let { error ->
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(ErrorRed.copy(0.15f))
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.ErrorOutline, null, tint = ErrorRed, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(error, style = MaterialTheme.typography.bodySmall, color = ErrorRed)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ===== RADIUS STATUS BAR =====
            // Tampilkan hanya jika lokasi sudah didapat
            if (uiState.lokasiSaatIni != null) {
                AnimatedVisibility(
                    visible = visible,
                    enter = fadeIn(tween(400, 500))
                ) {
                    RadiusStatusBar(uiState = uiState)
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // ===== SUBMIT BUTTON =====
            // bisaSubmit: lokasi ada, foto ada, tidak loading,
            //             TIDAK mock, TIDAK di luar radius (jika sudah diketahui)
            val diluarRadius = uiState.dalamRadius == false
            val isMock       = uiState.mockLocationTerdeteksi
            val bisaSubmit   = uiState.lokasiSaatIni != null
                    && fotoFile != null
                    && !uiState.isSubmitting
                    && !diluarRadius
                    && !isMock

            val gradientColors = when {
                diluarRadius || isMock -> listOf(Color(0xFF374151), Color(0xFF1F2937)) // abu-abu disabled
                jenisAbsen == JenisAbsen.MASUK -> listOf(PrimaryLight, AccentCyan)
                else -> listOf(Color(0xFF7C3AED), Color(0xFFDB2777))
            }

            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(600, 550)) + slideInVertically(tween(600, 550)) { it }
            ) {
                GradientButton(
                    text = when {
                        isMock       -> "Absen Ditolak — Fake GPS"
                        diluarRadius -> "Di Luar Radius Kantor"
                        jenisAbsen == JenisAbsen.MASUK -> "Absen Masuk Sekarang"
                        else -> "Absen Pulang Sekarang"
                    },
                    onClick = {
                        fotoFile?.let { file ->
                            if (jenisAbsen == JenisAbsen.MASUK)
                                viewModel.absenMasuk(file, catatan.ifBlank { null })
                            else
                                viewModel.absenPulang(file, catatan.ifBlank { null })
                        }
                    },
                    enabled = bisaSubmit,
                    isLoading = uiState.isSubmitting,
                    gradientColors = gradientColors,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .height(56.dp),
                    icon = {
                        Icon(
                            when {
                                isMock || diluarRadius -> Icons.Default.Block
                                jenisAbsen == JenisAbsen.MASUK -> Icons.Default.Login
                                else -> Icons.Default.Logout
                            },
                            null, tint = Color.White, modifier = Modifier.size(22.dp)
                        )
                    }
                )
            }

            // Hint kondisi belum siap
            if (!bisaSubmit && !uiState.isSubmitting) {
                Spacer(modifier = Modifier.height(8.dp))
                val hint = when {
                    isMock -> "Nonaktifkan aplikasi Fake GPS lalu refresh lokasi"
                    diluarRadius -> "Menuju area kantor (radius ${uiState.radiusKantor}m)"
                    uiState.lokasiSaatIni == null && fotoFile == null -> "Diperlukan: lokasi GPS & foto selfie"
                    uiState.lokasiSaatIni == null -> "Menunggu lokasi GPS..."
                    fotoFile == null -> "Ambil foto selfie terlebih dahulu"
                    else -> ""
                }
                if (hint.isNotBlank()) {
                    Text(
                        hint,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

// ==========================================
// MAPS LOCATION CARD
// ==========================================

@Composable
fun MapsLocationCard(
    uiState: AbsenUiState,
    profil: com.dss.absensiKoas.data.model.UserDetailResponse?,
    onRefresh: () -> Unit
) {
    val lokasi = uiState.lokasiSaatIni

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        cornerRadius = 20.dp
    ) {
        Column {
            // Header lokasi
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
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
                            if (uiState.mockLocationTerdeteksi) Icons.Default.LocationOff else Icons.Default.MyLocation,
                            null,
                            tint = if (uiState.mockLocationTerdeteksi) ErrorRed else AccentCyan,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text("Lokasi GPS", style = MaterialTheme.typography.titleSmall, color = Color.White, fontWeight = FontWeight.Bold)
                        Text(
                            when {
                                uiState.isLoadingLokasi        -> "Mencari sinyal GPS..."
                                uiState.mockLocationTerdeteksi -> "⚠ Mock Location terdeteksi!"
                                lokasi != null                 -> "Akurasi: ${lokasi.accuracy.toInt()}m"
                                else                           -> "Lokasi belum tersedia"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = when {
                                uiState.mockLocationTerdeteksi -> ErrorRed
                                lokasi != null -> AccentGreen
                                else -> TextSecondary
                            }
                        )
                    }
                }
                IconButton(onClick = onRefresh, modifier = Modifier.size(36.dp)) {
                    if (uiState.isLoadingLokasi) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = AccentCyan, strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.Refresh, null, tint = TextSecondary)
                    }
                }
            }

            // ===== GOOGLE MAPS =====
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .clip(RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp))
            ) {
                if (lokasi != null) {
                    val userPos = LatLng(lokasi.latitude, lokasi.longitude)
                    val cameraState = rememberCameraPositionState {
                        position = CameraPosition.fromLatLngZoom(userPos, 17f)
                    }

                    LaunchedEffect(lokasi) {
                        cameraState.animate(CameraUpdateFactory.newLatLngZoom(userPos, 17f), 1200)
                    }

                    GoogleMap(
                        modifier = Modifier.fillMaxSize(),
                        cameraPositionState = cameraState,
                        properties = MapProperties(
                            isMyLocationEnabled = false,
                            mapType = MapType.NORMAL
                        ),
                        uiSettings = MapUiSettings(
                            zoomControlsEnabled = false,
                            compassEnabled = false,
                            myLocationButtonEnabled = false,
                            scrollGesturesEnabled = true,
                            zoomGesturesEnabled = true
                        )
                    ) {
                        // Marker posisi user
                        Marker(
                            state = MarkerState(position = userPos),
                            title = "Posisi Anda",
                            snippet = "Lat: ${String.format("%.6f", lokasi.latitude)}, Lon: ${String.format("%.6f", lokasi.longitude)}",
                            icon = BitmapDescriptorFactory.defaultMarker(
                                if (uiState.mockLocationTerdeteksi) BitmapDescriptorFactory.HUE_RED
                                else BitmapDescriptorFactory.HUE_AZURE
                            )
                        )

                        // Lingkaran akurasi GPS
                        Circle(
                            center = userPos,
                            radius = lokasi.accuracy.toDouble(),
                            fillColor = AccentCyan.copy(alpha = 0.1f),
                            strokeColor = AccentCyan.copy(alpha = 0.5f),
                            strokeWidth = 2f
                        )
                    }

                    // Overlay koordinat di atas maps
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(10.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Black.copy(alpha = 0.65f))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            "${String.format("%.5f", lokasi.latitude)}, ${String.format("%.5f", lokasi.longitude)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White
                        )
                    }

                    // Badge mock location warning
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
                                Icon(Icons.Default.Warning, null, tint = Color.White, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("FAKE GPS TERDETEKSI", style = MaterialTheme.typography.labelSmall, color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                } else {
                    // Placeholder saat lokasi belum ada
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(CardDark),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            if (uiState.isLoadingLokasi) {
                                CircularProgressIndicator(color = AccentCyan)
                                Spacer(modifier = Modifier.height(12.dp))
                                Text("Mencari lokasi GPS...", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                            } else {
                                Icon(Icons.Outlined.LocationOff, null, tint = TextSecondary, modifier = Modifier.size(40.dp))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Tekan refresh untuk mengambil lokasi", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// FOTO CAPTURE CARD
// ==========================================

@Composable
fun FotoCaptureCard(fotoFile: File?, onAmbilFoto: () -> Unit) {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        cornerRadius = 20.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.CameraAlt, null, tint = AccentCyan, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Foto Selfie", style = MaterialTheme.typography.titleSmall, color = Color.White, fontWeight = FontWeight.Bold)
                if (fotoFile != null) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(AccentGreen.copy(0.2f))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text("✓ Siap", style = MaterialTheme.typography.labelSmall, color = AccentGreen)
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
                        model = fotoFile,
                        contentDescription = "Foto selfie",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    // Tombol retake
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(12.dp)
                            .clip(RoundedCornerShape(50))
                            .background(Color.Black.copy(0.65f))
                            .clickable(onClick = onAmbilFoto)
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CameraAlt, null, tint = Color.White, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Ulangi", style = MaterialTheme.typography.labelSmall, color = Color.White)
                        }
                    }
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(60.dp)
                                .background(
                                    Brush.linearGradient(listOf(PrimaryLight, AccentCyan)),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.CameraAlt, null, tint = Color.White, modifier = Modifier.size(28.dp))
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Tap untuk ambil foto selfie", style = MaterialTheme.typography.bodyMedium, color = Color.White, fontWeight = FontWeight.Medium)
                        Text("Gunakan kamera depan — wajah harus terlihat jelas", style = MaterialTheme.typography.bodySmall, color = TextSecondary, textAlign = TextAlign.Center)
                    }
                }
            }
        }
    }
}

// ==========================================
// ABSEN HEADER
// ==========================================

@Composable
private fun AbsenHeader(jenisAbsen: JenisAbsen, visible: Boolean) {
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(600),
        label = "header_alpha"
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(Color.Black.copy(0.3f), Color.Transparent)))
            .statusBarsPadding()
            .padding(20.dp)
            .graphicsLayer { this.alpha = alpha }
    ) {
        Column {
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
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm — dd MMM yyyy")),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }
        }
    }
}

// ==========================================
// SUKSES OVERLAY
// ==========================================

@Composable
private fun AbsenSuccessOverlay(
    result: com.dss.absensiKoas.data.model.AbsenResponse,
    jenisAbsen: JenisAbsen,
    onDismiss: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMediumLow),
        label = "success_scale"
    )
    val isMock   = result.mockLocationDetected == true
    val isValid  = result.lokasiValid == true

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.75f)),
        contentAlignment = Alignment.Center
    ) {
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp)
                .graphicsLayer { scaleX = scale; scaleY = scale },
            cornerRadius = 28.dp,
            alpha = 0.2f
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Icon animasi
                Box(
                    modifier = Modifier
                        .size(90.dp)
                        .background(
                            Brush.radialGradient(
                                if (isMock) listOf(ErrorRed.copy(0.3f), Color.Transparent)
                                else listOf(AccentGreen.copy(0.3f), Color.Transparent)
                            ),
                            CircleShape
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
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Detail info
                listOfNotNull(
                    "Status" to result.status,
                    result.jarakDariKantor?.let { "Jarak dari kantor" to "${it.toInt()} meter" },
                    result.durasiKerjaMenit?.let { "Durasi kerja" to "${it / 60} jam ${it % 60} menit" },
                    "Lokasi valid" to if (isValid) "✓ Ya" else "✗ Di luar radius"
                ).forEach { (label, value) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(label, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        Text(value, style = MaterialTheme.typography.bodySmall, color = Color.White, fontWeight = FontWeight.SemiBold)
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
                            color = if (isMock) ErrorRed else AccentGreen, textAlign = TextAlign.Center)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                GradientButton(
                    text = "Selesai",
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

@Composable
private fun PermissionScreen(onRequest: () -> Unit) {
    AnimatedGradientBackground {
        Column(
            modifier = Modifier.fillMaxSize().padding(40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier.size(90.dp)
                    .background(ErrorRed.copy(0.2f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.LocationOff, null, tint = ErrorRed, modifier = Modifier.size(48.dp))
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text("Izin Diperlukan", style = MaterialTheme.typography.headlineSmall, color = Color.White, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                "Aplikasi membutuhkan akses Lokasi GPS dan Kamera untuk proses absensi yang valid.",
                style = MaterialTheme.typography.bodyMedium, color = TextSecondary, textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(32.dp))
            GradientButton("Berikan Izin", onClick = onRequest, modifier = Modifier.fillMaxWidth())
        }
    }
}

// ========================================
// RADIUS STATUS BAR
// ========================================

/**
 * Komponen visual yang menampilkan status radius secara jelas:
 * - Progress bar jarak user ke kantor
 * - Warna hijau = dalam radius, merah = di luar, orange = mock
 * - Tidak bisa scroll melewati ini — user HARUS lihat statusnya
 */
@Composable
fun RadiusStatusBar(uiState: AbsenUiState) {
    val jarak       = uiState.jarakKeKantor
    val radius      = uiState.radiusKantor.toFloat()
    val dalamRadius = uiState.dalamRadius
    val isMock      = uiState.mockLocationTerdeteksi

    // Warna berdasarkan kondisi
    val (bgColor, borderColor, iconColor, statusColor) = when {
        isMock      -> listOf(ErrorRed.copy(0.12f), ErrorRed.copy(0.5f), ErrorRed, ErrorRed)
        dalamRadius == false -> listOf(AccentAmber.copy(0.12f), AccentAmber.copy(0.5f), AccentAmber, AccentAmber)
        dalamRadius == true  -> listOf(AccentGreen.copy(0.12f), AccentGreen.copy(0.5f), AccentGreen, AccentGreen)
        else        -> listOf(Color.White.copy(0.05f), Color.White.copy(0.15f), TextSecondary, TextSecondary)
    }

    // Progress jarak (0.0 = di kantor, 1.0 = di radius persis, >1.0 = di luar)
    val progressFraction = if (jarak != null && radius > 0)
        (jarak / radius).coerceIn(0f, 1f)
    else 0f

    // Animasi progress bar
    val animatedProgress by animateFloatAsState(
        targetValue = progressFraction,
        animationSpec = tween(800, easing = CubicBezierEasing(0.25f, 1f, 0.5f, 1f)),
        label = "radius_progress"
    )

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        cornerRadius = 18.dp,
        alpha = 0.1f
    ) {
        Column(
            modifier = Modifier
                .border(1.dp, borderColor, RoundedCornerShape(18.dp))
                .background(bgColor, RoundedCornerShape(18.dp))
                .padding(16.dp)
        ) {
            // Baris atas: icon + teks status + jarak
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Icon status dengan pulse jika mock
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(iconColor.copy(0.2f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        when {
                            isMock      -> Icons.Default.GpsOff
                            dalamRadius == false -> Icons.Default.LocationOff
                            dalamRadius == true  -> Icons.Default.LocationOn
                            else -> Icons.Default.MyLocation
                        },
                        null,
                        tint = iconColor,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = when {
                            isMock      -> "Fake GPS Terdeteksi"
                            dalamRadius == false -> "Di Luar Radius Kantor"
                            dalamRadius == true  -> "Dalam Radius Kantor"
                            else -> "Memeriksa Radius..."
                        },
                        style = MaterialTheme.typography.titleSmall,
                        color = statusColor,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = when {
                            isMock -> "Nonaktifkan fake GPS dan refresh lokasi"
                            jarak != null -> {
                                val jarakStr = if (jarak >= 1000) String.format("%.1f km", jarak / 1000)
                                else "${jarak.toInt()} m"
                                val radiusStr = if (radius >= 1000) String.format("%.1f km", radius / 1000)
                                else "${radius.toInt()} m"
                                "$jarakStr dari kantor • radius $radiusStr"
                            }
                            else -> uiState.namaOpd ?: "Memuat data kantor..."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = statusColor.copy(alpha = 0.8f)
                    )
                }

                // Badge status
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(statusColor.copy(0.2f))
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = when {
                            isMock      -> "DITOLAK"
                            dalamRadius == false -> "DITOLAK"
                            dalamRadius == true  -> "OK"
                            else -> "..."
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = statusColor,
                        fontWeight = FontWeight.Black
                    )
                }
            }

            // Progress bar jarak
            if (jarak != null && !isMock) {
                Spacer(modifier = Modifier.height(12.dp))

                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Posisi Anda", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                        Text(
                            if (dalamRadius == true) "✓ Dalam radius" else "Batas radius",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (dalamRadius == true) AccentGreen else AccentAmber
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Track
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(50))
                            .background(Color.White.copy(0.1f))
                    ) {
                        // Progress fill
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
                                    ),
                                    RoundedCornerShape(50)
                                )
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("0 m", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                        Text(
                            if (radius >= 1000) String.format("%.0f km", radius / 1000)
                            else "${radius.toInt()} m",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                }
            }

            // Pesan info dari ViewModel
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
                        Icon(Icons.Default.Info, null, tint = statusColor, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(pesan, style = MaterialTheme.typography.bodySmall, color = statusColor)
                    }
                }
            }
        }
    }
}