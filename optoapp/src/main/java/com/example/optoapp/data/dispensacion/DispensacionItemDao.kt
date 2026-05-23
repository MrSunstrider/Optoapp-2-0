package com.example.optoapp.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface DispensacionItemDao {

    @Query("SELECT * FROM dispensacion_items WHERE dispensacionId = :dispensacionId ORDER BY rowid")
    fun getItemsByDispensacion(dispensacionId: String): Flow<List<DispensacionItem>>

    @Query("SELECT * FROM dispensacion_items WHERE dispensacionId = :dispensacionId ORDER BY rowid")
    suspend fun getItemsListByDispensacion(dispensacionId: String): List<DispensacionItem>

    @Query("SELECT * FROM dispensacion_items WHERE opticaId = :opticaId")
    suspend fun getItemsListByOptica(opticaId: String): List<DispensacionItem>

    @Query("SELECT * FROM dispensacion_items")
    suspend fun getAllItems(): List<DispensacionItem>

    @Upsert
    suspend fun insertItem(item: DispensacionItem)

    @Query("DELETE FROM dispensacion_items WHERE id = :id")
    suspend fun deleteById(id: String): Int

    @Query("DELETE FROM dispensacion_items WHERE dispensacionId = :dispensacionId")
    suspend fun deleteByDispensacionId(dispensacionId: String): Int

    @Query("DELETE FROM dispensacion_items")
    suspend fun deleteAll()
}
