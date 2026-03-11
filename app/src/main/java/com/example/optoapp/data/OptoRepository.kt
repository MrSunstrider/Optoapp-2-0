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
    
    suspend fun getPacienteById(id: String): Paciente? = pacienteDao.getPacienteById(id)
    
    suspend fun insertPaciente(paciente: Paciente) = pacienteDao.insertPaciente(paciente)
    
    suspend fun updatePaciente(paciente: Paciente) = pacienteDao.updatePaciente(paciente)
    
    suspend fun deletePaciente(paciente: Paciente) = pacienteDao.deletePaciente(paciente)
    
    fun getEvaluacionesByPaciente(pacienteId: String): Flow<List<EvaluacionClinica>> = 
        evaluacionDao.getEvaluacionesByPaciente(pacienteId)
        
    suspend fun getEvaluacionById(id: String): EvaluacionClinica? = evaluacionDao.getEvaluacionById(id)
        
    suspend fun insertEvaluacion(evaluacion: EvaluacionClinica) = evaluacionDao.insertEvaluacion(evaluacion)
    
    fun getDispensacionesByPaciente(pacienteId: String): Flow<List<DispensacionOptica>> = 
        dispensacionDao.getDispensacionesByPaciente(pacienteId)
        
    fun getAllDispensaciones(): Flow<List<DispensacionOptica>> = dispensacionDao.getAllDispensaciones()
    
    suspend fun getDispensacionById(id: String): DispensacionOptica? = dispensacionDao.getDispensacionById(id)
        
    suspend fun insertDispensacion(dispensacion: DispensacionOptica) = dispensacionDao.insertDispensacion(dispensacion)
    
    fun getPagosByDispensacion(dispensacionId: String): Flow<List<Pago>> = pagoDao.getPagosByDispensacion(dispensacionId)
    
    suspend fun insertPago(pago: Pago) = pagoDao.insertPago(pago)

    // Servicios Extra
    fun getAllServicios(): Flow<List<ServicioExtra>> = servicioExtraDao.getAllServicios()
    
    fun getServiciosByPaciente(pacienteId: String): Flow<List<ServicioExtra>> = servicioExtraDao.getServiciosByPaciente(pacienteId)
    
    suspend fun getServicioById(id: String): ServicioExtra? = servicioExtraDao.getServicioById(id)
    
    suspend fun insertServicio(servicio: ServicioExtra) = servicioExtraDao.insertServicio(servicio)
    
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

    suspend fun restoreBackup(backupData: BackupData) {
        clearAllData()
        backupData.pacientes.forEach { insertPaciente(it) }
        backupData.evaluaciones.forEach { insertEvaluacion(it) }
        backupData.dispensaciones.forEach { insertDispensacion(it) }
        backupData.pagos.forEach { insertPago(it) }
        backupData.serviciosExtra.forEach { insertServicio(it) }
    }
}

data class BackupData(
    val pacientes: List<Paciente>,
    val evaluaciones: List<EvaluacionClinica>,
    val dispensaciones: List<DispensacionOptica>,
    val pagos: List<Pago>,
    val serviciosExtra: List<ServicioExtra> = emptyList()
)
