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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPaciente(paciente: Paciente)

    @Update
    suspend fun updatePaciente(paciente: Paciente)

    @Delete
    suspend fun deletePaciente(paciente: Paciente)
    
    @Query("DELETE FROM pacientes")
    suspend fun deleteAll()
}

@Dao
interface EvaluacionDao {
    @Query("SELECT * FROM evaluaciones WHERE pacienteId = :pacienteId ORDER BY fecha DESC")
    fun getEvaluacionesByPaciente(pacienteId: String): Flow<List<EvaluacionClinica>>

    @Query("SELECT * FROM evaluaciones WHERE id = :id")
    suspend fun getEvaluacionById(id: String): EvaluacionClinica?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvaluacion(evaluacion: EvaluacionClinica)
    
    @Query("DELETE FROM evaluaciones")
    suspend fun deleteAll()
    
    @Query("SELECT * FROM evaluaciones")
    suspend fun getAllEvaluaciones(): List<EvaluacionClinica>
}

@Dao
interface DispensacionDao {
    @Query("SELECT * FROM dispensaciones WHERE pacienteId = :pacienteId ORDER BY fecha DESC")
    fun getDispensacionesByPaciente(pacienteId: String): Flow<List<DispensacionOptica>>

    @Query("SELECT * FROM dispensaciones")
    fun getAllDispensaciones(): Flow<List<DispensacionOptica>>

    @Query("SELECT * FROM dispensaciones WHERE id = :id")
    suspend fun getDispensacionById(id: String): DispensacionOptica?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDispensacion(dispensacion: DispensacionOptica)
    
    @Query("DELETE FROM dispensaciones")
    suspend fun deleteAll()
    
    @Query("SELECT * FROM dispensaciones")
    suspend fun getAllDispensacionesList(): List<DispensacionOptica>
}

@Dao
interface PagoDao {
    @Query("SELECT * FROM pagos WHERE dispensacionId = :dispensacionId ORDER BY fecha DESC")
    fun getPagosByDispensacion(dispensacionId: String): Flow<List<Pago>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPago(pago: Pago)
    
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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertServicio(servicio: ServicioExtra)

    @Delete
    suspend fun deleteServicio(servicio: ServicioExtra)
    
    @Query("DELETE FROM servicios_extra")
    suspend fun deleteAll()
}
