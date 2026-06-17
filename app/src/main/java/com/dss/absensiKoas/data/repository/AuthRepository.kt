package com.dss.absensiKoas.data.repository

import com.dss.absensiKoas.data.api.AbsensiApi
import com.dss.absensiKoas.data.local.TokenManager
import com.dss.absensiKoas.data.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val api: AbsensiApi,
    private val tokenManager: TokenManager
) {

    /**
     * Login dan simpan token JWT ke DataStore.
     */
    fun login(username: String, password: String): Flow<Resource<LoginResponse>> = flow {
        emit(Resource.Loading)
        try {
            val deviceId = tokenManager.getOrCreateDeviceId()
            val deviceInfo = "Android ${android.os.Build.VERSION.RELEASE} ${android.os.Build.MODEL}"

            val response = api.login(
                LoginRequest(
                    username = username,
                    password = password,
                    deviceId = deviceId,
                    deviceInfo = deviceInfo,

                )
            )

            if (response.isSuccessful && response.body()?.success == true) {
                val data = response.body()!!.data!!
                tokenManager.saveSession(
                    accessToken = data.accessToken,
                    refreshToken = data.refreshToken,
                    userId = data.user.id,
                    username = data.user.username,
                    role = data.user.role,
                    // Simpan koordinat kantor & radius OPD untuk validasi radius offline
                    opdNama        = data.user.opd?.nama,
                    opdLatKantor   = data.user.opd?.latitudeKantor,
                    opdLonKantor   = data.user.opd?.longitudeKantor,
                    opdRadiusAbsen = data.user.opd?.radiusAbsen
                )
                emit(Resource.Success(data))
            } else {
                val errorMsg = response.body()?.error ?: "Login gagal. Periksa username dan password."
                emit(Resource.Error(errorMsg))
            }
        } catch (e: Exception) {
            emit(Resource.Error(mapException(e)))
        }
    }

    /**
     * Registrasi akun baru (mandiri, perlu approval admin).
     */
    fun registrasi(request: RegisterRequest): Flow<Resource<UserDetailResponse>> = flow {
        emit(Resource.Loading)
        try {
            val deviceId = tokenManager.getOrCreateDeviceId()
            val response = api.registrasi(request.copy(deviceId = deviceId))

            if (response.isSuccessful && response.body()?.success == true) {
                emit(Resource.Success(response.body()!!.data!!))
            } else {
                emit(Resource.Error(response.body()?.error ?: "Registrasi gagal"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(mapException(e)))
        }
    }

    /**
     * Logout - hapus token dari server (blacklist) dan lokal.
     */
    fun logout(): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading)
        try {
            // Coba logout ke server (boleh gagal, tetap hapus token lokal)
            try {
                api.logout()
            } catch (_: Exception) { }

            tokenManager.clearSession()
            emit(Resource.Success(Unit))
        } catch (e: Exception) {
            // Tetap hapus session lokal walaupun ada error
            tokenManager.clearSession()
            emit(Resource.Success(Unit))
        }
    }

    suspend fun isLoggedIn(): Boolean = tokenManager.isLoggedIn()

    suspend fun getRole(): String? = tokenManager.roleFlow.first()

    private fun mapException(e: Exception): String {
        return when (e) {
            is java.net.UnknownHostException -> "Tidak dapat terhubung ke server. Periksa koneksi internet."
            is java.net.SocketTimeoutException -> "Koneksi timeout. Coba lagi."
            is java.io.IOException -> "Gagal terhubung ke server."
            else -> e.message ?: "Terjadi kesalahan tidak diketahui"
        }
    }
}