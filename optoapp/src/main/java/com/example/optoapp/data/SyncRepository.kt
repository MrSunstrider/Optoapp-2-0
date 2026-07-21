package com.example.optoapp.data

import com.example.optoapp.data.montura.MonturaDao
import com.example.optoapp.data.montura.MonturaMovimientoDao

class SyncRepository(
    private val syncStateTracker: SyncStateTracker,
    private val monturaDao: MonturaDao,
    private val monturaMovimientoDao: MonturaMovimientoDao,
) {
    suspend fun getPendingDeletions(opticaId: String): List<SyncEntityState> = syncStateTracker.dao.getPendingDeletions(opticaId)

    suspend fun clearDeletionState(opticaId: String, type: String, id: String) = syncStateTracker.dao.clearEntityState(opticaId, type, id)

    suspend fun getMonturasSnapshotForOptica(opticaId: String): List<Montura> = monturaDao.getMonturasListByOptica(opticaId)

    suspend fun getMovimientosMonturaSnapshotForOptica(opticaId: String): List<MonturaMovimiento> = monturaMovimientoDao.getMovimientosListByOptica(opticaId)
}
