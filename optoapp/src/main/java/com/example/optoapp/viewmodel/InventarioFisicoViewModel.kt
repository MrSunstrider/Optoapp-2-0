package com.example.optoapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.optoapp.data.InventarioFisico
import com.example.optoapp.data.InventarioFisicoDetalle
import com.example.optoapp.data.InventarioFisicoRepository
import com.example.optoapp.data.Montura
import com.example.optoapp.data.OptoRepository
import com.example.optoapp.data.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MonturaConteoLabel(
    val titulo: String,
    val subtitulo: String,
)

internal fun buildMonturaConteoLabels(monturas: List<Montura>): Map<String, MonturaConteoLabel> =
    monturas.associate { m ->
        val specs = listOfNotNull(
            m.color.takeIf { it.isNotBlank() },
            m.talla.takeIf { it.isNotBlank() }?.let { "Talla $it" },
        ).joinToString(" · ")
        m.id to MonturaConteoLabel(
            titulo = "${m.marca} ${m.modelo}".trim().ifBlank { m.sku.ifBlank { "Sin datos" } },
            subtitulo = buildString {
                append("SKU ${m.sku.ifBlank { "—" }}")
                if (specs.isNotBlank()) append(" · ").append(specs)
            },
        )
    }

sealed class InventarioFisicoUiState {
    object Loading : InventarioFisicoUiState()
    data class SessionList(val sessions: List<InventarioFisico>) : InventarioFisicoUiState()
    data class Detail(
        val session: InventarioFisico,
        val detalles: List<InventarioFisicoDetalle>,
        val labelsByMonturaId: Map<String, MonturaConteoLabel> = emptyMap(),
        val progressMessage: String? = null,
    ) : InventarioFisicoUiState()
    data class Error(val message: String) : InventarioFisicoUiState()
}

@HiltViewModel
class InventarioFisicoViewModel @Inject constructor(
    private val repository: InventarioFisicoRepository,
    private val optoRepository: OptoRepository,
    private val sessionManager: SessionManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow<InventarioFisicoUiState>(InventarioFisicoUiState.Loading)
    val uiState: StateFlow<InventarioFisicoUiState> = _uiState

    fun loadSessions() {
        viewModelScope.launch {
            _uiState.value = InventarioFisicoUiState.Loading
            val opticaId = sessionManager.opticaId.first()
            repository.getByOptica(opticaId).collect { sessions ->
                _uiState.value = InventarioFisicoUiState.SessionList(sessions)
            }
        }
    }

    fun createSession() {
        viewModelScope.launch {
            val opticaId = sessionManager.opticaId.first()
            val userId = sessionManager.userEmail.first()
            val session = repository.createSession(opticaId, userId)
            if (session == null) {
                _uiState.value = InventarioFisicoUiState.Error(
                    "Ya hay un conteo de monturas en progreso. Completalo antes de iniciar otro.",
                )
            } else {
                loadSessionDetail(session.id)
            }
        }
    }

    fun loadSessionDetail(sessionId: String) {
        viewModelScope.launch {
            val opticaId = sessionManager.opticaId.first()
            val session = repository.getById(sessionId, opticaId)
            if (session == null) {
                _uiState.value = InventarioFisicoUiState.Error("Conteo no encontrado")
                return@launch
            }
            val detalles = repository.getDetalles(sessionId)
            val labels = try {
                buildMonturaConteoLabels(
                    optoRepository.getMonturasSnapshotForOptica(session.opticaId),
                )
            } catch (_: Exception) {
                emptyMap()
            }
            val counted = detalles.count { it.stockContado != null }
            _uiState.value = InventarioFisicoUiState.Detail(
                session = session,
                detalles = detalles,
                labelsByMonturaId = labels,
                progressMessage = if (detalles.isNotEmpty()) {
                    "$counted de ${detalles.size} monturas contadas"
                } else {
                    null
                },
            )
        }
    }

    fun updateStockContado(detalleId: String, stockContado: Int) {
        viewModelScope.launch {
            val current = _uiState.value
            if (current !is InventarioFisicoUiState.Detail) return@launch
            val detalle = current.detalles.find { it.id == detalleId } ?: return@launch
            val diferencia = stockContado - detalle.stockSistema

            repository.upsertDetalle(
                detalle.copy(stockContado = stockContado, diferencia = diferencia),
            )
            loadSessionDetail(current.session.id)
        }
    }

    fun closeSession() {
        viewModelScope.launch {
            val current = _uiState.value
            if (current !is InventarioFisicoUiState.Detail) return@launch
            repository.closeSession(current.session.id, current.session.opticaId)
            loadSessions()
        }
    }
}
