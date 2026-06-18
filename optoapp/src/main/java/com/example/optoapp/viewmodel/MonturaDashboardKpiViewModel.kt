package com.example.optoapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.optoapp.data.SessionManager
import com.example.optoapp.data.montura.CategoriaCount
import com.example.optoapp.data.montura.MonturaDashboardKpiRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class MonturaDashboardKpiState(
    val totalModelos: Int = 0,
    val stockByCategory: List<CategoriaCount> = emptyList(),
    val lowStockCount: Int = 0,
    val lastMovementDate: LocalDate? = null,
    val isLoading: Boolean = true
)

@HiltViewModel
class MonturaDashboardKpiViewModel @Inject constructor(
    private val repository: MonturaDashboardKpiRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(MonturaDashboardKpiState())
    val uiState: StateFlow<MonturaDashboardKpiState> = _uiState.asStateFlow()

    init {
        observeKpis()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun observeKpis() {
        sessionManager.opticaId
            .filter { it.isNotBlank() && it != "mi_optica_base" }
            .distinctUntilChanged()
            .flatMapLatest { opticaId ->
                combine(
                    repository.totalModelos(opticaId),
                    repository.stockByCategory(opticaId),
                    repository.lowStockCount(opticaId),
                    repository.lastMovementDate(opticaId)
                ) { total, categories, lowStock, lastDate ->
                    MonturaDashboardKpiState(
                        totalModelos = total,
                        stockByCategory = categories,
                        lowStockCount = lowStock,
                        lastMovementDate = lastDate,
                        isLoading = false
                    )
                }
            }
            .onStart { _uiState.update { it.copy(isLoading = true) } }
            .catch { _uiState.update { it.copy(isLoading = false) } }
            .onEach { state -> _uiState.value = state }
            .launchIn(viewModelScope)
    }
}
