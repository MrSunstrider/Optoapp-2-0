package com.example.optoapp.data.inventariofisico

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.optoapp.data.InventarioFisico
import com.example.optoapp.data.InventarioFisicoDetalle
import kotlinx.coroutines.flow.Flow

@Dao
interface InventarioFisicoDao {
    @Query("SELECT * FROM inventario_fisico WHERE opticaId = :opticaId ORDER BY fecha DESC")
    fun getByOptica(opticaId: String): Flow<List<InventarioFisico>>

    @Query("SELECT * FROM inventario_fisico WHERE opticaId = :opticaId AND estado = 'EN_PROGRESO'")
    suspend fun getActiveByOptica(opticaId: String): InventarioFisico?

    @Query("SELECT * FROM inventario_fisico WHERE id = :id")
    suspend fun getById(id: String): InventarioFisico?

    @Query("SELECT * FROM inventario_fisico_detalle WHERE inventarioId = :inventarioId")
    suspend fun getDetalles(inventarioId: String): List<InventarioFisicoDetalle>

    @Insert
    suspend fun insertSession(session: InventarioFisico)

    @Insert
    suspend fun insertDetalles(detalles: List<InventarioFisicoDetalle>)

    @Update
    suspend fun updateDetalle(detalle: InventarioFisicoDetalle)

    @Update
    suspend fun updateSession(session: InventarioFisico)
}
