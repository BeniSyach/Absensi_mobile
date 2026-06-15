package com.dss.absensiKoas.ui.screen.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dss.absensiKoas.data.model.LoginResponse
import com.dss.absensiKoas.data.model.RegisterRequest
import com.dss.absensiKoas.data.model.UserDetailResponse
import com.dss.absensiKoas.data.repository.AuthRepository
import com.dss.absensiKoas.data.repository.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LoginUiState(
    val username: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val loginSuccess: Boolean = false
)

data class RegistrasiUiState(
    val nip: String = "",
    val username: String = "",
    val password: String = "",
    val konfirmasiPassword: String = "",
    val namaLengkap: String = "",
    val email: String = "",
    val telepon: String = "",
    val opdId: Long? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val registrasiSuccess: Boolean = false
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _loginState = MutableStateFlow(LoginUiState())
    val loginState: StateFlow<LoginUiState> = _loginState.asStateFlow()

    private val _registrasiState = MutableStateFlow(RegistrasiUiState())
    val registrasiState: StateFlow<RegistrasiUiState> = _registrasiState.asStateFlow()

    fun onUsernameChange(value: String) {
        _loginState.value = _loginState.value.copy(username = value, errorMessage = null)
    }

    fun onPasswordChange(value: String) {
        _loginState.value = _loginState.value.copy(password = value, errorMessage = null)
    }

    fun login() {
        val state = _loginState.value
        if (state.username.isBlank() || state.password.isBlank()) {
            _loginState.value = state.copy(errorMessage = "Username dan password harus diisi")
            return
        }

        viewModelScope.launch {
            authRepository.login(state.username, state.password).collect { result ->
                when (result) {
                    is Resource.Loading -> {
                        _loginState.value = _loginState.value.copy(isLoading = true, errorMessage = null)
                    }
                    is Resource.Success -> {
                        _loginState.value = _loginState.value.copy(
                            isLoading = false,
                            loginSuccess = true
                        )
                    }
                    is Resource.Error -> {
                        _loginState.value = _loginState.value.copy(
                            isLoading = false,
                            errorMessage = result.message
                        )
                    }
                }
            }
        }
    }

    fun resetLoginSuccess() {
        _loginState.value = _loginState.value.copy(loginSuccess = false)
    }

    // === Registrasi ===

    fun updateRegistrasiField(
        nip: String? = null,
        username: String? = null,
        password: String? = null,
        konfirmasiPassword: String? = null,
        namaLengkap: String? = null,
        email: String? = null,
        telepon: String? = null,
        opdId: Long? = null
    ) {
        val current = _registrasiState.value
        _registrasiState.value = current.copy(
            nip = nip ?: current.nip,
            username = username ?: current.username,
            password = password ?: current.password,
            konfirmasiPassword = konfirmasiPassword ?: current.konfirmasiPassword,
            namaLengkap = namaLengkap ?: current.namaLengkap,
            email = email ?: current.email,
            telepon = telepon ?: current.telepon,
            opdId = opdId ?: current.opdId,
            errorMessage = null
        )
    }

    fun registrasi() {
        val state = _registrasiState.value

        // Validasi dasar
        when {
            state.nip.isBlank() || state.username.isBlank() || state.password.isBlank()
                    || state.namaLengkap.isBlank() -> {
                _registrasiState.value = state.copy(errorMessage = "Semua field wajib diisi")
                return
            }
            state.password != state.konfirmasiPassword -> {
                _registrasiState.value = state.copy(errorMessage = "Password dan konfirmasi tidak cocok")
                return
            }
            state.password.length < 8 -> {
                _registrasiState.value = state.copy(errorMessage = "Password minimal 8 karakter")
                return
            }
            state.opdId == null -> {
                _registrasiState.value = state.copy(errorMessage = "Pilih OPD/instansi Anda")
                return
            }
        }

        viewModelScope.launch {
            val request = RegisterRequest(
                nip = state.nip,
                username = state.username,
                password = state.password,
                konfirmasiPassword = state.konfirmasiPassword,
                namaLengkap = state.namaLengkap,
                email = state.email.ifBlank { null },
                telepon = state.telepon.ifBlank { null },
                opdId = state.opdId!!
            )

            authRepository.registrasi(request).collect { result ->
                when (result) {
                    is Resource.Loading -> {
                        _registrasiState.value = _registrasiState.value.copy(isLoading = true, errorMessage = null)
                    }
                    is Resource.Success -> {
                        _registrasiState.value = _registrasiState.value.copy(
                            isLoading = false,
                            registrasiSuccess = true
                        )
                    }
                    is Resource.Error -> {
                        _registrasiState.value = _registrasiState.value.copy(
                            isLoading = false,
                            errorMessage = result.message
                        )
                    }
                }
            }
        }
    }
}