package com.dss.absensiKoas.ui.screen.absen

import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dss.absensiKoas.data.local.TokenManager
import com.dss.absensiKoas.data.model.AbsenResponse
import com.dss.absensiKoas.data.model.StatusHariIniResponse
import com.dss.absensiKoas.data.repository.AbsensiRepository
import com.dss.absensiKoas.data.repository.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

enum class JenisAbsen { MASUK, PULANG }

data class AbsenUiState(
    val isLoadingLokasi: Boolean = false,
    val isSubmitting: Boolean = false,
    val lokasiSaatIni: Location? = null,
    val mockLocationTerdeteksi: Boolean = false,
    val errorMessage: String? = null,
    val absenResult: AbsenResponse? = null,
    val statusHariIni: StatusHariIniResponse? = null,
    val isLoadingStatus: Boolean = false,

    // === DATA OPD KANTOR ===
    val namaOpd: String? = null,
    val latKantor: Double? = null,
    val lonKantor: Double? = null,
    val radiusKantor: Int = 100,              // meter, default 100m

    // === HASIL VALIDASI RADIUS ===
    val jarakKeKantor: Float? = null,         // dalam meter
    val dalamRadius: Boolean? = null,         // null = belum dicek, true/false = hasil cek
    val pesanLokasi: String? = null           // pesan informatif untuk user
)

