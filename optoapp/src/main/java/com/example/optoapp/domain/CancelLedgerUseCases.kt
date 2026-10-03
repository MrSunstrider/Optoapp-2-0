package com.example.optoapp.domain

import com.example.optoapp.data.DispensacionOptica
import com.example.optoapp.data.OptoRepository
import com.example.optoapp.data.Pago
import com.example.optoapp.data.Resource
import com.example.optoapp.data.ServicioExtra
import com.example.optoapp.data.montura.MonturaInventoryCoordinator
import com.example.optoapp.data.pago.PagoDao
import com.example.optoapp.sync.PostSaveSyncScheduler
import com.example.optoapp.util.DispensacionStockHelper
import com.example.optoapp.util.DateUtils
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import kotlin.math.abs

private val CREDIT_TIPOS = setOf("Abono", "Pago completo")
internal const val TIPO_REVERSO = "Reverso"
private const val TIPO_REEMBOLSO = "Reembolso"
internal const val NOTA_COMPENSACION_PREFIX = "Compensación de "

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

internal fun buildReverso(credit: Pago, parentId: String, opticaId: String, forDispensacion: Boolean) = Pago(
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
                nota = "$NOTA_COMPENSACION_PREFIX${debit.tipo.trim()} ${debit.id.take(8)} por $contexto",
                opticaId = opticaId,
                ventaId = debit.ventaId,
                updatedAt = Instant.now().toString(),
            ),
        )
    }
    return snapshot
}

/**
 * Mirrors [AnularDispensacionUseCase]: the estado check is the first statement of the transaction
 * so a retried cancel writes nothing. Legacy header frames keep their `:rev:` referencia so a
 * servicio cancelled before this change is never restocked twice.
 */
class CancelServicioExtraUseCase @Inject constructor(
    private val repository: OptoRepository,
    private val pagoDao: PagoDao,
    private val postSaveSyncScheduler: PostSaveSyncScheduler,
    private val stockHelper: DispensacionStockHelper,
) {
    suspend operator fun invoke(servicioId: String, opticaId: String, motivo: String): LifecycleOutcome {
        val reason = normalizeMotivo(motivo)
        val outcome = repository.withTransaction {
            val servicio = (repository.getServicioById(servicioId, opticaId) as? Resource.Success)?.data
                ?: throw IllegalStateException("Servicio no encontrado.")
            if (OrderStatusPolicy.isTerminal(servicio.estado)) {
                return@withTransaction LifecycleOutcome.AlreadyTerminal(servicio.estado.trim())
            }
            reverseLedgerFully(repository, pagoDao, servicioId, opticaId, forDispensacion = false, contexto = "anulación")
            restockFrames(servicio, opticaId)
            repository.getRegalosByServicioExtraId(servicioId, opticaId)
                .filter { it.productoId.isNotBlank() }
                .forEach { regalo ->
                    stockHelper.restockOnce(
                        regalo.productoId, opticaId, regalo.cantidad,
                        movimientoReferenciaForRegaloAnulacion(regalo.id), "Reversión por anulación de regalo de servicio",
                    )
                }
            repository.updateServicio(
                servicio.copy(
                    estado = OrderStatusPolicy.ANULADO,
                    motivoAnulacion = reason,
                    fechaAnulacion = DateUtils.today(),
                    updatedAt = Instant.now().toString(),
                ),
            )
            LifecycleOutcome.Applied
        }
        if (outcome == LifecycleOutcome.Applied) {
            postSaveSyncScheduler.scheduleFinanzasSync(opticaId)
            postSaveSyncScheduler.scheduleInventarioSync(opticaId)
        }
        return outcome
    }

    private suspend fun restockFrames(servicio: ServicioExtra, opticaId: String) {
        val itemsWithStock = repository.getServicioExtraItems(servicio.id, opticaId).filter { !it.monturaId.isNullOrBlank() }
        if (itemsWithStock.isEmpty()) {
            servicio.monturaId?.takeIf { it.isNotBlank() }?.let { monturaId ->
                stockHelper.restockOnce(
                    monturaId, opticaId, 1, movimientoReferenciaForServicioExtraReverso(servicio.id, monturaId), NOTA_SERVICIO,
                )
            }
            return
        }
        itemsWithStock.forEach { item ->
            stockHelper.restockOnce(item.monturaId!!, opticaId, 1, movimientoReferenciaForServicioItemAnulacion(item.id), NOTA_SERVICIO)
        }
    }
}

