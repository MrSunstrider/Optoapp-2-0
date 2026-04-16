package com.example.optoapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.optoapp.data.DispensacionOptica
import com.example.optoapp.data.MonturaMovimiento
import com.example.optoapp.data.OptoRepository
import com.example.optoapp.data.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import java.util.UUID
import javax.inject.Inject
import com.example.optoapp.data.Pago
import java.time.LocalDate
import com.example.optoapp.util.DateUtils

data class DispensacionUiState(
    val pacienteNombre: String = "",
    val ot: String = "",
    val tipoLente: String = "",
    val distanciaLente: String = "",
    val altura: String = "",
    val materialLente: String = "",
    val tratamientos: List<String> = emptyList(),
    val colorLente: String = "",
    val notasDiseno: String = "",
    
    val origenMontura: String = "",
    val monturaId: String = "",
    val tipoAro: String = "",
    val materialMontura: String = "",
    val descripcionMontura: String = "",
    val tipoMontura: String = "", // Added to match entity
    
    val montoTotal: String = "",
    val estadoEntrega: String = "Pendiente",
    val fecha: LocalDate = DateUtils.today(),
    val fechaVencimientoGarantia: LocalDate? = null,
    
    val subTipoBifocal: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    
    val pagos: List<Pago> = emptyList(),
    val pagosToDelete: List<Pago> = emptyList(),
    val generatedId: String = UUID.randomUUID().toString()
)

