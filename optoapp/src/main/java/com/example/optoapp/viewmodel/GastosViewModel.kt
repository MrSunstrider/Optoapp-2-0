package com.example.optoapp.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.optoapp.data.OptoRepository
import com.example.optoapp.data.SessionManager
import com.example.optoapp.data.gastooperativo.GastoOperativoEntity
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
import java.util.UUID
import javax.inject.Inject

data class GastosUiState(
    val gastos: List<GastoOperativoEntity> = emptyList(),
    val isDialogVisible: Boolean = false,
    val editingGasto: GastoOperativoEntity? = null,
    val categoria: String = "alquiler",
    val descripcion: String = "",
    val monto: String = "",
    val fecha: LocalDate = DateUtils.today(),
    val nota: String = "",
    val isRecurring: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class GastosViewModel @Inject constructor(
    private val repository: OptoRepository,
    private val sessionManager: SessionManager,
    private val postSaveSyncScheduler: PostSaveSyncScheduler,
    private val syncFinanzasUseCase: SyncFinanzasUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(GastosUiState())
    val uiState: StateFlow<GastosUiState> = _uiState.asStateFlow()

    private val _allGastos = MutableStateFlow<List<GastoOperativoEntity>>(emptyList())
    val allGastos: StateFlow<List<GastoOperativoEntity>> = _allGastos.asStateFlow()

    private var syncTriggered = false

    init {
        viewModelScope.launch {
            sessionManager.opticaId.flatMapLatest { repository.getGastosOperativos(it) }
                .catch { e ->
                    Log.e(TAG, "GastosViewModel Flow crashed, restarting", e)
                    emit(emptyList())
                }
                .collect { gastos ->
                    try {
                        _allGastos.value = autoGenerarSiFalta(gastos).sortedByDescending { it.fecha }
                    } catch (e: Exception) {
                        Log.e(TAG, "autoGenerarSiFalta failed, showing raw gastos", e)
                        _allGastos.value = gastos.sortedByDescending { it.fecha }
                    }
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

    private suspend fun autoGenerarSiFalta(gastos: List<GastoOperativoEntity>): List<GastoOperativoEntity> {
        val hoy = DateUtils.today()
        val nuevos = CostosYGastosViewModel.autoGenerarRecurrentes(
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

    companion object {
        private const val TAG = "GastosVM"
        val CATEGORIAS = CostosYGastosViewModel.CATEGORIAS
    }

    val categorias = CATEGORIAS

    fun showNewGasto() {
        _uiState.value = GastosUiState(isDialogVisible = true)
    }

    fun refreshGastos() {
        viewModelScope.launch {
            try {
                val opticaId = sessionManager.opticaId.first()
                syncFinanzasUseCase(opticaId, downloadAfterUpload = true, skipUpload = true)
            } catch (e: Exception) {
                Log.e(TAG, "refreshGastos failed", e)
            }
        }
    }

    fun editGasto(gasto: GastoOperativoEntity) {
        _uiState.value = GastosUiState(
            isDialogVisible = true,
            editingGasto = gasto,
            categoria = gasto.categoria,
            descripcion = gasto.descripcion ?: "",
            monto = gasto.monto.toString(),
            fecha = gasto.fecha,
            nota = gasto.nota ?: "",
        )
    }

    fun dismissDialog() {
        _uiState.value = GastosUiState()
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

    fun save() {
        if (_uiState.value.isLoading) return
        val s = _uiState.value
        val monto = s.monto.toBigDecimalOrNull()
        if (monto == null || monto <= BigDecimal.ZERO) {
            _uiState.update { it.copy(error = "Ingresa un monto válido") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
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
                _uiState.value = GastosUiState(isLoading = false)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Error al guardar", isLoading = false) }
            }
        }
    }

    fun delete(gasto: GastoOperativoEntity) {
        if (_uiState.value.isLoading) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                repository.deleteGastoOperativo(gasto)
                postSaveSyncScheduler.scheduleFinanzasSync(gasto.opticaId)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Error al eliminar el gasto", isLoading = false) }
            }
        }
    }
}
