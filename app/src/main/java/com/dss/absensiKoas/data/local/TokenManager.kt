package com.dss.absensiKoas.data.local

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "absensi_secure_prefs")

/**
 * Penyimpanan token JWT, refresh token, dan device ID.
 * Menggunakan Jetpack DataStore (lebih aman & async dibanding SharedPreferences).
 */
@Singleton
class TokenManager @Inject constructor(
    private val context: Context
) {
    private object Keys {
        val ACCESS_TOKEN  = stringPreferencesKey("access_token")
        val REFRESH_TOKEN = stringPreferencesKey("refresh_token")
        val DEVICE_ID     = stringPreferencesKey("device_id")
        val USER_ID       = longPreferencesKey("user_id")
        val USERNAME      = stringPreferencesKey("username")
        val ROLE          = stringPreferencesKey("role")

        // Data OPD — disimpan saat login untuk validasi radius offline.
        // PENTING: koordinat GPS pakai doublePreferencesKey, BUKAN float.
        // Float hanya punya ~7 digit presisi desimal; pada koordinat lat/lon
        // (contoh: -6.914744123) ini bisa menggeser posisi sampai beberapa
        // meter setelah disimpan-baca ulang — fatal untuk validasi radius kantor.
        val OPD_NAMA         = stringPreferencesKey("opd_nama")
        val OPD_LAT_KANTOR   = doublePreferencesKey("opd_lat_kantor")
        val OPD_LON_KANTOR   = doublePreferencesKey("opd_lon_kantor")
        val OPD_RADIUS_ABSEN = intPreferencesKey("opd_radius_absen")
    }

    val accessTokenFlow: Flow<String?>  = context.dataStore.data.map { it[Keys.ACCESS_TOKEN] }
    val refreshTokenFlow: Flow<String?> = context.dataStore.data.map { it[Keys.REFRESH_TOKEN] }
    val usernameFlow: Flow<String?>     = context.dataStore.data.map { it[Keys.USERNAME] }
    val roleFlow: Flow<String?>         = context.dataStore.data.map { it[Keys.ROLE] }

    // Data OPD untuk cek radius di sisi klien
    suspend fun getOpdLatKantor(): Double? =
        context.dataStore.data.map { it[Keys.OPD_LAT_KANTOR] }.first()

    suspend fun getOpdLonKantor(): Double? =
        context.dataStore.data.map { it[Keys.OPD_LON_KANTOR] }.first()

    suspend fun getOpdRadiusAbsen(): Int =
        context.dataStore.data.map { it[Keys.OPD_RADIUS_ABSEN] ?: 100 }.first()

    suspend fun getOpdNama(): String? =
        context.dataStore.data.map { it[Keys.OPD_NAMA] }.first()

    suspend fun getAccessToken(): String? = accessTokenFlow.first()
    suspend fun getRefreshToken(): String? = refreshTokenFlow.first()

    suspend fun saveSession(
        accessToken: String,
        refreshToken: String?,
        userId: Long,
        username: String,
        role: String,
        opdNama: String? = null,
        opdLatKantor: Double? = null,
        opdLonKantor: Double? = null,
        opdRadiusAbsen: Int? = null
    ) {
        context.dataStore.edit { prefs ->
            prefs[Keys.ACCESS_TOKEN] = accessToken
            refreshToken?.let { prefs[Keys.REFRESH_TOKEN] = it }
            prefs[Keys.USER_ID]  = userId
            prefs[Keys.USERNAME] = username
            prefs[Keys.ROLE]     = role
            // Simpan data OPD jika tersedia — langsung Double, tanpa konversi presisi
            opdNama?.let        { prefs[Keys.OPD_NAMA]         = it }
            opdLatKantor?.let   { prefs[Keys.OPD_LAT_KANTOR]   = it }
            opdLonKantor?.let   { prefs[Keys.OPD_LON_KANTOR]   = it }
            opdRadiusAbsen?.let { prefs[Keys.OPD_RADIUS_ABSEN] = it }
        }
    }

    /** Update hanya access token — dipanggil TokenAuthenticator setelah refresh sukses */
    suspend fun updateAccessToken(accessToken: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.ACCESS_TOKEN] = accessToken
        }
    }

    suspend fun getOrCreateDeviceId(): String {
        val existing = context.dataStore.data.map { it[Keys.DEVICE_ID] }.first()
        if (existing != null) return existing

        val newId = "android-${android.os.Build.MODEL}-${java.util.UUID.randomUUID().toString().take(8)}"
            .replace(" ", "_")
        context.dataStore.edit { it[Keys.DEVICE_ID] = newId }
        return newId
    }

    /**
     * Hapus semua data sesi — dipanggil saat logout manual
     * ATAU saat sesi dipaksa habis (refresh token gagal, dipanggil
     * dari TokenAuthenticator.triggerSessionExpired()).
     */
    suspend fun clearSession() {
        context.dataStore.edit { it.clear() }
    }

    suspend fun isLoggedIn(): Boolean = getAccessToken() != null
}