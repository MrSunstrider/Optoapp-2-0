package com.example.optoapp.viewmodel

import android.database.sqlite.SQLiteConstraintException
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.optoapp.data.FinanzasRemoteDefaults
import com.example.optoapp.data.Paciente
import com.example.optoapp.data.Pago
import com.example.optoapp.data.Resource
import com.example.optoapp.data.ServicioExtra
import com.example.optoapp.data.regaloservicio.RegaloServicioExtraEntity
import com.example.optoapp.data.servicio.ServicioExtraItem
import com.example.optoapp.domain.PagoEffect
import com.example.optoapp.domain.inventario.inventarioParaServicioExtra
import com.example.optoapp.domain.inventario.monturaMatchesDescripcion
import com.example.optoapp.domain.movimientoReferenciaForRegalo
import com.example.optoapp.domain.movimientoReferenciaForServicioExtraReverso
import com.example.optoapp.sync.PostSaveSyncScheduler
import com.example.optoapp.util.DateUtils
import com.example.optoapp.util.DispensacionStockHelper
import com.example.optoapp.util.MontoDraftFormatting
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.IOException
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject

data class ServicioExtraItemUi(
    val id: String = UUID.randomUUID().toString(),
    val monturaId: String? = null,
    val descripcion: String = "",
    val montoDraft: String = "",
)

data class ServiciosUiState(
    val id: String = UUID.randomUUID().toString(),
    val ot: String = "",
    val descripcion: String = "",
    val montoTotal: String = "",
    val monturaId: String? = null,
    val items: List<ServicioExtraItemUi> = listOf(ServicioExtraItemUi()),
    val regalos: List<RegaloDispensacionUi> = emptyList(),
    val estado: String = "Pendiente",
    val fecha: LocalDate = DateUtils.today(),
    val fechaEntrega: LocalDate? = null,
    val pacienteId: String? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val pagos: List<Pago> = emptyList(),
    val pagosToDelete: List<Pago> = emptyList(),
    val generatedId: String = UUID.randomUUID().toString(),
    val isEdit: Boolean = false,
)