private const val NOTA_SERVICIO = "Reversión por anulación de servicio extra"

sealed interface LifecycleOutcome {
    data object Applied : LifecycleOutcome
    data class AlreadyTerminal(val estado: String) : LifecycleOutcome
}

private const val MOTIVO_MAX_LENGTH = 500

internal fun normalizeMotivo(motivo: String): String {
    val trimmed = motivo.trim()
    require(trimmed.isNotEmpty()) { "El motivo es obligatorio." }
    require(trimmed.length <= MOTIVO_MAX_LENGTH) { "El motivo no puede superar $MOTIVO_MAX_LENGTH caracteres." }
    return trimmed
}

/**
 * The estado check is the first statement of the transaction so a retried or overlapping
 * cancel finds the order already terminal and writes nothing.
 */
class AnularDispensacionUseCase @Inject constructor(
    private val repository: OptoRepository,
    private val pagoDao: PagoDao,
    private val stockHelper: DispensacionStockHelper,
    private val postSaveSyncScheduler: PostSaveSyncScheduler,
    private val calcularMontoPagado: CalcularMontoPagadoUseCase,
) {
    suspend operator fun invoke(dispensacionId: String, opticaId: String, motivo: String): LifecycleOutcome {
        val reason = normalizeMotivo(motivo)
        val outcome = repository.withTransaction {
            val disp = (repository.getDispensacionById(dispensacionId, opticaId) as? Resource.Success)?.data
                ?: throw IllegalStateException("Dispensación no encontrada.")
            if (OrderStatusPolicy.isTerminal(disp.estadoEntrega)) {
                return@withTransaction LifecycleOutcome.AlreadyTerminal(disp.estadoEntrega.trim())
            }
            check(OrderStatusPolicy.canCancel(disp.estadoEntrega)) {
                "No se puede anular una orden en estado ${disp.estadoEntrega}."
            }
            reverseLedgerFully(repository, pagoDao, dispensacionId, opticaId, forDispensacion = true, contexto = "anulación")
            restockFrames(disp, opticaId)
            restockRegalos(dispensacionId, opticaId)
            repository.updateDispensacion(
                disp.copy(
                    estadoEntrega = OrderStatusPolicy.ANULADO,
                    motivoAnulacion = reason,
                    fechaAnulacion = DateUtils.today(),
                    montoPagado = calcularMontoPagado(dispensacionId, opticaId),
                ),
            )
            LifecycleOutcome.Applied
        }
        if (outcome == LifecycleOutcome.Applied) {
            postSaveSyncScheduler.scheduleFinanzasSync(opticaId)
            postSaveSyncScheduler.scheduleInventarioSync(opticaId)
        }
        return outcome
    }

    private suspend fun restockFrames(disp: DispensacionOptica, opticaId: String) {
        val items = repository.getDispensacionItemsByDispensacion(disp.id, opticaId)
        if (items.isEmpty()) {
            if (OrigenMontura.isTienda(disp.origenMontura) && disp.monturaId.isNotBlank()) {
                restock(disp.monturaId, opticaId, 1, movimientoReferenciaForDispensacionHeaderAnulacion(disp.id, disp.monturaId))
            }
            return
        }
        items.filter { OrigenMontura.isTienda(it.origenMontura) && it.monturaId.isNotBlank() }.forEach { item ->
            restock(item.monturaId, opticaId, 1, movimientoReferenciaForDispensacionItemAnulacion(disp.id, item.id))
        }
    }

    private suspend fun restockRegalos(dispensacionId: String, opticaId: String) {
        repository.getRegalosByDispensacionId(dispensacionId, opticaId)
            .filter { it.productoId.isNotBlank() }
            .forEach { regalo ->
                restock(regalo.productoId, opticaId, regalo.cantidad, movimientoReferenciaForRegaloAnulacion(regalo.id))
            }
    }

    private suspend fun restock(monturaId: String, opticaId: String, delta: Int, referenciaId: String) {
        stockHelper.restockOnce(monturaId, opticaId, delta, referenciaId, "Reversión por anulación de dispensación")
    }
}

/**
 * Hard delete is only for orders that left no trace: with any pago or stock movement the order
 * must be cancelled instead, so the ledger and stock audit stay intact.
 */
