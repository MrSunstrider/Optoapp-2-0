package com.example.optoapp.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface DispensacionItemDao {

    @Query("SELECT * FROM dispensacion_items WHERE dispensacion_id = :dispensacionId AND optica_id = :opticaId ORDER BY rowid")
    fun getItemsByDispensacion(dispensacionId: String, opticaId: String): Flow<List<DispensacionItem>>

    @Query("SELECT * FROM dispensacion_items WHERE dispensacion_id = :dispensacionId AND optica_id = :opticaId ORDER BY rowid")
    suspend fun getItemsListByDispensacion(dispensacionId: String, opticaId: String): List<DispensacionItem>

    @Query("SELECT * FROM dispensacion_items WHERE id = :id AND optica_id = :opticaId")
    suspend fun getById(id: String, opticaId: String): DispensacionItem?

    @Query("SELECT * FROM dispensacion_items WHERE optica_id = :opticaId")
    suspend fun getItemsListByOptica(opticaId: String): List<DispensacionItem>

    // Legacy — prefer getItemsListByOptica for multi-tenant isolation
    @Query("SELECT * FROM dispensacion_items")
    suspend fun getAllItems(): List<DispensacionItem>

    @Upsert
    suspend fun insertItem(item: DispensacionItem)

    @Query("DELETE FROM dispensacion_items WHERE id = :id AND optica_id = :opticaId")
    suspend fun deleteById(id: String, opticaId: String): Int

    @Query("DELETE FROM dispensacion_items WHERE dispensacion_id = :dispensacionId AND optica_id = :opticaId")
    suspend fun deleteByDispensacionId(dispensacionId: String, opticaId: String): Int

    @Query("UPDATE dispensacion_items SET dispensacion_id = :targetId WHERE dispensacion_id = :sourceId AND optica_id = :opticaId")
    suspend fun reassignItemsDispensacion(sourceId: String, targetId: String, opticaId: String): Int
}
