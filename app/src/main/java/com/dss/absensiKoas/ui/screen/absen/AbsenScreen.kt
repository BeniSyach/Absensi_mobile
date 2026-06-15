package com.dss.absensiKoas.ui.screen.absen

import android.Manifest
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.dss.absensiKoas.ui.component.CameraCaptureScreen
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionState
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import java.io.File
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun AbsenScreen(
    jenisAbsen: JenisAbsen,
    onAbsenSukses: () -> Unit,
    viewModel: AbsenViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    var showCamera by remember { mutableStateOf(false) }
    var fotoFile by remember { mutableStateOf<File?>(null) }
    var catatan by remember { mutableStateOf("") }

    val permissionsState = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.CAMERA
        )
    )

    LaunchedEffect(Unit) {
        if (!permissionsState.allPermissionsGranted) {
            permissionsState.launchMultiplePermissionRequest()
        } else {
            viewModel.ambilLokasi()
        }
    }

    LaunchedEffect(permissionsState.allPermissionsGranted) {
        if (permissionsState.allPermissionsGranted && uiState.lokasiSaatIni == null) {
            viewModel.ambilLokasi()
        }
    }

    if (showCamera) {
        CameraCaptureScreen(
            onPhotoTaken = { file ->
                fotoFile = file
                showCamera = false
            },
            onDismiss = { showCamera = false }
        )
        return
    }

    uiState.absenResult?.let { result ->
        AbsenSuccessDialog(
            result = result,
            onDismiss = {
                viewModel.resetAbsenResult()
                onAbsenSukses()
            }
        )
    }

    if (!permissionsState.allPermissionsGranted) {
        PermissionRequiredScreen(permissionsState.permissions) {
            permissionsState.launchMultiplePermissionRequest()
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = if (jenisAbsen == JenisAbsen.MASUK) "Absen Masuk" else "Absen Pulang",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = java.time.LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("EEEE, dd MMMM yyyy - HH:mm")),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(20.dp))

        LokasiCard(
            uiState = uiState,
            onRefreshLokasi = { viewModel.ambilLokasi() }
        )

        Spacer(modifier = Modifier.height(16.dp))

        FotoCard(
            fotoFile = fotoFile,
            onAmbilFoto = { showCamera = true }
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = catatan,
            onValueChange = { catatan = it },
            label = { Text("Catatan (opsional)") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2,
            maxLines = 3,
            shape = RoundedCornerShape(12.dp)
        )

        uiState.errorMessage?.let { error ->
            Spacer(modifier = Modifier.height(12.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Error, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(error, color = MaterialTheme.colorScheme.onErrorContainer, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        val bisaSubmit = uiState.lokasiSaatIni != null && fotoFile != null && !uiState.isSubmitting

        Button(
            onClick = {
                fotoFile?.let { file ->
                    if (jenisAbsen == JenisAbsen.MASUK) {
                        viewModel.absenMasuk(file, catatan.ifBlank { null })
                    } else {
                        viewModel.absenPulang(file, catatan.ifBlank { null })
                    }
                }
            },
            enabled = bisaSubmit,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (jenisAbsen == JenisAbsen.MASUK)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.tertiary
            )
        ) {
            if (uiState.isSubmitting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
            } else {
                Icon(
                    if (jenisAbsen == JenisAbsen.MASUK) Icons.Default.Login else Icons.Default.Logout,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    if (jenisAbsen == JenisAbsen.MASUK) "Absen Masuk Sekarang" else "Absen Pulang Sekarang",
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}

@Composable
private fun LokasiCard(
    uiState: AbsenUiState,
    onRefreshLokasi: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when {
                uiState.mockLocationTerdeteksi -> MaterialTheme.colorScheme.errorContainer
                uiState.lokasiSaatIni != null -> MaterialTheme.colorScheme.primaryContainer
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = if (uiState.mockLocationTerdeteksi)
                        MaterialTheme.colorScheme.error
                    else
                        MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Lokasi Anda",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = onRefreshLokasi, modifier = Modifier.size(32.dp)) {
                    if (uiState.isLoadingLokasi) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.Refresh, contentDescription = "Muat ulang lokasi")
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            when {
                uiState.isLoadingLokasi -> {
                    Text("Mencari lokasi GPS...", style = MaterialTheme.typography.bodyMedium)
                }
                uiState.lokasiSaatIni != null -> {
                    val loc = uiState.lokasiSaatIni
                    Text(
                        "Lat: ${String.format("%.6f", loc.latitude)}, Lon: ${String.format("%.6f", loc.longitude)}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        "Akurasi: ${loc.accuracy.toInt()} meter",
                        style = MaterialTheme.typography.bodySmall
                    )

                    if (uiState.mockLocationTerdeteksi) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                "Mock Location terdeteksi!",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                else -> {
                    Text(
                        "Lokasi belum tersedia. Tekan tombol refresh.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            uiState.peringatanLokasi?.let { peringatan ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    peringatan,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun FotoCard(
    fotoFile: File?,
    onAmbilFoto: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onAmbilFoto
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp),
            contentAlignment = Alignment.Center
        ) {
            if (fotoFile != null) {
                AsyncImage(
                    model = fotoFile,
                    contentDescription = "Foto absen",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(12.dp)
                ) {
                    FloatingActionButton(
                        onClick = onAmbilFoto,
                        modifier = Modifier.size(48.dp),
                        containerColor = MaterialTheme.colorScheme.primary
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = "Ambil ulang foto", tint = Color.White)
                    }
                }
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.CameraAlt,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Tap untuk ambil foto selfie",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        "Foto wajib untuk absen",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun AbsenSuccessDialog(
    result: com.dss.absensiKoas.data.model.AbsenResponse,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(16.dp)) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val isSukses = result.lokasiValid == true && result.mockLocationDetected != true

                Icon(
                    if (isSukses) Icons.Default.CheckCircle else Icons.Default.Info,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = if (isSukses) Color(0xFF2E7D32) else Color(0xFFF57F17)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    if (result.jenis == "MASUK") "Absen Masuk Berhasil" else "Absen Pulang Berhasil",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    "Status: ${result.status}",
                    style = MaterialTheme.typography.bodyMedium
                )

                result.jarakDariKantor?.let {
                    Text(
                        "Jarak dari kantor: ${it.toInt()} meter",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                result.durasiKerjaMenit?.let {
                    val jam = it / 60
                    val menit = it % 60
                    Text(
                        "Durasi kerja: $jam jam $menit menit",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                result.pesan?.let {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        it,
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        color = if (result.mockLocationDetected == true)
                            MaterialTheme.colorScheme.error
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Selesai")
                }
            }
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun PermissionRequiredScreen(
    permissions: List<PermissionState>,
    onRequestPermission: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.LocationOff,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "Izin Diperlukan",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Aplikasi membutuhkan izin Lokasi dan Kamera untuk melakukan absensi. " +
                    "Silakan berikan izin agar dapat melanjutkan.",
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onRequestPermission) {
            Text("Berikan Izin")
        }
    }
}