class EliminarDispensacionUseCase @Inject constructor(
    private val repository: OptoRepository,
    private val pagoDao: PagoDao,
    private val inventoryCoordinator: MonturaInventoryCoordinator,
) {
    suspend operator fun invoke(dispensacionId: String, opticaId: String) {
        repository.withTransaction {
            val disp = (repository.getDispensacionById(dispensacionId, opticaId) as? Resource.Success)?.data
                ?: throw IllegalStateException("Dispensación no encontrada.")
            check(!hasTrace(dispensacionId, opticaId)) { "La orden tiene pagos o movimientos de stock. Usa Anular." }
            repository.deleteDispensacion(disp)
        }
    }

    suspend fun hasTrace(dispensacionId: String, opticaId: String): Boolean {
        val regaloIds = repository.getRegalosByDispensacionId(dispensacionId, opticaId).map { it.id }
        return pagoDao.countByDispensacion(dispensacionId, opticaId) > 0 ||
            inventoryCoordinator.countMovimientosForDispensacion(dispensacionId, regaloIds, opticaId) > 0
    }
}

sealed interface ReclamoOutcome {
    data class Created(val replacementId: String, val replacementOt: String) : ReclamoOutcome
    data class AlreadyTerminal(val estado: String) : ReclamoOutcome
}

class ReclamoStockInsuficienteException(val monturaId: String, val montura: String) :
    IllegalStateException("Sin stock de la montura $montura para el reemplazo. No se registró el reclamo.")

/** Ledger amounts at or below this are treated as zero; the claim preview must use the same threshold. */
internal const val MONEY_EPSILON = 0.005

fun lastCreditMetodo(pagos: List<Pago>): String? = pagos
    .filter { PagoEffect.signedAmount(it.tipo, it.monto) > 0.0 && it.metodoPago.isNotBlank() }
    .maxWithOrNull(compareBy<Pago> { it.fecha }.thenBy { it.updatedAt.orEmpty() })
    ?.metodoPago

/**
 * The original becomes Reclamada and a `-R<n>` replacement receives the original's net paid per
 * metodo, so each metodo nets 0 in Cierre de Caja and only the refund of the excess moves cash.
 * The estado check is the first statement of the transaction so a retried claim writes nothing.
 * Regalos are not copied to the replacement.
 */
