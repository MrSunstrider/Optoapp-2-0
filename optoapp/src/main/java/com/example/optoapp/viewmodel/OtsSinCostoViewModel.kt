package com.example.optoapp.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.optoapp.data.DispensacionDao
import com.example.optoapp.data.OtSinCostoRow
import com.example.optoapp.data.SessionManager
import com.example.optoapp.ui.navigation.Route
import com.example.optoapp.util.CurrentMonthProvider
import com.example.optoapp.util.MesArg
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.YearMonth
import javax.inject.Inject

data class OtsSinCostoUiState(
    val mes: YearMonth,
    val isLoading: Boolean = true,
    val filas: List<OtSinCostoRow> = emptyList(),
)

@HiltViewModel
class OtsSinCostoViewModel @Inject constructor(
    private val dispensacionDao: DispensacionDao,
    private val sessionManager: SessionManager,
    currentMonthProvider: CurrentMonthProvider,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        OtsSinCostoUiState(
            mes = MesArg.parseOrCurrent(savedStateHandle.get<String>(MesArg.KEY), currentMonthProvider.current()),
        ),
    )
    val uiState: StateFlow<OtsSinCostoUiState> = _uiState.asStateFlow()

    init {
        val mes = _uiState.value.mes
        viewModelScope.launch {
            sessionManager.opticaId.collectLatest { opticaId ->
                dispensacionDao.observeOtsSinCosto(opticaId, mes.atDay(1), mes.atEndOfMonth())
                    .collect { filas -> _uiState.update { it.copy(isLoading = false, filas = filas) } }
            }
        }
    }

    fun rutaEdicion(fila: OtSinCostoRow): String =
        Route.EditarDispensacion(fila.pacienteId, fila.id, Route.EditarDispensacion.SECCION_COSTO).route
}
