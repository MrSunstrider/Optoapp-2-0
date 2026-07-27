package com.example.optoapp.data.gastooperativo

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface GastoOperativoDao {
    @Query("SELECT * FROM gastos_operativos WHERE opticaId = :opticaId ORDER BY fecha DESC")
    fun getByOpticaId(opticaId: String): Flow<List<GastoOperativoEntity>>

    @Query("SELECT * FROM gastos_operativos WHERE opticaId = :opticaId AND fecha >= :start AND fecha <= :end ORDER BY fecha DESC")
    fun getByOpticaIdAndDateRange(
        opticaId: String,
        start: LocalDate,
        end: LocalDate,
    ): Flow<List<GastoOperativoEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(gasto: GastoOperativoEntity)

    @Upsert
    suspend fun upsert(gasto: GastoOperativoEntity)

    @Query("SELECT * FROM gastos_operativos WHERE opticaId = :opticaId ORDER BY fecha DESC")
    suspend fun getByOpticaIdList(opticaId: String): List<GastoOperativoEntity>

    @Query("DELETE FROM gastos_operativos WHERE id = :id AND opticaId = :opticaId")
    suspend fun delete(id: String, opticaId: String)
}