@HiltViewModel
class AbsenViewModel @Inject constructor(
    private val absensiRepository: AbsensiRepository,
    private val tokenManager: TokenManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(AbsenUiState())
    val uiState: StateFlow<AbsenUiState> = _uiState.asStateFlow()

    init {
        muatDataOpdDariCache()
    }

    /**
     * Muat koordinat kantor & radius dari DataStore (tersimpan saat login).
     * Tidak perlu request API — data sudah ada di lokal.
     */
    private fun muatDataOpdDariCache() {
        viewModelScope.launch {
            val lat    = tokenManager.getOpdLatKantor()
            val lon    = tokenManager.getOpdLonKantor()
            val radius = tokenManager.getOpdRadiusAbsen()
            val nama   = tokenManager.getOpdNama()

            _uiState.value = _uiState.value.copy(
                latKantor  = lat,
                lonKantor  = lon,
                radiusKantor = radius,
                namaOpd    = nama
            )
        }
    }

    /**
     * Ambil lokasi GPS terkini, lalu hitung jarak ke kantor secara otomatis.
     */
    fun ambilLokasi() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoadingLokasi = true,
                errorMessage    = null,
                pesanLokasi     = null,
                dalamRadius     = null,
                jarakKeKantor   = null
            )
            try {
                val location = absensiRepository.ambilLokasiSaatIni()
                val isMock   = absensiRepository.cekMockLocation(location)

                // Hitung jarak ke kantor (jika koordinat kantor tersedia)
                val lat    = _uiState.value.latKantor
                val lon    = _uiState.value.lonKantor
                val radius = _uiState.value.radiusKantor

                val jarak: Float?
                val dalamRadius: Boolean?
                val pesanLokasi: String?

                if (lat != null && lon != null) {
                    jarak = absensiRepository.hitungJarak(
                        location.latitude, location.longitude, lat, lon
                    )
                    dalamRadius = jarak <= radius

                    pesanLokasi = when {
                        isMock      -> "⚠️ Fake GPS terdeteksi! Absen akan ditandai dan diperiksa admin."
                        !dalamRadius -> "📍 Anda berada ${formatJarak(jarak)} dari kantor.\n" +
                                "Radius absen: ${radius}m. Silakan menuju area kantor."
                        location.accuracy > 50f ->
                            "📶 Akurasi GPS rendah (${location.accuracy.toInt()}m). Coba di area terbuka."
                        else        -> "✅ Anda berada dalam radius kantor (${formatJarak(jarak)})"
                    }
                } else {
                    // Koordinat kantor belum ada — tidak bisa validasi
                    jarak       = null
                    dalamRadius = null
                    pesanLokasi = if (isMock)
                        "⚠️ Fake GPS terdeteksi!"
                    else
                        "📍 Lokasi didapat. Validasi radius akan dilakukan server."
                }

                _uiState.value = _uiState.value.copy(
                    isLoadingLokasi       = false,
                    lokasiSaatIni         = location,
                    mockLocationTerdeteksi = isMock,
                    jarakKeKantor         = jarak,
                    dalamRadius           = dalamRadius,
                    pesanLokasi           = pesanLokasi
                )

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoadingLokasi = false,
                    errorMessage    = e.message
                        ?: "Gagal mendapatkan lokasi. Pastikan GPS aktif."
                )
            }
        }
    }

    /**
     * Submit absen masuk.
     * Blokir jika:
     *  1. Lokasi belum ada
     *  2. Di luar radius kantor (dalamRadius == false)
     *  3. Mock location terdeteksi → tetap BLOKIR (tidak boleh absen dari lokasi palsu)
     */
    fun absenMasuk(fotoFile: File, catatan: String? = null) {
        if (!validasiSebelumSubmit()) return

        viewModelScope.launch {
            val lokasiRequest = absensiRepository.toLokasiRequest(_uiState.value.lokasiSaatIni!!)
            absensiRepository.absenMasuk(fotoFile, lokasiRequest, catatan)
                .collect { handleAbsenResult(it) }
        }
    }

    /**
     * Submit absen pulang.
     * Sama — blokir jika di luar radius atau mock location.
     */
    fun absenPulang(fotoFile: File, catatan: String? = null) {
        if (!validasiSebelumSubmit()) return

        viewModelScope.launch {
            val lokasiRequest = absensiRepository.toLokasiRequest(_uiState.value.lokasiSaatIni!!)
            absensiRepository.absenPulang(fotoFile, lokasiRequest, catatan)
                .collect { handleAbsenResult(it) }
        }
    }

    /**
     * Validasi terpusat sebelum submit.
     * Return true = boleh lanjut, false = ditolak.
     */
    private fun validasiSebelumSubmit(): Boolean {
        val state = _uiState.value

        if (state.lokasiSaatIni == null) {
            _uiState.value = state.copy(
                errorMessage = "Lokasi belum tersedia. Tekan tombol refresh GPS."
            )
            return false
        }

        // Blokir jika mock location terdeteksi
        if (state.mockLocationTerdeteksi) {
            _uiState.value = state.copy(
                errorMessage = "🚫 Absen ditolak: Fake GPS / Mock Location terdeteksi pada perangkat Anda.\n" +
                        "Nonaktifkan aplikasi fake GPS dan coba lagi."
            )
            return false
        }

        // Blokir jika sudah tahu di luar radius
        if (state.dalamRadius == false) {
            val jarak = state.jarakKeKantor
            val radius = state.radiusKantor
            _uiState.value = state.copy(
                errorMessage = "🚫 Absen ditolak: Anda berada ${jarak?.let { formatJarak(it) } ?: "jauh"} " +
                        "dari kantor.\nRadius absen maksimal ${radius}m. " +
                        "Silakan menuju area kantor terlebih dahulu."
            )
            return false
        }

        return true
    }

    private fun handleAbsenResult(result: Resource<AbsenResponse>) {
        when (result) {
            is Resource.Loading -> {
                _uiState.value = _uiState.value.copy(isSubmitting = true, errorMessage = null)
            }
            is Resource.Success -> {
                _uiState.value = _uiState.value.copy(isSubmitting = false, absenResult = result.data)
            }
            is Resource.Error -> {
                _uiState.value = _uiState.value.copy(isSubmitting = false, errorMessage = result.message)
            }
        }
    }

    fun resetAbsenResult() {
        _uiState.value = _uiState.value.copy(absenResult = null)
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    private fun formatJarak(meter: Float): String =
        if (meter >= 1000) String.format("%.1f km", meter / 1000)
        else "${meter.toInt()} meter"
}