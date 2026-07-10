package com.example.optoapp.viewmodel

import android.database.sqlite.SQLiteConstraintException
import android.util.Log
import androidx.lifecycle.ViewModel
import java.io.IOException
import kotlinx.coroutines.CancellationException
import androidx.lifecycle.viewModelScope
import com.example.optoapp.data.FinanzasRemoteDefaults
import com.example.optoapp.data.OptoRepository
import com.example.optoapp.data.ServicioExtra
import com.example.optoapp.data.Paciente
import dagger.hilt.android.lifecycle.HiltViewModel
import com.example.optoapp.data.Resource
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject
import com.example.optoapp.data.Pago
import java.time.LocalDate
import com.example.optoapp.sync.PostSaveSyncScheduler
import com.example.optoapp.util.DateUtils

data class ServiciosUiState(
    val id: String = UUID.randomUUID().toString(),
    val ot: String = "",
    val descripcion: String = "",
    val montoTotal: String = "",
    val estado: String = "Pendiente",
    val fecha: LocalDate = DateUtils.today(),
    val fechaEntrega: LocalDate? = null,
    val pacienteId: String? = null,
    val isLoading: Boolean = false,
    val error: String? = null,

    val pagos: List<Pago> = emptyList(),
    val pagosToDelete: List<Pago> = emptyList(),
    val generatedId: String = UUID.randomUUID().toString(),
    val isEdit: Boolean = false
)

@HiltViewModel
class ServiciosViewModel @Inject constructor(
    private val repository: com.example.optoapp.data.OptoRepository,
    private val sessionManager: com.example.optoapp.data.SessionManager,
    private val postSaveSyncScheduler: PostSaveSyncScheduler
) : ViewModel() {

    companion object {
        private const val TAG = "ServiciosViewModel"
    }

    private val _uiState = MutableStateFlow(ServiciosUiState())
    val uiState: StateFlow<ServiciosUiState> = _uiState.asStateFlow()

    // -- Delete confirmation dialog state --

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
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Reactive aCuenta sum map for dynamic saldo computation (aCuenta is @Ignore in entity)
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

    fun updateUiState(update: (ServiciosUiState) -> ServiciosUiState) {
        _uiState.value = update(_uiState.value)
    }

    fun updateEstado(estado: String) {
        _uiState.update {
            it.copy(
                estado = estado,
                fechaEntrega = if (estado == "Entregado") DateUtils.today() else it.fechaEntrega
            )
        }
    }

    fun loadServicio(id: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, generatedId = id)
            when (val result = repository.getServicioById(id)) {
                is Resource.Success -> {
                    val s = result.data ?: return@launch
                    val loadedPagos = repository.getPagosByServicioExtra(id).first()
                    _uiState.value = ServiciosUiState(
                        id = s.id,
                        ot = s.ot,
                        descripcion = s.descripcion,
                        montoTotal = s.montoTotal.toString(),
                        estado = s.estado,
                        fecha = s.fecha,
                        fechaEntrega = s.fechaEntrega,
                        pacienteId = s.pacienteId,
                        pagos = loadedPagos,
                        generatedId = id,
                        isEdit = true,
                        isLoading = false,
                        error = null
                    )
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
            val state = _uiState.value
            if (state.descripcion.isBlank() || state.montoTotal.isBlank()) {
                _uiState.update {
                    it.copy(error = "Completa la descripción y el monto total para guardar.")
                }
                return@launch
            }
            val montoParsed = state.montoTotal.toDoubleOrNull()
            if (montoParsed == null) {
                _uiState.update { it.copy(error = "El monto total no es un número válido.") }
                return@launch
            }
            if (montoParsed <= 0.0) {
                _uiState.update { it.copy(error = FinanzasRemoteDefaults.Messages.MONTO_TOTAL_MAYOR_A_CERO) }
                return@launch
            }
            if (state.pagos.any { it.monto <= 0.0 }) {
                _uiState.update { it.copy(error = FinanzasRemoteDefaults.Messages.ABONO_MAYOR_A_CERO) }
                return@launch
            }
            val totalAbonos = state.pagos.sumOf { it.monto }
            if (totalAbonos > montoParsed) {
                _uiState.update { it.copy(error = FinanzasRemoteDefaults.Messages.ABONO_MAYOR_QUE_TOTAL) }
                return@launch
            }

            try {
                val currentOpticaId = sessionManager.opticaId.first().trim().ifBlank {
                    com.example.optoapp.data.SessionManager.LEGACY_OPTICA_ID
                }
                val finalId = if (state.id.isNotBlank()) state.id else state.generatedId

                val servicio = ServicioExtra(
                    id = finalId,
                    ot = state.ot.trim(),
                    descripcion = state.descripcion.trim(),
                    montoTotal = montoParsed,
                    aCuenta = state.pagos.filter { it.tipo != "Anulación" }.sumOf { it.monto },
                    estado = state.estado,
                    fecha = state.fecha,
                    fechaEntrega = state.fechaEntrega,
                    pacienteId = state.pacienteId?.takeIf { !it.isBlank() },
                    metodoPago = FinanzasRemoteDefaults.ServicioExtra.METODO_PAGO_ROW,
                    opticaId = currentOpticaId
                )

                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                repository.runInTransaction {
                    kotlinx.coroutines.runBlocking {
                    if (state.isEdit) {
                        repository.updateServicio(servicio)
                    } else {
                        repository.insertServicio(servicio)
                    }

                    state.pagos.forEach { pago ->
                        val pagoToSave = pago.copy(
                            servicioExtraId = finalId,
                            opticaId = currentOpticaId,
                            ventaId = "v_serv_$finalId"
                        )
                        repository.insertPago(pagoToSave)
                    }

                    state.pagosToDelete.forEach { pago ->
                        repository.deletePagoRegistrandoAnulacionEnCaja(pago, currentOpticaId)
                    }

                    }
                }
                }

                _uiState.update { it.copy(error = null) }

                postSaveSyncScheduler.scheduleFinanzasSync(currentOpticaId)

                onSuccess()
            } catch (e: SQLiteConstraintException) {
                Log.e(TAG, "Guardar servicio: restricción BD", e)
                _uiState.update {
                    it.copy(
                        error = "No se pudo guardar: revisa el paciente asociado o deja el servicio sin paciente."
                    )
                }
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
                // Anular: crear pagos inversos para cada abono existente
                val existingPagos = repository.getPagosByServicioExtra(servicio.id).first()
                    .filter { it.tipo != "Anulación" }
                existingPagos.forEach { pago ->
                    val anulacionPago = pago.copy(
                        id = UUID.randomUUID().toString(),
                        tipo = "Anulación",
                        monto = -pago.monto,
                        nota = "Anulación de servicio ${servicio.descripcion.take(24)}",
                        ventaId = "v_serv_${servicio.id}"
                    )
                    repository.insertPago(anulacionPago)
                }
                // Marcar como Anulado en vez de hard-delete
                repository.updateServicio(servicio.copy(estado = "Anulado"))

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
}
