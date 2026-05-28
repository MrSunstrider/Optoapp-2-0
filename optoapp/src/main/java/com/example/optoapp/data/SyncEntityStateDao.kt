package com.example.optoapp.data

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface SyncEntityStateDao {
    @Upsert
    suspend fun upsert(row: SyncEntityState)

    @Query(
        """
        SELECT * FROM sync_entity_state
        WHERE opticaId = :opticaId AND status = 'error'
        ORDER BY updatedAt DESC
        LIMIT 200
        """
    )
    fun observeErrorsForOptica(opticaId: String): Flow<List<SyncEntityState>>

    @Query("SELECT COUNT(*) FROM sync_entity_state WHERE opticaId = :opticaId AND status = 'error'")
    fun countErrorsForOptica(opticaId: String): Flow<Int>

    @Query("DELETE FROM sync_entity_state WHERE opticaId = :opticaId AND status = 'error'")
    suspend fun deleteErrorsForOptica(opticaId: String)

    @Query("SELECT * FROM sync_entity_state WHERE opticaId = :opticaId AND status = 'deleted'")
    suspend fun getPendingDeletions(opticaId: String): List<SyncEntityState>

    @Query("SELECT * FROM sync_entity_state WHERE opticaId = :opticaId AND status = 'pending' ORDER BY updatedAt ASC")
    fun getPendingForOptica(opticaId: String): Flow<List<SyncEntityState>>

    @Query("DELETE FROM sync_entity_state WHERE opticaId = :opticaId AND entityType = :type AND entityId = :id")
    suspend fun clearEntityState(opticaId: String, type: String, id: String)
}
