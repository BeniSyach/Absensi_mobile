package com.dss.absensiKoas.ui.screen.riwayat

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.dss.absensiKoas.data.model.AbsenRiwayatItem
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

private enum class TabRiwayat { MASUK, PULANG }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RiwayatScreen(
    onBack: () -> Unit,
    viewModel: RiwayatViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var tab by remember { mutableStateOf(TabRiwayat.MASUK) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Riwayat Absensi") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Kembali")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            TabRow(selectedTabIndex = tab.ordinal) {
                Tab(
                    selected = tab == TabRiwayat.MASUK,
                    onClick = { tab = TabRiwayat.MASUK },
                    text = { Text("Absen Masuk") }
                )
                Tab(
                    selected = tab == TabRiwayat.PULANG,
                    onClick = { tab = TabRiwayat.PULANG },
                    text = { Text("Absen Pulang") }
                )
            }

            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                val data = if (tab == TabRiwayat.MASUK) uiState.riwayatMasuk else uiState.riwayatPulang

                if (data.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            "Tidak ada riwayat untuk periode ini",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(data) { item ->
                            RiwayatItemCard(item = item, jenisMasuk = tab == TabRiwayat.MASUK)
                        }
                    }
                }
            }

            uiState.errorMessage?.let { error ->
                Text(
                    error,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}

@Composable
private fun RiwayatItemCard(item: AbsenRiwayatItem, jenisMasuk: Boolean) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        formatTanggal(item.tanggal),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    val waktu = if (jenisMasuk) item.waktuMasuk else item.waktuPulang
                    Text(
                        waktu?.let { formatWaktu(it) } ?: "-",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                item.status?.let { status ->
                    AssistChip(
                        onClick = {},
                        label = { Text(status, style = MaterialTheme.typography.labelSmall) },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = statusColor(status).copy(alpha = 0.15f),
                            labelColor = statusColor(status)
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (item.lokasiValid == true) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF2E7D32), modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Lokasi valid", style = MaterialTheme.typography.labelSmall, color = Color(0xFF2E7D32))
                } else {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFEF6C00), modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Di luar radius", style = MaterialTheme.typography.labelSmall, color = Color(0xFFEF6C00))
                }

                item.jarakDariKantor?.let {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("${it.toInt()}m dari kantor", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

private fun statusColor(status: String): Color = when (status) {
    "TERLAMBAT", "PULANG_AWAL" -> Color(0xFFEF6C00)
    "ALPA" -> Color(0xFFC62828)
    "IZIN", "SAKIT" -> Color(0xFF1565C0)
    else -> Color(0xFF2E7D32)
}

private fun formatTanggal(isoDate: String): String {
    return try {
        val date = LocalDateTime.parse(isoDate + "T00:00:00")
        date.format(DateTimeFormatter.ofPattern("EEEE, dd MMM yyyy", java.util.Locale("id", "ID")))
    } catch (e: Exception) {
        isoDate
    }
}

private fun formatWaktu(isoDateTime: String): String {
    return try {
        val dt = LocalDateTime.parse(isoDateTime)
        dt.format(DateTimeFormatter.ofPattern("HH:mm:ss"))
    } catch (e: Exception) {
        isoDateTime
    }
}