package com.example.optoapp.domain.sync

import android.util.Log
import com.example.optoapp.data.ConflictDao
import com.example.optoapp.data.SyncStateTracker
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.Serializable
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Helper para detectar conflictos entre Room local y Supabase remoto
 * antes de hacer upsert, usando el campo updated_at como referencia.
 *
 * Cada UseCase de sync lo invoca antes del upload masivo.
 */
@Singleton
class ConflictHelper @Inject constructor(
    private val supabase: SupabaseClient,
    private val syncStateTracker: SyncStateTracker,
    private val conflictDao: ConflictDao
) {
    companion object {
        private const val TAG = "ConflictHelper"
    }

    /**
     * Filtra una lista de entidades locales, separando las seguras para upsert
     * de las que están en conflicto. Las entidades en conflicto se registran
     * en [ConflictRecord] y en [SyncStateTracker].
     *
     * @param tableName nombre de la tabla en Supabase (snake_case)
     * @param opticaId óptica activa
     * @param entityType tipo de entidad para SyncStateTracker
     * @param localEntities lista de entidades locales con id y updatedAt
     * @return lista de entidades SEGURAS para upsert (sin conflicto)
     */
    suspend fun filterConflicts(
        tableName: String,
        opticaId: String,
        entityType: String,
        localEntities: List<LocalEntity>
    ): List<LocalEntity> {
        if (localEntities.isEmpty()) return localEntities

        // IDs con updatedAt local no nulo — solo esos podemos verificar
        val checkable = localEntities.filter { it.updatedAt != null }
        if (checkable.isEmpty()) return localEntities

        val checkableIds = checkable.map { it.id }
        val remoteTimestamps = fetchRemoteUpdatedAt(tableName, opticaId, checkableIds)

        val safe = mutableListOf<LocalEntity>()
        for (entity in localEntities) {
            val remoteUpdatedAt = remoteTimestamps[entity.id]
            if (entity.updatedAt == null || remoteUpdatedAt == null) {
                // No hay data remota o local para comparar — subir sin problema
                safe.add(entity)
                continue
            }

            if (entity.updatedAt >= remoteUpdatedAt) {
                // Local es más nuevo o igual → seguro
                safe.add(entity)
            } else {
                // Remoto es más nuevo → CONFLICTO
                Log.w(TAG, "Conflicto en $entityType/${entity.id}: local=${entity.updatedAt} < remoto=$remoteUpdatedAt")
                conflictDao.upsertConflict(
                    entityId = entity.id,
                    opticaId = opticaId,
                    entityType = entityType,
                    localSnapshot = entity.updatedAt,      // placeholder: idealmente sería el JSON completo
                    remoteSnapshot = remoteUpdatedAt        // placeholder
                )
                syncStateTracker.markConflicted(opticaId, entityType, entity.id)
            }
        }

        val conflictedCount = localEntities.size - safe.size
        if (conflictedCount > 0) {
            Log.w(TAG, "$conflictedCount entidades $entityType en conflicto, se omiten del upload")
        }
        return safe
    }

    /**
     * Obtiene el mapa id → updated_at desde Supabase para una lista de IDs.
     */
    private suspend fun fetchRemoteUpdatedAt(
        tableName: String,
        opticaId: String,
        ids: List<String>
    ): Map<String, String> {
        if (ids.isEmpty()) return emptyMap()
        return try {
            // Traemos TODAS las filas de la óptica y filtramos en memoria
            // (alternativa: chunk + raw SQL RPC si el volumen es muy grande)
            val allRows = supabase.postgrest[tableName]
                .select {
                    filter { eq("optica_id", opticaId) }
                }
                .decodeList<RemoteTimestamp>()
            val idSet = ids.toSet()
            allRows
                .mapNotNull { row ->
                    if (row.id !in idSet) return@mapNotNull null
                    row.updatedAt?.let { ts -> row.id to ts }
                }
                .toMap()
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching remote timestamps from $tableName: ${e.message}")
            emptyMap()
        }
    }

    @Serializable
    private data class RemoteTimestamp(
        val id: String,
        @kotlinx.serialization.SerialName("updated_at")
        val updatedAt: String? = null
    )
}

/**
 * Representa una entidad local con su id y updatedAt para comparación con remoto.
 */
data class LocalEntity(
    val id: String,
    val updatedAt: String? = null
)
