package com.example.optoapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.optoapp.data.DispensacionItem
import com.example.optoapp.data.DispensacionOptica
import com.example.optoapp.data.EvaluacionClinica
import com.example.optoapp.data.FinanzasRemoteDefaults
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
import com.example.optoapp.sync.PostSaveSyncScheduler
import com.example.optoapp.util.DateUtils
import com.example.optoapp.util.DispensacionStockHelper

data class DispensacionUiState(
    val pacienteNombre: String = "",
    val ot: String = "",
    /** Items de lente (1..N). El primer item es el lente principal de la dispensación. */
    val items: List<DispensacionItemUi> = listOf(DispensacionItemUi()),
    /** Items eliminados (pendientes de persistir la baja). */
    val itemsToDelete: List<String> = emptyList(),

    val origenMontura: String = "",
    val monturaId: String = "",
    val tipoAro: String = "",
    val materialMontura: String = "",
    val descripcionMontura: String = "",
    val tipoMontura: String = "",

    val montoTotal: String = "",
    val estadoEntrega: String = "Pendiente",
    val fecha: LocalDate = DateUtils.today(),
    val fechaVencimientoGarantia: LocalDate? = null,

    val isLoading: Boolean = false,
    val error: String? = null,

    val pagos: List<Pago> = emptyList(),
    val pagosToDelete: List<Pago> = emptyList(),
    val generatedId: String = UUID.randomUUID().toString()
)

data class DispensacionItemUi(
    val id: String = UUID.randomUUID().toString(),
    //── Lente ──
    val tipoLente: String = "",
    val distanciaLente: String = "",
    val altura: String = "",
    val materialLente: String = "",
    val tratamientos: List<String> = emptyList(),
    val colorLente: String = "",
    val notasDiseno: String = "",
    val subTipoBifocal: String = "",
    //── Montura ──
    val monturaId: String = "",
    val origenMontura: String = "",
    val tipoAro: String = "",
    val materialMontura: String = "",
    val descripcionMontura: String = "",
    val tipoMontura: String = ""
)

