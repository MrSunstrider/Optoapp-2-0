package com.example.optoapp.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query

/**
 * Registro de un conflicto de sincronización detectado entre Room local y Supabase remoto.
 *
 * Cuando [SyncStateTracker] detecta que una entidad local tiene un [updatedAt]
 * más viejo que su contraparte remota (o viceversa), se guarda un [ConflictRecord]
 * con los snapshots de ambas versiones para que el usuario resuelva manualmente.
 */
@Entity(tableName = "conflict_records")
data class ConflictRecord(
    @PrimaryKey val entityId: String,
    val opticaId: String,
    val entityType: String,
    val localSnapshot: String,
    val remoteSnapshot: String,
    val detectedAt: Long = System.currentTimeMillis()
)

@Dao
interface ConflictDao {
    @Query("SELECT * FROM conflict_records WHERE opticaId = :opticaId ORDER BY detectedAt DESC")
    suspend fun getConflicts(opticaId: String): List<ConflictRecord>

    @Query("SELECT * FROM conflict_records WHERE entityType = :entityType AND opticaId = :opticaId ORDER BY detectedAt DESC")
    suspend fun getConflictsByType(opticaId: String, entityType: String): List<ConflictRecord>

    @Query("SELECT COUNT(*) FROM conflict_records WHERE opticaId = :opticaId")
    suspend fun countConflicts(opticaId: String): Int

    @Query("""
        INSERT OR REPLACE INTO conflict_records (entityId, opticaId, entityType, localSnapshot, remoteSnapshot, detectedAt)
        VALUES (:entityId, :opticaId, :entityType, :localSnapshot, :remoteSnapshot, :detectedAt)
    """)
    suspend fun upsertConflict(
        entityId: String,
        opticaId: String,
        entityType: String,
        localSnapshot: String,
        remoteSnapshot: String,
        detectedAt: Long = System.currentTimeMillis()
    )

    @Query("DELETE FROM conflict_records WHERE entityId = :entityId AND opticaId = :opticaId")
    suspend fun resolveConflict(entityId: String, opticaId: String)

    @Query("DELETE FROM conflict_records WHERE opticaId = :opticaId")
    suspend fun clearConflicts(opticaId: String)

    @Query("SELECT entityId FROM conflict_records WHERE opticaId = :opticaId AND entityType = :entityType")
    suspend fun getConflictEntityIds(opticaId: String, entityType: String): List<String>
}
