package com.example.optoapp.domain

import com.example.optoapp.data.OptoRepository
import com.example.optoapp.data.Pago
import com.example.optoapp.data.Resource
import com.example.optoapp.data.pago.PagoDao
import com.example.optoapp.domain.movimientoReferenciaForRegalo
import com.example.optoapp.sync.PostSaveSyncScheduler
import com.example.optoapp.util.DispensacionStockHelper
import com.example.optoapp.util.DateUtils
import java.time.Instant
import java.util.UUID
import javax.inject.Inject

internal suspend fun insertMissingReversos(
    repository: OptoRepository,
    pagoDao: PagoDao,
    parentId: String,
    opticaId: String,
    forDispensacion: Boolean,
) {
    for (credit in pagoDao.getCreditPagosByParent(parentId, opticaId)) {
        if (pagoDao.getReversoByOriginalId(credit.id, opticaId) != null) continue
        repository.insertPago(
            Pago(
                id = UUID.randomUUID().toString(),
                dispensacionId = if (forDispensacion) parentId else null,
                servicioExtraId = if (forDispensacion) null else parentId,
                fecha = DateUtils.today(),
                tipo = "Reverso",
                monto = credit.monto,
                metodoPago = credit.metodoPago,
                nota = "Reverso de ${credit.tipo} ${credit.id.take(8)}",
                opticaId = opticaId,
                ventaId = credit.ventaId,
                reversaPagoId = credit.id,
                updatedAt = Instant.now().toString(),
            ),
        )
    }
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
