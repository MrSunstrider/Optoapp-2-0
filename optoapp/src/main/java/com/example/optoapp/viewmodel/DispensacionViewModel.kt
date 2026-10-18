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
import com.example.optoapp.domain.CalcularCostoItemUseCase
import com.example.optoapp.domain.CalcularMontoPagadoUseCase
import com.example.optoapp.domain.CapturaCosto
import com.example.optoapp.domain.CapturaCostoItem
import com.example.optoapp.domain.ComponenteCosto
import com.example.optoapp.domain.CostoFechado
import com.example.optoapp.domain.CostoVentaCalculator
import com.example.optoapp.domain.EliminarDispensacionUseCase
import com.example.optoapp.domain.LifecycleOutcome
import com.example.optoapp.domain.OrderStatusPolicy
import com.example.optoapp.domain.OrigenBiselado
import com.example.optoapp.domain.OrigenComponente
import com.example.optoapp.domain.OrigenMontura
import com.example.optoapp.domain.PagoEffect
import com.example.optoapp.domain.RecetaCosto
import com.example.optoapp.domain.ReclamarDispensacionUseCase
import com.example.optoapp.domain.ReclamoOutcome
import com.example.optoapp.domain.ResumenRecalcMarker
import com.example.optoapp.domain.SyncBiseladoGastosDerivados
import com.example.optoapp.domain.VendedorAttribution
import com.example.optoapp.domain.lastCreditMetodo
import com.example.optoapp.domain.auth.AuthorizationGuard
import com.example.optoapp.domain.inventario.InventarioItemKind
import com.example.optoapp.sync.PostSaveSyncScheduler
import com.example.optoapp.util.DateUtils
import com.example.optoapp.util.DispensacionStockHelper
import com.example.optoapp.util.MontoDraftFormatting
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
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
    val infoMessage: String? = null,

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
    val altoIndice: String = "",
    val curvaBase: String = "",
    val forzarStockIgnorandoAstigmatismo: Boolean = false,
    val ojosLente: String = com.example.optoapp.domain.OjosLente.AMBOS,
    val origenBiselado: String = com.example.optoapp.domain.OrigenBiselado.TERCERO,
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
    val costoOverrides: Map<ComponenteCosto, Double> = emptyMap(),
    val costoOrigen: String? = null,
    // Session-only because the schema stores a single origin per item, not one per component.
    val costoOrigenes: Map<ComponenteCosto, OrigenComponente> = emptyMap(),
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
    private val calcularCostoItem: CalcularCostoItemUseCase,
    private val guardarBiseladoDesdeOt: com.example.optoapp.domain.GuardarBiseladoDesdeOtUseCase,
    private val guardarCostoLenteDesdeOt: com.example.optoapp.domain.GuardarCostoLenteDesdeOtUseCase,
    private val eliminarDispensacionUseCase: EliminarDispensacionUseCase,
    private val resumenRecalcMarker: ResumenRecalcMarker,
    private val supabase: SupabaseClient,
    private val configuracionFinancieraDao: com.example.optoapp.data.configuracionfinanciera.ConfiguracionFinancieraDao,
    private val gastoOperativoDao: com.example.optoapp.data.gastooperativo.GastoOperativoDao,
) : ViewModel() {
    private val _uiState = MutableStateFlow(DispensacionUiState(generatedId = UUID.randomUUID().toString()))
    val uiState: StateFlow<DispensacionUiState> = _uiState.asStateFlow()
    private val _lifecycle = MutableStateFlow(OrderLifecycleState())
    val lifecycle: StateFlow<OrderLifecycleState> = _lifecycle.asStateFlow()
    private val _monturasActivas = MutableStateFlow<List<com.example.optoapp.data.Montura>>(emptyList())
    val monturasActivas: StateFlow<List<com.example.optoapp.data.Montura>> = _monturasActivas.asStateFlow()
    private var origenBiseladoDefault: String = com.example.optoapp.domain.OrigenBiselado.TERCERO

    // WHY: pre-loaded so the optician doesn't need to navigate away from the screen
    private val _ultimaEvaluacionTicket = MutableStateFlow<EvaluacionClinica?>(null)
    val ultimaEvaluacionTicket: StateFlow<EvaluacionClinica?> = _ultimaEvaluacionTicket.asStateFlow()

    init {
        viewModelScope.launch {
            val opticaId = sessionManager.opticaId.first()
            origenBiseladoDefault = com.example.optoapp.domain.OrigenBiselado.normalize(
                configuracionFinancieraDao.getByOpticaIdOnce(opticaId)?.origenBiseladoDefault,
            )
            // WHY: DispensacionItemUi.id defaults to a UUID, so id.isEmpty() never fires; reseed
            // the single pristine first line after config loads so origenBiseladoDefault applies.
            _uiState.update { state ->
                if (state.items.size == 1 && isPristineBlankFirstLine(state.items.first())) {
                    state.copy(items = listOf(blankItem()))
                } else {
                    state
                }
            }
        }
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
                            blankItem().copy(
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
        altoIndice = altoIndice.orEmpty(),
        curvaBase = curvaBase.orEmpty(),
        forzarStockIgnorandoAstigmatismo = forzarStockIgnorandoAstigmatismo,
        ojosLente = com.example.optoapp.domain.OjosLente.normalize(ojosLente),
        origenBiselado = com.example.optoapp.domain.OrigenBiselado.normalize(origenBiselado),
        monturaId = monturaId, origenMontura = origenMontura,
        tipoAro = tipoAro, materialMontura = materialMontura,
        descripcionMontura = descripcionMontura, tipoMontura = tipoMontura,
        costoRealOd = costoRealOd, costoRealOi = costoRealOi,
        costoRealMontura = costoRealMontura, costoRealBiselado = costoRealBiselado,
        costoRealLc = costoRealLc,
        costoOrigen = costoOrigen,
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
            s.copy(items = s.items + blankItem())
        }
    }

    private fun blankItem() = DispensacionItemUi(origenBiselado = origenBiseladoDefault)

    private fun isPristineBlankFirstLine(item: DispensacionItemUi): Boolean =
        item.tipoLente.isBlank() &&
            item.materialLente.isBlank() &&
            item.monturaId.isBlank() &&
            item.distanciaLente.isBlank() &&
            item.tratamientos.isEmpty() &&
            item.costoRealOd == null &&
            item.costoRealOi == null &&
            item.costoRealMontura == null &&
            item.costoRealBiselado == null &&
            item.costoRealLc == null

    private suspend fun clearLinkedBiseladoGastos(opticaId: String, deletedItemIds: List<String>) {
        if (deletedItemIds.isEmpty()) return
        syncBiseladoGastosDerivados(
            opticaId = opticaId,
            ot = "",
            fecha = LocalDate.now(),
            items = emptyList(),
            deletedItemIds = deletedItemIds,
        )
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
            val finalItems = if (updated.isEmpty()) listOf(blankItem()) else updated
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
            val receta: RecetaCosto?
            val recetaPrevia: RecetaCosto?
            try {
                role = sessionManager.opticaRol.first()
                currentOpticaId = sessionManager.opticaId.first()
                itemsAnteriores = if (dispensacionId != null && dispensacionId != "null") {
                    repository.getDispensacionItemsByDispensacion(dispensacionId, currentOpticaId)
                } else {
                    emptyList()
                }
                receta = recetaDe(s.evaluacionId, currentOpticaId)
                recetaPrevia = recetaPrevia(dispensacionId.takeUnless { isNew }, s.evaluacionId, receta, currentOpticaId)
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
                vendedorUserId = if (isNew) {
                    VendedorAttribution.forCreate(runCatching { supabase.auth.currentUserOrNull()?.id }.getOrNull())
                } else {
                    null
                },
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
                            val persisted = if (isNew) null else persistedEditable(finalId, currentOpticaId)
                            val row = persisted?.let { editableRow(disp, it) } ?: disp
                            // Runs inside the transaction so a failed save never leaves stock adjusted.
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

                            val previos = itemsAnteriores.associateBy { it.id }
                            repository.deleteItemsByDispensacionId(finalId, currentOpticaId)
                            val itemsGuardados = s.items.map { itemUi ->
                                val item = capturarCosto(
                                    itemUi.toEntity(finalId, currentOpticaId),
                                    previos[itemUi.id],
                                    itemUi.costoOverrides,
                                    receta,
                                    recetaPrevia,
                                    s.fecha,
                                )
                                repository.insertDispensacionItem(item)
                                item
                            }
                            syncBiseladoGastosDerivados(
                                opticaId = currentOpticaId,
                                ot = row.ot,
                                fecha = row.fecha,
                                items = itemsGuardados,
                                deletedItemIds = s.itemsToDelete,
                            )
                            resumenRecalcMarker.registrar(
                                currentOpticaId,
                                ResumenRecalcMarker.CAUSA_DISPENSACION,
                                finalId,
                                anterior = persisted?.let { CostoFechado(it.fecha, costoItems(itemsAnteriores)) },
                                nuevo = CostoFechado(row.fecha, costoItems(itemsGuardados)),
                            )

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

                            // Uses the DAO net because Información Financiera may have saved newer pagos than the wizard holds.
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
            onComplete()
            _uiState.update { it.copy(isLoading = false) }
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
    private suspend fun persistedEditable(id: String, opticaId: String): DispensacionOptica {
        val persisted = (repository.getDispensacionById(id, opticaId) as? Resource.Success)?.data
            ?: throw IllegalStateException("Dispensación no encontrada.")
        OrderStatusPolicy.requireEditable(persisted.estadoEntrega, "editar la dispensación")
        return persisted
    }

    private fun editableRow(edited: DispensacionOptica, persisted: DispensacionOptica): DispensacionOptica = edited.copy(
        reclamoOrigenId = persisted.reclamoOrigenId,
        motivoAnulacion = persisted.motivoAnulacion,
        fechaAnulacion = persisted.fechaAnulacion,
        vendedorUserId = VendedorAttribution.forEdit(edited.vendedorUserId, persisted.vendedorUserId),
    )

    // Gifts are left out: this save never changes them, so they cannot move the day's cost.
    private fun costoItems(items: List<DispensacionItem>): Double =
        CostoVentaCalculator.dispensacion(items, costoUnitarioSnapshot = null, regalos = emptyList()).costo

    /** Claims the loading flag before launching so a second tap queued behind the first is ignored. */
    private fun tryStartAction(): Boolean {
        if (_uiState.value.isLoading) return false
        _uiState.update { it.copy(isLoading = true, error = null, infoMessage = null) }
        return true
    }

    fun deleteDispensacion(dispensacionId: String, onComplete: () -> Unit) {
        if (!tryStartAction()) return
        viewModelScope.launch {
            try {
                val role = sessionManager.opticaRol.first()
                AuthorizationGuard.requireRole(role, setOf("admin", "gerente"), "eliminar dispensación")
                val opticaId = sessionManager.opticaId.first()
                // Capture item ids before eliminar cascades them away.
                val deletedItemIds = repository.getDispensacionItemsByDispensacion(dispensacionId, opticaId)
                    .map { it.id }
                eliminarDispensacionUseCase(dispensacionId, opticaId)
                clearLinkedBiseladoGastos(opticaId, deletedItemIds)
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
                        _uiState.update { it.copy(isLoading = false, infoMessage = "Esta orden ya fue reclamada") }
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

    suspend fun metodoReembolsoSugerido(originalDispensacionId: String): String = runCatching {
        val opticaId = sessionManager.opticaId.first()
        lastCreditMetodo(repository.getPagosByDispensacion(originalDispensacionId, opticaId).first())
    }.onFailure { e ->
        if (e is kotlinx.coroutines.CancellationException) throw e
        Log.e(TAG, "suggested refund method lookup failed", e)
    }.getOrNull() ?: METODO_REEMBOLSO_POR_DEFECTO

    fun anularDispensacion(dispensacionId: String, motivo: String, onComplete: () -> Unit) {
        if (!tryStartAction()) return
        viewModelScope.launch {
            try {
                val role = sessionManager.opticaRol.first()
                AuthorizationGuard.requireRole(role, setOf("admin", "gerente"), "anular dispensación")
                val opticaId = sessionManager.opticaId.first()
                when (val outcome = anularDispensacionUseCase(dispensacionId, opticaId, motivo)) {
                    LifecycleOutcome.Applied -> {
                        val deletedItemIds = repository.getDispensacionItemsByDispensacion(dispensacionId, opticaId)
                            .map { it.id }
                        clearLinkedBiseladoGastos(opticaId, deletedItemIds)
                        _uiState.update { it.copy(isLoading = false) }
                    }
                    is LifecycleOutcome.AlreadyTerminal -> {
                        if (outcome.estado == OrderStatusPolicy.RECLAMADA) {
                            _uiState.update { it.copy(isLoading = false, error = "No se puede anular una orden reclamada") }
                            return@launch
                        }
                        _uiState.update { it.copy(isLoading = false, infoMessage = "Esta orden ya fue anulada") }
                    }
                }
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

    fun setCostoOverride(itemIndex: Int, componente: ComponenteCosto, valor: Double) {
        val item = _uiState.value.items.getOrNull(itemIndex) ?: return
        updateItem(itemIndex, item.copy(costoOverrides = item.costoOverrides + (componente to valor)))
    }

    fun persistBiseladoToMatrix(itemIndex: Int) {
        viewModelScope.launch {
            val s = _uiState.value
            val itemUi = s.items.getOrNull(itemIndex) ?: return@launch
            val costo = itemUi.costoOverrides[ComponenteCosto.BISELADO]
                ?: itemUi.costoRealBiselado
                ?: return@launch
            val opticaId = sessionManager.opticaId.first()
            val receta = recetaDe(s.evaluacionId, opticaId)
            applyMatrizResult(
                guardarBiseladoDesdeOt(
                    opticaId = opticaId,
                    item = itemUi.toEntity(s.generatedId, opticaId),
                    costoPorPar = costo,
                    receta = receta,
                    hoy = s.fecha,
                ),
                opticaId,
                okMessage = "Biselado guardado en Costos y gastos",
            )
        }
    }

    fun persistCostoLenteToMatrix(itemIndex: Int, ojo: ComponenteCosto) {
        if (ojo != ComponenteCosto.OD && ojo != ComponenteCosto.OI) return
        viewModelScope.launch {
            val s = _uiState.value
            val itemUi = s.items.getOrNull(itemIndex) ?: return@launch
            val costo = when (ojo) {
                ComponenteCosto.OD -> itemUi.costoOverrides[ComponenteCosto.OD] ?: itemUi.costoRealOd
                ComponenteCosto.OI -> itemUi.costoOverrides[ComponenteCosto.OI] ?: itemUi.costoRealOi
                else -> null
            } ?: return@launch
            val opticaId = sessionManager.opticaId.first()
            val receta = recetaDe(s.evaluacionId, opticaId)
            val label = if (ojo == ComponenteCosto.OD) "OD" else "OI"
            applyMatrizResult(
                guardarCostoLenteDesdeOt(
                    opticaId = opticaId,
                    item = itemUi.toEntity(s.generatedId, opticaId),
                    ojo = ojo,
                    costoUnitario = costo,
                    receta = receta,
                    hoy = s.fecha,
                ),
                opticaId,
                okMessage = "Costo lente $label guardado en Costos y gastos",
            )
        }
    }

    private fun applyMatrizResult(
        result: com.example.optoapp.domain.ResultadoCostoMatriz,
        opticaId: String,
        okMessage: String,
    ) {
        when (result) {
            is com.example.optoapp.domain.ResultadoCostoMatriz.Creado,
            is com.example.optoapp.domain.ResultadoCostoMatriz.Versionado,
            -> {
                _uiState.update { it.copy(infoMessage = okMessage, error = null) }
                postSaveSyncScheduler.scheduleFinanzasSync(opticaId)
            }
            is com.example.optoapp.domain.ResultadoCostoMatriz.Rechazado -> {
                _uiState.update { it.copy(error = result.motivo) }
            }
        }
    }

    // Re-runs the preview because clearing must show the same fallback (lookup or persisted snapshot) the save applies.
    fun clearCostoOverride(itemIndex: Int, componente: ComponenteCosto) {
        val item = _uiState.value.items.getOrNull(itemIndex) ?: return
        if (componente !in item.costoOverrides) return
        updateItem(itemIndex, item.copy(costoOverrides = item.costoOverrides - componente))
        calculateCosts(itemIndex)
    }

    fun calculateCosts(itemIndex: Int) {
        viewModelScope.launch {
            val s = _uiState.value
            val itemUi = s.items.getOrNull(itemIndex) ?: return@launch
            val opticaId = sessionManager.opticaId.first()
            val receta = recetaDe(s.evaluacionId, opticaId)
            val previo = repository.getDispensacionItemsByDispensacion(s.generatedId, opticaId).find { it.id == itemUi.id }
            val recetaPrevia = recetaPrevia(previo?.dispensacionId, s.evaluacionId, receta, opticaId)
            val captura = capturarDetalle(
                itemUi.toEntity(s.generatedId, opticaId),
                previo,
                itemUi.costoOverrides,
                receta,
                recetaPrevia,
                s.fecha,
            )
            val capturado = captura.item
            val actual = _uiState.value.items.getOrNull(itemIndex)?.takeIf { it.id == itemUi.id } ?: return@launch
            updateItem(
                itemIndex,
                actual.copy(
                    costoRealOd = capturado.costoRealOd,
                    costoRealOi = capturado.costoRealOi,
                    costoRealMontura = capturado.costoRealMontura,
                    costoRealBiselado = capturado.costoRealBiselado,
                    costoRealLc = capturado.costoRealLc,
                    costoOrigen = capturado.costoOrigen,
                    costoOrigenes = captura.origenes,
                ),
            )
        }
    }

    private suspend fun capturarCosto(
        item: DispensacionItem,
        previo: DispensacionItem?,
        overrides: Map<ComponenteCosto, Double>,
        receta: RecetaCosto?,
        recetaPrevia: RecetaCosto?,
        fecha: LocalDate,
    ): DispensacionItem = capturarDetalle(item, previo, overrides, receta, recetaPrevia, fecha).item

    private suspend fun capturarDetalle(
        item: DispensacionItem,
        previo: DispensacionItem?,
        overrides: Map<ComponenteCosto, Double>,
        receta: RecetaCosto?,
        recetaPrevia: RecetaCosto?,
        fecha: LocalDate,
    ): CapturaCosto = CapturaCostoItem.capturarDetalle(item, previo, overrides, receta, recetaPrevia) {
        calcularCostoItem(item.opticaId, it, receta, fecha)
    }

    private suspend fun recetaDe(evaluacionId: String?, opticaId: String): RecetaCosto? {
        val id = evaluacionId?.takeIf { it.isNotBlank() } ?: return null
        return (repository.getEvaluacionById(id, opticaId) as? Resource.Success)?.data?.let(RecetaCosto::from)
    }

    // Reuses the previously linked evaluation because that is the prescription the order was priced with.
    private suspend fun recetaPrevia(
        dispensacionId: String?,
        evaluacionId: String?,
        receta: RecetaCosto?,
        opticaId: String,
    ): RecetaCosto? {
        if (dispensacionId == null) return null
        val previa = (repository.getDispensacionById(dispensacionId, opticaId) as? Resource.Success)?.data
            ?.evaluacionId?.takeIf { it.isNotBlank() }
        return if (previa == evaluacionId?.takeIf { it.isNotBlank() }) receta else recetaDe(previa, opticaId)
    }

    private suspend fun syncBiseladoGastosDerivados(
        opticaId: String,
        ot: String,
        fecha: LocalDate,
        items: List<DispensacionItem>,
        deletedItemIds: List<String>,
    ) {
        SyncBiseladoGastosDerivados.sync(
            gastoOperativoDao = gastoOperativoDao,
            opticaId = opticaId,
            ot = ot,
            fecha = fecha,
            items = items,
            deletedItemIds = deletedItemIds,
        )
    }

    private fun DispensacionItemUi.toEntity(dispensacionId: String, opticaId: String): DispensacionItem {
        val requiereAltura = tipoLente in setOf("Bifocal", "Multifocal", "Ocupacional")
        return DispensacionItem(
            id = id,
            dispensacionId = dispensacionId,
            tipoLente = tipoLente,
            materialLente = materialLente,
            tratamientos = tratamientos,
            colorLente = colorLente,
            distanciaLente = if (tipoLente == "Monofocal") distanciaLente else "",
            altura = if (requiereAltura) altura.trim() else "",
            subTipoBifocal = if (tipoLente == "Bifocal") subTipoBifocal else "",
            filtroDiscromatopsiaTipo = filtroDiscromatopsiaTipo,
            notasDiseno = notasDiseno,
            monturaId = monturaId,
            origenMontura = origenMontura,
            tipoAro = tipoAro,
            materialMontura = materialMontura,
            descripcionMontura = descripcionMontura,
            tipoMontura = tipoMontura,
            opticaId = opticaId,
            altoIndice = altoIndice.takeIf { it.isNotBlank() },
            curvaBase = curvaBase.takeIf { it.isNotBlank() },
            forzarStockIgnorandoAstigmatismo = forzarStockIgnorandoAstigmatismo,
            ojosLente = com.example.optoapp.domain.OjosLente.normalize(ojosLente),
            origenBiselado = com.example.optoapp.domain.OrigenBiselado.normalize(origenBiselado),
            costoRealOd = costoRealOd,
            costoRealOi = costoRealOi,
            costoRealMontura = costoRealMontura,
            costoRealBiselado = costoRealBiselado,
            costoRealLc = costoRealLc,
            costoOrigen = costoOrigen,
        )
    }

    companion object {
        private const val TAG = "DispensacionVM"
        private const val ORIGEN_PACIENTE = "Paciente"
        private const val ORIGEN_PACIENTE_LEGACY = "Traída por paciente"
        private const val METODO_REEMBOLSO_POR_DEFECTO = "Efectivo"
    }
}
