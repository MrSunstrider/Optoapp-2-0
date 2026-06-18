package com.example.optoapp.data.ordencompra

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.optoapp.data.OrdenCompraItem

@Dao
interface OrdenCompraItemDao {
    @Query("SELECT * FROM orden_compra_items WHERE ordenId = :ordenId")
    suspend fun getByOrden(ordenId: String): List<OrdenCompraItem>

    @Query("SELECT * FROM orden_compra_items WHERE monturaId = :monturaId")
    suspend fun getByMontura(monturaId: String): List<OrdenCompraItem>

    @Insert
    suspend fun insert(item: OrdenCompraItem)

    @Update
    suspend fun update(item: OrdenCompraItem)

    @Query("DELETE FROM orden_compra_items WHERE ordenId = :ordenId")
    suspend fun deleteByOrden(ordenId: String)
}
