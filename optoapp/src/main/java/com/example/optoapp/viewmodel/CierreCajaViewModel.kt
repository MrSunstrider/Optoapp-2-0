package com.example.optoapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.optoapp.data.OptoRepository
import com.example.optoapp.data.Pago
import com.example.optoapp.data.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import com.example.optoapp.util.DateUtils
import javax.inject.Inject

data class CierreCajaUiState(
    val fecha: LocalDate = DateUtils.today(),
    val pagos: List<Pago> = emptyList(),
    val isLoading: Boolean = false
)

@HiltViewModel
class CierreCajaViewModel @Inject constructor(
    private val repository: OptoRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(CierreCajaUiState())
    val uiState: StateFlow<CierreCajaUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            sessionManager.userTimeZone.collectLatest { _ ->
                _uiState.update { it.copy(fecha = DateUtils.today()) }
            }
        }
        observePagos()
    }

    fun setFecha(fecha: LocalDate) {
        _uiState.update { it.copy(fecha = fecha) }
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    private fun observePagos() {
        kotlinx.coroutines.flow.combine(
            _uiState.map { it.fecha }.distinctUntilChanged(),
            sessionManager.opticaId
        ) { fecha, opticaId -> fecha to opticaId }
            .distinctUntilChanged()
            .flatMapLatest { (fecha, opticaId) ->
                repository.getPagosByDateRangeForOptica(fecha, fecha, opticaId)
            }
            .onEach { lista ->
                _uiState.update { it.copy(pagos = lista, isLoading = false) }
            }.launchIn(viewModelScope)
    }
    
    // Helper to get totals by method
    fun getTotalesPorMetodo(): Map<String, Double> {
        return _uiState.value.pagos.groupBy { it.metodoPago }
            .mapValues { entry -> entry.value.sumOf { it.monto } }
    }
}
