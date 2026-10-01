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
import com.example.optoapp.data.Pago
import com.example.optoapp.data.Resource
import com.example.optoapp.data.regalodispensacion.RegaloDispensacionEntity
import com.example.optoapp.domain.CalcularMontoPagadoUseCase
import com.example.optoapp.domain.EliminarDispensacionUseCase
import com.example.optoapp.domain.LifecycleOutcome
import com.example.optoapp.domain.OrderStatusPolicy
import com.example.optoapp.domain.OrigenMontura
import com.example.optoapp.domain.PagoEffect
import com.example.optoapp.domain.ReclamarDispensacionUseCase
import com.example.optoapp.domain.ReclamoOutcome
import com.example.optoapp.domain.lastCreditMetodo
import com.example.optoapp.domain.auth.AuthorizationGuard
import com.example.optoapp.domain.inventario.InventarioItemKind
import com.example.optoapp.sync.PostSaveSyncScheduler
import com.example.optoapp.util.DateUtils
import com.example.optoapp.util.DispensacionStockHelper
import com.example.optoapp.util.MontoDraftFormatting
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject

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
    val motivoAnulacion: String? = null,
    val fechaAnulacion: LocalDate? = null,
    val reclamoOrigenId: String? = null,
    val reemplazo: ReclamoLink? = null,
    val reclamoOrigen: ReclamoLink? = null,

    val isLoading: Boolean = false,
    val error: String? = null,

    val pagos: List<Pago> = emptyList(),
    val pagosToDelete: List<Pago> = emptyList(),
    val generatedId: String = "",
    val montoPagado: Double = 0.0,
    val regalos: List<RegaloDispensacionUi> = emptyList(),
    val monturasDisponibles: List<Montura> = emptyList(),

    val evaluacionId: String? = null,
    val evaluacionesDisponibles: List<EvaluacionClinica> = emptyList(),
)

data class ReclamoLink(val id: String, val ot: String)

data class RegaloDispensacionUi(
    val id: String = UUID.randomUUID().toString(),
    val productoId: String = "",
    val descripcion: String = "",
    val cantidad: Int = 1,
    val costoUnitario: Double = 0.0,
    val motivo: String = "",
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
    val costoRealOd: Double? = null,
    val costoRealOi: Double? = null,
    val costoRealMontura: Double? = null,
    val costoRealBiselado: Double? = null,
    val costoRealLc: Double? = null,
)

