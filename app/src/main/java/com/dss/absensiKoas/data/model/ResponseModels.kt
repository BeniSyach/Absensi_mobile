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

// ── Shift ─────────────────────────────────────────────────
@Serializable
data class ShiftResponse(

    val id: Int,

    val nama: String,

    val aktif: Boolean,

    val opdId: Int?,

    val namaOpd: String?,

    val waktuKerja: List<WaktuKerjaResponse>

)


@Serializable
data class WaktuKerjaResponse(

    val id: Int,

    val hari: String,

    val jamMasuk: String,

    val jamPulang: String,

    val toleransiTerlambat: Int,

    val toleransiPulangAwal: Int,

    val lintasHari: Boolean,

    val aktif: Boolean

){
    fun emoji(): String = when {
        jamMasuk >= "05:00" && jamMasuk < "11:00" -> "🌅"
        jamMasuk >= "11:00" && jamMasuk < "17:00" -> "☀️"
        else -> "🌙"
    }



    fun labelLengkap(): String {
        val suffix = if (lintasHari == true) " (lintas hari 🌙)" else ""
        return "$hari · $jamMasuk – $jamPulang$suffix"
    }
}
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
    val shift: ShiftResponse? = null
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
    val durasiKerjaMenit: Int? = null,
    val shiftId: Long? = null,
    val shiftNama: String? = null,
    val shiftLintasHari: Boolean? = null
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
    val durasiKerjaMenit: Int? = null,
    val shiftId: Long? = null,
    val shiftNama: String? = null,
    val shiftLintasHari: Boolean? = null
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
    val durasiKerjaMenit: Int? = null,
    val pesan: String? = null,
    val tanggal: String? = null
)