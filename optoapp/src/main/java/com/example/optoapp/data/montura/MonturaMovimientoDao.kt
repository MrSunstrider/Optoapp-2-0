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

    @Query("DELETE FROM montura_movimientos WHERE id = :id AND opticaId = :opticaId")
    suspend fun deleteMovimiento(id: String, opticaId: String)

    @Query("SELECT * FROM montura_movimientos WHERE opticaId = :opticaId")
    suspend fun getMovimientosListByOptica(opticaId: String): List<MonturaMovimiento>

    @Query("SELECT MAX(fecha) FROM montura_movimientos WHERE opticaId = :opticaId")
    fun getLastMovementDate(opticaId: String): Flow<java.time.LocalDate?>

    @Query("SELECT * FROM montura_movimientos WHERE id = :id AND opticaId = :opticaId")
    suspend fun getMovimientoById(id: String, opticaId: String): MonturaMovimiento?

    @Query("SELECT * FROM montura_movimientos WHERE opticaId = :opticaId AND fecha >= :since")
    suspend fun getMovimientosDesde(opticaId: String, since: java.time.LocalDate): List<MonturaMovimiento>
}