class ReclamarDispensacionUseCase @Inject constructor(
    private val repository: OptoRepository,
    private val pagoDao: PagoDao,
    private val stockHelper: DispensacionStockHelper,
    private val postSaveSyncScheduler: PostSaveSyncScheduler,
    private val calcularMontoPagado: CalcularMontoPagadoUseCase,
) {
    suspend operator fun invoke(
        originalId: String,
        opticaId: String,
        motivo: String,
        nuevoMontoTotal: Double,
        metodoReembolso: String,
    ): ReclamoOutcome {
        val reason = normalizeMotivo(motivo)
        require(nuevoMontoTotal.isFinite() && nuevoMontoTotal >= 0.0) { "El nuevo monto total debe ser mayor o igual a 0." }
        val outcome = repository.withTransaction {
            val original = (repository.getDispensacionById(originalId, opticaId) as? Resource.Success)?.data
                ?: throw IllegalStateException("Dispensación no encontrada.")
            if (original.estadoEntrega.trim() == OrderStatusPolicy.RECLAMADA) {
                return@withTransaction ReclamoOutcome.AlreadyTerminal(OrderStatusPolicy.RECLAMADA)
            }
            check(OrderStatusPolicy.canClaim(original.estadoEntrega)) {
                "Solo se puede reclamar una orden entregada (estado actual: ${original.estadoEntrega})."
            }
            val snapshot = ledgerSnapshot(pagoDao.getPagosByParent(originalId, opticaId))
            requireTransferable(snapshot, calcularMontoPagado(originalId, opticaId))
            val reembolso = (snapshot.netPaid - nuevoMontoTotal).takeIf { it > MONEY_EPSILON }
            require(reembolso == null || metodoReembolso.isNotBlank()) { "Selecciona el método de reembolso." }
            val replacement = insertReplacement(original, nuevoMontoTotal)
            reverseLedgerFully(repository, pagoDao, originalId, opticaId, forDispensacion = true, contexto = "reclamo")
            transferCredit(snapshot, replacement, original.ot, reembolso, metodoReembolso.trim())
            repository.updateDispensacion(
                original.copy(
                    estadoEntrega = OrderStatusPolicy.RECLAMADA,
                    motivoAnulacion = reason,
                    fechaAnulacion = DateUtils.today(),
                    montoPagado = calcularMontoPagado(originalId, opticaId),
                ),
            )
            repository.updateDispensacion(replacement.copy(montoPagado = calcularMontoPagado(replacement.id, opticaId)))
            ReclamoOutcome.Created(replacement.id, replacement.ot)
        }
        if (outcome is ReclamoOutcome.Created) {
            postSaveSyncScheduler.scheduleFinanzasSync(opticaId)
            postSaveSyncScheduler.scheduleInventarioSync(opticaId)
        }
        return outcome
    }

    private fun requireTransferable(snapshot: LedgerSnapshot, persistedNetPaid: Double) {
        val netPaid = snapshot.netPaid
        check(netPaid >= -MONEY_EPSILON && abs(netPaid - persistedNetPaid) <= MONEY_EPSILON) {
            "Saldo pagado inconsistente en la orden original; sincroniza y reintenta."
        }
    }

    private suspend fun insertReplacement(original: DispensacionOptica, nuevoMontoTotal: Double): DispensacionOptica {
        val replacement = original.copy(
            id = UUID.randomUUID().toString(),
            ot = repository.nextReclamoOt(original.opticaId, original.ot, DateUtils.today()),
            fecha = DateUtils.today(),
            estadoEntrega = OrderStatusPolicy.PENDIENTE,
            fechaEntrega = null,
            fechaVencimientoGarantia = null,
            montoTotal = nuevoMontoTotal,
            montoPagado = 0.0,
            reclamoOrigenId = original.id,
            motivoAnulacion = null,
            fechaAnulacion = null,
        )
        repository.insertDispensacion(replacement)
        val items = repository.getDispensacionItemsByDispensacion(original.id, original.opticaId)
        items.forEach { item ->
            repository.insertDispensacionItem(item.copy(id = UUID.randomUUID().toString(), dispensacionId = replacement.id))
        }
        val frames = if (items.isEmpty()) {
            listOf(CopiedFrame(replacement.origenMontura, replacement.monturaId, replacement.descripcionMontura))
        } else {
            items.map { CopiedFrame(it.origenMontura, it.monturaId, it.descripcionMontura) }
        }
        frames
            .filter { OrigenMontura.isTienda(it.origen) && it.monturaId.isNotBlank() }
            .forEach { consumeFrame(replacement, it) }
        return replacement
    }

    private class CopiedFrame(val origen: String, val monturaId: String, val descripcion: String)

    private suspend fun consumeFrame(replacement: DispensacionOptica, frame: CopiedFrame) {
        stockHelper.adjustStockAndRegistrarMovimiento(
            frame.monturaId, replacement.opticaId, -1, "SALIDA_VENTA", replacement.id, "Venta por reclamo de OT ${replacement.ot}",
        ).getOrElse { throw ReclamoStockInsuficienteException(frame.monturaId, frame.descripcion.ifBlank { frame.monturaId }) }
    }

    private suspend fun transferCredit(
        snapshot: LedgerSnapshot,
        replacement: DispensacionOptica,
        originalOt: String,
        reembolso: Double?,
        metodoReembolso: String,
    ) {
        snapshot.netByMetodo.filterValues { it > MONEY_EPSILON }.forEach { (metodo, monto) ->
            repository.insertPago(replacementPago(replacement, "Abono", monto, metodo, "Crédito por reclamo de OT $originalOt"))
        }
        snapshot.netByMetodo.filterValues { it < -MONEY_EPSILON }.forEach { (metodo, monto) ->
            repository.insertPago(replacementPago(replacement, TIPO_REEMBOLSO, -monto, metodo, "Ajuste de crédito por reclamo de OT $originalOt"))
        }
        if (reembolso != null) {
            repository.insertPago(replacementPago(replacement, TIPO_REEMBOLSO, reembolso, metodoReembolso, "Reembolso por reclamo de OT $originalOt"))
        }
    }

    private fun replacementPago(replacement: DispensacionOptica, tipo: String, monto: Double, metodo: String, nota: String) = Pago(
        id = UUID.randomUUID().toString(),
        dispensacionId = replacement.id,
        fecha = DateUtils.today(),
        tipo = tipo,
        monto = monto,
        metodoPago = metodo,
        nota = nota,
        opticaId = replacement.opticaId,
        ventaId = "v_disp_${replacement.id}",
        updatedAt = Instant.now().toString(),
    )
}
