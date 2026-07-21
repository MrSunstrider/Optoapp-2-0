package com.example.optoapp.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface DispensacionItemDao {

    @MapInfo(keyColumn = "dispensacionId", valueColumn = "costoTotal")
    @Query(
        """
        SELECT dispensacion_id AS dispensacionId,
        COALESCE(SUM(COALESCE(costo_real_od, 0) + COALESCE(costo_real_oi, 0) + COALESCE(costo_real_montura, 0) + COALESCE(costo_real_biselado, 0) + COALESCE(costo_real_lc, 0)), 0) AS costoTotal
        FROM dispensacion_items 
        WHERE dispensacion_id IN (:ids) 
        GROUP BY dispensacion_id
    """,
    )
    suspend fun getCostosByDispensacionIds(ids: Set<String>): Map<String, Double>

    @Query("SELECT * FROM dispensacion_items WHERE dispensacion_id = :dispensacionId ORDER BY rowid")
    fun getItemsByDispensacion(dispensacionId: String): Flow<List<DispensacionItem>>

    @Query("SELECT * FROM dispensacion_items WHERE dispensacion_id = :dispensacionId ORDER BY rowid")
    suspend fun getItemsListByDispensacion(dispensacionId: String): List<DispensacionItem>

    @Query("SELECT * FROM dispensacion_items WHERE id = :id")
    suspend fun getById(id: String): DispensacionItem?

    @Query("SELECT * FROM dispensacion_items WHERE optica_id = :opticaId")
    suspend fun getItemsListByOptica(opticaId: String): List<DispensacionItem>

    @Deprecated(
        message = "Use getItemsListByOptica to enforce multi-tenant isolation",
        replaceWith = ReplaceWith("getItemsListByOptica(opticaId)"),
    )
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
