package com.dss.absensiKoas.data.api

import com.dss.absensiKoas.data.model.*
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*

interface AbsensiApi {

    // ============================
    // AUTH
    // ============================

    @POST("api/v1/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<ApiResponse<LoginResponse>>

    @POST("api/v1/auth/logout")
    suspend fun logout(): Response<ApiResponse<Unit>>

    @POST("api/v1/auth/refresh")
    suspend fun refreshToken(
        @Header("X-Refresh-Token") refreshToken: String
    ): Response<ApiResponse<LoginResponse>>

    // ── Shift: pegawai ambil daftar shift OPD-nya ────────────────
    /**
     * Endpoint baru: GET /api/v1/absensi/shift/available
     * Mengembalikan shift aktif dari OPD user.
     * Tidak perlu parameter — server ambil dari JWT.
     */
    @GET("api/v1/absensi/shift/available")
    suspend fun getDaftarShift(): Response<ApiResponse<List<ShiftResponse>>>

    // ============================
    // REGISTRASI & PROFIL USER
    // ============================

    @POST("api/v1/user/registrasi")
    suspend fun registrasi(@Body request: RegisterRequest): Response<ApiResponse<UserDetailResponse>>

    @GET("api/v1/user/profil")
    suspend fun getProfil(): Response<ApiResponse<UserDetailResponse>>

    @PUT("api/v1/user/profil")
    suspend fun updateProfil(@Body request: UpdateProfilRequest): Response<ApiResponse<UserDetailResponse>>

    @PUT("api/v1/user/ganti-password")
    suspend fun gantiPassword(@Body request: GantiPasswordRequest): Response<ApiResponse<Unit>>

    @Multipart
    @POST("api/v1/user/foto-profil")
    suspend fun uploadFotoProfil(
        @Part foto: MultipartBody.Part
    ): Response<ApiResponse<UserDetailResponse>>

    @PUT("api/v1/user/device-id")
    suspend fun updateDeviceId(@Query("deviceId") deviceId: String): Response<ApiResponse<Unit>>

    // ============================
    // ABSENSI
    // ============================

    @Multipart
    @POST("api/v1/absensi/masuk")
    suspend fun absenMasuk(
        @Part foto: MultipartBody.Part,
        @Part("data") data: RequestBody
    ): Response<ApiResponse<AbsenResponse>>

    @Multipart
    @POST("api/v1/absensi/pulang")
    suspend fun absenPulang(
        @Part foto: MultipartBody.Part,
        @Part("data") data: RequestBody
    ): Response<ApiResponse<AbsenResponse>>

    @GET("api/v1/absensi/status/hari-ini")
    suspend fun statusHariIni(): Response<ApiResponse<StatusHariIniResponse>>

    @GET("api/v1/absensi/riwayat/masuk")
    suspend fun riwayatAbsenMasuk(
        @Query("dari") dari: String,
        @Query("sampai") sampai: String
    ): Response<ApiResponse<List<AbsenRiwayatItem>>>

    @GET("api/v1/absensi/riwayat/pulang")
    suspend fun riwayatAbsenPulang(
        @Query("dari") dari: String,
        @Query("sampai") sampai: String
    ): Response<ApiResponse<List<AbsenRiwayatItem>>>
}