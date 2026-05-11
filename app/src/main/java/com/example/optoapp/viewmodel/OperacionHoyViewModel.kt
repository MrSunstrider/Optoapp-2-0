package com.example.optoapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.optoapp.data.DispensacionOptica
import com.example.optoapp.data.Montura
import com.example.optoapp.data.OptoRepository
import com.example.optoapp.data.Pago
import com.example.optoapp.data.ServicioExtra
import com.example.optoapp.data.SessionManager
import com.example.optoapp.util.DateUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import javax.inject.Inject

data class OperacionHoyUiState(
    val fecha: LocalDate = DateUtils.today(),
    val citasHoy: Int = 0,
    val entregasPendientes: Int = 0,
    val cobrosHoy: Double = 0.0,
    val stockCritico: Int = 0,
    val alertas: List<String> = emptyList(),
    val pagosHoy: List<Pago> = emptyList(),
    val dispensacionesPendientes: List<DispensacionOptica> = emptyList(),
    val serviciosPendientes: List<ServicioExtra> = emptyList(),
    val monturas: List<Montura> = emptyList()
)

@HiltViewModel
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class OperacionHoyViewModel @Inject constructor(
    private val repository: OptoRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val trigger = MutableStateFlow(System.currentTimeMillis())

    val uiState: StateFlow<OperacionHoyUiState> = combine(trigger, sessionManager.opticaId) { _, opticaId ->
        opticaId
    }.flatMapLatest { opticaId ->
        val today = DateUtils.today()
        combine(
            repository.getEvaluacionesProximaCitaEnRango(opticaId, today, today),
            repository.getAllDispensacionesForOptica(opticaId),
            repository.getPagosByDateRangeForOptica(today, today, opticaId),
            repository.getAllServiciosForOptica(opticaId),
            repository.getMonturasByOptica(opticaId)
        ) { citas, dispensaciones, pagosHoy, servicios, monturas ->
            val dispPendientes = dispensaciones.filter { it.estadoEntrega.equals("Pendiente", ignoreCase = true) }
            val servPendientes = servicios.filter { it.estado.equals("Pendiente", ignoreCase = true) }
            val stockCritico = monturas.count { it.activo && it.stockActual <= it.stockMinimo }
            val alertas = buildList {
                if (stockCritico > 0) add("Hay $stockCritico monturas en stock crítico.")
                val atrasadas = dispPendientes.count { it.fecha.isBefore(today) }
                if (atrasadas > 0) add("Hay $atrasadas entregas pendientes con fecha anterior a hoy.")
                if (servPendientes.isNotEmpty()) add("Servicios extra pendientes: ${servPendientes.size}.")
                if (citas.isNotEmpty()) add("Citas de control programadas hoy: ${citas.size}.")
            }
            OperacionHoyUiState(
                fecha = today,
                citasHoy = citas.size,
                entregasPendientes = dispPendientes.size + servPendientes.size,
                cobrosHoy = pagosHoy.sumOf { it.monto },
                stockCritico = stockCritico,
                alertas = alertas,
                pagosHoy = pagosHoy,
                dispensacionesPendientes = dispPendientes,
                serviciosPendientes = servPendientes,
                monturas = monturas
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), OperacionHoyUiState())

    fun refresh() {
        trigger.value = System.currentTimeMillis()
    }
}
