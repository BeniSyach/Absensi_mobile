package com.dss.absensiKoas.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LockClock
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.dss.absensiKoas.ui.theme.AccentAmber
import com.dss.absensiKoas.ui.theme.CardDark
import com.dss.absensiKoas.ui.theme.PrimaryLight
import com.dss.absensiKoas.ui.theme.TextSecondary

/**
 * Dialog modal yang muncul saat sesi habis (token expired & refresh gagal).
 *
 * dismissOnBackPress = false dan dismissOnClickOutside = false
 * SENGAJA dipasang agar user tidak bisa menutup dialog ini tanpa menekan
 * tombol "Login Ulang" — mencegah user "terjebak" di layar yang sudah
 * tidak punya sesi valid lagi.
 */
@Composable
fun SessionExpiredDialog(onLoginUlang: () -> Unit) {
    Dialog(
        onDismissRequest = { /* sengaja kosong, lihat properties di bawah */ },
        properties = DialogProperties(
            dismissOnBackPress    = false,
            dismissOnClickOutside = false
        )
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = CardDark
        ) {
            Column(
                modifier = Modifier.padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .background(AccentAmber.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.LockClock,
                        contentDescription = null,
                        tint = AccentAmber,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    "Sesi Anda Telah Habis",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    "Demi keamanan, sesi login Anda sudah tidak berlaku. " +
                            "Silakan masuk kembali untuk melanjutkan.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onLoginUlang,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryLight)
                ) {
                    Text(
                        "Login Ulang",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
