package com.example.optoapp.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.optoapp.data.DispensacionItem
import com.example.optoapp.data.DispensacionOptica
import com.example.optoapp.data.EvaluacionClinica
import com.example.optoapp.data.FinanzasRemoteDefaults
import com.example.optoapp.data.Montura
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
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import com.example.optoapp.data.Pago
import com.example.optoapp.data.regalodispensacion.RegaloDispensacionEntity
import com.example.optoapp.domain.CalcularMontoPagadoUseCase
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
    val generatedId: String = "",
    val montoPagado: Double = 0.0,
    val regalos: List<RegaloDispensacionUi> = emptyList(),
    val monturasDisponibles: List<Montura> = emptyList(),

    // ── Costos y Gastos (Phase 4) ──
    val evaluacionId: String? = null,
    val evaluacionesDisponibles: List<EvaluacionClinica> = emptyList()
)

data class RegaloDispensacionUi(
    val id: String = UUID.randomUUID().toString(),
    val productoId: String = "",
    val descripcion: String = "",
    val cantidad: Int = 1,
    val costoUnitario: Double = 0.0,
    val motivo: String = ""
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
    val tipoMontura: String = "",
    // ── Cost fields (auto-filled from matrix, manually overridable) ──
    val costoRealOd: Double? = null,
    val costoRealOi: Double? = null,
    val costoRealMontura: Double? = null,
    val costoRealBiselado: Double? = null,
    val costoRealLc: Double? = null
)

