package com.example.optoapp.data

import com.example.optoapp.data.montura.MonturaDao
import com.example.optoapp.data.montura.MonturaMovimientoDao

/**
 * Repositorio especializado en operaciones de estado de sincronización (sync state)
 * y snapshots para subida remota (Supabase).
 * Incluye tombstones de entidades eliminadas pendientes de propagación.
 * Extraído de [OptoRepository] para reducir el God class.
 */
class SyncRepository(
    private val syncStateTracker: SyncStateTracker,
    private val monturaDao: MonturaDao,
    private val monturaMovimientoDao: MonturaMovimientoDao,
) {
    /** Retorna todos los registros marcados como eliminados pendientes de sync remoto. */
    suspend fun getPendingDeletions(opticaId: String): List<SyncEntityState> = syncStateTracker.dao.getPendingDeletions(opticaId)

    /** Limpia el tombstone después de que la eliminación fue confirmada en Supabase. */
    suspend fun clearDeletionState(opticaId: String, type: String, id: String) = syncStateTracker.dao.clearEntityState(opticaId, type, id)

    /** Snapshot de monturas de la óptica activa (sync inventario). */
    suspend fun getMonturasSnapshotForOptica(opticaId: String): List<Montura> = monturaDao.getMonturasListByOptica(opticaId)

    /** Snapshot de movimientos de montura de la óptica activa (sync inventario). */
    suspend fun getMovimientosMonturaSnapshotForOptica(opticaId: String): List<MonturaMovimiento> = monturaMovimientoDao.getMovimientosListByOptica(opticaId)
}
