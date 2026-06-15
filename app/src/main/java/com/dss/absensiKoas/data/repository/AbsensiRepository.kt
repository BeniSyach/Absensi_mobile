package com.dss.absensiKoas.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.location.Location
import com.dss.absensiKoas.data.api.AbsensiApi
import com.dss.absensiKoas.data.model.*
import com.dss.absensiKoas.util.LocationHelper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AbsensiRepository @Inject constructor(
    private val api: AbsensiApi,
    private val locationHelper: LocationHelper,
    private val json: Json
) {

    /**
     * Ambil lokasi GPS terkini.
     * Throws exception jika lokasi tidak tersedia (misal GPS mati).
     */
    suspend fun ambilLokasiSaatIni(): Location {
        return locationHelper.getCurrentLocation()
            ?: throw IllegalStateException("Lokasi tidak tersedia. Pastikan GPS aktif.")
    }

    fun cekMockLocation(location: Location): Boolean = locationHelper.isMockLocation(location)

    fun toLokasiRequest(location: Location): LokasiRequest = locationHelper.toLokasiRequest(location)

    fun hitungJarak(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float =
        locationHelper.hitungJarak(lat1, lon1, lat2, lon2)

    /**
     * Absen masuk dengan foto dan data lokasi.
     * Foto akan dikompres sebelum dikirim untuk menghemat bandwidth.
     */
    fun absenMasuk(
        fotoFile: File,
        lokasi: LokasiRequest,
        catatan: String? = null
    ): Flow<Resource<AbsenResponse>> = flow {
        emit(Resource.Loading)
        try {
            val fotoPart = buildFotoPart(fotoFile, "foto")
            val dataJson = json.encodeToString(AbsenRequestData(lokasi = lokasi, catatan = catatan))
            val dataPart = dataJson.toRequestBody("application/json".toMediaType())

            val response = api.absenMasuk(fotoPart, dataPart)

            if (response.isSuccessful && response.body()?.success == true) {
                emit(Resource.Success(response.body()!!.data!!))
            } else {
                emit(Resource.Error(response.body()?.error ?: parseErrorBody(response.errorBody()?.string())))
            }
        } catch (e: Exception) {
            emit(Resource.Error(mapException(e)))
        }
    }

    /**
     * Absen pulang dengan foto dan data lokasi.
     */
    fun absenPulang(
        fotoFile: File,
        lokasi: LokasiRequest,
        catatan: String? = null
    ): Flow<Resource<AbsenResponse>> = flow {
        emit(Resource.Loading)
        try {
            val fotoPart = buildFotoPart(fotoFile, "foto")
            val dataJson = json.encodeToString(AbsenRequestData(lokasi = lokasi, catatan = catatan))
            val dataPart = dataJson.toRequestBody("application/json".toMediaType())

            val response = api.absenPulang(fotoPart, dataPart)

            if (response.isSuccessful && response.body()?.success == true) {
                emit(Resource.Success(response.body()!!.data!!))
            } else {
                emit(Resource.Error(response.body()?.error ?: parseErrorBody(response.errorBody()?.string())))
            }
        } catch (e: Exception) {
            emit(Resource.Error(mapException(e)))
        }
    }

    /**
     * Ambil status absen hari ini (sudah masuk/pulang atau belum).
     */
    fun getStatusHariIni(): Flow<Resource<StatusHariIniResponse>> = flow {
        emit(Resource.Loading)
        try {
            val response = api.statusHariIni()
            if (response.isSuccessful && response.body()?.success == true) {
                emit(Resource.Success(response.body()!!.data!!))
            } else {
                emit(Resource.Error(response.body()?.error ?: "Gagal memuat status"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(mapException(e)))
        }
    }

    fun getRiwayatMasuk(dari: String, sampai: String): Flow<Resource<List<AbsenRiwayatItem>>> = flow {
        emit(Resource.Loading)
        try {
            val response = api.riwayatAbsenMasuk(dari, sampai)
            if (response.isSuccessful && response.body()?.success == true) {
                emit(Resource.Success(response.body()!!.data ?: emptyList()))
            } else {
                emit(Resource.Error(response.body()?.error ?: "Gagal memuat riwayat"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(mapException(e)))
        }
    }

    fun getRiwayatPulang(dari: String, sampai: String): Flow<Resource<List<AbsenRiwayatItem>>> = flow {
        emit(Resource.Loading)
        try {
            val response = api.riwayatAbsenPulang(dari, sampai)
            if (response.isSuccessful && response.body()?.success == true) {
                emit(Resource.Success(response.body()!!.data ?: emptyList()))
            } else {
                emit(Resource.Error(response.body()?.error ?: "Gagal memuat riwayat"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(mapException(e)))
        }
    }

    /**
     * Kompres foto sebelum dikirim (resize max 1024px, kualitas 80%)
     * untuk hemat bandwidth saat upload massal jam absen.
     */
    private fun buildFotoPart(fotoFile: File, partName: String): MultipartBody.Part {
        val compressedBytes = compressImage(fotoFile, maxDimension = 1024, quality = 80)
        val requestBody = compressedBytes.toRequestBody("image/jpeg".toMediaType())
        return MultipartBody.Part.createFormData(partName, "absen_${System.currentTimeMillis()}.jpg", requestBody)
    }

    private fun compressImage(file: File, maxDimension: Int, quality: Int): ByteArray {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, options)

        var scale = 1
        while (options.outWidth / scale > maxDimension || options.outHeight / scale > maxDimension) {
            scale *= 2
        }

        val decodeOptions = BitmapFactory.Options().apply { inSampleSize = scale }
        val bitmap = BitmapFactory.decodeFile(file.absolutePath, decodeOptions)

        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
        bitmap.recycle()
        return outputStream.toByteArray()
    }

    private fun parseErrorBody(errorBody: String?): String {
        if (errorBody == null) return "Terjadi kesalahan"
        return try {
            val errorResponse = json.decodeFromString<ApiResponse<Unit>>(errorBody)
            errorResponse.error ?: "Terjadi kesalahan"
        } catch (e: Exception) {
            "Terjadi kesalahan"
        }
    }

    private fun mapException(e: Exception): String {
        return when (e) {
            is java.net.UnknownHostException -> "Tidak dapat terhubung ke server. Periksa koneksi internet."
            is java.net.SocketTimeoutException -> "Koneksi timeout. Coba lagi."
            is java.io.IOException -> "Gagal terhubung ke server."
            else -> e.message ?: "Terjadi kesalahan tidak diketahui"
        }
    }
}