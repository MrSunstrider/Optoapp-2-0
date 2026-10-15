package com.example.optoapp.data.costolc

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface CostoLcDao {

    @Query(
        """
        SELECT * FROM costos_lc
        WHERE optica_id = :opticaId
          AND tipo_lc = :tipoLc
          AND material_lc = :materialLc
          AND modalidad = :modalidad
          AND ((laboratorio_id IS NULL AND :laboratorioId IS NULL) OR laboratorio_id = :laboratorioId)
          AND vigente_hasta IS NULL
        LIMIT 1
    """,
    )
    suspend fun lookup(
        opticaId: String,
        tipoLc: String,
        materialLc: String,
        modalidad: String,
        laboratorioId: String?,
    ): CostoLcEntity?

    @Query(
        """
        SELECT * FROM costos_lc
        WHERE optica_id = :opticaId
          AND vigente_hasta IS NULL
        ORDER BY tipo_lc, material_lc
    """,
    )
    fun getByOpticaId(opticaId: String): Flow<List<CostoLcEntity>>

    @Query(
        """
        SELECT * FROM costos_lc
        WHERE optica_id = :opticaId
          AND vigente_hasta IS NULL
        ORDER BY tipo_lc, material_lc
    """,
    )
    suspend fun getByOpticaIdList(opticaId: String): List<CostoLcEntity>

    // Closed versions are included because closing a row is itself an edit the server must receive.
    @Query("SELECT * FROM costos_lc WHERE optica_id = :opticaId")
    suspend fun getAllByOpticaIdList(opticaId: String): List<CostoLcEntity>

    @Query("SELECT * FROM costos_lc WHERE id = :id")
    suspend fun getById(id: String): CostoLcEntity?

    @Upsert
    suspend fun upsertAll(entities: List<CostoLcEntity>)
}
