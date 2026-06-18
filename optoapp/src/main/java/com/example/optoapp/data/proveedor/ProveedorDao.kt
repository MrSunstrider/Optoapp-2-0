package com.example.optoapp.data.proveedor

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.optoapp.data.Proveedor
import kotlinx.coroutines.flow.Flow

@Dao
interface ProveedorDao {
    @Query("SELECT * FROM proveedores WHERE opticaId = :opticaId AND activo = 1 ORDER BY nombre ASC")
    fun getActivosByOptica(opticaId: String): Flow<List<Proveedor>>

    @Query("SELECT * FROM proveedores WHERE opticaId = :opticaId")
    suspend fun getListByOptica(opticaId: String): List<Proveedor>

    @Query("SELECT * FROM proveedores WHERE id = :id")
    suspend fun getById(id: String): Proveedor?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(proveedor: Proveedor)

    @Update
    suspend fun update(proveedor: Proveedor)
}
