package com.example.optoapp.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.optoapp.data.OptoRepository
import com.example.optoapp.data.SessionManager
import com.example.optoapp.data.costobiselado.CostoBiseladoDao
import com.example.optoapp.data.costobiselado.CostoBiseladoEntity
import com.example.optoapp.data.costolc.CostoLcDao
import com.example.optoapp.data.costolc.CostoLcEntity
import com.example.optoapp.data.costoproducto.CostoProductoDao
import com.example.optoapp.data.costoproducto.CostoProductoEntity
import com.example.optoapp.data.gastooperativo.GastoOperativoEntity
import com.example.optoapp.domain.OpticalCatalog
import com.example.optoapp.domain.SyncFinanzasUseCase
import com.example.optoapp.sync.PostSaveSyncScheduler
import com.example.optoapp.util.DateUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject

/** 5 cost blocks for ophthalmic lenses. Each block maps to stock_o_fabricacion values. */
val COST_BLOCKS = listOf(
    "Stock Monofocal",
    "Stock Bifocal",
    "Stock Multifocal",
    "Fabricación Resina",
    "Fabricación Cristal",
)

/** Fine-grained filter derived from the selected display block. */
data class BlockFilter(
    val stockOFab: String,
    val tipoLente: String? = null,
    val material: String? = null,
)

/** Maps display block name to a BlockFilter that narrows by stockOFab + optional tipoLente/material. */
fun blockFilter(block: String): BlockFilter = when (block) {
    "Stock Monofocal" -> BlockFilter("stock", tipoLente = "Monofocal")
    "Stock Bifocal" -> BlockFilter("stock", tipoLente = "Bifocal")
    "Stock Multifocal" -> BlockFilter("stock", tipoLente = "Multifocal")
    "Fabricación Resina" -> BlockFilter("fabricacion", material = "Resina")
    "Fabricación Cristal" -> BlockFilter("fabricacion", material = "Cristal")
    else -> BlockFilter(block)
}

/** In-memory secondary filter applied after getByBloque. */
fun List<com.example.optoapp.data.costoproducto.CostoProductoEntity>.filteredBy(
    f: BlockFilter,
): List<com.example.optoapp.data.costoproducto.CostoProductoEntity> =
    filter { e -> f.tipoLente == null || e.tipoLente == f.tipoLente }
        .filter { e -> f.material == null || e.material == f.material }

data class CostosYGastosUiState(
    val selectedTab: Int = 0,
    val selectedBlock: String? = null,
    val costosDelBloque: List<CostoProductoEntity> = emptyList(),
    val costosPorDispensacion: Map<String, List<CostoProductoEntity>> = emptyMap(),
    val dispensacionFilterId: String? = null,
    // Edit cost dialog
    val editingCosto: CostoProductoEntity? = null,
    val nuevoCostoUnitario: String = "",
    val gastosOperativos: List<GastoOperativoEntity> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val gastosLoading: Boolean = true,
    val gastosError: String? = null,
    // Dialog state for gastos operativos CRUD
    val isDialogVisible: Boolean = false,
    val editingGasto: GastoOperativoEntity? = null,
    val categoria: String = "alquiler",
    val descripcion: String = "",
    val monto: String = "",
    val fecha: LocalDate = DateUtils.today(),
    val nota: String = "",
    val isRecurring: Boolean = false,
    // Dialog state for costos productos CRUD (Tab 0)
    val isCostoDialogVisible: Boolean = false,
    val creatingCosto: Boolean = false,
    val costoMaterial: String = "",
    val costoTipoLente: String = "",
    val costoStockOFabricacion: String = "",
    val costoTratamiento: String = "",
    val costoSerie: String = "",
    val costoCostoUnitario: String = "",
    val costoSaveError: String? = null,
    val deletingCosto: CostoProductoEntity? = null,
    // Tab 1 — Biselado
    val costosBiselado: List<CostoBiseladoEntity> = emptyList(),
    val isBiseladoDialogVisible: Boolean = false,
    val editingBiselado: CostoBiseladoEntity? = null,
    val biseladoMaterial: String = "",
    val biseladoTipoAro: String = "",
    val biseladoStockOFabricacion: String = "stock",
    val biseladoSerie: String = "",
    val biseladoAltoIndice: String = "",
    val biseladoCostoPorPar: String = "",
    val biseladoProveedor: String = "",
    val biseladoSaveError: String? = null,
    val deletingBiselado: CostoBiseladoEntity? = null,
    // Tab 2 — Lentes de Contacto
    val costosLc: List<CostoLcEntity> = emptyList(),
    val isLcDialogVisible: Boolean = false,
    val editingLc: CostoLcEntity? = null,
    val lcTipo: String = "",
    val lcMaterial: String = "",
    val lcModalidad: String = "mensual",
    val lcCostoUnitario: String = "",
    val lcSaveError: String? = null,
    val deletingLc: CostoLcEntity? = null,
)

