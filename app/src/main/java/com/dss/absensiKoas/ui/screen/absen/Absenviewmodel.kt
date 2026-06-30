package com.dss.absensiKoas.ui.screen.absen

import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dss.absensiKoas.data.local.TokenManager
import com.dss.absensiKoas.data.model.AbsenResponse
import com.dss.absensiKoas.data.model.ShiftResponse
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

/**
 * State machine alur absen:
 * IDLE → PILIH_SHIFT → AMBIL_LOKASI → AMBIL_FOTO → KONFIRMASI → SUBMIT → SUKSES
 */
enum class AbsenStep {
    PILIH_SHIFT,      // Step 1: user pilih shift (hanya untuk MASUK)
    AMBIL_LOKASI,     // Step 2: ambil GPS
    AMBIL_FOTO,       // Step 3: ambil foto selfie
    KONFIRMASI,       // Step 4: review sebelum submit
    SUBMIT,           // Step 5: sedang upload ke server
    SUKSES            // Step 6: berhasil
}

data class AbsenUiState(
// ── Step saat ini ──
    val step: AbsenStep = AbsenStep.PILIH_SHIFT,

// ── Data shift ──
    val isLoadingShift: Boolean = false,
    val daftarShift: List<ShiftResponse> = emptyList(),
    val shiftDipilih: ShiftResponse? = null,     // shift yang dipilih user

// ── Data lokasi ──
    val isLoadingLokasi: Boolean = false,
    val lokasiSaatIni: Location? = null,
    val mockLocationTerdeteksi: Boolean = false,
    val jarakKeKantor: Float? = null,
    val dalamRadius: Boolean? = null,
    val pesanLokasi: String? = null,

// ── Data kantor (dari cache login) ──
    val namaOpd: String? = null,
    val latKantor: Double? = null,
    val lonKantor: Double? = null,
    val radiusKantor: Int = 100,

// ── Foto ──
    val fotoFile: File? = null,

// ── Submit ──
    val isSubmitting: Boolean = false,
    val absenResult: AbsenResponse? = null,

// ── Status hari ini ──
    val statusHariIni: StatusHariIniResponse? = null,

// ── Error ──
    val errorMessage: String? = null,
    val catatan: String = ""
)

