package com.example.optoapp.domain

import android.util.Log
import com.example.optoapp.data.DispensacionItem
import com.example.optoapp.data.DispensacionOptica
import com.example.optoapp.data.FinanzasRemoteDefaults
import com.example.optoapp.data.OptoRepository
import com.example.optoapp.data.SyncStateTracker
import com.example.optoapp.domain.sync.EntitySnapshotSerializer
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.CancellationException
import java.io.IOException
import javax.inject.Inject

/**
 * Uploads dispensaciones, servicios_extra and pagos to Supabase
 * with deduplication, reconciliation, and retry.
 * Extracted from [SyncFinanzasUseCase].
 */
class UploadSyncCoordinator @Inject constructor(
    private val repository: OptoRepository,
    private val supabase: SupabaseClient,
    private val syncStateTracker: SyncStateTracker,
    private val mergeHandler: DispensacionMergeHandler,
    private val networkRetryHelper: NetworkRetryHelper,
    private val conflictHelper: com.example.optoapp.domain.sync.ConflictHelper
) {
    companion object {
        private const val TAG = "SyncFinanzas"
        private const val TABLE_DISPENSACIONES = "dispensaciones"
        private const val TABLE_DISPENSACION_ITEMS = "dispensacion_items"
        private const val TABLE_PAGOS = "pagos"
        private const val TABLE_SERVICIOS = "servicios_extra"
        private const val TABLE_ARQUEO_CAJA = "arqueo_caja"
        private const val UPSERT_BATCH_SIZE = 80
    }

    suspend fun uploadDispensaciones(opticaId: String): Int {
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
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            Log.w(TAG, "Error en red consultando dispensaciones remotas: ${e.message}")
            emptyList()
        } catch (e: Exception) {
            Log.w(TAG, "Error inesperado consultando dispensaciones remotas: ${e.message}")
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
        val safeIds = conflictHelper.filterConflicts(
            tableName = TABLE_DISPENSACIONES,
            opticaId = opticaId,
            entityType = "dispensacion",
            localEntities = rows.map { com.example.optoapp.domain.sync.LocalEntity(it.id, it.updatedAt, EntitySnapshotSerializer.serialize(it)) }
        ).map { it.id }.toSet()
        val safeRows = rows.filter { it.id in safeIds }
        try {
            safeRows.chunked(UPSERT_BATCH_SIZE).forEachIndexed { index, chunk ->
                networkRetryHelper.retryNetwork("upsert:$TABLE_DISPENSACIONES:chunk${index + 1}") {
                    supabase.postgrest[TABLE_DISPENSACIONES].upsert(chunk)
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            Log.e(TAG, "Error en red subiendo dispensaciones: ${e.message}", e)
            syncStateTracker.markError(opticaId, "upload_dispensaciones", "batch", e.message)
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error inesperado subiendo dispensaciones: ${e.message}", e)
            syncStateTracker.markError(opticaId, "upload_dispensaciones", "batch", e.message)
            throw e
        }
        syncStateTracker.markSynced(opticaId, "upload_dispensaciones", "batch")
        uniqueById.values.forEach { (localId, _) ->
            syncStateTracker.markSynced(opticaId, "dispensacion", localId)
        }
        return rows.size
    }

    suspend fun uploadServicios(opticaId: String): Int {
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
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            Log.w(TAG, "Error en red consultando servicios remotos: ${e.message}")
            emptyList()
        } catch (e: Exception) {
            Log.w(TAG, "Error inesperado consultando servicios remotos: ${e.message}")
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
        val safeIds = conflictHelper.filterConflicts(
            tableName = TABLE_SERVICIOS,
            opticaId = opticaId,
            entityType = "servicio_extra",
            localEntities = rows.map { com.example.optoapp.domain.sync.LocalEntity(it.id, it.updatedAt, EntitySnapshotSerializer.serialize(it)) }
        ).map { it.id }.toSet()
        val safeRows = rows.filter { it.id in safeIds }
        try {
            safeRows.chunked(UPSERT_BATCH_SIZE).forEachIndexed { index, chunk ->
                networkRetryHelper.retryNetwork("upsert:$TABLE_SERVICIOS:chunk${index + 1}") {
                    supabase.postgrest[TABLE_SERVICIOS].upsert(chunk)
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            Log.e(TAG, "Error en red subiendo servicios extra: ${e.message}", e)
            syncStateTracker.markError(opticaId, "upload_servicios_extra", "batch", e.message)
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error inesperado subiendo servicios extra: ${e.message}", e)
            syncStateTracker.markError(opticaId, "upload_servicios_extra", "batch", e.message)
            throw e
        }
        syncStateTracker.markSynced(opticaId, "upload_servicios_extra", "batch")
        servicios.forEach { s ->
            syncStateTracker.markSynced(opticaId, "servicio_extra", s.id)
        }
        return servicios.size
    }

    suspend fun uploadDispensacionItems(opticaId: String): Int {
        val items = repository.getDispensacionItemsSnapshotForOptica(opticaId)
        if (items.isEmpty()) {
            syncStateTracker.markSynced(opticaId, "upload_dispensacion_items", "batch")
            return 0
        }
        val opticaRemota = opticaId.trim().ifBlank { FinanzasRemoteDefaults.OPTICA_ID_FALLBACK }
        val rows = items.map { it.toRemoto().copy(opticaId = opticaRemota) }.distinctBy { it.id }
        val safeIds = conflictHelper.filterConflicts(
            tableName = TABLE_DISPENSACION_ITEMS,
            opticaId = opticaId,
            entityType = "dispensacion_item",
            localEntities = rows.map { com.example.optoapp.domain.sync.LocalEntity(it.id, "", EntitySnapshotSerializer.serialize(it)) }
        ).map { it.id }.toSet()
        val safeRows = rows.filter { it.id in safeIds }
        try {
            safeRows.chunked(UPSERT_BATCH_SIZE).forEachIndexed { index, chunk ->
                networkRetryHelper.retryNetwork("upsert:$TABLE_DISPENSACION_ITEMS:chunk${index + 1}") {
                    supabase.postgrest[TABLE_DISPENSACION_ITEMS].upsert(chunk)
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            Log.e(TAG, "Error en red subiendo items de dispensación: ${e.message}", e)
            syncStateTracker.markError(opticaId, "upload_dispensacion_items", "batch", e.message)
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error inesperado subiendo items de dispensación: ${e.message}", e)
            syncStateTracker.markError(opticaId, "upload_dispensacion_items", "batch", e.message)
            throw e
        }
        syncStateTracker.markSynced(opticaId, "upload_dispensacion_items", "batch")
        items.forEach { item ->
            syncStateTracker.markSynced(opticaId, "dispensacion_item", item.id)
        }
        return items.size
    }

    suspend fun uploadPagos(opticaId: String): Int {
        val pagos = repository.getPagosSnapshotForOptica(opticaId)
        if (pagos.isEmpty()) {
            syncStateTracker.markSynced(opticaId, "upload_pagos", "batch")
            return 0
        }
        val opticaRemota = opticaId.trim().ifBlank { FinanzasRemoteDefaults.OPTICA_ID_FALLBACK }
        val rows = pagos.map { it.toRemoto().copy(opticaId = opticaRemota) }.distinctBy { it.id }
        val safeIds = conflictHelper.filterConflicts(
            tableName = TABLE_PAGOS,
            opticaId = opticaId,
            entityType = "pago",
            localEntities = rows.map { com.example.optoapp.domain.sync.LocalEntity(it.id, it.updatedAt, EntitySnapshotSerializer.serialize(it)) }
        ).map { it.id }.toSet()
        val safeRows = rows.filter { it.id in safeIds }
        try {
            safeRows.chunked(UPSERT_BATCH_SIZE).forEachIndexed { index, chunk ->
                networkRetryHelper.retryNetwork("upsert:$TABLE_PAGOS:chunk${index + 1}") {
                    supabase.postgrest[TABLE_PAGOS].upsert(chunk)
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            Log.e(TAG, "Error en red subiendo pagos: ${e.message}", e)
            syncStateTracker.markError(opticaId, "upload_pagos", "batch", e.message)
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error inesperado subiendo pagos: ${e.message}", e)
            syncStateTracker.markError(opticaId, "upload_pagos", "batch", e.message)
            throw e
        }
        syncStateTracker.markSynced(opticaId, "upload_pagos", "batch")
        pagos.forEach { p ->
            syncStateTracker.markSynced(opticaId, "pago", p.id)
        }
        return pagos.size
    }

    suspend fun uploadArqueos(opticaId: String): Int {
        val localArqueos = repository.getArqueosByOpticaList(opticaId)
        if (localArqueos.isEmpty()) return 0

        val safeIds = conflictHelper.filterConflicts(
            tableName = TABLE_ARQUEO_CAJA,
            opticaId = opticaId,
            entityType = "arqueo_caja",
            localEntities = localArqueos.map { com.example.optoapp.domain.sync.LocalEntity(it.id, it.updatedAt, EntitySnapshotSerializer.serialize(it)) }
        ).map { it.id }.toSet()

        var uploaded = 0
        localArqueos.forEach { arqueo ->
            if (arqueo.id !in safeIds) return@forEach
            try {
                supabase.postgrest[TABLE_ARQUEO_CAJA].upsert(arqueo.toRemota())
                uploaded++
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "arqueo upload failed for ${arqueo.id}", e)
            }
        }
        return uploaded
    }
}
