package com.dss.absensiKoas

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.dss.absensiKoas.data.local.TokenManager
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            AbsensiKoasTheme {
                Surface(modifier = Modifier.fillMaxSize()) {

                    var showSplash by remember { mutableStateOf(true) }
                    var startDestination by remember { mutableStateOf<String?>(null) }

                    // Cek login di background, bersamaan dengan splash
                    LaunchedEffect(Unit) {
                        val isLoggedIn = tokenManager.isLoggedIn()
                        startDestination = if (isLoggedIn) {
                            NavRoutes.Home.route
                        } else {
                            NavRoutes.Login.route
                        }
                    }

                    if (showSplash) {
                        SplashScreen(
                            onFinished = { showSplash = false }
                        )
                    } else {
                        if (startDestination == null) {
                            // Jaga-jaga jika cek login belum selesai saat splash habis
                            androidx.compose.foundation.layout.Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        } else {
                            val navController = rememberNavController()
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
}