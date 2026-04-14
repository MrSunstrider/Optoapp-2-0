package com.example.optoapp.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import com.google.gson.annotations.SerializedName
import java.time.LocalDate

class OptoRepository(
    private val pacienteDao: PacienteDao,
    private val evaluacionDao: EvaluacionDao,
    private val dispensacionDao: DispensacionDao,
    private val pagoDao: PagoDao,
    private val servicioExtraDao: ServicioExtraDao
) {
    fun pacientesFlowForOptica(opticaId: String): Flow<List<Paciente>> =
        pacienteDao.getPacientesByOptica(opticaId)

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
    
    suspend fun insertPaciente(paciente: Paciente) = pacienteDao.insertPaciente(paciente)
    
    suspend fun updatePaciente(paciente: Paciente) = pacienteDao.updatePaciente(paciente)
    
    suspend fun deletePaciente(paciente: Paciente) = pacienteDao.deletePaciente(paciente)
    
    fun getEvaluacionesByPaciente(pacienteId: String): Flow<List<EvaluacionClinica>> = 
        evaluacionDao.getEvaluacionesByPaciente(pacienteId)
        
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
    
    suspend fun insertEvaluacion(evaluacion: EvaluacionClinica) = evaluacionDao.insertEvaluacion(evaluacion)
    
    suspend fun updateEvaluacion(evaluacion: EvaluacionClinica) = evaluacionDao.updateEvaluacion(evaluacion)
    
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
        
    suspend fun insertDispensacion(dispensacion: DispensacionOptica) = dispensacionDao.insertDispensacion(dispensacion)
    
    suspend fun updateDispensacion(dispensacion: DispensacionOptica) = dispensacionDao.updateDispensacion(dispensacion)
    
    fun getPagosByDispensacion(dispensacionId: String): Flow<List<Pago>> = pagoDao.getPagosByDispensacion(dispensacionId)
    
    suspend fun insertPago(pago: Pago) = pagoDao.insertPago(pago)
    
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
    
    suspend fun insertServicio(servicio: ServicioExtra) = servicioExtraDao.insertServicio(servicio)
    
    suspend fun updateServicio(servicio: ServicioExtra) = servicioExtraDao.updateServicio(servicio)
    
    suspend fun deleteServicio(servicio: ServicioExtra) = servicioExtraDao.deleteServicio(servicio)

    // ─── Métodos de sincronización (Fase 3) ──────────────────────────────────
    // Devuelven una lista completa en un instante dado (snapshot), usados por los
    // Use Cases de sincronización que necesitan leer todos los registros de una vez.

    /** Alias de upsert para la bajada de datos desde Supabase. */
    suspend fun upsertPaciente(paciente: Paciente) = pacienteDao.insertPaciente(paciente)

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

    // ─────────────────────────────────────────────────────────────────────────

    suspend fun clearAllData() {
        servicioExtraDao.deleteAll()
        pagoDao.deleteAll()
        dispensacionDao.deleteAll()
        evaluacionDao.deleteAll()
        pacienteDao.deleteAll()
    }
    
    suspend fun getBackupDataForOptica(opticaId: String): BackupData {
        return BackupData(
            pacientes = pacienteDao.getPacientesListByOptica(opticaId),
            evaluaciones = evaluacionDao.getEvaluacionesListByOptica(opticaId),
            dispensaciones = dispensacionDao.getDispensacionesListByOptica(opticaId),
            pagos = pagoDao.getPagosListByOptica(opticaId),
            serviciosExtra = servicioExtraDao.getServiciosListByOptica(opticaId)
        )
    }

    private fun <T : Any> T.sanitizeNulls(): T {
        try {
            this::class.java.declaredFields.forEach { field ->
                field.isAccessible = true
                val value = field.get(this)
                if (value == null) {
                    if (field.type == String::class.java) {
                        field.set(this, "")
                    } else if (field.type == List::class.java) {
                        field.set(this, emptyList<Any>())
                    } else if (field.type == Double::class.java || field.type == Double::class.javaPrimitiveType) {
                        field.set(this, 0.0)
                    } else if (field.type == Int::class.java || field.type == Int::class.javaPrimitiveType) {
                        field.set(this, 0)
                    } else if (field.type == Long::class.java || field.type == Long::class.javaPrimitiveType) {
                        field.set(this, 0L)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return this
    }

    suspend fun restoreBackup(backupData: BackupData, currentOpticaId: String) {
        // Ejecutamos en una transacción secuencial limpiando primero.
        clearAllData()
        
        backupData.pacientes?.forEach { 
            try { 
                insertPaciente(it.sanitizeNulls().copy(opticaId = currentOpticaId)) 
            } catch(e: Exception) { 
                e.printStackTrace() 
            } 
        }
        
        backupData.evaluaciones?.forEach { 
            try { 
                insertEvaluacion(it.sanitizeNulls().copy(opticaId = currentOpticaId)) 
            } catch(e: Exception) { 
                e.printStackTrace() 
            } 
        }
        
        backupData.dispensaciones?.forEach { 
            try { 
                insertDispensacion(it.sanitizeNulls().copy(opticaId = currentOpticaId)) 
            } catch(e: Exception) { 
                e.printStackTrace() 
            } 
        }
        
        backupData.pagos?.forEach { 
            try { 
                insertPago(it.sanitizeNulls().copy(opticaId = currentOpticaId)) 
            } catch(e: Exception) { 
                e.printStackTrace() 
            } 
        }
        
        backupData.serviciosExtra?.forEach { 
            try { 
                insertServicio(it.sanitizeNulls().copy(opticaId = currentOpticaId)) 
            } catch(e: Exception) { 
                e.printStackTrace() 
            } 
        }
    }
}

data class BackupData(
    val version: Int = 3,
    val dateExported: Long = System.currentTimeMillis(),
    val appIdentifier: String = "OptoApp-2.0",
    val pacientes: List<Paciente>? = emptyList(),
    val evaluaciones: List<EvaluacionClinica>? = emptyList(),
    @SerializedName("dispensaciones", alternate = ["ordenes", "ventas"])
    val dispensaciones: List<DispensacionOptica>? = emptyList(),
    val pagos: List<Pago>? = emptyList(),
    @SerializedName("serviciosExtra", alternate = ["servicios", "otrosServicios"])
    val serviciosExtra: List<ServicioExtra>? = emptyList()
)
