package com.example.optoapp.data.ordencompra

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.optoapp.data.OrdenCompra
import kotlinx.coroutines.flow.Flow

@Dao
interface OrdenCompraDao {
    @Query("SELECT * FROM ordenes_compra WHERE opticaId = :opticaId ORDER BY fecha DESC")
    fun getByOptica(opticaId: String): Flow<List<OrdenCompra>>

    @Query("SELECT * FROM ordenes_compra WHERE opticaId = :opticaId")
    suspend fun getListByOptica(opticaId: String): List<OrdenCompra>

    @Query("SELECT * FROM ordenes_compra WHERE id = :id")
    suspend fun getById(id: String): OrdenCompra?

    @Query("SELECT MAX(numero) FROM ordenes_compra WHERE opticaId = :opticaId AND numero LIKE :prefix")
    suspend fun getLastNumero(opticaId: String, prefix: String): String?

    @Insert
    suspend fun insert(oc: OrdenCompra)

    @Update
    suspend fun update(oc: OrdenCompra)
}
