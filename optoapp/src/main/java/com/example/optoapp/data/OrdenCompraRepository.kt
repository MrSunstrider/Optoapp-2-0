package com.example.optoapp.data

import android.util.Log
import com.example.optoapp.data.montura.MonturaInventoryCoordinator
import com.example.optoapp.data.ordencompra.OrdenCompraDao
import com.example.optoapp.data.ordencompra.OrdenCompraItemDao
import com.example.optoapp.domain.movimientoReferenciaForOrdenCompraItem
import com.example.optoapp.sync.PostSaveSyncScheduler
import dagger.Lazy
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import java.io.IOException
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Keeps purchase-order logic isolated from OptoRepository's god-repository coupling.
 */
@Singleton
open class OrdenCompraRepository @Inject constructor(
    private val ocDao: OrdenCompraDao,
    private val itemDao: OrdenCompraItemDao,
    private val monturaCoordinator: MonturaInventoryCoordinator,
    private val postSaveSyncScheduler: Lazy<PostSaveSyncScheduler>,
) {
    companion object {
        private const val TAG = "OrdenCompraRepo"
    }

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
     * Updates recibido for individual items. If all items are fully received,
     * transition estado to COMPLETADA and create ENTRADA movements.
     */
    open suspend fun receiveItems(ocId: String, opticaId: String, receivedQuantities: Map<String, Int>) {
        val oc = ocDao.getById(ocId, opticaId) ?: return

        var allFullyReceived = true
        val items = itemDao.getByOrden(ocId)
        val updatedItems = items.map { item ->
            val qty = receivedQuantities[item.id] ?: item.recibido
            val updated = item.copy(recibido = qty)
            if (qty < item.cantidad) allFullyReceived = false
            updated
        }

        updatedItems.forEach { itemDao.update(it) }

        val total = updatedItems.sumOf { it.recibido.toDouble() * it.costoUnitario }

        if (allFullyReceived && updatedItems.isNotEmpty()) {
            ocDao.update(
                oc.copy(
                    estado = "COMPLETADA",
                    total = total,
                    updatedAt = Instant.now().toString(),
                ),
            )
            // Stock must increase to reflect physical inventory after receipt
            updatedItems.forEach { item ->
                val stockChanged = monturaCoordinator.adjustMonturaStock(
                    item.monturaId,
                    oc.opticaId,
                    item.recibido,
                )
                if (stockChanged > 0) {
                    try {
                        val movimiento = MonturaMovimiento(
                            id = java.util.UUID.randomUUID().toString(),
                            monturaId = item.monturaId,
                            fecha = java.time.LocalDate.now(),
                            tipo = "ENTRADA",
                            cantidad = item.recibido,
                            stockPrevio = 0,
                            stockNuevo = 0,
                            referenciaId = movimientoReferenciaForOrdenCompraItem(item.id),
                            nota = "Recepcion OC ${oc.numero}",
                            opticaId = oc.opticaId,
                            userId = oc.updatedBy ?: "",
                            costoUnitario = item.costoUnitario,
                            tipoDocumento = "OC",
                        )
                        monturaCoordinator.insertMonturaMovimiento(movimiento)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: IOException) {
                        Log.e(TAG, "Error de red al registrar movimiento de recepcion: ${e.message}", e)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error al registrar movimiento de recepcion: ${e.message}", e)
                    }
                }
            }
        } else {
            ocDao.update(
                oc.copy(
                    total = total,
                    estado = "PARCIAL",
                    updatedAt = Instant.now().toString(),
                ),
            )
        }
        postSaveSyncScheduler.get().scheduleOrdenCompraSync(oc.opticaId)
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
