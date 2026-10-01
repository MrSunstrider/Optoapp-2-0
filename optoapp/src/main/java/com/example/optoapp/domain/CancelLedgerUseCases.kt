package com.example.optoapp.domain

import com.example.optoapp.data.OptoRepository
import com.example.optoapp.data.Pago
import com.example.optoapp.data.Resource
import com.example.optoapp.data.pago.PagoDao
import com.example.optoapp.sync.PostSaveSyncScheduler
import com.example.optoapp.util.DispensacionStockHelper
import com.example.optoapp.util.DateUtils
import java.time.Instant
import java.util.UUID
import javax.inject.Inject

private val CREDIT_TIPOS = setOf("Abono", "Pago completo")
private const val TIPO_REVERSO = "Reverso"
private const val TIPO_REEMBOLSO = "Reembolso"

/**
 * Ledger of one parent with reversed credit/Reverso pairs removed. `legacyDebits` are Reembolsos
 * and orphan Reversos: a Reverso may only reverse a credit, so they need compensating Abonos.
 */
internal data class LedgerSnapshot(
    val unreversedCredits: List<Pago>,
    val legacyDebits: List<Pago>,
    val netByMetodo: Map<String, Double>,
) {
    val netPaid: Double get() = netByMetodo.values.sum()
}

internal fun ledgerSnapshot(pagos: List<Pago>): LedgerSnapshot {
    val creditIds = pagos.filter { it.tipo.trim() in CREDIT_TIPOS }.map { it.id }.toSet()
    val reversedCreditIds = pagos
        .filter { it.tipo.trim() == TIPO_REVERSO && it.reversaPagoId in creditIds }
        .mapNotNull { it.reversaPagoId }
        .toSet()
    val unreversedCredits = pagos.filter { it.tipo.trim() in CREDIT_TIPOS && it.id !in reversedCreditIds }
    val legacyDebits = pagos.filter {
        when (it.tipo.trim()) {
            TIPO_REEMBOLSO -> true
            TIPO_REVERSO -> it.reversaPagoId !in creditIds
            else -> false
        }
    }
    val netByMetodo = (unreversedCredits + legacyDebits)
        .groupBy { it.metodoPago }
        .mapValues { (_, rows) -> rows.sumOf { PagoEffect.signedAmount(it.tipo, it.monto) } }
    return LedgerSnapshot(unreversedCredits, legacyDebits, netByMetodo)
}

internal suspend fun insertMissingReversos(
    repository: OptoRepository,
    pagoDao: PagoDao,
    parentId: String,
    opticaId: String,
    forDispensacion: Boolean,
): List<Pago> {
    val inserted = mutableListOf<Pago>()
    for (credit in pagoDao.getCreditPagosByParent(parentId, opticaId)) {
        if (pagoDao.getReversoByOriginalId(credit.id, opticaId) != null) continue
        val reverso = buildReverso(credit, parentId, opticaId, forDispensacion)
        repository.insertPago(reverso)
        inserted += reverso
    }
    return inserted
}

private fun buildReverso(credit: Pago, parentId: String, opticaId: String, forDispensacion: Boolean) = Pago(
    id = UUID.randomUUID().toString(),
    dispensacionId = if (forDispensacion) parentId else null,
    servicioExtraId = if (forDispensacion) null else parentId,
    fecha = DateUtils.today(),
    tipo = TIPO_REVERSO,
    monto = credit.monto,
    metodoPago = credit.metodoPago,
    nota = "Reverso de ${credit.tipo.trim()} ${credit.id.take(8)}",
    opticaId = opticaId,
    ventaId = credit.ventaId,
    reversaPagoId = credit.id,
    updatedAt = Instant.now().toString(),
)

/**
 * Leaves the parent at net 0 using only Reverso/Abono rows (server CHECK forbids reversing a
 * debit). Reversos go first so the compensating Abonos are never themselves reversed.
 * Reversos come from the snapshot, not the exact-tipo SQL lookups, so legacy rows with padded
 * `tipo` are neither skipped nor reversed twice.
 * Must run inside the caller's transaction. Returns the ledger as it was before reversal.
 */
internal suspend fun reverseLedgerFully(
    repository: OptoRepository,
    pagoDao: PagoDao,
    parentId: String,
    opticaId: String,
    forDispensacion: Boolean,
    contexto: String,
): LedgerSnapshot {
    val snapshot = ledgerSnapshot(pagoDao.getPagosByParent(parentId, opticaId))
    for (credit in snapshot.unreversedCredits) {
        repository.insertPago(buildReverso(credit, parentId, opticaId, forDispensacion))
    }
    for (debit in snapshot.legacyDebits) {
        repository.insertPago(
            Pago(
                id = UUID.randomUUID().toString(),
                dispensacionId = if (forDispensacion) parentId else null,
                servicioExtraId = if (forDispensacion) null else parentId,
                fecha = DateUtils.today(),
                tipo = "Abono",
                monto = debit.monto,
                metodoPago = debit.metodoPago,
                nota = "Compensación de ${debit.tipo.trim()} ${debit.id.take(8)} por $contexto",
                opticaId = opticaId,
                ventaId = debit.ventaId,
                updatedAt = Instant.now().toString(),
            ),
        )
    }
    return snapshot
}

