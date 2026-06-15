package com.dss.absensiKoas.util

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.os.Build
import com.dss.absensiKoas.data.model.LokasiRequest
import com.google.android.gms.location.*
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Helper untuk mendapatkan lokasi GPS akurat dan mendeteksi mock location.
 *
 * STRATEGI ANTI-FAKE-GPS:
 * 1. Cek Location.isFromMockProvider() - flag resmi dari Android
 * 2. Gunakan FusedLocationProvider dengan PRIORITY_HIGH_ACCURACY (GPS asli)
 * 3. Cek apakah Developer Options / Mock Location aktif di sistem
 * 4. Validasi akurasi GPS (akurasi 0 atau negatif = mencurigakan)
 */
@Singleton
class LocationHelper @Inject constructor(
    private val context: Context
) {
    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    /**
     * Ambil lokasi terkini dengan akurasi tinggi.
     * Timeout 15 detik agar tidak menggantung jika GPS lemah.
     */
    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(): Location? = suspendCancellableCoroutine { cont ->
        val cancellationTokenSource = CancellationTokenSource()

        val request = CurrentLocationRequest.Builder()
            .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
            .setMaxUpdateAgeMillis(0) // Selalu ambil lokasi baru, jangan cache
            .setDurationMillis(15_000)
            .build()

        fusedLocationClient.getCurrentLocation(request, cancellationTokenSource.token)
            .addOnSuccessListener { location ->
                cont.resume(location)
            }
            .addOnFailureListener { e ->
                cont.resumeWithException(e)
            }

        cont.invokeOnCancellation {
            cancellationTokenSource.cancel()
        }
    }

    /**
     * Cek apakah lokasi terdeteksi sebagai mock location (fake GPS).
     * Method isFromMockProvider() tersedia mulai API 18.
     */
    fun isMockLocation(location: Location): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            location.isMock
        } else {
            @Suppress("DEPRECATION")
            location.isFromMockProvider
        }
    }

    /**
     * Cek apakah developer sedang mengaktifkan Mock Location di Developer Options.
     * Ini deteksi tambahan di level sistem (selain flag per-Location).
     */
    fun isMockLocationEnabledGlobally(): Boolean {
        return try {
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            // Cek provider "test" yang biasa dipakai aplikasi fake GPS
            locationManager.allProviders.any { provider ->
                try {
                    val testLocation = locationManager.getLastKnownLocation(provider)
                    testLocation != null && isMockLocation(testLocation)
                } catch (e: SecurityException) {
                    false
                }
            }
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Konversi Location Android menjadi LokasiRequest untuk dikirim ke API.
     * Menyertakan semua informasi yang dibutuhkan server untuk validasi anti-fake-GPS.
     */
    fun toLokasiRequest(location: Location): LokasiRequest {
        return LokasiRequest(
            latitude = location.latitude,
            longitude = location.longitude,
            akurasiGps = if (location.hasAccuracy()) location.accuracy else null,
            locationProvider = location.provider ?: "fused",
            isMockLocation = isMockLocation(location) || isMockLocationEnabledGlobally(),
            altitude = if (location.hasAltitude()) location.altitude else null,
            bearing = if (location.hasBearing()) location.bearing else null,
            speed = if (location.hasSpeed()) location.speed else null
        )
    }

    /**
     * Hitung jarak antara 2 koordinat (meter) - untuk preview di UI sebelum kirim ke server
     */
    fun hitungJarak(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
        val result = FloatArray(1)
        Location.distanceBetween(lat1, lon1, lat2, lon2, result)
        return result[0]
    }
}