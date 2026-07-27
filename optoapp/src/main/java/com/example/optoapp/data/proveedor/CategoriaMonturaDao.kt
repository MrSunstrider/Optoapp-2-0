package com.example.optoapp.data.proveedor

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.optoapp.data.CategoriaMontura
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoriaMonturaDao {
    @Query("SELECT * FROM categorias_montura WHERE opticaId = :opticaId ORDER BY nombre ASC")
    fun getByOptica(opticaId: String): Flow<List<CategoriaMontura>>

    @Query("SELECT * FROM categorias_montura WHERE opticaId = :opticaId")
    suspend fun getListByOptica(opticaId: String): List<CategoriaMontura>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(categoria: CategoriaMontura)

    @Query(
        """
        UPDATE categorias_montura SET nombre=:nombre, descripcion=:descripcion,
        opticaId=:opticaId WHERE id=:id AND opticaId=:opticaId
    """,
    )
    suspend fun update(id: String, opticaId: String, nombre: String, descripcion: String): Int

    @Query("DELETE FROM categorias_montura WHERE id = :id AND opticaId = :opticaId")
    suspend fun delete(id: String, opticaId: String)
}
