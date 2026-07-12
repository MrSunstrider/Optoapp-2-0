package com.example.optoapp.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.optoapp.data.OptoRepository
import com.example.optoapp.data.SessionManager
import com.example.optoapp.data.costobiselado.CostoBiseladoDao
import com.example.optoapp.data.costoproducto.CostoProductoDao
import com.example.optoapp.data.costoproducto.CostoProductoEntity
import com.example.optoapp.data.gastooperativo.GastoOperativoEntity
import com.example.optoapp.domain.SyncFinanzasUseCase
import com.example.optoapp.sync.PostSaveSyncScheduler
import com.example.optoapp.util.DateUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject

/** 8 cost blocks organized by product category. Each block maps to stock_o_fabricacion values. */
val COST_BLOCKS = listOf(
    "Stock Monofocal",
    "Stock Bifocal",
    "Stock Multifocal",
    "Fabricación Resina",
    "Fabricación Cristal",
    "Monturas",
    "Biselado",
    "Lentes Contacto"
)

/** Maps display block name to stock_o_fabricacion filter value. */
fun blockToFilter(block: String): String = when (block) {
    "Stock Monofocal", "Stock Bifocal", "Stock Multifocal" -> "stock"
    "Fabricación Resina", "Fabricación Cristal" -> "fabricacion"
    "Monturas" -> "montura"
    "Biselado" -> "biselado"
    "Lentes Contacto" -> "lente_contacto"
    else -> block
}

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
    // Dialog state for gastos operativos CRUD
    val showDialog: Boolean = false,
    val editingGasto: GastoOperativoEntity? = null,
    val categoria: String = "alquiler",
    val descripcion: String = "",
    val monto: String = "",
    val fecha: LocalDate = DateUtils.today(),
    val nota: String = "",
    val esRecurrente: Boolean = false
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class CostosYGastosViewModel @Inject constructor(
    private val repository: OptoRepository,
    private val costoProductoDao: CostoProductoDao,
    private val costoBiseladoDao: CostoBiseladoDao,
    private val sessionManager: SessionManager,
    private val postSaveSyncScheduler: PostSaveSyncScheduler,
    private val syncFinanzasUseCase: SyncFinanzasUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(CostosYGastosUiState())
    val uiState: StateFlow<CostosYGastosUiState> = _uiState.asStateFlow()

    private var syncTriggered = false

    companion object {
        private const val TAG = "CostosYGastosVM"
        val CATEGORIAS = listOf("alquiler", "servicios", "personal", "proveedores", "insumos", "marketing", "impuestos", "otro")
    }

    init {
        // Load gastos operativos on init (same pattern as GastosViewModel)
        viewModelScope.launch {
            sessionManager.opticaId.flatMapLatest { repository.getGastosOperativos(it) }
                .catch { e ->
                    Log.e(TAG, "Gastos flow crashed, restarting", e)
                    emit(emptyList())
                }
                .collect { gastos ->
                    _uiState.update { it.copy(gastosOperativos = gastos) }
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
    }

    // ─── Tab management ───────────────────────────────────────────────────

    fun selectTab(index: Int) {
        _uiState.update { it.copy(selectedTab = index) }
    }

    // ─── Block management ─────────────────────────────────────────────────

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

    // ─── Cost override editing (R6: manual override persists) ──────────

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
                // Refresh block data
                val opticaId = sessionManager.opticaId.first()
                val bloqueFilter = blockToFilter(s.selectedBlock ?: return@launch)
                val refreshed = costoProductoDao.getByBloque(opticaId, bloqueFilter).first()
                _uiState.update { it.copy(editingCosto = null, nuevoCostoUnitario = "", costosDelBloque = refreshed, error = null) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Error al guardar: ${e.message}") }
            }
        }
    }

    // ─── Gastos Operativos CRUD (replicates GastosViewModel pattern) ──────

    val categorias = CATEGORIAS

    fun showNewGasto() {
        _uiState.update { it.copy(showDialog = true, editingGasto = null, categoria = "alquiler", descripcion = "", monto = "", fecha = DateUtils.today(), nota = "", esRecurrente = false) }
    }

    fun editGasto(gasto: GastoOperativoEntity) {
        _uiState.update {
            it.copy(
                showDialog = true,
                editingGasto = gasto,
                categoria = gasto.categoria,
                descripcion = gasto.descripcion ?: "",
                monto = gasto.monto.toString(),
                fecha = gasto.fecha,
                nota = gasto.nota ?: "",
                esRecurrente = gasto.esRecurrente
            )
        }
    }

    fun dismissDialog() {
        _uiState.update { it.copy(showDialog = false, editingGasto = null, error = null) }
    }

    fun updateCategoria(c: String) { _uiState.update { it.copy(categoria = c) } }
    fun updateDescripcion(d: String) { _uiState.update { it.copy(descripcion = d) } }
    fun updateMonto(m: String) { _uiState.update { it.copy(monto = m) } }
    fun updateFecha(f: LocalDate) { _uiState.update { it.copy(fecha = f) } }
    fun updateNota(n: String) { _uiState.update { it.copy(nota = n) } }
    fun toggleRecurrente() { _uiState.update { it.copy(esRecurrente = !it.esRecurrente) } }

    fun saveGasto() {
        val s = _uiState.value
        val monto = s.monto.toDoubleOrNull()
        if (monto == null || monto <= 0) {
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
                    esRecurrente = s.esRecurrente
                )
                repository.upsertGastoOperativo(gasto)
                postSaveSyncScheduler.scheduleFinanzasSync(opticaId)
                _uiState.update { it.copy(showDialog = false, editingGasto = null, error = null) }
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
}
