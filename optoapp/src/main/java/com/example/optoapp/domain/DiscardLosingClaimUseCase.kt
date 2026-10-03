package com.example.optoapp.domain

import com.example.optoapp.data.DispensacionOptica
import com.example.optoapp.data.MonturaMovimiento
import com.example.optoapp.data.OptoRepository
import com.example.optoapp.data.Pago
import com.example.optoapp.data.SyncStateTracker
import com.example.optoapp.data.montura.MonturaInventoryCoordinator
import com.example.optoapp.data.montura.MonturaMovimientoDao
import com.example.optoapp.data.pago.PagoDao
import com.example.optoapp.util.AppLogger
import com.example.optoapp.util.DateUtils
import com.example.optoapp.util.DispensacionStockHelper
import kotlinx.coroutines.CancellationException
import java.time.Instant
import java.util.UUID
import javax.inject.Inject

/**
 * Discards each local claim that lost to another device (`quarantine:reclamo_duplicate`) so the
 * next download can bring the winner's replacement and pagos.
 *
 * The local original is not restored to its pre-claim estado: the winner already made it Reclamada
 * on the server and download applies a remote terminal row over a local terminal one, so the
 * original is only held back from upload until that download refreshes it.
 *
 * Stock: a sale movimiento that never reached the server is deleted and its stock delta reverted
 * locally, because emitting a compensating AJUSTE would upload a movement the server has nothing
 * to net against. One that was already uploaded (inventario can sync before finanzas detects the
 * conflict) is compensated with an idempotent restock so the server ledger nets to zero.
 */
