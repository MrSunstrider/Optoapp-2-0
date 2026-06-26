package com.example.optoapp.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface EvaluacionDao {
    @Query("SELECT * FROM evaluaciones WHERE pacienteId = :pacienteId ORDER BY fecha DESC")
    fun getEvaluacionesByPaciente(pacienteId: String): Flow<List<EvaluacionClinica>>

    @Query("SELECT * FROM evaluaciones WHERE id = :id")
    suspend fun getEvaluacionById(id: String): EvaluacionClinica?

    @Upsert
    suspend fun insertEvaluacion(evaluacion: EvaluacionClinica)

    @Update
    suspend fun updateEvaluacion(evaluacion: EvaluacionClinica)

    @Delete
    suspend fun deleteEvaluacion(evaluacion: EvaluacionClinica)

    @Query("DELETE FROM evaluaciones")
    suspend fun deleteAll()

    @Query("UPDATE evaluaciones SET opticaId = :newOpticaId WHERE opticaId = 'mi_optica_base'")
    suspend fun reassignFromLegacyMiOpticaBase(newOpticaId: String): Int

    @Query("SELECT * FROM evaluaciones")
    suspend fun getAllEvaluaciones(): List<EvaluacionClinica>

    @Query("SELECT * FROM evaluaciones WHERE opticaId = :opticaId")
    suspend fun getEvaluacionesListByOptica(opticaId: String): List<EvaluacionClinica>

    @Query("SELECT COUNT(*) FROM evaluaciones WHERE fecha >= :start AND fecha <= :end")
    fun countEvaluacionesInRange(start: LocalDate, end: LocalDate): Flow<Int>

    @Query("SELECT COUNT(*) FROM evaluaciones WHERE fecha >= :start AND fecha <= :end AND opticaId = :opticaId")
    fun countEvaluacionesInRangeForOptica(start: LocalDate, end: LocalDate, opticaId: String): Flow<Int>

    @Query(
        """
        SELECT * FROM evaluaciones
        WHERE opticaId = :opticaId
        AND proximaCita IS NOT NULL
        AND proximaCita >= :start
        AND proximaCita <= :end
        ORDER BY proximaCita ASC
        """
    )
    fun getEvaluacionesConProximaCitaEnRango(opticaId: String, start: LocalDate, end: LocalDate): Flow<List<EvaluacionClinica>>

    @Query("SELECT * FROM evaluaciones WHERE pacienteId = :pacienteId ORDER BY fecha DESC LIMIT 1")
    suspend fun getLastEvaluacionByPacienteId(pacienteId: String): EvaluacionClinica?
}
