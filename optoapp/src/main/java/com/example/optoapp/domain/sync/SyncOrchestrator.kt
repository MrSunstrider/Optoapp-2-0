package com.example.optoapp.domain.sync

import android.util.Log
import com.example.optoapp.data.Resource
import com.example.optoapp.domain.SyncFinanzasUseCase
import com.example.optoapp.util.AppLogger
import com.example.optoapp.util.BackgroundErrorCollector
import com.example.optoapp.domain.SyncHistorialUseCase
import com.example.optoapp.domain.SyncInventarioFisicoUseCase
import com.example.optoapp.domain.SyncInventarioUseCase
import com.example.optoapp.domain.SyncInventoryKpisUseCase
import com.example.optoapp.domain.SyncOrdenesCompraUseCase
import com.example.optoapp.domain.SyncPacientesUseCase
import com.example.optoapp.domain.SyncProveedoresUseCase
import com.example.optoapp.sync.SyncGate
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import androidx.annotation.VisibleForTesting
import javax.inject.Inject

class SyncOrchestrator @Inject constructor(
    private val syncPacientesUseCase: SyncPacientesUseCase,
    private val syncHistorialUseCase: SyncHistorialUseCase,
    private val syncFinanzasUseCase: SyncFinanzasUseCase,
    private val syncInventarioUseCase: SyncInventarioUseCase,
    private val syncProveedoresUseCase: SyncProveedoresUseCase,
    private val syncOrdenesCompraUseCase: SyncOrdenesCompraUseCase,
    private val syncInventarioFisicoUseCase: SyncInventarioFisicoUseCase,
    private val syncInventoryKpisUseCase: SyncInventoryKpisUseCase,
    private val syncGate: SyncGate,
    private val bgErrorCollector: BackgroundErrorCollector,
) {
    companion object {
        private const val TAG = "SyncOrchestrator"

        @VisibleForTesting
        @Volatile
        var syncTimeoutMs: Long = 300_000L
    }

    // Modules always called with downloadAfterUpload=true so server-stamped
    // timestamps are written back to Room
    suspend fun executeModules(opticaId: String, skipUpload: Boolean): Boolean {
        return try {
            withTimeout(syncTimeoutMs) {
                syncGate.mutex.withLock {
                    var hasErrors = false

                    val p = syncPacientesUseCase(opticaId, downloadAfterUpload = true, skipUpload = skipUpload)
                    if (p is Resource.Error) {
                        hasErrors = true
                        Log.w(TAG, "pacientes: ${p.message}")
                        bgErrorCollector.record("sync:pacientes", p.message ?: "")
                    }

                    val h = syncHistorialUseCase(opticaId, downloadAfterUpload = true, skipUpload = skipUpload)
                    if (h is Resource.Error) {
                        hasErrors = true
                        Log.w(TAG, "historial: ${h.message}")
                        bgErrorCollector.record("sync:historial", h.message ?: "")
                    }

                    val f = syncFinanzasUseCase(opticaId, downloadAfterUpload = true, skipUpload = skipUpload)
                    if (f is Resource.Error) {
                        hasErrors = true
                        Log.w(TAG, "finanzas: ${f.message}")
                        bgErrorCollector.record("sync:finanzas", f.message ?: "")
                    }

                    val pv = syncProveedoresUseCase(opticaId, downloadAfterUpload = true, skipUpload = skipUpload)
                    if (pv is Resource.Error) {
                        hasErrors = true
                        Log.w(TAG, "proveedores: ${pv.message}")
                        bgErrorCollector.record("sync:proveedores", pv.message ?: "")
                    }

                    val oc = syncOrdenesCompraUseCase(opticaId, downloadAfterUpload = true, skipUpload = skipUpload)
                    if (oc is Resource.Error) {
                        hasErrors = true
                        Log.w(TAG, "ordenes_compra: ${oc.message}")
                        bgErrorCollector.record("sync:ordenes_compra", oc.message ?: "")
                    }

                    val kpi = syncInventoryKpisUseCase(opticaId)
                    if (kpi is Resource.Error) {
                        hasErrors = true
                        Log.w(TAG, "inventory_kpis: ${kpi.message}")
                        bgErrorCollector.record("sync:inventory_kpis", kpi.message ?: "")
                    }

                    val i = syncInventarioUseCase(opticaId, downloadAfterUpload = true, skipUpload = skipUpload)
                    if (i is Resource.Error) {
                        hasErrors = true
                        Log.w(TAG, "inventario: ${i.message}")
                        bgErrorCollector.record("sync:inventario", i.message ?: "")
                    }

                    val ifx = syncInventarioFisicoUseCase(opticaId, downloadAfterUpload = true, skipUpload = skipUpload)
                    if (ifx is Resource.Error) {
                        hasErrors = true
                        Log.w(TAG, "inventario_fisico: ${ifx.message}")
                        bgErrorCollector.record("sync:inventario_fisico", ifx.message ?: "")
                    }

                    hasErrors
                }
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Sync execution timed out after ${syncTimeoutMs}ms", e)
            true
        }
    }

    // WHY: Per-module callback enables granular remote telemetry (UPSERT only stores last status — silent sync
    // needs to report each module individually so we know which one failed when the user isn't watching)
    private fun recordModuleError(module: String, result: Resource<*>) {
        if (result is Resource.Error) {
            bgErrorCollector.record("sync:$module", result.message ?: "")
        }
    }

    suspend fun executeSilentModules(
        opticaId: String,
        onModuleResult: suspend (module: String, result: Resource<*>) -> Unit,
    ) {
        try {
            withTimeout(syncTimeoutMs) {
                syncGate.mutex.withLock {
                    run {
                        val r = syncPacientesUseCase(opticaId, downloadAfterUpload = true)
                        onModuleResult("pacientes", r); recordModuleError("pacientes", r)
                    }
                    run {
                        val r = syncHistorialUseCase(opticaId, downloadAfterUpload = true)
                        onModuleResult("historial", r); recordModuleError("historial", r)
                    }
                    run {
                        val r = syncFinanzasUseCase(opticaId, downloadAfterUpload = true)
                        onModuleResult("finanzas", r); recordModuleError("finanzas", r)
                    }
                    run {
                        val r = syncProveedoresUseCase(opticaId, downloadAfterUpload = true)
                        onModuleResult("proveedores", r); recordModuleError("proveedores", r)
                    }
                    run {
                        val r = syncOrdenesCompraUseCase(opticaId, downloadAfterUpload = true)
                        onModuleResult("ordenes_compra", r); recordModuleError("ordenes_compra", r)
                    }
                    run {
                        val r = syncInventoryKpisUseCase(opticaId)
                        onModuleResult("inventory_kpis", r); recordModuleError("inventory_kpis", r)
                    }
                    run {
                        val r = syncInventarioUseCase(opticaId, downloadAfterUpload = true)
                        onModuleResult("inventario", r); recordModuleError("inventario", r)
                    }
                    run {
                        val r = syncInventarioFisicoUseCase(opticaId, downloadAfterUpload = true)
                        onModuleResult("inventario_fisico", r); recordModuleError("inventario_fisico", r)
                    }
                }
            }
        } catch (e: TimeoutCancellationException) {
            AppLogger.e(TAG, "Silent sync timed out after ${syncTimeoutMs}ms")
            onModuleResult("_timeout", Resource.Error<Any?>("Sync timed out after ${syncTimeoutMs}ms"))
        }
    }
}
