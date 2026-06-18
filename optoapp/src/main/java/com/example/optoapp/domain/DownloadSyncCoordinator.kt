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
 * Downloads dispensaciones, servicios_extra and pagos from Supabase.
 * Extracted from [SyncFinanzasUseCase].
 */
class DownloadSyncCoordinator @Inject constructor(
    private val repository: OptoRepository,
    private val supabase: SupabaseClient,
    private val syncStateTracker: SyncStateTracker,
    private val deletionSyncHelper: DeletionSyncHelper
) {
    companion object {
        private const val TAG = "SyncFinanzas"
        private const val TABLE_DISPENSACIONES = "dispensaciones"
        private const val TABLE_DISPENSACION_ITEMS = "dispensacion_items"
        private const val TABLE_SERVICIOS = "servicios_extra"
        private const val TABLE_PAGOS = "pagos"
        private const val TABLE_ARQUEO_CAJA = "arqueo_caja"
    }

    suspend fun downloadDispensacionItems(opticaId: String): Int {
        val remotos = supabase.postgrest[TABLE_DISPENSACION_ITEMS]
            .select { filter { eq("optica_id", opticaId) } }
            .decodeList<DispensacionItemRemota>()
        remotos.forEach { r ->
            try {
                val local = r.toEntity()
                repository.insertDispensacionItem(local)
                syncStateTracker.markSynced(opticaId, "dispensacion_item", local.id)
            } catch (e: CancellationException) {
                throw e
            } catch (e: IOException) {
                Log.e(TAG, "Error descargando item de dispensación: ${e.message}", e)
                syncStateTracker.markError(opticaId, "dispensacion_item", r.id, e.message)
            } catch (e: Exception) {
                Log.e(TAG, "Error inesperado descargando item de dispensación: ${e.message}", e)
                syncStateTracker.markError(opticaId, "dispensacion_item", r.id, e.message)
            }
        }
        return remotos.size
    }

    suspend fun downloadDispensaciones(opticaId: String): Int {
        val skipIds = deletionSyncHelper.deletedIds(opticaId)
        val remotos = supabase.postgrest[TABLE_DISPENSACIONES]
            .select { filter { eq("optica_id", opticaId) } }
            .decodeList<DispensacionRemota>()
        remotos.forEach { r ->
            if (r.id in skipIds) return@forEach
            try {
                val local = r.toEntity()
                repository.insertDispensacion(local)
                syncStateTracker.markSynced(opticaId, "dispensacion", local.id)
            } catch (e: CancellationException) {
                throw e
            } catch (e: IOException) {
                Log.e(TAG, "Error en red descargando dispensación: ${e.message}", e)
                syncStateTracker.markError(opticaId, "dispensacion", r.id, e.message)
            } catch (e: Exception) {
                Log.e(TAG, "Error inesperado descargando dispensación: ${e.message}", e)
                syncStateTracker.markError(opticaId, "dispensacion", r.id, e.message)
            }
        }
        return remotos.size
    }

    suspend fun downloadServicios(opticaId: String): Int {
        val skipIds = deletionSyncHelper.deletedIds(opticaId)
        val remotos = supabase.postgrest[TABLE_SERVICIOS]
            .select { filter { eq("optica_id", opticaId) } }
            .decodeList<ServicioRemoto>()
        remotos.forEach { r ->
            if (r.id in skipIds) return@forEach
            try {
                val local = r.toEntity()
                repository.insertServicio(local)
                syncStateTracker.markSynced(opticaId, "servicio_extra", local.id)
            } catch (e: CancellationException) {
                throw e
            } catch (e: IOException) {
                Log.e(TAG, "Error en red descargando servicio extra: ${e.message}", e)
                syncStateTracker.markError(opticaId, "servicio_extra", r.id, e.message)
            } catch (e: Exception) {
                Log.e(TAG, "Error inesperado descargando servicio extra: ${e.message}", e)
                syncStateTracker.markError(opticaId, "servicio_extra", r.id, e.message)
            }
        }
        return remotos.size
    }

    suspend fun downloadPagos(opticaId: String): Int {
        val skipIds = deletionSyncHelper.deletedIds(opticaId)
        val remotos = supabase.postgrest[TABLE_PAGOS]
            .select { filter { eq("optica_id", opticaId) } }
            .decodeList<PagoRemoto>()
        remotos.forEach { r ->
            if (r.id in skipIds) return@forEach
            try {
                val local = r.toEntity()
                repository.insertPago(local)
                syncStateTracker.markSynced(opticaId, "pago", local.id)
            } catch (e: CancellationException) {
                throw e
            } catch (e: IOException) {
                Log.e(TAG, "Error en red descargando pago: ${e.message}", e)
                syncStateTracker.markError(opticaId, "pago", r.id, e.message)
            } catch (e: Exception) {
                Log.e(TAG, "Error inesperado descargando pago: ${e.message}", e)
                syncStateTracker.markError(opticaId, "pago", r.id, e.message)
            }
        }
        return remotos.size
    }

    suspend fun downloadArqueos(opticaId: String): Int {
        return try {
            val remoteArqueos = supabase.postgrest[TABLE_ARQUEO_CAJA]
                .select { filter { eq("optica_id", opticaId) } }
                .decodeList<ArqueoCajaRemota>()
            remoteArqueos.forEach { remote ->
                try {
                    val local = repository.getArqueoByFechaSync(
                        LocalDate.parse(remote.fecha), remote.opticaId
                    )
                    if (local == null || remote.updatedAt > local.updatedAt) {
                        repository.upsertArqueoFromRemote(remote.toLocal())
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.w(TAG, "arqueo upsert failed for ${remote.id}", e)
                }
            }
            remoteArqueos.size
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "arqueo download failed", e)
            0
        }
    }
}
