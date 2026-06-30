package com.dss.absensiKoas.data.api

import com.dss.absensiKoas.data.local.SessionManager
import com.dss.absensiKoas.data.local.TokenManager
import com.dss.absensiKoas.data.model.ApiResponse
import com.dss.absensiKoas.data.model.LoginResponse
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Authenticator dipanggil OkHttp SECARA OTOMATIS setiap kali response = 401.
 * Berbeda dengan Interceptor biasa, Authenticator baru jalan SETELAH
 * tahu response gagal — jadi pas dipakai untuk refresh-token flow.
 *
 * Alur:
 *  1. Request manapun balas 401 (token expired)
 *  2. OkHttp panggil authenticate() di sini
 *  3. Kita coba POST /api/v1/auth/refresh dengan refresh token tersimpan
 *  4a. Jika refresh SUKSES   → simpan access token baru, retry request asli
 *  4b. Jika refresh GAGAL    → hapus sesi lokal + panggil SessionManager.notifySessionExpired()
 *                              return null (OkHttp berhenti retry, request asli tetap gagal)
 *
 * Catatan penting:
 * - Authenticator dipanggil di thread OkHttp (bukan main thread) sehingga
 *   pemanggilan suspend function harus dibungkus runBlocking.
 * - responseCount dipakai untuk mencegah infinite retry loop jika
 *   token baru pun ternyata tetap ditolak server.
 */
@Singleton
class TokenAuthenticator @Inject constructor(
    private val tokenManager: TokenManager,
    private val sessionManager: SessionManager,
    private val plainOkHttpClient: OkHttpClient, // client TANPA authenticator, hindari rekursi
    private val baseUrl: String,
    private val json: Json
) : Authenticator {

    override fun authenticate(route: Route?, response: Response): Request? {
        // Cegah infinite loop: jika request ini sudah pernah di-retry sebelumnya, menyerah.
        if (responseCount(response) >= 2) {
            triggerSessionExpired()
            return null
        }

        // Endpoint publik (login/refresh/registrasi) tidak butuh authenticator
        val path = response.request.url.encodedPath
        if (path.contains("/auth/login") || path.contains("/auth/refresh") || path.contains("/user/registrasi")) {
            return null
        }

        return runBlocking {
            val refreshToken = tokenManager.getRefreshToken()

            if (refreshToken.isNullOrBlank()) {
                // Tidak ada refresh token sama sekali → sesi memang sudah habis
                triggerSessionExpired()
                return@runBlocking null
            }

            val newAccessToken = tryRefreshToken(refreshToken)

            if (newAccessToken == null) {
                // Refresh token juga ditolak server (sudah expired/invalid)
                triggerSessionExpired()
                null
            } else {
                tokenManager.updateAccessToken(newAccessToken)
                // Retry request ASLI dengan token baru
                response.request.newBuilder()
                    .header("Authorization", "Bearer $newAccessToken")
                    .build()
            }
        }
    }

    /**
     * Panggil endpoint refresh secara manual menggunakan plain OkHttpClient
     * (TANPA AuthInterceptor/Authenticator) agar tidak rekursi/saling memanggil.
     */
    private fun tryRefreshToken(refreshToken: String): String? {
        return try {
            val request = Request.Builder()
                .url("$baseUrl/api/v1/auth/refresh")
                .header("X-Refresh-Token", refreshToken)
                .post(okhttp3.RequestBody.create(null, ByteArray(0)))
                .build()

            val response = plainOkHttpClient.newCall(request).execute()
            response.use {
                if (!it.isSuccessful) return null
                val bodyStr = it.body?.string() ?: return null
                val parsed = json.decodeFromString<ApiResponse<LoginResponse>>(bodyStr)
                if (parsed.success) parsed.data?.accessToken else null
            }
        } catch (e: Exception) {
            // Network error saat refresh (timeout, no internet, dll)
            null
        }
    }

    private fun triggerSessionExpired() {
        // Hapus token lokal secara síncron (blocking, aman karena bukan main thread)
        runBlocking { tokenManager.clearSession() }
        sessionManager.notifySessionExpired()
    }

    /** Hitung berapa kali request ini sudah di-retry, dengan menyusuri priorResponse */
    private fun responseCount(response: Response): Int {
        var result = 1
        var prior = response.priorResponse
        while (prior != null) {
            result++
            prior = prior.priorResponse
        }
        return result
    }
}