package com.example.optoapp.domain

import com.example.optoapp.data.OptoRepository
import com.example.optoapp.data.SyncStateTracker
import com.example.optoapp.util.AppLogger
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.CancellationException
import java.io.IOException
import javax.inject.Inject

/**
 * Extracted from [SyncFinanzasUseCase] so the upload coordinator can share deletion
 * propagation without duplicating Supabase filter logic per entity type.
 */
open class DeletionSyncHelper @Inject constructor(
    private val repository: OptoRepository,
    private val supabase: SupabaseClient,
    private val syncStateTracker: SyncStateTracker,
) {
    companion object {
        private const val TAG = "SyncFinanzas"
        private const val TABLE_DISPENSACIONES = "dispensaciones"
        private const val TABLE_PAGOS = "pagos"
        private const val TABLE_SERVICIOS = "servicios_extra"
        private const val TABLE_GASTOS_OPERATIVOS = "gastos_operativos"
        private const val TABLE_DISPENSACION_ITEMS = "dispensacion_items"
        private const val TABLE_SERVICIO_EXTRA_ITEMS = "servicio_extra_items"
        private const val TABLE_REGALOS_SERVICIO = "regalos_servicio_extra"

        /** Raised with SQLSTATE P0001 by `guard_dispensaciones_delete` (migration 20261002034600). */
        private const val DISPENSACION_HAS_TRACE = "dispensacion_has_trace"

        const val DELETE_REFUSED_MESSAGE =
            "No se pudo eliminar la orden: tiene pagos o movimientos registrados en otro dispositivo; se restauró."
    }

    suspend fun pushPendingDeletions(opticaId: String) {
        val pending = repository.getPendingDeletions(opticaId)
        if (pending.isEmpty()) return
        AppLogger.d(TAG, "Finanzas: propagando ${pending.size} eliminaciones a Supabase")
        pending.forEach { tombstone ->
            val table = when (tombstone.entityType) {
                "servicio_extra" -> TABLE_SERVICIOS
                "dispensacion" -> TABLE_DISPENSACIONES
                "pago" -> TABLE_PAGOS
                "gasto_operativo" -> TABLE_GASTOS_OPERATIVOS
                "dispensacion_item" -> TABLE_DISPENSACION_ITEMS
                "servicio_extra_item" -> TABLE_SERVICIO_EXTRA_ITEMS
                "regalo_servicio_extra" -> TABLE_REGALOS_SERVICIO
                else -> null
            }
            if (table == null) {
                repository.clearDeletionState(opticaId, tombstone.entityType, tombstone.entityId)
                return@forEach
            }
            try {
                deleteRemote(table, tombstone.entityId, opticaId)
                repository.clearDeletionState(opticaId, tombstone.entityType, tombstone.entityId)
                AppLogger.d(TAG, "Eliminado remoto ${tombstone.entityType}/${tombstone.entityId}")
            } catch (e: CancellationException) {
                throw e
            } catch (e: IOException) {
                AppLogger.e(TAG, "Error en red eliminando remoto ${tombstone.entityType}/${tombstone.entityId}: ${e.message}", e)
            } catch (e: Exception) {
                if (tombstone.entityType == "dispensacion" && isTraceRefusal(e)) {
                    restoreRefusedDispensacion(opticaId, tombstone.entityId)
                } else {
                    AppLogger.e(TAG, "Error inesperado eliminando remoto ${tombstone.entityType}/${tombstone.entityId}: ${e.message}", e)
                }
            }
        }
    }

    /**
     * The server keeps the row (another device recorded pagos or movimientos on it), so dropping the
     * tombstone lets this sync's download bring the order back instead of retrying a delete forever.
     */
    private suspend fun restoreRefusedDispensacion(opticaId: String, id: String) {
        AppLogger.w(TAG, "Supabase rechazó eliminar dispensacion/$id (tiene trazas); se restaurará al descargar")
        repository.clearDeletionState(opticaId, "dispensacion", id)
        syncStateTracker.markError(opticaId, "eliminacion_rechazada", id, DELETE_REFUSED_MESSAGE)
    }

    private fun isTraceRefusal(e: Throwable): Boolean =
        generateSequence(e) { it.cause }.any { it.message?.contains(DISPENSACION_HAS_TRACE) == true }

    internal open suspend fun deleteRemote(table: String, id: String, opticaId: String) {
        supabase.postgrest[table].delete {
            filter {
                eq("id", id)
                eq("optica_id", opticaId)
            }
        }
    }

    /** IDs marcados para eliminación que NO deben reinsertarse al bajar de la nube. */
    suspend fun deletedIds(opticaId: String): Set<String> = repository.getPendingDeletions(opticaId).map { it.entityId }.toSet()
}
