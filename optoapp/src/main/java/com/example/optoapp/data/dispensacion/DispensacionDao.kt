package com.example.optoapp.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface DispensacionDao {
    @Query("SELECT * FROM dispensaciones WHERE pacienteId = :pacienteId AND opticaId = :opticaId ORDER BY fecha DESC")
    fun getDispensacionesByPaciente(pacienteId: String, opticaId: String): Flow<List<DispensacionOptica>>

    @Query("SELECT * FROM dispensaciones WHERE opticaId = :opticaId")
    fun getAllDispensacionesForOptica(opticaId: String): Flow<List<DispensacionOptica>>

    @Query("SELECT * FROM dispensaciones WHERE id = :id AND opticaId = :opticaId")
    suspend fun getDispensacionById(id: String, opticaId: String): DispensacionOptica?

    @Upsert
    suspend fun insertDispensacion(dispensacion: DispensacionOptica)

    @Update
    suspend fun updateDispensacion(dispensacion: DispensacionOptica)

    @Query("UPDATE dispensaciones SET opticaId = :newOpticaId WHERE opticaId = 'mi_optica_base'")
    suspend fun reassignFromLegacyMiOpticaBase(newOpticaId: String): Int

    @Query("SELECT * FROM dispensaciones WHERE opticaId = :opticaId")
    suspend fun getDispensacionesListByOptica(opticaId: String): List<DispensacionOptica>

    @Query("SELECT * FROM dispensaciones WHERE fecha >= :start AND fecha <= :end AND opticaId = :opticaId")
    fun getDispensacionesByDateRangeForOptica(start: LocalDate, end: LocalDate, opticaId: String): Flow<List<DispensacionOptica>>

    @Query("SELECT SUM(montoTotal) FROM dispensaciones WHERE opticaId = :opticaId")
    fun getTotalVendidoForOptica(opticaId: String): Flow<Double?>

    @Query("SELECT SUM(montoPagado) FROM dispensaciones WHERE opticaId = :opticaId")
    fun getTotalPagadoForOptica(opticaId: String): Flow<Double?>

    @Query("SELECT ot FROM dispensaciones WHERE opticaId = :opticaId AND ot LIKE ('OT-' || :year || '-%')")
    suspend fun getOtsWithYearPrefix(opticaId: String, year: String): List<String>

    @Query("DELETE FROM dispensaciones WHERE id = :id AND opticaId = :opticaId")
    suspend fun deleteById(id: String, opticaId: String): Int

    @Query("SELECT * FROM dispensaciones WHERE pacienteId = :pacienteId AND opticaId = :opticaId ORDER BY fecha DESC LIMIT 1")
    suspend fun getLastDispensacionByPacienteId(pacienteId: String, opticaId: String): DispensacionOptica?

    @Query("SELECT * FROM dispensaciones WHERE id IN (:ids) AND opticaId = :opticaId")
    suspend fun getDispensacionesByIds(ids: List<String>, opticaId: String): List<DispensacionOptica>
}
