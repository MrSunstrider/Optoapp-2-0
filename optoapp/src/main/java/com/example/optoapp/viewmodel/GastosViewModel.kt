package com.example.optoapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.optoapp.data.OptoRepository
import com.example.optoapp.data.SessionManager
import com.example.optoapp.data.gastooperativo.GastoOperativoEntity
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

data class GastosUiState(
    val gastos: List<GastoOperativoEntity> = emptyList(),
    val showDialog: Boolean = false,
    val editingGasto: GastoOperativoEntity? = null,
    val categoria: String = "alquiler",
    val descripcion: String = "",
    val monto: String = "",
    val fecha: LocalDate = DateUtils.today(),
    val nota: String = "",
    val esRecurrente: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class GastosViewModel @Inject constructor(
    private val repository: OptoRepository,
    private val sessionManager: SessionManager,
    private val postSaveSyncScheduler: PostSaveSyncScheduler
) : ViewModel() {

    private val _uiState = MutableStateFlow(GastosUiState())
    val uiState: StateFlow<GastosUiState> = _uiState.asStateFlow()

    private val _allGastos = MutableStateFlow<List<GastoOperativoEntity>>(emptyList())
    val allGastos: StateFlow<List<GastoOperativoEntity>> = _allGastos.asStateFlow()

    init {
        viewModelScope.launch {
            sessionManager.opticaId.flatMapLatest { repository.getGastosOperativos(it) }
                .collect { gastos ->
                    _allGastos.value = autoGenerarSiFalta(gastos)
                }
        }
    }

    private suspend fun autoGenerarSiFalta(gastos: List<GastoOperativoEntity>): List<GastoOperativoEntity> {
        val hoy = DateUtils.today()
        val mesInicio = hoy.withDayOfMonth(1)
        val nuevos = autoGenerarRecurrentes(
            templates = gastos.filter { it.esRecurrente },
            existentes = gastos,
            mesActual = hoy
        )
        if (nuevos.isEmpty()) return gastos
        val opticaId = sessionManager.opticaId.first()
        nuevos.forEach { repository.upsertGastoOperativo(it) }
        postSaveSyncScheduler.scheduleFinanzasSync(opticaId)
        return gastos + nuevos
    }

    companion object {
        val CATEGORIAS = listOf("alquiler", "servicios", "personal", "proveedores", "insumos", "marketing", "impuestos", "otro")

        fun autoGenerarRecurrentes(
            templates: List<GastoOperativoEntity>,
            existentes: List<GastoOperativoEntity>,
            mesActual: LocalDate
        ): List<GastoOperativoEntity> {
            val mesInicio = mesActual.withDayOfMonth(1)
            val mesFin = mesActual.withDayOfMonth(mesActual.lengthOfMonth())
            return templates
                .filter { it.esRecurrente }
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
                        esRecurrente = false,
                        nota = "Auto-generado de ${template.categoria}"
                    )
                }
        }
    }

    val categorias = CATEGORIAS

    fun showNewGasto() {
        _uiState.value = GastosUiState(showDialog = true)
    }

    fun editGasto(gasto: GastoOperativoEntity) {
        _uiState.value = GastosUiState(
            showDialog = true,
            editingGasto = gasto,
            categoria = gasto.categoria,
            descripcion = gasto.descripcion ?: "",
            monto = gasto.monto.toString(),
            fecha = gasto.fecha,
            nota = gasto.nota ?: ""
        )
    }

    fun dismissDialog() {
        _uiState.value = GastosUiState()
    }

    fun updateCategoria(c: String) { _uiState.update { it.copy(categoria = c) } }
    fun updateDescripcion(d: String) { _uiState.update { it.copy(descripcion = d) } }
    fun updateMonto(m: String) { _uiState.update { it.copy(monto = m) } }
    fun updateFecha(f: LocalDate) { _uiState.update { it.copy(fecha = f) } }
    fun updateNota(n: String) { _uiState.update { it.copy(nota = n) } }
    fun toggleRecurrente() { _uiState.update { it.copy(esRecurrente = !it.esRecurrente) } }

    fun save() {
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
                _uiState.value = GastosUiState()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Error al guardar") }
            }
        }
    }

    fun delete(gasto: GastoOperativoEntity) {
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
