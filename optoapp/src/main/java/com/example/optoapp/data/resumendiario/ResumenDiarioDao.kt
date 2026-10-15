package com.example.optoapp.data.resumendiario

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface ResumenDiarioDao {
    @Query("SELECT * FROM resumen_diario WHERE opticaId = :opticaId ORDER BY fecha DESC")
    fun getByOpticaId(opticaId: String): Flow<List<ResumenDiarioEntity>>

    @Query(
        """
        SELECT * FROM resumen_diario
        WHERE opticaId = :opticaId AND fecha >= :desde AND fecha <= :hasta
        ORDER BY fecha ASC
    """,
    )
    suspend fun getByOpticaBetween(opticaId: String, desde: String, hasta: String): List<ResumenDiarioEntity>

    @Query(
        """
        SELECT * FROM resumen_diario
        WHERE opticaId = :opticaId AND fecha = :fecha
        LIMIT 1
    """,
    )
    suspend fun getByOpticaAndDate(opticaId: String, fecha: String): ResumenDiarioEntity?

    @Upsert
    suspend fun upsert(resumen: ResumenDiarioEntity)

    @Query("DELETE FROM resumen_diario")
    suspend fun deleteAll()
}
