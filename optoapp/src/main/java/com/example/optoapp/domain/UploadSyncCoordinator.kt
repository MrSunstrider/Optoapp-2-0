package com.example.optoapp.domain

import com.example.optoapp.data.OptoDatabase
import com.example.optoapp.data.OptoRepository
import com.example.optoapp.data.SyncStateTracker
import com.example.optoapp.data.DispensacionOptica
import com.example.optoapp.data.costobiselado.CostoBiseladoDao
import com.example.optoapp.data.costoproducto.CostoProductoDao
import com.example.optoapp.util.AppLogger
import androidx.room.withTransaction
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.io.IOException
import java.time.Instant
import javax.inject.Inject

/**
 * Extracted from [SyncFinanzasUseCase] so each entity type can be uploaded independently
 * while sharing deduplication (OT-based reconciliation), retry, and state tracking.
 */
open class UploadSyncCoordinator @Inject constructor(
    private val repository: OptoRepository,
    private val supabase: SupabaseClient,
    private val database: OptoDatabase,
    private val syncStateTracker: SyncStateTracker,
    private val mergeHandler: DispensacionMergeHandler,
    private val networkRetryHelper: NetworkRetryHelper,
    private val costoProductoDao: CostoProductoDao,
    private val costoBiseladoDao: CostoBiseladoDao,
) {
    companion object {
        private const val TAG = "SyncFinanzas"
        private const val TABLE_DISPENSACIONES = "dispensaciones"
        private const val TABLE_DISPENSACION_ITEMS = "dispensacion_items"
        private const val TABLE_PAGOS = "pagos"
        private const val TABLE_SERVICIOS = "servicios_extra"
        private const val TABLE_GASTOS_OPERATIVOS = "gastos_operativos"
        private const val TABLE_REGALOS = "regalos_dispensacion"
        private const val TABLE_COSTOS_PRODUCTOS = "costos_productos"
        private const val TABLE_COSTOS_BISELADO = "costos_biselado"
        private const val UPSERT_BATCH_SIZE = 80

        // ── RPC helpers (T2.2) ─────────────────────────────────────────

        internal fun buildAdjustStockParams(
            monturaId: String,
            opticaId: String,
            delta: Int,
            referenceId: String,
            note: String,
            tipo: String,
            fecha: String,
        ): JsonObject = buildJsonObject {
            put("p_montura_id", monturaId)
            put("p_optica_id", opticaId)
            put("p_delta", delta)
            put("p_reference_id", referenceId)
            put("p_note", note)
            put("p_tipo", tipo)
            put("p_fecha", fecha)
        }

        internal fun parseAdjustStockOk(response: JsonObject): Boolean =
            response["ok"]?.jsonPrimitive?.let { it.content == "true" } ?: false

        internal fun parseAdjustStockNewStock(response: JsonObject): Int? =
            response["new_stock"]?.jsonPrimitive?.content?.toIntOrNull()

        internal fun parseAdjustStockError(response: JsonObject): String? =
            response["error"]?.jsonPrimitive?.content
    }

    class UploadPreCheckFailedException(
        message: String,
        cause: Throwable,
    ) : Exception(message, cause)

    // WHY: testability seam — Room's withTransaction is an extension function on
    // RoomDatabase that cannot be mocked by MockK. Making this open lets tests
    // override it to run the block inline without a real database transaction.
    internal open suspend fun <T> runInTransaction(block: suspend () -> T): T =
        database.withTransaction(block)

    // WHY: kotlinx.serialization cannot resolve erased generic type parameters,
    // so serialization must happen inside upsertBlock at the call site
    private suspend fun <R> executeSimpleUpsert(
        opticaId: String,
        tableName: String,
        entityType: String,
        batchTrackingType: String,
        rows: List<R>,
        idSelector: (R) -> String,
        upsertBlock: suspend (List<R>) -> Unit,
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
            AppLogger.e(TAG, "Error en red subiendo $entityType: ${e.message}", e)
            syncStateTracker.markError(opticaId, batchTrackingType, "batch", e.message)
            throw UploadPartialException(uploadedCount, e)
        } catch (e: Exception) {
            AppLogger.e(TAG, "Error inesperado subiendo $entityType: ${e.message}", e)
            syncStateTracker.markError(opticaId, batchTrackingType, "batch", e.message)
            throw e
        }
        runInTransaction {
            rows.forEach { r ->
                syncStateTracker.markSynced(opticaId, entityType, idSelector(r))
            }
        }
        syncStateTracker.markSynced(opticaId, batchTrackingType, "batch")
        return uploadedCount
    }

    suspend fun uploadDispensaciones(opticaId: String): Int {
        require(opticaId.isNotBlank()) { "opticaId must not be blank for upload" }
        mergeHandler.resolveLocalDuplicateDispensaciones(opticaId)
        val dispensaciones = repository.getDispensacionesSnapshotForOptica(opticaId)
        if (dispensaciones.isEmpty()) {
            syncStateTracker.markSynced(opticaId, "upload_dispensaciones", "batch")
            return 0
        }
        // Snapshot of items used for RPC stock adjustment after upsert.
        val items = repository.getDispensacionItemsSnapshotForOptica(opticaId)
        val allPagos = repository.getPagosSnapshotForOptica(opticaId)
        val pagosSumByDisp = allPagos
            .filter { it.tipo != "Anulación" && it.dispensacionId != null }
            .groupBy { it.dispensacionId!! }
            .mapValues { (_, pags) -> pags.sumOf { it.monto } }
        val localById = dispensaciones.associateBy { it.id }
        val opticaRemota = opticaId.trim()
        val remotosExistentes = try {
            supabase.postgrest[TABLE_DISPENSACIONES]
                .select {
                    filter { eq("optica_id", opticaRemota) }
                }
                .decodeList<DispensacionRemotaLookup>()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AppLogger.e(TAG, "FATAL: Cannot reconcile with remote. Aborting to prevent duplicates.", e)
            throw UploadPreCheckFailedException("Reconciliation fetch failed for $TABLE_DISPENSACIONES", e)
        }
        val remoteIdByOt = remotosExistentes
            .mapNotNull { r ->
                normalizedOtForUnique(r.ot)?.let { key -> key to r.id }
            }
            .toMap()
        val deferredMerges = mutableListOf<Pair<DispensacionOptica, DispensacionOptica>>()
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
                    deferredMerges.add(canonicalLocal to duplicateLocal)
                } else {
                    AppLogger.w(TAG, "OT duplicada en lote sin datos locales para fusión (dedupeKey=$dedupeKey, localId=${dispensacion.id})")
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
                    deferredMerges.add(canonicalLocal to duplicateLocal)
                } else {
                    AppLogger.w(TAG, "Conflicto de reconciliación sin datos locales para fusión ($localId -> $firstLocalId)")
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

                // After chunk upsert, adjust stock via RPC for each
                // dispensacion item that has a non-blank monturaId.
                val chunkDispIds = chunk.map { it.id }.toSet()
                items.filter { it.dispensacionId in chunkDispIds && it.monturaId.isNotBlank() }
                    .forEach { item ->
                        try {
                            val params = buildAdjustStockParams(
                                monturaId = item.monturaId,
                                opticaId = item.opticaId,
                                delta = -1,
                                referenceId = item.dispensacionId,
                                note = "venta_dispensacion",
                                tipo = "venta",
                                fecha = localById[item.dispensacionId]?.fecha?.toString() ?: "",
                            )
                            val result = supabase.postgrest.rpc(
                                "rpc_adjust_montura_stock",
                                params,
                            ).decodeAs<JsonObject>()
                            val ok = parseAdjustStockOk(result)
                            if (!ok) {
                                val error = parseAdjustStockError(result) ?: "unknown"
                                AppLogger.w(TAG, "RPC adjust stock failed: $error (disp=${item.dispensacionId}, montura=${item.monturaId})")
                            }
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            AppLogger.e(TAG, "RPC adjust stock exception: ${e.message} (disp=${item.dispensacionId}, montura=${item.monturaId})", e)
                        }
                    }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            AppLogger.e(TAG, "Error en red subiendo dispensaciones: ${e.message}", e)
            syncStateTracker.markError(opticaId, "upload_dispensaciones", "batch", e.message)
            throw UploadPartialException(uploadedCount, e)
        } catch (e: Exception) {
            AppLogger.e(TAG, "Error inesperado subiendo dispensaciones: ${e.message}", e)
            syncStateTracker.markError(opticaId, "upload_dispensaciones", "batch", e.message)
            throw e
        }
        deferredMerges.forEach { (canonical, duplicate) ->
            mergeHandler.mergeLocalDispensacionConflict(
                opticaId = opticaId,
                canonical = canonical,
                duplicate = duplicate,
            )
        }
        runInTransaction {
            uniqueById.values.forEach { (localId, _) ->
                syncStateTracker.markSynced(opticaId, "dispensacion", localId)
            }
        }
        syncStateTracker.markSynced(opticaId, "upload_dispensaciones", "batch")
        return rows.size
    }

    suspend fun uploadServicios(opticaId: String): Int {
        val servicios = repository.getServiciosSnapshotForOptica(opticaId)
        if (servicios.isEmpty()) {
            syncStateTracker.markSynced(opticaId, "upload_servicios_extra", "batch")
            return 0
        }
        val allPagosServ = repository.getPagosSnapshotForOptica(opticaId)
        val aCuentaSumByServ = allPagosServ
            .filter { it.tipo != "Anulación" && it.servicioExtraId != null }
            .groupBy { it.servicioExtraId!! }
            .mapValues { (_, pags) -> pags.sumOf { it.monto } }
        require(opticaId.isNotBlank()) { "opticaId must not be blank for upload" }
        val opticaRemota = opticaId.trim()
        val remotosExistentes = try {
            fetchRemoteServiciosForLookup(opticaRemota)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AppLogger.e(TAG, "FATAL: Cannot reconcile with remote. Aborting to prevent duplicates.", e)
            throw UploadPreCheckFailedException("Reconciliation fetch failed for $TABLE_SERVICIOS", e)
        }
        val remoteIdByOt = remotosExistentes
            .mapNotNull { r ->
                normalizedOtForUnique(r.ot)?.let { key -> key to r.id }
            }
            .toMap()

        val uniqueById = LinkedHashMap<String, Pair<String, ServicioRemoto>>()
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
            if (uniqueById.containsKey(reconciled.id)) return@forEach
            uniqueById[reconciled.id] = servicio.id to reconciled
        }
        val rows = uniqueById.values.map { it.second }
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
            AppLogger.e(TAG, "Error en red subiendo servicios extra: ${e.message}", e)
            syncStateTracker.markError(opticaId, "upload_servicios_extra", "batch", e.message)
            throw UploadPartialException(uploadedCount, e)
        } catch (e: Exception) {
            AppLogger.e(TAG, "Error inesperado subiendo servicios extra: ${e.message}", e)
            syncStateTracker.markError(opticaId, "upload_servicios_extra", "batch", e.message)
            throw e
        }
        runInTransaction {
            uniqueById.values.forEach { (localId, _) ->
                syncStateTracker.markSynced(opticaId, "servicio_extra", localId)
            }
        }
        syncStateTracker.markSynced(opticaId, "upload_servicios_extra", "batch")
        return rows.size
    }

    suspend fun uploadDispensacionItems(opticaId: String): Int {
        val items = repository.getDispensacionItemsSnapshotForOptica(opticaId)
        if (items.isEmpty()) {
            syncStateTracker.markSynced(opticaId, "upload_dispensacion_items", "batch")
            return 0
        }
        require(opticaId.isNotBlank()) { "opticaId must not be blank for upload" }
        val opticaRemota = opticaId.trim()
        val rows = items.map { it.toRemoto().copy(opticaId = opticaRemota) }.distinctBy { it.id }
        return executeSimpleUpsert(
            opticaId,
            TABLE_DISPENSACION_ITEMS,
            "dispensacion_item",
            "upload_dispensacion_items",
            rows,
            { it.id },
        ) { supabase.postgrest[TABLE_DISPENSACION_ITEMS].upsert(it) }
    }

    // WHY: testability seam — MockK cannot mock chained PostgREST DSL calls.
    // Test subclasses override this to return canned lookup data for reconciliation testing.
    internal open suspend fun fetchRemotePagosForLookup(opticaId: String): List<PagoRemotoLookup> {
        val remotos = supabase.postgrest[TABLE_PAGOS]
            .select { filter { eq("optica_id", opticaId) } }
            .decodeList<PagoRemotoLookup>()
        return remotos
    }

    // WHY: testability seam for servicios reconciliation (same pattern as pagos).
    internal open suspend fun fetchRemoteServiciosForLookup(opticaId: String): List<ServicioRemotoLookup> {
        return supabase.postgrest[TABLE_SERVICIOS]
            .select { filter { eq("optica_id", opticaId) } }
            .decodeList<ServicioRemotoLookup>()
    }

    // ── Pagos business-key reconciliation ─────────────────────────────

    internal data class PagoKey(
        val dispensacionId: String?,
        val tipo: String,
        val monto: Double,
        val metodoPago: String,
        val fecha: String,
    )

    suspend fun uploadPagos(opticaId: String): Int {
        val pagos = repository.getPagosSnapshotForOptica(opticaId)
        if (pagos.isEmpty()) {
            syncStateTracker.markSynced(opticaId, "upload_pagos", "batch")
            return 0
        }
        require(opticaId.isNotBlank()) { "opticaId must not be blank for upload" }
        val opticaRemota = opticaId.trim()
        val rows = pagos.map { it.toRemoto().copy(opticaId = opticaRemota) }.distinctBy { it.id }

        // Reconcile by business key (dispensacion_id, tipo, monto, metodo_pago, fecha)
        val remotos = try {
            fetchRemotePagosForLookup(opticaRemota)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AppLogger.e(TAG, "FATAL: Cannot reconcile pagos with remote. Aborting to prevent duplicates.", e)
            throw UploadPreCheckFailedException("Reconciliation fetch failed for $TABLE_PAGOS", e)
        }
        val remoteIdByKey = remotos.map { r ->
            PagoKey(r.dispensacionId ?: "", r.tipo, r.monto, r.metodoPago, r.fecha) to r.id
        }.toMap()

        // Build localId → reconciled row map with uniqueById dedup (C1 fix)
        val uniqueById = LinkedHashMap<String, Pair<String, PagoRemoto>>()
        rows.forEach { row ->
            val key = PagoKey(row.dispensacionId ?: "", row.tipo, row.monto, row.metodoPago, row.fecha)
            val remoteId = remoteIdByKey[key]
            val reconciled = if (remoteId != null && remoteId != row.id) row.copy(id = remoteId) else row
            if (uniqueById.containsKey(reconciled.id)) return@forEach
            uniqueById[reconciled.id] = row.id to reconciled
        }
        val uniqueRows = uniqueById.values.map { it.second }
        var uploadedCount = 0
        try {
            uniqueRows.chunked(UPSERT_BATCH_SIZE).forEachIndexed { index, chunk ->
                networkRetryHelper.retryNetwork("upsert:$TABLE_PAGOS:chunk${index + 1}") {
                    supabase.postgrest[TABLE_PAGOS].upsert(chunk)
                }
                uploadedCount += chunk.size
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            AppLogger.e(TAG, "Error en red subiendo pagos: ${e.message}", e)
            syncStateTracker.markError(opticaId, "upload_pagos", "batch", e.message)
            throw UploadPartialException(uploadedCount, e)
        } catch (e: Exception) {
            AppLogger.e(TAG, "Error inesperado subiendo pagos: ${e.message}", e)
            syncStateTracker.markError(opticaId, "upload_pagos", "batch", e.message)
            throw e
        }
        runInTransaction {
            uniqueById.values.forEach { (localId, _) ->
                syncStateTracker.markSynced(opticaId, "pago", localId)
            }
        }
        syncStateTracker.markSynced(opticaId, "upload_pagos", "batch")
        return uniqueRows.size
    }

    suspend fun uploadGastosOperativos(opticaId: String): Int {
        val localGastos = repository.getGastosOperativosList(opticaId)
        if (localGastos.isEmpty()) {
            syncStateTracker.markSynced(opticaId, "upload_gastos_operativos", "batch")
            return 0
        }
        require(opticaId.isNotBlank()) { "opticaId must not be blank for upload" }
        val opticaRemota = opticaId.trim()
        val rows = localGastos.map { it.toRemoto().copy(opticaId = opticaRemota) }.distinctBy { it.id }
        return executeSimpleUpsert(
            opticaId,
            TABLE_GASTOS_OPERATIVOS,
            "gasto_operativo",
            "upload_gastos_operativos",
            rows,
            { it.id },
        ) { supabase.postgrest[TABLE_GASTOS_OPERATIVOS].upsert(it) }
    }

    suspend fun uploadRegalos(opticaId: String): Int {
        val regalos = repository.getRegalosSnapshotForOptica(opticaId)
        if (regalos.isEmpty()) {
            syncStateTracker.markSynced(opticaId, "upload_regalos", "batch")
            return 0
        }
        require(opticaId.isNotBlank()) { "opticaId must not be blank for upload" }
        val opticaRemota = opticaId.trim()
        val rows = regalos.map { it.toRemoto().copy(opticaId = opticaRemota) }.distinctBy { it.id }
        return executeSimpleUpsert(
            opticaId,
            TABLE_REGALOS,
            "regalo_dispensacion",
            "upload_regalos",
            rows,
            { it.id },
        ) { supabase.postgrest[TABLE_REGALOS].upsert(it) }
    }

    suspend fun uploadCostosProductos(opticaId: String): Int {
        val localCostos = costoProductoDao.getByOpticaIdList(opticaId)
        if (localCostos.isEmpty()) {
            syncStateTracker.markSynced(opticaId, "upload_costos_productos", "batch")
            return 0
        }
        require(opticaId.isNotBlank()) { "opticaId must not be blank for upload" }
        val opticaRemota = opticaId.trim()
        val rows = localCostos.map { it.toRemoto().copy(opticaId = opticaRemota) }.distinctBy { it.id }
        return executeSimpleUpsert(
            opticaId,
            TABLE_COSTOS_PRODUCTOS,
            "costo_producto",
            "upload_costos_productos",
            rows,
            { it.id },
        ) { supabase.postgrest[TABLE_COSTOS_PRODUCTOS].upsert(it) }
    }

    suspend fun uploadCostosBiselado(opticaId: String): Int {
        val localBiselado = costoBiseladoDao.getByOpticaIdList(opticaId)
        if (localBiselado.isEmpty()) {
            syncStateTracker.markSynced(opticaId, "upload_costos_biselado", "batch")
            return 0
        }
        require(opticaId.isNotBlank()) { "opticaId must not be blank for upload" }
        val opticaRemota = opticaId.trim()
        val rows = localBiselado.map { it.toRemoto().copy(opticaId = opticaRemota) }.distinctBy { it.id }
        return executeSimpleUpsert(
            opticaId,
            TABLE_COSTOS_BISELADO,
            "costo_biselado",
            "upload_costos_biselado",
            rows,
            { it.id },
        ) { supabase.postgrest[TABLE_COSTOS_BISELADO].upsert(it) }
    }
}