@HiltViewModel
class DispensacionViewModel @Inject constructor(
    private val repository: com.example.optoapp.data.OptoRepository,
    private val sessionManager: com.example.optoapp.data.SessionManager,
    private val postSaveSyncScheduler: PostSaveSyncScheduler,
    private val stockHelper: DispensacionStockHelper,
    private val calcularMontoPagadoUseCase: CalcularMontoPagadoUseCase,
    private val anularDispensacionUseCase: com.example.optoapp.domain.AnularDispensacionUseCase,
    private val reclamarDispensacionUseCase: ReclamarDispensacionUseCase,
    private val costoProductoDao: com.example.optoapp.data.costoproducto.CostoProductoDao,
    private val costoBiseladoDao: com.example.optoapp.data.costobiselado.CostoBiseladoDao,
    private val eliminarDispensacionUseCase: EliminarDispensacionUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(DispensacionUiState(generatedId = UUID.randomUUID().toString()))
    val uiState: StateFlow<DispensacionUiState> = _uiState.asStateFlow()
    private val _lifecycle = MutableStateFlow(OrderLifecycleState())
    val lifecycle: StateFlow<OrderLifecycleState> = _lifecycle.asStateFlow()
    private val _monturasActivas = MutableStateFlow<List<com.example.optoapp.data.Montura>>(emptyList())
    val monturasActivas: StateFlow<List<com.example.optoapp.data.Montura>> = _monturasActivas.asStateFlow()

    // WHY: pre-loaded so the optician doesn't need to navigate away from the screen
    private val _ultimaEvaluacionTicket = MutableStateFlow<EvaluacionClinica?>(null)
    val ultimaEvaluacionTicket: StateFlow<EvaluacionClinica?> = _ultimaEvaluacionTicket.asStateFlow()

    init {
        viewModelScope.launch {
            sessionManager.opticaId.collect { opticaId ->
                repository.getMonturasByOptica(opticaId).collect { items ->
                    _monturasActivas.value = items.filter {
                        it.activo && InventarioItemKind.isArmazon(it.categoria)
                    }
                }
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun getDispensacionesByPaciente(pacienteId: String) = sessionManager.opticaId.flatMapLatest { opticaId ->
        repository.getDispensacionesByPaciente(pacienteId, opticaId)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val pagosSumByDispensacion: StateFlow<Map<String, Double>> = sessionManager.opticaId
        .flatMapLatest { opticaId ->
            repository.getAllPagosFlowForOptica(opticaId)
                .map { pagos ->
                    pagos.filter { it.dispensacionId != null }
                        .groupBy { it.dispensacionId!! }
                        .mapValues { (_, pags) -> pags.sumOf { PagoEffect.signedAmount(it.tipo, it.monto) } }
                }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    @OptIn(ExperimentalCoroutinesApi::class)
    val aCuentaSumByServicio: StateFlow<Map<String, Double>> = sessionManager.opticaId
        .flatMapLatest { opticaId ->
            repository.getAllPagosFlowForOptica(opticaId)
                .map { pagos ->
                    pagos.filter { it.servicioExtraId != null }
                        .groupBy { it.servicioExtraId!! }
                        .mapValues { (_, pags) -> pags.sumOf { PagoEffect.signedAmount(it.tipo, it.monto) } }
                }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    fun loadPacienteNombre(pacienteId: String) {
        viewModelScope.launch {
            when (val result = repository.getPacienteByIdScoped(pacienteId, sessionManager.opticaId.first())) {
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
            val opticaId = sessionManager.opticaId.first()
            val list = repository.getEvaluacionesByPaciente(pacienteId, opticaId).first()
            _ultimaEvaluacionTicket.value = list.maxByOrNull { it.fecha }
        }
    }

    fun loadDispensacion(dispensacionId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, generatedId = dispensacionId) }
            val opticaId = sessionManager.opticaId.first()
            when (val result = repository.getDispensacionById(dispensacionId, opticaId)) {
                is Resource.Success -> {
                    val d = result.data ?: return@launch
                    val loadedPagos = repository.getPagosByDispensacion(dispensacionId, opticaId).first()
                        .filter { it.tipo != "Anulación" }
                    val computedMontoPagado = calcularMontoPagadoUseCase(dispensacionId, opticaId)
                    val loadedItems = repository.getDispensacionItemsByDispensacion(dispensacionId, opticaId)
                    val itemsUi = if (loadedItems.isNotEmpty()) {
                        loadedItems.map { it.toUi() }
                    } else {
                        listOf(
                            DispensacionItemUi(
                                tipoLente = d.tipoLente,
                                distanciaLente = d.distanciaLente,
                                altura = d.altura,
                                materialLente = d.materialLente,
                                tratamientos = d.tratamientos,
                                colorLente = d.colorLente,
                                notasDiseno = d.notasDiseno,
                                subTipoBifocal = d.subTipoBifocal,
                            ),
                        )
                    }
                    val loadedRegalos = repository.getRegalosByDispensacionId(dispensacionId, opticaId)
                    val regalosUi = loadedRegalos.map { entity ->
                        RegaloDispensacionUi(
                            id = entity.id,
                            productoId = entity.productoId,
                            descripcion = entity.descripcion,
                            cantidad = entity.cantidad,
                            costoUnitario = entity.costoUnitario,
                            motivo = entity.motivo,
                        )
                    }
                    val reemplazo = if (d.estadoEntrega.trim() == OrderStatusPolicy.RECLAMADA) {
                        repository.getDispensacionByReclamoOrigenId(dispensacionId, opticaId)?.let { ReclamoLink(it.id, it.ot) }
                    } else {
                        null
                    }
                    val reclamoOrigen = d.reclamoOrigenId?.let { origenId ->
                        (repository.getDispensacionById(origenId, opticaId) as? Resource.Success)?.data
                            ?.let { ReclamoLink(it.id, it.ot) }
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
                            montoTotal = MontoDraftFormatting.formatDraft(d.montoTotal),
                            estadoEntrega = d.estadoEntrega,
                            fechaEntrega = d.fechaEntrega,
                            fecha = d.fecha,
                            fechaVencimientoGarantia = d.fechaVencimientoGarantia,
                            motivoAnulacion = d.motivoAnulacion,
                            fechaAnulacion = d.fechaAnulacion,
                            reclamoOrigenId = d.reclamoOrigenId,
                            reemplazo = reemplazo,
                            reclamoOrigen = reclamoOrigen,
                            pagos = loadedPagos,
                            montoPagado = computedMontoPagado,
                            regalos = regalosUi,
                            evaluacionId = d.evaluacionId?.takeIf { it.isNotBlank() },
                        )
                    }
                    _lifecycle.value = orderLifecycleState(
                        estado = d.estadoEntrega,
                        role = sessionManager.opticaRol.first(),
                        hasTrace = eliminarDispensacionUseCase.hasTrace(dispensacionId, opticaId),
                    )
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
        costoRealLc = costoRealLc,
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
            // Items without an id were never persisted, so there is no row to delete.
            val toDelete = if (removed.id.isNotEmpty()) s.itemsToDelete + removed.id else s.itemsToDelete
            val updated = s.items.toMutableList().apply { removeAt(index) }
            val finalItems = if (updated.isEmpty()) listOf(DispensacionItemUi()) else updated
            s.copy(items = finalItems, itemsToDelete = toDelete)
        }
    }

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
            if (_uiState.value.isLoading) return@launch
            _uiState.update { it.copy(isLoading = true, error = null) }
            val s = _uiState.value

            fun fail(message: String) {
                _uiState.update { it.copy(isLoading = false, error = message) }
            }

            if (s.items.isEmpty()) {
                fail("Agrega al menos un lente a la dispensación.")
                return@launch
            }
            val primerItem = s.items.first()
            val requiereAltura = primerItem.tipoLente == "Bifocal" || primerItem.tipoLente == "Multifocal" || primerItem.tipoLente == "Ocupacional"
            if (requiereAltura && primerItem.altura.isBlank()) {
                fail("La altura es obligatoria para ${primerItem.tipoLente}.")
                return@launch
            }
            val alturaValida = primerItem.altura.trim().replace(",", ".").toDoubleOrNull()
            if (requiereAltura && (alturaValida == null || alturaValida <= 0.0)) {
                fail("Ingresa una altura válida en mm.")
                return@launch
            }
            for (item in s.items.drop(1)) {
                val requiereAlturaItem = item.tipoLente in setOf("Bifocal", "Multifocal", "Ocupacional")
                if (requiereAlturaItem && item.altura.isBlank()) {
                    fail("La altura es obligatoria para ${item.tipoLente}.")
                    return@launch
                }
                val itemAlturaValida = item.altura.trim().replace(",", ".").toDoubleOrNull()
                if (requiereAlturaItem && (itemAlturaValida == null || itemAlturaValida <= 0.0)) {
                    fail("Ingresa una altura válida en mm.")
                    return@launch
                }
            }
            if (s.ot.isBlank()) {
                fail("La OT es obligatoria para guardar la dispensación.")
                return@launch
            }
            val isNew = dispensacionId == null || dispensacionId == "null"
            val montoTotal = if (isNew) {
                s.montoTotal.replace(",", ".").toDoubleOrNull()?.takeIf { it > 0.0 } ?: 0.0
            } else {
                val parsed = s.montoTotal.replace(",", ".").toDoubleOrNull()
                val zeroAllowed = parsed == 0.0 && dispensacionId != null && isClaimReplacement(dispensacionId)
                if (parsed == null || parsed < 0.0 || (parsed == 0.0 && !zeroAllowed)) {
                    fail(FinanzasRemoteDefaults.Messages.MONTO_TOTAL_MAYOR_A_CERO)
                    return@launch
                }
                parsed
            }
            if (s.pagos.any { it.monto <= 0.0 }) {
                fail(FinanzasRemoteDefaults.Messages.ABONO_MAYOR_A_CERO)
                return@launch
            }
            val totalAbonos = s.pagos.sumOf { PagoEffect.signedAmount(it.tipo, it.monto) }
            if (totalAbonos > montoTotal) {
                fail(FinanzasRemoteDefaults.Messages.ABONO_MAYOR_QUE_TOTAL)
                return@launch
            }

            val role: String
            val currentOpticaId: String
            val itemsAnteriores: List<com.example.optoapp.data.DispensacionItem>
            try {
                role = sessionManager.opticaRol.first()
                currentOpticaId = sessionManager.opticaId.first()
                itemsAnteriores = if (dispensacionId != null && dispensacionId != "null") {
                    repository.getDispensacionItemsByDispensacion(dispensacionId, currentOpticaId)
                } else {
                    emptyList()
                }
            } catch (e: Exception) {
                Log.e(TAG, "save prepare failed", e)
                fail(e.message ?: "Error al preparar el guardado.")
                return@launch
            }

            val finalId = dispensacionId ?: s.generatedId

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
                montoPagado = s.pagos.sumOf { PagoEffect.signedAmount(it.tipo, it.monto) },
                metodoPago = "",
                estadoEntrega = if (isNew) "Pendiente" else s.estadoEntrega,
                fechaEntrega = if (isNew) null else s.fechaEntrega,
                fechaVencimientoGarantia = s.fechaVencimientoGarantia,
                distanciaLente = if (primerItem.tipoLente == "Monofocal") primerItem.distanciaLente else "",
                altura = if (requiereAltura) primerItem.altura.trim() else "",
                evaluacionId = s.evaluacionId?.ifBlank { null },
            )

            fun isTienda(m: DispensacionItem) = m.origenMontura == "Tienda" && m.monturaId.isNotBlank()
            fun isTiendaUi(m: DispensacionItemUi) = m.origenMontura == "Tienda" && m.monturaId.isNotBlank()

            val oldTiendaMonturas = itemsAnteriores.filter { isTienda(it) }.map { it.monturaId }
            val newTiendaMonturas = s.items.filter { isTiendaUi(it) }.map { it.monturaId }

            val toAddStock = oldTiendaMonturas.filter { id -> id !in newTiendaMonturas }
            val toRemoveStock = newTiendaMonturas.filter { id -> id !in oldTiendaMonturas }

            try {
                if (dispensacionId != null && dispensacionId != "null") {
                    AuthorizationGuard.requireRole(role, setOf("admin", "gerente"), "editar dispensación")
                }
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    repository.runInTransaction {
                        kotlinx.coroutines.runBlocking {
                            val row = if (isNew) disp else persistedEditableRow(disp, currentOpticaId)
                            // Stock adjustments MUST run inside the transaction for atomicity.
                            // If the transaction fails, stock is not modified.
                            toAddStock.forEach { mid ->
                                stockHelper.adjustStockAndRegistrarMovimiento(mid, currentOpticaId, 1, "AJUSTE", finalId, "Reversión por edición")
                            }
                            toRemoveStock.forEach { mid ->
                                val result = stockHelper.adjustStockAndRegistrarMovimiento(mid, currentOpticaId, -1, "SALIDA_VENTA", finalId, "Salida por venta")
                                if (result.isFailure) {
                                    val monturaInfo = repository.getMonturaById(mid, currentOpticaId).let { r ->
                                        if (r is Resource.Success && r.data != null) {
                                            "${r.data.sku} ${r.data.marca} ${r.data.modelo} (stock: ${r.data.stockActual})"
                                        } else {
                                            mid.take(8)
                                        }
                                    }
                                    throw RuntimeException("Stock insuficiente: $monturaInfo")
                                }
                            }

                            if (dispensacionId != null && dispensacionId != "null") {
                                repository.updateDispensacion(row)
                            } else {
                                repository.insertDispensacion(row)
                            }

                            repository.deleteItemsByDispensacionId(finalId, currentOpticaId)
                            s.items.forEachIndexed { _, itemUi ->
                                val requiereAlturaItem = itemUi.tipoLente in setOf("Bifocal", "Multifocal", "Ocupacional")
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
                                    costoRealLc = itemUi.costoRealLc,
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
                                    ventaId = "v_disp_$finalId",
                                )
                                repository.insertPago(pagoToSave)
                            }

                            s.pagosToDelete.forEach { pago ->
                                repository.deletePagoRegistrandoAnulacionEnCaja(pago, currentOpticaId)
                            }

                            // Prefer DAO effect-aware net over wizard in-memory pagos (IF may have newer rows).
                            val montoPagadoNeto = calcularMontoPagadoUseCase(finalId, currentOpticaId)
                            repository.updateDispensacion(row.copy(montoPagado = montoPagadoNeto))
                        }
                    }
                }
                if (toAddStock.isNotEmpty() || toRemoveStock.isNotEmpty()) {
                    postSaveSyncScheduler.scheduleInventarioSync(currentOpticaId)
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                _uiState.update { it.copy(isLoading = false) }
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "save failed", e)
                _uiState.update { it.copy(isLoading = false, error = e.message ?: "Error al guardar la dispensación.") }
                return@launch
            }

            postSaveSyncScheduler.scheduleFinanzasSync(currentOpticaId)
            _uiState.update { it.copy(isLoading = false) }
            onComplete()
        }
    }

    /** A warranty remake may be free, so a claim replacement keeps a total of 0 on edit. */
    private suspend fun isClaimReplacement(dispensacionId: String): Boolean {
        val persisted = repository.getDispensacionById(dispensacionId, sessionManager.opticaId.first())
        return (persisted as? Resource.Success)?.data?.reclamoOrigenId != null
    }

    /**
     * `updateDispensacion` replaces the whole row, so fields the wizard does not edit
     * (claim linkage, cancellation metadata) are carried from the persisted row.
     */
    private suspend fun persistedEditableRow(edited: DispensacionOptica, opticaId: String): DispensacionOptica {
        val persisted = (repository.getDispensacionById(edited.id, opticaId) as? Resource.Success)?.data
            ?: throw IllegalStateException("Dispensación no encontrada.")
        OrderStatusPolicy.requireEditable(persisted.estadoEntrega, "editar la dispensación")
        return edited.copy(
            reclamoOrigenId = persisted.reclamoOrigenId,
            motivoAnulacion = persisted.motivoAnulacion,
            fechaAnulacion = persisted.fechaAnulacion,
        )
    }

    /** Claims the loading flag before launching so a second tap queued behind the first is ignored. */
    private fun tryStartAction(): Boolean {
        if (_uiState.value.isLoading) return false
        _uiState.update { it.copy(isLoading = true, error = null) }
        return true
    }

    fun deleteDispensacion(dispensacionId: String, onComplete: () -> Unit) {
        if (!tryStartAction()) return
        viewModelScope.launch {
            try {
                val role = sessionManager.opticaRol.first()
                AuthorizationGuard.requireRole(role, setOf("admin", "gerente"), "eliminar dispensación")
                eliminarDispensacionUseCase(dispensacionId, sessionManager.opticaId.first())
                _uiState.update { it.copy(isLoading = false) }
                onComplete()
            } catch (e: kotlinx.coroutines.CancellationException) {
                _uiState.update { it.copy(isLoading = false) }
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "delete failed", e)
                _uiState.update { it.copy(isLoading = false, error = e.message ?: "Error al eliminar la dispensación.") }
            }
        }
    }

    fun crearReclamo(
        originalDispensacionId: String,
        motivo: String,
        nuevoMontoTotal: Double,
        metodoReembolso: String,
        onCreated: (replacementId: String) -> Unit,
    ) {
        if (!tryStartAction()) return
        viewModelScope.launch {
            try {
                val role = sessionManager.opticaRol.first()
                AuthorizationGuard.requireRole(role, setOf("admin", "gerente"), "reclamar dispensación")
                val opticaId = sessionManager.opticaId.first()
                when (val outcome = reclamarDispensacionUseCase(originalDispensacionId, opticaId, motivo, nuevoMontoTotal, metodoReembolso)) {
                    is ReclamoOutcome.Created -> {
                        _uiState.update { it.copy(isLoading = false) }
                        onCreated(outcome.replacementId)
                    }
                    is ReclamoOutcome.AlreadyTerminal ->
                        _uiState.update { it.copy(isLoading = false, error = "Esta orden ya fue reclamada") }
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                _uiState.update { it.copy(isLoading = false) }
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "reclamo failed", e)
                _uiState.update { it.copy(isLoading = false, error = e.message ?: "Error al registrar el reclamo.") }
            }
        }
    }

    suspend fun metodoReembolsoSugerido(originalDispensacionId: String): String {
        val opticaId = sessionManager.opticaId.first()
        val pagos = repository.getPagosByDispensacion(originalDispensacionId, opticaId).first()
        return lastCreditMetodo(pagos) ?: METODO_REEMBOLSO_POR_DEFECTO
    }

    fun anularDispensacion(dispensacionId: String, motivo: String, onComplete: () -> Unit) {
        if (!tryStartAction()) return
        viewModelScope.launch {
            try {
                val role = sessionManager.opticaRol.first()
                AuthorizationGuard.requireRole(role, setOf("admin", "gerente"), "anular dispensación")
                val opticaId = sessionManager.opticaId.first()
                val outcome = anularDispensacionUseCase(dispensacionId, opticaId, motivo)
                val rejectedReclamada = outcome is LifecycleOutcome.AlreadyTerminal &&
                    outcome.estado == OrderStatusPolicy.RECLAMADA
                if (rejectedReclamada) {
                    _uiState.update { it.copy(isLoading = false, error = "No se puede anular una orden reclamada") }
                    return@launch
                }
                _uiState.update { it.copy(isLoading = false) }
                onComplete()
            } catch (e: kotlinx.coroutines.CancellationException) {
                _uiState.update { it.copy(isLoading = false) }
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "anular failed", e)
                _uiState.update { it.copy(isLoading = false, error = e.message ?: "Error al anular la dispensación.") }
            }
        }
    }

    private fun normalizeOrigenMontura(value: String): String = when (value.trim()) {
        OrigenMontura.TIENDA_LEGACY -> OrigenMontura.TIENDA
        ORIGEN_PACIENTE_LEGACY -> ORIGEN_PACIENTE
        else -> value.trim()
    }

    fun loadEvaluacionesDisponibles(pacienteId: String) {
        viewModelScope.launch {
            val opticaId = sessionManager.opticaId.first()
            val list = repository.getEvaluacionesByPaciente(pacienteId, opticaId).first()
            val last = list.maxByOrNull { it.fecha }
            _uiState.update { state ->
                val shouldAutoSelect = state.evaluacionId.isNullOrBlank()
                state.copy(
                    evaluacionesDisponibles = list,
                    evaluacionId = if (shouldAutoSelect && last != null) last.id else state.evaluacionId,
                )
            }
        }
    }

    fun setEvaluacionId(evaluacionId: String?) {
        _uiState.update { it.copy(evaluacionId = evaluacionId) }
    }

    fun calculateCosts(itemIndex: Int) {
        val s = _uiState.value
        val evalId = s.evaluacionId?.takeIf { it.isNotBlank() } ?: return
        viewModelScope.launch {
            when (val result = repository.getEvaluacionById(evalId, sessionManager.opticaId.first())) {
                is Resource.Success -> {
                    val evaluacion = result.data ?: return@launch
                    if (itemIndex !in s.items.indices) return@launch

                    val item = s.items[itemIndex]
                    val opticaId = sessionManager.opticaId.first()

                    val odEsf = evaluacion.recetaOdEsf?.replace(",", ".")?.toDoubleOrNull()
                    val odCil = evaluacion.recetaOdCil?.replace(",", ".")?.toDoubleOrNull()
                    val oiEsf = evaluacion.recetaOiEsf?.replace(",", ".")?.toDoubleOrNull()
                    val oiCil = evaluacion.recetaOiCil?.replace(",", ".")?.toDoubleOrNull()

                    var costoOd: Double? = null
                    var costoOi: Double? = null
                    var costoMontura: Double? = null
                    var costoBiselado: Double? = null
                    var costoLc: Double? = null

                    // R5: contact lenses are priced by type, material and laboratory, never by prescription.
                    val isLc = item.tipoLente.contains("Contacto", ignoreCase = true)
                    if (isLc) {
                        val lcTipo = when {
                            item.tipoLente.contains("Cosmét", ignoreCase = true) -> "lente_contacto_cosmetico"
                            item.tipoLente.contains("Medida", ignoreCase = true) -> "lente_contacto_medida"
                            else -> item.tipoLente
                        }
                        val lcMaterial = evaluacion.lcMaterial?.ifBlank { item.materialLente } ?: item.materialLente
                        val lcLab = evaluacion.lcLaboratorio?.ifBlank { null }
                        val lcLookup = costoProductoDao.lookupLc(
                            opticaId = opticaId,
                            material = lcMaterial,
                            tipoLente = lcTipo,
                            stockOFabricacion = "stock",
                            laboratorioId = lcLab,
                        )
                        costoLc = lcLookup?.costoUnitario
                    } else {
                        val material = item.materialLente
                        val tipoLente = item.tipoLente

                        val tratamientoStr = item.tratamientos.sorted().joinToString(" + ").ifBlank { null }

                        if (odEsf != null) {
                            val tipo = determineTipoLente(odEsf, odCil)
                            val serie = determineSeriePorCilindro(odCil)
                            val lookupResult = costoProductoDao.lookup(
                                opticaId = opticaId,
                                material = material,
                                tipoLente = tipoLente,
                                stockOFabricacion = tipo,
                                tratamiento = tratamientoStr,
                                serie = serie,
                            )
                            costoOd = lookupResult?.costoUnitario
                        }

                        if (oiEsf != null) {
                            val tipo = determineTipoLente(oiEsf, oiCil)
                            val serie = determineSeriePorCilindro(oiCil)
                            val lookupResult = costoProductoDao.lookup(
                                opticaId = opticaId,
                                material = material,
                                tipoLente = tipoLente,
                                stockOFabricacion = tipo,
                                tratamiento = tratamientoStr,
                                serie = serie,
                            )
                            costoOi = lookupResult?.costoUnitario
                        }

                        // The optica's cost matrix wins over the frame's catalog cost when both exist.
                        if (item.origenMontura == "Tienda" && item.monturaId.isNotBlank()) {
                            val monturaLookup = costoProductoDao.lookup(
                                opticaId = opticaId,
                                material = material,
                                tipoLente = "montura",
                                stockOFabricacion = "montura",
                                tratamiento = null,
                                serie = null,
                            )
                            costoMontura = monturaLookup?.costoUnitario
                            if (costoMontura == null) {
                                val monturaResult = repository.getMonturaById(item.monturaId, opticaId)
                                if (monturaResult is Resource.Success) {
                                    costoMontura = monturaResult.data?.costo
                                }
                            }

                            val tipoAro = normalizeTipoAro(item.tipoAro)
                            val biseladoTipo = when {
                                odEsf != null -> determineTipoLente(odEsf, odCil)
                                oiEsf != null -> determineTipoLente(oiEsf, oiCil)
                                else -> "stock"
                            }
                            val biseladoLookup = costoBiseladoDao.lookup(
                                opticaId = opticaId,
                                material = item.materialMontura.ifBlank { "Resina" },
                                tipoAro = tipoAro,
                                stockOFabricacion = biseladoTipo,
                                serie = 1,
                                altoIndice = null,
                            )
                            costoBiselado = biseladoLookup?.costoPorPar
                        }
                    }

                    // R6: a manual cost override persists even if the cost matrix changes.
                    val updatedItem = item.copy(
                        costoRealOd = item.costoRealOd ?: costoOd,
                        costoRealOi = item.costoRealOi ?: costoOi,
                        costoRealMontura = item.costoRealMontura ?: costoMontura,
                        costoRealBiselado = item.costoRealBiselado ?: costoBiselado,
                        costoRealLc = item.costoRealLc ?: costoLc,
                    )
                    updateItem(itemIndex, updatedItem)
                }
                else -> Unit
            }
        }
    }

    private fun normalizeTipoAro(tipoAro: String): String = when {
        tipoAro.contains("Completo", ignoreCase = true) -> "aro_completo"
        tipoAro.contains("Semi", ignoreCase = true) -> "semi_aire"
        tipoAro.contains("aire", ignoreCase = true) -> "al_aire"
        else -> "aro_completo"
    }

    companion object {
        private const val TAG = "DispensacionVM"
        private const val ORIGEN_PACIENTE = "Paciente"
        private const val ORIGEN_PACIENTE_LEGACY = "Traída por paciente"
        private const val METODO_REEMBOLSO_POR_DEFECTO = "Efectivo"

        fun determineTipoLente(esfera: Double, cilindro: Double?): String {
            val absEsf = kotlin.math.abs(esfera)
            val absCil = cilindro?.let { kotlin.math.abs(it) } ?: 0.0
            return if (absEsf > 6.00 || absCil > 6.00) "fabricacion" else "stock"
        }

        /**
         * Cylinder series for stock lenses:
         * null or 0 to -2.00 → 1ra (serie=1)
         * -2.25 to -4.00 → 2da (serie=2)
         * -4.25 to -6.00 → 3ra (serie=3)
         */
        fun determineSeriePorCilindro(cilindro: Double?): Int? = when {
            cilindro == null || kotlin.math.abs(cilindro) <= 2.00 -> 1
            kotlin.math.abs(cilindro) <= 4.00 -> 2
            kotlin.math.abs(cilindro) <= 6.00 -> 3
            else -> null
        }
    }
}
