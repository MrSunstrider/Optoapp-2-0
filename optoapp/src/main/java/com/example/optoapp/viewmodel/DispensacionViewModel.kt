package com.example.optoapp.viewmodel

import android.util.Log
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import java.util.UUID
import javax.inject.Inject
import com.example.optoapp.data.Pago
import com.example.optoapp.data.venta.Venta
import java.time.LocalDate
import com.example.optoapp.sync.PostSaveSyncScheduler
import com.example.optoapp.util.DateUtils
import com.example.optoapp.util.DispensacionStockHelper

data class DispensacionUiState(
    val pacienteNombre: String = "",
    val ot: String = "",
    /** El primer item es el lente principal; la lista permite adjuntar lentes adicionales a la misma OT (ej. bifocal + monofocal para cerca). */
    val items: List<DispensacionItemUi> = listOf(DispensacionItemUi()),
    /** Retenidos hasta que la transacción de guardado confirme la baja, evitando orphan rows en ediciones. */
    val itemsToDelete: List<String> = emptyList(),

    val origenMontura: String = "",
    val monturaId: String = "",
    val tipoAro: String = "",
    val materialMontura: String = "",
    val descripcionMontura: String = "",
    val tipoMontura: String = "",

    val montoTotal: String = "",
    val estadoEntrega: String = "Pendiente",
    val fechaEntrega: LocalDate? = null,
    val fecha: LocalDate = DateUtils.today(),
    val fechaVencimientoGarantia: LocalDate? = null,

    val isLoading: Boolean = false,
    val error: String? = null,

    val pagos: List<Pago> = emptyList(),
    val pagosToDelete: List<Pago> = emptyList(),
    val generatedId: String = ""
)