@HiltViewModel
class DispensacionViewModel @Inject constructor(
    private val repository: com.example.optoapp.data.OptoRepository,
    private val sessionManager: com.example.optoapp.data.SessionManager,
    private val syncFinanzasUseCase: com.example.optoapp.domain.SyncFinanzasUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(DispensacionUiState())
    val uiState: StateFlow<DispensacionUiState> = _uiState.asStateFlow()
    private val _monturasActivas = MutableStateFlow<List<com.example.optoapp.data.Montura>>(emptyList())
    val monturasActivas: StateFlow<List<com.example.optoapp.data.Montura>> = _monturasActivas.asStateFlow()

    init {
        viewModelScope.launch {
            sessionManager.opticaId.collect { opticaId ->
                repository.getMonturasByOptica(opticaId).collect { items ->
                    _monturasActivas.value = items.filter { it.activo }
                }
            }
        }
    }

    fun getDispensacionesByPaciente(pacienteId: String) = repository.getDispensacionesByPaciente(pacienteId)

    fun loadPacienteNombre(pacienteId: String) {
        viewModelScope.launch {
            when (val result = repository.getPacienteById(pacienteId)) {
                is Resource.Success -> {
                    val nombre = result.data?.nombreCompleto.orEmpty()
                    _uiState.update { it.copy(pacienteNombre = nombre) }
                }
                else -> Unit
            }
        }
    }

    fun loadDispensacion(dispensacionId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, generatedId = dispensacionId) }
            when (val result = repository.getDispensacionById(dispensacionId)) {
                is Resource.Success -> {
                    val d = result.data!!
                    val loadedPagos = repository.getPagosByDispensacion(dispensacionId).first()
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            ot = d.ot,
                            tipoLente = d.tipoLente,
                            distanciaLente = d.distanciaLente,
                            altura = d.altura,
                            materialLente = d.materialLente,
                            tratamientos = d.tratamientos,
                            colorLente = d.colorLente,
                            notasDiseno = d.notasDiseno,
                            subTipoBifocal = d.subTipoBifocal,
                            origenMontura = d.origenMontura,
                            monturaId = d.monturaId,
                            tipoAro = d.tipoAro,
                            materialMontura = d.materialMontura,
                            descripcionMontura = d.descripcionMontura,
                            tipoMontura = d.tipoMontura,
                            montoTotal = d.montoTotal.toString(),
                            estadoEntrega = d.estadoEntrega,
                            fecha = d.fecha,
                            fechaVencimientoGarantia = d.fechaVencimientoGarantia,
                            pagos = loadedPagos
                        )
                    }
                }
                is Resource.Error -> {
                    _uiState.update { it.copy(isLoading = false, error = result.message) }
                }
                is Resource.Loading -> { }
            }
        }
    }

    fun addPago(pago: Pago) {
        _uiState.update { it.copy(pagos = it.pagos + pago) }
    }

    fun updatePagoLocal(pago: Pago) {
        _uiState.update { s ->
            val updatedPagos = s.pagos.map { if (it.id == pago.id) pago else it }
            s.copy(pagos = updatedPagos)
        }
    }

    fun removePagoLocal(pago: Pago) {
        _uiState.update { s ->
            val updatedPagos = s.pagos.filter { it.id != pago.id }
            val updatedToDelete = if (pago.id.isNotEmpty()) s.pagosToDelete + pago else s.pagosToDelete
            s.copy(pagos = updatedPagos, pagosToDelete = updatedToDelete)
        }
    }

    fun updateUiState(update: (DispensacionUiState) -> DispensacionUiState) {
        _uiState.update(update)
    }

    fun saveDispensacion(pacienteId: String, dispensacionId: String?, onComplete: () -> Unit) {
        viewModelScope.launch {
            val s = _uiState.value
            val requiereAltura = s.tipoLente == "Bifocal" || s.tipoLente == "Progresivo" || s.tipoLente == "Ocupacional"
            if (requiereAltura && s.altura.isBlank()) {
                _uiState.update { it.copy(error = "La altura es obligatoria para ${s.tipoLente}.") }
                return@launch
            }

            val alturaValida = s.altura.trim().replace(",", ".").toDoubleOrNull()
            if (requiereAltura && (alturaValida == null || alturaValida <= 0.0)) {
                _uiState.update { it.copy(error = "Ingresa una altura válida en mm.") }
                return@launch
            }
            if (s.ot.isBlank()) {
                _uiState.update { it.copy(error = "La OT es obligatoria para guardar la dispensación.") }
                return@launch
            }

            _uiState.update { it.copy(error = null) }
            val currentOpticaId = sessionManager.opticaId.first()
            val finalId = dispensacionId ?: s.generatedId
            val finalMontoPagado = s.pagos.sumOf { it.monto }
            val dispensacionAnterior = if (dispensacionId != null && dispensacionId != "null") {
                (repository.getDispensacionById(dispensacionId) as? Resource.Success)?.data
            } else null
            val monturaActualId = if (s.origenMontura == "Nueva de Tienda") s.monturaId else ""

            val disp = DispensacionOptica(
                id = finalId,
                ot = s.ot.trim(),
                monturaId = monturaActualId,
                pacienteId = pacienteId,
                fecha = s.fecha,
                opticaId = currentOpticaId,
                tipoLente = s.tipoLente,
                materialLente = s.materialLente,
                tratamientos = s.tratamientos,
                colorLente = s.colorLente,
                notasDiseno = s.notasDiseno,
                subTipoBifocal = if (s.tipoLente == "Bifocal") s.subTipoBifocal else "",
                origenMontura = s.origenMontura,
                tipoAro = s.tipoAro,
                materialMontura = s.materialMontura,
                descripcionMontura = s.descripcionMontura,
                tipoMontura = s.tipoMontura,
                montoTotal = s.montoTotal.toDoubleOrNull() ?: 0.0,
                montoPagado = finalMontoPagado,
                metodoPago = "",
                estadoEntrega = s.estadoEntrega,
                fechaVencimientoGarantia = s.fechaVencimientoGarantia,
                distanciaLente = if (s.tipoLente == "Monofocal") s.distanciaLente else "",
                altura = if (requiereAltura) s.altura.trim() else ""
            )

            suspend fun registrarMovimiento(monturaId: String, delta: Int, referencia: String, nota: String, tipo: String) {
                val stockAntes = (repository.getMonturaById(monturaId) as? Resource.Success)?.data?.stockActual ?: return
                val changed = repository.adjustMonturaStock(monturaId, currentOpticaId, delta)
                if (changed > 0) {
                    val stockDespues = stockAntes + delta
                    repository.insertMonturaMovimiento(
                        MonturaMovimiento(
                            id = UUID.randomUUID().toString(),
                            monturaId = monturaId,
                            fecha = s.fecha,
                            tipo = tipo,
                            cantidad = kotlin.math.abs(delta),
                            stockPrevio = stockAntes,
                            stockNuevo = stockDespues,
                            referenciaId = referencia,
                            nota = nota,
                            opticaId = currentOpticaId
                        )
                    )
                } else if (delta < 0) {
                    _uiState.update { it.copy(error = "Stock insuficiente para la montura seleccionada.") }
                }
            }

            val monturaAnteriorId = dispensacionAnterior?.monturaId?.takeIf { it.isNotBlank() } ?: ""
            if (monturaAnteriorId.isNotBlank() && monturaAnteriorId != monturaActualId) {
                registrarMovimiento(
                    monturaId = monturaAnteriorId,
                    delta = 1,
                    referencia = finalId,
                    nota = "Reversión por edición de dispensación",
                    tipo = "AJUSTE"
                )
            }
            if (monturaActualId.isNotBlank() && monturaActualId != monturaAnteriorId) {
                registrarMovimiento(
                    monturaId = monturaActualId,
                    delta = -1,
                    referencia = finalId,
                    nota = "Salida por venta en dispensación",
                    tipo = "SALIDA_VENTA"
                )
                if (_uiState.value.error != null) return@launch
            }

            if (dispensacionId != null && dispensacionId != "null") {
                repository.updateDispensacion(disp)
            } else {
                repository.insertDispensacion(disp)
            }

            // Guardar pagos vinculados a esta dispensación
            s.pagos.forEach { pago ->
                val pagoToSave = pago.copy(dispensacionId = finalId, opticaId = currentOpticaId)
                repository.insertPago(pagoToSave)
            }

            // Eliminar pagos marcados
            s.pagosToDelete.forEach { pago ->
                repository.deletePago(pago)
            }

            // Silent Sync en segundo plano
            viewModelScope.launch { 
                try {
                    syncFinanzasUseCase(currentOpticaId)
                } catch (e: Exception) {}
            }
            
            onComplete()
        }
    }
}
