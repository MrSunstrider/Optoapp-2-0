package com.example.optoapp.data

import android.util.Log
import androidx.room.withTransaction
import dagger.Lazy
import kotlinx.coroutines.flow.Flow

import com.example.optoapp.data.montura.MonturaDao
import com.example.optoapp.data.montura.MonturaMovimientoDao
import com.example.optoapp.data.pago.PagoDao
import com.example.optoapp.data.servicio.ServicioExtraDao
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject
import java.time.LocalDate
import java.util.UUID
import com.example.optoapp.util.DateUtils

class OptoRepository(
    private val database: OptoDatabase,
    private val pacienteDao: PacienteDao,
    private val evaluacionDao: EvaluacionDao,
    private val dispensacionDao: DispensacionDao,
    private val pagoDao: PagoDao,
    private val servicioExtraDao: ServicioExtraDao,
    private val monturaDao: MonturaDao,
    private val monturaMovimientoDao: MonturaMovimientoDao,
    private val syncStateTracker: SyncStateTracker,
    private val postSaveSyncScheduler: Lazy<com.example.optoapp.sync.PostSaveSyncScheduler>,
    // Repositorios especializados
    val pacienteRepo: PacienteRepository,
    val dispensacionRepo: DispensacionRepository,
    val syncRepo: SyncRepository
) {
    companion object {
        private const val TAG = "OptoRepository"
    }

    private fun triggerFinanzasSync(opticaId: String) {
        postSaveSyncScheduler.get().scheduleFinanzasSync(opticaId)
    }

    private fun triggerInventarioSync(opticaId: String) {
        postSaveSyncScheduler.get().scheduleInventarioSync(opticaId)
    }

    private fun triggerPacientesSync(opticaId: String) {
        postSaveSyncScheduler.get().schedulePacientesSync(opticaId)
    }

    private fun triggerHistorialSync(opticaId: String) {
        postSaveSyncScheduler.get().scheduleHistorialSync(opticaId)
    }

    // ── Paciente ─────────────────────────────────────────────────────────────

    fun pacientesFlowForOptica(opticaId: String): Flow<List<Paciente>> =
        pacienteRepo.pacientesFlowForOptica(opticaId)

    fun countPacientesForOptica(opticaId: String): Flow<Int> =
        pacienteRepo.countPacientesForOptica(opticaId)

    fun searchPacientesForOptica(opticaId: String, query: String): Flow<List<Paciente>> =
        pacienteRepo.searchPacientesForOptica(opticaId, query)

    fun getPacientesWithPendingBalanceForOptica(opticaId: String): Flow<List<Paciente>> =
        pacienteRepo.getPacientesWithPendingBalanceForOptica(opticaId)

    fun getPacientesWithPendingDeliveryForOptica(opticaId: String): Flow<List<Paciente>> =
        pacienteRepo.getPacientesWithPendingDeliveryForOptica(opticaId)

    suspend fun getPacienteById(id: String): Resource<Paciente> =
        pacienteRepo.getPacienteById(id)

    suspend fun insertPaciente(paciente: Paciente) {
        pacienteRepo.insertPaciente(paciente)
        triggerPacientesSync(paciente.opticaId)
    }

    suspend fun updatePaciente(paciente: Paciente) {
        pacienteRepo.updatePaciente(paciente)
        triggerPacientesSync(paciente.opticaId)
    }

    suspend fun deletePaciente(paciente: Paciente) = pacienteRepo.deletePaciente(paciente)

    // ── Evaluación ───────────────────────────────────────────────────────────

    fun getEvaluacionesByPaciente(pacienteId: String): Flow<List<EvaluacionClinica>> =
        pacienteRepo.getEvaluacionesByPaciente(pacienteId)

    fun getEvaluacionesProximaCitaEnRango(opticaId: String, start: LocalDate, end: LocalDate): Flow<List<EvaluacionClinica>> =
        pacienteRepo.getEvaluacionesProximaCitaEnRango(opticaId, start, end)

    fun countEvaluacionesInRange(start: LocalDate, end: LocalDate): Flow<Int> =
        pacienteRepo.countEvaluacionesInRange(start, end)

    fun countEvaluacionesInRangeForOptica(start: LocalDate, end: LocalDate, opticaId: String): Flow<Int> =
        pacienteRepo.countEvaluacionesInRangeForOptica(start, end, opticaId)

    suspend fun getEvaluacionById(id: String): Resource<EvaluacionClinica> =
        pacienteRepo.getEvaluacionById(id)

    suspend fun deleteEvaluacion(evaluacion: EvaluacionClinica) = pacienteRepo.deleteEvaluacion(evaluacion)

    suspend fun insertEvaluacion(evaluacion: EvaluacionClinica) {
        pacienteRepo.insertEvaluacion(evaluacion)
        triggerHistorialSync(evaluacion.opticaId)
    }

    suspend fun updateEvaluacion(evaluacion: EvaluacionClinica) {
        pacienteRepo.updateEvaluacion(evaluacion)
        triggerHistorialSync(evaluacion.opticaId)
    }

    // ── Dispensación ─────────────────────────────────────────────────────────

    fun getDispensacionesByPaciente(pacienteId: String): Flow<List<DispensacionOptica>> =
        dispensacionRepo.getDispensacionesByPaciente(pacienteId)

    fun getAllDispensaciones(): Flow<List<DispensacionOptica>> =
        dispensacionRepo.getAllDispensaciones()

    fun getAllDispensacionesForOptica(opticaId: String): Flow<List<DispensacionOptica>> =
        dispensacionRepo.getAllDispensacionesForOptica(opticaId)

    fun getTotalVendido(): Flow<Double?> = dispensacionRepo.getTotalVendido()

    fun getTotalPagado(): Flow<Double?> = dispensacionRepo.getTotalPagado()

    fun getTotalVendidoForOptica(opticaId: String): Flow<Double?> =
        dispensacionRepo.getTotalVendidoForOptica(opticaId)

    fun getTotalPagadoForOptica(opticaId: String): Flow<Double?> =
        dispensacionRepo.getTotalPagadoForOptica(opticaId)

    fun getDispensacionesByDateRange(start: LocalDate, end: LocalDate): Flow<List<DispensacionOptica>> =
        dispensacionRepo.getDispensacionesByDateRange(start, end)

    fun getDispensacionesByDateRangeForOptica(start: LocalDate, end: LocalDate, opticaId: String): Flow<List<DispensacionOptica>> =
        dispensacionRepo.getDispensacionesByDateRangeForOptica(start, end, opticaId)

    suspend fun getDispensacionById(id: String): Resource<DispensacionOptica> =
        dispensacionRepo.getDispensacionById(id)

    suspend fun insertDispensacion(dispensacion: DispensacionOptica) {
        dispensacionRepo.insertDispensacion(dispensacion)
        triggerFinanzasSync(dispensacion.opticaId)
    }

    suspend fun updateDispensacion(dispensacion: DispensacionOptica) {
        dispensacionRepo.updateDispensacion(dispensacion)
        triggerFinanzasSync(dispensacion.opticaId)
    }

    suspend fun deleteDispensacionById(id: String): Int =
        dispensacionRepo.deleteDispensacionById(id)

    suspend fun deleteDispensacion(dispensacion: DispensacionOptica) {
        dispensacionRepo.deleteDispensacionById(dispensacion.id)
        syncStateTracker.markDeleted(dispensacion.opticaId, "dispensacion", dispensacion.id)
        triggerFinanzasSync(dispensacion.opticaId)
    }

    suspend fun existsDuplicateOt(opticaId: String, ot: String, excludeDispensacionId: String?): Boolean =
        dispensacionRepo.existsDuplicateOt(opticaId, ot, excludeDispensacionId)

    suspend fun suggestNextOt(opticaId: String, fecha: LocalDate): String =
        dispensacionRepo.suggestNextOt(opticaId, fecha)

    suspend fun suggestNextHistoriaOptometrica(opticaId: String): String =
        pacienteRepo.suggestNextHistoriaOptometrica(opticaId)

    suspend fun existsDuplicateHistoriaOptometrica(opticaId: String, historia: String, excludePacienteId: String?): Boolean =
        pacienteRepo.existsDuplicateHistoriaOptometrica(opticaId, historia, excludePacienteId)

    // ── Pagos ────────────────────────────────────────────────────────────────

    fun getPagosByDispensacion(dispensacionId: String): Flow<List<Pago>> =
        dispensacionRepo.getPagosByDispensacion(dispensacionId)

    suspend fun insertPago(pago: Pago) {
        dispensacionRepo.insertPago(pago)
        triggerFinanzasSync(pago.opticaId)
    }

    suspend fun getPagoById(id: String): Pago? = dispensacionRepo.getPagoById(id)

    suspend fun reassignPagosDispensacion(oldDispensacionId: String, newDispensacionId: String): Int =
        dispensacionRepo.reassignPagosDispensacion(oldDispensacionId, newDispensacionId)

    suspend fun deletePagoRegistrandoAnulacionEnCaja(
        pago: Pago,
        opticaId: String,
        fechaAnulacion: LocalDate = DateUtils.today()
    ) = dispensacionRepo.deletePagoRegistrandoAnulacionEnCaja(pago, opticaId, fechaAnulacion)

    suspend fun deletePago(pago: Pago) = dispensacionRepo.deletePago(pago)

    fun getPagosByServicioExtra(servicioExtraId: String): Flow<List<Pago>> =
        dispensacionRepo.getPagosByServicioExtra(servicioExtraId)

    fun getPagosByDateRange(start: LocalDate, end: LocalDate): Flow<List<Pago>> =
        dispensacionRepo.getPagosByDateRange(start, end)

    fun getPagosByDateRangeForOptica(start: LocalDate, end: LocalDate, opticaId: String): Flow<List<Pago>> =
        dispensacionRepo.getPagosByDateRangeForOptica(start, end, opticaId)

    // ── Servicios Extra ──────────────────────────────────────────────────────

    fun getAllServicios(): Flow<List<ServicioExtra>> =
        dispensacionRepo.getAllServicios()

    fun getAllServiciosForOptica(opticaId: String): Flow<List<ServicioExtra>> =
        dispensacionRepo.getAllServiciosForOptica(opticaId)

    fun getServiciosByPaciente(pacienteId: String): Flow<List<ServicioExtra>> =
        dispensacionRepo.getServiciosByPaciente(pacienteId)

    suspend fun getServicioById(id: String): Resource<ServicioExtra> =
        dispensacionRepo.getServicioById(id)

    suspend fun insertServicio(servicio: ServicioExtra) {
        dispensacionRepo.insertServicio(servicio)
        triggerFinanzasSync(servicio.opticaId)
    }

    suspend fun updateServicio(servicio: ServicioExtra) {
        dispensacionRepo.updateServicio(servicio)
        triggerFinanzasSync(servicio.opticaId)
    }

    suspend fun deleteServicio(servicio: ServicioExtra) {
        dispensacionRepo.deleteServicio(servicio)
        syncStateTracker.markDeleted(servicio.opticaId, "servicio_extra", servicio.id)
        triggerFinanzasSync(servicio.opticaId)
    }

    // ── Monturas ─────────────────────────────────────────────────────────────

    fun getMonturasByOptica(opticaId: String): Flow<List<Montura>> =
        monturaDao.getMonturasByOptica(opticaId)

    suspend fun getMonturaById(id: String): Resource<Montura> {
        return try {
            val montura = monturaDao.getMonturaById(id)
            if (montura != null) Resource.Success(montura)
            else Resource.Error("Montura no encontrada")
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Error al obtener montura")
        }
    }

    suspend fun insertMontura(montura: Montura) {
        monturaDao.insertMontura(montura)
        triggerInventarioSync(montura.opticaId)
    }

    suspend fun updateMontura(montura: Montura) {
        monturaDao.updateMontura(montura)
        triggerInventarioSync(montura.opticaId)
    }

    suspend fun deleteMontura(montura: Montura) {
        monturaDao.deleteMontura(montura)
        triggerInventarioSync(montura.opticaId)
    }

    suspend fun adjustMonturaStock(monturaId: String, opticaId: String, delta: Int): Int {
        val changed = monturaDao.adjustStock(monturaId, opticaId, delta)
        if (changed > 0) triggerInventarioSync(opticaId)
        return changed
    }

    fun getMovimientosMonturaByOptica(opticaId: String): Flow<List<MonturaMovimiento>> =
        monturaMovimientoDao.getMovimientosByOptica(opticaId)

    fun getMovimientosByMontura(monturaId: String): Flow<List<MonturaMovimiento>> =
        monturaMovimientoDao.getMovimientosByMontura(monturaId)

    suspend fun insertMonturaMovimiento(movimiento: MonturaMovimiento) {
        monturaMovimientoDao.insertMovimiento(movimiento)
        triggerInventarioSync(movimiento.opticaId)
    }

    // ─── Sync Snapshot Methods ───────────────────────────────────────────────

    suspend fun upsertPaciente(paciente: Paciente) = pacienteDao.insertPaciente(paciente)

    suspend fun upsertMontura(montura: Montura) = monturaDao.insertMontura(montura)

    suspend fun upsertMonturaMovimiento(movimiento: MonturaMovimiento) =
        monturaMovimientoDao.insertMovimiento(movimiento)

    suspend fun getPacientesSnapshotForOptica(opticaId: String): List<Paciente> =
        pacienteRepo.getPacientesSnapshotForOptica(opticaId)

    suspend fun getEvaluacionesSnapshotForOptica(opticaId: String): List<EvaluacionClinica> =
        pacienteRepo.getEvaluacionesSnapshotForOptica(opticaId)

    suspend fun getDispensacionesSnapshotForOptica(opticaId: String): List<DispensacionOptica> =
        dispensacionRepo.getDispensacionesSnapshotForOptica(opticaId)

    suspend fun getPagosSnapshotForOptica(opticaId: String): List<Pago> =
        dispensacionRepo.getPagosSnapshotForOptica(opticaId)

    suspend fun getServiciosSnapshotForOptica(opticaId: String): List<ServicioExtra> =
        dispensacionRepo.getServiciosSnapshotForOptica(opticaId)

    suspend fun getMonturasSnapshotForOptica(opticaId: String): List<Montura> =
        syncRepo.getMonturasSnapshotForOptica(opticaId)

    suspend fun getMovimientosMonturaSnapshotForOptica(opticaId: String): List<MonturaMovimiento> =
        syncRepo.getMovimientosMonturaSnapshotForOptica(opticaId)

    // ─── Sync State ──────────────────────────────────────────────────────────

    suspend fun getPendingDeletions(opticaId: String): List<SyncEntityState> =
        syncRepo.getPendingDeletions(opticaId)

    suspend fun clearDeletionState(opticaId: String, type: String, id: String) =
        syncRepo.clearDeletionState(opticaId, type, id)

    // ─────────────────────────────────────────────────────────────────────────

    suspend fun clearAllData() {
        dispensacionRepo.deleteAll()
        evaluacionDao.deleteAll()
        pacienteDao.deleteAll()
    }

    suspend fun reassignLegacyMiOpticaBaseTo(currentOpticaId: String) {
        if (currentOpticaId.isBlank() || currentOpticaId == SessionManager.LEGACY_OPTICA_ID) return
        val pacienteResult = pacienteRepo.reassignFromLegacyMiOpticaBase(currentOpticaId)
        val dispensacionResult = dispensacionRepo.reassignFromLegacyMiOpticaBase(currentOpticaId)
        if (pacienteResult + dispensacionResult > 0) {
            Log.d(TAG, "Reasignadas ${pacienteResult + dispensacionResult} filas de mi_optica_base → opticaId=$currentOpticaId")
        }
    }

    suspend fun getBackupDataForOptica(opticaId: String): BackupData {
        return BackupData(
            sourceOpticaId = opticaId,
            pacientes = pacienteRepo.getPacientesSnapshotForOptica(opticaId),
            evaluaciones = pacienteRepo.getEvaluacionesSnapshotForOptica(opticaId),
            dispensaciones = dispensacionRepo.getDispensacionesSnapshotForOptica(opticaId),
            pagos = dispensacionRepo.getPagosSnapshotForOptica(opticaId),
            serviciosExtra = dispensacionRepo.getServiciosSnapshotForOptica(opticaId)
        )
    }

    private fun Paciente.withDefaults(): Paciente = copy(
        dni = dni ?: "",
        sexo = sexo ?: "",
        email = email ?: "",
        historiaOptometrica = historiaOptometrica ?: "",
        direccion = direccion ?: "",
        distrito = distrito ?: "",
        ocupacion = ocupacion ?: "",
        acompanante = acompanante ?: "",
        hobbies = hobbies ?: "",
        ultimasEtiquetas = if (ultimasEtiquetas == null) emptyList() else ultimasEtiquetas
    )

    private fun EvaluacionClinica.withDefaults(): EvaluacionClinica = copy(
        necesidadVisual = if (necesidadVisual == null) emptyList() else necesidadVisual,
        diagnosticoOd = if (diagnosticoOd == null) emptyList() else diagnosticoOd,
        diagnosticoOi = if (diagnosticoOi == null) emptyList() else diagnosticoOi,
        diagnosticoOtros = if (diagnosticoOtros == null) emptyList() else diagnosticoOtros
    )

    private fun DispensacionOptica.withDefaults(): DispensacionOptica = copy(
        tratamientos = if (tratamientos == null) emptyList() else tratamientos
    )

    private fun Pago.withDefaults(): Pago = this
    private fun ServicioExtra.withDefaults(): ServicioExtra = this

    suspend fun restoreBackup(backupData: BackupData, currentOpticaId: String) {
        clearAllData()

        backupData.pacientes?.forEach {
            try {
                insertPaciente(it.withDefaults().copy(opticaId = currentOpticaId))
            } catch(e: Exception) {
                e.printStackTrace()
            }
        }

        backupData.evaluaciones?.forEach {
            try {
                insertEvaluacion(it.withDefaults().copy(opticaId = currentOpticaId))
            } catch(e: Exception) {
                e.printStackTrace()
            }
        }

        backupData.dispensaciones?.forEach {
            try {
                insertDispensacion(it.withDefaults().copy(opticaId = currentOpticaId))
            } catch(e: Exception) {
                e.printStackTrace()
            }
        }

        backupData.pagos?.forEach {
            try {
                insertPago(it.withDefaults().copy(opticaId = currentOpticaId))
            } catch(e: Exception) {
                e.printStackTrace()
            }
        }

        backupData.serviciosExtra?.forEach {
            try {
                insertServicio(it.withDefaults().copy(opticaId = currentOpticaId))
            } catch(e: Exception) {
                e.printStackTrace()
            }
        }
    }

    suspend fun resolveDuplicatePacientesByHistoria(opticaId: String): DuplicateHoResolutionResult =
        pacienteRepo.resolveDuplicatePacientesByHistoria(opticaId, database)
}

