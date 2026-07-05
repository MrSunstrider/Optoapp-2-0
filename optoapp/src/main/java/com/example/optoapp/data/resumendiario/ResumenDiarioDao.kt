package com.example.optoapp.data.resumendiario

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface ResumenDiarioDao {
    @Query("SELECT * FROM resumen_diario WHERE opticaId = :opticaId ORDER BY fecha DESC")
    fun getByOpticaId(opticaId: String): Flow<List<ResumenDiarioEntity>>

    @Upsert
    suspend fun upsert(resumen: ResumenDiarioEntity)

    @Query("DELETE FROM resumen_diario")
    suspend fun deleteAll()
}
