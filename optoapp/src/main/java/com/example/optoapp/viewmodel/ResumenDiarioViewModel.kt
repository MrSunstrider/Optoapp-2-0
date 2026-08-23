package com.example.optoapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.optoapp.data.SessionManager
import com.example.optoapp.data.resumendiario.ResumenDiarioDao
import com.example.optoapp.data.resumendiario.ResumenDiarioEntity
import com.example.optoapp.sync.PostSaveSyncScheduler
import com.example.optoapp.util.DateUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class ResumenDiarioUiState(
    val mesSeleccionado: LocalDate = DateUtils.today().withDayOfMonth(1),
    val rows: List<ResumenDiarioEntity> = emptyList(),
    val refreshing: Boolean = false,
    val message: String? = null,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ResumenDiarioViewModel @Inject constructor(
    private val resumenDiarioDao: ResumenDiarioDao,
    private val sessionManager: SessionManager,
    private val postSaveSyncScheduler: PostSaveSyncScheduler,
) : ViewModel() {

    private val mesSeleccionado = MutableStateFlow(DateUtils.today().withDayOfMonth(1))
    private val refreshing = MutableStateFlow(false)
    private val message = MutableStateFlow<String?>(null)

    private val rowsFlow = combine(sessionManager.opticaId, mesSeleccionado) { oid, mes ->
        oid to yearMonthOf(mes)
    }.flatMapLatest { (oid, yearMonth) ->
        if (oid.isBlank()) flowOf(emptyList())
        else resumenDiarioDao.observeByOpticaAndMonth(oid, yearMonth)
    }

    val uiState: StateFlow<ResumenDiarioUiState> = combine(
        mesSeleccionado,
        rowsFlow,
        refreshing,
        message,
    ) { mes, rows, isRefreshing, msg ->
        ResumenDiarioUiState(
            mesSeleccionado = mes,
            rows = rows,
            refreshing = isRefreshing,
            message = msg,
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, ResumenDiarioUiState())

    fun navigateMonth(delta: Int) {
        mesSeleccionado.update { it.plusMonths(delta.toLong()) }
    }

    fun refresh() {
        viewModelScope.launch {
            refreshing.value = true
            message.value = null
            try {
                val opticaId = sessionManager.opticaId.first()
                if (opticaId.isNotBlank()) {
                    // Finances sync download recalcs resumen_diario remotely; never uploads it.
                    postSaveSyncScheduler.scheduleFinanzasSync(opticaId)
                    message.value = "Actualización de resumen programada"
                } else {
                    message.value = "Óptica no disponible"
                }
            } finally {
                refreshing.value = false
            }
        }
    }

    companion object {
        fun yearMonthOf(date: LocalDate): String =
            String.format("%04d-%02d", date.year, date.monthValue)
    }
}