@HiltViewModel
class DispensacionViewModel @Inject constructor(
    private val repository: com.example.optoapp.data.OptoRepository,
    private val sessionManager: com.example.optoapp.data.SessionManager,
    private val postSaveSyncScheduler: PostSaveSyncScheduler,
    private val stockHelper: DispensacionStockHelper,
    private val calcularMontoPagadoUseCase: CalcularMontoPagadoUseCase
) : ViewModel() {
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

    // Reactive pagos sum maps for dynamic saldo computation (montoPagado/aCuenta are @Ignore)
    @OptIn(ExperimentalCoroutinesApi::class)
    val pagosSumByDispensacion: StateFlow<Map<String, Double>> = sessionManager.opticaId
        .flatMapLatest { opticaId ->
            repository.getAllPagosFlowForOptica(opticaId)
                .map { pagos ->
                    pagos.filter { it.tipo != "Anulación" && it.dispensacionId != null }
                        .groupBy { it.dispensacionId!! }
                        .mapValues { (_, pags) -> pags.sumOf { it.monto } }
                }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    @OptIn(ExperimentalCoroutinesApi::class)
    val aCuentaSumByServicio: StateFlow<Map<String, Double>> = sessionManager.opticaId
        .flatMapLatest { opticaId ->
            repository.getAllPagosFlowForOptica(opticaId)
                .map { pagos ->
                    pagos.filter { it.tipo != "Anulación" && it.servicioExtraId != null }
                        .groupBy { it.servicioExtraId!! }
                        .mapValues { (_, pags) -> pags.sumOf { it.monto } }
                }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

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
                    val computedMontoPagado = calcularMontoPagadoUseCase(dispensacionId)
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
                    val loadedRegalos = repository.getRegalosByDispensacionId(dispensacionId)
                    val regalosUi = loadedRegalos.map { entity ->
                        RegaloDispensacionUi(
                            id = entity.id,
                            productoId = entity.productoId,
                            descripcion = entity.descripcion,
                            cantidad = entity.cantidad,
                            costoUnitario = entity.costoUnitario,
                            motivo = entity.motivo
                        )
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
                            pagos = loadedPagos,
                            montoPagado = computedMontoPagado,
                            regalos = regalosUi,
                            evaluacionId = d.evaluacionId ?: ""
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
        descripcionMontura = descripcionMontura, tipoMontura = tipoMontura,
        costoRealOd = costoRealOd, costoRealOi = costoRealOi,
        costoRealMontura = costoRealMontura, costoRealBiselado = costoRealBiselado,
        costoRealLc = costoRealLc
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

    // ─── Regalo management ─────────────────────────────────────────────────────

    fun addRegalo(regalo: RegaloDispensacionUi) {
        _uiState.update { it.copy(regalos = it.regalos + regalo) }
    }

    fun removeRegalo(index: Int) {
        _uiState.update { s ->
            val updated = s.regalos.toMutableList()
            if (index in updated.indices) updated.removeAt(index)
            s.copy(regalos = updated)
        }
    }

    fun updateRegalo(index: Int, regalo: RegaloDispensacionUi) {
        _uiState.update { s ->
            val updated = s.regalos.toMutableList()
            if (index in updated.indices) updated[index] = regalo
            s.copy(regalos = updated)
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
                altura = if (requiereAltura) primerItem.altura.trim() else "",
                evaluacionId = s.evaluacionId?.ifBlank { null }
            )

            fun isTienda(m: DispensacionItem) = m.origenMontura == "Tienda" && m.monturaId.isNotBlank()
            fun isTiendaUi(m: DispensacionItemUi) = m.origenMontura == "Tienda" && m.monturaId.isNotBlank()

            val oldTiendaMonturas = itemsAnteriores.filter { isTienda(it) }.map { it.monturaId }
            val newTiendaMonturas = s.items.filter { isTiendaUi(it) }.map { it.monturaId }

            val toAddStock = oldTiendaMonturas.filter { id -> id !in newTiendaMonturas }
            val toRemoveStock = newTiendaMonturas.filter { id -> id !in oldTiendaMonturas }

            try {
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
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
                            val monturaInfo = repository.getMonturaById(mid, currentOpticaId).let { r ->
                                if (r is Resource.Success && r.data != null) "${r.data.sku} ${r.data.marca} ${r.data.modelo} (stock: ${r.data.stockActual})"
                                else mid.take(8)
                            }
                            throw RuntimeException("Stock insuficiente: $monturaInfo")
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
                            opticaId = currentOpticaId,
                            costoRealOd = itemUi.costoRealOd,
                            costoRealOi = itemUi.costoRealOi,
                            costoRealMontura = itemUi.costoRealMontura,
                            costoRealBiselado = itemUi.costoRealBiselado,
                            costoRealLc = itemUi.costoRealLc
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

                    // Save regalos
                    val existingRegalos = if (dispensacionId != null && dispensacionId != "null") {
                        repository.getRegalosByDispensacionId(finalId)
                    } else emptyList()
                    // Restore stock for removed regalos
                    existingRegalos.forEach { regalo ->
                        stockHelper.adjustStockAndRegistrarMovimiento(
                            regalo.productoId, currentOpticaId, regalo.cantidad,
                            "AJUSTE", finalId,
                            "Reversión por edición de regalos"
                        )
                    }
                    repository.deleteRegalosByDispensacionId(finalId)
                    s.regalos.forEach { regaloUi ->
                        val entity = RegaloDispensacionEntity(
                            id = regaloUi.id,
                            dispensacionId = finalId,
                            productoId = regaloUi.productoId,
                            cantidad = regaloUi.cantidad,
                            costoUnitario = regaloUi.costoUnitario,
                            descripcion = regaloUi.descripcion,
                            motivo = regaloUi.motivo,
                            opticaId = currentOpticaId
                        )
                        repository.insertRegalo(entity)
                        if (regaloUi.productoId.isNotBlank()) {
                            val stockResult = stockHelper.adjustStockAndRegistrarMovimiento(
                                regaloUi.productoId, currentOpticaId, -regaloUi.cantidad,
                                "SALIDA_VENTA", finalId,
                                "Salida por regalo de dispensación"
                            )
                            if (stockResult.isFailure) {
                                throw RuntimeException("Stock insuficiente para regalo: ${regaloUi.descripcion}")
                            }
                        }
                    }

                    }
                }
                }
                toAddStock.forEach { mid -> postSaveSyncScheduler.scheduleInventarioSync(currentOpticaId) }
                toRemoveStock.forEach { mid -> postSaveSyncScheduler.scheduleInventarioSync(currentOpticaId) }
            } catch (e: RuntimeException) {
                Log.e(TAG, "save failed", e)
                _uiState.update { it.copy(error = e.message ?: "Error al guardar la dispensación.") }
                return@launch
            }

            postSaveSyncScheduler.scheduleFinanzasSync(currentOpticaId)

            onComplete()
        }
    }

    fun deleteDispensacion(dispensacionId: String, onComplete: () -> Unit) {
        // Hard delete for mistakes: remove completely + revert stock
        // No inverse Pago, no financial trace — this never happened.
        viewModelScope.launch {
            val opticaId = sessionManager.opticaId.first()
            val regalos = repository.getRegalosByDispensacionId(dispensacionId)
            regalos.forEach { regalo ->
                stockHelper.adjustStockAndRegistrarMovimiento(
                    regalo.productoId, opticaId, regalo.cantidad,
                    "AJUSTE", dispensacionId,
                    "Devolución por borrado de dispensación"
                )
            }
            val result = repository.getDispensacionById(dispensacionId)
            if (result is Resource.Success && result.data != null) {
                repository.deleteDispensacion(result.data)
            }
            onComplete()
        }
    }

    fun crearReclamo(
        originalDispensacionId: String,
        nuevoMontoTotal: Double,
        onComplete: () -> Unit
    ) {
        viewModelScope.launch {
            val opticaId = sessionManager.opticaId.first()
            val result = repository.getDispensacionById(originalDispensacionId)
            if (result is Resource.Success && result.data != null) {
                val original = result.data
                val totalPagadoOriginal = calcularMontoPagadoUseCase(originalDispensacionId)

                // Mark original as reclamada
                repository.updateDispensacion(
                    original.copy(
                        estadoEntrega = "Reclamada",
                        updatedAt = java.time.Instant.now().toString()
                    )
                )

                // Create new dispensacion with reclamoOrigenId
                val newId = UUID.randomUUID().toString()
                val nuevaDisp = original.copy(
                    id = newId,
                    estadoEntrega = "Pendiente",
                    fecha = DateUtils.today(),
                    reclamoOrigenId = originalDispensacionId,
                    montoTotal = nuevoMontoTotal,
                    montoPagado = 0.0,
                    updatedAt = java.time.Instant.now().toString()
                )
                repository.insertDispensacion(nuevaDisp)

                // Calculate financial difference
                val diff = nuevoMontoTotal - totalPagadoOriginal
                when {
                    diff > 0 -> {
                        // Patient owes more — no refund, charge will happen on new disp
                    }
                    diff < 0 -> {
                        // Create refund Pago for the difference
                        val refundPago = Pago(
                            id = UUID.randomUUID().toString(),
                            dispensacionId = originalDispensacionId,
                            fecha = DateUtils.today(),
                            tipo = "Anulación",
                            monto = diff, // negative amount = refund
                            metodoPago = original.metodoPago,
                            nota = "Reembolso por reclamo de OT ${original.ot}",
                            opticaId = opticaId
                        )
                        repository.insertPago(refundPago)
                    }
                    // diff == 0 → no additional pago needed
                }
            }
            onComplete()
        }
    }

    fun anularDispensacion(dispensacionId: String, onComplete: () -> Unit) {
        viewModelScope.launch {
            val opticaId = sessionManager.opticaId.first()
            val result = repository.getDispensacionById(dispensacionId)
            if (result is Resource.Success && result.data != null) {
                val disp = result.data
                val totalPagado = calcularMontoPagadoUseCase(dispensacionId)

                val updatedDisp = disp.copy(
                    estadoEntrega = "Anulado",
                    updatedAt = java.time.Instant.now().toString()
                )

                // Create inverse Pago for the total montoPagado
                val anulacionPago = Pago(
                    id = UUID.randomUUID().toString(),
                    dispensacionId = dispensacionId,
                    fecha = DateUtils.today(),
                    tipo = "Anulación",
                    monto = -totalPagado,
                    metodoPago = disp.metodoPago,
                    nota = "Anulación de dispensación OT ${disp.ot}",
                    opticaId = opticaId
                )

                repository.updateDispensacion(updatedDisp)
                repository.insertPago(anulacionPago)

                // Revert stock for regalos
                val regalos = repository.getRegalosByDispensacionId(dispensacionId)
                regalos.forEach { regalo ->
                    stockHelper.adjustStockAndRegistrarMovimiento(
                        regalo.productoId, opticaId, regalo.cantidad,
                        "AJUSTE", dispensacionId,
                        "Reversión por anulación de dispensación"
                    )
                }
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

    // ─── Costos y Gastos: evaluacion_id linkage ───────────────────────────

    fun loadEvaluacionesDisponibles(pacienteId: String) {
        viewModelScope.launch {
            val list = repository.getEvaluacionesByPaciente(pacienteId).first()
            _uiState.update { it.copy(evaluacionesDisponibles = list) }
            // Preselect last evaluation
            val last = list.maxByOrNull { it.fecha }
            if (last != null) {
                _uiState.update { it.copy(evaluacionId = last.id) }
            }
        }
    }

    fun setEvaluacionId(evaluacionId: String?) {
        _uiState.update { it.copy(evaluacionId = evaluacionId) }
    }

    fun calculateCosts(itemIndex: Int) {
        val s = _uiState.value
        val evalId = s.evaluacionId ?: return
        viewModelScope.launch {
            when (val result = repository.getEvaluacionById(evalId)) {
                is Resource.Success -> {
                    val evaluacion = result.data ?: return@launch
                    if (itemIndex !in s.items.indices) return@launch

                    val item = s.items[itemIndex]
                    val opticaId = sessionManager.opticaId.first()

                    // Parse receta values from linked evaluation
                    val odEsf = evaluacion.recetaOdEsf.replace(",", ".").toDoubleOrNull()
                    val odCil = evaluacion.recetaOdCil.replace(",", ".").toDoubleOrNull()
                    val oiEsf = evaluacion.recetaOiEsf.replace(",", ".").toDoubleOrNull()
                    val oiCil = evaluacion.recetaOiCil.replace(",", ".").toDoubleOrNull()

                    var costoOd: Double? = null
                    var costoOi: Double? = null
                    var costoMontura: Double? = null
                    var costoBiselado: Double? = null
                    var costoLc: Double? = null

                    // LC branch: lookup by tipo_lente + material + laboratorio (R5)
                    val isLc = item.tipoLente.contains("Contacto", ignoreCase = true)
                    if (isLc) {
                        val lcTipo = when {
                            item.tipoLente.contains("Cosmét", ignoreCase = true) -> "lente_contacto_cosmetico"
                            item.tipoLente.contains("Medida", ignoreCase = true) -> "lente_contacto_medida"
                            else -> item.tipoLente // pass through as-is
                        }
                        val lcMaterial = evaluacion.lcMaterial.ifBlank { item.materialLente }
                        val lcLab = evaluacion.lcLaboratorio.ifBlank { null }
                        val lcLookup = repository.lookupCostoProductoLc(
                            material = lcMaterial,
                            tipoLente = lcTipo,
                            stockOFabricacion = "stock",
                            laboratorioId = lcLab
                        )
                        costoLc = lcLookup?.costoUnitario
                    } else {

                    val material = item.materialLente
                    val tipoLente = item.tipoLente

                    if (odEsf != null) {
                        val tipo = determineTipoLente(odEsf)
                        val serie = if (tipo == "stock" && odCil != null) determineSeriePorCilindro(odCil) else null
                        val lookupResult = repository.lookupCostoProducto(
                            material = material,
                            tipoLente = tipoLente,
                            stockOFabricacion = tipo,
                            tratamiento = item.tratamientos.firstOrNull(),
                            serie = serie
                        )
                        costoOd = lookupResult?.costoUnitario
                    }

                    if (oiEsf != null) {
                        val tipo = determineTipoLente(oiEsf)
                        val serie = if (tipo == "stock" && oiCil != null) determineSeriePorCilindro(oiCil) else null
                        val lookupResult = repository.lookupCostoProducto(
                            material = material,
                            tipoLente = tipoLente,
                            stockOFabricacion = tipo,
                            tratamiento = item.tratamientos.firstOrNull(),
                            serie = serie
                        )
                        costoOi = lookupResult?.costoUnitario
                    }

                    // Montura cost lookup: try costos_productos where stockOFabricacion='montura', fallback to monturas.costo
                    if (item.origenMontura == "Tienda" && item.monturaId.isNotBlank()) {
                        val monturaLookup = repository.lookupCostoProducto(
                            material = material,
                            tipoLente = "montura",
                            stockOFabricacion = "montura",
                            tratamiento = null,
                            serie = null
                        )
                        costoMontura = monturaLookup?.costoUnitario
                        if (costoMontura == null) {
                            val monturaResult = repository.getMonturaById(item.monturaId, opticaId)
                            if (monturaResult is Resource.Success) {
                                costoMontura = monturaResult.data?.costo
                            }
                        }

                        val tipoAro = normalizeTipoAro(item.tipoAro)
                        val biseladoLookup = repository.lookupCostoBiselado(
                            material = item.materialMontura.ifBlank { "Resina" },
                            tipoAro = tipoAro,
                            stockOFabricacion = "stock",
                            serie = 1,
                            altoIndice = null
                        )
                        costoBiselado = biseladoLookup?.costoPorPar
                    }
                    } // end else (!isLc)

                    // Auto-fill costs in item (R6: override persists even if matrix changes)
                    val updatedItem = item.copy(
                        costoRealOd = item.costoRealOd ?: costoOd,
                        costoRealOi = item.costoRealOi ?: costoOi,
                        costoRealMontura = item.costoRealMontura ?: costoMontura,
                        costoRealBiselado = item.costoRealBiselado ?: costoBiselado,
                        costoRealLc = item.costoRealLc ?: costoLc
                    )
                    updateItem(itemIndex, updatedItem)
                }
                else -> { /* no-op: evaluation not found */ }
            }
        }
    }

    private fun normalizeTipoAro(tipoAro: String): String = when {
        tipoAro.contains("Completo", ignoreCase = true) -> "aro_completo"
        tipoAro.contains("ranurado", ignoreCase = true) || tipoAro.contains("semi", ignoreCase = true) -> "ranurado"
        tipoAro.contains("al aire", ignoreCase = true) || tipoAro.contains("aire", ignoreCase = true) -> "al_aire"
        tipoAro.contains("taladro", ignoreCase = true) -> "taladro"
        else -> "aro_completo"
    }

    companion object {
        private const val TAG = "DispensacionVM"
        private const val ORIGEN_TIENDA = "Tienda"
        private const val ORIGEN_PACIENTE = "Paciente"
        private const val ORIGEN_TIENDA_LEGACY = "Nueva de Tienda"
        private const val ORIGEN_PACIENTE_LEGACY = "Traída por paciente"

        /** |esfera| ≤ 6.00 → stock, else → fabricacion */
        fun determineTipoLente(esfera: Double): String =
            if (kotlin.math.abs(esfera) <= 6.00) "stock" else "fabricacion"

        /**
         * Cylinder series for stock lenses:
         * 0 to -2.00 → 1ra (serie=1)
         * -2.25 to -4.00 → 2da (serie=2)
         * -4.25 to -6.00 → 3ra (serie=3)
         */
        fun determineSeriePorCilindro(cilindro: Double): Int? {
            val absCil = kotlin.math.abs(cilindro)
            return when {
                absCil <= 2.00 -> 1
                absCil <= 4.00 -> 2
                absCil <= 6.00 -> 3
                else -> null
            }
        }
    }
}
