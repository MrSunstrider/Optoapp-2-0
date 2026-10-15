package com.example.optoapp.data.costobiselado

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface CostoBiseladoDao {
    // alto_indice is free text on both sides, so it is matched after normalization by the caller, not in SQL.
    @Query(
        """
        SELECT * FROM costos_biselado
        WHERE optica_id = :opticaId
          AND material = :material
          AND tipo_aro = :tipoAro
          AND stock_o_fabricacion = :stockOFabricacion
          AND serie IS :serie
          AND vigente_desde <= :fecha
          AND (vigente_hasta IS NULL OR vigente_hasta >= :fecha)
        ORDER BY vigente_desde DESC
    """,
    )
    suspend fun lookupVigente(
        opticaId: String,
        material: String,
        tipoAro: String,
        stockOFabricacion: String,
        serie: Int?,
        fecha: String,
    ): List<CostoBiseladoEntity>

    @Query(
        """
        SELECT * FROM costos_biselado
        WHERE optica_id = :opticaId
          AND vigente_hasta IS NULL
        ORDER BY material, tipo_aro
    """,
    )
    suspend fun getByOpticaIdList(opticaId: String): List<CostoBiseladoEntity>

    @Upsert
    suspend fun upsertAll(entities: List<CostoBiseladoEntity>)
}
