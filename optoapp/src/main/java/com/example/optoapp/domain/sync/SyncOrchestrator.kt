package com.example.optoapp.domain.sync

import android.util.Log
import com.example.optoapp.data.Resource
import com.example.optoapp.domain.SyncFinanzasUseCase
import com.example.optoapp.domain.SyncHistorialUseCase
import com.example.optoapp.domain.SyncInventarioFisicoUseCase
import com.example.optoapp.domain.SyncInventarioUseCase
import com.example.optoapp.domain.SyncInventoryKpisUseCase
import com.example.optoapp.domain.SyncOrdenesCompraUseCase
import com.example.optoapp.domain.SyncPacientesUseCase
import com.example.optoapp.domain.SyncProveedoresUseCase
import com.example.optoapp.sync.SyncGate
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject

/**
 * Drives the sync iteration across all entity modules under a global mutex.
 *
 * This class owns the "run all 8 modules" loop that was duplicated across
 * [com.example.optoapp.viewmodel.SyncViewModel]'s three sync methods.
 * Each method ([executeModules], [executeSilentModules]) handles a different
 * orchestration style (error aggregation vs. per-module callback).
 */
class SyncOrchestrator @Inject constructor(
    private val syncPacientesUseCase: SyncPacientesUseCase,
    private val syncHistorialUseCase: SyncHistorialUseCase,
    private val syncFinanzasUseCase: SyncFinanzasUseCase,
    private val syncInventarioUseCase: SyncInventarioUseCase,
    private val syncProveedoresUseCase: SyncProveedoresUseCase,
    private val syncOrdenesCompraUseCase: SyncOrdenesCompraUseCase,
    private val syncInventarioFisicoUseCase: SyncInventarioFisicoUseCase,
    private val syncInventoryKpisUseCase: SyncInventoryKpisUseCase,
    private val syncGate: SyncGate
) {
    companion object {
        private const val TAG = "SyncOrchestrator"
    }

    /**
     * Runs all 8 entity modules under the sync mutex and returns whether any
     * module reported a [Resource.Error].
     *
     * Used by the full-sync and download-only flows. Modules are always called
     * with [downloadAfterUpload] = true so the server-stamped timestamps are
     * written back to Room.
     *
     * @param opticaId the optica context.
     * @param skipUpload when true, skips the local-upload phase (download-only).
     * @return true if any module returned [Resource.Error].
     */
    suspend fun executeModules(opticaId: String, skipUpload: Boolean): Boolean {
        return syncGate.mutex.withLock {
            var hasErrors = false

            val p = syncPacientesUseCase(opticaId, downloadAfterUpload = true, skipUpload = skipUpload)
            if (p is Resource.Error) { hasErrors = true; Log.w(TAG, "pacientes: ${p.message}") }

            val h = syncHistorialUseCase(opticaId, downloadAfterUpload = true, skipUpload = skipUpload)
            if (h is Resource.Error) { hasErrors = true; Log.w(TAG, "historial: ${h.message}") }

            val f = syncFinanzasUseCase(opticaId, downloadAfterUpload = true, skipUpload = skipUpload)
            if (f is Resource.Error) { hasErrors = true; Log.w(TAG, "finanzas: ${f.message}") }

            val pv = syncProveedoresUseCase(opticaId, downloadAfterUpload = true, skipUpload = skipUpload)
            if (pv is Resource.Error) { hasErrors = true; Log.w(TAG, "proveedores: ${pv.message}") }

            val oc = syncOrdenesCompraUseCase(opticaId, downloadAfterUpload = true, skipUpload = skipUpload)
            if (oc is Resource.Error) { hasErrors = true; Log.w(TAG, "ordenes_compra: ${oc.message}") }

            val kpi = syncInventoryKpisUseCase(opticaId)
            if (kpi is Resource.Error) { hasErrors = true; Log.w(TAG, "inventory_kpis: ${kpi.message}") }

            val i = syncInventarioUseCase(opticaId, downloadAfterUpload = true, skipUpload = skipUpload)
            if (i is Resource.Error) { hasErrors = true; Log.w(TAG, "inventario: ${i.message}") }

            val ifx = syncInventarioFisicoUseCase(opticaId, downloadAfterUpload = true, skipUpload = skipUpload)
            if (ifx is Resource.Error) { hasErrors = true; Log.w(TAG, "inventario_fisico: ${ifx.message}") }

            hasErrors
        }
    }

    /**
     * Silent-sync variant: runs all 8 modules under the sync mutex but delivers
     * each module's result individually via [onModuleResult] instead of aggregating
     * errors. This lets the caller record per-module telemetry.
     *
     * This variant always calls modules with the default [skipUpload] = false
     * (upload, then download) to match the existing silent-sync contract.
     */
    suspend fun executeSilentModules(
        opticaId: String,
        onModuleResult: suspend (module: String, result: Resource<*>) -> Unit
    ) {
        syncGate.mutex.withLock {
            onModuleResult("pacientes", syncPacientesUseCase(opticaId, downloadAfterUpload = true))
            onModuleResult("historial", syncHistorialUseCase(opticaId, downloadAfterUpload = true))
            onModuleResult("finanzas", syncFinanzasUseCase(opticaId, downloadAfterUpload = true))
            onModuleResult("proveedores", syncProveedoresUseCase(opticaId, downloadAfterUpload = true))
            onModuleResult("ordenes_compra", syncOrdenesCompraUseCase(opticaId, downloadAfterUpload = true))
            onModuleResult("inventory_kpis", syncInventoryKpisUseCase(opticaId))
            onModuleResult("inventario", syncInventarioUseCase(opticaId, downloadAfterUpload = true))
            onModuleResult("inventario_fisico", syncInventarioFisicoUseCase(opticaId, downloadAfterUpload = true))
        }
    }
}
