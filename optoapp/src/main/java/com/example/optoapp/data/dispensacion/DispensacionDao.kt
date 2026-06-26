package com.example.optoapp.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface DispensacionDao {
    @Query("SELECT * FROM dispensaciones WHERE pacienteId = :pacienteId ORDER BY fecha DESC")
    fun getDispensacionesByPaciente(pacienteId: String): Flow<List<DispensacionOptica>>

    @Query("SELECT * FROM dispensaciones")
    fun getAllDispensaciones(): Flow<List<DispensacionOptica>>

    @Query("SELECT * FROM dispensaciones WHERE opticaId = :opticaId")
    fun getAllDispensacionesForOptica(opticaId: String): Flow<List<DispensacionOptica>>

    @Query("SELECT SUM(montoTotal) FROM dispensaciones")
    fun getTotalVendido(): Flow<Double?>

    @Query("SELECT SUM(montoPagado) FROM dispensaciones")
    fun getTotalPagado(): Flow<Double?>

    @Query("SELECT * FROM dispensaciones WHERE id = :id")
    suspend fun getDispensacionById(id: String): DispensacionOptica?

    @Upsert
    suspend fun insertDispensacion(dispensacion: DispensacionOptica)

    @Update
    suspend fun updateDispensacion(dispensacion: DispensacionOptica)

    @Query("DELETE FROM dispensaciones")
    suspend fun deleteAll()

    @Query("UPDATE dispensaciones SET opticaId = :newOpticaId WHERE opticaId = 'mi_optica_base'")
    suspend fun reassignFromLegacyMiOpticaBase(newOpticaId: String): Int

    @Query("SELECT * FROM dispensaciones")
    suspend fun getAllDispensacionesList(): List<DispensacionOptica>

    @Query("SELECT * FROM dispensaciones WHERE opticaId = :opticaId")
    suspend fun getDispensacionesListByOptica(opticaId: String): List<DispensacionOptica>

    @Query("SELECT * FROM dispensaciones WHERE fecha >= :start AND fecha <= :end")
    fun getDispensacionesByDateRange(start: LocalDate, end: LocalDate): Flow<List<DispensacionOptica>>

    @Query("SELECT * FROM dispensaciones WHERE fecha >= :start AND fecha <= :end AND opticaId = :opticaId")
    fun getDispensacionesByDateRangeForOptica(start: LocalDate, end: LocalDate, opticaId: String): Flow<List<DispensacionOptica>>

    @Query("SELECT SUM(montoTotal) FROM dispensaciones WHERE opticaId = :opticaId")
    fun getTotalVendidoForOptica(opticaId: String): Flow<Double?>

    @Query("SELECT SUM(montoPagado) FROM dispensaciones WHERE opticaId = :opticaId")
    fun getTotalPagadoForOptica(opticaId: String): Flow<Double?>

    @Query("SELECT ot FROM dispensaciones WHERE opticaId = :opticaId AND ot LIKE ('OT-' || :year || '-%')")
    suspend fun getOtsWithYearPrefix(opticaId: String, year: String): List<String>

    @Query("DELETE FROM dispensaciones WHERE id = :id")
    suspend fun deleteById(id: String): Int

    @Query("SELECT * FROM dispensaciones WHERE pacienteId = :pacienteId ORDER BY fecha DESC LIMIT 1")
    suspend fun getLastDispensacionByPacienteId(pacienteId: String): DispensacionOptica?
}