@HiltViewModel
class DispensacionViewModel @Inject constructor(
    private val repository: com.example.optoapp.data.OptoRepository,
    private val sessionManager: com.example.optoapp.data.SessionManager,
    private val postSaveSyncScheduler: PostSaveSyncScheduler,
    private val stockHelper: DispensacionStockHelper
) : ViewModel() {
    companion object {
        private const val ORIGEN_TIENDA = "Tienda"
        private const val ORIGEN_PACIENTE = "Paciente"
        private const val ORIGEN_TIENDA_LEGACY = "Nueva de Tienda"
        private const val ORIGEN_PACIENTE_LEGACY = "Traída por paciente"
    }

    private val _uiState = MutableStateFlow(DispensacionUiState())
    val uiState: StateFlow<DispensacionUiState> = _uiState.asStateFlow()
    private val _monturasActivas = MutableStateFlow<List<com.example.optoapp.data.Montura>>(emptyList())
    val monturasActivas: StateFlow<List<com.example.optoapp.data.Montura>> = _monturasActivas.asStateFlow()

    /** Última evaluación del paciente (por fecha) para refracción/DIP en ticket de laboratorio. */
    private val _ultimaEvaluacionTicket = MutableStateFlow<EvaluacionClinica?>(null)
    val ultimaEvaluacionTicket: StateFlow<EvaluacionClinica?> = _ultimaEvaluacionTicket.asStateFlow()

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

    fun loadUltimaEvaluacionParaTicket(pacienteId: String) {
        viewModelScope.launch {
            val list = repository.getEvaluacionesByPaciente(pacienteId).first()
            _ultimaEvaluacionTicket.value = list.maxByOrNull { it.fecha }
        }
    }

    fun loadDispensacion(dispensacionId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, generatedId = dispensacionId) }
            when (val result = repository.getDispensacionById(dispensacionId)) {
                is Resource.Success -> {
                    val d = result.data!!
                    val loadedPagos = repository.getPagosByDispensacion(dispensacionId).first()
                    val loadedItems = repository.getDispensacionItemsByDispensacion(dispensacionId)
                    val itemsUi = if (loadedItems.isNotEmpty()) {
                        loadedItems.map { it.toUi() }
                    } else {
                        // Backward compat: crear item desde los campos del entity
                        listOf(DispensacionItemUi(
                            tipoLente = d.tipoLente, distanciaLente = d.distanciaLente,
                            altura = d.altura, materialLente = d.materialLente,
                            tratamientos = d.tratamientos, colorLente = d.colorLente,
                            notasDiseno = d.notasDiseno, subTipoBifocal = d.subTipoBifocal
                        ))
                    }
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            ot = d.ot,
                            items = itemsUi,
                            origenMontura = normalizeOrigenMontura(d.origenMontura),
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

    private fun DispensacionItem.toUi() = DispensacionItemUi(
        id = id,
        tipoLente = tipoLente, distanciaLente = distanciaLente,
        altura = altura, materialLente = materialLente,
        tratamientos = tratamientos, colorLente = colorLente,
        notasDiseno = notasDiseno, subTipoBifocal = subTipoBifocal,
        monturaId = monturaId, origenMontura = origenMontura,
        tipoAro = tipoAro, materialMontura = materialMontura,
        descripcionMontura = descripcionMontura, tipoMontura = tipoMontura
    )

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

    //── Manejo de items (múltiples lentes) ──────────────────────────────────────

    /** Agrega un item de lente vacío a la lista. */
    fun addItem() {
        _uiState.update { s ->
            s.copy(items = s.items + DispensacionItemUi())
        }
    }

    /** Actualiza un item específico por índice. */
    fun updateItem(index: Int, item: DispensacionItemUi) {
        _uiState.update { s ->
            val updated = s.items.toMutableList()
            if (index in updated.indices) {
                updated[index] = item
            }
            s.copy(items = updated)
        }
    }

    /** Elimina un item por índice. Siempre mantener al menos un item vacío. */
    fun removeItem(index: Int) {
        _uiState.update { s ->
            val removed = s.items[index]
            // Solo marcar para borrado si tiene ID generado (ya fue persistido o se persistirá)
            val toDelete = if (removed.id.isNotEmpty()) s.itemsToDelete + removed.id else s.itemsToDelete
            val updated = s.items.toMutableList().apply { removeAt(index) }
            val finalItems = if (updated.isEmpty()) listOf(DispensacionItemUi()) else updated
            s.copy(items = finalItems, itemsToDelete = toDelete)
        }
    }

    /** Rellena OT con el siguiente correlativo OT-AAAA-#### según fecha y óptica activa. */
    fun suggestOt() {
        viewModelScope.launch {
            val oid = sessionManager.opticaId.first()
            val fecha = _uiState.value.fecha
            val next = repository.suggestNextOt(oid, fecha)
            _uiState.update { it.copy(ot = next, error = null) }
        }
    }

    fun saveDispensacion(pacienteId: String, dispensacionId: String?, onComplete: () -> Unit) {
        viewModelScope.launch {
            val s = _uiState.value

            // Validar items
            if (s.items.isEmpty()) {
                _uiState.update { it.copy(error = "Agrega al menos un lente a la dispensación.") }
                return@launch
            }
            val primerItem = s.items.first()
            val requiereAltura = primerItem.tipoLente == "Bifocal" || primerItem.tipoLente == "Progresivo" || primerItem.tipoLente == "Ocupacional"
            if (requiereAltura && primerItem.altura.isBlank()) {
                _uiState.update { it.copy(error = "La altura es obligatoria para ${primerItem.tipoLente}.") }
                return@launch
            }
            val alturaValida = primerItem.altura.trim().replace(",", ".").toDoubleOrNull()
            if (requiereAltura && (alturaValida == null || alturaValida <= 0.0)) {
                _uiState.update { it.copy(error = "Ingresa una altura válida en mm.") }
                return@launch
            }
            if (s.ot.isBlank()) {
                _uiState.update { it.copy(error = "La OT es obligatoria para guardar la dispensación.") }
                return@launch
            }
            val montoTotal = s.montoTotal.toDoubleOrNull()
            if (montoTotal == null || montoTotal <= 0.0) {
                _uiState.update { it.copy(error = FinanzasRemoteDefaults.Messages.MONTO_TOTAL_MAYOR_A_CERO) }
                return@launch
            }
            if (s.pagos.any { it.monto <= 0.0 }) {
                _uiState.update { it.copy(error = FinanzasRemoteDefaults.Messages.ABONO_MAYOR_A_CERO) }
                return@launch
            }
            val totalAbonos = s.pagos.sumOf { it.monto }
            if (totalAbonos > montoTotal) {
                _uiState.update { it.copy(error = FinanzasRemoteDefaults.Messages.ABONO_MAYOR_QUE_TOTAL) }
                return@launch
            }

            val currentOpticaId = sessionManager.opticaId.first()
            val finalId = dispensacionId ?: s.generatedId

            _uiState.update { it.copy(error = null) }
            val finalMontoPagado = totalAbonos

            // Cargar items anteriores si estamos editando (para stock)
            val itemsAnteriores = if (dispensacionId != null && dispensacionId != "null") {
                repository.getDispensacionItemsByDispensacion(dispensacionId)
            } else emptyList()

            val origenMonturaNormalizado = "Tienda" // El header lo hereda del primer item si aplica
            val primerItemMonturaId = if (primerItem.origenMontura == "Tienda") primerItem.monturaId else ""

            // El primer item va a los campos principales del entity (backward compat)
            val disp = DispensacionOptica(
                id = finalId,
                ot = s.ot.trim(),
                monturaId = primerItemMonturaId,
                pacienteId = pacienteId,
                fecha = s.fecha,
                opticaId = currentOpticaId,
                tipoLente = primerItem.tipoLente,
                materialLente = primerItem.materialLente,
                tratamientos = primerItem.tratamientos,
                colorLente = primerItem.colorLente,
                notasDiseno = primerItem.notasDiseno,
                subTipoBifocal = if (primerItem.tipoLente == "Bifocal") primerItem.subTipoBifocal else "",
                origenMontura = if (primerItem.origenMontura == "Tienda") "Tienda" else "Paciente",
                tipoAro = primerItem.tipoAro,
                materialMontura = primerItem.materialMontura,
                descripcionMontura = primerItem.descripcionMontura,
                tipoMontura = primerItem.tipoMontura,
                montoTotal = montoTotal,
                montoPagado = finalMontoPagado,
                metodoPago = "",
                estadoEntrega = s.estadoEntrega,
                fechaVencimientoGarantia = s.fechaVencimientoGarantia,
                distanciaLente = if (primerItem.tipoLente == "Monofocal") primerItem.distanciaLente else "",
                altura = if (requiereAltura) primerItem.altura.trim() else ""
            )

            // Stock: comparar monturas de tienda entre items anteriores y nuevos
            fun isTienda(m: DispensacionItem) = m.origenMontura == "Tienda" && m.monturaId.isNotBlank()
            fun isTiendaUi(m: DispensacionItemUi) = m.origenMontura == "Tienda" && m.monturaId.isNotBlank()

            val oldTiendaMonturas = itemsAnteriores.filter { isTienda(it) }.map { it.monturaId }
            val newTiendaMonturas = s.items.filter { isTiendaUi(it) }.map { it.monturaId }

            // Monturas que se quitaron → +1 stock
            val toAddStock = oldTiendaMonturas.filter { id -> id !in newTiendaMonturas }
            // Monturas que se agregaron → -1 stock
            val toRemoveStock = newTiendaMonturas.filter { id -> id !in oldTiendaMonturas }

            toAddStock.forEach { mid ->
                stockHelper.adjustStockAndRegistrarMovimiento(mid, currentOpticaId, 1, "AJUSTE", finalId, "Reversión por edición")
                postSaveSyncScheduler.scheduleInventarioSync(currentOpticaId)
            }
            toRemoveStock.forEach { mid ->
                val result = stockHelper.adjustStockAndRegistrarMovimiento(mid, currentOpticaId, -1, "SALIDA_VENTA", finalId, "Salida por venta")
                result.onFailure { e ->
                    _uiState.update { it.copy(error = e.message ?: "Stock insuficiente para una de las monturas seleccionadas.") }
                }
                if (_uiState.value.error != null) return@launch
                postSaveSyncScheduler.scheduleInventarioSync(currentOpticaId)
            }

            // Guardar/actualizar dispensación header
            if (dispensacionId != null && dispensacionId != "null") {
                repository.updateDispensacion(disp)
            } else {
                repository.insertDispensacion(disp)
            }

            // Guardar items (cada uno con su lente + montura)
            repository.deleteItemsByDispensacionId(finalId) // Limpiar y re-insertar
            s.items.forEachIndexed { _, itemUi ->
                val requiereAlturaItem = itemUi.tipoLente in setOf("Bifocal", "Progresivo", "Ocupacional")
                val item = DispensacionItem(
                    id = itemUi.id,
                    dispensacionId = finalId,
                    tipoLente = itemUi.tipoLente,
                    materialLente = itemUi.materialLente,
                    tratamientos = itemUi.tratamientos,
                    colorLente = itemUi.colorLente,
                    distanciaLente = if (itemUi.tipoLente == "Monofocal") itemUi.distanciaLente else "",
                    altura = if (requiereAlturaItem) itemUi.altura.trim() else "",
                    subTipoBifocal = if (itemUi.tipoLente == "Bifocal") itemUi.subTipoBifocal else "",
                    notasDiseno = itemUi.notasDiseno,
                    monturaId = itemUi.monturaId,
                    origenMontura = itemUi.origenMontura,
                    tipoAro = itemUi.tipoAro,
                    materialMontura = itemUi.materialMontura,
                    descripcionMontura = itemUi.descripcionMontura,
                    tipoMontura = itemUi.tipoMontura,
                    opticaId = currentOpticaId
                )
                repository.insertDispensacionItem(item)
            }

            // Eliminar items marcados (solo si estamos editando una existente)
            if (dispensacionId != null && dispensacionId != "null") {
                s.itemsToDelete.forEach { itemId ->
                    repository.deleteDispensacionItemById(itemId, currentOpticaId)
                }
            }

            // Guardar pagos vinculados a esta dispensación
            s.pagos.forEach { pago ->
                val pagoToSave = pago.copy(dispensacionId = finalId, opticaId = currentOpticaId)
                repository.insertPago(pagoToSave)
            }

            // Eliminar pagos marcados
            s.pagosToDelete.forEach { pago ->
                repository.deletePagoRegistrandoAnulacionEnCaja(pago, currentOpticaId)
            }

            postSaveSyncScheduler.scheduleFinanzasSync(currentOpticaId)

            onComplete()
        }
    }

    private fun normalizeOrigenMontura(value: String): String = when (value.trim()) {
        ORIGEN_TIENDA_LEGACY -> ORIGEN_TIENDA
        ORIGEN_PACIENTE_LEGACY -> ORIGEN_PACIENTE
        else -> value.trim()
    }

    private fun isOrigenTienda(value: String): Boolean =
        value == ORIGEN_TIENDA || value == ORIGEN_TIENDA_LEGACY
}
