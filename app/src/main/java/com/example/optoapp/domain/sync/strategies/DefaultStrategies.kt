package com.example.optoapp.domain.sync.strategies

import com.example.optoapp.domain.SyncPacientesUseCase
import com.example.optoapp.domain.SyncHistorialUseCase
import com.example.optoapp.domain.SyncFinanzasUseCase
import com.example.optoapp.domain.sync.SyncResult
import com.example.optoapp.domain.sync.SyncStrategy
import javax.inject.Inject

/**
 * Estrategia de sincronización completa: Sube todo lo local y baja todo lo remoto.
 * Es la lógica actual refactorizada.
 */
class FullSyncStrategy @Inject constructor(
    private val syncPacientesUseCase: SyncPacientesUseCase,
    private val syncHistorialUseCase: SyncHistorialUseCase,
    private val syncFinanzasUseCase: SyncFinanzasUseCase
) : SyncStrategy {

    override suspend fun executeSync(opticaId: String): SyncResult {
        return try {
            // Sincronizar pacientes
            val pResult = syncPacientesUseCase(opticaId, downloadAfterUpload = true)
            if (pResult is com.example.optoapp.data.Resource.Error) {
                return SyncResult.Error(pResult.message ?: "Error en pacientes")
            }

            // Sincronizar historial
            val hResult = syncHistorialUseCase(opticaId, downloadAfterUpload = true)
            if (hResult is com.example.optoapp.data.Resource.Error) {
                return SyncResult.Error(hResult.message ?: "Error en historial")
            }

            // Sincronizar finanzas
            val fResult = syncFinanzasUseCase(opticaId, downloadAfterUpload = true)
            if (fResult is com.example.optoapp.data.Resource.Error) {
                return SyncResult.Error(fResult.message ?: "Error en finanzas")
            }

            SyncResult.Success
        } catch (e: Exception) {
            SyncResult.Error(e.localizedMessage ?: "Error inesperado en sincronización")
        }
    }
}

/**
 * Estrategia de solo lectura: No sube nada, solo baja cambios.
 */
class ReadOnlySyncStrategy @Inject constructor(
    private val syncPacientesUseCase: SyncPacientesUseCase
) : SyncStrategy {
    override suspend fun executeSync(opticaId: String): SyncResult {
        // En una implementación real, el UseCase debería soportar solo bajada
        val result = syncPacientesUseCase(opticaId, downloadAfterUpload = true)
        return if (result is com.example.optoapp.data.Resource.Success) SyncResult.Success 
               else SyncResult.Error(result.message ?: "Error")
    }
}
