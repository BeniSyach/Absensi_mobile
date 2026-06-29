package com.dss.absensiKoas.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LoginRequest(
    val username: String,
    val password: String,
    val deviceId: String? = null,
    val deviceInfo: String? = null
)

@Serializable
data class LokasiRequest(
    val latitude: Double,
    val longitude: Double,
    val akurasiGps: Float? = null,
    val locationProvider: String? = null,
    val isMockLocation: Boolean? = null,
    val altitude: Double? = null,
    val bearing: Float? = null,
    val speed: Float? = null
)

@Serializable
data class AbsenRequestData(
    val shiftId: Long? = null,
    val lokasi: LokasiRequest,
    val catatan: String? = null
)

@Serializable
data class RegisterRequest(
    val nip: String,
    val username: String,
    val password: String,
    val konfirmasiPassword: String,
    val namaLengkap: String,
    val email: String? = null,
    val telepon: String? = null,
    val opdId: Long,
    val deviceId: String? = null
)

@Serializable
data class GantiPasswordRequest(
    val passwordLama: String,
    val passwordBaru: String,
    val konfirmasiPasswordBaru: String
)

@Serializable
data class UpdateProfilRequest(
    val namaLengkap: String,
    val email: String? = null,
    val telepon: String? = null
)