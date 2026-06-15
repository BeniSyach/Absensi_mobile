package com.dss.absensiKoas.ui.screen.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dss.absensiKoas.data.model.StatusHariIniResponse
import com.dss.absensiKoas.data.model.UserDetailResponse
import com.dss.absensiKoas.data.repository.AbsensiRepository
import com.dss.absensiKoas.data.repository.AuthRepository
import com.dss.absensiKoas.data.repository.Resource
import com.dss.absensiKoas.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val isLoading: Boolean = false,
    val profil: UserDetailResponse? = null,
    val statusHariIni: StatusHariIniResponse? = null,
    val errorMessage: String? = null,
    val logoutSuccess: Boolean = false
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val absensiRepository: AbsensiRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        muatData()
    }

    fun muatData() {
        muatProfil()
        muatStatusHariIni()
    }

    private fun muatProfil() {
        viewModelScope.launch {
            userRepository.getProfil().collect { result ->
                when (result) {
                    is Resource.Loading -> {
                        _uiState.value = _uiState.value.copy(isLoading = true)
                    }
                    is Resource.Success -> {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            profil = result.data
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

    fun muatStatusHariIni() {
        viewModelScope.launch {
            absensiRepository.getStatusHariIni().collect { result ->
                when (result) {
                    is Resource.Success -> {
                        _uiState.value = _uiState.value.copy(statusHariIni = result.data)
                    }
                    else -> { /* abaikan loading/error untuk status ringkas */ }
                }
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.logout().collect { result ->
                if (result is Resource.Success) {
                    _uiState.value = _uiState.value.copy(logoutSuccess = true)
                }
            }
        }
    }
}