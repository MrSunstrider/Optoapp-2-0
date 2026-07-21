package com.example.optoapp.viewmodel

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.optoapp.data.ConflictDao
import com.example.optoapp.data.ConflictRecord
import com.example.optoapp.data.MembershipRepository
import com.example.optoapp.data.OpticaMembership
import com.example.optoapp.data.OptoRepository
import com.example.optoapp.data.OrdenCompraRepository
import com.example.optoapp.data.ProveedorRepository
import com.example.optoapp.data.Resource
import com.example.optoapp.data.SessionManager
import com.example.optoapp.data.SyncEntityStateDao
import com.example.optoapp.data.SyncTelemetry
import com.example.optoapp.data.SyncTelemetryRemoteRow
import com.example.optoapp.domain.SyncFinanzasUseCase
import com.example.optoapp.domain.SyncHistorialUseCase
import com.example.optoapp.domain.SyncInventarioFisicoUseCase
import com.example.optoapp.domain.SyncInventarioUseCase
import com.example.optoapp.domain.SyncInventoryKpisUseCase
import com.example.optoapp.domain.SyncOrdenesCompraUseCase
import com.example.optoapp.domain.SyncPacientesUseCase
import com.example.optoapp.domain.SyncProveedoresUseCase
import com.example.optoapp.domain.SyncSessionHelper
import com.example.optoapp.domain.sync.BumpEntityStrategy
import com.example.optoapp.domain.sync.SyncOrchestrator
import com.example.optoapp.subscription.SubscriptionManager
import com.example.optoapp.sync.PostSaveSyncScheduler
import com.example.optoapp.sync.SyncGate
import com.example.optoapp.util.BackgroundErrorCollector
import com.example.optoapp.util.SyncErrorSanitizer
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.withLock
import java.io.IOException
import javax.inject.Inject

sealed class SyncState {
    object Idle : SyncState()
    object Loading : SyncState()
    object Offline : SyncState()
    data class Success(val message: String) : SyncState()
    data class Error(val message: String) : SyncState()
}