class CancelServicioExtraUseCase @Inject constructor(
    private val repository: OptoRepository,
    private val pagoDao: PagoDao,
    private val postSaveSyncScheduler: PostSaveSyncScheduler,
    private val stockHelper: DispensacionStockHelper,
) {
    suspend operator fun invoke(servicioId: String, opticaId: String) {
        val servicio = (repository.getServicioById(servicioId, opticaId) as? Resource.Success)?.data ?: return
        if (servicio.estado == "Anulado") return
        insertMissingReversos(repository, pagoDao, servicioId, opticaId, forDispensacion = false)

        repository.withTransaction {
            val items = repository.getServicioExtraItems(servicioId, opticaId)
            val itemsWithStock = items.filter { !it.monturaId.isNullOrBlank() }
            if (itemsWithStock.isNotEmpty()) {
                for (item in itemsWithStock) {
                    requireStock(
                        stockHelper.adjustStockAndRegistrarMovimiento(
                            monturaId = item.monturaId!!,
                            opticaId = opticaId,
                            delta = 1,
                            tipo = "AJUSTE",
                            referenciaId = item.id,
                            nota = "Reversión por anulación de servicio extra",
                        ),
                        "No se pudo reponer el stock del producto vendido.",
                    )
                }
            } else {
                servicio.monturaId?.takeIf { it.isNotBlank() }?.let { monturaId ->
                    requireStock(
                        stockHelper.adjustStockAndRegistrarMovimiento(
                            monturaId = monturaId,
                            opticaId = opticaId,
                            delta = 1,
                            tipo = "AJUSTE",
                            referenciaId = movimientoReferenciaForServicioExtraReverso(servicioId, monturaId),
                            nota = "Reversión por anulación de servicio extra",
                        ),
                        "No se pudo reponer el stock del producto vendido.",
                    )
                }
            }

            for (regalo in repository.getRegalosByServicioExtraId(servicioId, opticaId)) {
                if (regalo.productoId.isBlank()) continue
                requireStock(
                    stockHelper.adjustStockAndRegistrarMovimiento(
                        monturaId = regalo.productoId,
                        opticaId = opticaId,
                        delta = regalo.cantidad,
                        tipo = "AJUSTE",
                        referenciaId = movimientoReferenciaForRegalo(regalo.id),
                        nota = "Reversión por anulación de regalo de servicio",
                    ),
                    "No se pudo reponer el stock del regalo.",
                )
            }

            repository.updateServicio(servicio.copy(estado = "Anulado", updatedAt = Instant.now().toString()))
        }
        postSaveSyncScheduler.scheduleFinanzasSync(opticaId)
        postSaveSyncScheduler.scheduleInventarioSync(opticaId)
    }

    private fun requireStock(result: Result<Int>, fallbackMessage: String) {
        if (result.isFailure) {
            throw IllegalStateException(result.exceptionOrNull()?.message ?: fallbackMessage)
        }
    }
}

class CancelDispensacionUseCase @Inject constructor(
    private val repository: OptoRepository,
    private val pagoDao: PagoDao,
    private val postSaveSyncScheduler: PostSaveSyncScheduler,
) {
    suspend operator fun invoke(dispensacionId: String, opticaId: String) {
        val disp = (repository.getDispensacionById(dispensacionId, opticaId) as? Resource.Success)?.data ?: return
        if (disp.estadoEntrega == "Anulado") return
        insertMissingReversos(repository, pagoDao, dispensacionId, opticaId, forDispensacion = true)
        repository.updateDispensacion(disp.copy(estadoEntrega = "Anulado", updatedAt = Instant.now().toString()))
        postSaveSyncScheduler.scheduleFinanzasSync(opticaId)
    }
}

class ReclaimDispensacionUseCase @Inject constructor(
    private val repository: OptoRepository,
    private val postSaveSyncScheduler: PostSaveSyncScheduler,
) {
    suspend operator fun invoke(
        dispensacionId: String,
        opticaId: String,
        refundMonto: Double,
        metodoPago: String,
        ot: String,
    ) {
        require(refundMonto >= 0.0) { "Reembolso monto must be >= 0" }
        val disp = (repository.getDispensacionById(dispensacionId, opticaId) as? Resource.Success)?.data ?: return
        repository.updateDispensacion(disp.copy(estadoEntrega = "Reclamada", updatedAt = Instant.now().toString()))
        if (refundMonto > 0.0) {
            repository.insertPago(
                Pago(
                    id = UUID.randomUUID().toString(),
                    dispensacionId = dispensacionId,
                    fecha = DateUtils.today(),
                    tipo = "Reembolso",
                    monto = refundMonto,
                    metodoPago = metodoPago,
                    nota = "Reembolso por reclamo de OT $ot",
                    opticaId = opticaId,
                    updatedAt = Instant.now().toString(),
                ),
            )
        }
        postSaveSyncScheduler.scheduleFinanzasSync(opticaId)
    }
}
