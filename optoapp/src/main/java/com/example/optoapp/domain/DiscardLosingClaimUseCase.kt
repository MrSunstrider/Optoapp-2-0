package com.example.optoapp.domain

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
    }

    suspend operator fun invoke(opticaId: String): Int {
        val losing = syncStateTracker.quarantineReasons(opticaId, "dispensacion")
            .mapNotNull { (replacementId, reason) ->
                FinanzasUploadValidator.reclamoOrigenIdOf(reason)?.let { replacementId to it }
            }
        losing.forEach { (replacementId, origenId) ->
            repository.withTransaction { discard(opticaId, replacementId, origenId) }
            AppLogger.w(TAG, "Reclamo local $replacementId descartado: otro dispositivo reclamó $origenId primero")
        }
        return losing.size
    }

    /**
     * Credit recorded on the original offline and never uploaded is unknown to the winning claim, so
     * once download has adopted the winner it moves to the winner's replacement with the claim's own
     * mechanics: a Reverso of that credit on the original and an Abono with the same metodo on the
     * replacement. Synced credits are excluded because the winner already reversed them remotely.
     */
    suspend fun transferResidualCredit(opticaId: String): Int {
        val awaiting = syncStateTracker.awaitingRemoteIds(opticaId, "dispensacion")
        val winners = repository.getDispensacionesSnapshotForOptica(opticaId).filter { replacement ->
            val origenId = replacement.reclamoOrigenId
            !origenId.isNullOrBlank() && origenId !in awaiting &&
                syncStateTracker.isSynced(opticaId, "dispensacion", replacement.id)
        }
        return winners.sumOf { winner -> repository.withTransaction { transferToWinner(opticaId, winner.id) } }
    }

    private suspend fun transferToWinner(opticaId: String, winnerId: String): Int {
        val winner = repository.getDispensacionById(winnerId, opticaId).data ?: return 0
        val origenId = winner.reclamoOrigenId ?: return 0
        val original = repository.getDispensacionById(origenId, opticaId).data ?: return 0
        if (original.estadoEntrega.trim() != OrderStatusPolicy.RECLAMADA) return 0
        val residual = ledgerSnapshot(pagoDao.getPagosByParent(origenId, opticaId)).unreversedCredits
            .filter { !syncStateTracker.isSynced(opticaId, "pago", it.id) }
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
            opticaId, "reclamo_descartado", "$origenId:credito",
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
        val ot = original?.ot?.takeIf { it.isNotBlank() } ?: replacement?.ot.orEmpty()
        syncStateTracker.markError(
            opticaId, "reclamo_descartado", replacementId,
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
    pago.tipo.trim() == "Reverso" ||
        (pago.nota.startsWith(NOTA_COMPENSACION_PREFIX) && pago.nota.endsWith("por reclamo"))
