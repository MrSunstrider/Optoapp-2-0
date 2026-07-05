package com.example.optoapp.data.venta

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface VentaDao {
    @Query("SELECT * FROM ventas WHERE id = :id")
    suspend fun getVentaById(id: String): Venta?

    @Query("SELECT * FROM ventas WHERE opticaId = :opticaId AND fecha >= :start AND fecha <= :end ORDER BY fecha DESC")
    fun getVentasByOpticaAndDateRange(opticaId: String, start: LocalDate, end: LocalDate): Flow<List<Venta>>

    @Upsert
    suspend fun upsertVenta(venta: Venta)

    @Query("SELECT * FROM ventas WHERE opticaId = :opticaId")
    suspend fun getAllVentasByOptica(opticaId: String): List<Venta>

    @Query("DELETE FROM ventas")
    suspend fun deleteAll()

    @Query("DELETE FROM ventas WHERE id = :id")
    suspend fun deleteById(id: String)
}
