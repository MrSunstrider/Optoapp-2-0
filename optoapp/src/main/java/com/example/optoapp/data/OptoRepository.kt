package com.example.optoapp.data

import android.util.Log
import androidx.room.withTransaction
import com.example.optoapp.data.backup.BackupRestoreCoordinator
import com.example.optoapp.data.gastooperativo.GastoOperativoDao
import com.example.optoapp.data.gastooperativo.GastoOperativoEntity
import com.example.optoapp.data.montura.MonturaInventoryCoordinator
import com.example.optoapp.data.regalodispensacion.RegaloDispensacionEntity
import com.example.optoapp.data.sync.SyncSnapshotCoordinator
import dagger.Lazy
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.flow.Flow
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
import java.time.Instant
import java.time.LocalDate

open class OptoRepository(
    private val database: OptoDatabase,
    private val syncStateTracker: SyncStateTracker,
    private val postSaveSyncScheduler: Lazy<com.example.optoapp.sync.PostSaveSyncScheduler>,
    val pacienteRepo: PacienteRepository,
    val dispensacionRepo: DispensacionRepository,
    val syncRepo: SyncRepository,
    val snapshotCoordinator: SyncSnapshotCoordinator,
    val backupCoordinator: BackupRestoreCoordinator,
    val monturaCoordinator: MonturaInventoryCoordinator,
    private val gastoOperativoDao: GastoOperativoDao,
    private val supabase: SupabaseClient,
) {
    companion object {
        private const val TAG = "OptoRepository"
    }

    fun runInTransaction(block: () -> Unit) {
        database.runInTransaction(block)
    }

    // Room's withTransaction is not a suspend function — this adapter bridges the gap
    // so sync state writes can be atomic with entity writes inside coroutines.
    suspend fun <T> withTransaction(block: suspend () -> T): T = database.withTransaction(block)

    fun pacientesFlowForOptica(opticaId: String) = pacienteRepo.pacientesFlowForOptica(opticaId)
    fun countPacientesForOptica(opticaId: String) = pacienteRepo.countPacientesForOptica(opticaId)
    fun searchPacientesForOptica(opticaId: String, query: String) = pacienteRepo.searchPacientesForOptica(opticaId, query)
    fun getPacientesWithPendingBalanceForOptica(opticaId: String) = pacienteRepo.getPacientesWithPendingBalanceForOptica(opticaId)
    fun getPacientesWithPendingDeliveryForOptica(opticaId: String) = pacienteRepo.getPacientesWithPendingDeliveryForOptica(opticaId)
    @Deprecated("Use getPacienteByIdScoped to enforce multi-tenant isolation")
    suspend fun getPacienteById(id: String) = pacienteRepo.getPacienteById(id, "")
    suspend fun getPacienteByIdScoped(id: String, opticaId: String) = pacienteRepo.getPacienteById(id, opticaId)
    suspend fun insertPaciente(paciente: Paciente) {
        val stamped = paciente.copy(updatedAt = Instant.now().toString())
        pacienteRepo.insertPaciente(stamped)
        postSaveSyncScheduler.get().schedulePacientesSync(stamped.opticaId)
    }
    suspend fun updatePaciente(paciente: Paciente) {
        val stamped = paciente.copy(updatedAt = Instant.now().toString())
        pacienteRepo.upsertPaciente(stamped)
        postSaveSyncScheduler.get().schedulePacientesSync(stamped.opticaId)
    }
    suspend fun deletePaciente(paciente: Paciente) {
        database.withTransaction {
            pacienteRepo.deletePaciente(paciente)
            syncStateTracker.markDeleted(paciente.opticaId, "paciente", paciente.id)
        }
        postSaveSyncScheduler.get().schedulePacientesSync(paciente.opticaId)
    }

    /** Attempts the remote delete for a patient. Throws on network failure so callers can handle retry. */
    suspend fun deletePacienteRemoto(id: String, opticaId: String) {
        supabase.postgrest["pacientes"].delete {
            filter {
                eq("id", id)
                eq("optica_id", opticaId)
            }
        }
    }

    fun getEvaluacionesByPaciente(pacienteId: String, opticaId: String) = pacienteRepo.getEvaluacionesByPaciente(pacienteId, opticaId)
    fun getEvaluacionesProximaCitaEnRango(opticaId: String, start: LocalDate, end: LocalDate) = pacienteRepo.getEvaluacionesProximaCitaEnRango(opticaId, start, end)

    @Suppress("DEPRECATION")
    @Deprecated(
        message = "Use countEvaluacionesInRangeForOptica to enforce multi-tenant isolation",
        replaceWith = ReplaceWith("countEvaluacionesInRangeForOptica(start, end, opticaId)"),
    )
    fun countEvaluacionesInRange(start: LocalDate, end: LocalDate) = pacienteRepo.countEvaluacionesInRange(start, end)
    fun countEvaluacionesInRangeForOptica(start: LocalDate, end: LocalDate, opticaId: String) = pacienteRepo.countEvaluacionesInRangeForOptica(start, end, opticaId)
    suspend fun getEvaluacionById(id: String, opticaId: String) = pacienteRepo.getEvaluacionById(id, opticaId)
    suspend fun getLastEvaluacionByPacienteId(pacienteId: String, opticaId: String) = pacienteRepo.getLastEvaluacionByPacienteId(pacienteId, opticaId)
    suspend fun deleteEvaluacion(evaluacion: EvaluacionClinica) = pacienteRepo.deleteEvaluacion(evaluacion)
    suspend fun insertEvaluacion(evaluacion: EvaluacionClinica) {
        val stamped = evaluacion.copy(updatedAt = Instant.now().toString())
        pacienteRepo.insertEvaluacion(stamped)
        postSaveSyncScheduler.get().scheduleHistorialSync(stamped.opticaId)
    }
    suspend fun updateEvaluacion(evaluacion: EvaluacionClinica) {
        val stamped = evaluacion.copy(updatedAt = Instant.now().toString())
        pacienteRepo.updateEvaluacion(stamped)
        postSaveSyncScheduler.get().scheduleHistorialSync(stamped.opticaId)
    }

    fun getDispensacionesByPaciente(pacienteId: String, opticaId: String) = dispensacionRepo.getDispensacionesByPaciente(pacienteId, opticaId)
    fun getAllDispensacionesForOptica(opticaId: String) = dispensacionRepo.getAllDispensacionesForOptica(opticaId)
    fun getTotalVendidoForOptica(opticaId: String) = dispensacionRepo.getTotalVendidoForOptica(opticaId)
    fun getTotalPagadoForOptica(opticaId: String) = dispensacionRepo.getTotalPagadoForOptica(opticaId)
    fun getDispensacionesByDateRangeForOptica(start: LocalDate, end: LocalDate, opticaId: String) = dispensacionRepo.getDispensacionesByDateRangeForOptica(start, end, opticaId)
    suspend fun getDispensacionById(id: String, opticaId: String) = dispensacionRepo.getDispensacionById(id, opticaId)
    suspend fun getLastDispensacionByPacienteId(pacienteId: String, opticaId: String) = dispensacionRepo.getLastDispensacionByPacienteId(pacienteId, opticaId)
    suspend fun insertDispensacion(dispensacion: DispensacionOptica) {
        val stamped = dispensacion.copy(updatedAt = Instant.now().toString())
        dispensacionRepo.insertDispensacion(stamped)
        postSaveSyncScheduler.get().scheduleFinanzasSync(stamped.opticaId)
    }
    suspend fun updateDispensacion(dispensacion: DispensacionOptica) {
        val stamped = dispensacion.copy(updatedAt = Instant.now().toString())
        dispensacionRepo.updateDispensacion(stamped)
        postSaveSyncScheduler.get().scheduleFinanzasSync(stamped.opticaId)
    }
    suspend fun deleteDispensacionById(id: String, opticaId: String) = dispensacionRepo.deleteDispensacionById(id, opticaId)
    suspend fun deleteDispensacion(dispensacion: DispensacionOptica) {
        database.withTransaction {
            dispensacionRepo.deleteDispensacionById(dispensacion.id, dispensacion.opticaId)
            syncStateTracker.markDeleted(dispensacion.opticaId, "dispensacion", dispensacion.id)
        }
        postSaveSyncScheduler.get().scheduleFinanzasSync(dispensacion.opticaId)
    }
    suspend fun insertDispensacionItem(item: DispensacionItem) {
        dispensacionRepo.insertDispensacionItem(item)
        postSaveSyncScheduler.get().scheduleFinanzasSync(item.opticaId)
    }
    suspend fun deleteDispensacionItemById(id: String, opticaId: String) {
        dispensacionRepo.deleteDispensacionItemById(id, opticaId)
        syncStateTracker.markDeleted(opticaId, "dispensacion_item", id)
    }
    suspend fun deleteItemsByDispensacionId(dispensacionId: String, opticaId: String) = dispensacionRepo.deleteItemsByDispensacionId(dispensacionId, opticaId)
    suspend fun getDispensacionItemById(id: String, opticaId: String) = dispensacionRepo.getDispensacionItemById(id, opticaId)
    suspend fun getDispensacionItemsByDispensacion(dispensacionId: String) = dispensacionRepo.getItemsListByDispensacion(dispensacionId)
    suspend fun suggestNextOt(opticaId: String, fecha: LocalDate) = dispensacionRepo.suggestNextOt(opticaId, fecha)
    suspend fun suggestNextHistoriaOptometrica(opticaId: String) = pacienteRepo.suggestNextHistoriaOptometrica(opticaId)
    suspend fun existsDuplicateHistoriaOptometrica(opticaId: String, historia: String, excludePacienteId: String?) = pacienteRepo.existsDuplicateHistoriaOptometrica(opticaId, historia, excludePacienteId)

    fun getPagosByDispensacion(dispensacionId: String, opticaId: String) =
        dispensacionRepo.getPagosByDispensacion(dispensacionId, opticaId)
    suspend fun insertPago(pago: Pago) {
        val stamped = pago.copy(updatedAt = Instant.now().toString())
        dispensacionRepo.insertPago(stamped)
        postSaveSyncScheduler.get().scheduleFinanzasSync(stamped.opticaId)
    }
    suspend fun updatePago(pago: Pago) {
        val stamped = pago.copy(updatedAt = Instant.now().toString())
        dispensacionRepo.updatePago(stamped)
        postSaveSyncScheduler.get().scheduleFinanzasSync(stamped.opticaId)
    }
    suspend fun getPagoById(id: String, opticaId: String) = dispensacionRepo.getPagoById(id, opticaId)
    suspend fun reassignPagosDispensacion(oldDispensacionId: String, newDispensacionId: String, opticaId: String) = dispensacionRepo.reassignPagosDispensacion(oldDispensacionId, newDispensacionId, opticaId)
    suspend fun reassignItemsDispensacion(sourceId: String, targetId: String, opticaId: String) = dispensacionRepo.reassignItemsDispensacion(sourceId, targetId, opticaId)
    suspend fun reassignRegalosDispensacion(sourceId: String, targetId: String, opticaId: String) = database.regaloDispensacionDao().reassignRegalosDispensacion(sourceId, targetId, opticaId)
    suspend fun deletePagoRegistrandoAnulacionEnCaja(pago: Pago, opticaId: String) = dispensacionRepo.deletePagoRegistrandoAnulacionEnCaja(pago, opticaId)
    suspend fun deletePago(pago: Pago) = dispensacionRepo.deletePago(pago)
    fun getPagosByServicioExtra(servicioExtraId: String, opticaId: String) =
        dispensacionRepo.getPagosByServicioExtra(servicioExtraId, opticaId)

    fun getPagosByDateRangeForOptica(start: LocalDate, end: LocalDate, opticaId: String) =
        dispensacionRepo.getPagosByDateRangeForOptica(start, end, opticaId)
    fun getAllPagosFlowForOptica(opticaId: String) = dispensacionRepo.getPagosFlowForOptica(opticaId)

    fun getAllServiciosForOptica(opticaId: String) = dispensacionRepo.getAllServiciosForOptica(opticaId)
    fun getServiciosByDateRangeForOptica(start: LocalDate, end: LocalDate, opticaId: String) = dispensacionRepo.getServiciosByDateRangeForOptica(start, end, opticaId)
    suspend fun getServiciosByIds(ids: List<String>, opticaId: String) = dispensacionRepo.getServiciosByIds(ids, opticaId)
    suspend fun getDispensacionesByIds(ids: List<String>, opticaId: String) = dispensacionRepo.getDispensacionesByIds(ids, opticaId)
    suspend fun getServicioById(id: String, opticaId: String) = dispensacionRepo.getServicioById(id, opticaId)
    suspend fun insertServicio(servicio: ServicioExtra) {
        val stamped = servicio.copy(updatedAt = Instant.now().toString())
        dispensacionRepo.insertServicio(stamped)
        postSaveSyncScheduler.get().scheduleFinanzasSync(stamped.opticaId)
    }
    suspend fun updateServicio(servicio: ServicioExtra) {
        val stamped = servicio.copy(updatedAt = Instant.now().toString())
        dispensacionRepo.updateServicio(stamped)
        postSaveSyncScheduler.get().scheduleFinanzasSync(stamped.opticaId)
    }
    suspend fun deleteServicio(servicio: ServicioExtra) {
        database.withTransaction {
            dispensacionRepo.deleteServicio(servicio)
            syncStateTracker.markDeleted(servicio.opticaId, "servicio_extra", servicio.id)
        }
        postSaveSyncScheduler.get().scheduleFinanzasSync(servicio.opticaId)
    }

    fun getMonturasByOptica(opticaId: String) = monturaCoordinator.getMonturasByOptica(opticaId)
    suspend fun getMonturaById(id: String, opticaId: String) = monturaCoordinator.getMonturaById(id, opticaId)
    suspend fun insertMontura(montura: Montura) = monturaCoordinator.insertMontura(montura)
    suspend fun updateMontura(montura: Montura) = monturaCoordinator.updateMontura(montura)
    suspend fun deleteMontura(montura: Montura) = monturaCoordinator.deleteMontura(montura)
    suspend fun adjustMonturaStock(monturaId: String, opticaId: String, delta: Int) = monturaCoordinator.adjustMonturaStock(monturaId, opticaId, delta)
    fun getMovimientosMonturaByOptica(opticaId: String) = monturaCoordinator.getMovimientosMonturaByOptica(opticaId)
    fun getMovimientosByMontura(monturaId: String) = monturaCoordinator.getMovimientosByMontura(monturaId)
    suspend fun getMovimientoMonturaById(id: String) = monturaCoordinator.getMovimientoMonturaById(id)
    suspend fun insertMonturaMovimiento(movimiento: MonturaMovimiento) = monturaCoordinator.insertMonturaMovimiento(movimiento)

    suspend fun upsertPaciente(paciente: Paciente) = snapshotCoordinator.upsertPaciente(paciente)
    suspend fun upsertMontura(montura: Montura) = snapshotCoordinator.upsertMontura(montura)
    suspend fun upsertMonturaMovimiento(movimiento: MonturaMovimiento) = snapshotCoordinator.upsertMonturaMovimiento(movimiento)
    suspend fun getPacientesSnapshotForOptica(opticaId: String) = snapshotCoordinator.getPacientesSnapshotForOptica(opticaId)
    suspend fun getEvaluacionesSnapshotForOptica(opticaId: String) = snapshotCoordinator.getEvaluacionesSnapshotForOptica(opticaId)
    suspend fun getDispensacionesSnapshotForOptica(opticaId: String) = snapshotCoordinator.getDispensacionesSnapshotForOptica(opticaId)
    suspend fun getDispensacionItemsSnapshotForOptica(opticaId: String) = snapshotCoordinator.getDispensacionItemsSnapshotForOptica(opticaId)
    suspend fun getPagosSnapshotForOptica(opticaId: String) = snapshotCoordinator.getPagosSnapshotForOptica(opticaId)
    suspend fun getServiciosSnapshotForOptica(opticaId: String) = snapshotCoordinator.getServiciosSnapshotForOptica(opticaId)
    suspend fun getMonturasSnapshotForOptica(opticaId: String) = snapshotCoordinator.getMonturasSnapshotForOptica(opticaId)
    suspend fun getMovimientosMonturaSnapshotForOptica(opticaId: String) = snapshotCoordinator.getMovimientosMonturaSnapshotForOptica(opticaId)
    suspend fun getRegalosSnapshotForOptica(opticaId: String) = snapshotCoordinator.getRegalosSnapshotForOptica(opticaId)

    suspend fun getPendingDeletions(opticaId: String) = syncRepo.getPendingDeletions(opticaId)
    suspend fun clearDeletionState(opticaId: String, type: String, id: String) = syncRepo.clearDeletionState(opticaId, type, id)

    suspend fun reassignLegacyMiOpticaBaseTo(currentOpticaId: String) {
        if (currentOpticaId.isBlank() || currentOpticaId == SessionManager.LEGACY_OPTICA_ID) return
        val pacienteResult = pacienteRepo.reassignFromLegacyMiOpticaBase(currentOpticaId)
        val dispensacionResult = dispensacionRepo.reassignFromLegacyMiOpticaBase(currentOpticaId)
        if (pacienteResult + dispensacionResult > 0) {
            Log.d(TAG, "Reasignadas ${pacienteResult + dispensacionResult} filas de mi_optica_base → opticaId=$currentOpticaId")
        }
    }

    // WHY: one Room file is shared by every account on the device. Logout/login
    // must drop leftover rows so the next user does not upsert another óptica's PKs.
    open suspend fun wipeLocalAccountData() {
        runCatching { postSaveSyncScheduler.get().cancelPending() }
        database.clearAllTables()
    }

    suspend fun getBackupDataForOptica(opticaId: String) = backupCoordinator.getBackupDataForOptica(opticaId)
    suspend fun restoreBackup(backupData: BackupData, currentOpticaId: String) = backupCoordinator.restoreBackup(backupData, currentOpticaId)

    suspend fun resolveDuplicatePacientesByHistoria(opticaId: String) = pacienteRepo.resolveDuplicatePacientesByHistoria(opticaId, database)

    // These methods write a record received from the server as-is:
    //  - NO copy(updatedAt = Instant.now()) — server timestamp is preserved verbatim.
    //  - NO postSaveSyncScheduler call — download is the terminal step of a sync cycle.
    suspend fun upsertServicioFromRemote(servicio: ServicioExtra) = dispensacionRepo.insertServicio(servicio)

    suspend fun upsertDispensacionFromRemote(dispensacion: DispensacionOptica) = dispensacionRepo.insertDispensacion(dispensacion)

    suspend fun upsertPagoFromRemote(pago: Pago) = dispensacionRepo.insertPago(pago)

    suspend fun upsertEvaluacionFromRemote(evaluacion: EvaluacionClinica) = pacienteRepo.insertEvaluacion(evaluacion)

    suspend fun upsertDispensacionItemFromRemote(item: DispensacionItem) = dispensacionRepo.insertDispensacionItem(item)

    fun getGastosOperativos(opticaId: String): Flow<List<GastoOperativoEntity>> = gastoOperativoDao.getByOpticaId(opticaId)

    suspend fun getGastosOperativosList(opticaId: String): List<GastoOperativoEntity> = gastoOperativoDao.getByOpticaIdList(opticaId)

    suspend fun insertGastoOperativo(gasto: GastoOperativoEntity) {
        val stamped = gasto.copy(createdAt = gasto.createdAt ?: Instant.now().toString())
        gastoOperativoDao.insert(stamped)
        postSaveSyncScheduler.get().scheduleFinanzasSync(stamped.opticaId)
    }

    suspend fun upsertGastoOperativo(gasto: GastoOperativoEntity) {
        val stamped = gasto.copy(createdAt = gasto.createdAt ?: Instant.now().toString())
        gastoOperativoDao.upsert(stamped)
        postSaveSyncScheduler.get().scheduleFinanzasSync(stamped.opticaId)
    }

    suspend fun deleteGastoOperativo(gasto: GastoOperativoEntity) {
        database.withTransaction {
            gastoOperativoDao.delete(gasto.id, gasto.opticaId)
            syncStateTracker.markDeleted(gasto.opticaId, "gasto_operativo", gasto.id)
        }
        postSaveSyncScheduler.get().scheduleFinanzasSync(gasto.opticaId)
    }

    suspend fun upsertGastoOperativoFromRemote(gasto: GastoOperativoEntity) = gastoOperativoDao.upsert(gasto)

    suspend fun upsertRegaloFromRemote(regalo: RegaloDispensacionEntity) = database.regaloDispensacionDao().upsert(regalo)

    suspend fun getRegalosByDispensacionId(dispId: String, opticaId: String) =
        database.regaloDispensacionDao().getByDispensacionId(dispId, opticaId)

    suspend fun insertRegalo(regalo: RegaloDispensacionEntity) = database.regaloDispensacionDao().insert(regalo)

    suspend fun deleteRegaloById(id: String, opticaId: String) = database.regaloDispensacionDao().deleteById(id, opticaId)

    suspend fun deleteRegalosByDispensacionId(dispId: String, opticaId: String) = database.regaloDispensacionDao().deleteByDispensacionId(dispId, opticaId)
}

data class DuplicateHoResolutionResult(
    val mergedPacientes: Int = 0,
    val movedEvaluaciones: Int = 0,
    val movedDispensaciones: Int = 0,
    val movedServicios: Int = 0,
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
    val serviciosExtra: List<ServicioExtra>? = emptyList(),
)

object BackupDataSerializer : KSerializer<BackupData> {
    private val delegate = BackupData.serializer()
    override val descriptor: SerialDescriptor = delegate.descriptor
    override fun serialize(encoder: Encoder, value: BackupData) = encoder.encodeSerializableValue(delegate, value)
    override fun deserialize(decoder: Decoder): BackupData {
        val jsonDecoder = decoder as JsonDecoder
        val root = jsonDecoder.decodeJsonElement().jsonObject
        return jsonDecoder.json.decodeFromJsonElement(delegate, patchAlternateKeys(root))
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
