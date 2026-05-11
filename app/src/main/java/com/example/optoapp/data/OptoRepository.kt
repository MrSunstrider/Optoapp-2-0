package com.example.optoapp.data

import android.util.Log
import androidx.room.withTransaction
import dagger.Lazy
import kotlinx.coroutines.flow.Flow

import com.google.gson.annotations.SerializedName
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
    private val postSaveSyncScheduler: Lazy<com.example.optoapp.sync.PostSaveSyncScheduler>
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
    fun pacientesFlowForOptica(opticaId: String): Flow<List<Paciente>> =
        pacienteDao.getPacientesByOptica(opticaId)

    fun countPacientesForOptica(opticaId: String): Flow<Int> =
        pacienteDao.countByOptica(opticaId)

    fun searchPacientesForOptica(opticaId: String, query: String): Flow<List<Paciente>> =
        if (query.isEmpty()) pacienteDao.getPacientesByOptica(opticaId)
        else pacienteDao.searchPacientesForOptica(opticaId, query)

    fun getPacientesWithPendingBalanceForOptica(opticaId: String): Flow<List<Paciente>> =
        pacienteDao.getPacientesWithPendingBalanceForOptica(opticaId)

    fun getPacientesWithPendingDeliveryForOptica(opticaId: String): Flow<List<Paciente>> =
        pacienteDao.getPacientesWithPendingDeliveryForOptica(opticaId)
    
    suspend fun getPacienteById(id: String): Resource<Paciente> {
        return try {
            val paciente = pacienteDao.getPacienteById(id)
            if (paciente != null) Resource.Success(paciente)
            else Resource.Error("Paciente no encontrado")
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Error al obtener paciente")
        }
    }
    
    suspend fun insertPaciente(paciente: Paciente) {
        pacienteDao.insertPaciente(paciente)
        triggerPacientesSync(paciente.opticaId)
    }
    
    suspend fun updatePaciente(paciente: Paciente) {
        pacienteDao.updatePaciente(paciente)
        triggerPacientesSync(paciente.opticaId)
    }
    
    suspend fun deletePaciente(paciente: Paciente) = pacienteDao.deletePaciente(paciente)
    
    fun getEvaluacionesByPaciente(pacienteId: String): Flow<List<EvaluacionClinica>> = 
        evaluacionDao.getEvaluacionesByPaciente(pacienteId)

    /** Evaluaciones con próxima cita en [start, end] (por fecha de cita), óptica actual. */
    fun getEvaluacionesProximaCitaEnRango(opticaId: String, start: LocalDate, end: LocalDate): Flow<List<EvaluacionClinica>> =
        evaluacionDao.getEvaluacionesConProximaCitaEnRango(opticaId, start, end)
        
    fun countEvaluacionesInRange(start: LocalDate, end: LocalDate): Flow<Int> = evaluacionDao.countEvaluacionesInRange(start, end)

    fun countEvaluacionesInRangeForOptica(start: LocalDate, end: LocalDate, opticaId: String): Flow<Int> =
        evaluacionDao.countEvaluacionesInRangeForOptica(start, end, opticaId)

    fun getDispensacionesByDateRange(start: LocalDate, end: LocalDate): Flow<List<DispensacionOptica>> =
        dispensacionDao.getDispensacionesByDateRange(start, end)

    fun getDispensacionesByDateRangeForOptica(start: LocalDate, end: LocalDate, opticaId: String): Flow<List<DispensacionOptica>> =
        dispensacionDao.getDispensacionesByDateRangeForOptica(start, end, opticaId)
        
    suspend fun getEvaluacionById(id: String): Resource<EvaluacionClinica> {
        return try {
            val evaluacion = evaluacionDao.getEvaluacionById(id)
            if (evaluacion != null) Resource.Success(evaluacion)
            else Resource.Error("Evaluación no encontrada")
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Error al obtener evaluación")
        }
    }
        
    suspend fun deleteEvaluacion(evaluacion: EvaluacionClinica) = evaluacionDao.deleteEvaluacion(evaluacion)
    
    suspend fun insertEvaluacion(evaluacion: EvaluacionClinica) {
        evaluacionDao.insertEvaluacion(evaluacion)
        triggerHistorialSync(evaluacion.opticaId)
    }
    
    suspend fun updateEvaluacion(evaluacion: EvaluacionClinica) {
        evaluacionDao.updateEvaluacion(evaluacion)
        triggerHistorialSync(evaluacion.opticaId)
    }
    
    fun getDispensacionesByPaciente(pacienteId: String): Flow<List<DispensacionOptica>> = 
        dispensacionDao.getDispensacionesByPaciente(pacienteId)
        
    fun getAllDispensaciones(): Flow<List<DispensacionOptica>> = dispensacionDao.getAllDispensaciones()

    fun getAllDispensacionesForOptica(opticaId: String): Flow<List<DispensacionOptica>> =
        dispensacionDao.getAllDispensacionesForOptica(opticaId)

    fun getTotalVendido(): Flow<Double?> = dispensacionDao.getTotalVendido()

    fun getTotalPagado(): Flow<Double?> = dispensacionDao.getTotalPagado()

    fun getTotalVendidoForOptica(opticaId: String): Flow<Double?> =
        dispensacionDao.getTotalVendidoForOptica(opticaId)

    fun getTotalPagadoForOptica(opticaId: String): Flow<Double?> =
        dispensacionDao.getTotalPagadoForOptica(opticaId)
    
    suspend fun getDispensacionById(id: String): Resource<DispensacionOptica> {
        return try {
            val dispensacion = dispensacionDao.getDispensacionById(id)
            if (dispensacion != null) Resource.Success(dispensacion)
            else Resource.Error("Dispensación no encontrada")
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Error al obtener dispensación")
        }
    }
        
    suspend fun insertDispensacion(dispensacion: DispensacionOptica) {
        dispensacionDao.insertDispensacion(dispensacion)
        triggerFinanzasSync(dispensacion.opticaId)
    }
    
    suspend fun updateDispensacion(dispensacion: DispensacionOptica) {
        dispensacionDao.updateDispensacion(dispensacion)
        triggerFinanzasSync(dispensacion.opticaId)
    }
    
    suspend fun deleteDispensacionById(id: String): Int = dispensacionDao.deleteById(id)

    suspend fun deleteDispensacion(dispensacion: DispensacionOptica) {
        dispensacionDao.deleteById(dispensacion.id)
        // Registrar tombstone para que el siguiente sync elimine de Supabase
        syncStateTracker.markDeleted(dispensacion.opticaId, "dispensacion", dispensacion.id)
        triggerFinanzasSync(dispensacion.opticaId)
    }

    /** True si otra dispensación de la misma óptica ya usa esta OT (misma cadena ignorando mayúsculas/espacios). */
    suspend fun existsDuplicateOt(opticaId: String, ot: String, excludeDispensacionId: String?): Boolean {
        val n = ot.trim()
        if (n.isEmpty()) return false
        val ex = excludeDispensacionId.orEmpty()
        return dispensacionDao.countDispensacionesWithSameOt(opticaId, n, ex) > 0
    }

    /** Siguiente correlativo `OT-<año>-####` según OT existentes de la óptica para ese año. */
    suspend fun suggestNextOt(opticaId: String, fecha: LocalDate): String {
        val year = fecha.year.toString()
        val ots = dispensacionDao.getOtsWithYearPrefix(opticaId, year)
        val regex = Regex("^OT-$year-(\\d+)$", RegexOption.IGNORE_CASE)
        var max = 0
        for (ot in ots) {
            regex.find(ot.trim())?.groupValues?.get(1)?.toIntOrNull()?.let { if (it > max) max = it }
        }
        val next = max + 1
        return "OT-$year-" + next.toString().padStart(4, '0')
    }

    /** Siguiente correlativo `HO-<año>-####` para historia optométrica en la óptica activa. */
    suspend fun suggestNextHistoriaOptometrica(opticaId: String): String {
        val historias = pacienteDao.getHistoriasOptometricasByOptica(opticaId)
        val year = LocalDate.now().year.toString()
        val regex = Regex("^HO-$year-(\\d+)$", RegexOption.IGNORE_CASE)
        var max = 0
        for (historia in historias) {
            regex.find(historia.trim())?.groupValues?.get(1)?.toIntOrNull()?.let { if (it > max) max = it }
        }
        val next = max + 1
        return "HO-$year-" + next.toString().padStart(4, '0')
    }

    /** True si ya existe esa historia optométrica en la misma óptica (ignorando mayúsculas/espacios). */
    suspend fun existsDuplicateHistoriaOptometrica(opticaId: String, historia: String, excludePacienteId: String?): Boolean {
        val n = historia.trim()
        if (n.isEmpty()) return false
        val ex = excludePacienteId.orEmpty()
        return pacienteDao.countPacientesByHistoriaOptometrica(opticaId, n, ex) > 0
    }
    
    fun getPagosByDispensacion(dispensacionId: String): Flow<List<Pago>> = pagoDao.getPagosByDispensacion(dispensacionId)
    
    suspend fun insertPago(pago: Pago) {
        pagoDao.insertPago(pago)
        triggerFinanzasSync(pago.opticaId)
    }

    suspend fun getPagoById(id: String): Pago? = pagoDao.getPagoById(id)
    suspend fun reassignPagosDispensacion(oldDispensacionId: String, newDispensacionId: String): Int =
        pagoDao.reassignDispensacionId(oldDispensacionId, newDispensacionId)

    /**
     * Borra un abono. Si ya existía en BD, registra un movimiento de anulación el día [fechaAnulacion]
     * (importe negativo) para que cierre de caja refleje la salida o reversión en la fecha correcta.
     */
    suspend fun deletePagoRegistrandoAnulacionEnCaja(
        pago: Pago,
        opticaId: String,
        fechaAnulacion: LocalDate = DateUtils.today()
    ) {
        val existing = pagoDao.getPagoById(pago.id)
        if (existing != null && existing.monto != 0.0) {
            val reversal = Pago(
                id = UUID.randomUUID().toString(),
                dispensacionId = existing.dispensacionId,
                servicioExtraId = existing.servicioExtraId,
                fecha = fechaAnulacion,
                tipo = "Anulación",
                monto = -existing.monto,
                metodoPago = existing.metodoPago,
                nota = "Anula abono ${existing.id.take(8)}… (${DateUtils.formatLocalized(existing.fecha)})",
                opticaId = opticaId
            )
            pagoDao.insertPago(reversal)
        }
        pagoDao.deletePago(pago)
    }

    suspend fun deletePago(pago: Pago) = pagoDao.deletePago(pago)
    
    fun getPagosByServicioExtra(servicioExtraId: String): Flow<List<Pago>> = 
        pagoDao.getPagosByServicioExtra(servicioExtraId)
        
    fun getPagosByDateRange(start: LocalDate, end: LocalDate): Flow<List<Pago>> =
        pagoDao.getPagosByDateRange(start, end)

    fun getPagosByDateRangeForOptica(start: LocalDate, end: LocalDate, opticaId: String): Flow<List<Pago>> =
        pagoDao.getPagosByDateRangeForOptica(start, end, opticaId)

    // Servicios Extra
    fun getAllServicios(): Flow<List<ServicioExtra>> = servicioExtraDao.getAllServicios()

    fun getAllServiciosForOptica(opticaId: String): Flow<List<ServicioExtra>> =
        servicioExtraDao.getAllServiciosForOptica(opticaId)
    
    fun getServiciosByPaciente(pacienteId: String): Flow<List<ServicioExtra>> = servicioExtraDao.getServiciosByPaciente(pacienteId)
    
    suspend fun getServicioById(id: String): Resource<ServicioExtra> {
        return try {
            val servicio = servicioExtraDao.getServicioById(id)
            if (servicio != null) Resource.Success(servicio)
            else Resource.Error("Servicio no encontrado")
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Error al obtener servicio")
        }
    }
    
    suspend fun insertServicio(servicio: ServicioExtra) {
        servicioExtraDao.insertServicio(servicio)
        triggerFinanzasSync(servicio.opticaId)
    }
    
    suspend fun updateServicio(servicio: ServicioExtra) {
        servicioExtraDao.updateServicio(servicio)
        triggerFinanzasSync(servicio.opticaId)
    }
    
    suspend fun deleteServicio(servicio: ServicioExtra) {
        servicioExtraDao.deleteServicio(servicio)
        // Registrar tombstone para que el siguiente sync elimine de Supabase
        syncStateTracker.markDeleted(servicio.opticaId, "servicio_extra", servicio.id)
        triggerFinanzasSync(servicio.opticaId)
    }

    // Monturas
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

    // ─── Métodos de sincronización (Fase 3) ──────────────────────────────────
    // Devuelven una lista completa en un instante dado (snapshot), usados por los
    // Use Cases de sincronización que necesitan leer todos los registros de una vez.

    /** Alias de upsert para la bajada de datos desde Supabase. */
    suspend fun upsertPaciente(paciente: Paciente) = pacienteDao.insertPaciente(paciente)

    /** Alias de upsert para la bajada de inventario desde Supabase (no dispara sync post-guardado). */
    suspend fun upsertMontura(montura: Montura) = monturaDao.insertMontura(montura)

    /** Alias de upsert para la bajada de movimientos desde Supabase (no dispara sync post-guardado). */
    suspend fun upsertMonturaMovimiento(movimiento: MonturaMovimiento) =
        monturaMovimientoDao.insertMovimiento(movimiento)

    /** Snapshot de pacientes de la óptica activa (sync upload). */
    suspend fun getPacientesSnapshotForOptica(opticaId: String): List<Paciente> =
        pacienteDao.getPacientesListByOptica(opticaId)

    /** Snapshot de evaluaciones de la óptica activa. */
    suspend fun getEvaluacionesSnapshotForOptica(opticaId: String): List<EvaluacionClinica> =
        evaluacionDao.getEvaluacionesListByOptica(opticaId)

    /** Snapshot de dispensaciones de la óptica activa. */
    suspend fun getDispensacionesSnapshotForOptica(opticaId: String): List<DispensacionOptica> =
        dispensacionDao.getDispensacionesListByOptica(opticaId)

    /** Snapshot de pagos de la óptica activa. */
    suspend fun getPagosSnapshotForOptica(opticaId: String): List<Pago> =
        pagoDao.getPagosListByOptica(opticaId)

    /** Snapshot de servicios extra de la óptica activa. */
    suspend fun getServiciosSnapshotForOptica(opticaId: String): List<ServicioExtra> =
        servicioExtraDao.getServiciosListByOptica(opticaId)

    /** Snapshot de monturas de la óptica activa (sync inventario). */
    suspend fun getMonturasSnapshotForOptica(opticaId: String): List<Montura> =
        monturaDao.getMonturasListByOptica(opticaId)

    /** Snapshot de movimientos de montura de la óptica activa (sync inventario). */
    suspend fun getMovimientosMonturaSnapshotForOptica(opticaId: String): List<MonturaMovimiento> =
        monturaMovimientoDao.getMovimientosListByOptica(opticaId)

    /** Retorna todos los registros marcados como eliminados pendientes de sync remoto. */
    suspend fun getPendingDeletions(opticaId: String): List<SyncEntityState> =
        syncStateTracker.dao.getPendingDeletions(opticaId)

    /** Limpia el tombstone después de que la eliminación fue confirmada en Supabase. */
    suspend fun clearDeletionState(opticaId: String, type: String, id: String) =
        syncStateTracker.dao.clearEntityState(opticaId, type, id)

    // ─────────────────────────────────────────────────────────────────────────

    suspend fun clearAllData() {
        servicioExtraDao.deleteAll()
        pagoDao.deleteAll()
        dispensacionDao.deleteAll()
        evaluacionDao.deleteAll()
        pacienteDao.deleteAll()
    }

    /**
     * Reasigna filas creadas con el tenant legacy `mi_optica_base` al [currentOpticaId] de sesión SaaS,
     * para que aparezcan en listas filtradas y entren en el snapshot de sync.
     */
    suspend fun reassignLegacyMiOpticaBaseTo(currentOpticaId: String) {
        if (currentOpticaId.isBlank() || currentOpticaId == SessionManager.LEGACY_OPTICA_ID) return
        val p = pacienteDao.reassignFromLegacyMiOpticaBase(currentOpticaId)
        val e = evaluacionDao.reassignFromLegacyMiOpticaBase(currentOpticaId)
        val d = dispensacionDao.reassignFromLegacyMiOpticaBase(currentOpticaId)
        val s = servicioExtraDao.reassignFromLegacyMiOpticaBase(currentOpticaId)
        val pg = pagoDao.reassignFromLegacyMiOpticaBase(currentOpticaId)
        val n = p + e + d + s + pg
        if (n > 0) Log.d(TAG, "Reasignadas $n filas de mi_optica_base → opticaId=$currentOpticaId (p=$p e=$e d=$d s=$s pg=$pg)")
    }
    
    suspend fun getBackupDataForOptica(opticaId: String): BackupData {
        return BackupData(
            sourceOpticaId = opticaId,
            pacientes = pacienteDao.getPacientesListByOptica(opticaId),
            evaluaciones = evaluacionDao.getEvaluacionesListByOptica(opticaId),
            dispensaciones = dispensacionDao.getDispensacionesListByOptica(opticaId),
            pagos = pagoDao.getPagosListByOptica(opticaId),
            serviciosExtra = servicioExtraDao.getServiciosListByOptica(opticaId)
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

    suspend fun resolveDuplicatePacientesByHistoria(opticaId: String): DuplicateHoResolutionResult {
        val pacientes = pacienteDao.getPacientesListByOptica(opticaId)
        val grouped = pacientes
            .mapNotNull { p ->
                val ho = p.historiaOptometrica?.trim()?.uppercase().orEmpty()
                if (ho.isBlank()) null else ho to p
            }
            .groupBy({ it.first }, { it.second })
            .filterValues { it.size > 1 }
        if (grouped.isEmpty()) return DuplicateHoResolutionResult()

        var mergedPacientes = 0
        var movedEvaluaciones = 0
        var movedDispensaciones = 0
        var movedServicios = 0

        database.withTransaction {
            grouped.forEach { (_, rows) ->
                val canonical = rows.minByOrNull { it.fechaCreacion } ?: return@forEach
                rows.forEach { duplicate ->
                    if (duplicate.id == canonical.id) return@forEach

                    val mergedCanonical = canonical.mergeWith(duplicate)
                    pacienteDao.updatePaciente(mergedCanonical)
                    movedEvaluaciones += pacienteDao.reassignEvaluacionesPaciente(duplicate.id, canonical.id)
                    movedDispensaciones += pacienteDao.reassignDispensacionesPaciente(duplicate.id, canonical.id)
                    movedServicios += pacienteDao.reassignServiciosPaciente(duplicate.id, canonical.id)
                    pacienteDao.deletePacienteById(duplicate.id)
                    mergedPacientes++
                }
            }
        }
        return DuplicateHoResolutionResult(
            mergedPacientes = mergedPacientes,
            movedEvaluaciones = movedEvaluaciones,
            movedDispensaciones = movedDispensaciones,
            movedServicios = movedServicios
        )
    }
}

data class DuplicateHoResolutionResult(
    val mergedPacientes: Int = 0,
    val movedEvaluaciones: Int = 0,
    val movedDispensaciones: Int = 0,
    val movedServicios: Int = 0
)

private fun Paciente.mergeWith(other: Paciente): Paciente {
    fun chooseText(primary: String?, fallback: String?): String? =
        primary?.takeIf { it.isNotBlank() } ?: fallback?.takeIf { it.isNotBlank() }
    return copy(
        nombreCompleto = if (nombreCompleto.isNotBlank()) nombreCompleto else other.nombreCompleto,
        edad = maxOf(edad, other.edad),
        telefono = chooseText(telefono, other.telefono).orEmpty(),
        dni = chooseText(dni, other.dni),
        fechaNacimiento = fechaNacimiento ?: other.fechaNacimiento,
        sexo = chooseText(sexo, other.sexo),
        email = chooseText(email, other.email),
        historiaOptometrica = chooseText(historiaOptometrica, other.historiaOptometrica),
        direccion = chooseText(direccion, other.direccion),
        distrito = chooseText(distrito, other.distrito),
        ocupacion = chooseText(ocupacion, other.ocupacion),
        acompanante = chooseText(acompanante, other.acompanante),
        hobbies = chooseText(hobbies, other.hobbies),
        ultimasEtiquetas = (ultimasEtiquetas + other.ultimasEtiquetas).distinct()
    )
}

data class BackupData(
    val version: Int = 3,
    val dateExported: Long = System.currentTimeMillis(),
    val appIdentifier: String = "OptoApp-2.0",
    @SerializedName("source_optica_id")
    val sourceOpticaId: String? = null,
    val pacientes: List<Paciente>? = emptyList(),
    val evaluaciones: List<EvaluacionClinica>? = emptyList(),
    @SerializedName("dispensaciones", alternate = ["ordenes", "ventas"])
    val dispensaciones: List<DispensacionOptica>? = emptyList(),
    val pagos: List<Pago>? = emptyList(),
    @SerializedName("serviciosExtra", alternate = ["servicios", "otrosServicios"])
    val serviciosExtra: List<ServicioExtra>? = emptyList()
)
