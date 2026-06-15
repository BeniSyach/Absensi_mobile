package com.dss.absensiKoas.ui.screen.riwayat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dss.absensiKoas.data.model.AbsenRiwayatItem
import com.dss.absensiKoas.data.repository.AbsensiRepository
import com.dss.absensiKoas.data.repository.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class RiwayatUiState(
    val isLoading: Boolean = false,
    val riwayatMasuk: List<AbsenRiwayatItem> = emptyList(),
    val riwayatPulang: List<AbsenRiwayatItem> = emptyList(),
    val errorMessage: String? = null,
    val dari: LocalDate = LocalDate.now().withDayOfMonth(1),
    val sampai: LocalDate = LocalDate.now()
)

@HiltViewModel
class RiwayatViewModel @Inject constructor(
    private val absensiRepository: AbsensiRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RiwayatUiState())
    val uiState: StateFlow<RiwayatUiState> = _uiState.asStateFlow()

    private val formatter = DateTimeFormatter.ISO_LOCAL_DATE

    init {
        muatRiwayat()
    }

    fun setPeriode(dari: LocalDate, sampai: LocalDate) {
        _uiState.value = _uiState.value.copy(dari = dari, sampai = sampai)
        muatRiwayat()
    }

    fun muatRiwayat() {
        val dariStr = _uiState.value.dari.format(formatter)
        val sampaiStr = _uiState.value.sampai.format(formatter)

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            absensiRepository.getRiwayatMasuk(dariStr, sampaiStr).collect { result ->
                if (result is Resource.Success) {
                    _uiState.value = _uiState.value.copy(riwayatMasuk = result.data)
                } else if (result is Resource.Error) {
                    _uiState.value = _uiState.value.copy(errorMessage = result.message)
                }
            }

            absensiRepository.getRiwayatPulang(dariStr, sampaiStr).collect { result ->
                when (result) {
                    is Resource.Success -> {
                        _uiState.value = _uiState.value.copy(isLoading = false, riwayatPulang = result.data)
                    }
                    is Resource.Error -> {
                        _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = result.message)
                    }
                    else -> {}
                }
            }
        }
    }
}