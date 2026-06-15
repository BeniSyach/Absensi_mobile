package com.dss.absensiKoas.ui.screen.absen

import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
    // Peringatan tambahan untuk ditampilkan ke user sebelum submit
    val peringatanLokasi: String? = null
)

@HiltViewModel
class AbsenViewModel @Inject constructor(
    private val absensiRepository: AbsensiRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AbsenUiState())
    val uiState: StateFlow<AbsenUiState> = _uiState.asStateFlow()

    /**
     * Muat status absen hari ini (sudah absen masuk/pulang atau belum).
     */
    fun muatStatusHariIni() {
        viewModelScope.launch {
            absensiRepository.getStatusHariIni().collect { result ->
                when (result) {
                    is Resource.Loading -> {
                        _uiState.value = _uiState.value.copy(isLoadingStatus = true)
                    }
                    is Resource.Success -> {
                        _uiState.value = _uiState.value.copy(
                            isLoadingStatus = false,
                            statusHariIni = result.data
                        )
                    }
                    is Resource.Error -> {
                        _uiState.value = _uiState.value.copy(isLoadingStatus = false)
                    }
                }
            }
        }
    }

    /**
     * Ambil lokasi GPS terkini dan cek apakah mock location terdeteksi.
     * Dipanggil sebelum user mengambil foto, agar lokasi sudah siap.
     */
    fun ambilLokasi() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoadingLokasi = true,
                errorMessage = null,
                peringatanLokasi = null
            )
            try {
                val location = absensiRepository.ambilLokasiSaatIni()
                val isMock = absensiRepository.cekMockLocation(location)

                val peringatan = when {
                    isMock -> "⚠️ Terdeteksi lokasi palsu (Mock Location/Fake GPS). Absen tetap bisa dikirim namun akan ditandai untuk diperiksa admin."
                    location.accuracy <= 0f -> "⚠️ Akurasi GPS tidak valid. Coba pindah ke area terbuka."
                    location.accuracy > 50f -> "Akurasi GPS rendah (${location.accuracy.toInt()}m). Untuk hasil lebih baik, coba di area terbuka."
                    else -> null
                }

                _uiState.value = _uiState.value.copy(
                    isLoadingLokasi = false,
                    lokasiSaatIni = location,
                    mockLocationTerdeteksi = isMock,
                    peringatanLokasi = peringatan
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoadingLokasi = false,
                    errorMessage = e.message ?: "Gagal mendapatkan lokasi. Pastikan GPS aktif dan izin lokasi diberikan."
                )
            }
        }
    }

    /**
     * Submit absen masuk dengan foto yang sudah diambil.
     */
    fun absenMasuk(fotoFile: File, catatan: String? = null) {
        val location = _uiState.value.lokasiSaatIni
        if (location == null) {
            _uiState.value = _uiState.value.copy(errorMessage = "Lokasi belum tersedia. Coba muat ulang lokasi.")
            return
        }

        viewModelScope.launch {
            val lokasiRequest = absensiRepository.toLokasiRequest(location)
            absensiRepository.absenMasuk(fotoFile, lokasiRequest, catatan).collect { result ->
                handleAbsenResult(result)
            }
        }
    }

    /**
     * Submit absen pulang dengan foto yang sudah diambil.
     */
    fun absenPulang(fotoFile: File, catatan: String? = null) {
        val location = _uiState.value.lokasiSaatIni
        if (location == null) {
            _uiState.value = _uiState.value.copy(errorMessage = "Lokasi belum tersedia. Coba muat ulang lokasi.")
            return
        }

        viewModelScope.launch {
            val lokasiRequest = absensiRepository.toLokasiRequest(location)
            absensiRepository.absenPulang(fotoFile, lokasiRequest, catatan).collect { result ->
                handleAbsenResult(result)
            }
        }
    }

    private fun handleAbsenResult(result: Resource<AbsenResponse>) {
        when (result) {
            is Resource.Loading -> {
                _uiState.value = _uiState.value.copy(isSubmitting = true, errorMessage = null)
            }
            is Resource.Success -> {
                _uiState.value = _uiState.value.copy(
                    isSubmitting = false,
                    absenResult = result.data
                )
            }
            is Resource.Error -> {
                _uiState.value = _uiState.value.copy(
                    isSubmitting = false,
                    errorMessage = result.message
                )
            }
        }
    }

    fun resetAbsenResult() {
        _uiState.value = _uiState.value.copy(absenResult = null)
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    /**
     * Hitung jarak dari kantor untuk preview di UI (info saja, validasi sebenarnya di server)
     */
    fun hitungJarakKeKantor(latKantor: Double, lonKantor: Double): Float? {
        val lokasi = _uiState.value.lokasiSaatIni ?: return null
        return absensiRepository.hitungJarak(lokasi.latitude, lokasi.longitude, latKantor, lonKantor)
    }
}