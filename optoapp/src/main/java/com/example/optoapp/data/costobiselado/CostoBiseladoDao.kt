package com.example.optoapp.data.costobiselado

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface CostoBiseladoDao {
    @Query(
        """
        SELECT * FROM costos_biselado
        WHERE material = :material
          AND tipo_aro = :tipoAro
          AND stock_o_fabricacion = :stockOFabricacion
          AND (serie IS NULL AND :serie IS NULL OR serie = :serie)
          AND (alto_indice IS NULL AND :altoIndice IS NULL OR alto_indice = :altoIndice)
          AND vigente_hasta IS NULL
        LIMIT 1
    """,
    )
    suspend fun lookup(
        material: String,
        tipoAro: String,
        stockOFabricacion: String,
        serie: Int?,
        altoIndice: String?,
    ): CostoBiseladoEntity?

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