data class DispensacionItemUi(
    val id: String = UUID.randomUUID().toString(),
    val tipoLente: String = "",
    val distanciaLente: String = "",
    val altura: String = "",
    val materialLente: String = "",
    val tratamientos: List<String> = emptyList(),
    val colorLente: String = "",
    val notasDiseno: String = "",
    val filtroDiscromatopsiaTipo: String = "",
    val subTipoBifocal: String = "",
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
        private const val TAG = "DispensacionVM"
        private const val ORIGEN_TIENDA = "Tienda"
        private const val ORIGEN_PACIENTE = "Paciente"
        private const val ORIGEN_TIENDA_LEGACY = "Nueva de Tienda"
        private const val ORIGEN_PACIENTE_LEGACY = "Traída por paciente"
    }

    private val _uiState = MutableStateFlow(DispensacionUiState(generatedId = UUID.randomUUID().toString()))
    val uiState: StateFlow<DispensacionUiState> = _uiState.asStateFlow()
    private val _monturasActivas = MutableStateFlow<List<com.example.optoapp.data.Montura>>(emptyList())
    val monturasActivas: StateFlow<List<com.example.optoapp.data.Montura>> = _monturasActivas.asStateFlow()

    /** Precargada para el ticket de laboratorio: evita que el óptico cambie de pantalla consultando la última refracción y DIP del paciente. */
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
                    val d = result.data ?: return@launch
                    val loadedPagos = repository.getPagosByDispensacion(dispensacionId).first()
                        .filter { it.tipo != "Anulación" }
                    val loadedItems = repository.getDispensacionItemsByDispensacion(dispensacionId)
                    val itemsUi = if (loadedItems.isNotEmpty()) {
                        loadedItems.map { it.toUi() }
                    } else {
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
                            fechaEntrega = d.fechaEntrega,
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
        notasDiseno = notasDiseno, filtroDiscromatopsiaTipo = filtroDiscromatopsiaTipo, subTipoBifocal = subTipoBifocal,
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

    fun addItem() {
        _uiState.update { s ->
            s.copy(items = s.items + DispensacionItemUi())
        }
    }

    fun updateItem(index: Int, item: DispensacionItemUi) {
        _uiState.update { s ->
            val updated = s.items.toMutableList()
            if (index in updated.indices) {
                updated[index] = item
            }
            s.copy(items = updated)
        }
    }

    /** Siempre mantener al menos un item vacío para que el usuario pueda seguir agregando lentes. */
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
            for (item in s.items.drop(1)) {
                val requiereAlturaItem = item.tipoLente in setOf("Bifocal", "Progresivo", "Ocupacional")
                if (requiereAlturaItem && item.altura.isBlank()) {
                    _uiState.update { it.copy(error = "La altura es obligatoria para ${item.tipoLente}.") }
                    return@launch
                }
                val itemAlturaValida = item.altura.trim().replace(",", ".").toDoubleOrNull()
                if (requiereAlturaItem && (itemAlturaValida == null || itemAlturaValida <= 0.0)) {
                    _uiState.update { it.copy(error = "Ingresa una altura válida en mm.") }
                    return@launch
                }
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

            val itemsAnteriores = if (dispensacionId != null && dispensacionId != "null") {
                repository.getDispensacionItemsByDispensacion(dispensacionId)
            } else emptyList()

            val primerItemMonturaId = if (primerItem.origenMontura == "Tienda") primerItem.monturaId else ""
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
                montoPagado = s.pagos.filter { it.tipo != "Anulación" }.sumOf { it.monto },
                metodoPago = "",
                estadoEntrega = s.estadoEntrega,
                fechaEntrega = s.fechaEntrega,
                fechaVencimientoGarantia = s.fechaVencimientoGarantia,
                distanciaLente = if (primerItem.tipoLente == "Monofocal") primerItem.distanciaLente else "",
                altura = if (requiereAltura) primerItem.altura.trim() else ""
            )

            fun isTienda(m: DispensacionItem) = m.origenMontura == "Tienda" && m.monturaId.isNotBlank()
            fun isTiendaUi(m: DispensacionItemUi) = m.origenMontura == "Tienda" && m.monturaId.isNotBlank()

            val oldTiendaMonturas = itemsAnteriores.filter { isTienda(it) }.map { it.monturaId }
            val newTiendaMonturas = s.items.filter { isTiendaUi(it) }.map { it.monturaId }

            val toAddStock = oldTiendaMonturas.filter { id -> id !in newTiendaMonturas }
            val toRemoveStock = newTiendaMonturas.filter { id -> id !in oldTiendaMonturas }

            try {
                repository.runInTransaction {
                    kotlinx.coroutines.runBlocking {
                    // Stock adjustments MUST run inside the transaction for atomicity.
                    // If the transaction fails, stock is not modified.
                    toAddStock.forEach { mid ->
                        stockHelper.adjustStockAndRegistrarMovimiento(mid, currentOpticaId, 1, "AJUSTE", finalId, "Reversión por edición")
                    }
                    toRemoveStock.forEach { mid ->
                        val result = stockHelper.adjustStockAndRegistrarMovimiento(mid, currentOpticaId, -1, "SALIDA_VENTA", finalId, "Salida por venta")
                        if (result.isFailure) {
                            throw RuntimeException(result.exceptionOrNull()?.message ?: "Stock insuficiente para una de las monturas seleccionadas.")
                        }
                    }

                    if (dispensacionId != null && dispensacionId != "null") {
                        repository.updateDispensacion(disp)
                    } else {
                        repository.insertDispensacion(disp)
                    }

                    repository.deleteItemsByDispensacionId(finalId, currentOpticaId)
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

                    if (dispensacionId != null && dispensacionId != "null") {
                        s.itemsToDelete.forEach { itemId ->
                            repository.deleteDispensacionItemById(itemId, currentOpticaId)
                        }
                    }

                    s.pagos.forEach { pago ->
                        val pagoToSave = pago.copy(
                            dispensacionId = finalId,
                            opticaId = currentOpticaId,
                            ventaId = "v_disp_$finalId"
                        )
                        repository.insertPago(pagoToSave)
                    }

                    s.pagosToDelete.forEach { pago ->
                        repository.deletePagoRegistrandoAnulacionEnCaja(pago, currentOpticaId)
                    }

                    val costoMonturas = s.items
                        .filter { it.origenMontura in setOf(ORIGEN_TIENDA, ORIGEN_TIENDA_LEGACY) && it.monturaId.isNotBlank() }
                        .sumOf { item ->
                            _monturasActivas.value.find { m -> m.id == item.monturaId }?.costo ?: 0.0
                        }

                    val venta = Venta(
                        id = "v_disp_$finalId",
                        opticaId = currentOpticaId,
                        origen = "dispensacion",
                        origenId = finalId,
                        pacienteId = pacienteId,
                        ot = s.ot.trim(),
                        fecha = s.fecha,
                        fechaEntrega = s.fechaEntrega,
                        montoTotal = montoTotal,
                        costoUnitarioSnapshot = costoMonturas.takeIf { it > 0.0 },
                        estado = s.estadoEntrega
                    )
                    repository.upsertVenta(venta)
                    } // runBlocking
                } // runInTransaction
                // Stock sync scheduling only after successful transaction commit.
                toAddStock.forEach { mid -> postSaveSyncScheduler.scheduleInventarioSync(currentOpticaId) }
                toRemoveStock.forEach { mid -> postSaveSyncScheduler.scheduleInventarioSync(currentOpticaId) }
            } catch (e: RuntimeException) {
                Log.e(TAG, "save failed: stock error", e)
                _uiState.update { it.copy(error = "Stock insuficiente para una de las monturas seleccionadas.") }
                return@launch
            }

            postSaveSyncScheduler.scheduleFinanzasSync(currentOpticaId)

            onComplete()
        }
    }

    fun deleteDispensacion(dispensacionId: String, onComplete: () -> Unit) {
        viewModelScope.launch {
            val opticaId = sessionManager.opticaId.first()
            val result = repository.getDispensacionById(dispensacionId)
            if (result is Resource.Success && result.data != null) {
                repository.deleteDispensacion(result.data)
                repository.deleteVentaById("v_disp_$dispensacionId", dispensacionId, opticaId)
            }
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
