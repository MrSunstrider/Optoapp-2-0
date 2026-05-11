package com.example.optoapp.domain

import android.util.Log
import com.example.optoapp.data.DispensacionOptica
import com.example.optoapp.data.FinanzasRemoteDefaults
import com.example.optoapp.data.OptoRepository
import com.example.optoapp.data.Resource
import com.example.optoapp.util.rethrowIfCancellation
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.delay
import java.time.LocalDate
import javax.inject.Inject

/**
 * FASE 3 – Paso 3.3
 * Sincronización de Dispensaciones, Pagos y Servicios Extra.
 *
 * P0-T2 — Orden de subida obligatorio: **Dispensaciones → servicios_extra → pagos**
 * (padres antes que pagos; pagos referencian dispensación y/o servicio).
 *
 * Orden de bajada: el mismo, para que existan padres antes de insertar pagos locales.
 */
class SyncFinanzasUseCase @Inject constructor(
    private val repository: OptoRepository,
    private val supabase: SupabaseClient,
    private val syncStateTracker: com.example.optoapp.data.SyncStateTracker,
    private val mergeHandler: DispensacionMergeHandler
) {
    companion object {
        private const val TAG = "SyncFinanzas"
        private const val TABLE_DISPENSACIONES = "dispensaciones"
        private const val TABLE_PAGOS          = "pagos"
        private const val TABLE_SERVICIOS      = "servicios_extra"
        private const val UPSERT_BATCH_SIZE = 80
        private const val NETWORK_RETRY_ATTEMPTS = 3
    }

    /**
     * Ejecuta la sincronización completa del módulo financiero (Upload -> Download).
     */
    suspend operator fun invoke(
        opticaId: String,
        downloadAfterUpload: Boolean = true
    ): Resource<FinanzasSyncResult> {
        return try {
            Log.d(TAG, "Finanzas: inicio (opticaId=$opticaId, download=$downloadAfterUpload)")

            pushPendingDeletions(opticaId)

            val dispUp = uploadDispensaciones(opticaId)
            Log.d(TAG, "Finanzas: upload dispensaciones=$dispUp")
            val servUp = uploadServicios(opticaId)
            Log.d(TAG, "Finanzas: upload servicios_extra=$servUp")
            val pagosUp = uploadPagos(opticaId)
            Log.d(TAG, "Finanzas: upload pagos=$pagosUp")

            val dispDown: Int
            val servDown: Int
            val pagosDown: Int
            if (downloadAfterUpload) {
                dispDown = downloadDispensaciones(opticaId)
                Log.d(TAG, "Finanzas: download dispensaciones=$dispDown")
                servDown = downloadServicios(opticaId)
                Log.d(TAG, "Finanzas: download servicios_extra=$servDown")
                pagosDown = downloadPagos(opticaId)
                Log.d(TAG, "Finanzas: download pagos=$pagosDown; fin OK")
            } else {
                dispDown = 0
                servDown = 0
                pagosDown = 0
                Log.d(TAG, "Finanzas: fin upload-only OK")
            }

            Resource.Success(
                FinanzasSyncResult(
                    uploadedDispensaciones = dispUp,
                    uploadedServicios = servUp,
                    uploadedPagos = pagosUp,
                    downloadedDispensaciones = dispDown,
                    downloadedServicios = servDown,
                    downloadedPagos = pagosDown
                )
            )
        } catch (e: Exception) {
            rethrowIfCancellation(e)
            Log.e(TAG, "Error en sincronización financiera", e)
            Resource.Error("Error sincronizando finanzas: ${e.localizedMessage}")
        }
    }

    private suspend fun pushPendingDeletions(opticaId: String) {
        val pending = repository.getPendingDeletions(opticaId)
        if (pending.isEmpty()) return
        Log.d(TAG, "Finanzas: propagando ${pending.size} eliminaciones a Supabase")
        pending.forEach { tombstone ->
            val table = when (tombstone.entityType) {
                "servicio_extra" -> TABLE_SERVICIOS
                "dispensacion"   -> TABLE_DISPENSACIONES
                "pago"           -> TABLE_PAGOS
                else             -> null
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
                Log.d(TAG, "Eliminado remoto ${tombstone.entityType}/${tombstone.entityId}")
            } catch (e: Exception) {
                rethrowIfCancellation(e)
                Log.w(TAG, "No se pudo eliminar remoto ${tombstone.entityType}/${tombstone.entityId}: ${e.message}")
            }
        }
    }

    private suspend fun uploadDispensaciones(opticaId: String): Int {
        mergeHandler.resolveLocalDuplicateDispensaciones(opticaId)
        val dispensaciones = repository.getDispensacionesSnapshotForOptica(opticaId)
        if (dispensaciones.isEmpty()) {
            syncStateTracker.markSynced(opticaId, "upload_dispensaciones", "batch")
            return 0
        }
        val localById = dispensaciones.associateBy { it.id }
        val opticaRemota = opticaId.trim().ifBlank { FinanzasRemoteDefaults.OPTICA_ID_FALLBACK }
        val remotosExistentes = try {
            supabase.postgrest[TABLE_DISPENSACIONES]
                .select {
                    filter { eq("optica_id", opticaRemota) }
                }
                .decodeList<DispensacionRemotaLookup>()
        } catch (e: Exception) {
            rethrowIfCancellation(e)
            Log.w(TAG, "No se pudo consultar dispensaciones remotas para reconciliar OT: ${e.message}")
            emptyList()
        }
        val remoteIdByOt = remotosExistentes
            .mapNotNull { r ->
                normalizedOtForUnique(r.ot)?.let { key -> key to r.id }
            }
            .toMap()
        val uniqueRows = LinkedHashMap<String, Pair<String, DispensacionRemota>>()
        dispensaciones.forEach { dispensacion ->
            val base = dispensacion.toRemoto().copy(opticaId = opticaRemota)
            val normalizedOt = normalizedOtForUnique(base.ot)
            val reconciled = if (normalizedOt != null) {
                val existingRemoteId = remoteIdByOt[normalizedOt]
                if (existingRemoteId != null && existingRemoteId != base.id) {
                    base.copy(id = existingRemoteId)
                } else {
                    base
                }
            } else {
                base
            }
            val dedupeKey = normalizedOt?.let { "ot:$it" } ?: "id:${reconciled.id}"
            if (uniqueRows.containsKey(dedupeKey)) {
                val canonicalLocalId = uniqueRows[dedupeKey]?.first.orEmpty()
                val canonicalLocal = localById[canonicalLocalId]
                val duplicateLocal = localById[dispensacion.id]
                if (canonicalLocal != null && duplicateLocal != null) {
                    mergeHandler.mergeLocalDispensacionConflict(
                        opticaId = opticaId,
                        canonical = canonicalLocal,
                        duplicate = duplicateLocal
                    )
                } else {
                    Log.w(TAG, "OT duplicada en lote sin datos locales para fusión (dedupeKey=$dedupeKey, localId=${dispensacion.id})")
                }
                return@forEach
            }
            uniqueRows[dedupeKey] = dispensacion.id to reconciled
        }
        val uniqueById = LinkedHashMap<String, Pair<String, DispensacionRemota>>()
        uniqueRows.values.forEach { (localId, row) ->
            if (uniqueById.containsKey(row.id)) {
                val firstLocalId = uniqueById[row.id]?.first.orEmpty()
                val canonicalLocal = localById[firstLocalId]
                val duplicateLocal = localById[localId]
                if (canonicalLocal != null && duplicateLocal != null) {
                    mergeHandler.mergeLocalDispensacionConflict(
                        opticaId = opticaId,
                        canonical = canonicalLocal,
                        duplicate = duplicateLocal
                    )
                } else {
                    Log.w(TAG, "Conflicto de reconciliación sin datos locales para fusión ($localId -> $firstLocalId)")
                }
                return@forEach
            }
            uniqueById[row.id] = localId to row
        }
        val rows = uniqueById.values.map { it.second }
        try {
            rows.chunked(UPSERT_BATCH_SIZE).forEachIndexed { index, chunk ->
                retryNetwork("upsert:$TABLE_DISPENSACIONES:chunk${index + 1}") {
                    supabase.postgrest[TABLE_DISPENSACIONES].upsert(chunk)
                }
            }
        } catch (e: Exception) {
            rethrowIfCancellation(e)
            syncStateTracker.markError(opticaId, "upload_dispensaciones", "batch", e.message)
            throw e
        }
        syncStateTracker.markSynced(opticaId, "upload_dispensaciones", "batch")
        uniqueById.values.forEach { (localId, _) ->
            syncStateTracker.markSynced(opticaId, "dispensacion", localId)
        }
        return rows.size
    }

    private suspend fun uploadServicios(opticaId: String): Int {
        val servicios = repository.getServiciosSnapshotForOptica(opticaId)
        if (servicios.isEmpty()) {
            syncStateTracker.markSynced(opticaId, "upload_servicios_extra", "batch")
            return 0
        }
        val opticaRemota = opticaId.trim().ifBlank { FinanzasRemoteDefaults.OPTICA_ID_FALLBACK }
        val remotosExistentes = try {
            supabase.postgrest[TABLE_SERVICIOS]
                .select {
                    filter { eq("optica_id", opticaRemota) }
                }
                .decodeList<ServicioRemotoLookup>()
        } catch (e: Exception) {
            rethrowIfCancellation(e)
            Log.w(TAG, "No se pudo consultar servicios remotos para reconciliar OT: ${e.message}")
            emptyList()
        }
        val remoteIdByOt = remotosExistentes
            .mapNotNull { r ->
                normalizedOtForUnique(r.ot)?.let { key -> key to r.id }
            }
            .toMap()

        val uniqueRows = LinkedHashMap<String, ServicioRemoto>()
        servicios.forEach { servicio ->
            val base = servicio.toRemoto().copy(opticaId = opticaRemota)
            val normalizedOt = normalizedOtForUnique(base.ot)
            val reconciled = if (normalizedOt != null) {
                val existingRemoteId = remoteIdByOt[normalizedOt]
                if (existingRemoteId != null && existingRemoteId != base.id) {
                    base.copy(id = existingRemoteId)
                } else {
                    base
                }
            } else {
                base
            }
            val dedupeKey = normalizedOt?.let { "ot:$it" } ?: "id:${reconciled.id}"
            uniqueRows[dedupeKey] = reconciled
        }
        val rows = uniqueRows.values.toList().distinctBy { it.id }
        try {
            rows.chunked(UPSERT_BATCH_SIZE).forEachIndexed { index, chunk ->
                retryNetwork("upsert:$TABLE_SERVICIOS:chunk${index + 1}") {
                    supabase.postgrest[TABLE_SERVICIOS].upsert(chunk)
                }
            }
        } catch (e: Exception) {
            rethrowIfCancellation(e)
            syncStateTracker.markError(opticaId, "upload_servicios_extra", "batch", e.message)
            throw e
        }
        syncStateTracker.markSynced(opticaId, "upload_servicios_extra", "batch")
        servicios.forEach { s ->
            syncStateTracker.markSynced(opticaId, "servicio_extra", s.id)
        }
        return servicios.size
    }

    private suspend fun uploadPagos(opticaId: String): Int {
        val pagos = repository.getPagosSnapshotForOptica(opticaId)
        if (pagos.isEmpty()) {
            syncStateTracker.markSynced(opticaId, "upload_pagos", "batch")
            return 0
        }
        val opticaRemota = opticaId.trim().ifBlank { FinanzasRemoteDefaults.OPTICA_ID_FALLBACK }
        val rows = pagos.map { it.toRemoto().copy(opticaId = opticaRemota) }.distinctBy { it.id }
        try {
            rows.chunked(UPSERT_BATCH_SIZE).forEachIndexed { index, chunk ->
                retryNetwork("upsert:$TABLE_PAGOS:chunk${index + 1}") {
                    supabase.postgrest[TABLE_PAGOS].upsert(chunk)
                }
            }
        } catch (e: Exception) {
            rethrowIfCancellation(e)
            syncStateTracker.markError(opticaId, "upload_pagos", "batch", e.message)
            throw e
        }
        syncStateTracker.markSynced(opticaId, "upload_pagos", "batch")
        pagos.forEach { p ->
            syncStateTracker.markSynced(opticaId, "pago", p.id)
        }
        return pagos.size
    }

    private suspend fun retryNetwork(
        opName: String,
        block: suspend () -> Unit
    ) {
        var lastError: Exception? = null
        repeat(NETWORK_RETRY_ATTEMPTS) { attempt ->
            try {
                block()
                return
            } catch (e: Exception) {
                rethrowIfCancellation(e)
                lastError = e
                val shouldRetry = isTransientNetworkError(e)
                if (!shouldRetry || attempt == NETWORK_RETRY_ATTEMPTS - 1) throw e
                val backoffMs = 400L * (attempt + 1)
                Log.w(TAG, "$opName fallo de red (intento ${attempt + 1}/$NETWORK_RETRY_ATTEMPTS). Reintentando en ${backoffMs}ms")
                delay(backoffMs)
            }
        }
        lastError?.let { throw it }
    }

    internal fun isTransientNetworkError(e: Exception): Boolean {
        val msg = e.message?.lowercase().orEmpty()
        return msg.contains("timeout") ||
            msg.contains("timed out") ||
            msg.contains("connect") && msg.contains("failed") ||
            msg.contains("unable to resolve host") ||
            msg.contains("network is unreachable") ||
            msg.contains("connection reset")
    }

    /** IDs marcados para eliminación que NO deben reinsertarse al bajar de la nube. */
    private suspend fun deletedIds(opticaId: String): Set<String> {
        return repository.getPendingDeletions(opticaId).map { it.entityId }.toSet()
    }

    private suspend fun downloadDispensaciones(opticaId: String): Int {
        val skipIds = deletedIds(opticaId)
        val remotos = supabase.postgrest[TABLE_DISPENSACIONES].select { filter { eq("optica_id", opticaId) } }.decodeList<DispensacionRemota>()
        remotos.forEach { r ->
            if (r.id in skipIds) return@forEach
            try {
                val local = r.toEntity()
                repository.insertDispensacion(local)
                syncStateTracker.markSynced(opticaId, "dispensacion", local.id)
            } catch (e: Exception) {
                rethrowIfCancellation(e)
                syncStateTracker.markError(opticaId, "dispensacion", r.id, e.message)
            }
        }
        return remotos.size
    }

    private suspend fun downloadServicios(opticaId: String): Int {
        val skipIds = deletedIds(opticaId)
        val remotos = supabase.postgrest[TABLE_SERVICIOS].select { filter { eq("optica_id", opticaId) } }.decodeList<ServicioRemoto>()
        remotos.forEach { r ->
            if (r.id in skipIds) return@forEach
            try {
                val local = r.toEntity()
                repository.insertServicio(local)
                syncStateTracker.markSynced(opticaId, "servicio_extra", local.id)
            } catch (e: Exception) {
                rethrowIfCancellation(e)
                syncStateTracker.markError(opticaId, "servicio_extra", r.id, e.message)
            }
        }
        return remotos.size
    }

    private suspend fun downloadPagos(opticaId: String): Int {
        val skipIds = deletedIds(opticaId)
        val remotos = supabase.postgrest[TABLE_PAGOS]
            .select { filter { eq("optica_id", opticaId) } }
            .decodeList<PagoRemoto>()
        remotos.forEach { r ->
            if (r.id in skipIds) return@forEach
            try {
                val local = r.toEntity()
                repository.insertPago(local)
                syncStateTracker.markSynced(opticaId, "pago", local.id)
            } catch (e: Exception) {
                rethrowIfCancellation(e)
                syncStateTracker.markError(opticaId, "pago", r.id, e.message)
            }
        }
        return remotos.size
    }
}
