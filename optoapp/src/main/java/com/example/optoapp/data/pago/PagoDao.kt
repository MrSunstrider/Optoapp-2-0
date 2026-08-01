package com.example.optoapp.data.pago

import androidx.room.*
import com.example.optoapp.data.Pago
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface PagoDao {
    // Legacy — prefer getPagoByIdForOptica for multi-tenant isolation
    @Query("SELECT * FROM pagos WHERE id = :id")
    suspend fun getPagoById(id: String): Pago?

    @Query("SELECT * FROM pagos WHERE id = :id AND opticaId = :opticaId")
    suspend fun getPagoByIdForOptica(id: String, opticaId: String): Pago?

    @Query("SELECT * FROM pagos WHERE dispensacionId = :dispensacionId ORDER BY fecha DESC")
    fun getPagosByDispensacion(dispensacionId: String): Flow<List<Pago>>

    @Query("SELECT * FROM pagos WHERE servicioExtraId = :servicioExtraId ORDER BY fecha DESC")
    fun getPagosByServicioExtra(servicioExtraId: String): Flow<List<Pago>>

    // Legacy — prefer getPagosByDateRangeForOptica for multi-tenant isolation
    @Query("SELECT * FROM pagos WHERE fecha >= :start AND fecha <= :end ORDER BY fecha DESC")
    fun getPagosByDateRange(start: LocalDate, end: LocalDate): Flow<List<Pago>>

    @Query("SELECT * FROM pagos WHERE fecha >= :start AND fecha <= :end AND opticaId = :opticaId ORDER BY fecha DESC")
    fun getPagosByDateRangeForOptica(start: LocalDate, end: LocalDate, opticaId: String): Flow<List<Pago>>

    @Upsert
    suspend fun insertPago(pago: Pago)

    @Query(
        """
        UPDATE pagos SET dispensacionId=:dispensacionId,
        servicioExtraId=:servicioExtraId, fecha=:fecha, tipo=:tipo,
        monto=:monto, metodoPago=:metodoPago, nota=:nota,
        opticaId=:opticaId, updatedAt=:updatedAt, updatedBy=:updatedBy,
        ventaId=:ventaId
        WHERE id=:id AND opticaId=:opticaId
    """,
    )
    suspend fun updatePago(
        id: String,
        opticaId: String,
        dispensacionId: String?,
        servicioExtraId: String?,
        fecha: java.time.LocalDate,
        tipo: String,
        monto: Double,
        metodoPago: String,
        nota: String,
        updatedAt: String?,
        updatedBy: String?,
        ventaId: String?,
    ): Int

    @Query("DELETE FROM pagos WHERE id = :id AND opticaId = :opticaId")
    suspend fun deletePago(id: String, opticaId: String): Int

    @Query("UPDATE pagos SET opticaId = :newOpticaId WHERE opticaId = 'mi_optica_base'")
    suspend fun reassignFromLegacyMiOpticaBase(newOpticaId: String): Int

    // Legacy — prefer getPagosListByOptica for multi-tenant isolation
    @Query("SELECT * FROM pagos")
    suspend fun getAllPagos(): List<Pago>

    @Query("SELECT * FROM pagos WHERE opticaId = :opticaId")
    suspend fun getPagosListByOptica(opticaId: String): List<Pago>

    @Query("SELECT * FROM pagos WHERE opticaId = :opticaId")
    fun getPagosFlowByOptica(opticaId: String): Flow<List<Pago>>

    // Legacy — prefer reassignDispensacionIdForOptica for multi-tenant isolation
    @Query("UPDATE pagos SET dispensacionId = :newDispensacionId WHERE dispensacionId = :oldDispensacionId")
    suspend fun reassignDispensacionId(oldDispensacionId: String, newDispensacionId: String): Int

    @Query("UPDATE pagos SET dispensacionId = :newDispensacionId WHERE dispensacionId = :oldDispensacionId AND opticaId = :opticaId")
    suspend fun reassignDispensacionIdForOptica(oldDispensacionId: String, newDispensacionId: String, opticaId: String): Int

    @Query("SELECT COALESCE(SUM(monto), 0) FROM pagos WHERE dispensacionId = :dispensacionId AND tipo != :excludeTipoAnulacion")
    suspend fun sumMontoByDispensacion(dispensacionId: String, excludeTipoAnulacion: String): Double

    @Query("SELECT COALESCE(SUM(monto), 0) FROM pagos WHERE servicioExtraId = :servicioExtraId")
    suspend fun sumMontoByServicioExtra(servicioExtraId: String): Double
}
