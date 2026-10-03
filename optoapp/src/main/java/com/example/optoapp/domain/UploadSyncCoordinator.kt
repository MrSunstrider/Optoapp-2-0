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
        private const val TABLE_SERVICIO_EXTRA_ITEMS = "servicio_extra_items"
        private const val TABLE_REGALOS_SERVICIO = "regalos_servicio_extra"
        private const val TABLE_COSTOS_PRODUCTOS = "costos_productos"
        private const val TABLE_COSTOS_BISELADO = "costos_biselado"
        private const val UPSERT_BATCH_SIZE = 80
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
        val snapshot = repository.getDispensacionesSnapshotForOptica(opticaId)
        if (snapshot.isEmpty()) {
            syncStateTracker.markSynced(opticaId, "upload_dispensaciones", "batch")
            return 0
        }
        val allPagos = repository.getPagosSnapshotForOptica(opticaId)
        val pagosSumByDisp = allPagos
            .filter { it.dispensacionId != null }
            .groupBy { it.dispensacionId!! }
            .mapValues { (_, pags) -> pags.sumOf { PagoEffect.signedAmount(it.tipo, it.monto) } }
        val opticaRemota = opticaId.trim()
        val remotosExistentes = try {
            fetchRemoteDispensacionesForLookup(opticaRemota)
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
        val dispensaciones = renumberCollidingClaimOts(snapshot, remotosExistentes, remoteIdByOt)
        val localById = dispensaciones.associateBy { it.id }
        val claimConflicts = detectClaimConflicts(dispensaciones, localById, remotosExistentes, remoteIdByOt)
        claimConflicts.forEach { (replacementId, reason) ->
            syncStateTracker.markError(opticaId, "dispensacion", replacementId, reason)
        }
        // WHY: while a claim conflict is unresolved the local original carries this device's claim
        // (estado/motivo/fecha); uploading it would overwrite the winner's original.
        val skippedLocalIds = claimConflicts.keys +
            claimConflicts.keys.mapNotNull { localById[it]?.reclamoOrigenId } +
            syncStateTracker.awaitingRemoteIds(opticaId, "dispensacion")
        val deferredMerges = mutableListOf<Pair<DispensacionOptica, DispensacionOptica>>()
        val uniqueRows = LinkedHashMap<String, Pair<String, DispensacionRemota>>()
        dispensaciones.forEach { dispensacion ->
            if (dispensacion.id in skippedLocalIds) return@forEach
            val pagosSum = pagosSumByDisp[dispensacion.id] ?: 0.0
            val safePagosSum = FinanzasUploadValidator.safeParentBalanceForUpload(pagosSum)
            if (safePagosSum < pagosSum) {
                AppLogger.w(
                    TAG,
                    "Dispensación ${dispensacion.id}: pagos net $pagosSum < 0; upload monto_pagado=0 (CHECK floor)",
                )
            }
            val base = dispensacion.toRemoto(pagosSum = safePagosSum).copy(opticaId = opticaRemota)
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
        // WHY: the original may have adopted a remote id by OT; the replacement must point at that id.
        val remoteIdByLocalId = uniqueById.values.associate { (localId, row) -> localId to row.id }
        val rows = uniqueById.values.map { (_, row) ->
            val remoteOrigenId = row.reclamoOrigenId?.let(remoteIdByLocalId::get)
            if (remoteOrigenId != null) row.copy(reclamoOrigenId = remoteOrigenId) else row
        }
        // WHY: the server unique index decides which claim wins; an original sent before its replacement
        // would carry this device's claim (estado/motivo/fecha) over the winner's original.
        val replacementByOriginal = unconfirmedClaimReplacements(opticaId, dispensaciones)
        val (heldOriginals, firstPass) = rows.partition { uniqueById[it.id]?.first in replacementByOriginal }
        val acceptedRemoteIds = mutableSetOf<String>()
        var uploadedCount = 0
        var chunkNumber = 0
        suspend fun uploadInChunks(batch: List<DispensacionRemota>) = batch.chunked(UPSERT_BATCH_SIZE).forEach { chunk ->
            chunkNumber++
            val label = "upsert:$TABLE_DISPENSACIONES:chunk$chunkNumber"
            uploadedCount += upsertIsolating(
                chunk,
                upsert = { c ->
                    networkRetryHelper.retryNetwork(label) {
                        upsertDispensacionesChunk(c)
                    }
                    acceptedRemoteIds.addAll(c.map { it.id })
                },
                onPoison = { row, reason ->
                    val localId = uniqueById[row.id]?.first ?: row.id
                    val localOrigenId = localById[localId]?.reclamoOrigenId
                    if (reason == FinanzasUploadValidator.CLAIM_UNIQUE_VIOLATION && localOrigenId != null) {
                        syncStateTracker.markError(
                            opticaId, "dispensacion", localId,
                            FinanzasUploadValidator.reclamoDuplicateReason(localOrigenId),
                        )
                    } else if (remotosExistentes.isEmpty()) {
                        // WHY: upsert ON CONFLICT id updates another tenant's row → RLS 42501.
                        repository.deleteDispensacionById(localId, opticaId)
                        AppLogger.w(
                            TAG,
                            "Descartada dispensación local $localId: RLS al subir a óptica vacía (PK de otra cuenta)",
                        )
                    } else {
                        syncStateTracker.markError(opticaId, "dispensacion", localId, reason)
                    }
                },
            )
        }
        try {
            uploadInChunks(firstPass)
            uploadInChunks(
                heldOriginals.filter { row ->
                    val replacementLocalId = replacementByOriginal[uniqueById[row.id]?.first]
                    remoteIdByLocalId[replacementLocalId] in acceptedRemoteIds
                },
            )
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
            uniqueById.values.forEach { (localId, row) ->
                if (row.id in acceptedRemoteIds) {
                    syncStateTracker.markSynced(opticaId, "dispensacion", localId)
                }
            }
        }
        syncStateTracker.markSynced(opticaId, "upload_dispensaciones", "batch")
        return acceptedRemoteIds.size
    }

    /**
     * A local replacement must never adopt another row's id: the first claim to reach the server
     * wins, so a replacement whose original already has a different remote replacement loses
     * (reclamo_duplicate), and one whose OT is taken by an unrelated remote order is held back.
     */
    private fun remoteReplacementsByOrigen(remotos: List<DispensacionRemotaLookup>): Map<String, String> = remotos
        .mapNotNull { r -> r.reclamoOrigenId?.takeIf { it.isNotBlank() }?.let { it to r.id } }
        .toMap()

    private fun remoteOrigenIdOf(
        replacement: DispensacionOptica,
        localById: Map<String, DispensacionOptica>,
        remoteIdByOt: Map<String, String>,
    ): String {
        val origenId = replacement.reclamoOrigenId.orEmpty()
        val original = localById[origenId] ?: return origenId
        return normalizedOtForUnique(original.ot)?.let(remoteIdByOt::get) ?: original.id
    }

    /**
     * Claim OTs are numbered offline, so another order may already own `<base>-R<n>` on the server.
     * Such a replacement takes the next suffix free both locally and remotely instead of waiting
     * forever; a losing claim (the original already has another remote replacement) keeps its OT
     * because [detectClaimConflicts] discards it anyway.
     */
    private suspend fun renumberCollidingClaimOts(
        snapshot: List<DispensacionOptica>,
        remotos: List<DispensacionRemotaLookup>,
        remoteIdByOt: Map<String, String>,
    ): List<DispensacionOptica> {
        val snapshotById = snapshot.associateBy { it.id }
        val remoteReplacementByOrigen = remoteReplacementsByOrigen(remotos)
        val takenOts = (snapshot.mapNotNull { normalizedOtForUnique(it.ot) } + remoteIdByOt.keys).toMutableSet()
        return snapshot.map { replacement ->
            if (replacement.reclamoOrigenId.isNullOrBlank()) return@map replacement
            val otOwnerId = normalizedOtForUnique(replacement.ot)?.let(remoteIdByOt::get)
            if (otOwnerId == null || otOwnerId == replacement.id) return@map replacement
            val winnerId = remoteReplacementByOrigen[remoteOrigenIdOf(replacement, snapshotById, remoteIdByOt)]
            if (winnerId != null && winnerId != replacement.id) return@map replacement
            val freeOt = nextFreeReclamoOt(replacement.ot, takenOts) ?: return@map replacement
            takenOts += normalizedOtForUnique(freeOt).orEmpty()
            val renumbered = replacement.copy(ot = freeOt)
            runInTransaction {
                repository.updateDispensacion(renumbered)
                rewriteClaimSaleNotes(replacement, freeOt)
            }
            AppLogger.w(TAG, "OT ${replacement.ot} del reclamo ${replacement.id} ya existe en la nube; renumerada a $freeOt")
            renumbered
        }
    }

    /**
     * The claim's frame sale notes carry the replacement's OT ("Venta por reclamo de OT ..."); the
     * pagos notes carry the original's OT, which a renumber never changes. The rows get a fresh
     * `updatedAt` so a sale already uploaded under the old OT is upserted again by the next inventario sync.
     */
    private suspend fun rewriteClaimSaleNotes(replacement: DispensacionOptica, newOt: String) {
        val stamp = Instant.now().toString()
        repository.getMovimientosMonturaSnapshotForOptica(replacement.opticaId)
            .filter { it.referenciaId == replacement.id || it.referenciaId.startsWith("${replacement.id}:") }
            .forEach { movimiento ->
                val nota = replaceOtToken(movimiento.nota, replacement.ot, newOt)
                if (nota != movimiento.nota) {
                    repository.upsertMonturaMovimiento(movimiento.copy(nota = nota, updatedAt = stamp))
                }
            }
    }

    private fun detectClaimConflicts(
        dispensaciones: List<DispensacionOptica>,
        localById: Map<String, DispensacionOptica>,
        remotos: List<DispensacionRemotaLookup>,
        remoteIdByOt: Map<String, String>,
    ): Map<String, String> {
        val remoteReplacementByOrigen = remoteReplacementsByOrigen(remotos)
        val conflicts = LinkedHashMap<String, String>()
        dispensaciones.forEach { replacement ->
            val origenId = replacement.reclamoOrigenId ?: return@forEach
            val winnerId = remoteReplacementByOrigen[remoteOrigenIdOf(replacement, localById, remoteIdByOt)]
            val otOwnerId = normalizedOtForUnique(replacement.ot)?.let(remoteIdByOt::get)
            val reason = when {
                winnerId != null && winnerId != replacement.id ->
                    FinanzasUploadValidator.reclamoDuplicateReason(origenId)
                otOwnerId != null && otOwnerId != replacement.id ->
                    FinanzasUploadValidator.reclamoOtConflictReason(otOwnerId)
                else -> null
            }
            if (reason != null) conflicts[replacement.id] = reason
        }
        return conflicts
    }

    /** Original local id -> its replacement's local id, for claims the server has not confirmed yet. */
    private suspend fun unconfirmedClaimReplacements(
        opticaId: String,
        dispensaciones: List<DispensacionOptica>,
    ): Map<String, String> = dispensaciones
        .filter { !it.reclamoOrigenId.isNullOrBlank() && !syncStateTracker.isSynced(opticaId, "dispensacion", it.id) }
        .associate { it.reclamoOrigenId!! to it.id }

    /**
     * Ledger rows of a claim reach the server only after the claim itself: an early Reverso on the
     * original would debit it even if this device's claim later loses. Pagos of an original held for
     * the winner wait for the download so residual local credit can still be moved to the winner.
     */
    private suspend fun claimLedgerDeferral(opticaId: String): (com.example.optoapp.data.Pago) -> Boolean {
        val replacementByOriginal = unconfirmedClaimReplacements(opticaId, repository.getDispensacionesSnapshotForOptica(opticaId))
        val pendingReplacements = replacementByOriginal.values.toSet()
        val awaiting = syncStateTracker.awaitingRemoteIds(opticaId, "dispensacion")
        return { pago ->
            val dispId = pago.dispensacionId
            dispId != null && (
                dispId in pendingReplacements ||
                    dispId in awaiting ||
                    (dispId in replacementByOriginal && isClaimReversalPago(pago))
                )
        }
    }

    private suspend fun losingClaimReplacementIds(opticaId: String): Set<String> =
        syncStateTracker.quarantineReasons(opticaId, "dispensacion")
            .filterValues { FinanzasUploadValidator.reclamoOrigenIdOf(it) != null }
            .keys

    suspend fun uploadServicios(opticaId: String): Int {
        val servicios = repository.getServiciosSnapshotForOptica(opticaId)
        if (servicios.isEmpty()) {
            syncStateTracker.markSynced(opticaId, "upload_servicios_extra", "batch")
            return 0
        }
        val allPagosServ = repository.getPagosSnapshotForOptica(opticaId)
        val aCuentaSumByServ = allPagosServ
            .filter { it.servicioExtraId != null }
            .groupBy { it.servicioExtraId!! }
            .mapValues { (_, pags) -> pags.sumOf { PagoEffect.signedAmount(it.tipo, it.monto) } }
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
            val safeACuenta = FinanzasUploadValidator.safeParentBalanceForUpload(aCuentaSum)
            if (safeACuenta < aCuentaSum) {
                AppLogger.w(
                    TAG,
                    "Servicio ${servicio.id}: pagos net $aCuentaSum < 0; upload a_cuenta=0 (CHECK floor)",
                )
            }
            val base = servicio.toRemoto(aCuentaSum = safeACuenta).copy(opticaId = opticaRemota)
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
        val losingReplacements = losingClaimReplacementIds(opticaId)
        val rows = items
            .filterNot { it.dispensacionId in losingReplacements }
            .map { it.toRemoto().copy(opticaId = opticaRemota) }
            .distinctBy { it.id }
        return executeSimpleUpsert(
            opticaId,
            TABLE_DISPENSACION_ITEMS,
            "dispensacion_item",
            "upload_dispensacion_items",
            rows,
            { it.id },
        ) { upsertDispensacionItemsChunk(it) }
    }

    internal open suspend fun upsertDispensacionItemsChunk(chunk: List<DispensacionItemRemota>) {
        supabase.postgrest[TABLE_DISPENSACION_ITEMS].upsert(chunk)
    }

    internal open suspend fun upsertRegalosChunk(chunk: List<RegaloDispensacionRemota>) {
        supabase.postgrest[TABLE_REGALOS].upsert(chunk)
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
        val servicioExtraId: String?,
        val reversaPagoId: String?,
        val tipo: String,
        val monto: Double,
        val metodoPago: String,
        val fecha: String,
    )

    // WHY: testability seam — isolate PostgREST upsert from coordinator logic.
    internal open suspend fun upsertDispensacionesChunk(chunk: List<DispensacionRemota>) {
        supabase.postgrest[TABLE_DISPENSACIONES].upsert(chunk)
    }

    internal open suspend fun fetchRemoteDispensacionesForLookup(opticaId: String): List<DispensacionRemotaLookup> =
        supabase.postgrest[TABLE_DISPENSACIONES]
            .select { filter { eq("optica_id", opticaId) } }
            .decodeList()

    internal open suspend fun upsertPagosChunk(chunk: List<PagoRemoto>) {
        supabase.postgrest[TABLE_PAGOS].upsert(chunk)
    }

    internal open suspend fun fetchRemoteParentIds(opticaId: String): Pair<Set<String>, Set<String>> {
        val dispIds = supabase.postgrest[TABLE_DISPENSACIONES]
            .select { filter { eq("optica_id", opticaId) } }
            .decodeList<DispensacionRemotaLookup>()
            .map { it.id }
            .toSet()
        val servIds = supabase.postgrest[TABLE_SERVICIOS]
            .select { filter { eq("optica_id", opticaId) } }
            .decodeList<ServicioRemotoLookup>()
            .map { it.id }
            .toSet()
        return dispIds to servIds
    }

    /**
     * Binary-split on CHECK/domain RestException so one poison row does not block siblings.
     */
    internal suspend fun <T> upsertIsolating(
        chunk: List<T>,
        upsert: suspend (List<T>) -> Unit,
        onPoison: suspend (T, String) -> Unit,
    ): Int {
        if (chunk.isEmpty()) return 0
        return try {
            upsert(chunk)
            chunk.size
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            val msg = e.message
            if (chunk.size == 1) {
                if (!FinanzasUploadValidator.isIsolatableUploadFailure(msg)) throw e
                onPoison(chunk[0], FinanzasUploadValidator.poisonReason(msg))
                return 0
            }
            if (!FinanzasUploadValidator.isIsolatableUploadFailure(msg)) throw e
            val mid = chunk.size / 2
            upsertIsolating(chunk.subList(0, mid), upsert, onPoison) +
                upsertIsolating(chunk.subList(mid, chunk.size), upsert, onPoison)
        }
    }

    suspend fun uploadPagos(opticaId: String): Int {
        val pagos = repository.getPagosSnapshotForOptica(opticaId)
        if (pagos.isEmpty()) {
            syncStateTracker.markSynced(opticaId, "upload_pagos", "batch")
            return 0
        }
        require(opticaId.isNotBlank()) { "opticaId must not be blank for upload" }
        val opticaRemota = opticaId.trim()

        val remoteParents = try {
            fetchRemoteParentIds(opticaRemota)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AppLogger.e(TAG, "FATAL: Cannot fetch parent ids for pagos gate.", e)
            throw UploadPreCheckFailedException("Parent lookup failed for $TABLE_PAGOS", e)
        }
        val (remoteDispIds, remoteServIds) = remoteParents
        val quarantinedDisp = syncStateTracker.quarantinedEntityIds(opticaId, "dispensacion")
        val quarantinedServ = syncStateTracker.quarantinedEntityIds(opticaId, "servicio_extra")

        val isDeferred = claimLedgerDeferral(opticaId)
        var quarantineCount = 0
        var deferredCount = 0
        val eligible = mutableListOf<com.example.optoapp.data.Pago>()
        val poisonedLocalIds = mutableSetOf<String>()
        for (pago in pagos) {
            if (isDeferred(pago)) {
                deferredCount++
                continue
            }
            val reason = FinanzasUploadValidator.validatePago(
                pago.tipo, pago.monto, pago.dispensacionId, pago.servicioExtraId, pago.reversaPagoId,
            ) ?: run {
                val dispId = pago.dispensacionId
                val servId = pago.servicioExtraId
                when {
                    dispId != null && (dispId in quarantinedDisp || dispId !in remoteDispIds) ->
                        FinanzasUploadValidator.parentMissingReason("dispensacion", dispId)
                    servId != null && (servId in quarantinedServ || servId !in remoteServIds) ->
                        FinanzasUploadValidator.parentMissingReason("servicio", servId)
                    else -> null
                }
            }
            if (reason != null) {
                syncStateTracker.markError(opticaId, "pago", pago.id, reason)
                quarantineCount++
                poisonedLocalIds.add(pago.id)
            } else {
                eligible.add(pago)
            }
        }

        if (deferredCount > 0) {
            AppLogger.d(TAG, "Pagos de reclamo retenidos hasta confirmar el reclamo: $deferredCount")
        }
        val rows = eligible.map { it.toRemoto().copy(opticaId = opticaRemota) }.distinctBy { it.id }
        val remotos = try {
            fetchRemotePagosForLookup(opticaRemota)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AppLogger.e(TAG, "FATAL: Cannot reconcile pagos with remote. Aborting to prevent duplicates.", e)
            throw UploadPreCheckFailedException("Reconciliation fetch failed for $TABLE_PAGOS", e)
        }
        val remoteIds = remotos.mapTo(HashSet()) { it.id }
        val remoteIdByKey = remotos.associate { r ->
            PagoKey(
                r.dispensacionId.orEmpty(), r.servicioExtraId.orEmpty(), r.reversaPagoId.orEmpty(),
                r.tipo, r.monto, r.metodoPago, r.fecha,
            ) to r.id
        }
        val remoteReversoIdByTarget = remotos
            .filter { it.tipo.trim() == TIPO_REVERSO && !it.reversaPagoId.isNullOrBlank() }
            .associate { it.reversaPagoId!! to it.id }

        val uniqueById = LinkedHashMap<String, Pair<String, PagoRemoto>>()
        rows.forEach { row ->
            val key = PagoKey(
                row.dispensacionId.orEmpty(), row.servicioExtraId.orEmpty(), row.reversaPagoId.orEmpty(),
                row.tipo, row.monto, row.metodoPago, row.fecha,
            )
            // WHY: a pago already stored remotely under its own id must keep it; remapping it onto an
            // equal-looking twin would collapse two real pagos into one.
            val remoteId = if (row.id in remoteIds) null else remoteIdByKey[key]
            val reconciled = if (remoteId != null && remoteId != row.id) row.copy(id = remoteId) else row
            val target = reconciled.reversaPagoId
            val remoteReversoId = target?.takeIf { reconciled.tipo.trim() == TIPO_REVERSO }
                ?.let(remoteReversoIdByTarget::get)
            if (target != null && remoteReversoId != null && remoteReversoId != reconciled.id) {
                // WHY: another device already reversed this credit (e.g. a winning claim); a second
                // Reverso would double-debit the order and trips pagos_reversa_pago_id_uidx.
                syncStateTracker.markError(opticaId, "pago", row.id, FinanzasUploadValidator.reversoDuplicateReason(target))
                quarantineCount++
                poisonedLocalIds.add(row.id)
                return@forEach
            }
            if (uniqueById.containsKey(reconciled.id)) return@forEach
            uniqueById[reconciled.id] = row.id to reconciled
        }
        // WHY: a debit landing before its offsetting credit (chunk split or row-by-row fallback)
        // drives the server parent balance below 0 and trips the monto_pagado CHECK.
        val uniqueRows = uniqueById.values.map { it.second }
            .sortedBy { if (PagoEffect.signedAmount(it.tipo, it.monto) >= 0) 0 else 1 }
        var uploadedCount = 0
        try {
            uniqueRows.chunked(UPSERT_BATCH_SIZE).forEachIndexed { index, chunk ->
                uploadedCount += upsertIsolating(
                    chunk,
                    upsert = { c ->
                        networkRetryHelper.retryNetwork("upsert:$TABLE_PAGOS:chunk${index + 1}") {
                            upsertPagosChunk(c)
                        }
                    },
                    onPoison = { row, reason ->
                        quarantineCount++
                        val localId = uniqueById[row.id]?.first ?: row.id
                        poisonedLocalIds.add(localId)
                        val effectiveReason = if (reason == FinanzasUploadValidator.REVERSO_UNIQUE_VIOLATION) {
                            FinanzasUploadValidator.reversoDuplicateReason(row.reversaPagoId.orEmpty())
                        } else {
                            reason
                        }
                        syncStateTracker.markError(opticaId, "pago", localId, effectiveReason)
                    },
                )
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
                if (localId !in poisonedLocalIds) {
                    syncStateTracker.markSynced(opticaId, "pago", localId)
                }
            }
        }
        if (quarantineCount > 0) {
            syncStateTracker.markError(opticaId, "upload_pagos", "batch", "quarantine:partial:$quarantineCount")
            throw UploadPartialException(uploadedCount, IOException("quarantine:partial:$quarantineCount"))
        }
        syncStateTracker.markSynced(opticaId, "upload_pagos", "batch")
        return uploadedCount
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
        val losingReplacements = losingClaimReplacementIds(opticaId)
        val rows = regalos
            .filterNot { it.dispensacionId in losingReplacements }
            .map { it.toRemoto().copy(opticaId = opticaRemota) }
            .distinctBy { it.id }
        return executeSimpleUpsert(
            opticaId,
            TABLE_REGALOS,
            "regalo_dispensacion",
            "upload_regalos",
            rows,
            { it.id },
        ) { upsertRegalosChunk(it) }
    }

    suspend fun uploadServicioExtraItems(opticaId: String): Int {
        val items = repository.getServicioExtraItemsSnapshotForOptica(opticaId)
        if (items.isEmpty()) {
            syncStateTracker.markSynced(opticaId, "upload_servicio_extra_items", "batch")
            return 0
        }
        require(opticaId.isNotBlank()) { "opticaId must not be blank for upload" }
        val opticaRemota = opticaId.trim()
        val rows = items.map { it.toRemoto().copy(opticaId = opticaRemota) }.distinctBy { it.id }
        return executeSimpleUpsert(
            opticaId,
            TABLE_SERVICIO_EXTRA_ITEMS,
            "servicio_extra_item",
            "upload_servicio_extra_items",
            rows,
            { it.id },
        ) { supabase.postgrest[TABLE_SERVICIO_EXTRA_ITEMS].upsert(it) }
    }

    suspend fun uploadRegalosServicioExtra(opticaId: String): Int {
        val regalos = repository.getRegalosServicioExtraSnapshotForOptica(opticaId)
        if (regalos.isEmpty()) {
            syncStateTracker.markSynced(opticaId, "upload_regalos_servicio_extra", "batch")
            return 0
        }
        require(opticaId.isNotBlank()) { "opticaId must not be blank for upload" }
        val opticaRemota = opticaId.trim()
        val rows = regalos.map { it.toRemoto().copy(opticaId = opticaRemota) }.distinctBy { it.id }
        return executeSimpleUpsert(
            opticaId,
            TABLE_REGALOS_SERVICIO,
            "regalo_servicio_extra",
            "upload_regalos_servicio_extra",
            rows,
            { it.id },
        ) { supabase.postgrest[TABLE_REGALOS_SERVICIO].upsert(it) }
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
