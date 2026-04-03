package com.example.optoapp.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PacienteDao {
    @Query("SELECT * FROM pacientes ORDER BY nombreCompleto ASC")
    fun getAllPacientes(): Flow<List<Paciente>>

    @Query("SELECT * FROM pacientes WHERE id = :id")
    suspend fun getPacienteById(id: String): Paciente?

    @Query("SELECT * FROM pacientes WHERE nombreCompleto LIKE '%' || :query || '%' OR id LIKE '%' || :query || '%' OR telefono LIKE '%' || :query || '%'")
    fun searchPacientes(query: String): Flow<List<Paciente>>

    @Upsert
    suspend fun insertPaciente(paciente: Paciente)

    @Update
    suspend fun updatePaciente(paciente: Paciente)

    @Delete
    suspend fun deletePaciente(paciente: Paciente)
    
    @Query("DELETE FROM pacientes")
    suspend fun deleteAll()

    @Query("""
        SELECT * FROM pacientes 
        WHERE id IN (SELECT pacienteId FROM dispensaciones WHERE (montoTotal - montoPagado) > 0)
        OR id IN (SELECT pacienteId FROM servicios_extra WHERE (montoTotal - aCuenta) > 0)
        ORDER BY nombreCompleto ASC
    """)
    fun getPacientesWithPendingBalance(): Flow<List<Paciente>>

    @Query("""
        SELECT * FROM pacientes 
        WHERE id IN (SELECT pacienteId FROM dispensaciones WHERE estadoEntrega = 'Pendiente')
        OR id IN (SELECT pacienteId FROM servicios_extra WHERE estado = 'Pendiente')
        ORDER BY nombreCompleto ASC
    """)
    fun getPacientesWithPendingDelivery(): Flow<List<Paciente>>
}

@Dao
interface EvaluacionDao {
    @Query("SELECT * FROM evaluaciones WHERE pacienteId = :pacienteId ORDER BY fecha DESC")
    fun getEvaluacionesByPaciente(pacienteId: String): Flow<List<EvaluacionClinica>>

    @Query("SELECT * FROM evaluaciones WHERE id = :id")
    suspend fun getEvaluacionById(id: String): EvaluacionClinica?

    @Upsert
    suspend fun insertEvaluacion(evaluacion: EvaluacionClinica)
    
    @Delete
    suspend fun deleteEvaluacion(evaluacion: EvaluacionClinica)
    
    @Query("DELETE FROM evaluaciones")
    suspend fun deleteAll()
    
    @Query("SELECT * FROM evaluaciones")
    suspend fun getAllEvaluaciones(): List<EvaluacionClinica>

    @Query("SELECT COUNT(*) FROM evaluaciones WHERE fecha >= :start AND fecha <= :end")
    fun countEvaluacionesInRange(start: Long, end: Long): Flow<Int>
}

@Dao
interface DispensacionDao {
    @Query("SELECT * FROM dispensaciones WHERE pacienteId = :pacienteId ORDER BY fecha DESC")
    fun getDispensacionesByPaciente(pacienteId: String): Flow<List<DispensacionOptica>>

    @Query("SELECT * FROM dispensaciones")
    fun getAllDispensaciones(): Flow<List<DispensacionOptica>>

    @Query("SELECT SUM(montoTotal) FROM dispensaciones")
    fun getTotalVendido(): Flow<Double?>

    @Query("SELECT SUM(montoPagado) FROM dispensaciones")
    fun getTotalPagado(): Flow<Double?>

    @Query("SELECT * FROM dispensaciones WHERE id = :id")
    suspend fun getDispensacionById(id: String): DispensacionOptica?

    @Upsert
    suspend fun insertDispensacion(dispensacion: DispensacionOptica)
    
    @Query("DELETE FROM dispensaciones")
    suspend fun deleteAll()
    
    @Query("SELECT * FROM dispensaciones")
    suspend fun getAllDispensacionesList(): List<DispensacionOptica>

    @Query("SELECT * FROM dispensaciones WHERE fecha >= :start AND fecha <= :end")
    fun getDispensacionesByDateRange(start: Long, end: Long): Flow<List<DispensacionOptica>>
}

@Dao
interface PagoDao {
    @Query("SELECT * FROM pagos WHERE dispensacionId = :dispensacionId ORDER BY fecha DESC")
    fun getPagosByDispensacion(dispensacionId: String): Flow<List<Pago>>

    @Query("SELECT * FROM pagos WHERE servicioExtraId = :servicioExtraId ORDER BY fecha DESC")
    fun getPagosByServicioExtra(servicioExtraId: String): Flow<List<Pago>>

    @Query("SELECT * FROM pagos WHERE fecha >= :start AND fecha <= :end ORDER BY fecha DESC")
    fun getPagosByDateRange(start: Long, end: Long): Flow<List<Pago>>

    @Upsert
    suspend fun insertPago(pago: Pago)

    @Update
    suspend fun updatePago(pago: Pago)

    @Delete
    suspend fun deletePago(pago: Pago)
    
    @Query("DELETE FROM pagos")
    suspend fun deleteAll()
    
    @Query("SELECT * FROM pagos")
    suspend fun getAllPagos(): List<Pago>
}

@Dao
interface ServicioExtraDao {
    @Query("SELECT * FROM servicios_extra ORDER BY fecha DESC")
    fun getAllServicios(): Flow<List<ServicioExtra>>

    @Query("SELECT * FROM servicios_extra WHERE pacienteId = :pacienteId ORDER BY fecha DESC")
    fun getServiciosByPaciente(pacienteId: String): Flow<List<ServicioExtra>>

    @Query("SELECT * FROM servicios_extra WHERE id = :id")
    suspend fun getServicioById(id: String): ServicioExtra?

    @Upsert
    suspend fun insertServicio(servicio: ServicioExtra)

    @Delete
    suspend fun deleteServicio(servicio: ServicioExtra)
    
    @Query("DELETE FROM servicios_extra")
    suspend fun deleteAll()
}
