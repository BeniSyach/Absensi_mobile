package com.dss.absensiKoas.data.local

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Event bus global untuk sinyal sesi habis.
 *
 * Dipakai oleh TokenAuthenticator (layer network, bukan Compose)
 * untuk memberi tahu UI bahwa sesi sudah tidak valid lagi —
 * baik karena token expired dan refresh token GAGAL, atau refresh token
 * sendiri sudah expired.
 *
 * UI (MainActivity) mengamati [sessionExpiredEvent] dan menampilkan
 * dialog "Sesi Anda telah habis" lalu navigasi ke Login.
 */
@Singleton
class SessionManager @Inject constructor() {

    // replay = 0 agar event lama tidak diputar ulang ke collector baru
    private val _sessionExpiredEvent = MutableSharedFlow<Unit>(
        replay = 0,
        extraBufferCapacity = 1
    )
    val sessionExpiredEvent: SharedFlow<Unit> = _sessionExpiredEvent.asSharedFlow()

    /**
     * Dipanggil dari layer network (OkHttp Authenticator)
     * saat token sudah tidak valid dan refresh gagal.
     *
     * tryEmit aman dipanggil dari thread OkHttp (bukan main thread),
     * tidak butuh suspend.
     */
    fun notifySessionExpired() {
        _sessionExpiredEvent.tryEmit(Unit)
    }
}