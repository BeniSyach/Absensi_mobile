package com.dss.absensiKoas.ui.screen.profil

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dss.absensiKoas.data.model.GantiPasswordRequest
import com.dss.absensiKoas.data.model.UpdateProfilRequest
import com.dss.absensiKoas.data.model.UserDetailResponse
import com.dss.absensiKoas.data.repository.Resource
import com.dss.absensiKoas.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class ProfilUiState(
    val isLoading: Boolean = false,
    val profil: UserDetailResponse? = null,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    // form edit
    val namaLengkap: String = "",
    val email: String = "",
    val telepon: String = "",
    val isSaving: Boolean = false,
    val updateSuccess: Boolean = false
)

data class GantiPasswordUiState(
    val passwordLama: String = "",
    val passwordBaru: String = "",
    val konfirmasiPasswordBaru: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val success: Boolean = false
)

@HiltViewModel
class ProfilViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfilUiState())
    val uiState: StateFlow<ProfilUiState> = _uiState.asStateFlow()

    private val _gantiPasswordState = MutableStateFlow(GantiPasswordUiState())
    val gantiPasswordState: StateFlow<GantiPasswordUiState> = _gantiPasswordState.asStateFlow()

    init {
        muatProfil()
    }

    fun muatProfil() {
        viewModelScope.launch {
            userRepository.getProfil().collect { result ->
                when (result) {
                    is Resource.Loading -> {
                        _uiState.value = _uiState.value.copy(isLoading = true)
                    }
                    is Resource.Success -> {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            profil = result.data,
                            namaLengkap = result.data.namaLengkap,
                            email = result.data.email ?: "",
                            telepon = result.data.telepon ?: ""
                        )
                    }
                    is Resource.Error -> {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = result.message
                        )
                    }
                }
            }
        }
    }

    fun onNamaChange(value: String) {
        _uiState.value = _uiState.value.copy(namaLengkap = value, errorMessage = null)
    }

    fun onEmailChange(value: String) {
        _uiState.value = _uiState.value.copy(email = value, errorMessage = null)
    }

    fun onTeleponChange(value: String) {
        _uiState.value = _uiState.value.copy(telepon = value, errorMessage = null)
    }

    fun simpanProfil() {
        val state = _uiState.value
        if (state.namaLengkap.isBlank()) {
            _uiState.value = state.copy(errorMessage = "Nama lengkap tidak boleh kosong")
            return
        }

        viewModelScope.launch {
            userRepository.updateProfil(
                UpdateProfilRequest(
                    namaLengkap = state.namaLengkap,
                    email = state.email.ifBlank { null },
                    telepon = state.telepon.ifBlank { null }
                )
            ).collect { result ->
                when (result) {
                    is Resource.Loading -> {
                        _uiState.value = _uiState.value.copy(isSaving = true, errorMessage = null)
                    }
                    is Resource.Success -> {
                        _uiState.value = _uiState.value.copy(
                            isSaving = false,
                            profil = result.data,
                            updateSuccess = true,
                            successMessage = "Profil berhasil diperbarui"
                        )
                    }
                    is Resource.Error -> {
                        _uiState.value = _uiState.value.copy(
                            isSaving = false,
                            errorMessage = result.message
                        )
                    }
                }
            }
        }
    }

    fun uploadFotoProfil(file: File) {
        viewModelScope.launch {
            userRepository.uploadFotoProfil(file).collect { result ->
                when (result) {
                    is Resource.Loading -> {
                        _uiState.value = _uiState.value.copy(isSaving = true)
                    }
                    is Resource.Success -> {
                        _uiState.value = _uiState.value.copy(
                            isSaving = false,
                            profil = result.data,
                            successMessage = "Foto profil berhasil diperbarui"
                        )
                    }
                    is Resource.Error -> {
                        _uiState.value = _uiState.value.copy(
                            isSaving = false,
                            errorMessage = result.message
                        )
                    }
                }
            }
        }
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(errorMessage = null, successMessage = null, updateSuccess = false)
    }

    // === Ganti Password ===

    fun onPasswordLamaChange(value: String) {
        _gantiPasswordState.value = _gantiPasswordState.value.copy(passwordLama = value, errorMessage = null)
    }

    fun onPasswordBaruChange(value: String) {
        _gantiPasswordState.value = _gantiPasswordState.value.copy(passwordBaru = value, errorMessage = null)
    }

    fun onKonfirmasiPasswordChange(value: String) {
        _gantiPasswordState.value = _gantiPasswordState.value.copy(konfirmasiPasswordBaru = value, errorMessage = null)
    }

    fun gantiPassword() {
        val state = _gantiPasswordState.value
        if (state.passwordLama.isBlank() || state.passwordBaru.isBlank()) {
            _gantiPasswordState.value = state.copy(errorMessage = "Semua field harus diisi")
            return
        }
        if (state.passwordBaru != state.konfirmasiPasswordBaru) {
            _gantiPasswordState.value = state.copy(errorMessage = "Password baru dan konfirmasi tidak cocok")
            return
        }
        if (state.passwordBaru.length < 8) {
            _gantiPasswordState.value = state.copy(errorMessage = "Password baru minimal 8 karakter")
            return
        }

        viewModelScope.launch {
            userRepository.gantiPassword(
                GantiPasswordRequest(
                    passwordLama = state.passwordLama,
                    passwordBaru = state.passwordBaru,
                    konfirmasiPasswordBaru = state.konfirmasiPasswordBaru
                )
            ).collect { result ->
                when (result) {
                    is Resource.Loading -> {
                        _gantiPasswordState.value = _gantiPasswordState.value.copy(isLoading = true, errorMessage = null)
                    }
                    is Resource.Success -> {
                        _gantiPasswordState.value = _gantiPasswordState.value.copy(isLoading = false, success = true)
                    }
                    is Resource.Error -> {
                        _gantiPasswordState.value = _gantiPasswordState.value.copy(
                            isLoading = false,
                            errorMessage = result.message
                        )
                    }
                }
            }
        }
    }
}