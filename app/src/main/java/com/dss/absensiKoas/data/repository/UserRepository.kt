package com.dss.absensiKoas.data.repository

import com.dss.absensiKoas.data.api.AbsensiApi
import com.dss.absensiKoas.data.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepository @Inject constructor(
    private val api: AbsensiApi
) {

    fun getProfil(): Flow<Resource<UserDetailResponse>> = flow {
        emit(Resource.Loading)
        try {
            val response = api.getProfil()
            if (response.isSuccessful && response.body()?.success == true) {
                emit(Resource.Success(response.body()!!.data!!))
            } else {
                emit(Resource.Error(response.body()?.error ?: "Gagal memuat profil"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(mapException(e)))
        }
    }

    fun updateProfil(request: UpdateProfilRequest): Flow<Resource<UserDetailResponse>> = flow {
        emit(Resource.Loading)
        try {
            val response = api.updateProfil(request)
            if (response.isSuccessful && response.body()?.success == true) {
                emit(Resource.Success(response.body()!!.data!!))
            } else {
                emit(Resource.Error(response.body()?.error ?: "Gagal update profil"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(mapException(e)))
        }
    }

    fun gantiPassword(request: GantiPasswordRequest): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading)
        try {
            val response = api.gantiPassword(request)
            if (response.isSuccessful && response.body()?.success == true) {
                emit(Resource.Success(Unit))
            } else {
                emit(Resource.Error(response.body()?.error ?: "Gagal mengubah password"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(mapException(e)))
        }
    }

    fun uploadFotoProfil(fotoFile: File): Flow<Resource<UserDetailResponse>> = flow {
        emit(Resource.Loading)
        try {
            val requestBody = fotoFile.asRequestBody("image/jpeg".toMediaType())
            val part = MultipartBody.Part.createFormData("foto", fotoFile.name, requestBody)

            val response = api.uploadFotoProfil(part)
            if (response.isSuccessful && response.body()?.success == true) {
                emit(Resource.Success(response.body()!!.data!!))
            } else {
                emit(Resource.Error(response.body()?.error ?: "Gagal upload foto"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(mapException(e)))
        }
    }

    private fun mapException(e: Exception): String {
        return when (e) {
            is java.net.UnknownHostException -> "Tidak dapat terhubung ke server."
            is java.net.SocketTimeoutException -> "Koneksi timeout."
            is java.io.IOException -> "Gagal terhubung ke server."
            else -> e.message ?: "Terjadi kesalahan tidak diketahui"
        }
    }
}