@HiltViewModel
class ServiciosViewModel @Inject constructor(
    private val repository: com.example.optoapp.data.OptoRepository,
    private val sessionManager: com.example.optoapp.data.SessionManager,
    private val postSaveSyncScheduler: PostSaveSyncScheduler,
    private val cancelServicioExtraUseCase: com.example.optoapp.domain.CancelServicioExtraUseCase,
    private val stockHelper: DispensacionStockHelper,
) : ViewModel() {

    companion object {
        private const val TAG = "ServiciosViewModel"
    }

    private val _uiState = MutableStateFlow(ServiciosUiState())
    val uiState: StateFlow<ServiciosUiState> = _uiState.asStateFlow()

    private var initialItems: List<ServicioExtraItem> = emptyList()
    private var initialRegalos: List<RegaloServicioExtraEntity> = emptyList()

    private val _showDeleteDialog = MutableStateFlow(false)
    val showDeleteDialog: StateFlow<Boolean> = _showDeleteDialog.asStateFlow()

    private val _servicioToDelete = MutableStateFlow<ServicioExtra?>(null)
    val servicioToDelete: StateFlow<ServicioExtra?> = _servicioToDelete.asStateFlow()

    private val _deleteError = MutableStateFlow<String?>(null)
    val deleteError: StateFlow<String?> = _deleteError.asStateFlow()

    init {
        viewModelScope.launch {
            val oid = sessionManager.opticaId.first()
            repository.reassignLegacyMiOpticaBaseTo(oid)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val allServicios: StateFlow<List<ServicioExtra>> = sessionManager.opticaId
        .flatMapLatest { repository.getAllServiciosForOptica(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val pacientes: StateFlow<List<Paciente>> = sessionManager.opticaId
        .flatMapLatest { repository.pacientesFlowForOptica(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val monturas: StateFlow<List<com.example.optoapp.data.Montura>> = sessionManager.opticaId
        .flatMapLatest { repository.getMonturasByOptica(it) }
        .map { list -> inventarioParaServicioExtra(list) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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

    fun updateUiState(update: (ServiciosUiState) -> ServiciosUiState) {
        _uiState.value = update(_uiState.value)
    }

    fun updateEstado(estado: String) {
        _uiState.update {
            it.copy(
                estado = estado,
                fechaEntrega = if (estado == "Entregado") DateUtils.today() else it.fechaEntrega,
            )
        }
    }

    fun addItem() {
        _uiState.update { s ->
            deriveHeader(s.copy(items = s.items + ServicioExtraItemUi()))
        }
    }

    fun updateItem(index: Int, item: ServicioExtraItemUi) {
        _uiState.update { s ->
            val updated = s.items.toMutableList()
            if (index in updated.indices) updated[index] = item
            deriveHeader(s.copy(items = updated))
        }
    }

    fun removeItem(index: Int) {
        _uiState.update { s ->
            if (s.items.size <= 1) return@update s
            val updated = s.items.toMutableList().apply { removeAt(index) }
            deriveHeader(s.copy(items = updated))
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

    fun loadServicio(id: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, generatedId = id)
            val opticaId = sessionManager.opticaId.first()
            when (val result = repository.getServicioById(id, opticaId)) {
                is Resource.Success -> {
                    val s = result.data ?: return@launch
                    val loadedPagos = repository.getPagosByServicioExtra(id, opticaId).first()
                    val loadedItems = repository.getServicioExtraItems(id, opticaId)
                    val loadedRegalos = repository.getRegalosByServicioExtraId(id, opticaId)
                    initialItems = loadedItems
                    initialRegalos = loadedRegalos

                    val itemsUi = if (loadedItems.isNotEmpty()) {
                        loadedItems.map { entity ->
                            ServicioExtraItemUi(
                                id = entity.id,
                                monturaId = entity.monturaId,
                                descripcion = entity.descripcion,
                                montoDraft = MontoDraftFormatting.formatDraft(entity.monto),
                            )
                        }
                    } else {
                        val resolvedMonturaId = s.monturaId?.takeIf { it.isNotBlank() }
                            ?: repository.getMonturasSnapshotForOptica(opticaId)
                                .firstOrNull { monturaMatchesDescripcion(it, s.descripcion) }
                                ?.id
                        listOf(
                            ServicioExtraItemUi(
                                id = if (resolvedMonturaId != null) s.id else UUID.randomUUID().toString(),
                                monturaId = resolvedMonturaId,
                                descripcion = s.descripcion,
                                montoDraft = MontoDraftFormatting.formatDraft(s.montoTotal),
                            ),
                        )
                    }

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

                    val base = ServiciosUiState(
                        id = s.id,
                        ot = s.ot,
                        estado = s.estado,
                        fecha = s.fecha,
                        fechaEntrega = s.fechaEntrega,
                        pacienteId = s.pacienteId,
                        pagos = loadedPagos,
                        regalos = regalosUi,
                        items = itemsUi,
                        generatedId = id,
                        isEdit = true,
                        isLoading = false,
                        error = null,
                    )
                    _uiState.value = deriveHeader(base)
                }
                is Resource.Error -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, error = result.message)
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

    fun clearServicioError() {
        _uiState.update { it.copy(error = null) }
    }

    fun saveServicio(onSuccess: () -> Unit) {
        viewModelScope.launch {
            val state = deriveHeader(_uiState.value)
            _uiState.value = state

            val validItems = state.items.filter { it.descripcion.isNotBlank() }
            if (validItems.isEmpty()) {
                _uiState.update { it.copy(error = "Agrega al menos un producto con descripción.") }
                return@launch
            }
            val lineMontos = validItems.map { item ->
                MontoDraftFormatting.parseDraft(item.montoDraft)
            }
            if (lineMontos.any { it == null }) {
                _uiState.update { it.copy(error = "Cada producto debe tener un monto válido.") }
                return@launch
            }
            val montoParsed = lineMontos.sumOf { it!! }
            if (montoParsed <= 0.0) {
                _uiState.update { it.copy(error = FinanzasRemoteDefaults.Messages.MONTO_TOTAL_MAYOR_A_CERO) }
                return@launch
            }
            if (state.pagos.any { it.monto <= 0.0 }) {
                _uiState.update { it.copy(error = FinanzasRemoteDefaults.Messages.ABONO_MAYOR_A_CERO) }
                return@launch
            }
            val totalAbonos = state.pagos.sumOf { PagoEffect.signedAmount(it.tipo, it.monto) }
            if (totalAbonos > montoParsed) {
                _uiState.update { it.copy(error = FinanzasRemoteDefaults.Messages.ABONO_MAYOR_QUE_TOTAL) }
                return@launch
            }

            try {
                val currentOpticaId = sessionManager.opticaId.first().trim().ifBlank {
                    com.example.optoapp.data.SessionManager.LEGACY_OPTICA_ID
                }
                val finalId = if (state.id.isNotBlank()) state.id else state.generatedId
                val headerMonturaId = validItems.firstOrNull { !it.monturaId.isNullOrBlank() }?.monturaId
                val headerDescripcion = validItems.joinToString(" + ") { it.descripcion.trim() }
                    .ifBlank { validItems.first().descripcion.trim() }

                val existingServicio = if (state.isEdit) {
                    (repository.getServicioById(finalId, currentOpticaId) as? Resource.Success)?.data
                } else {
                    null
                }
                val previousItems = if (state.isEdit) {
                    initialItems.ifEmpty { repository.getServicioExtraItems(finalId, currentOpticaId) }
                } else {
                    emptyList()
                }
                val previousRegalos = if (state.isEdit) {
                    initialRegalos.ifEmpty { repository.getRegalosByServicioExtraId(finalId, currentOpticaId) }
                } else {
                    emptyList()
                }

                val servicio = ServicioExtra(
                    id = finalId,
                    ot = state.ot.trim(),
                    monturaId = headerMonturaId,
                    descripcion = headerDescripcion,
                    montoTotal = montoParsed,
                    aCuenta = state.pagos.sumOf { PagoEffect.signedAmount(it.tipo, it.monto) },
                    estado = state.estado,
                    fecha = state.fecha,
                    fechaEntrega = state.fechaEntrega,
                    pacienteId = state.pacienteId?.takeIf { it.isNotBlank() },
                    metodoPago = FinanzasRemoteDefaults.ServicioExtra.METODO_PAGO_ROW,
                    opticaId = currentOpticaId,
                )

                val persistedItems = validItems.map { ui ->
                    ServicioExtraItem(
                        id = ui.id.ifBlank { UUID.randomUUID().toString() },
                        servicioExtraId = finalId,
                        monturaId = ui.monturaId?.takeIf { it.isNotBlank() },
                        descripcion = ui.descripcion.trim(),
                        monto = MontoDraftFormatting.parseDraft(ui.montoDraft) ?: 0.0,
                        opticaId = currentOpticaId,
                    )
                }

                val regalosToPersist = state.regalos.map { regaloUi ->
                    RegaloServicioExtraEntity(
                        id = regaloUi.id.ifBlank { UUID.randomUUID().toString() },
                        servicioExtraId = finalId,
                        productoId = regaloUi.productoId,
                        cantidad = regaloUi.cantidad,
                        costoUnitario = regaloUi.costoUnitario,
                        descripcion = regaloUi.descripcion,
                        motivo = regaloUi.motivo,
                        opticaId = currentOpticaId,
                    )
                }

                repository.withTransaction {
                    if (state.isEdit) {
                        applyEditStockDiff(
                            previousItems = previousItems,
                            persistedItems = persistedItems,
                            previousRegalos = previousRegalos,
                            regalosToPersist = regalosToPersist,
                            existingHeaderMonturaId = existingServicio?.monturaId,
                            servicioId = finalId,
                            opticaId = currentOpticaId,
                        )
                        val nextItemIds = persistedItems.map { it.id }.toSet()
                        val nextRegaloIds = regalosToPersist.map { it.id }.toSet()
                        for (removed in previousItems.filter { it.id !in nextItemIds }) {
                            repository.deleteServicioExtraItemById(removed.id, currentOpticaId)
                        }
                        for (removed in previousRegalos.filter { it.id !in nextRegaloIds }) {
                            repository.deleteRegaloServicioExtraById(removed.id, currentOpticaId)
                        }
                        repository.deleteServicioExtraItemsByServicioId(finalId, currentOpticaId)
                        repository.deleteRegalosByServicioExtraId(finalId, currentOpticaId)
                    } else {
                        for (item in persistedItems) {
                            applyItemSale(item, currentOpticaId)
                        }
                        for (entity in regalosToPersist) {
                            applyRegaloSale(entity, currentOpticaId)
                        }
                    }

                    if (state.isEdit) {
                        repository.updateServicio(servicio)
                    } else {
                        repository.insertServicio(servicio)
                    }

                    for (item in persistedItems) {
                        repository.insertServicioExtraItem(item)
                    }

                    for (entity in regalosToPersist) {
                        repository.insertRegaloServicioExtra(entity)
                    }

                    state.pagos.forEach { pago ->
                        val pagoToSave = pago.copy(
                            servicioExtraId = finalId,
                            opticaId = currentOpticaId,
                            ventaId = "v_serv_$finalId",
                        )
                        repository.insertPago(pagoToSave)
                    }

                    state.pagosToDelete.forEach { pago ->
                        repository.deletePagoRegistrandoAnulacionEnCaja(pago, currentOpticaId)
                    }
                }

                initialItems = persistedItems
                initialRegalos = state.regalos.map { regaloUi ->
                    RegaloServicioExtraEntity(
                        id = regaloUi.id,
                        servicioExtraId = finalId,
                        productoId = regaloUi.productoId,
                        cantidad = regaloUi.cantidad,
                        costoUnitario = regaloUi.costoUnitario,
                        descripcion = regaloUi.descripcion,
                        motivo = regaloUi.motivo,
                        opticaId = currentOpticaId,
                    )
                }

                _uiState.update { it.copy(error = null) }

                postSaveSyncScheduler.scheduleFinanzasSync(currentOpticaId)
                val stockChanged = previousItems.any { !it.monturaId.isNullOrBlank() } ||
                    previousRegalos.any { it.productoId.isNotBlank() } ||
                    persistedItems.any { !it.monturaId.isNullOrBlank() } ||
                    state.regalos.any { it.productoId.isNotBlank() }
                if (stockChanged) {
                    postSaveSyncScheduler.scheduleInventarioSync(currentOpticaId)
                }

                onSuccess()
            } catch (e: SQLiteConstraintException) {
                Log.e(TAG, "Guardar servicio: restricción BD", e)
                _uiState.update {
                    it.copy(
                        error = "No se pudo guardar: revisa el paciente asociado o deja el servicio sin paciente.",
                    )
                }
            } catch (e: IllegalStateException) {
                _uiState.update { it.copy(error = e.message ?: "No se pudo ajustar el stock.") }
            } catch (e: CancellationException) {
                throw e
            } catch (e: IOException) {
                Log.e(TAG, "Guardar servicio: error de red/IO", e)
                _uiState.update { it.copy(error = "Error inesperado. Reintente más tarde.") }
            } catch (e: Exception) {
                Log.e(TAG, "Guardar servicio", e)
                _uiState.update { it.copy(error = "Error inesperado. Reintente más tarde.") }
            }
        }
    }

    fun showDeleteConfirmation(servicio: ServicioExtra) {
        _servicioToDelete.value = servicio
        _showDeleteDialog.value = true
        _deleteError.value = null
    }

    fun dismissDeleteDialog() {
        _showDeleteDialog.value = false
        _servicioToDelete.value = null
        _deleteError.value = null
    }

    fun confirmDelete() {
        val servicio = _servicioToDelete.value ?: return
        viewModelScope.launch {
            try {
                val opticaId = sessionManager.opticaId.first()
                cancelServicioExtraUseCase(servicio.id, opticaId)
                _showDeleteDialog.value = false
                _servicioToDelete.value = null
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Eliminar servicio", e)
                _deleteError.value = "Error inesperado. Reintente más tarde."
            }
        }
    }

    fun clearDeleteError() {
        _deleteError.value = null
    }

    private fun deriveHeader(state: ServiciosUiState): ServiciosUiState {
        val nonBlank = state.items.filter { it.descripcion.isNotBlank() }
        val source = nonBlank.ifEmpty { state.items }
        val descripcion = source.map { it.descripcion.trim() }
            .filter { it.isNotBlank() }
            .joinToString(" + ")
            .ifBlank { source.firstOrNull()?.descripcion.orEmpty() }
        val montoSum = source.mapNotNull { MontoDraftFormatting.parseDraft(it.montoDraft) }.sum()
        val montoTotal = MontoDraftFormatting.formatDraft(montoSum.takeIf { it > 0.0 })
        val monturaId = source.firstOrNull { !it.monturaId.isNullOrBlank() }?.monturaId
        return state.copy(
            descripcion = descripcion,
            montoTotal = montoTotal,
            monturaId = monturaId,
        )
    }

    private suspend fun applyEditStockDiff(
        previousItems: List<ServicioExtraItem>,
        persistedItems: List<ServicioExtraItem>,
        previousRegalos: List<RegaloServicioExtraEntity>,
        regalosToPersist: List<RegaloServicioExtraEntity>,
        existingHeaderMonturaId: String?,
        servicioId: String,
        opticaId: String,
    ) {
        if (previousItems.isEmpty()) {
            existingHeaderMonturaId?.takeIf { it.isNotBlank() }?.let { headerMid ->
                if (persistedItems.any { !it.monturaId.isNullOrBlank() }) {
                    requireStockMovement(
                        stockHelper.adjustStockAndRegistrarMovimiento(
                            monturaId = headerMid,
                            opticaId = opticaId,
                            delta = 1,
                            tipo = "AJUSTE",
                            referenciaId = movimientoReferenciaForServicioExtraReverso(servicioId, headerMid),
                            nota = "Reversión por edición de servicio extra",
                        ),
                        "No se pudo reponer el stock del producto anterior.",
                    )
                }
            }
        }

        val nextItemsById = persistedItems.associateBy { it.id }
        for (prev in previousItems) {
            val next = nextItemsById[prev.id]
            val prevMid = prev.monturaId?.takeIf { it.isNotBlank() } ?: continue
            if (next == null || next.monturaId?.trim() != prev.monturaId?.trim()) {
                requireStockMovement(
                    stockHelper.adjustStockAndRegistrarMovimiento(
                        monturaId = prevMid,
                        opticaId = opticaId,
                        delta = 1,
                        tipo = "AJUSTE",
                        referenciaId = prev.id,
                        nota = "Reversión por edición de servicio extra",
                    ),
                    "No se pudo reponer el stock del producto anterior.",
                )
            }
        }

        val prevItemsById = previousItems.associateBy { it.id }
        for (next in persistedItems) {
            val prev = prevItemsById[next.id]
            if (prev != null && itemStockUnchanged(prev, next)) continue
            applyItemSale(next, opticaId)
        }

        val nextRegalosById = regalosToPersist.associateBy { it.id }
        for (prev in previousRegalos) {
            val next = nextRegalosById[prev.id]
            if (prev.productoId.isBlank()) continue
            if (next == null || !regaloStockUnchanged(prev, next)) {
                requireStockMovement(
                    stockHelper.adjustStockAndRegistrarMovimiento(
                        monturaId = prev.productoId,
                        opticaId = opticaId,
                        delta = prev.cantidad,
                        tipo = "AJUSTE",
                        referenciaId = movimientoReferenciaForRegalo(prev.id),
                        nota = "Reversión por edición de regalos de servicio",
                    ),
                    "No se pudo reponer el stock del regalo anterior.",
                )
            }
        }

        val prevRegalosById = previousRegalos.associateBy { it.id }
        for (next in regalosToPersist) {
            val prev = prevRegalosById[next.id]
            if (prev != null && regaloStockUnchanged(prev, next)) continue
            applyRegaloSale(next, opticaId)
        }
    }

    private suspend fun applyItemSale(item: ServicioExtraItem, opticaId: String) {
        val mid = item.monturaId?.takeIf { it.isNotBlank() } ?: return
        requireStockMovement(
            stockHelper.adjustStockAndRegistrarMovimiento(
                monturaId = mid,
                opticaId = opticaId,
                delta = -1,
                tipo = "SALIDA_VENTA",
                referenciaId = item.id,
                nota = "Salida por servicio extra",
            ),
            "Stock insuficiente para el producto seleccionado.",
        )
    }

    private suspend fun applyRegaloSale(entity: RegaloServicioExtraEntity, opticaId: String) {
        if (entity.productoId.isBlank()) return
        requireStockMovement(
            stockHelper.adjustStockAndRegistrarMovimiento(
                monturaId = entity.productoId,
                opticaId = opticaId,
                delta = -entity.cantidad,
                tipo = "SALIDA_VENTA",
                referenciaId = movimientoReferenciaForRegalo(entity.id),
                nota = "Salida por regalo de servicio extra",
            ),
            "Stock insuficiente para regalo: ${entity.descripcion}",
        )
    }

    private fun requireStockMovement(result: Result<Int>, fallbackMessage: String) {
        if (result.isFailure) {
            throw IllegalStateException(result.exceptionOrNull()?.message ?: fallbackMessage)
        }
    }

    private fun itemStockUnchanged(prev: ServicioExtraItem, next: ServicioExtraItem): Boolean =
        prev.monturaId?.trim() == next.monturaId?.trim()

    private fun regaloStockUnchanged(
        prev: RegaloServicioExtraEntity,
        next: RegaloServicioExtraEntity,
    ): Boolean = prev.productoId == next.productoId && prev.cantidad == next.cantidad
}
