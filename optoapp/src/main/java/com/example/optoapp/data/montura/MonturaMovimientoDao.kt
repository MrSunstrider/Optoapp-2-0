package com.example.optoapp.data.montura

import androidx.room.*
import com.example.optoapp.data.MonturaMovimiento
import kotlinx.coroutines.flow.Flow

@Dao
interface MonturaMovimientoDao {
    @Query("SELECT * FROM montura_movimientos WHERE opticaId = :opticaId ORDER BY fecha DESC")
    fun getMovimientosByOptica(opticaId: String): Flow<List<MonturaMovimiento>>

    @Query("SELECT * FROM montura_movimientos WHERE monturaId = :monturaId ORDER BY fecha DESC")
    fun getMovimientosByMontura(monturaId: String): Flow<List<MonturaMovimiento>>

    @Upsert
    suspend fun insertMovimiento(movimiento: MonturaMovimiento)

    @Query("SELECT * FROM montura_movimientos WHERE opticaId = :opticaId")
    suspend fun getMovimientosListByOptica(opticaId: String): List<MonturaMovimiento>
}
