package com.example.optoapp.domain.sync

import com.example.optoapp.domain.sync.strategies.FullSyncStrategy
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncManager @Inject constructor(
    private val fullSyncStrategy: FullSyncStrategy
) {
    private var currentStrategy: SyncStrategy = fullSyncStrategy

    fun setStrategy(strategy: SyncStrategy) {
        currentStrategy = strategy
    }

    suspend fun performSync(opticaId: String): SyncResult {
        return currentStrategy.executeSync(opticaId)
    }

    /**
     * Ejemplo de cómo cambiar de estrategia según el contexto
     */
    fun switchToFullSync() {
        setStrategy(fullSyncStrategy)
    }
}
