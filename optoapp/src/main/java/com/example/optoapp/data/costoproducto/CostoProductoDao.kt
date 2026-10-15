package com.example.optoapp.data.costoproducto

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface CostoProductoDao {
    // ISO yyyy-MM-dd strings compare chronologically; two open rows after a sync conflict resolve to the newest.
    @Query(
        """
        SELECT * FROM costos_productos
        WHERE optica_id = :opticaId
          AND material = :material
          AND tipo_lente = :tipoLente
          AND stock_o_fabricacion = :stockOFabricacion
          AND tratamiento IS :tratamiento
          AND serie IS :serie
          AND vigente_desde <= :fecha
          AND (vigente_hasta IS NULL OR vigente_hasta >= :fecha)
        ORDER BY vigente_desde DESC
        LIMIT 1
    """,
    )
    suspend fun lookupVigente(
        opticaId: String,
        material: String,
        tipoLente: String,
        stockOFabricacion: String,
        tratamiento: String?,
        serie: Int?,
        fecha: String,
    ): CostoProductoEntity?

    @Query(
        """
        SELECT * FROM costos_productos
        WHERE optica_id = :opticaId
          AND stock_o_fabricacion = :bloque
          AND vigente_hasta IS NULL
        ORDER BY material, tipo_lente
    """,
    )
    fun getByBloque(opticaId: String, bloque: String): Flow<List<CostoProductoEntity>>

    @Query(
        """
        SELECT * FROM costos_productos
        WHERE optica_id = :opticaId
          AND vigente_hasta IS NULL
        ORDER BY material, tipo_lente
    """,
    )
    suspend fun getByOpticaIdList(opticaId: String): List<CostoProductoEntity>

    @Upsert
    suspend fun upsertAll(entities: List<CostoProductoEntity>)

    @Query(
        """
        SELECT * FROM costos_productos
        WHERE optica_id = :opticaId
          AND material = :material
          AND tipo_lente = :tipoLente
          AND stock_o_fabricacion = :stockOFabricacion
          AND (laboratorio_id IS NULL OR laboratorio_id = :laboratorioId)
          AND vigente_hasta IS NULL
        LIMIT 1
    """,
    )
    suspend fun lookupLc(
        opticaId: String,
        material: String,
        tipoLente: String,
        stockOFabricacion: String,
        laboratorioId: String?,
    ): CostoProductoEntity?
}
