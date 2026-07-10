package com.example.optoapp.domain

import android.util.Log
import com.example.optoapp.data.DispensacionItem
import com.example.optoapp.data.OptoRepository
import com.example.optoapp.data.SyncStateTracker
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.CancellationException
import java.io.IOException
import java.time.LocalDate
import javax.inject.Inject

/**
 * Extracted from [SyncFinanzasUseCase] so finanzas sync can download each entity type
 * independently while sharing retry, skip-deletion, and tracking logic.
 */
class DownloadSyncCoordinator @Inject constructor(
    private val repository: OptoRepository,
    private val supabase: SupabaseClient,
    private val syncStateTracker: SyncStateTracker,
    private val deletionSyncHelper: DeletionSyncHelper,
    private val networkRetryHelper: NetworkRetryHelper
) {
    companion object {
        private const val TAG = "SyncFinanzas"
        private const val TABLE_DISPENSACIONES = "dispensaciones"
        private const val TABLE_DISPENSACION_ITEMS = "dispensacion_items"
        private const val TABLE_SERVICIOS = "servicios_extra"
        private const val TABLE_PAGOS = "pagos"
        private const val TABLE_RESUMEN_DIARIO = "resumen_diario"
        private const val TABLE_CONFIGURACION_FINANCIERA = "configuracion_financiera"
        private const val TABLE_REGALOS = "regalos_dispensacion"
    }

    private suspend inline fun <reified T : Any> downloadTable(
        opticaId: String,
        tableName: String,
        entityType: String,
        skipDeletions: Boolean,
        crossinline getId: (T) -> String,
        crossinline upsert: suspend (T) -> Unit
    ): Int {
        val skipIds = if (skipDeletions) deletionSyncHelper.deletedIds(opticaId) else emptySet()
        val remotos: List<T>
        try {
            var result: List<T> = emptyList()
            networkRetryHelper.retryNetwork("download:$tableName") {
                result = supabase.postgrest[tableName]
                    .select { filter { eq("optica_id", opticaId) } }
                    .decodeList<T>()
            }
            remotos = result
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            Log.e(TAG, "Error de red descargando $entityType: ${e.message}", e)
            syncStateTracker.markError(opticaId, "download_$entityType", "batch", e.message)
            return 0
        } catch (e: Exception) {
            Log.e(TAG, "Error inesperado descargando $entityType: ${e.message}", e)
            syncStateTracker.markError(opticaId, "download_$entityType", "batch", e.message)
            return 0
        }
        remotos.forEach { r ->
            if (skipDeletions && getId(r) in skipIds) return@forEach
            try {
                repository.withTransaction {
                    upsert(r)
                    syncStateTracker.markSynced(opticaId, entityType, getId(r))
                }
            } catch (e: CancellationException) { throw e }
            catch (e: IOException) {
                Log.e(TAG, "Error de red descargando item $entityType ${getId(r)}: ${e.message}", e)
                syncStateTracker.markError(opticaId, entityType, getId(r), e.message)
            } catch (e: Exception) {
                Log.e(TAG, "Error inesperado descargando item $entityType ${getId(r)}: ${e.message}", e)
                syncStateTracker.markError(opticaId, entityType, getId(r), e.message)
            }
        }
        return remotos.size
    }

    suspend fun downloadDispensacionItems(opticaId: String): Int = downloadTable<DispensacionItemRemota>(
        opticaId, TABLE_DISPENSACION_ITEMS, "dispensacion_item", skipDeletions = false,
        getId = { it.id }
    ) { r ->
        repository.upsertDispensacionItemFromRemote(r.toEntity())
    }

    suspend fun downloadDispensaciones(opticaId: String): Int = downloadTable<DispensacionRemota>(
        opticaId, TABLE_DISPENSACIONES, "dispensacion", skipDeletions = true,
        getId = { it.id }
    ) { r ->
        repository.upsertDispensacionFromRemote(r.toEntity())
    }

    suspend fun downloadServicios(opticaId: String): Int = downloadTable<ServicioRemoto>(
        opticaId, TABLE_SERVICIOS, "servicio_extra", skipDeletions = true,
        getId = { it.id }
    ) { r ->
        repository.upsertServicioFromRemote(r.toEntity())
    }

    suspend fun downloadPagos(opticaId: String): Int = downloadTable<PagoRemoto>(
        opticaId, TABLE_PAGOS, "pago", skipDeletions = true,
        getId = { it.id }
    ) { r ->
        repository.upsertPagoFromRemote(r.toEntity())
    }

    suspend fun downloadRegalos(opticaId: String): Int = downloadTable<RegaloDispensacionRemota>(
        opticaId, TABLE_REGALOS, "regalo_dispensacion", skipDeletions = true,
        getId = { it.id }
    ) { r ->
        repository.upsertRegaloFromRemote(r.toEntity())
    }

    suspend fun downloadResumenDiario(opticaId: String): Int {
        return try {
            val remotos = supabase.postgrest[TABLE_RESUMEN_DIARIO]
                .select { filter { eq("optica_id", opticaId) } }
                .decodeList<ResumenDiarioRemoto>()
            remotos.forEach { r ->
                try {
                    repository.withTransaction {
                        repository.upsertResumenDiarioFromRemote(r.toEntity())
                        syncStateTracker.markSynced(opticaId, "resumen_diario", r.id)
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.w(TAG, "resumen_diario upsert failed for ${r.id}", e)
                }
            }
            remotos.size
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "resumen_diario download failed", e)
            0
        }
    }

    suspend fun downloadConfiguracionFinanciera(opticaId: String): Int {
        return try {
            val remotos = supabase.postgrest[TABLE_CONFIGURACION_FINANCIERA]
                .select { filter { eq("optica_id", opticaId) } }
                .decodeList<ConfiguracionFinancieraRemoto>()
            remotos.forEach { r ->
                try {
                    repository.withTransaction {
                        repository.upsertConfiguracionFinancieraFromRemote(r.toEntity())
                        syncStateTracker.markSynced(opticaId, "configuracion_financiera", r.opticaId)
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.w(TAG, "configuracion_financiera upsert failed for ${r.opticaId}", e)
                }
            }
            remotos.size
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "configuracion_financiera download failed", e)
            0
        }
    }
}
