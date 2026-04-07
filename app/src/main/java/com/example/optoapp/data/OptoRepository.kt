package com.example.optoapp.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class OptoRepository(
    private val pacienteDao: PacienteDao,
    private val evaluacionDao: EvaluacionDao,
    private val dispensacionDao: DispensacionDao,
    private val pagoDao: PagoDao,
    private val servicioExtraDao: ServicioExtraDao
) {
    val allPacientes: Flow<List<Paciente>> = pacienteDao.getAllPacientes()
    
    fun searchPacientes(query: String): Flow<List<Paciente>> = pacienteDao.searchPacientes(query)
    
    fun getPacientesWithPendingBalance(): Flow<List<Paciente>> = pacienteDao.getPacientesWithPendingBalance()
    
    fun getPacientesWithPendingDelivery(): Flow<List<Paciente>> = pacienteDao.getPacientesWithPendingDelivery()
    
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
        
    fun countEvaluacionesInRange(start: Long, end: Long): Flow<Int> = evaluacionDao.countEvaluacionesInRange(start, end)
    
    fun getDispensacionesByDateRange(start: Long, end: Long): Flow<List<DispensacionOptica>> = dispensacionDao.getDispensacionesByDateRange(start, end)
        
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
    
    fun getTotalVendido(): Flow<Double?> = dispensacionDao.getTotalVendido()
    
    fun getTotalPagado(): Flow<Double?> = dispensacionDao.getTotalPagado()
    
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
        
    fun getPagosByDateRange(start: Long, end: Long): Flow<List<Pago>> = 
        pagoDao.getPagosByDateRange(start, end)

    // Servicios Extra
    fun getAllServicios(): Flow<List<ServicioExtra>> = servicioExtraDao.getAllServicios()
    
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

    suspend fun clearAllData() {
        servicioExtraDao.deleteAll()
        pagoDao.deleteAll()
        dispensacionDao.deleteAll()
        evaluacionDao.deleteAll()
        pacienteDao.deleteAll()
    }
    
    suspend fun getBackupData(): BackupData {
        return BackupData(
            pacientes = allPacientes.first(),
            evaluaciones = evaluacionDao.getAllEvaluaciones(),
            dispensaciones = dispensacionDao.getAllDispensacionesList(),
            pagos = pagoDao.getAllPagos(),
            serviciosExtra = servicioExtraDao.getAllServicios().first()
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

    suspend fun restoreBackup(backupData: BackupData) {
        // Ejecutamos en una transacción para evitar inconsistencias si algo falla a mitad de camino
        // Aunque Room no permite @Transaction en suspend functions de Repositorios fácilmente sin acceso directo a DB,
        // lo hacemos secuencialmente limpiando primero.
        clearAllData()
        
        backupData.pacientes?.forEach { 
            try { 
                insertPaciente(it.sanitizeNulls()) 
            } catch(e: Exception) { 
                e.printStackTrace() 
            } 
        }
        
        backupData.evaluaciones?.forEach { 
            try { 
                insertEvaluacion(it.sanitizeNulls()) 
            } catch(e: Exception) { 
                e.printStackTrace() 
            } 
        }
        
        backupData.dispensaciones?.forEach { 
            try { 
                insertDispensacion(it.sanitizeNulls()) 
            } catch(e: Exception) { 
                e.printStackTrace() 
            } 
        }
        
        backupData.pagos?.forEach { 
            try { 
                insertPago(it.sanitizeNulls()) 
            } catch(e: Exception) { 
                e.printStackTrace() 
            } 
        }
        
        backupData.serviciosExtra?.forEach { 
            try { 
                insertServicio(it.sanitizeNulls()) 
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
    val dispensaciones: List<DispensacionOptica>? = emptyList(),
    val pagos: List<Pago>? = emptyList(),
    val serviciosExtra: List<ServicioExtra>? = emptyList()
)
