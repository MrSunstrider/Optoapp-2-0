package com.example.optoapp.data.arqueo

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface ArqueoCajaDao {

    @Query("SELECT * FROM arqueo_caja WHERE fecha = :fecha AND opticaId = :opticaId LIMIT 1")
    fun getArqueoByFechaAndOptica(fecha: LocalDate, opticaId: String): Flow<ArqueoCaja?>

    @Query("SELECT * FROM arqueo_caja WHERE opticaId = :opticaId ORDER BY fecha DESC")
    fun getArqueosByOptica(opticaId: String): Flow<List<ArqueoCaja>>

    @Query("SELECT * FROM arqueo_caja WHERE opticaId = :opticaId ORDER BY fecha DESC")
    suspend fun getArqueosByOpticaList(opticaId: String): List<ArqueoCaja>

    @Query("SELECT * FROM arqueo_caja WHERE fecha = :fecha AND opticaId = :opticaId LIMIT 1")
    suspend fun getArqueoByFechaSync(fecha: LocalDate, opticaId: String): ArqueoCaja?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertArqueo(a: ArqueoCaja)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertArqueo(a: ArqueoCaja)

    @Update
    suspend fun updateArqueo(a: ArqueoCaja)

    @Query("DELETE FROM arqueo_caja")
    suspend fun deleteAll()
}
