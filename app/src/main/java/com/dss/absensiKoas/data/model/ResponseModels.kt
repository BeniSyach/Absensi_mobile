package com.dss.absensiKoas.data.model

import kotlinx.serialization.Serializable

@Serializable
data class ApiResponse<T>(
    val success: Boolean,
    val message: String? = null,
    val data: T? = null,
    val error: String? = null,
    val timestamp: String? = null
)

@Serializable
data class LoginResponse(
    val accessToken: String,
    val refreshToken: String? = null,
    val tokenType: String,
    val expiresIn: Long,
    val user: UserResponse
)

@Serializable
data class UserResponse(
    val id: Long,
    val nip: String,
    val username: String,
    val namaLengkap: String,
    val email: String? = null,
    val telepon: String? = null,
    val fotoProfil: String? = null,
    val role: String,
    val opd: OpdResponse? = null
)

@Serializable
data class OpdResponse(
    val id: Long,
    val kode: String,
    val nama: String,
    val alamat: String? = null,
    val latitudeKantor: Double,
    val longitudeKantor: Double,
    val radiusAbsen: Int
)

@Serializable
data class ShiftResponse(
    val id: Long,
    val nama: String,
    val jamMasuk: String,
    val jamPulang: String,
    val toleransiTerlambat: Int? = null,
    val toleransiPulangAwal: Int? = null,
    val hariKerja: Set<String>? = null
)

@Serializable
data class UserDetailResponse(
    val id: Long,
    val nip: String,
    val username: String,
    val namaLengkap: String,
    val email: String? = null,
    val telepon: String? = null,
    val fotoProfil: String? = null,
    val role: String,
    val aktif: Boolean? = null,
    val deviceId: String? = null,
    val opd: OpdResponse? = null,
    val shiftAktif: ShiftResponse? = null
)

@Serializable
data class AbsenResponse(
    val id: Long,
    val jenis: String,
    val waktu: String,
    val latitude: Double,
    val longitude: Double,
    val jarakDariKantor: Double? = null,
    val lokasiValid: Boolean? = null,
    val mockLocationDetected: Boolean? = null,
    val fotoAbsen: String? = null,
    val status: String,
    val pesan: String? = null,
    val durasiKerjaMenit: Int? = null
)

@Serializable
data class StatusHariIniResponse(
    val tanggal: String,
    val sudahAbsenMasuk: Boolean,
    val sudahAbsenPulang: Boolean,
    val waktuMasuk: String? = null,
    val statusMasuk: String? = null,
    val waktuPulang: String? = null,
    val statusPulang: String? = null,
    val durasiKerjaMenit: Int? = null
)

@Serializable
data class AbsenRiwayatItem(
    val id: Long,
    val jenis: String,
    val waktu: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val jarakDariKantor: Double? = null,
    val lokasiValid: Boolean? = null,
    val mockLocationDetected: Boolean? = null,
    val fotoAbsen: String? = null,
    val status: String? = null,
    val durasiKerjaMenit: Int? = null
)