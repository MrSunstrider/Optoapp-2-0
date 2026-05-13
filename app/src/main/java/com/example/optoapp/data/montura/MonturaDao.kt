package com.example.optoapp.data.montura

import androidx.room.*
import com.example.optoapp.data.Montura
import kotlinx.coroutines.flow.Flow

@Dao
interface MonturaDao {
    @Query("SELECT * FROM monturas WHERE opticaId = :opticaId ORDER BY activo DESC, marca ASC, modelo ASC")
    fun getMonturasByOptica(opticaId: String): Flow<List<Montura>>

    @Query("SELECT * FROM monturas WHERE id = :id")
    suspend fun getMonturaById(id: String): Montura?

    @Query("UPDATE monturas SET stockActual = stockActual + :delta WHERE id = :monturaId AND opticaId = :opticaId AND (stockActual + :delta) >= 0")
    suspend fun adjustStock(monturaId: String, opticaId: String, delta: Int): Int

    @Upsert
    suspend fun insertMontura(montura: Montura)

    @Update
    suspend fun updateMontura(montura: Montura)

    @Delete
    suspend fun deleteMontura(montura: Montura)

    @Query("SELECT * FROM monturas WHERE opticaId = :opticaId")
    suspend fun getMonturasListByOptica(opticaId: String): List<Montura>
}
