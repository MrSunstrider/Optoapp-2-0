package com.example.optoapp.data

import android.util.Log
import com.example.optoapp.data.inventariofisico.InventarioFisicoDao
import com.example.optoapp.data.montura.MonturaDao
import com.example.optoapp.data.montura.MonturaInventoryCoordinator
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import java.io.IOException
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Keeps physical inventory logic isolated from OptoRepository's god-repository coupling.
 */
@Singleton
open class InventarioFisicoRepository @Inject constructor(
    private val ifDao: InventarioFisicoDao,
    private val monturaCoordinator: MonturaInventoryCoordinator,
    private val monturaDao: MonturaDao
) {
    companion object {
        private const val TAG = "InventarioFisicoRepo"
    }

    fun getByOptica(opticaId: String): Flow<List<InventarioFisico>> =
        ifDao.getByOptica(opticaId)

    suspend fun getById(id: String): InventarioFisico? =
        ifDao.getById(id)

    suspend fun getListByOptica(opticaId: String): List<InventarioFisico> =
        ifDao.getListByOptica(opticaId)

    suspend fun getDetalles(inventarioId: String): List<InventarioFisicoDetalle> =
        ifDao.getDetalles(inventarioId)

    suspend fun getActiveByOptica(opticaId: String): InventarioFisico? =
        ifDao.getActiveByOptica(opticaId)

    open suspend fun createSession(opticaId: String, userId: String): InventarioFisico? {
        try {
            val existing = ifDao.getActiveByOptica(opticaId)
            if (existing != null) return null

            val session = InventarioFisico(
                id = UUID.randomUUID().toString(),
                fecha = LocalDate.now(),
                estado = "EN_PROGRESO",
                opticaId = opticaId,
                userId = userId,
                updatedAt = Instant.now().toString()
            )
            ifDao.insertSession(session)

            val monturas = monturaDao.getMonturasListByOptica(opticaId)
            val detalles = monturas.map { m ->
                InventarioFisicoDetalle(
                    id = UUID.randomUUID().toString(),
                    inventarioId = session.id,
                    monturaId = m.id,
                    stockSistema = m.stockActual
                )
            }
            if (detalles.isNotEmpty()) {
                ifDao.insertDetalles(detalles)
            }
            return session
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error creando sesion de inventario fisico", e)
            return null
        }
    }

    open suspend fun closeSession(sessionId: String) {
        val session = ifDao.getById(sessionId) ?: return
        val detalles = ifDao.getDetalles(sessionId)
        for (d in detalles) {
            val diff = d.diferencia ?: 0
            if (diff == 0) continue

            val stockPrevio = d.stockSistema
            val stockNuevo = stockPrevio + diff
            val cantidadAbs = kotlin.math.abs(diff)

            monturaCoordinator.adjustMonturaStock(d.monturaId, session.opticaId, diff)
            val movimiento = MonturaMovimiento(
                id = UUID.randomUUID().toString(),
                monturaId = d.monturaId,
                fecha = LocalDate.now(),
                tipo = "AJUSTE_INVENTARIO",
                cantidad = cantidadAbs,
                stockPrevio = stockPrevio,
                stockNuevo = stockNuevo,
                referenciaId = sessionId,
                nota = "Ajuste inventario fisico: $sessionId",
                opticaId = session.opticaId,
                userId = session.userId,
                costoUnitario = 0.0,
                tipoDocumento = "AJUSTE_INVENTARIO"
            )
            monturaCoordinator.insertMonturaMovimiento(movimiento)
        }
        ifDao.updateSession(session.copy(estado = "COMPLETADO", updatedAt = Instant.now().toString()))
    }

    open suspend fun upsertSession(session: InventarioFisico) {
        val existing = ifDao.getById(session.id)
        if (existing != null) {
            ifDao.updateSession(session)
        } else {
            runCatching { ifDao.insertSession(session) }
        }
    }

    open suspend fun upsertDetalle(detalle: InventarioFisicoDetalle) {
        val existing = ifDao.getDetalles(detalle.inventarioId).find { it.id == detalle.id }
        if (existing != null) {
            ifDao.updateDetalle(detalle)
        } else {
            runCatching { ifDao.insertDetalles(listOf(detalle)) }
        }
    }
}
