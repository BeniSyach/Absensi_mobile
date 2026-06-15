package com.dss.absensiKoas.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.dss.absensiKoas.ui.screen.absen.AbsenScreen
import com.dss.absensiKoas.ui.screen.absen.JenisAbsen
import com.dss.absensiKoas.ui.screen.home.HomeScreen
import com.dss.absensiKoas.ui.screen.login.LoginScreen
import com.dss.absensiKoas.ui.screen.login.RegistrasiScreen
import com.dss.absensiKoas.ui.screen.profil.GantiPasswordScreen
import com.dss.absensiKoas.ui.screen.profil.ProfilScreen
import com.dss.absensiKoas.ui.screen.riwayat.RiwayatScreen

@Composable
fun AbsensiNavGraph(
    navController: NavHostController,
    startDestination: String
) {
    NavHost(navController = navController, startDestination = startDestination) {

        // === LOGIN ===
        composable(NavRoutes.Login.route) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(NavRoutes.Home.route) {
                        popUpTo(NavRoutes.Login.route) { inclusive = true }
                    }
                },
                onNavigateToRegistrasi = {
                    navController.navigate(NavRoutes.Registrasi.route)
                }
            )
        }

        // === REGISTRASI ===
        composable(NavRoutes.Registrasi.route) {
            RegistrasiScreen(
                onBack = { navController.popBackStack() },
                onRegistrasiSukses = {
                    navController.popBackStack()
                }
            )
        }

        // === HOME / DASHBOARD ===
        composable(NavRoutes.Home.route) {
            HomeScreen(
                onAbsenMasuk = { navController.navigate(NavRoutes.AbsenMasuk.route) },
                onAbsenPulang = { navController.navigate(NavRoutes.AbsenPulang.route) },
                onRiwayat = { navController.navigate(NavRoutes.Riwayat.route) },
                onProfil = { navController.navigate(NavRoutes.Profil.route) },
                onLogout = {
                    navController.navigate(NavRoutes.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        // === ABSEN MASUK ===
        composable(NavRoutes.AbsenMasuk.route) {
            AbsenScreen(
                jenisAbsen = JenisAbsen.MASUK,
                onAbsenSukses = {
                    navController.popBackStack()
                }
            )
        }

        // === ABSEN PULANG ===
        composable(NavRoutes.AbsenPulang.route) {
            AbsenScreen(
                jenisAbsen = JenisAbsen.PULANG,
                onAbsenSukses = {
                    navController.popBackStack()
                }
            )
        }

        // === RIWAYAT ===
        composable(NavRoutes.Riwayat.route) {
            RiwayatScreen(
                onBack = { navController.popBackStack() }
            )
        }

        // === PROFIL ===
        composable(NavRoutes.Profil.route) {
            ProfilScreen(
                onBack = { navController.popBackStack() },
                onGantiPassword = { navController.navigate(NavRoutes.GantiPassword.route) }
            )
        }

        // === GANTI PASSWORD ===
        composable(NavRoutes.GantiPassword.route) {
            GantiPasswordScreen(
                onBack = { navController.popBackStack() },
                onSuccess = {
                    // Setelah ganti password, sesi server dihapus -> arahkan ke login
                    navController.navigate(NavRoutes.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
    }
}