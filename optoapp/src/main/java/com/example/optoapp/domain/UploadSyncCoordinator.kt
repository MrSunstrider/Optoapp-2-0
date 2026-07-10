package com.example.optoapp.domain

import android.util.Log
import com.example.optoapp.data.DispensacionItem
import com.example.optoapp.data.DispensacionOptica
import com.example.optoapp.data.FinanzasRemoteDefaults
import com.example.optoapp.data.OptoRepository
import com.example.optoapp.data.SyncStateTracker
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.CancellationException
import java.io.IOException
import javax.inject.Inject

/**
 * Extracted from [SyncFinanzasUseCase] so each entity type can be uploaded independently
 * while sharing deduplication (OT-based reconciliation), retry, and state tracking.
 */
class UploadSyncCoordinator @Inject constructor(
    private val repository: OptoRepository,
    private val supabase: SupabaseClient,
    private val syncStateTracker: SyncStateTracker,
    private val mergeHandler: DispensacionMergeHandler,
    private val networkRetryHelper: NetworkRetryHelper
) {
    companion object {
        private const val TAG = "SyncFinanzas"
        private const val TABLE_DISPENSACIONES = "dispensaciones"
        private const val TABLE_DISPENSACION_ITEMS = "dispensacion_items"
        private const val TABLE_PAGOS = "pagos"
        private const val TABLE_SERVICIOS = "servicios_extra"
        private const val TABLE_GASTOS_OPERATIVOS = "gastos_operativos"
        private const val TABLE_REGALOS = "regalos_dispensacion"
        private const val UPSERT_BATCH_SIZE = 80
    }

    /**
     * Shared upload pipeline: chunk → retry → markSynced → track count.
     * Serialization must happen inside [upsertBlock] at the call site where
     * the concrete DTO type is known — kotlinx.serialization cannot resolve
     * erased generic type parameters.
     */
    private suspend fun <R> executeSimpleUpsert(
        opticaId: String,
        tableName: String,
        entityType: String,
        batchTrackingType: String,
        rows: List<R>,
        idSelector: (R) -> String,
        upsertBlock: suspend (List<R>) -> Unit
    ): Int {
        if (rows.isEmpty()) {
            syncStateTracker.markSynced(opticaId, batchTrackingType, "batch")
            return 0
        }
        var uploadedCount = 0
        try {
            rows.chunked(UPSERT_BATCH_SIZE).forEachIndexed { index, chunk ->
                networkRetryHelper.retryNetwork("upsert:$tableName:chunk${index + 1}") {
                    upsertBlock(chunk)
                }
                uploadedCount += chunk.size
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            Log.e(TAG, "Error en red subiendo $entityType: ${e.message}", e)
            syncStateTracker.markError(opticaId, batchTrackingType, "batch", e.message)
            throw UploadPartialException(uploadedCount, e)
        } catch (e: Exception) {
            Log.e(TAG, "Error inesperado subiendo $entityType: ${e.message}", e)
            syncStateTracker.markError(opticaId, batchTrackingType, "batch", e.message)
            throw e
        }
        syncStateTracker.markSynced(opticaId, batchTrackingType, "batch")
        rows.forEach { r ->
            syncStateTracker.markSynced(opticaId, entityType, idSelector(r))
        }
        return uploadedCount
    }

    suspend fun uploadDispensaciones(opticaId: String): Int {
        mergeHandler.resolveLocalDuplicateDispensaciones(opticaId)
        val dispensaciones = repository.getDispensacionesSnapshotForOptica(opticaId)
        if (dispensaciones.isEmpty()) {
            syncStateTracker.markSynced(opticaId, "upload_dispensaciones", "batch")
            return 0
        }
        // Compute montoPagado dynamically from pagos (montoPagado is @Ignore in entity)
        val allPagos = repository.getPagosSnapshotForOptica(opticaId)
        val pagosSumByDisp = allPagos
            .filter { it.tipo != "Anulación" && it.dispensacionId != null }
            .groupBy { it.dispensacionId!! }
            .mapValues { (_, pags) -> pags.sumOf { it.monto } }
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
            val pagosSum = pagosSumByDisp[dispensacion.id] ?: 0.0
            val base = dispensacion.toRemoto(pagosSum = pagosSum).copy(opticaId = opticaRemota)
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
        var uploadedCount = 0
        try {
            rows.chunked(UPSERT_BATCH_SIZE).forEachIndexed { index, chunk ->
                networkRetryHelper.retryNetwork("upsert:$TABLE_DISPENSACIONES:chunk${index + 1}") {
                    supabase.postgrest[TABLE_DISPENSACIONES].upsert(chunk)
                }
                uploadedCount += chunk.size
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            Log.e(TAG, "Error en red subiendo dispensaciones: ${e.message}", e)
            syncStateTracker.markError(opticaId, "upload_dispensaciones", "batch", e.message)
            throw UploadPartialException(uploadedCount, e)
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
        // Compute aCuenta dynamically from pagos (aCuenta is @Ignore in entity)
        val allPagosServ = repository.getPagosSnapshotForOptica(opticaId)
        val aCuentaSumByServ = allPagosServ
            .filter { it.tipo != "Anulación" && it.servicioExtraId != null }
            .groupBy { it.servicioExtraId!! }
            .mapValues { (_, pags) -> pags.sumOf { it.monto } }
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
            val aCuentaSum = aCuentaSumByServ[servicio.id] ?: 0.0
            val base = servicio.toRemoto(aCuentaSum = aCuentaSum).copy(opticaId = opticaRemota)
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
        var uploadedCount = 0
        try {
            rows.chunked(UPSERT_BATCH_SIZE).forEachIndexed { index, chunk ->
                networkRetryHelper.retryNetwork("upsert:$TABLE_SERVICIOS:chunk${index + 1}") {
                    supabase.postgrest[TABLE_SERVICIOS].upsert(chunk)
                }
                uploadedCount += chunk.size
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            Log.e(TAG, "Error en red subiendo servicios extra: ${e.message}", e)
            syncStateTracker.markError(opticaId, "upload_servicios_extra", "batch", e.message)
            throw UploadPartialException(uploadedCount, e)
        } catch (e: Exception) {
            Log.e(TAG, "Error inesperado subiendo servicios extra: ${e.message}", e)
            syncStateTracker.markError(opticaId, "upload_servicios_extra", "batch", e.message)
            throw e
        }
        syncStateTracker.markSynced(opticaId, "upload_servicios_extra", "batch")
        rows.forEach { r ->
            syncStateTracker.markSynced(opticaId, "servicio_extra", r.id)
        }
        return rows.size
    }

    suspend fun uploadDispensacionItems(opticaId: String): Int {
        val items = repository.getDispensacionItemsSnapshotForOptica(opticaId)
        if (items.isEmpty()) {
            syncStateTracker.markSynced(opticaId, "upload_dispensacion_items", "batch")
            return 0
        }
        val opticaRemota = opticaId.trim().ifBlank { FinanzasRemoteDefaults.OPTICA_ID_FALLBACK }
        val rows = items.map { it.toRemoto().copy(opticaId = opticaRemota) }.distinctBy { it.id }
        return executeSimpleUpsert(
            opticaId, TABLE_DISPENSACION_ITEMS, "dispensacion_item",
            "upload_dispensacion_items", rows, { it.id }
        ) { supabase.postgrest[TABLE_DISPENSACION_ITEMS].upsert(it) }
    }

    suspend fun uploadPagos(opticaId: String): Int {
        val pagos = repository.getPagosSnapshotForOptica(opticaId)
        if (pagos.isEmpty()) {
            syncStateTracker.markSynced(opticaId, "upload_pagos", "batch")
            return 0
        }
        val opticaRemota = opticaId.trim().ifBlank { FinanzasRemoteDefaults.OPTICA_ID_FALLBACK }
        val rows = pagos.map { it.toRemoto().copy(opticaId = opticaRemota) }.distinctBy { it.id }
        return executeSimpleUpsert(
            opticaId, TABLE_PAGOS, "pago",
            "upload_pagos", rows, { it.id }
        ) { supabase.postgrest[TABLE_PAGOS].upsert(it) }
    }

    suspend fun uploadGastosOperativos(opticaId: String): Int {
        val localGastos = repository.getGastosOperativosList(opticaId)
        if (localGastos.isEmpty()) {
            syncStateTracker.markSynced(opticaId, "upload_gastos_operativos", "batch")
            return 0
        }
        val opticaRemota = opticaId.trim().ifBlank { FinanzasRemoteDefaults.OPTICA_ID_FALLBACK }
        val rows = localGastos.map { it.toRemoto().copy(opticaId = opticaRemota) }.distinctBy { it.id }
        return executeSimpleUpsert(
            opticaId, TABLE_GASTOS_OPERATIVOS, "gasto_operativo",
            "upload_gastos_operativos", rows, { it.id }
        ) { supabase.postgrest[TABLE_GASTOS_OPERATIVOS].upsert(it) }
    }

    suspend fun uploadRegalos(opticaId: String): Int {
        val regalos = repository.getRegalosSnapshotForOptica(opticaId)
        if (regalos.isEmpty()) {
            syncStateTracker.markSynced(opticaId, "upload_regalos", "batch")
            return 0
        }
        val opticaRemota = opticaId.trim().ifBlank { FinanzasRemoteDefaults.OPTICA_ID_FALLBACK }
        val rows = regalos.map { it.toRemoto().copy(opticaId = opticaRemota) }.distinctBy { it.id }
        return executeSimpleUpsert(
            opticaId, TABLE_REGALOS, "regalo_dispensacion",
            "upload_regalos", rows, { it.id }
        ) { supabase.postgrest[TABLE_REGALOS].upsert(it) }
    }

}
