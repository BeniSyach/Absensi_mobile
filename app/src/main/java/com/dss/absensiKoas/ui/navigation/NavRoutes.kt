package com.dss.absensiKoas.ui.navigation

sealed class NavRoutes(val route: String) {
    object Login : NavRoutes("login")
    object Registrasi : NavRoutes("registrasi")
    object Home : NavRoutes("home")
    object AbsenMasuk : NavRoutes("absen_masuk")
    object AbsenPulang : NavRoutes("absen_pulang")
    object Riwayat : NavRoutes("riwayat")
    object Profil : NavRoutes("profil")
    object GantiPassword : NavRoutes("ganti_password")
}
