package com.example.optoapp.sync

import android.util.Log
import com.example.optoapp.data.Resource
import com.example.optoapp.di.ApplicationScope
import com.example.optoapp.domain.SyncFinanzasUseCase
import com.example.optoapp.domain.SyncHistorialUseCase
import com.example.optoapp.domain.SyncPacientesUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Ejecuta sync tras guardar en Room en un [CoroutineScope] de aplicación (no se cancela al salir de la pantalla).
 * Usa el mismo [SyncGate] que [com.example.optoapp.viewmodel.SyncViewModel] para no solapar upserts.
 */
@Singleton
class PostSaveSyncScheduler @Inject constructor(
    @ApplicationScope private val applicationScope: CoroutineScope,
    private val syncGate: SyncGate,
    private val syncPacientesUseCase: SyncPacientesUseCase,
    private val syncHistorialUseCase: SyncHistorialUseCase,
    private val syncFinanzasUseCase: SyncFinanzasUseCase
) {

    fun schedulePacientesSync(opticaId: String) {
        applicationScope.launch {
            try {
                syncGate.mutex.withLock {
                    when (val r = syncPacientesUseCase(opticaId)) {
                        is Resource.Error -> Log.w(TAG, "Sync pacientes post-guardado: ${r.message}")
                        else -> Log.d(TAG, "Sync pacientes post-guardado OK")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Sync pacientes post-guardado", e)
            }
        }
    }

    fun scheduleHistorialSync(opticaId: String) {
        applicationScope.launch {
            try {
                syncGate.mutex.withLock {
                    when (val r = syncHistorialUseCase(opticaId)) {
                        is Resource.Error -> Log.w(TAG, "Sync historial post-guardado: ${r.message}")
                        else -> Log.d(TAG, "Sync historial post-guardado OK")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Sync historial post-guardado", e)
            }
        }
    }

    fun scheduleFinanzasSync(opticaId: String) {
        applicationScope.launch {
            try {
                syncGate.mutex.withLock {
                    when (val r = syncFinanzasUseCase(opticaId)) {
                        is Resource.Error -> Log.w(TAG, "Sync finanzas post-guardado: ${r.message}")
                        else -> Log.d(TAG, "Sync finanzas post-guardado OK")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Sync finanzas post-guardado", e)
            }
        }
    }

    companion object {
        private const val TAG = "PostSaveSync"
    }
}
