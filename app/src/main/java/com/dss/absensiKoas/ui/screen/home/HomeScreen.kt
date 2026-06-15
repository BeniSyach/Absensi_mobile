package com.dss.absensiKoas.ui.screen.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.dss.absensiKoas.BuildConfig
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
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

    LaunchedEffect(uiState.logoutSuccess) {
        if (uiState.logoutSuccess) onLogout()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Absensi") },
                actions = {
                    IconButton(onClick = { viewModel.muatData() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Muat ulang")
                    }
                    IconButton(onClick = { viewModel.logout() }) {
                        Icon(Icons.Default.Logout, contentDescription = "Logout")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            ProfilHeader(
                nama = uiState.profil?.namaLengkap ?: "Memuat...",
                nip = uiState.profil?.nip ?: "-",
                opd = uiState.profil?.opd?.nama ?: "-",
                fotoUrl = uiState.profil?.fotoProfil?.let { "${BuildConfig.BASE_URL}api/v1/foto/$it" },
                shiftNama = uiState.profil?.shiftAktif?.nama,
                jamMasuk = uiState.profil?.shiftAktif?.jamMasuk,
                jamPulang = uiState.profil?.shiftAktif?.jamPulang,
                onClick = onProfil
            )

            Spacer(modifier = Modifier.height(20.dp))

            StatusHariIniCard(uiState = uiState)

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                "Menu Absensi",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MenuAbsenButton(
                    label = "Absen Masuk",
                    icon = Icons.Default.Login,
                    color = MaterialTheme.colorScheme.primary,
                    enabled = uiState.statusHariIni?.sudahAbsenMasuk != true,
                    sudahDilakukan = uiState.statusHariIni?.sudahAbsenMasuk == true,
                    modifier = Modifier.weight(1f),
                    onClick = onAbsenMasuk
                )
                MenuAbsenButton(
                    label = "Absen Pulang",
                    icon = Icons.Default.Logout,
                    color = MaterialTheme.colorScheme.tertiary,
                    enabled = uiState.statusHariIni?.sudahAbsenMasuk == true
                            && uiState.statusHariIni?.sudahAbsenPulang != true,
                    sudahDilakukan = uiState.statusHariIni?.sudahAbsenPulang == true,
                    modifier = Modifier.weight(1f),
                    onClick = onAbsenPulang
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = onRiwayat,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.History, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Riwayat Absensi")
            }

            uiState.errorMessage?.let { error ->
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun ProfilHeader(
    nama: String,
    nip: String,
    opd: String,
    fotoUrl: String?,
    shiftNama: String?,
    jamMasuk: String?,
    jamPulang: String?,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (fotoUrl != null) {
                AsyncImage(
                    model = fotoUrl,
                    contentDescription = "Foto profil",
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Person, contentDescription = null)
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(nama, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("NIP: $nip", style = MaterialTheme.typography.bodySmall)
                Text(opd, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (shiftNama != null && jamMasuk != null && jamPulang != null) {
                    Text(
                        "$shiftNama: $jamMasuk - $jamPulang",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Icon(Icons.Default.ChevronRight, contentDescription = null)
        }
    }
}

@Composable
private fun StatusHariIniCard(uiState: HomeUiState) {
    val today = LocalDateTime.now()
    val formatter = DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale("id", "ID"))

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Status Hari Ini",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                today.format(formatter),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            val status = uiState.statusHariIni

            Row(modifier = Modifier.fillMaxWidth()) {
                StatusItem(
                    label = "Absen Masuk",
                    waktu = status?.waktuMasuk?.takeLast(8),
                    statusText = status?.statusMasuk,
                    sudah = status?.sudahAbsenMasuk == true,
                    modifier = Modifier.weight(1f)
                )
                StatusItem(
                    label = "Absen Pulang",
                    waktu = status?.waktuPulang?.takeLast(8),
                    statusText = status?.statusPulang,
                    sudah = status?.sudahAbsenPulang == true,
                    modifier = Modifier.weight(1f)
                )
            }

            status?.durasiKerjaMenit?.let {
                Spacer(modifier = Modifier.height(8.dp))
                val jam = it / 60
                val menit = it % 60
                Text(
                    "Total durasi kerja: $jam jam $menit menit",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun StatusItem(
    label: String,
    waktu: String?,
    statusText: String?,
    sudah: Boolean,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                if (sudah) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                contentDescription = null,
                tint = if (sudah) Color(0xFF2E7D32) else MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(label, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
        }
        if (sudah) {
            Text(waktu ?: "-", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
            statusText?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.labelSmall,
                    color = when (it) {
                        "TERLAMBAT", "PULANG_AWAL" -> Color(0xFFEF6C00)
                        else -> Color(0xFF2E7D32)
                    }
                )
            }
        } else {
            Text("Belum absen", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun MenuAbsenButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    enabled: Boolean,
    sudahDilakukan: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.height(110.dp),
        onClick = onClick,
        enabled = enabled,
        colors = CardDefaults.cardColors(
            containerColor = if (sudahDilakukan)
                MaterialTheme.colorScheme.surfaceVariant
            else
                color.copy(alpha = if (enabled) 0.12f else 0.05f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                if (sudahDilakukan) Icons.Default.CheckCircle else icon,
                contentDescription = null,
                tint = if (sudahDilakukan) Color(0xFF2E7D32) else if (enabled) color else MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                if (sudahDilakukan) "$label\n(Sudah)" else label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )
        }
    }
}