package com.example.optoapp.sync

import android.util.Log
import com.example.optoapp.data.Resource
import com.example.optoapp.di.ApplicationScope
import com.example.optoapp.domain.SyncFinanzasUseCase
import com.example.optoapp.domain.SyncHistorialUseCase
import com.example.optoapp.domain.SyncPacientesUseCase
import com.example.optoapp.domain.SyncSessionHelper
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
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
    private val supabase: SupabaseClient,
    private val syncPacientesUseCase: SyncPacientesUseCase,
    private val syncHistorialUseCase: SyncHistorialUseCase,
    private val syncFinanzasUseCase: SyncFinanzasUseCase
) {

    fun schedulePacientesSync(opticaId: String) {
        applicationScope.launch {
            if (!ensureSessionForPostSaveSync("pacientes")) return@launch
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
            if (!ensureSessionForPostSaveSync("historial")) return@launch
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
            if (!ensureSessionForPostSaveSync("finanzas")) return@launch
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

    private suspend fun ensureSessionForPostSaveSync(stage: String): Boolean {
        return runCatching {
            SyncSessionHelper.refreshSessionBeforeSync(supabase)
            val currentUser = supabase.auth.currentUserOrNull()
            if (currentUser == null) {
                Log.w(TAG, "Sync $stage post-guardado cancelada: sesión Supabase no activa")
                false
            } else {
                true
            }
        }.getOrElse { e ->
            Log.w(TAG, "Sync $stage post-guardado cancelada: no se pudo validar sesión (${e.localizedMessage})")
            false
        }
    }

    companion object {
        private const val TAG = "PostSaveSync"
    }
}