data class DuplicateHoResolutionResult(
    val mergedPacientes: Int = 0,
    val movedEvaluaciones: Int = 0,
    val movedDispensaciones: Int = 0,
    val movedServicios: Int = 0
)

@Serializable
data class BackupData(
    val version: Int = 3,
    val dateExported: Long = System.currentTimeMillis(),
    val appIdentifier: String = "OptoApp-2.0",
    @SerialName("source_optica_id")
    val sourceOpticaId: String? = null,
    val pacientes: List<Paciente>? = emptyList(),
    val evaluaciones: List<EvaluacionClinica>? = emptyList(),
    @SerialName("dispensaciones")
    val dispensaciones: List<DispensacionOptica>? = emptyList(),
    val pagos: List<Pago>? = emptyList(),
    @SerialName("serviciosExtra")
    val serviciosExtra: List<ServicioExtra>? = emptyList()
)

object BackupDataSerializer : KSerializer<BackupData> {
    private val delegate = BackupData.serializer()

    override val descriptor: SerialDescriptor = delegate.descriptor

    override fun serialize(encoder: Encoder, value: BackupData) {
        encoder.encodeSerializableValue(delegate, value)
    }

    override fun deserialize(decoder: Decoder): BackupData {
        val jsonDecoder = decoder as JsonDecoder
        val root = jsonDecoder.decodeJsonElement().jsonObject
        val patched = patchAlternateKeys(root)
        return jsonDecoder.json.decodeFromJsonElement(delegate, patched)
    }

    private fun patchAlternateKeys(root: JsonObject): JsonObject {
        val map = root.toMutableMap()
        patchKey(map, "dispensaciones", "ordenes", "ventas")
        patchKey(map, "serviciosExtra", "servicios", "otrosServicios")
        return JsonObject(map)
    }

    private fun patchKey(map: MutableMap<String, JsonElement>, primary: String, vararg alternates: String) {
        if (map.containsKey(primary)) {
            alternates.forEach { map.remove(it) }
        } else {
            for (alt in alternates) {
                val element = map.remove(alt)
                if (element != null) {
                    map[primary] = element
                    return
                }
            }
        }
    }
}
