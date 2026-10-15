package com.example.optoapp.data

import com.example.optoapp.data.montura.MonturaDao
import com.example.optoapp.data.montura.MonturaInventoryCoordinator
import com.example.optoapp.data.ordencompra.OrdenCompraDao
import com.example.optoapp.data.ordencompra.OrdenCompraItemDao
import com.example.optoapp.domain.CostoPromedioPonderado
import com.example.optoapp.domain.movimientoReferenciaForOrdenCompraItem
import com.example.optoapp.sync.PostSaveSyncScheduler
import dagger.Lazy
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Keeps purchase-order logic isolated from OptoRepository's god-repository coupling.
 */
@Singleton
open class OrdenCompraRepository @Inject constructor(
    private val ocDao: OrdenCompraDao,
    private val itemDao: OrdenCompraItemDao,
    private val monturaDao: MonturaDao,
    private val monturaCoordinator: MonturaInventoryCoordinator,
    private val transactionRunner: DatabaseTransactionRunner,
    private val postSaveSyncScheduler: Lazy<PostSaveSyncScheduler>,
) {
    fun getByOptica(opticaId: String): Flow<List<OrdenCompra>> = ocDao.getByOptica(opticaId)

    suspend fun getListByOptica(opticaId: String): List<OrdenCompra> = ocDao.getListByOptica(opticaId)

    suspend fun getById(id: String, opticaId: String): OrdenCompra? = ocDao.getById(id, opticaId)

    suspend fun getItems(ordenId: String): List<OrdenCompraItem> = itemDao.getByOrden(ordenId)

    suspend fun getOrdenItemById(id: String): OrdenCompraItem? = itemDao.getById(id)

    open suspend fun create(oc: OrdenCompra, items: List<OrdenCompraItem>) {
        val total = items.sumOf { it.cantidad.toDouble() * it.costoUnitario }
        val stamped = oc.copy(total = total, updatedAt = Instant.now().toString())
        ocDao.insert(stamped)
        items.forEach { itemDao.insert(it) }
        postSaveSyncScheduler.get().scheduleOrdenCompraSync(oc.opticaId)
    }

    open suspend fun update(oc: OrdenCompra) {
        val stamped = oc.copy(updatedAt = Instant.now().toString())
        ocDao.update(stamped)
    }

    open suspend fun updateEstado(ocId: String, newEstado: String, opticaId: String) {
        val existing = ocDao.getById(ocId, opticaId) ?: return
        ocDao.update(existing.copy(estado = newEstado, updatedAt = Instant.now().toString()))
        postSaveSyncScheduler.get().scheduleOrdenCompraSync(existing.opticaId)
    }

    /**
     * Records received quantities. Stock and weighted average cost move only when the whole order
     * is received, using each item's cumulative quantity, so a partial receipt never averages a
     * cost twice.
     *
     * Item quantities, order state, ENTRADA movements, stock and cost commit together or not at
     * all: a failure throws and leaves the order exactly as it was, so the receipt can be retried.
     * The ENTRADA is claimed first through its unique key, which makes a repeated receipt a no-op.
     * Sync is scheduled only after the commit.
     */
    open suspend fun receiveItems(ocId: String, opticaId: String, receivedQuantities: Map<String, Int>) {
        val oc = ocDao.getById(ocId, opticaId) ?: return
        val stockMoved = transactionRunner.inTransaction { recordReceipt(oc, receivedQuantities) }
        if (stockMoved) monturaCoordinator.scheduleInventarioSync(oc.opticaId)
        postSaveSyncScheduler.get().scheduleOrdenCompraSync(oc.opticaId)
    }

    private suspend fun recordReceipt(oc: OrdenCompra, receivedQuantities: Map<String, Int>): Boolean {
        val items = itemDao.getByOrden(oc.id).map { it.copy(recibido = receivedQuantities[it.id] ?: it.recibido) }
        items.forEach { itemDao.update(it) }

        val fullyReceived = items.isNotEmpty() && items.none { it.recibido < it.cantidad }
        val now = Instant.now().toString()
        ocDao.update(
            oc.copy(
                estado = if (fullyReceived) "COMPLETADA" else "PARCIAL",
                total = items.sumOf { it.recibido.toDouble() * it.costoUnitario },
                updatedAt = now,
            ),
        )
        if (!fullyReceived) return false

        var stockMoved = false
        items.filter { it.recibido > 0 }.forEach { item ->
            if (enterStock(oc, item, now)) stockMoved = true
        }
        return stockMoved
    }

    private suspend fun enterStock(oc: OrdenCompra, item: OrdenCompraItem, now: String): Boolean {
        val montura = checkNotNull(monturaDao.getMonturaByIdForOptica(item.monturaId, oc.opticaId)) {
            "Montura no encontrada: ${item.monturaId}"
        }
        val claimed = monturaCoordinator.insertMonturaMovimientoIfAbsent(
            MonturaMovimiento(
                id = UUID.randomUUID().toString(),
                monturaId = item.monturaId,
                fecha = LocalDate.now(),
                tipo = "ENTRADA",
                cantidad = item.recibido,
                stockPrevio = montura.stockActual,
                stockNuevo = montura.stockActual + item.recibido,
                referenciaId = movimientoReferenciaForOrdenCompraItem(item.id),
                nota = "Recepcion OC ${oc.numero}",
                opticaId = oc.opticaId,
                userId = oc.updatedBy ?: "",
                costoUnitario = item.costoUnitario,
                tipoDocumento = "OC",
            ),
        )
        if (!claimed) return false

        if (item.costoUnitario > 0.0) {
            val costo = CostoPromedioPonderado.calcular(montura.stockActual, montura.costo, item.recibido, item.costoUnitario)
            check(monturaDao.updateCosto(item.monturaId, oc.opticaId, costo, now) > 0) { "No se pudo actualizar el costo" }
        }
        check(monturaCoordinator.adjustMonturaStockLocal(item.monturaId, oc.opticaId, item.recibido) > 0) {
            "No se pudo ajustar el stock"
        }
        return true
    }

    open suspend fun delete(ocId: String, opticaId: String) {
        val oc = ocDao.getById(ocId, opticaId) ?: return
        itemDao.deleteByOrden(ocId, oc.opticaId)
        ocDao.update(oc.copy(estado = "CANCELADA", updatedAt = Instant.now().toString()))
        postSaveSyncScheduler.get().scheduleOrdenCompraSync(oc.opticaId)
    }

    open suspend fun upsertOrdenCompra(oc: OrdenCompra) {
        val existing = ocDao.getById(oc.id, oc.opticaId)
        if (existing != null) {
            ocDao.update(oc)
        } else {
            runCatching { ocDao.insert(oc) }
        }
    }

    open suspend fun upsertItem(item: OrdenCompraItem) {
        val existing = itemDao.getByOrden(item.ordenId).find { it.id == item.id }
        if (existing != null) {
            itemDao.update(item)
        } else {
            runCatching { itemDao.insert(item) }
        }
    }
}
