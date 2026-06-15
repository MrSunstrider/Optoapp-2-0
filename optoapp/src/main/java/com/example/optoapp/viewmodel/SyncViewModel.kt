package com.example.optoapp.viewmodel

import android.content.Context
import android.net.ConnectivityManager
import android.util.Log
import android.net.NetworkCapabilities
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.optoapp.data.ConflictDao
import com.example.optoapp.data.ConflictRecord
import com.example.optoapp.data.OptoRepository
import com.example.optoapp.data.SyncEntityStateDao
import com.example.optoapp.data.Resource
import com.example.optoapp.data.SessionManager
import com.example.optoapp.data.SyncTelemetry
import com.example.optoapp.data.SyncTelemetryRemoteRow
import com.example.optoapp.data.MembershipRepository
import com.example.optoapp.subscription.SubscriptionManager
import com.example.optoapp.sync.errorLabelForException
import kotlinx.coroutines.CancellationException
import java.io.IOException
import com.example.optoapp.util.BackgroundErrorCollector
import com.example.optoapp.util.SyncErrorSanitizer
import com.example.optoapp.domain.SyncFinanzasUseCase
import com.example.optoapp.domain.SyncHistorialUseCase
import com.example.optoapp.domain.SyncInventarioUseCase
import com.example.optoapp.domain.SyncPacientesUseCase
import com.example.optoapp.domain.SyncSessionHelper
import com.example.optoapp.sync.SyncGate
import com.example.optoapp.sync.PostSaveSyncScheduler
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject

sealed class SyncState {
    object Idle : SyncState()
    object Loading : SyncState()
    data class Success(val message: String) : SyncState()
    data class Error(val message: String) : SyncState()
}

/**
 * Paso 5.1: SyncViewModel
 * Orquestador global de la sincronización de datos local <-> nube.
 */
