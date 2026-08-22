package com.example.optoapp.data.pago

import androidx.room.*
import com.example.optoapp.data.Pago
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface PagoDao {
    @Query("SELECT * FROM pagos WHERE id = :id AND opticaId = :opticaId")
    suspend fun getPagoByIdForOptica(id: String, opticaId: String): Pago?

    @Query("SELECT * FROM pagos WHERE dispensacionId = :dispensacionId AND opticaId = :opticaId ORDER BY fecha DESC")
    fun getPagosByDispensacion(dispensacionId: String, opticaId: String): Flow<List<Pago>>

    @Query("SELECT * FROM pagos WHERE servicioExtraId = :servicioExtraId AND opticaId = :opticaId ORDER BY fecha DESC")
    fun getPagosByServicioExtra(servicioExtraId: String, opticaId: String): Flow<List<Pago>>

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
        ventaId=:ventaId, reversaPagoId=:reversaPagoId
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
        reversaPagoId: String?,
    ): Int

    @Query("SELECT * FROM pagos WHERE reversaPagoId = :originalPagoId AND tipo = 'Reverso' AND opticaId = :opticaId LIMIT 1")
    suspend fun getReversoByOriginalId(originalPagoId: String, opticaId: String): Pago?

    @Query(
        """
        SELECT * FROM pagos
        WHERE (dispensacionId = :parentId OR servicioExtraId = :parentId)
          AND tipo IN ('Abono', 'Pago completo')
          AND opticaId = :opticaId
        """,
    )
    suspend fun getCreditPagosByParent(parentId: String, opticaId: String): List<Pago>

    @Query("DELETE FROM pagos WHERE id = :id AND opticaId = :opticaId")
    suspend fun deletePago(id: String, opticaId: String): Int

    @Query("UPDATE pagos SET opticaId = :newOpticaId WHERE opticaId = 'mi_optica_base'")
    suspend fun reassignFromLegacyMiOpticaBase(newOpticaId: String): Int

    @Query("SELECT * FROM pagos WHERE opticaId = :opticaId")
    suspend fun getPagosListByOptica(opticaId: String): List<Pago>

    @Query("SELECT * FROM pagos WHERE opticaId = :opticaId")
    fun getPagosFlowByOptica(opticaId: String): Flow<List<Pago>>

    @Query("UPDATE pagos SET dispensacionId = :newDispensacionId WHERE dispensacionId = :oldDispensacionId AND opticaId = :opticaId")
    suspend fun reassignDispensacionIdForOptica(oldDispensacionId: String, newDispensacionId: String, opticaId: String): Int

    @Query(
        """
        SELECT COALESCE(SUM(
          CASE TRIM(tipo)
            WHEN 'Abono' THEN monto
            WHEN 'Pago completo' THEN monto
            WHEN 'Reembolso' THEN -monto
            WHEN 'Reverso' THEN -monto
            ELSE 0
          END
        ), 0) FROM pagos WHERE dispensacionId = :dispensacionId AND opticaId = :opticaId
        """,
    )
    suspend fun sumMontoByDispensacion(dispensacionId: String, opticaId: String): Double

    @Query(
        """
        SELECT COALESCE(SUM(
          CASE TRIM(tipo)
            WHEN 'Abono' THEN monto
            WHEN 'Pago completo' THEN monto
            WHEN 'Reembolso' THEN -monto
            WHEN 'Reverso' THEN -monto
            ELSE 0
          END
        ), 0) FROM pagos WHERE servicioExtraId = :servicioExtraId AND opticaId = :opticaId
        """,
    )
    suspend fun sumMontoByServicioExtra(servicioExtraId: String, opticaId: String): Double
}
