package com.dss.absensiKoas.data.repository

/**
 * Wrapper generic untuk hasil operasi async (sukses, gagal, loading).
 */
sealed class Resource<out T> {
    data class Success<out T>(val data: T) : Resource<T>()
    data class Error(val message: String) : Resource<Nothing>()
    object Loading : Resource<Nothing>()
}