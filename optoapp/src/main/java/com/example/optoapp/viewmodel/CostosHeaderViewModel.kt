package com.example.optoapp.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.optoapp.data.DispensacionDao
import com.example.optoapp.data.Resource
import com.example.optoapp.data.SessionManager
import com.example.optoapp.domain.FuenteDatos
import com.example.optoapp.domain.MarginFacts
import com.example.optoapp.domain.ObtenerAnalisisMensualUseCase
import com.example.optoapp.util.CurrentMonthProvider
import com.example.optoapp.util.MesArg
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.YearMonth
import javax.inject.Inject

data class CostosHeaderUiState(
    val mes: YearMonth,
    val isLoading: Boolean = true,
    val error: String? = null,
    val costoVentas: Double = 0.0,
    val gastos: Double = 0.0,
    val facts: MarginFacts = MarginFacts(),
    val otsSinCosto: Int = 0,
) {
    val offline: Boolean get() = facts.fuente == FuenteDatos.OFFLINE

    // Offline the coverage badge reads "Sin datos"; a count next to it would contradict it.
    val mostrarCtaOtsSinCosto: Boolean get() = !isLoading && error == null && !offline && otsSinCosto > 0
}

@HiltViewModel
class CostosHeaderViewModel @Inject constructor(
    private val obtenerAnalisisMensual: ObtenerAnalisisMensualUseCase,
    private val dispensacionDao: DispensacionDao,
    private val sessionManager: SessionManager,
    private val currentMonthProvider: CurrentMonthProvider,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val mes = MutableStateFlow(
        MesArg.parseOrCurrent(savedStateHandle.get<String>(MesArg.KEY), currentMonthProvider.current()),
    )

    private val _uiState = MutableStateFlow(CostosHeaderUiState(mes = mes.value))
    val uiState: StateFlow<CostosHeaderUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(sessionManager.opticaId, mes) { opticaId, seleccionado -> opticaId to seleccionado }
                .distinctUntilChanged()
                .collectLatest { (opticaId, seleccionado) -> cargar(opticaId, seleccionado) }
        }
    }

    fun seleccionarMes(nuevo: YearMonth) {
        mes.value = minOf(nuevo, currentMonthProvider.current())
    }

    private suspend fun cargar(opticaId: String, seleccionado: YearMonth) {
        _uiState.value = CostosHeaderUiState(mes = seleccionado)
        _uiState.value = when (val analisis = obtenerAnalisisMensual(opticaId, seleccionado.atDay(1))) {
            is Resource.Success -> {
                val datos = requireNotNull(analisis.data)
                _uiState.value.copy(
                    isLoading = false,
                    costoVentas = datos.costoDeVentas(),
                    gastos = datos.gastosMes,
                    facts = datos.marginFacts,
                )
            }
            else -> _uiState.value.copy(isLoading = false, error = analisis.message)
        }
        dispensacionDao.countOtsSinCosto(opticaId, seleccionado.atDay(1), seleccionado.atEndOfMonth())
            .collect { total -> _uiState.update { it.copy(otsSinCosto = total) } }
    }
}
