package com.example.optoapp.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.optoapp.data.AppRoles
import com.example.optoapp.data.OptoRepository
import com.example.optoapp.data.SessionManager
import com.example.optoapp.data.costobiselado.CostoBiseladoDao
import com.example.optoapp.data.costoproducto.CostoProductoDao
import com.example.optoapp.data.costoproducto.CostoProductoEntity
import com.example.optoapp.data.gastooperativo.GastoOperativoEntity
import com.example.optoapp.domain.GastoTipoResolver
import com.example.optoapp.domain.OpticalCatalog
import com.example.optoapp.domain.SyncFinanzasUseCase
import com.example.optoapp.sync.PostSaveSyncScheduler
import com.example.optoapp.util.DateUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth
import java.util.Locale
import java.util.UUID
import javax.inject.Inject

val COST_BLOCKS = listOf(
    "Stock Monofocal",
    "Stock Bifocal",
    "Stock Multifocal",
    "Fabricación Resina",
    "Fabricación Cristal",
)

fun blockToFilter(block: String): String = when (block) {
    "Stock Monofocal", "Stock Bifocal", "Stock Multifocal" -> "stock"
    "Fabricación Resina", "Fabricación Cristal" -> "fabricacion"
    else -> block
}

data class GastoFila(val gasto: GastoOperativoEntity, val tipo: String)

data class GrupoGastos(
    val categoria: String,
    val etiqueta: String,
    val subtotal: BigDecimal,
    val filas: List<GastoFila>,
)

fun agruparGastosDelMes(gastos: List<GastoOperativoEntity>, mes: YearMonth): List<GrupoGastos> =
    gastos
        .filter { YearMonth.from(it.fecha) == mes }
        .groupBy { it.categoria }
        .map { (categoria, delGrupo) ->
            GrupoGastos(
                categoria = categoria,
                etiqueta = categoria.replaceFirstChar { it.titlecase(Locale.ROOT) },
                subtotal = delGrupo.fold(BigDecimal.ZERO) { acc, g -> acc + g.monto },
                filas = delGrupo
                    .sortedByDescending { it.fecha }
                    .map { GastoFila(it, GastoTipoResolver.resolver(it.categoria, it.tipoGasto)) },
            )
        }
        .sortedWith(compareByDescending<GrupoGastos> { it.subtotal }.thenBy { it.categoria })

