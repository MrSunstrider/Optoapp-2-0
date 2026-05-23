package com.example.optoapp.viewmodel

import android.util.Log
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
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import javax.inject.Inject

data class OperacionHoyUiState(
    val fecha: LocalDate = DateUtils.today(),
    val citasHoy: Int = 0,
    val entregasPendientes: Int = 0,
    val cobrosHoy: Double = 0.0,
    val stockCritico: Int = 0,
    val monturasStockCritico: List<Montura> = emptyList(),
    val alertas: List<String> = emptyList(),
    val pagosHoy: List<Pago> = emptyList(),
    val dispensacionesPendientes: List<DispensacionOptica> = emptyList(),
    val serviciosPendientes: List<ServicioExtra> = emptyList(),
    val monturas: List<Montura> = emptyList(),
    val error: String? = null
)

@HiltViewModel
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class OperacionHoyViewModel @Inject constructor(
    private val repository: OptoRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    companion object {
        private const val TAG = "OperacionHoyVM"
    }

    private val trigger = MutableStateFlow(System.currentTimeMillis())

    private val _uiState = MutableStateFlow(OperacionHoyUiState())
    val uiState: StateFlow<OperacionHoyUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(trigger, sessionManager.opticaId) { _, opticaId -> opticaId }
                .collectLatest { opticaId ->
                    val today = DateUtils.today()

                    @Suppress("UNUSED")
                    val citasDeferred = async {
                        runCatching {
                            repository.getEvaluacionesProximaCitaEnRango(opticaId, today, today).first()
                        }.onFailure { e -> Log.e(TAG, "Error fetching citas", e) }
                            .getOrDefault(emptyList())
                    }
                    val dispensacionesDeferred = async {
                        runCatching {
                            repository.getAllDispensacionesForOptica(opticaId).first()
                        }.onFailure { e -> Log.e(TAG, "Error fetching dispensaciones", e) }
                            .getOrDefault(emptyList())
                    }
                    val pagosDeferred = async {
                        runCatching {
                            repository.getPagosByDateRangeForOptica(today, today, opticaId).first()
                        }.onFailure { e -> Log.e(TAG, "Error fetching pagos", e) }
                            .getOrDefault(emptyList())
                    }
                    val serviciosDeferred = async {
                        runCatching {
                            repository.getAllServiciosForOptica(opticaId).first()
                        }.onFailure { e -> Log.e(TAG, "Error fetching servicios", e) }
                            .getOrDefault(emptyList())
                    }
                    val monturasDeferred = async {
                        runCatching {
                            repository.getMonturasByOptica(opticaId).first()
                        }.onFailure { e -> Log.e(TAG, "Error fetching monturas", e) }
                            .getOrDefault(emptyList())
                    }

                    val citas = citasDeferred.await()
                    val dispensaciones = dispensacionesDeferred.await()
                    val pagosHoy = pagosDeferred.await()
                    val servicios = serviciosDeferred.await()
                    val monturas = monturasDeferred.await()

                    // Detectar si alguna fuente falló (todos los runCatching devuelven lista vacía en error)
                    val errorMsg = when {
                        citas.isEmpty() && dispensaciones.isEmpty() && pagosHoy.isEmpty()
                            && servicios.isEmpty() && monturas.isEmpty() -> "No se pudieron cargar los datos. Verifica tu conexión."
                        else -> null
                    }

                    val dispPendientes = dispensaciones.filter { it.estadoEntrega.equals("Pendiente", ignoreCase = true) }
                    val servPendientes = servicios.filter { it.estado.equals("Pendiente", ignoreCase = true) }
                    val monturasCriticas = monturas.filter { it.activo && it.stockActual <= it.stockMinimo }
                        .sortedBy { it.stockActual - it.stockMinimo }
                    val stockCritico = monturasCriticas.size
                    val alertas = buildList {
                        if (stockCritico > 0) {
                            add("Productos por reponer: $stockCritico")
                            monturasCriticas.take(5).forEach { m ->
                                add("• ${m.sku} ${m.marca} ${m.modelo}: ${m.stockActual}/${m.stockMinimo}")
                            }
                            if (monturasCriticas.size > 5) {
                                add("... y ${monturasCriticas.size - 5} más. Toca Stock Crítico para ver todos.")
                            }
                        }
                        val atrasadas = dispPendientes.count { it.fecha.isBefore(today) }
                        if (atrasadas > 0) add("Hay $atrasadas entregas pendientes con fecha anterior a hoy.")
                        if (servPendientes.isNotEmpty()) add("Servicios extra pendientes: ${servPendientes.size}.")
                        if (citas.isNotEmpty()) add("Citas de control programadas hoy: ${citas.size}.")
                    }
                    _uiState.value = OperacionHoyUiState(
                        fecha = today,
                        citasHoy = citas.size,
                        entregasPendientes = dispPendientes.size + servPendientes.size,
                        cobrosHoy = pagosHoy.sumOf { it.monto },
                        stockCritico = stockCritico,
                        monturasStockCritico = monturasCriticas,
                        alertas = alertas,
                        pagosHoy = pagosHoy,
                        dispensacionesPendientes = dispPendientes,
                        serviciosPendientes = servPendientes,
                        monturas = monturas,
                        error = errorMsg
                    )
                }
        }
    }

    fun refresh() {
        trigger.value = System.currentTimeMillis()
    }
}
