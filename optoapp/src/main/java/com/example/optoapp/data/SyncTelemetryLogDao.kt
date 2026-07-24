package com.example.optoapp.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SyncTelemetryLogDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: SyncTelemetryLogEntity)

    @Query("SELECT * FROM sync_telemetry_log WHERE opticaId = :opticaId ORDER BY createdAt DESC")
    fun observeByOpticaId(opticaId: String): Flow<List<SyncTelemetryLogEntity>>
}
