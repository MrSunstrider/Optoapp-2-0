package com.example.optoapp.data.ordencompra

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.optoapp.data.OrdenCompraItem

@Dao
interface OrdenCompraItemDao {
    @Query("SELECT * FROM orden_compra_items WHERE id = :id")
    suspend fun getById(id: String): OrdenCompraItem?

    @Query("SELECT * FROM orden_compra_items WHERE ordenId = :ordenId")
    suspend fun getByOrden(ordenId: String): List<OrdenCompraItem>

    @Query("SELECT * FROM orden_compra_items WHERE monturaId = :monturaId")
    suspend fun getByMontura(monturaId: String): List<OrdenCompraItem>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: OrdenCompraItem)

    @Update
    suspend fun update(item: OrdenCompraItem)

    @Query("DELETE FROM orden_compra_items WHERE ordenId = :ordenId AND ordenId IN (SELECT id FROM ordenes_compra WHERE opticaId = :opticaId)")
    suspend fun deleteByOrden(ordenId: String, opticaId: String)
}
