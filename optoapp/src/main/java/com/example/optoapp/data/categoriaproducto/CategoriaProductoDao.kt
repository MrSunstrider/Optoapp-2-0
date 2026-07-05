package com.example.optoapp.data.categoriaproducto

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoriaProductoDao {
    @Query("SELECT * FROM categorias_producto ORDER BY orden ASC")
    fun getAll(): Flow<List<CategoriaProductoEntity>>

    @Query("SELECT * FROM categorias_producto WHERE familia = :familia ORDER BY orden ASC")
    fun getByFamilia(familia: String): Flow<List<CategoriaProductoEntity>>

    @Query("SELECT * FROM categorias_producto WHERE id = :id")
    suspend fun getById(id: String): CategoriaProductoEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(categorias: List<CategoriaProductoEntity>)
}
