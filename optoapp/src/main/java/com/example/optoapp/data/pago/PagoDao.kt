package com.example.optoapp.data.pago

import androidx.room.*
import com.example.optoapp.data.Pago
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface PagoDao {
    @Deprecated(
        message = "Use getPagoByIdForOptica to enforce multi-tenant isolation",
        replaceWith = ReplaceWith("getPagoByIdForOptica(id, opticaId)")
    )
    @Query("SELECT * FROM pagos WHERE id = :id")
    suspend fun getPagoById(id: String): Pago?

    @Query("SELECT * FROM pagos WHERE id = :id AND opticaId = :opticaId")
    suspend fun getPagoByIdForOptica(id: String, opticaId: String): Pago?

    @Query("SELECT * FROM pagos WHERE dispensacionId = :dispensacionId ORDER BY fecha DESC")
    fun getPagosByDispensacion(dispensacionId: String): Flow<List<Pago>>

    @Query("SELECT * FROM pagos WHERE servicioExtraId = :servicioExtraId ORDER BY fecha DESC")
    fun getPagosByServicioExtra(servicioExtraId: String): Flow<List<Pago>>

    @Deprecated(
        message = "Use getPagosByDateRangeForOptica to enforce multi-tenant isolation",
        replaceWith = ReplaceWith("getPagosByDateRangeForOptica(start, end, opticaId)")
    )
    @Query("SELECT * FROM pagos WHERE fecha >= :start AND fecha <= :end ORDER BY fecha DESC")
    fun getPagosByDateRange(start: LocalDate, end: LocalDate): Flow<List<Pago>>

    @Query("SELECT * FROM pagos WHERE fecha >= :start AND fecha <= :end AND opticaId = :opticaId ORDER BY fecha DESC")
    fun getPagosByDateRangeForOptica(start: LocalDate, end: LocalDate, opticaId: String): Flow<List<Pago>>

    @Upsert
    suspend fun insertPago(pago: Pago)

    @Update
    suspend fun updatePago(pago: Pago)

    @Delete
    suspend fun deletePago(pago: Pago)

    @Query("DELETE FROM pagos WHERE opticaId = :opticaId")
    suspend fun deleteAll(opticaId: String)

    @Query("UPDATE pagos SET opticaId = :newOpticaId WHERE opticaId = 'mi_optica_base'")
    suspend fun reassignFromLegacyMiOpticaBase(newOpticaId: String): Int

    @Deprecated(
        message = "Use getPagosListByOptica to enforce multi-tenant isolation",
        replaceWith = ReplaceWith("getPagosListByOptica(opticaId)")
    )
    @Query("SELECT * FROM pagos")
    suspend fun getAllPagos(): List<Pago>

    @Query("SELECT * FROM pagos WHERE opticaId = :opticaId")
    suspend fun getPagosListByOptica(opticaId: String): List<Pago>

    @Deprecated(
        message = "Use reassignDispensacionIdForOptica to enforce multi-tenant isolation",
        replaceWith = ReplaceWith("reassignDispensacionIdForOptica(oldDispensacionId, newDispensacionId, opticaId)")
    )
    @Query("UPDATE pagos SET dispensacionId = :newDispensacionId WHERE dispensacionId = :oldDispensacionId")
    suspend fun reassignDispensacionId(oldDispensacionId: String, newDispensacionId: String): Int

    @Query("UPDATE pagos SET dispensacionId = :newDispensacionId WHERE dispensacionId = :oldDispensacionId AND opticaId = :opticaId")
    suspend fun reassignDispensacionIdForOptica(oldDispensacionId: String, newDispensacionId: String, opticaId: String): Int

    @Query("SELECT COALESCE(SUM(monto), 0) FROM pagos WHERE dispensacionId = :dispensacionId AND tipo != :excludeTipoAnulacion")
    suspend fun sumMontoByDispensacion(dispensacionId: String, excludeTipoAnulacion: String): Double

    @Query("SELECT COALESCE(SUM(monto), 0) FROM pagos WHERE servicioExtraId = :servicioExtraId")
    suspend fun sumMontoByServicioExtra(servicioExtraId: String): Double
}
