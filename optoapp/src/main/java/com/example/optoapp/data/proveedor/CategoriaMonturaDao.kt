package com.example.optoapp.data.proveedor

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.optoapp.data.CategoriaMontura
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoriaMonturaDao {
    @Query("SELECT * FROM categorias_montura WHERE opticaId = :opticaId ORDER BY nombre ASC")
    fun getByOptica(opticaId: String): Flow<List<CategoriaMontura>>

    @Query("SELECT * FROM categorias_montura WHERE opticaId = :opticaId")
    suspend fun getListByOptica(opticaId: String): List<CategoriaMontura>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(categoria: CategoriaMontura)

    @Update
    suspend fun update(categoria: CategoriaMontura)

    @Query("DELETE FROM categorias_montura WHERE id = :id AND opticaId = :opticaId")
    suspend fun delete(id: String, opticaId: String)
}
