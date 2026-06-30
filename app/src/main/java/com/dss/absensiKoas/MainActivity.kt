package com.dss.absensiKoas

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.dss.absensiKoas.data.local.SessionManager
import com.dss.absensiKoas.data.local.TokenManager
import com.dss.absensiKoas.ui.component.SessionExpiredDialog
import com.dss.absensiKoas.ui.navigation.AbsensiNavGraph
import com.dss.absensiKoas.ui.navigation.NavRoutes
import com.dss.absensiKoas.ui.screen.SplashScreen
import com.dss.absensiKoas.ui.theme.AbsensiKoasTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var tokenManager: TokenManager

    @Inject
    lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            AbsensiKoasTheme {
                Surface(modifier = Modifier.fillMaxSize()) {

                    // ── navController dibuat PALING AWAL ──
                    // Harus ada di sini (bukan di dalam blok else di bawah),
                    // karena dipakai juga oleh LaunchedEffect sesi habis
                    // yang letaknya lebih atas dalam komposisi.
                    val navController = rememberNavController()

                    var showSplash by remember { mutableStateOf(true) }
                    var startDestination by remember { mutableStateOf<String?>(null) }
                    var showSessionExpiredDialog by remember { mutableStateOf(false) }

                    // ── Amati event sesi habis SECARA GLOBAL ──
                    // Berjalan di level Activity, jadi tertangkap di MANAPUN
                    // posisi user saat itu — Home, Absen, Profil, dst.
                    LaunchedEffect(Unit) {
                        sessionManager.sessionExpiredEvent.collect {
                            showSessionExpiredDialog = true
                        }
                    }

                    // ── Cek status login di background, bersamaan dengan splash ──
                    LaunchedEffect(Unit) {
                        val isLoggedIn = tokenManager.isLoggedIn()
                        startDestination = if (isLoggedIn) {
                            NavRoutes.Home.route
                        } else {
                            NavRoutes.Login.route
                        }
                    }

                    // ── Render dialog di atas semua layar (overlay) ──
                    // Diletakkan SETELAH navController dideklarasikan agar
                    // closure onLoginUlang bisa mengaksesnya.
                    if (showSessionExpiredDialog) {
                        SessionExpiredDialog(
                            onLoginUlang = {
                                showSessionExpiredDialog = false
                                navigateToLoginAndClearStack(navController)
                            }
                        )
                    }

                    when {
                        showSplash -> {
                            SplashScreen(onFinished = { showSplash = false })
                        }
                        startDestination == null -> {
                            // Jaga-jaga jika cek login belum selesai saat splash habis
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        }
                        else -> {
                            AbsensiNavGraph(
                                navController = navController,
                                startDestination = startDestination!!
                            )
                        }
                    }
                }
            }
        }
    }

    /**
     * Navigasi ke Login dan bersihkan SELURUH back stack sebelumnya.
     * Tanpa ini, user bisa menekan tombol "back" dari Login dan
     * kembali ke layar lama yang sebenarnya sudah tidak punya sesi valid.
     */
    private fun navigateToLoginAndClearStack(
        navController: androidx.navigation.NavHostController
    ) {
        navController.navigate(NavRoutes.Login.route) {
            popUpTo(0) { inclusive = true }
            launchSingleTop = true
        }
    }
}