@HiltViewModel
class AbsenViewModel @Inject constructor(
    private val repository: AbsensiRepository,
    private val tokenManager: TokenManager
) : ViewModel() {

    private val _state = MutableStateFlow(AbsenUiState())
    val state: StateFlow<AbsenUiState> = _state.asStateFlow()

    init {
        muatDataOpdDanShift()
        muatStatusHariIni()
    }

    // ─────────────────────────────────────────────────────────────
    // INIT: muat data OPD dari cache dan daftar shift dari API
    // ─────────────────────────────────────────────────────────────

    private fun muatDataOpdDanShift() {
        viewModelScope.launch {
            // Koordinat kantor dari DataStore (tersimpan saat login)
            val lat    = tokenManager.getOpdLatKantor()
            val lon    = tokenManager.getOpdLonKantor()
            val radius = tokenManager.getOpdRadiusAbsen()
            val nama   = tokenManager.getOpdNama()

            _state.value = _state.value.copy(
                latKantor    = lat,
                lonKantor    = lon,
                radiusKantor = radius,
                namaOpd      = nama
            )

            // Muat daftar shift dari API
            muatDaftarShift()
        }
    }

    fun muatDaftarShift() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoadingShift = true, errorMessage = null)
            repository.getDaftarShift().collect { result ->
                when (result) {
                    is Resource.Loading -> _state.value = _state.value.copy(isLoadingShift = true)
                    is Resource.Success -> _state.value = _state.value.copy(
                        isLoadingShift = false,
                        daftarShift    = result.data
                    )
                    is Resource.Error   -> _state.value = _state.value.copy(
                        isLoadingShift = false,
                        errorMessage   = result.message
                    )
                }
            }
        }
    }

    fun muatStatusHariIni() {
        viewModelScope.launch {
            repository.getStatusHariIni().collect { result ->
                if (result is Resource.Success) {
                    _state.value = _state.value.copy(statusHariIni = result.data)
                }
            }
        }
    }

    // ─────────────────────────────────────────────────────────────
    // STEP 1: Pilih shift (hanya absen masuk)
    // Absen pulang langsung ke step lokasi
    // ─────────────────────────────────────────────────────────────

    fun pilihShift(shift: ShiftResponse) {
        _state.value = _state.value.copy(
            shiftDipilih = shift,
            errorMessage = null
        )
        android.util.Log.d("SHIFT", "Dipilih = ${_state.value.shiftDipilih}")
    }

    fun lanjutDariPilihShift(jenisAbsen: JenisAbsen) {
        android.util.Log.d(
            "SHIFT",
            "Sebelum lanjut = ${_state.value.shiftDipilih}"
        )
        if (jenisAbsen == JenisAbsen.MASUK && _state.value.shiftDipilih == null) {
            _state.value = _state.value.copy(errorMessage = "Pilih shift terlebih dahulu")
            return
        }
        // Langsung ambil lokasi di background, user ke step lokasi
        _state.value = _state.value.copy(step = AbsenStep.AMBIL_LOKASI)
        ambilLokasi()
    }

    /** Absen pulang: skip step pilih shift langsung ke lokasi */
    fun mulaiAbsenPulang() {
        _state.value = _state.value.copy(step = AbsenStep.AMBIL_LOKASI)
        ambilLokasi()
    }

    // ─────────────────────────────────────────────────────────────
    // STEP 2: Ambil lokasi GPS
    // ─────────────────────────────────────────────────────────────

    fun ambilLokasi() {
        viewModelScope.launch {
            _state.value = _state.value.copy(
                isLoadingLokasi = true,
                errorMessage    = null,
                dalamRadius     = null,
                jarakKeKantor   = null,
                pesanLokasi     = null
            )
            try {
                val location = repository.ambilLokasiSaatIni()
                val isMock   = repository.cekMockLocation(location)

                val lat    = _state.value.latKantor
                val lon    = _state.value.lonKantor
                val radius = _state.value.radiusKantor

                val jarak: Float?
                val dalamRadius: Boolean?
                val pesan: String?

                if (lat != null && lon != null) {
                    jarak = repository.hitungJarak(location.latitude, location.longitude, lat, lon)
                    dalamRadius = jarak <= radius

                    pesan = when {
                        isMock -> "⚠️ Fake GPS terdeteksi! Absen tidak bisa dilanjutkan."
                        !dalamRadius -> "📍 Anda ${formatJarak(jarak)} dari kantor.\nRadius absen: ${radius}m."
                        location.accuracy > 50f -> "📶 Akurasi GPS rendah (${location.accuracy.toInt()}m)."
                        else -> "✅ Dalam radius kantor (${formatJarak(jarak)})"
                    }
                } else {
                    jarak = null; dalamRadius = null
                    pesan = if (isMock) "⚠️ Fake GPS terdeteksi!" else "📍 Lokasi didapat."
                }

                _state.value = _state.value.copy(
                    isLoadingLokasi        = false,
                    lokasiSaatIni          = location,
                    mockLocationTerdeteksi = isMock,
                    jarakKeKantor          = jarak,
                    dalamRadius            = dalamRadius,
                    pesanLokasi            = pesan
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoadingLokasi = false,
                    errorMessage    = e.message ?: "Gagal mendapatkan lokasi. Pastikan GPS aktif."
                )
            }
        }
    }

    fun lanjutKeAmbilFoto() {
        val s = _state.value
        when {
            s.lokasiSaatIni == null -> {
                _state.value = s.copy(errorMessage = "Lokasi belum tersedia. Tekan refresh GPS.")
                return
            }
            s.mockLocationTerdeteksi -> {
                _state.value = s.copy(
                    errorMessage = "🚫 Fake GPS terdeteksi! Nonaktifkan aplikasi fake GPS terlebih dahulu."
                )
                return
            }
            s.dalamRadius == false -> {
                _state.value = s.copy(
                    errorMessage = "🚫 Anda berada di luar radius kantor (${formatJarak(s.jarakKeKantor)}).\n" +
                            "Silakan menuju area kantor terlebih dahulu."
                )
                return
            }
        }
        _state.value = s.copy(step = AbsenStep.AMBIL_FOTO, errorMessage = null)
    }

    // ─────────────────────────────────────────────────────────────
    // STEP 3: Foto diambil dari kamera
    // ─────────────────────────────────────────────────────────────

    fun onFotoDidambil(file: File) {
        _state.value = _state.value.copy(
            fotoFile = file,
            step     = AbsenStep.KONFIRMASI,
            errorMessage = null
        )
    }

    fun ulangiAmbilFoto() {
        _state.value = _state.value.copy(
            fotoFile = null,
            step     = AbsenStep.AMBIL_FOTO
        )
    }

    // ─────────────────────────────────────────────────────────────
    // STEP 4: Konfirmasi — user review semua data
    // ─────────────────────────────────────────────────────────────

    fun setCatatan(catatan: String) {
        _state.value = _state.value.copy(catatan = catatan)
    }

    fun kembaliKeLokasi() {
        _state.value = _state.value.copy(step = AbsenStep.AMBIL_LOKASI)
    }

    // ─────────────────────────────────────────────────────────────
    // STEP 5: Submit ke server
    // ─────────────────────────────────────────────────────────────

    fun submitAbsenMasuk() {
        val s = _state.value
        if (!validasiSebelumSubmit(s)) return

        val lokasiRequest = repository.toLokasiRequest(s.lokasiSaatIni!!)

        viewModelScope.launch {
            _state.value = s.copy(step = AbsenStep.SUBMIT, isSubmitting = true, errorMessage = null)

            repository.absenMasuk(
                fotoFile = s.fotoFile!!,
                shiftId  = s.shiftDipilih!!.id,
                lokasi   = lokasiRequest,
                catatan  = s.catatan.ifBlank { null }
            ).collect { result ->
                handleResult(result)
            }
        }
    }

    fun submitAbsenPulang() {
        val s = _state.value
        if (!validasiSebelumSubmit(s, requireShift = false)) return

        val lokasiRequest = repository.toLokasiRequest(s.lokasiSaatIni!!)

        viewModelScope.launch {
            _state.value = s.copy(step = AbsenStep.SUBMIT, isSubmitting = true, errorMessage = null)

            repository.absenPulang(
                fotoFile = s.fotoFile!!,
                lokasi   = lokasiRequest,
                catatan  = s.catatan.ifBlank { null }
            ).collect { result ->
                handleResult(result)
            }
        }
    }

    private fun validasiSebelumSubmit(s: AbsenUiState, requireShift: Boolean = true): Boolean {
        return when {
            requireShift && s.shiftDipilih == null -> {
                _state.value = s.copy(errorMessage = "Pilih shift terlebih dahulu")
                false
            }
            s.lokasiSaatIni == null -> {
                _state.value = s.copy(errorMessage = "Lokasi belum tersedia")
                false
            }
            s.mockLocationTerdeteksi -> {
                _state.value = s.copy(errorMessage = "🚫 Fake GPS terdeteksi. Absen ditolak.")
                false
            }
            s.dalamRadius == false -> {
                _state.value = s.copy(errorMessage = "🚫 Anda di luar radius kantor.")
                false
            }
            s.fotoFile == null -> {
                _state.value = s.copy(errorMessage = "Foto selfie belum diambil")
                false
            }
            else -> true
        }
    }

    private fun handleResult(result: Resource<AbsenResponse>) {
        when (result) {
            is Resource.Loading -> { /* sudah set di submit */ }
            is Resource.Success -> _state.value = _state.value.copy(
                isSubmitting = false,
                step         = AbsenStep.SUKSES,
                absenResult  = result.data
            )
            is Resource.Error   -> _state.value = _state.value.copy(
                isSubmitting = false,
                step         = AbsenStep.KONFIRMASI, // kembali ke konfirmasi agar bisa retry
                errorMessage = result.message
            )
        }
    }

    // ─────────────────────────────────────────────────────────────
    // Utils
    // ─────────────────────────────────────────────────────────────

    fun resetAbsenResult() {
        _state.value = AbsenUiState()
        muatDataOpdDanShift()
        muatStatusHariIni()
    }

    fun clearError() {
        _state.value = _state.value.copy(errorMessage = null)
    }

    private fun formatJarak(meter: Float?): String {
        if (meter == null) return "?"
        return if (meter >= 1000) String.format("%.1f km", meter / 1000f)
        else "${meter.toInt()} meter"
    }
}