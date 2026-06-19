package com.example.optoapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.optoapp.data.SessionManager
import com.example.optoapp.data.arqueo.ArqueoCaja
import com.example.optoapp.data.arqueo.IArqueoCajaRepo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject

// ---------------------------------------------------------------------------
// ArqueoCajaUiState
// ---------------------------------------------------------------------------

data class ArqueoCajaUiState(
    val fondoCaja: Double = 0.0,
    val efectivoContado: Double = 0.0,
    val tarjetaContado: Double = 0.0,
    val transferenciaContado: Double = 0.0,
    val movilContado: Double = 0.0,
    val existingArqueo: ArqueoCaja? = null,
    val isSellado: Boolean = false,
    val validationErrors: Map<String, String> = emptyMap()
)

// ---------------------------------------------------------------------------
// Badge color thresholds: 0 = green; abs ≤ 5% of cobrado = yellow; > 5% = red
// ---------------------------------------------------------------------------

enum class BadgeColor { GREEN, YELLOW, RED }

@HiltViewModel
class ArqueoCajaViewModel @Inject constructor(
    private val repo: IArqueoCajaRepo,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(ArqueoCajaUiState())
    val uiState: StateFlow<ArqueoCajaUiState> = _uiState.asStateFlow()

    // Resolved asynchronously in init — empty until the coroutine completes.
    private var currentUserId: String = ""

    init {
        viewModelScope.launch {
            currentUserId = sessionManager.userEmail.first()
        }
    }

    fun setFondoCaja(value: Double) { _uiState.update { it.copy(fondoCaja = value) } }
    fun setEfectivoContado(value: Double) { _uiState.update { it.copy(efectivoContado = value) } }
    fun setTarjetaContado(value: Double) { _uiState.update { it.copy(tarjetaContado = value) } }
    fun setTransferenciaContado(value: Double) { _uiState.update { it.copy(transferenciaContado = value) } }
    fun setMovilContado(value: Double) { _uiState.update { it.copy(movilContado = value) } }

    fun cerrarDia(fecha: LocalDate, opticaId: String, systemTotals: Map<String, Double>) {
        if (currentUserId.isEmpty()) {
            // Guard: userId not yet resolved — skip to avoid stamping an empty cerradoPor.
            return
        }
        viewModelScope.launch {
            val state = _uiState.value
            val errors = mutableMapOf<String, String>()
            if (state.fondoCaja < 0) errors["fondoCaja"] = "Cannot be negative"
            if (state.efectivoContado < 0) errors["efectivoContado"] = "Cannot be negative"
            if (state.tarjetaContado < 0) errors["tarjetaContado"] = "Cannot be negative"
            if (state.transferenciaContado < 0) errors["transferenciaContado"] = "Cannot be negative"
            if (state.movilContado < 0) errors["movilContado"] = "Cannot be negative"
            if (errors.isNotEmpty()) {
                _uiState.update { it.copy(validationErrors = errors) }
                return@launch
            }
            val efCobrado    = systemTotals["Efectivo"]      ?: 0.0
            val tarCobrado   = systemTotals["Tarjeta"]       ?: 0.0
            val transCobrado = systemTotals["Transferencia"] ?: 0.0
            val movCobrado   = systemTotals["Móvil"]         ?: 0.0
            val arqueo = ArqueoCaja(
                id = UUID.randomUUID().toString(),
                fecha = fecha,
                opticaId = opticaId,
                fondoCaja = state.fondoCaja,
                efectivoContado = state.efectivoContado,
                tarjetaContado = state.tarjetaContado,
                transferenciaContado = state.transferenciaContado,
                movilContado = state.movilContado,
                efectivoCobrado = efCobrado,
                tarjetaCobrado = tarCobrado,
                transferenciaCobrado = transCobrado,
                movilCobrado = movCobrado,
                diferenciaEfectivo = state.efectivoContado - efCobrado,
                diferenciaTarjeta = state.tarjetaContado - tarCobrado,
                diferenciaTransferencia = state.transferenciaContado - transCobrado,
                diferenciaMovil = state.movilContado - movCobrado,
                diferenciaTotal = (state.efectivoContado + state.tarjetaContado +
                                   state.transferenciaContado + state.movilContado) -
                                  (efCobrado + tarCobrado + transCobrado + movCobrado),
                cerradoPor = currentUserId,
                sellado = true,
                createdAt = Instant.now().toString(),
                updatedAt = Instant.now().toString()
            )
            repo.insertArqueo(arqueo)
            _uiState.update { it.copy(existingArqueo = arqueo, isSellado = true, validationErrors = emptyMap()) }
        }
    }

    companion object {
        fun badgeColorFor(contado: Double, cobrado: Double): BadgeColor = when {
            contado == cobrado -> BadgeColor.GREEN
            cobrado == 0.0    -> BadgeColor.RED
            else -> {
                val ratio = kotlin.math.abs(contado - cobrado) / cobrado
                if (ratio <= 0.05) BadgeColor.YELLOW else BadgeColor.RED
            }
        }
    }
}