data class GastosTabTriad(
    val showsLoading: Boolean,
    val showsEmpty: Boolean,
    val showsError: Boolean,
    val showsRetry: Boolean,
)

data class CostosAccess(val isRestricted: Boolean)

object CostosGastosUiPolicy {
    fun resolveAccess(rol: String?): CostosAccess {
        val allowed = rol != null && com.example.optoapp.data.AppRoles.canViewBiAndReports(rol)
        return CostosAccess(isRestricted = !allowed)
    }

    fun resolveGastosTriad(
        isLoading: Boolean,
        gastosCount: Int,
        errorMessage: String?,
    ): GastosTabTriad {
        val hasError = !errorMessage.isNullOrBlank()
        return GastosTabTriad(
            showsLoading = isLoading && !hasError,
            showsEmpty = !isLoading && !hasError && gastosCount == 0,
            showsError = hasError,
            showsRetry = hasError,
        )
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class CostosYGastosViewModel @Inject constructor(
    private val repository: OptoRepository,
    private val costoProductoDao: CostoProductoDao,
    private val costoBiseladoDao: CostoBiseladoDao,
    private val costoLcDao: CostoLcDao,
    private val sessionManager: SessionManager,
    private val postSaveSyncScheduler: PostSaveSyncScheduler,
    private val syncFinanzasUseCase: SyncFinanzasUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CostosYGastosUiState())
    val uiState: StateFlow<CostosYGastosUiState> = _uiState.asStateFlow()

    private var syncTriggered = false
    private val _gastosRetryTick = MutableStateFlow(0)
    private var loadBlockJob: Job? = null

    companion object {
        private const val TAG = "CostosYGastosVM"
        const val TAB_GASTOS = 3
        private const val TAB_MAX = 3
        val CATEGORIAS = listOf("alquiler", "servicios", "personal", "proveedores", "insumos", "marketing", "impuestos", "otro")

        fun clampTab(index: Int): Int = index.coerceIn(0, TAB_MAX)

        fun autoGenerarRecurrentes(
            templates: List<GastoOperativoEntity>,
            existentes: List<GastoOperativoEntity>,
            mesActual: LocalDate,
        ): List<GastoOperativoEntity> {
            val mesInicio = mesActual.withDayOfMonth(1)
            val mesFin = mesActual.withDayOfMonth(mesActual.lengthOfMonth())
            return templates
                .filter { it.isRecurring }
                .filter { template ->
                    existentes.none { existente ->
                        existente.id != template.id &&
                            existente.categoria == template.categoria &&
                            !existente.fecha.isBefore(mesInicio) &&
                            !existente.fecha.isAfter(mesFin)
                    }
                }
                .map { template ->
                    template.copy(
                        id = UUID.randomUUID().toString(),
                        fecha = mesInicio,
                        isRecurring = false,
                        nota = "Auto-generado de ${template.categoria}",
                    )
                }
        }
    }

    init {
        viewModelScope.launch {
            combine(sessionManager.opticaId, _gastosRetryTick) { opticaId, _ -> opticaId }
                .flatMapLatest { opticaId ->
                    _uiState.update { it.copy(gastosLoading = true, gastosError = null) }
                    repository.getGastosOperativos(opticaId)
                        .catch { e ->
                            Log.e(TAG, "Gastos flow crashed", e)
                            _uiState.update {
                                it.copy(
                                    gastosLoading = false,
                                    gastosError = "Error al cargar gastos: ${e.message}",
                                    gastosOperativos = emptyList(),
                                )
                            }
                            emit(emptyList())
                        }
                }
                .collect { gastos ->
                    if (_uiState.value.gastosError != null) return@collect
                    val resolved = try {
                        autoGenerarSiFalta(gastos)
                    } catch (e: Exception) {
                        Log.e(TAG, "autoGenerarSiFalta failed, showing raw gastos", e)
                        gastos
                    }
                    _uiState.update {
                        it.copy(
                            gastosOperativos = resolved,
                            gastosLoading = false,
                            gastosError = null,
                        )
                    }
                    if (!syncTriggered && gastos.isEmpty()) {
                        syncTriggered = true
                        val opticaId = sessionManager.opticaId.first()
                        Log.d(TAG, "Triggering finanzas download for gastos (opticaId=$opticaId)")
                        viewModelScope.launch {
                            try {
                                syncFinanzasUseCase(opticaId, downloadAfterUpload = true, skipUpload = true)
                            } catch (e: Exception) {
                                Log.e(TAG, "One-shot finanzas sync failed", e)
                                syncTriggered = false
                            }
                        }
                    }
                }
        }
        viewModelScope.launch {
            sessionManager.opticaId
                .flatMapLatest { opticaId -> costoBiseladoDao.getByOpticaId(opticaId) }
                .catch { e ->
                    Log.e(TAG, "Biselado flow crashed", e)
                    emit(emptyList())
                }
                .collect { rows ->
                    _uiState.update { it.copy(costosBiselado = rows) }
                }
        }
        viewModelScope.launch {
            sessionManager.opticaId
                .flatMapLatest { opticaId -> costoLcDao.getByOpticaId(opticaId) }
                .catch { e ->
                    Log.e(TAG, "LC flow crashed", e)
                    emit(emptyList())
                }
                .collect { rows ->
                    _uiState.update { it.copy(costosLc = rows) }
                }
        }
    }

    fun retryGastos() {
        _gastosRetryTick.update { it + 1 }
    }

    private suspend fun autoGenerarSiFalta(gastos: List<GastoOperativoEntity>): List<GastoOperativoEntity> {
        val hoy = DateUtils.today()
        val nuevos = autoGenerarRecurrentes(
            templates = gastos.filter { it.isRecurring },
            existentes = gastos,
            mesActual = hoy,
        )
        if (nuevos.isEmpty()) return gastos
        val opticaId = sessionManager.opticaId.first()
        nuevos.forEach { repository.upsertGastoOperativo(it) }
        postSaveSyncScheduler.scheduleFinanzasSync(opticaId)
        return gastos + nuevos
    }

    fun selectTab(index: Int) {
        _uiState.update { it.copy(selectedTab = clampTab(index)) }
    }

    fun loadBlock(block: String) {
        loadBlockJob?.cancel()
        _uiState.update { it.copy(selectedBlock = block, isLoading = true) }
        loadBlockJob = viewModelScope.launch {
            try {
                val opticaId = sessionManager.opticaId.first()
                val filter = blockFilter(block)
                costoProductoDao.getByBloque(opticaId, filter.stockOFab)
                    .map { it.filteredBy(filter) }
                    .collect { costos ->
                        _uiState.update { it.copy(isLoading = false, costosDelBloque = costos) }
                    }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Error loading block $block", e)
                _uiState.update { it.copy(isLoading = false, error = "Error al cargar bloque: ${e.message}") }
            }
        }
    }

    // R6: manual override persists

    fun showEditCosto(costo: CostoProductoEntity) {
        _uiState.update { it.copy(editingCosto = costo, nuevoCostoUnitario = costo.costoUnitario.toString(), error = null) }
    }

    fun dismissEditCosto() {
        _uiState.update { it.copy(editingCosto = null, nuevoCostoUnitario = "") }
    }

    fun updateNuevoCostoUnitario(value: String) {
        _uiState.update { it.copy(nuevoCostoUnitario = value) }
    }

    fun saveCostoEdit() {
        val s = _uiState.value
        val costo = s.editingCosto ?: return
        val nuevoValor = s.nuevoCostoUnitario.toDoubleOrNull()
        if (nuevoValor == null || nuevoValor <= 0) {
            _uiState.update { it.copy(error = "Ingresa un costo válido") }
            return
        }
        viewModelScope.launch {
            try {
                val updated = costo.copy(costoUnitario = nuevoValor)
                costoProductoDao.upsertAll(listOf(updated))
                                val opticaId = sessionManager.opticaId.first()
                val filter = blockFilter(s.selectedBlock ?: return@launch)
                val refreshed = costoProductoDao.getByBloque(opticaId, filter.stockOFab).first().filteredBy(filter)
                _uiState.update { it.copy(editingCosto = null, nuevoCostoUnitario = "", costosDelBloque = refreshed, error = null) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Error al guardar: ${e.message}") }
            }
        }
    }

    val categorias = CATEGORIAS

    fun showNewGasto() {
        _uiState.update { it.copy(isDialogVisible = true, editingGasto = null, categoria = "alquiler", descripcion = "", monto = "", fecha = DateUtils.today(), nota = "", isRecurring = false) }
    }

    fun editGasto(gasto: GastoOperativoEntity) {
        _uiState.update {
            it.copy(
                isDialogVisible = true,
                editingGasto = gasto,
                categoria = gasto.categoria,
                descripcion = gasto.descripcion ?: "",
                monto = gasto.monto.toString(),
                fecha = gasto.fecha,
                nota = gasto.nota ?: "",
                isRecurring = gasto.isRecurring,
            )
        }
    }

    fun dismissDialog() {
        _uiState.update { it.copy(isDialogVisible = false, editingGasto = null, error = null) }
    }

    fun updateCategoria(c: String) {
        _uiState.update { it.copy(categoria = c) }
    }
    fun updateDescripcion(d: String) {
        _uiState.update { it.copy(descripcion = d) }
    }
    fun updateMonto(m: String) {
        _uiState.update { it.copy(monto = m) }
    }
    fun updateFecha(f: LocalDate) {
        _uiState.update { it.copy(fecha = f) }
    }
    fun updateNota(n: String) {
        _uiState.update { it.copy(nota = n) }
    }
    fun toggleRecurrente() {
        _uiState.update { it.copy(isRecurring = !it.isRecurring) }
    }

    fun saveGasto() {
        val s = _uiState.value
        val monto = s.monto.toBigDecimalOrNull()
        if (monto == null || monto <= BigDecimal.ZERO) {
            _uiState.update { it.copy(error = "Ingresa un monto válido") }
            return
        }
        viewModelScope.launch {
            try {
                val opticaId = sessionManager.opticaId.first()
                val gasto = GastoOperativoEntity(
                    id = s.editingGasto?.id ?: UUID.randomUUID().toString(),
                    opticaId = opticaId,
                    categoria = s.categoria,
                    descripcion = s.descripcion.ifBlank { null },
                    monto = monto,
                    fecha = s.fecha,
                    fechaProgramada = null,
                    nota = s.nota.ifBlank { null },
                    isRecurring = s.isRecurring,
                )
                repository.upsertGastoOperativo(gasto)
                postSaveSyncScheduler.scheduleFinanzasSync(opticaId)
                _uiState.update { it.copy(isDialogVisible = false, editingGasto = null, error = null) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Error al guardar") }
            }
        }
    }

    fun deleteGasto(gasto: GastoOperativoEntity) {
        viewModelScope.launch {
            try {
                repository.deleteGastoOperativo(gasto)
                postSaveSyncScheduler.scheduleFinanzasSync(gasto.opticaId)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Error al eliminar el gasto") }
            }
        }
    }

    val materialesOpticos = OpticalCatalog.MATERIALES
    val tiposLente = OpticalCatalog.TIPO_LENTE.filter { it != "Lentes de Contacto" }
    val tratamientos = OpticalCatalog.TRATAMIENTOS

    fun showNewCosto() {
        val block = _uiState.value.selectedBlock
        _uiState.update {
            it.copy(
                isCostoDialogVisible = true,
                creatingCosto = true,
                costoMaterial = "",
                costoTipoLente = "",
                costoStockOFabricacion = block?.let { blockFilter(it).stockOFab } ?: "",
                costoTratamiento = "",
                costoSerie = "",
                costoCostoUnitario = "",
                costoSaveError = null,
                deletingCosto = null,
            )
        }
    }

    fun dismissCostoDialog() {
        _uiState.update {
            it.copy(
                isCostoDialogVisible = false,
                creatingCosto = false,
                costoSaveError = null,
            )
        }
    }

    fun updateCostoMaterial(value: String) {
        _uiState.update { it.copy(costoMaterial = value) }
    }
    fun updateCostoTipoLente(value: String) {
        _uiState.update { it.copy(costoTipoLente = value) }
    }
    fun updateCostoTratamiento(value: String) {
        _uiState.update { it.copy(costoTratamiento = value) }
    }
    fun updateCostoSerie(value: String) {
        _uiState.update { it.copy(costoSerie = value) }
    }
    fun updateCostoCostoUnitario(value: String) {
        _uiState.update { it.copy(costoCostoUnitario = value) }
    }

    fun saveCosto() {
        val s = _uiState.value
                if (s.costoMaterial.isBlank()) {
            _uiState.update { it.copy(costoSaveError = "Selecciona un material") }
            return
        }
        if (s.costoTipoLente.isBlank()) {
            _uiState.update { it.copy(costoSaveError = "Selecciona un tipo de lente") }
            return
        }
        val costoUnitario = s.costoCostoUnitario.toDoubleOrNull()
        if (costoUnitario == null || costoUnitario <= 0) {
            _uiState.update { it.copy(costoSaveError = "Ingresa un costo unitario válido") }
            return
        }
        viewModelScope.launch {
            try {
                val opticaId = sessionManager.opticaId.first()
                val entity = CostoProductoEntity(
                    id = UUID.randomUUID().toString(),
                    opticaId = opticaId,
                    material = s.costoMaterial,
                    tipoLente = s.costoTipoLente,
                    stockOFabricacion = s.costoStockOFabricacion,
                    tratamiento = s.costoTratamiento.ifBlank { null },
                    serie = s.costoSerie.toIntOrNull(),
                    costoUnitario = costoUnitario,
                    vigenteDesde = DateUtils.toIso(DateUtils.today()),
                )
                costoProductoDao.upsertAll(listOf(entity))
                                val filter = blockFilter(s.selectedBlock ?: return@launch)
                val refreshed = costoProductoDao.getByBloque(opticaId, filter.stockOFab).first().filteredBy(filter)
                _uiState.update {
                    it.copy(
                        isCostoDialogVisible = false,
                        creatingCosto = false,
                        costosDelBloque = refreshed,
                        costoSaveError = null,
                        costoMaterial = "",
                        costoTipoLente = "",
                        costoStockOFabricacion = "",
                        costoTratamiento = "",
                        costoSerie = "",
                        costoCostoUnitario = "",
                    )
                }
                postSaveSyncScheduler.scheduleFinanzasSync(opticaId)
            } catch (e: Exception) {
                _uiState.update { it.copy(costoSaveError = "Error al guardar: ${e.message}") }
            }
        }
    }

    fun confirmDeleteCosto(costo: CostoProductoEntity) {
        _uiState.update { it.copy(deletingCosto = costo) }
    }

    fun dismissDeleteDialog() {
        _uiState.update { it.copy(deletingCosto = null) }
    }

    fun deleteCosto() {
        val costo = _uiState.value.deletingCosto ?: return
        viewModelScope.launch {
            try {
                val updated = costo.copy(vigenteHasta = DateUtils.toIso(DateUtils.today()))
                costoProductoDao.upsertAll(listOf(updated))
                val opticaId = sessionManager.opticaId.first()
                val filter = blockFilter(_uiState.value.selectedBlock ?: return@launch)
                val refreshed = costoProductoDao.getByBloque(opticaId, filter.stockOFab).first().filteredBy(filter)
                _uiState.update {
                    it.copy(
                        deletingCosto = null,
                        costosDelBloque = refreshed,
                    )
                }
                postSaveSyncScheduler.scheduleFinanzasSync(opticaId)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(costoSaveError = "Error al eliminar: ${e.message}") }
            }
        }
    }

    val tiposAro = OpticalCatalog.TIPO_ARO
    val stockOFabricacionOptions = listOf("stock", "fabricacion")

    fun showNewBiselado() {
        _uiState.update {
            it.copy(
                isBiseladoDialogVisible = true,
                editingBiselado = null,
                biseladoMaterial = "",
                biseladoTipoAro = "",
                biseladoStockOFabricacion = "stock",
                biseladoSerie = "",
                biseladoAltoIndice = "",
                biseladoCostoPorPar = "",
                biseladoProveedor = "",
                biseladoSaveError = null,
            )
        }
    }

    fun editBiselado(entity: CostoBiseladoEntity) {
        _uiState.update {
            it.copy(
                isBiseladoDialogVisible = true,
                editingBiselado = entity,
                biseladoMaterial = entity.material,
                biseladoTipoAro = entity.tipoAro,
                biseladoStockOFabricacion = entity.stockOFabricacion,
                biseladoSerie = entity.serie?.toString().orEmpty(),
                biseladoAltoIndice = entity.altoIndice.orEmpty(),
                biseladoCostoPorPar = entity.costoPorPar.toString(),
                biseladoProveedor = entity.proveedor.orEmpty(),
                biseladoSaveError = null,
            )
        }
    }

    fun dismissBiseladoDialog() {
        _uiState.update {
            it.copy(isBiseladoDialogVisible = false, editingBiselado = null, biseladoSaveError = null)
        }
    }

    fun updateBiseladoMaterial(value: String) {
        _uiState.update { it.copy(biseladoMaterial = value) }
    }
    fun updateBiseladoTipoAro(value: String) {
        _uiState.update { it.copy(biseladoTipoAro = value) }
    }
    fun updateBiseladoStockOFabricacion(value: String) {
        _uiState.update { it.copy(biseladoStockOFabricacion = value) }
    }
    fun updateBiseladoSerie(value: String) {
        _uiState.update { it.copy(biseladoSerie = value) }
    }
    fun updateBiseladoAltoIndice(value: String) {
        _uiState.update { it.copy(biseladoAltoIndice = value) }
    }
    fun updateBiseladoCostoPorPar(value: String) {
        _uiState.update { it.copy(biseladoCostoPorPar = value) }
    }
    fun updateBiseladoProveedor(value: String) {
        _uiState.update { it.copy(biseladoProveedor = value) }
    }

    fun saveBiselado() {
        val s = _uiState.value
        if (s.biseladoMaterial.isBlank()) {
            _uiState.update { it.copy(biseladoSaveError = "Selecciona un material") }
            return
        }
        if (s.biseladoTipoAro.isBlank()) {
            _uiState.update { it.copy(biseladoSaveError = "Selecciona un tipo de aro") }
            return
        }
        val costoPorPar = s.biseladoCostoPorPar.toDoubleOrNull()
        if (costoPorPar == null || costoPorPar <= 0) {
            _uiState.update { it.copy(biseladoSaveError = "Ingresa un costo por par válido") }
            return
        }
        viewModelScope.launch {
            try {
                val opticaId = sessionManager.opticaId.first()
                val entity = CostoBiseladoEntity(
                    id = s.editingBiselado?.id ?: UUID.randomUUID().toString(),
                    opticaId = opticaId,
                    material = s.biseladoMaterial,
                    tipoAro = s.biseladoTipoAro,
                    stockOFabricacion = s.biseladoStockOFabricacion,
                    serie = s.biseladoSerie.toIntOrNull(),
                    altoIndice = s.biseladoAltoIndice.ifBlank { null },
                    costoPorPar = costoPorPar,
                    proveedor = s.biseladoProveedor.ifBlank { null },
                    vigenteDesde = s.editingBiselado?.vigenteDesde ?: DateUtils.toIso(DateUtils.today()),
                    vigenteHasta = s.editingBiselado?.vigenteHasta,
                )
                costoBiseladoDao.upsertAll(listOf(entity))
                _uiState.update {
                    it.copy(isBiseladoDialogVisible = false, editingBiselado = null, biseladoSaveError = null)
                }
                postSaveSyncScheduler.scheduleFinanzasSync(opticaId)
            } catch (e: Exception) {
                _uiState.update { it.copy(biseladoSaveError = "Error al guardar: ${e.message}") }
            }
        }
    }

    fun confirmDeleteBiselado(entity: CostoBiseladoEntity) {
        _uiState.update { it.copy(deletingBiselado = entity) }
    }

    fun dismissDeleteBiselado() {
        _uiState.update { it.copy(deletingBiselado = null) }
    }

    fun deleteBiselado() {
        val entity = _uiState.value.deletingBiselado ?: return
        viewModelScope.launch {
            try {
                val updated = entity.copy(vigenteHasta = DateUtils.toIso(DateUtils.today()))
                costoBiseladoDao.upsertAll(listOf(updated))
                val opticaId = sessionManager.opticaId.first()
                _uiState.update { it.copy(deletingBiselado = null) }
                postSaveSyncScheduler.scheduleFinanzasSync(opticaId)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(biseladoSaveError = "Error al eliminar: ${e.message}") }
            }
        }
    }

    val tiposLc = OpticalCatalog.TIPOS_LC
    val modalidadesLc = OpticalCatalog.MODALIDADES_LC
    val materialesLc = OpticalCatalog.MATERIALES_LC

    fun showNewLc() {
        _uiState.update {
            it.copy(
                isLcDialogVisible = true,
                editingLc = null,
                lcTipo = "",
                lcMaterial = "",
                lcModalidad = "mensual",
                lcCostoUnitario = "",
                lcSaveError = null,
            )
        }
    }

    fun editLc(entity: CostoLcEntity) {
        _uiState.update {
            it.copy(
                isLcDialogVisible = true,
                editingLc = entity,
                lcTipo = entity.tipoLc,
                lcMaterial = entity.materialLc,
                lcModalidad = entity.modalidad,
                lcCostoUnitario = entity.costoUnitario.toString(),
                lcSaveError = null,
            )
        }
    }

    fun dismissLcDialog() {
        _uiState.update { it.copy(isLcDialogVisible = false, editingLc = null, lcSaveError = null) }
    }

    fun updateLcTipo(value: String) {
        _uiState.update { it.copy(lcTipo = value) }
    }
    fun updateLcMaterial(value: String) {
        _uiState.update { it.copy(lcMaterial = value) }
    }
    fun updateLcModalidad(value: String) {
        _uiState.update { it.copy(lcModalidad = value) }
    }
    fun updateLcCostoUnitario(value: String) {
        _uiState.update { it.copy(lcCostoUnitario = value) }
    }

    fun saveLc() {
        val s = _uiState.value
        if (s.lcTipo !in OpticalCatalog.TIPOS_LC) {
            _uiState.update { it.copy(lcSaveError = "Selecciona un tipo de LC válido") }
            return
        }
        if (s.lcMaterial.isBlank()) {
            _uiState.update { it.copy(lcSaveError = "Selecciona un material") }
            return
        }
        if (s.lcModalidad !in OpticalCatalog.MODALIDADES_LC) {
            _uiState.update { it.copy(lcSaveError = "Selecciona una modalidad válida") }
            return
        }
        val costo = s.lcCostoUnitario.toDoubleOrNull()
        if (costo == null || costo <= 0) {
            _uiState.update { it.copy(lcSaveError = "Ingresa un costo unitario válido") }
            return
        }
        viewModelScope.launch {
            try {
                val opticaId = sessionManager.opticaId.first()
                val entity = CostoLcEntity(
                    id = s.editingLc?.id ?: UUID.randomUUID().toString(),
                    opticaId = opticaId,
                    tipoLc = s.lcTipo,
                    materialLc = s.lcMaterial,
                    modalidad = s.lcModalidad,
                    radioBase = s.editingLc?.radioBase,
                    diametro = s.editingLc?.diametro,
                    laboratorioId = s.editingLc?.laboratorioId,
                    costoUnitario = costo,
                    vigenteDesde = s.editingLc?.vigenteDesde ?: DateUtils.toIso(DateUtils.today()),
                    vigenteHasta = s.editingLc?.vigenteHasta,
                )
                costoLcDao.upsertAll(listOf(entity))
                _uiState.update { it.copy(isLcDialogVisible = false, editingLc = null, lcSaveError = null) }
                postSaveSyncScheduler.scheduleFinanzasSync(opticaId)
            } catch (e: Exception) {
                _uiState.update { it.copy(lcSaveError = "Error al guardar: ${e.message}") }
            }
        }
    }

    fun confirmDeleteLc(entity: CostoLcEntity) {
        _uiState.update { it.copy(deletingLc = entity) }
    }

    fun dismissDeleteLc() {
        _uiState.update { it.copy(deletingLc = null) }
    }

    fun deleteLc() {
        val entity = _uiState.value.deletingLc ?: return
        viewModelScope.launch {
            try {
                costoLcDao.upsertAll(listOf(entity.copy(vigenteHasta = DateUtils.toIso(DateUtils.today()))))
                val opticaId = sessionManager.opticaId.first()
                _uiState.update { it.copy(deletingLc = null) }
                postSaveSyncScheduler.scheduleFinanzasSync(opticaId)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(lcSaveError = "Error al eliminar: ${e.message}") }
            }
        }
    }
}
