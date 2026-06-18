package com.example.optoapp.sync

import android.util.Log
import androidx.annotation.VisibleForTesting
import com.example.optoapp.data.Resource
import kotlinx.coroutines.CancellationException
import java.io.IOException
import com.example.optoapp.di.ApplicationScope
import com.example.optoapp.domain.SyncFinanzasUseCase
import com.example.optoapp.domain.SyncProveedoresUseCase
import com.example.optoapp.util.BackgroundErrorCollector
import com.example.optoapp.domain.SyncHistorialUseCase
import com.example.optoapp.domain.SyncInventarioUseCase
import com.example.optoapp.domain.SyncPacientesUseCase
import com.example.optoapp.domain.SyncSessionHelper
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Uses ApplicationScope so sync survives screen teardown, and reuses SyncViewModel's
 * [SyncGate] to prevent overlapping upserts. [suppressSync] is set by performFullDownload
 * to avoid regenerating conflicts during bulk downloads.
 */
@Singleton
open class PostSaveSyncScheduler @Inject constructor(
    @ApplicationScope private val applicationScope: CoroutineScope,
    private val syncGate: SyncGate,
    private val supabase: SupabaseClient,
    private val syncPacientesUseCase: SyncPacientesUseCase? = null,
    private val syncHistorialUseCase: SyncHistorialUseCase? = null,
    private val syncFinanzasUseCase: SyncFinanzasUseCase? = null,
    private val syncInventarioUseCase: SyncInventarioUseCase? = null,
    private val syncProveedoresUseCase: SyncProveedoresUseCase? = null,
    private val bgErrorCollector: BackgroundErrorCollector? = null
) {
    private val scheduleMutex = Mutex()
    @VisibleForTesting
    internal val pendingJobs = mutableMapOf<String, Job>()

    /** Test hook — called before each sync attempt, after session validation. */
    @VisibleForTesting
    internal var onBeforeSync: (suspend (String) -> Unit)? = null

    /**
     * Cuando está en true, se saltea toda programación de sync.
     * Usado por SyncViewModel.performFullDownload() para evitar que la descarga
     * masiva dispare syncs post-guardado que regeneren conflictos.
     */
    @Volatile
    var suppressSync: Boolean = false

    /**
     * Cancela todos los syncs pendientes y aguarda su terminación antes de retornar.
     * Debe llamarse antes de [suppressSync] = true para evitar que jobs
     * programados antes del suppress se ejecuten después de la descarga.
     */
    suspend fun cancelPending() {
        scheduleMutex.withLock {
            for (job in pendingJobs.values) {
                job.cancelAndJoin()
            }
            pendingJobs.clear()
        }
    }

    @VisibleForTesting
    protected open fun scheduleDebounced(
        key: String,
        delayMs: Long = 800L,
        block: suspend () -> Unit
    ) {
        if (suppressSync) return
        applicationScope.launch {
            scheduleMutex.withLock {
                pendingJobs.remove(key)?.cancel()
                pendingJobs[key] = launch {
                    delay(delayMs)
                    block()
                }
            }
        }
    }

    open fun schedulePacientesSync(opticaId: String) {
        if (suppressSync) return
        scheduleDebounced(key = "pacientes:$opticaId") {
            onBeforeSync?.invoke("pacientes")
            try {
                syncGate.mutex.withLock {
                    if (!ensureSessionForPostSaveSync("pacientes")) return@withLock
                    when (val r = syncPacientesUseCase!!(opticaId)) {
                        is Resource.Error -> Log.w(TAG, "Sync pacientes post-guardado: ${r.message}")
                        else -> Log.d(TAG, "Sync pacientes post-guardado OK")
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: IOException) {
                Log.e(TAG, "Error en red sync pacientes post-guardado: ${e.message}", e)
            } catch (e: Exception) {
                Log.e(TAG, "Error inesperado sync pacientes post-guardado: ${e.message}", e)
            }
        }
    }

    open fun scheduleHistorialSync(opticaId: String) {
        if (suppressSync) return
        scheduleDebounced(key = "historial:$opticaId") {
            onBeforeSync?.invoke("historial")
            try {
                syncGate.mutex.withLock {
                    if (!ensureSessionForPostSaveSync("historial")) return@withLock
                    when (val r = syncHistorialUseCase!!(opticaId)) {
                        is Resource.Error -> Log.w(TAG, "Sync historial post-guardado: ${r.message}")
                        else -> Log.d(TAG, "Sync historial post-guardado OK")
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: IOException) {
                Log.e(TAG, "Error en red sync historial post-guardado: ${e.message}", e)
            } catch (e: Exception) {
                Log.e(TAG, "Error inesperado sync historial post-guardado: ${e.message}", e)
            }
        }
    }

    open fun scheduleFinanzasSync(opticaId: String) {
        if (suppressSync) return
        scheduleDebounced(key = "finanzas:$opticaId") {
            onBeforeSync?.invoke("finanzas")
            try {
                syncGate.mutex.withLock {
                    if (!ensureSessionForPostSaveSync("finanzas")) return@withLock
                    when (val r = syncFinanzasUseCase!!(opticaId)) {
                        is Resource.Error -> Log.w(TAG, "Sync finanzas post-guardado: ${r.message}")
                        else -> Log.d(TAG, "Sync finanzas post-guardado OK")
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: IOException) {
                Log.e(TAG, "Error en red sync finanzas post-guardado: ${e.message}", e)
            } catch (e: Exception) {
                Log.e(TAG, "Error inesperado sync finanzas post-guardado: ${e.message}", e)
            }
        }
    }

    open fun scheduleInventarioSync(opticaId: String) {
        if (suppressSync) return
        scheduleDebounced(key = "inventario:$opticaId") {
            onBeforeSync?.invoke("inventario")
            try {
                syncGate.mutex.withLock {
                    if (!ensureSessionForPostSaveSync("inventario")) return@withLock
                    when (val r = syncInventarioUseCase!!(opticaId)) {
                        is Resource.Error -> Log.w(TAG, "Sync inventario post-guardado: ${r.message}")
                        else -> Log.d(TAG, "Sync inventario post-guardado OK")
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: IOException) {
                Log.e(TAG, "Error en red sync inventario post-guardado: ${e.message}", e)
            } catch (e: Exception) {
                Log.e(TAG, "Error inesperado sync inventario post-guardado: ${e.message}", e)
            }
        }
    }

    open fun scheduleProveedoresSync(opticaId: String) {
        if (suppressSync) return
        scheduleDebounced(key = "proveedores:$opticaId") {
            onBeforeSync?.invoke("proveedores")
            try {
                syncGate.mutex.withLock {
                    if (!ensureSessionForPostSaveSync("proveedores")) return@withLock
                    when (val r = syncProveedoresUseCase!!(opticaId)) {
                        is Resource.Error -> Log.w(TAG, "Sync proveedores post-guardado: ${r.message}")
                        else -> Log.d(TAG, "Sync proveedores post-guardado OK")
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: IOException) {
                Log.e(TAG, "Error en red sync proveedores post-guardado: ${e.message}", e)
            } catch (e: Exception) {
                Log.e(TAG, "Error inesperado sync proveedores post-guardado: ${e.message}", e)
            }
        }
    }

    protected open suspend fun ensureSessionForPostSaveSync(stage: String): Boolean {
        if (!SyncSessionHelper.refreshSessionBeforeSync(supabase)) {
            Log.w(TAG, "Sync $stage post-guardado cancelada: no se pudo refrescar sesión")
            bgErrorCollector?.record("auth", "Post-save sync $stage cancelada: refresh JWT falló")
            return false
        }
        val currentUser = runCatching { supabase.auth.currentUserOrNull() }.getOrNull()
        if (currentUser == null) {
            Log.w(TAG, "Sync $stage post-guardado cancelada: sesión Supabase no activa")
            bgErrorCollector?.record("auth", "Post-save sync $stage cancelada: sin sesión")
            return false
        }
        val session = runCatching { supabase.auth.currentSessionOrNull() }.getOrNull()
        if (session?.accessToken.isNullOrBlank()) {
            Log.w(TAG, "Sync $stage post-guardado cancelada: JWT inválido o expirado")
            bgErrorCollector?.record("auth", "Post-save sync $stage cancelada: JWT inválido")
            return false
        }
        return true
    }

    companion object {
        private const val TAG = "PostSaveSync"
    }
}