class DiscardLosingClaimUseCase @Inject constructor(
    private val repository: OptoRepository,
    private val pagoDao: PagoDao,
    private val movimientoDao: MonturaMovimientoDao,
    private val monturaCoordinator: MonturaInventoryCoordinator,
    private val stockHelper: DispensacionStockHelper,
    private val syncStateTracker: SyncStateTracker,
) {
    companion object {
        private const val TAG = "SyncFinanzas"
        private const val NOTICE = "reclamo_descartado"
        private const val PENDING_CREDIT = "reclamo_credito"
        private const val DOWNLOAD_PAGOS = "download_pago"
        private const val DISCARD_FAILED_PREFIX = "quarantine:reclamo_descarte_fallido:"
    }

    /**
     * Each claim is discarded in its own transaction. One that fails is set aside with a visible
     * quarantine notice instead of being retried on every sync, which would keep finanzas partial
     * forever; clearing that notice from the error history retries it.
     */
    suspend operator fun invoke(opticaId: String): Int {
        val setAside = syncStateTracker.quarantineReasons(opticaId, NOTICE).keys
        val losing = syncStateTracker.quarantineReasons(opticaId, "dispensacion")
            .mapNotNull { (replacementId, reason) ->
                FinanzasUploadValidator.reclamoOrigenIdOf(reason)?.let { replacementId to it }
            }
            .filter { (replacementId, _) -> replacementId !in setAside }
        return losing.count { (replacementId, origenId) -> discardIsolated(opticaId, replacementId, origenId) }
    }

    private suspend fun discardIsolated(opticaId: String, replacementId: String, origenId: String): Boolean = try {
        repository.withTransaction { discard(opticaId, replacementId, origenId) }
        AppLogger.w(TAG, "Reclamo local $replacementId descartado: otro dispositivo reclamó $origenId primero")
        true
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        val ot = claimOt(opticaId, replacementId, origenId)
        syncStateTracker.markError(
            opticaId, NOTICE, replacementId,
            "$DISCARD_FAILED_PREFIX No se pudo descartar el reclamo local de la OT $ot (${e.message}).",
        )
        AppLogger.e(TAG, "No se pudo descartar el reclamo local $replacementId", e)
        false
    }

    private suspend fun claimOt(opticaId: String, replacementId: String, origenId: String): String =
        repository.getDispensacionById(origenId, opticaId).data?.ot?.takeIf { it.isNotBlank() }
            ?: repository.getDispensacionById(replacementId, opticaId).data?.ot.orEmpty()

    /**
     * Credit the winning claim never reversed (recorded offline, or synced after the winner's claim)
     * moves to the winner's replacement with the claim's own mechanics: a Reverso of that credit on
     * the original and an Abono with the same metodo on the replacement. Only originals whose local
     * claim was discarded qualify, and only right after a complete pagos download so the winner's
     * Reversos are local and already-reversed credit is never transferred twice. The download marker
     * is consumed here, so a success left by an earlier run never authorizes a later one; a recorded
     * download failure is left in place so it stays visible until a later fetch succeeds.
     *
     * An incomplete download caused by a local quarantine or pending deletion is something the user
     * can resolve, so each pending original gets one notice that the transfer is waiting; a network
     * failure already surfaces as a sync error and gets none. The notice is dropped once the
     * transfer is attempted on a complete download.
     */
    suspend fun transferResidualCredit(opticaId: String): Int {
        if (!syncStateTracker.isSynced(opticaId, DOWNLOAD_PAGOS, "batch")) {
            if (syncStateTracker.isSynced(opticaId, downloadBlockedEntityType("pago"), "batch")) {
                noticeCreditPending(opticaId)
            }
            return 0
        }
        syncStateTracker.clear(opticaId, DOWNLOAD_PAGOS, "batch")
        return syncStateTracker.awaitingRemoteIds(opticaId, PENDING_CREDIT).sumOf { localOrigenId ->
            repository.withTransaction { settle(opticaId, localOrigenId) }
                .also { syncStateTracker.clear(opticaId, NOTICE, pendingCreditNoticeId(localOrigenId)) }
        }
    }

    private suspend fun noticeCreditPending(opticaId: String) {
        syncStateTracker.awaitingRemoteIds(opticaId, PENDING_CREDIT).forEach { origenId ->
            val ot = repository.getDispensacionById(origenId, opticaId).data?.ot?.takeIf { it.isNotBlank() }
                ?: return@forEach
            syncStateTracker.markError(
                opticaId, NOTICE, pendingCreditNoticeId(origenId),
                "El crédito del reclamo de la OT $ot queda pendiente hasta resolver los pagos en espera.",
            )
        }
    }

    private fun pendingCreditNoticeId(origenId: String) = "$origenId:credito_pendiente"

    private suspend fun settle(opticaId: String, localOrigenId: String): Int {
        val snapshot = repository.getDispensacionesSnapshotForOptica(opticaId)
        if (snapshot.none { it.id == localOrigenId }) {
            syncStateTracker.clear(opticaId, PENDING_CREDIT, localOrigenId)
            syncStateTracker.clear(opticaId, "dispensacion", localOrigenId)
            return 0
        }
        val origenId = adoptedOriginalId(opticaId, localOrigenId, snapshot) ?: return 0
        val winner = syncedWinnerOf(opticaId, origenId, snapshot) ?: return 0
        val transferred = transferToWinner(opticaId, origenId, winner) ?: return 0
        syncStateTracker.clear(opticaId, PENDING_CREDIT, origenId)
        return transferred
    }

    /**
     * The original the winner points at. Download releases the hold on the local original by id; when
     * the original had adopted a remote id by OT the download brings that row instead, so the local
     * copy is folded into it, but only once that row is Reclamada and its winning replacement is
     * local. The pending-credit marker moves with the fold so it can never point at a deleted row.
     */
    private suspend fun adoptedOriginalId(
        opticaId: String,
        localOrigenId: String,
        snapshot: List<DispensacionOptica>,
    ): String? {
        if (localOrigenId !in syncStateTracker.awaitingRemoteIds(opticaId, "dispensacion")) return localOrigenId
        val local = snapshot.first { it.id == localOrigenId }
        val otKey = normalizedOtForUnique(local.ot) ?: return null
        val twin = snapshot.firstOrNull { row ->
            row.id != localOrigenId && row.reclamoOrigenId.isNullOrBlank() &&
                normalizedOtForUnique(row.ot) == otKey &&
                row.estadoEntrega.trim() == OrderStatusPolicy.RECLAMADA &&
                syncStateTracker.isSynced(opticaId, "dispensacion", row.id) &&
                syncedWinnerOf(opticaId, row.id, snapshot) != null
        } ?: return null
        repository.reassignPagosDispensacion(localOrigenId, twin.id, opticaId)
        repository.reassignItemsDispensacion(localOrigenId, twin.id, opticaId)
        repository.reassignRegalosDispensacion(localOrigenId, twin.id, opticaId)
        repository.deleteDispensacionById(localOrigenId, opticaId)
        syncStateTracker.clear(opticaId, "dispensacion", localOrigenId)
        syncStateTracker.clear(opticaId, PENDING_CREDIT, localOrigenId)
        syncStateTracker.markAwaitingRemote(opticaId, PENDING_CREDIT, twin.id)
        AppLogger.w(TAG, "Original local $localOrigenId fusionado en ${twin.id} descargado por OT ${local.ot}")
        return twin.id
    }

    private suspend fun syncedWinnerOf(
        opticaId: String,
        origenId: String,
        snapshot: List<DispensacionOptica>,
    ): DispensacionOptica? = snapshot.firstOrNull {
        it.reclamoOrigenId == origenId && syncStateTracker.isSynced(opticaId, "dispensacion", it.id)
    }

    /** Null while the original is not yet Reclamada locally, so the transfer waits for a later sync. */
    private suspend fun transferToWinner(opticaId: String, origenId: String, winner: DispensacionOptica): Int? {
        val original = repository.getDispensacionById(origenId, opticaId).data ?: return null
        if (original.estadoEntrega.trim() != OrderStatusPolicy.RECLAMADA) return null
        val residual = ledgerSnapshot(pagoDao.getPagosByParent(origenId, opticaId)).unreversedCredits
            .filterNot(::isClaimReversalPago)
        if (residual.isEmpty()) return 0
        residual.forEach { credit ->
            repository.insertPago(buildReverso(credit, origenId, opticaId, forDispensacion = true))
            repository.insertPago(
                Pago(
                    id = UUID.randomUUID().toString(),
                    dispensacionId = winner.id,
                    fecha = DateUtils.today(),
                    tipo = "Abono",
                    monto = credit.monto,
                    metodoPago = credit.metodoPago,
                    nota = "Crédito por reclamo de OT ${original.ot}",
                    opticaId = opticaId,
                    ventaId = "v_disp_${winner.id}",
                    updatedAt = Instant.now().toString(),
                ),
            )
        }
        repository.updateDispensacion(original.copy(montoPagado = pagoDao.sumMontoByDispensacion(origenId, opticaId)))
        repository.updateDispensacion(winner.copy(montoPagado = pagoDao.sumMontoByDispensacion(winner.id, opticaId)))
        syncStateTracker.markError(
            opticaId, NOTICE, "$origenId:credito",
            "El pago local de la OT ${original.ot} se transfirió a la orden ${winner.ot} del reclamo registrado en otro dispositivo.",
        )
        AppLogger.w(TAG, "Crédito local de $origenId (${residual.size} pagos) transferido al reclamo ganador ${winner.id}")
        return residual.size
    }

    private suspend fun discard(opticaId: String, replacementId: String, origenId: String) {
        val replacement = repository.getDispensacionById(replacementId, opticaId).data
        val original = repository.getDispensacionById(origenId, opticaId).data

        movimientoDao.getMovimientosListByOptica(opticaId)
            .filter { it.referenciaId == replacementId || it.referenciaId.startsWith("$replacementId:") }
            .forEach { undoMovimiento(opticaId, replacementId, it) }

        pagoDao.getPagosByParent(origenId, opticaId)
            .filter { isClaimReversalPago(it) && !syncStateTracker.isSynced(opticaId, "pago", it.id) }
            .forEach { pago ->
                pagoDao.deletePago(pago.id, opticaId)
                syncStateTracker.clear(opticaId, "pago", pago.id)
            }

        if (replacement != null) {
            pagoDao.getPagosByParent(replacementId, opticaId).forEach { syncStateTracker.clear(opticaId, "pago", it.id) }
            repository.getDispensacionItemsByDispensacion(replacementId, opticaId)
                .forEach { syncStateTracker.clear(opticaId, "dispensacion_item", it.id) }
            repository.getRegalosByDispensacionId(replacementId, opticaId)
                .forEach { syncStateTracker.clear(opticaId, "regalo_dispensacion", it.id) }
            repository.deleteDispensacionById(replacementId, opticaId)
        }
        syncStateTracker.clear(opticaId, "dispensacion", replacementId)
        syncStateTracker.markAwaitingRemote(opticaId, "dispensacion", origenId)
        syncStateTracker.markAwaitingRemote(opticaId, PENDING_CREDIT, origenId)
        val ot = original?.ot?.takeIf { it.isNotBlank() } ?: replacement?.ot.orEmpty()
        syncStateTracker.markError(
            opticaId, NOTICE, replacementId,
            "Otro dispositivo ya registró el reclamo de la OT $ot; se descartó el reclamo local.",
        )
    }

    private suspend fun undoMovimiento(opticaId: String, replacementId: String, movimiento: MonturaMovimiento) {
        val delta = movimiento.stockPrevio - movimiento.stockNuevo
        if (syncStateTracker.isSynced(opticaId, "montura_movimiento", movimiento.id)) {
            if (delta != 0) {
                stockHelper.restockOnce(
                    movimiento.monturaId, opticaId, delta,
                    movimientoReferenciaForDiscardedClaim(replacementId, movimiento.id),
                    "Reversión por reclamo descartado",
                )
            }
            return
        }
        movimientoDao.deleteMovimiento(movimiento.id, opticaId)
        syncStateTracker.clear(opticaId, "montura_movimiento", movimiento.id)
        if (delta != 0) {
            check(monturaCoordinator.adjustMonturaStockLocal(movimiento.monturaId, opticaId, delta) > 0) {
                "No se pudo restaurar el stock de ${movimiento.monturaId}"
            }
        }
    }

}

/** Rows a claim writes on its original: Reversos and the Abonos compensating its legacy debits. */
internal fun isClaimReversalPago(pago: Pago): Boolean =
    pago.tipo.trim() == TIPO_REVERSO ||
        (pago.nota.startsWith(NOTA_COMPENSACION_PREFIX) && pago.nota.endsWith("por reclamo"))
