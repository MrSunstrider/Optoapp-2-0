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

    @Query("""
        SELECT * FROM monturas
        WHERE opticaId = :opticaId
          AND activo = 1
          AND (:marca IS NULL OR marca LIKE '%' || :marca || '%')
          AND (:material IS NULL OR materialMontura LIKE '%' || :material || '%')
          AND (:categoria IS NULL OR categoria = :categoria)
          AND (:precioMin IS NULL OR precio >= :precioMin)
          AND (:precioMax IS NULL OR precio <= :precioMax)
          AND (CASE WHEN :stockBajo = 0 THEN 1
                    WHEN :stockBajo = 1 AND stockActual <= stockMinimo THEN 1
                    ELSE 0 END)
        ORDER BY marca ASC, modelo ASC
    """)
    suspend fun searchMonturas(
        opticaId: String,
        marca: String? = null,
        material: String? = null,
        categoria: String? = null,
        precioMin: Double? = null,
        precioMax: Double? = null,
        stockBajo: Int = 0
    ): List<Montura>
}
