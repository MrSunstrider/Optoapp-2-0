package com.example.optoapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.optoapp.data.OptoRepository
import com.example.optoapp.data.Pago
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

data class CierreCajaUiState(
    val fecha: Long = System.currentTimeMillis(),
    val pagos: List<Pago> = emptyList(),
    val isLoading: Boolean = false
)

@HiltViewModel
class CierreCajaViewModel @Inject constructor(
    private val repository: OptoRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CierreCajaUiState())
    val uiState: StateFlow<CierreCajaUiState> = _uiState.asStateFlow()

    init {
        observePagos()
    }

    fun setFecha(fecha: Long) {
        _uiState.update { it.copy(fecha = fecha) }
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    private fun observePagos() {
        _uiState.map { it.fecha }
            .distinctUntilChanged()
            .flatMapLatest { fecha ->
                val calendar = Calendar.getInstance().apply {
                    timeInMillis = fecha
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val start = calendar.timeInMillis
                calendar.set(Calendar.HOUR_OF_DAY, 23)
                calendar.set(Calendar.MINUTE, 59)
                calendar.set(Calendar.SECOND, 59)
                val end = calendar.timeInMillis
                
                repository.getPagosByDateRange(start, end)
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
