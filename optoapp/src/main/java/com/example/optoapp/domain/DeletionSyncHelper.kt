package com.example.optoapp.domain

import com.example.optoapp.data.OptoRepository
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
class DeletionSyncHelper @Inject constructor(
    private val repository: OptoRepository,
    private val supabase: SupabaseClient,
) {
    companion object {
        private const val TAG = "SyncFinanzas"
        private const val TABLE_DISPENSACIONES = "dispensaciones"
        private const val TABLE_PAGOS = "pagos"
        private const val TABLE_SERVICIOS = "servicios_extra"
        private const val TABLE_GASTOS_OPERATIVOS = "gastos_operativos"
        private const val TABLE_DISPENSACION_ITEMS = "dispensacion_items"
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
                else -> null
            }
            if (table == null) {
                repository.clearDeletionState(opticaId, tombstone.entityType, tombstone.entityId)
                return@forEach
            }
            try {
                supabase.postgrest[table].delete {
                    filter {
                        eq("id", tombstone.entityId)
                        eq("optica_id", opticaId)
                    }
                }
                repository.clearDeletionState(opticaId, tombstone.entityType, tombstone.entityId)
                AppLogger.d(TAG, "Eliminado remoto ${tombstone.entityType}/${tombstone.entityId}")
            } catch (e: CancellationException) {
                throw e
            } catch (e: IOException) {
                AppLogger.e(TAG, "Error en red eliminando remoto ${tombstone.entityType}/${tombstone.entityId}: ${e.message}", e)
            } catch (e: Exception) {
                AppLogger.e(TAG, "Error inesperado eliminando remoto ${tombstone.entityType}/${tombstone.entityId}: ${e.message}", e)
            }
        }
    }

    /** IDs marcados para eliminación que NO deben reinsertarse al bajar de la nube. */
    suspend fun deletedIds(opticaId: String): Set<String> = repository.getPendingDeletions(opticaId).map { it.entityId }.toSet()
}
