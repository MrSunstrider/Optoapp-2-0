package com.example.optoapp.data.montura

import androidx.room.*
import com.example.optoapp.data.MonturaMovimiento
import kotlinx.coroutines.flow.Flow

@Dao
interface MonturaMovimientoDao {
    @Query("SELECT * FROM montura_movimientos WHERE opticaId = :opticaId ORDER BY fecha DESC")
    fun getMovimientosByOptica(opticaId: String): Flow<List<MonturaMovimiento>>

    @Query("SELECT * FROM montura_movimientos WHERE monturaId = :monturaId AND opticaId = :opticaId ORDER BY fecha DESC")
    fun getMovimientosByMontura(monturaId: String, opticaId: String): Flow<List<MonturaMovimiento>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMovimiento(movimiento: MonturaMovimiento)

    /** Returns -1 when a row with the same id or (referenciaId, tipo, monturaId) already exists. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertMovimientoIfAbsent(movimiento: MonturaMovimiento): Long

    @Query(
        """
        SELECT * FROM montura_movimientos
        WHERE referenciaId = :referenciaId AND tipo = :tipo AND monturaId = :monturaId
        """,
    )
    suspend fun findByKey(referenciaId: String, tipo: String, monturaId: String): MonturaMovimiento?

    @Query("DELETE FROM montura_movimientos WHERE id = :id AND opticaId = :opticaId")
    suspend fun deleteMovimiento(id: String, opticaId: String)

    @Query("SELECT * FROM montura_movimientos WHERE opticaId = :opticaId")
    suspend fun getMovimientosListByOptica(opticaId: String): List<MonturaMovimiento>

    @Query("SELECT MAX(fecha) FROM montura_movimientos WHERE opticaId = :opticaId")
    fun getLastMovementDate(opticaId: String): Flow<java.time.LocalDate?>

    @Query("SELECT * FROM montura_movimientos WHERE id = :id AND opticaId = :opticaId")
    suspend fun getMovimientoById(id: String, opticaId: String): MonturaMovimiento?

    @Query(
        """
        SELECT COUNT(*) FROM montura_movimientos
        WHERE opticaId = :opticaId AND (
          referenciaId = :dispensacionId
          OR substr(referenciaId, 1, length(:dispensacionId) + 1) = :dispensacionId || ':'
          OR referenciaId IN (:regaloIds)
        )
        """,
    )
    suspend fun countForDispensacion(dispensacionId: String, regaloIds: List<String>, opticaId: String): Int

    @Query(
        """
        SELECT * FROM montura_movimientos
        WHERE opticaId = :opticaId AND (
          referenciaId = :dispensacionId
          OR substr(referenciaId, 1, length(:dispensacionId) + 1) = :dispensacionId || ':'
        )
        """,
    )
    suspend fun getMovimientosForDispensacion(dispensacionId: String, opticaId: String): List<MonturaMovimiento>

    @Query("SELECT * FROM montura_movimientos WHERE opticaId = :opticaId AND fecha >= :since")
    suspend fun getMovimientosDesde(opticaId: String, since: java.time.LocalDate): List<MonturaMovimiento>
}