data class CostosYGastosUiState(
    val selectedTab: Int = 0,
    val selectedBlock: String? = null,
    val costosDelBloque: List<CostoProductoEntity> = emptyList(),
    val editingCosto: CostoProductoEntity? = null,
    val nuevoCostoUnitario: String = "",
    val gastosOperativos: List<GastoOperativoEntity> = emptyList(),
    val mesGastos: YearMonth = YearMonth.from(DateUtils.today()),
    val gruposGastos: List<GrupoGastos> = emptyList(),
    val totalGastosMes: BigDecimal = BigDecimal.ZERO,
    val puedeEditarGastos: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isDialogVisible: Boolean = false,
    val editingGasto: GastoOperativoEntity? = null,
    val categoria: String = "alquiler",
    val descripcion: String = "",
    val monto: String = "",
    val fecha: LocalDate = DateUtils.today(),
    val nota: String = "",
    val isRecurring: Boolean = false,
    val gastoFormError: String? = null,
    val deletingGasto: GastoOperativoEntity? = null,
    val gastoDeleteError: String? = null,
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
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class CostosYGastosViewModel @Inject constructor(
    private val repository: OptoRepository,
    private val costoProductoDao: CostoProductoDao,
    private val costoBiseladoDao: CostoBiseladoDao,
    private val sessionManager: SessionManager,
    private val postSaveSyncScheduler: PostSaveSyncScheduler,
    private val syncFinanzasUseCase: SyncFinanzasUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CostosYGastosUiState())
    val uiState: StateFlow<CostosYGastosUiState> = _uiState.asStateFlow()

    private var syncTriggered = false

    companion object {
        private const val TAG = "CostosYGastosVM"
        val CATEGORIAS = listOf("alquiler", "servicios", "personal", "proveedores", "insumos", "marketing", "impuestos", "otro")

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
            sessionManager.opticaId.flatMapLatest { repository.getGastosOperativos(it) }
                .catch { e ->
                    Log.e(TAG, "Gastos flow crashed, restarting", e)
                    emit(emptyList())
                }
                .collect { gastos ->
                    val visibles = try {
                        autoGenerarSiFalta(gastos)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        Log.e(TAG, "autoGenerarSiFalta failed, showing raw gastos", e)
                        gastos
                    }
                    _uiState.update { conGastosDelMes(it.copy(gastosOperativos = visibles.sortedByDescending { g -> g.fecha })) }
                    if (!syncTriggered && gastos.isEmpty()) {
                        syncTriggered = true
                        val opticaId = sessionManager.opticaId.first()
                        Log.d(TAG, "Triggering finanzas download for gastos (opticaId=$opticaId)")
                        viewModelScope.launch {
                            syncFinanzasUseCase(opticaId, downloadAfterUpload = true, skipUpload = true)
                        }
                    }
                }
        }
        viewModelScope.launch {
            sessionManager.opticaRol
                .catch { e ->
                    Log.e(TAG, "Role flow crashed, gastos stay read-only", e)
                    emit("")
                }
                .collect { rol -> _uiState.update { it.copy(puedeEditarGastos = AppRoles.canWriteGastos(rol)) } }
        }
    }

    private fun conGastosDelMes(state: CostosYGastosUiState): CostosYGastosUiState {
        val grupos = agruparGastosDelMes(state.gastosOperativos, state.mesGastos)
        return state.copy(
            gruposGastos = grupos,
            totalGastosMes = grupos.fold(BigDecimal.ZERO) { acc, g -> acc + g.subtotal },
        )
    }

    fun selectMesGastos(mes: YearMonth) {
        _uiState.update { conGastosDelMes(it.copy(mesGastos = mes)) }
    }

    private suspend fun autoGenerarSiFalta(gastos: List<GastoOperativoEntity>): List<GastoOperativoEntity> {
        val nuevos = autoGenerarRecurrentes(
            templates = gastos.filter { it.isRecurring },
            existentes = gastos,
            mesActual = DateUtils.today(),
        )
        if (nuevos.isEmpty()) return gastos
        val opticaId = sessionManager.opticaId.first()
        nuevos.forEach { repository.upsertGastoOperativo(it) }
        postSaveSyncScheduler.scheduleFinanzasSync(opticaId)
        return gastos + nuevos
    }

    fun refreshGastos() {
        viewModelScope.launch {
            try {
                val opticaId = sessionManager.opticaId.first()
                syncFinanzasUseCase(opticaId, downloadAfterUpload = true, skipUpload = true)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "refreshGastos failed", e)
            }
        }
    }

    fun selectTab(index: Int) {
        _uiState.update { it.copy(selectedTab = index) }
    }

    fun loadBlock(block: String) {
        _uiState.update { it.copy(selectedBlock = block, isLoading = true) }
        viewModelScope.launch {
            try {
                val opticaId = sessionManager.opticaId.first()
                val bloqueFilter = blockToFilter(block)
                val costos = costoProductoDao.getByBloque(opticaId, bloqueFilter).first()
                _uiState.update { it.copy(isLoading = false, costosDelBloque = costos) }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading block $block", e)
                _uiState.update { it.copy(isLoading = false, error = "Error al cargar bloque: ${e.message}") }
            }
        }
    }

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
                val bloqueFilter = blockToFilter(s.selectedBlock ?: return@launch)
                val refreshed = costoProductoDao.getByBloque(opticaId, bloqueFilter).first()
                _uiState.update { it.copy(editingCosto = null, nuevoCostoUnitario = "", costosDelBloque = refreshed, error = null) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Error al guardar: ${e.message}") }
            }
        }
    }

    val categorias = CATEGORIAS

    fun showNewGasto() {
        if (!_uiState.value.puedeEditarGastos) return
        _uiState.update {
            it.copy(
                isDialogVisible = true,
                editingGasto = null,
                categoria = "alquiler",
                descripcion = "",
                monto = "",
                fecha = DateUtils.today(),
                nota = "",
                isRecurring = false,
                gastoFormError = null,
            )
        }
    }

    fun editGasto(gasto: GastoOperativoEntity) {
        if (!_uiState.value.puedeEditarGastos) return
        _uiState.update {
            it.copy(
                isDialogVisible = true,
                editingGasto = gasto,
                categoria = gasto.categoria,
                descripcion = gasto.descripcion ?: "",
                monto = gasto.monto.toPlainString(),
                fecha = gasto.fecha,
                nota = gasto.nota ?: "",
                isRecurring = gasto.isRecurring,
                gastoFormError = null,
            )
        }
    }

    fun dismissDialog() {
        _uiState.update { it.copy(isDialogVisible = false, editingGasto = null, gastoFormError = null) }
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
        if (s.isLoading) return
        val monto = s.monto.toBigDecimalOrNull()
        if (monto == null || monto <= BigDecimal.ZERO) {
            _uiState.update { it.copy(gastoFormError = "Ingresa un monto válido") }
            return
        }
        _uiState.update { it.copy(isLoading = true, gastoFormError = null) }
        viewModelScope.launch {
            try {
                val opticaId = sessionManager.opticaId.first()
                val gasto = gastoDesdeFormulario(s, opticaId, monto)
                repository.upsertGastoOperativo(gasto)
                postSaveSyncScheduler.scheduleFinanzasSync(opticaId)
                _uiState.update { it.copy(isDialogVisible = false, editingGasto = null) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "saveGasto failed", e)
                _uiState.update { it.copy(gastoFormError = "No se pudo guardar el gasto. Inténtalo de nuevo.") }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    // Rebuilding the entity from the form alone would wipe fechaProgramada and tipoGasto, which the
    // recurrence generator and margin reports rely on, every time a user edits a gasto.
    private fun gastoDesdeFormulario(s: CostosYGastosUiState, opticaId: String, monto: BigDecimal): GastoOperativoEntity {
        val base = s.editingGasto ?: GastoOperativoEntity(
            id = UUID.randomUUID().toString(),
            opticaId = opticaId,
            categoria = s.categoria,
            monto = monto,
            fecha = s.fecha,
        )
        return base.copy(
            categoria = s.categoria,
            descripcion = s.descripcion.ifBlank { null },
            monto = monto,
            fecha = s.fecha,
            nota = s.nota.ifBlank { null },
            isRecurring = s.isRecurring,
        )
    }

    fun confirmDeleteGasto(gasto: GastoOperativoEntity) {
        if (!_uiState.value.puedeEditarGastos) return
        _uiState.update { it.copy(deletingGasto = gasto, gastoDeleteError = null) }
    }

    fun dismissDeleteGasto() {
        _uiState.update { it.copy(deletingGasto = null, gastoDeleteError = null) }
    }

    fun deleteGasto() {
        val s = _uiState.value
        val gasto = s.deletingGasto ?: return
        if (s.isLoading || !s.puedeEditarGastos) return
        _uiState.update { it.copy(isLoading = true, gastoDeleteError = null) }
        viewModelScope.launch {
            try {
                repository.deleteGastoOperativo(gasto)
                postSaveSyncScheduler.scheduleFinanzasSync(gasto.opticaId)
                _uiState.update { it.copy(deletingGasto = null) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "deleteGasto failed", e)
                _uiState.update { it.copy(gastoDeleteError = "No se pudo eliminar el gasto. Inténtalo de nuevo.") }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
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
                costoStockOFabricacion = block?.let { blockToFilter(it) } ?: "",
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
                val bloqueFilter = blockToFilter(s.selectedBlock ?: return@launch)
                val refreshed = costoProductoDao.getByBloque(opticaId, bloqueFilter).first()
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
                val bloqueFilter = blockToFilter(_uiState.value.selectedBlock ?: return@launch)
                val refreshed = costoProductoDao.getByBloque(opticaId, bloqueFilter).first()
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
}