@HiltViewModel
class SyncViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val sessionManager: SessionManager,
    private val membershipRepository: MembershipRepository,
    private val repository: OptoRepository,
    private val syncTelemetry: SyncTelemetry,
    private val subscriptionManager: SubscriptionManager,
    private val supabase: SupabaseClient,
    private val syncPacientesUseCase: SyncPacientesUseCase,
    private val syncHistorialUseCase: SyncHistorialUseCase,
    private val syncFinanzasUseCase: SyncFinanzasUseCase,
    private val syncInventarioUseCase: SyncInventarioUseCase,
    private val syncGate: SyncGate,
    private val conflictDao: ConflictDao,
    private val syncEntityStateDao: SyncEntityStateDao,
    private val supabaseObserver: com.example.optoapp.domain.observer.SupabaseObserver,
    private val bgErrorCollector: BackgroundErrorCollector,
    private val postSaveSyncScheduler: PostSaveSyncScheduler
) : ViewModel() {

    companion object {
        private const val TAG = "SyncViewModel"
    }

    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    private val _isSilentSyncing = MutableStateFlow(false)
    val isSilentSyncing: StateFlow<Boolean> = _isSilentSyncing.asStateFlow()

    // ─── Estado de conflictos ──────────────────────────────────────────────
    private val _conflicts = MutableStateFlow<List<ConflictRecord>>(emptyList())
    val conflicts: StateFlow<List<ConflictRecord>> = _conflicts.asStateFlow()

    private val _conflictCount = MutableStateFlow(0)
    val conflictCount: StateFlow<Int> = _conflictCount.asStateFlow()

    /** Carga la lista de conflictos desde la DB local. */
    fun refreshConflicts() {
        viewModelScope.launch {
            val opticaId = sessionManager.opticaId.first()
            if (opticaId.isBlank() || opticaId == SessionManager.LEGACY_OPTICA_ID) return@launch
            val list = conflictDao.getConflicts(opticaId)
            _conflicts.value = list
            _conflictCount.value = list.size
        }
    }

    /**
     * Dispatches a sync call for a specific entity type.
     *
     * @param opticaId the optica to sync.
     * @param entityType the Room entity type string (e.g. "paciente", "evaluacion").
     * @param skipUpload when true, skips the upload phase (download-only). When false,
     *   uploads local data first, then downloads the server's updated_at back to Room.
     */
    private suspend fun syncForEntityType(opticaId: String, entityType: String, skipUpload: Boolean) {
        syncGate.mutex.withLock {
            when (entityType) {
                "paciente" -> syncPacientesUseCase(opticaId, skipUpload = skipUpload, downloadAfterUpload = true)
                "evaluacion" -> syncHistorialUseCase(opticaId, skipUpload = skipUpload, downloadAfterUpload = true)
                "dispensacion", "servicio_extra", "pago" ->
                    syncFinanzasUseCase(opticaId, skipUpload = skipUpload, downloadAfterUpload = true)
                "montura" -> syncInventarioUseCase(opticaId, skipUpload = skipUpload, downloadAfterUpload = true)
            }
        }
    }

    /**
     * Resolves a conflict by keeping the local version:
     * uploads the local entity first (skipUpload = false), then deletes the conflict record.
     *
     * REQ-A1: upload MUST happen before resolveConflict is called.
     * REQ-A2: downloadAfterUpload = true ensures the server's updated_at is written back to Room.
     * REQ-A3: uploading the local entity makes the server agree with local state — no new conflict.
     */
    fun resolveKeepMine(entity: ConflictRecord) {
        viewModelScope.launch {
            val opticaId = sessionManager.opticaId.first()
            try {
                syncForEntityType(opticaId, entity.entityType, skipUpload = false)
                conflictDao.resolveConflict(entity.entityId, opticaId)
                _conflicts.value = _conflicts.value.filter { it.entityId != entity.entityId }
                _conflictCount.value = _conflicts.value.size
                Log.d(TAG, "Conflicto resuelto (keep mine): ${entity.entityType}/${entity.entityId}")
            } catch (e: Exception) {
                Log.e(TAG, "Error resolviendo conflicto: ${e.message}", e)
            }
        }
    }

    /** Resuelve un conflicto: baja la versión remota forzando descarga. */
    fun resolveAcceptTheirs(entity: ConflictRecord) {
        viewModelScope.launch {
            val opticaId = sessionManager.opticaId.first()
            try {
                conflictDao.resolveConflict(entity.entityId, opticaId)
                _conflicts.value = _conflicts.value.filter { it.entityId != entity.entityId }
                _conflictCount.value = _conflicts.value.size
                // Force download of the module to overwrite local data with cloud version
                if (!SyncSessionHelper.refreshSessionBeforeSync(supabase)) return@launch
                syncForEntityType(opticaId, entity.entityType, skipUpload = true)
                Log.d(TAG, "Conflicto resuelto (accept theirs): ${entity.entityType}/${entity.entityId}")
            } catch (e: Exception) {
                Log.e(TAG, "Error resolviendo conflicto: ${e.message}", e)
            }
        }
    }

    /** Descarta un conflicto sin resolverlo (lo deja para después). */
    fun dismissConflict(entity: ConflictRecord) {
        viewModelScope.launch {
            val opticaId = sessionManager.opticaId.first()
            conflictDao.resolveConflict(entity.entityId, opticaId)
            _conflicts.value = _conflicts.value.filter { it.entityId != entity.entityId }
            _conflictCount.value = _conflicts.value.size
        }
    }

    /**
     * Resolves ALL conflicts by accepting the cloud version and forcing a full re-download.
     *
     * RC-4: Both conflict_records AND sync_entity_state rows with status = 'conflicted'
     * are cleared so that the next sync cycle does not re-detect stale conflicts.
     */
    fun acceptAllCloud() {
        viewModelScope.launch {
            val opticaId = sessionManager.opticaId.first()
            conflictDao.clearConflicts(opticaId)
            syncEntityStateDao.deleteConflictedForOptica(opticaId)
            _conflicts.value = emptyList()
            _conflictCount.value = 0
            performFullDownload()
            refreshConflicts()
        }
    }

    /**
     * Sincronización solo descarga (sin subir nada).
     * Útil para resolver conflictos aceptando la versión de la nube,
     * o para re-sincronizar un dispositivo desde cero.
     *
     * Silencia el PostSaveSyncScheduler para que las inserciones durante
     * la descarga no disparen syncs post-guardado que regeneren conflictos.
     */
    private suspend fun performFullDownload() {
        _syncState.value = SyncState.Loading
        if (!isNetworkAvailable()) {
            _syncState.value = SyncState.Error("Sin conexión a internet.")
            return
        }

        if (!SyncSessionHelper.refreshSessionBeforeSync(supabase)) {
            bgErrorCollector.record("auth", "Full download cancelada: no se pudo refrescar el JWT")
            _syncState.value = SyncState.Error("Tu sesión expiró. Vuelve a iniciar sesión.")
            return
        }
        val contextCheck = ensureSyncContext()
        if (contextCheck != null) {
            _syncState.value = SyncState.Error(contextCheck)
            return
        }

        val opticaId = sessionManager.opticaId.first()

        // Cancelar syncs pendientes y suprimir nuevos durante la descarga
        postSaveSyncScheduler.cancelPending()
        postSaveSyncScheduler.suppressSync = true
        try {
            var hasErrors = false
            syncGate.mutex.withLock {
                val p = syncPacientesUseCase(opticaId, downloadAfterUpload = true, skipUpload = true)
                if (p is Resource.Error) { hasErrors = true; Log.w(TAG, "Full download (pacientes): ${p.message}") }

                val h = syncHistorialUseCase(opticaId, downloadAfterUpload = true, skipUpload = true)
                if (h is Resource.Error) { hasErrors = true; Log.w(TAG, "Full download (historial): ${h.message}") }

                val f = syncFinanzasUseCase(opticaId, downloadAfterUpload = true, skipUpload = true)
                if (f is Resource.Error) { hasErrors = true; Log.w(TAG, "Full download (finanzas): ${f.message}") }

                val i = syncInventarioUseCase(opticaId, downloadAfterUpload = true, skipUpload = true)
                if (i is Resource.Error) { hasErrors = true; Log.w(TAG, "Full download (inventario): ${i.message}") }
            }

            if (hasErrors) {
                bgErrorCollector.record("sync", "Full download completada con errores")
                syncTelemetry.recordFullSyncError("Algunos módulos reportaron errores")
                recordRemoteSyncTelemetry(opticaId, "error", "finalizado", "Algunos módulos con error")
                _syncState.value = SyncState.Error("Descarga completada con errores. Revisa el estado de sync para más detalles.")
            } else {
                syncTelemetry.recordFullSyncSuccess()
                recordRemoteSyncTelemetry(opticaId, "ok", "finalizado", null)
                runCatching { subscriptionManager.refreshPlanFromServer(opticaId) }
                _syncState.value = SyncState.Success("Datos descargados desde la nube correctamente")
            }
        } finally {
            postSaveSyncScheduler.suppressSync = false
        }
    }

    /** Vuelve el estado de UI de sync a inactivo (tras mensaje o cierre de diálogo). */
    fun clearSyncUiState() {
        _syncState.value = SyncState.Idle
    }

    /**
     * Dispara la sincronización manual completa (Bidireccional).
     * Llama a los 4 UseCases secuencialmente, upload + download.
     */
    fun performFullSync() = viewModelScope.launch {
        _syncState.value = SyncState.Loading
        if (!isNetworkAvailable()) {
            _syncState.value = SyncState.Error("Sin conexión a internet.")
            return@launch
        }

        if (!SyncSessionHelper.refreshSessionBeforeSync(supabase)) {
            bgErrorCollector.record("auth", "Full sync cancelada: no se pudo refrescar el JWT")
            _syncState.value = SyncState.Error("Tu sesión expiró. Vuelve a iniciar sesión.")
            return@launch
        }
        val contextCheck = ensureSyncContext()
        if (contextCheck != null) {
            _syncState.value = SyncState.Error(contextCheck)
            return@launch
        }

        val opticaId = sessionManager.opticaId.first()
        repository.reassignLegacyMiOpticaBaseTo(opticaId)

        var hasErrors = false
        syncGate.mutex.withLock {
            // Ejecutar UseCases secuencialmente (orden: pacientes → historial → finanzas → inventario)
            val p = syncPacientesUseCase(opticaId, downloadAfterUpload = true)
            if (p is Resource.Error) { hasErrors = true; Log.w(TAG, "Full sync (pacientes): ${p.message}") }

            val h = syncHistorialUseCase(opticaId, downloadAfterUpload = true)
            if (h is Resource.Error) { hasErrors = true; Log.w(TAG, "Full sync (historial): ${h.message}") }

            val f = syncFinanzasUseCase(opticaId, downloadAfterUpload = true)
            if (f is Resource.Error) { hasErrors = true; Log.w(TAG, "Full sync (finanzas): ${f.message}") }

            val i = syncInventarioUseCase(opticaId, downloadAfterUpload = true)
            if (i is Resource.Error) { hasErrors = true; Log.w(TAG, "Full sync (inventario): ${i.message}") }
        }

        if (hasErrors) {
            bgErrorCollector.record("sync", "Full sync completada con errores")
            syncTelemetry.recordFullSyncError("Algunos módulos reportaron errores")
            recordRemoteSyncTelemetry(opticaId, "error", "finalizado", "Algunos módulos con error")
            _syncState.value = SyncState.Error("Sincronización completada con errores. Revisa el estado de sync para más detalles.")
        } else {
            syncTelemetry.recordFullSyncSuccess()
            recordRemoteSyncTelemetry(opticaId, "ok", "finalizado", null)
            runCatching { subscriptionManager.refreshPlanFromServer(opticaId) }
            _syncState.value = SyncState.Success("Sincronización completada con éxito")
        }
    }

    /**
     * Sincronización automática silenciosa (solo subida).
     * No cambia el estado global de UI para no interrumpir al usuario.
     */
    fun performSilentSync() = viewModelScope.launch {
        val contextCheck = ensureSyncContext()
        if (contextCheck != null) {
            Log.w(TAG, "Sync silenciosa cancelada: $contextCheck")
            return@launch
        }
        val opticaId = sessionManager.opticaId.first()
        // Sin red también: migrar datos legacy a la óptica de sesión para que las listas no queden vacías.
        repository.reassignLegacyMiOpticaBaseTo(opticaId)
        if (_isSilentSyncing.value || !isNetworkAvailable()) return@launch
        _isSilentSyncing.value = true
        try {
            syncGate.mutex.withLock {
                if (!SyncSessionHelper.refreshSessionBeforeSync(supabase)) {
                    Log.w(TAG, "Sync silenciosa cancelada: sesión expirada")
                    bgErrorCollector.record("auth", "Silent sync cancelada: JWT expirado")
                    return@withLock
                }
                var hasErrors = false
                when (val p = syncPacientesUseCase(opticaId, downloadAfterUpload = false)) {
                    is Resource.Error -> {
                        hasErrors = true
                        Log.w(TAG, "Sync silenciosa (pacientes): ${p.message}")
                        recordRemoteSyncTelemetry(opticaId, "error", "pacientes", p.message)
                    }
                    else -> {}
                }
                when (val h = syncHistorialUseCase(opticaId, downloadAfterUpload = false)) {
                    is Resource.Error -> {
                        hasErrors = true
                        Log.w(TAG, "Sync silenciosa (historial): ${h.message}")
                        recordRemoteSyncTelemetry(opticaId, "error", "historial", h.message)
                    }
                    else -> {}
                }
                when (val f = syncFinanzasUseCase(opticaId, downloadAfterUpload = false)) {
                    is Resource.Error -> {
                        hasErrors = true
                        Log.w(TAG, "Sync silenciosa (finanzas): ${f.message}")
                        recordRemoteSyncTelemetry(opticaId, "error", "finanzas", f.message)
                    }
                    else -> {}
                }
                when (val i = syncInventarioUseCase(opticaId, downloadAfterUpload = false)) {
                    is Resource.Error -> {
                        hasErrors = true
                        Log.w(TAG, "Sync silenciosa (inventario): ${i.message}")
                        recordRemoteSyncTelemetry(opticaId, "error", "inventario", i.message)
                    }
                    else -> {}
                }
                if (!hasErrors) {
                    recordRemoteSyncTelemetry(opticaId, "ok", "inventario", null)
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            Log.e(TAG, "Error en red en sync silenciosa: ${e.message}", e)
        } catch (e: Exception) {
            Log.e(TAG, "Error inesperado en sync silenciosa: ${e.message}", e)
        } finally {
            _isSilentSyncing.value = false
        }
    }

    private suspend fun recordRemoteSyncTelemetry(
        opticaId: String,
        status: String,
        stage: String,
        rawError: String?
    ) {
        runCatching {
            val safeError = SyncErrorSanitizer.forUserMessage(rawError).take(500)
            val row = SyncTelemetryRemoteRow(
                opticaId = opticaId,
                lastSyncAt = java.time.OffsetDateTime.now(java.time.ZoneOffset.UTC).toString(),
                lastStatus = status,
                lastStage = stage,
                lastError = if (status == "error") safeError else ""
            )
            supabase.postgrest["sync_telemetry_optica"].upsert(row)
        }.onFailure { e ->
            Log.w(TAG, "No se pudo guardar telemetría remota de sync: ${e.message}")
        }
    }

    /**
     * Ejemplo de uso del patrón Observer: Escucha cambios en la telemetría en tiempo real
     * desde otros dispositivos para la misma óptica.
     */
    fun observeRemoteTelemetry(opticaId: String) {
        viewModelScope.launch {
            supabaseObserver.observeTable("sync_telemetry_optica", opticaId)
                .collect { action ->
                    Log.d(TAG, "Cambio detectado en telemetría remota: $action")
                    // Aquí se podría disparar una actualización de UI si otro dispositivo finalizó sync
                }
        }
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false
        return when {
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
            else -> false
        }
    }

    private suspend fun ensureSyncContext(): String? {
        val session = runCatching { supabase.auth.currentSessionOrNull() }.getOrNull()
        if (session == null || session.accessToken.isNullOrBlank()) {
            return "Tu sesión de Supabase no está activa. Vuelve a iniciar sesión."
        }
        val currentUser = runCatching { supabase.auth.currentUserOrNull() }.getOrNull()
            ?: return "Tu sesión de Supabase no está activa. Vuelve a iniciar sesión."
        val opticaId = sessionManager.opticaId.first()
        if (opticaId.isBlank() || opticaId == SessionManager.LEGACY_OPTICA_ID) {
            return "Debes seleccionar o crear una óptica antes de sincronizar."
        }
        val memberships = membershipRepository.fetchMembershipsForCurrentUser()
        val belongsToOptica = memberships.any { it.opticaId == opticaId }
        if (!belongsToOptica) {
            return "Tu cuenta (${currentUser.email ?: "usuario"}) no tiene acceso a la óptica actual. Reingresa y selecciona una óptica válida."
        }
        return null
    }
}
