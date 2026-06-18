package com.example.optoapp.data.proveedor

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.optoapp.data.MonturaProveedor
import kotlinx.coroutines.flow.Flow

@Dao
interface MonturaProveedorDao {
    @Query("SELECT * FROM montura_proveedor WHERE monturaId = :monturaId AND activo = 1 ORDER BY costoProveedor ASC")
    fun getByMontura(monturaId: String): Flow<List<MonturaProveedor>>

    @Query("SELECT * FROM montura_proveedor WHERE proveedorId = :proveedorId")
    suspend fun getByProveedor(proveedorId: String): List<MonturaProveedor>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(mp: MonturaProveedor)

    @Update
    suspend fun update(mp: MonturaProveedor)
}