@HiltViewModel
class SyncViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val sessionManager: SessionManager,
    private val membershipRepository: MembershipRepository,
    private val repository: OptoRepository,
    private val proveedorRepository: ProveedorRepository,
    private val ordenCompraRepository: OrdenCompraRepository,
    private val syncTelemetry: SyncTelemetry,
    private val subscriptionManager: SubscriptionManager,
    private val supabase: SupabaseClient,
    private val syncPacientesUseCase: SyncPacientesUseCase,
    private val syncHistorialUseCase: SyncHistorialUseCase,
    private val syncFinanzasUseCase: SyncFinanzasUseCase,
    private val syncInventarioUseCase: SyncInventarioUseCase,
    private val syncProveedoresUseCase: SyncProveedoresUseCase,
    private val syncOrdenesCompraUseCase: SyncOrdenesCompraUseCase,
    private val syncInventarioFisicoUseCase: SyncInventarioFisicoUseCase,
    private val syncInventoryKpisUseCase: SyncInventoryKpisUseCase,
    private val syncGate: SyncGate,
    private val conflictDao: ConflictDao,
    private val syncEntityStateDao: SyncEntityStateDao,
    private val supabaseObserver: com.example.optoapp.domain.observer.TableObserver,
    private val bgErrorCollector: BackgroundErrorCollector,
    private val postSaveSyncScheduler: PostSaveSyncScheduler,
    private val syncOrchestrator: SyncOrchestrator,
) : ViewModel() {

    companion object {
        private const val TAG = "SyncViewModel"
    }

    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    private val _isSilentSyncing = MutableStateFlow(false)
    val isSilentSyncing: StateFlow<Boolean> = _isSilentSyncing.asStateFlow()

    private val _isOnline = MutableStateFlow(true)
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    private val _conflicts = MutableStateFlow<List<ConflictRecord>>(emptyList())
    val conflicts: StateFlow<List<ConflictRecord>> = _conflicts.asStateFlow()

    private val _conflictCount = MutableStateFlow(0)
    val conflictCount: StateFlow<Int> = _conflictCount.asStateFlow()

    private val bumpStrategy = BumpEntityStrategy(
        repository,
        proveedorRepository,
        ordenCompraRepository,
        sessionManager,
    )

    private var wasOffline = false

    /** Cached membership list from the last successful [ensureSyncContext] call.
     *  Used as fallback for download-only operations when the network is unavailable. */
    private var cachedMemberships: List<OpticaMembership>? = null
    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: android.net.Network) {
            _isOnline.value = true
            if (wasOffline) {
                wasOffline = false
                performSilentSync()
            }
        }
        override fun onLost(network: android.net.Network) {
            _isOnline.value = false
            wasOffline = true
        }
        override fun onCapabilitiesChanged(
            network: android.net.Network,
            capabilities: NetworkCapabilities,
        ) {
            _isOnline.value = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        }
    }

    init {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = cm.activeNetwork
        val capabilities = activeNetwork?.let { cm.getNetworkCapabilities(it) }
        _isOnline.value = capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        cm.registerDefaultNetworkCallback(networkCallback)
    }

    override fun onCleared() {
        super.onCleared()
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        cm.unregisterNetworkCallback(networkCallback)
    }

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
            syncForEntityTypeWithResult(opticaId, entityType, skipUpload)
        }
    }

    /**
     * Like [syncForEntityType] but called inside an already-held mutex lock and returns
     * the [Resource] from the use case so callers can detect failure.
     */
    private suspend fun syncForEntityTypeWithResult(
        opticaId: String,
        entityType: String,
        skipUpload: Boolean,
    ): Resource<*> = when (entityType) {
        "paciente" -> syncPacientesUseCase(opticaId, skipUpload = skipUpload, downloadAfterUpload = true)
        "evaluacion" -> syncHistorialUseCase(opticaId, skipUpload = skipUpload, downloadAfterUpload = true)
        "dispensacion", "servicio_extra", "pago", "dispensacion_item" ->
            syncFinanzasUseCase(opticaId, skipUpload = skipUpload, downloadAfterUpload = true)
        "proveedor", "categoria_montura" ->
            syncProveedoresUseCase(opticaId, skipUpload = skipUpload, downloadAfterUpload = true)
        "orden_compra", "orden_compra_item" ->
            syncOrdenesCompraUseCase(opticaId, skipUpload = skipUpload, downloadAfterUpload = true)
        "inventory_kpis" ->
            syncInventoryKpisUseCase(opticaId)
        "inventario_fisico", "inventario_fisico_detalle" ->
            syncInventarioFisicoUseCase(opticaId, skipUpload = skipUpload, downloadAfterUpload = true)
        "montura", "montura_movimiento" -> syncInventarioUseCase(opticaId, skipUpload = skipUpload, downloadAfterUpload = true)
        else -> Resource.Success(Unit)
    }

    /**
     * Bumps the local entity's updatedAt to now so that filterConflicts passes
     * (local > remote), then uploads, and only clears the conflict record on success.
     *
     * NOTE: Three-way merge (FR-10) is deferred until baseSnapshot is populated
     * at conflict detection time. Currently baseSnapshot is always "{}", so the
     * merge is never reached. When that infrastructure is ready, uncomment the
     * merge logic and remove this fallback.
     */
    fun resolveKeepMine(entity: ConflictRecord) {
        viewModelScope.launch {
            val opticaId = sessionManager.opticaId.first()
            try {
                val syncResult = syncGate.mutex.withLock {
                    bumpStrategy.bump(entity.entityId, entity.entityType)
                    syncForEntityTypeWithResult(opticaId, entity.entityType, skipUpload = false)
                }
                if (syncResult !is Resource.Error) {
                    conflictDao.resolveConflict(entity.entityId, opticaId)
                    _conflicts.value = _conflicts.value.filter { it.entityId != entity.entityId }
                    _conflictCount.value = _conflicts.value.size
                    Log.d(TAG, "Conflicto resuelto (keep mine): ${entity.entityType}/${entity.entityId}")
                } else {
                    Log.w(TAG, "Keep mine: sync falló, conflicto retenido: ${entity.entityType}/${entity.entityId}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error resolviendo conflicto: ${e.message}", e)
            }
        }
    }

    /**
     * Bumps all active conflicts for the optica: bump each entity's updatedAt to now,
     * then clear all conflict records and trigger a full sync.
     *
     * No opticaId parameter — reads from [SessionManager] like [acceptAllCloud].
     */
    fun resolveKeepMineAll() {
        viewModelScope.launch {
            val opticaId = sessionManager.opticaId.first()
            try {
                val allConflicts = conflictDao.getConflicts(opticaId)
                val resolvedIds = mutableListOf<String>()
                allConflicts.forEach { entity ->
                    runCatching { bumpStrategy.bump(entity.entityId, entity.entityType) }
                        .onSuccess { resolvedIds.add(entity.entityId) }
                        .onFailure { e ->
                            Log.w(TAG, "Keep mine all: no se pudo bump ${entity.entityType}/${entity.entityId}: ${e.message}")
                        }
                }
                // Only clear conflict records for entities that were actually resolved.
                resolvedIds.forEach { entityId -> conflictDao.resolveConflict(entityId, opticaId) }
                syncEntityStateDao.deleteConflictedForOptica(opticaId)
                _conflicts.value = _conflicts.value.filter { it.entityId !in resolvedIds }
                _conflictCount.value = _conflicts.value.size
                performFullSync()
                Log.d(TAG, "Keep mine all: ${resolvedIds.size}/${allConflicts.size} conflictos resueltos")
            } catch (e: Exception) {
                Log.e(TAG, "Error en resolveKeepMineAll: ${e.message}", e)
            }
        }
    }

    /**
     * FR-11: Resolve "accept theirs" by clearing the conflict and forcing a download.
     *
     * NOTE: Three-way merge (FR-11) is deferred until baseSnapshot is populated
     * at conflict detection time. Currently baseSnapshot is always "{}", so the
     * merge is never reached. When that infrastructure is ready, uncomment the
     * merge logic and remove this fallback.
     */
    fun resolveAcceptTheirs(entity: ConflictRecord) {
        viewModelScope.launch {
            val opticaId = sessionManager.opticaId.first()
            try {
                conflictDao.resolveConflict(entity.entityId, opticaId)
                _conflicts.value = _conflicts.value.filter { it.entityId != entity.entityId }
                _conflictCount.value = _conflicts.value.size
                if (!SyncSessionHelper.refreshSessionBeforeSync(supabase)) return@launch
                syncForEntityType(opticaId, entity.entityType, skipUpload = true)
                Log.d(TAG, "Conflicto resuelto (accept theirs): ${entity.entityType}/${entity.entityId}")
            } catch (e: Exception) {
                Log.e(TAG, "Error resolviendo conflicto: ${e.message}", e)
            }
        }
    }

    fun dismissConflict(entity: ConflictRecord) {
        viewModelScope.launch {
            val opticaId = sessionManager.opticaId.first()
            conflictDao.resolveConflict(entity.entityId, opticaId)
            _conflicts.value = _conflicts.value.filter { it.entityId != entity.entityId }
            _conflictCount.value = _conflicts.value.size
        }
    }

    // RC-4: Both conflict_records AND sync_entity_state rows with status = 'conflicted'
    // are cleared so that the next sync cycle does not re-detect stale conflicts.
    fun acceptAllCloud() {
        viewModelScope.launch {
            val opticaId = sessionManager.opticaId.first()
            performFullDownload()
            conflictDao.clearConflicts(opticaId)
            syncEntityStateDao.deleteConflictedForOptica(opticaId)
            _conflicts.value = emptyList()
            _conflictCount.value = 0
            refreshConflicts()
        }
    }

    /**
     * NOTE: applyMergedEntity was removed when the three-way merge code was
     * cleaned up (M6). Three-way merge is deferred until baseSnapshot is
     * populated at conflict detection time.
     */

    // Silencia el PostSaveSyncScheduler para que las inserciones durante
    // la descarga no disparen syncs post-guardado que regeneren conflictos.
    private suspend fun performFullDownload() {
        _syncState.value = SyncState.Loading
        if (!isNetworkAvailable()) {
            _syncState.value = SyncState.Offline
            return
        }

        if (!SyncSessionHelper.refreshSessionBeforeSync(supabase)) {
            bgErrorCollector.record("auth", "Full download cancelada: no se pudo refrescar el JWT")
            _syncState.value = SyncState.Error("Tu sesión expiró. Vuelve a iniciar sesión.")
            return
        }
        val contextCheck = ensureSyncContext(allowCached = true)
        if (contextCheck != null) {
            _syncState.value = SyncState.Error(contextCheck)
            return
        }

        val opticaId = sessionManager.opticaId.first()

        postSaveSyncScheduler.cancelPending()
        postSaveSyncScheduler.suppressSync = true
        try {
            val hasErrors = syncOrchestrator.executeModules(opticaId, skipUpload = true)

            if (hasErrors) {
                bgErrorCollector.record("sync", "Full download completada con errores")
                syncTelemetry.recordFullSyncError("Algunos módulos reportaron errores")
                recordRemoteSyncTelemetry(opticaId, "error", "finalizado", "Algunos módulos con error")
                _syncState.value = SyncState.Error("Algunos datos no se pudieron sincronizar. Se reintentará automáticamente.")
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

    fun clearSyncUiState() {
        _syncState.value = SyncState.Idle
    }

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

        val hasErrors = syncOrchestrator.executeModules(opticaId, skipUpload = false)

        if (hasErrors) {
            bgErrorCollector.record("sync", "Full sync completada con errores")
            syncTelemetry.recordFullSyncError("Algunos módulos reportaron errores")
            recordRemoteSyncTelemetry(opticaId, "error", "finalizado", "Algunos módulos con error")
            _syncState.value = SyncState.Error("Sincronización parcial. Se reintentará automáticamente.")
        } else {
            syncTelemetry.recordFullSyncSuccess()
            recordRemoteSyncTelemetry(opticaId, "ok", "finalizado", null)
            runCatching { subscriptionManager.refreshPlanFromServer(opticaId) }
            _syncState.value = SyncState.Success("Sincronización completada con éxito")
        }
    }

    fun performSilentSync() = viewModelScope.launch {
        val contextCheck = ensureSyncContext(allowCached = true)
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
            if (!SyncSessionHelper.refreshSessionBeforeSync(supabase)) {
                Log.w(TAG, "Sync silenciosa cancelada: sesión expirada")
                bgErrorCollector.record("auth", "Silent sync cancelada: JWT expirado")
                return@launch
            }
            var hasErrors = false
            syncOrchestrator.executeSilentModules(opticaId) { module, result ->
                if (result is Resource.Error) {
                    hasErrors = true
                    Log.w(TAG, "Sync silenciosa ($module): ${result.message}")
                    recordRemoteSyncTelemetry(opticaId, "error", module, result.message)
                }
            }
            if (!hasErrors) {
                recordRemoteSyncTelemetry(opticaId, "ok", "inventario", null)
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
        rawError: String?,
    ) {
        runCatching {
            val safeError = SyncErrorSanitizer.forUserMessage(rawError).take(500)
            val row = SyncTelemetryRemoteRow(
                opticaId = opticaId,
                lastSyncAt = java.time.OffsetDateTime.now(java.time.ZoneOffset.UTC).toString(),
                lastStatus = status,
                lastStage = stage,
                lastError = if (status == "error") safeError else "",
            )
            supabase.postgrest["sync_telemetry_optica"].upsert(row)
        }.onFailure { e ->
            Log.w(TAG, "No se pudo guardar telemetría remota de sync: ${e.message}")
        }
    }

    fun observeRemoteTelemetry(opticaId: String) {
        viewModelScope.launch {
            supabaseObserver.observeTable("sync_telemetry_optica", opticaId)
                .collect {
                    Log.d(TAG, "Cambio detectado en telemetría remota")
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

    private suspend fun ensureSyncContext(allowCached: Boolean = false): String? {
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
        val memberships = try {
            membershipRepository.fetchMembershipsForCurrentUser()
        } catch (e: Exception) {
            Log.w(TAG, "ensureSyncContext: error fetching memberships, allowCached=$allowCached", e)
            if (allowCached) {
                val cached = cachedMemberships
                if (cached != null) {
                    Log.d(TAG, "ensureSyncContext: falling back to cached memberships (${cached.size})")
                    cached
                } else if (e is IOException) {
                    // H10: Network error in download-only context — proceed with the
                    // already-validated opticaId. The user belongs to this optica since
                    // they navigated to it; without this fallback, a brief network blip
                    // blocks offline-capable download-only syncs even when cached data exists locally.
                    Log.w(TAG, "ensureSyncContext: IOException with no cached memberships — allowing download-only fallback")
                    return null
                } else {
                    return "Error verificando membresía: ${e.localizedMessage}"
                }
            } else {
                return "Error verificando membresía: ${e.localizedMessage}"
            }
        }
        cachedMemberships = memberships
        val belongsToOptica = memberships.any { it.opticaId == opticaId }
        if (!belongsToOptica) {
            return "Tu cuenta (${currentUser.email ?: "usuario"}) no tiene acceso a la óptica actual. Reingresa y selecciona una óptica válida."
        }
        return null
    }
}
