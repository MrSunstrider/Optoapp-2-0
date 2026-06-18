package com.example.optoapp.data.montura

import android.util.Log
import com.example.optoapp.data.Montura
import com.example.optoapp.data.MonturaMovimiento
import com.example.optoapp.data.Resource
import com.example.optoapp.sync.PostSaveSyncScheduler
import dagger.Lazy
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import java.io.IOException
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject

class MonturaInventoryCoordinator @Inject constructor(
    private val monturaDao: MonturaDao,
    private val monturaMovimientoDao: MonturaMovimientoDao,
    private val postSaveSyncScheduler: Lazy<PostSaveSyncScheduler>
) {
    companion object {
        private const val TAG = "MonturaInventoryCoordinator"
    }

    fun getMonturasByOptica(opticaId: String): Flow<List<Montura>> =
        monturaDao.getMonturasByOptica(opticaId)

    suspend fun getMonturaById(id: String): Resource<Montura> {
        return try {
            val montura = monturaDao.getMonturaById(id)
            if (montura != null) Resource.Success(montura)
            else Resource.Error("Montura no encontrada")
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            Log.e(TAG, "Error de red al obtener montura", e)
            Resource.Error(e.message ?: "Error al obtener montura")
        } catch (e: Exception) {
            Log.e(TAG, "Error inesperado al obtener montura", e)
            Resource.Error(e.message ?: "Error al obtener montura")
        }
    }

    suspend fun insertMontura(montura: Montura) {
        val stamped = montura.copy(updatedAt = Instant.now().toString())
        monturaDao.insertMontura(stamped)
        postSaveSyncScheduler.get().scheduleInventarioSync(stamped.opticaId)
    }

    suspend fun updateMontura(montura: Montura) {
        val stamped = montura.copy(updatedAt = Instant.now().toString())
        monturaDao.updateMontura(stamped)
        postSaveSyncScheduler.get().scheduleInventarioSync(stamped.opticaId)
    }

    suspend fun deleteMontura(montura: Montura) {
        monturaDao.deleteMontura(montura)
        postSaveSyncScheduler.get().scheduleInventarioSync(montura.opticaId)
    }

    suspend fun adjustMonturaStock(monturaId: String, opticaId: String, delta: Int): Int {
        val changed = monturaDao.adjustStock(monturaId, opticaId, delta)
        if (changed > 0) postSaveSyncScheduler.get().scheduleInventarioSync(opticaId)
        return changed
    }

    fun getMovimientosMonturaByOptica(opticaId: String): Flow<List<MonturaMovimiento>> =
        monturaMovimientoDao.getMovimientosByOptica(opticaId)

    fun getMovimientosByMontura(monturaId: String): Flow<List<MonturaMovimiento>> =
        monturaMovimientoDao.getMovimientosByMontura(monturaId)

    suspend fun insertMonturaMovimiento(movimiento: MonturaMovimiento) {
        monturaMovimientoDao.insertMovimiento(movimiento)
        postSaveSyncScheduler.get().scheduleInventarioSync(movimiento.opticaId)
    }

    suspend fun registrarSalida(
        monturaId: String,
        opticaId: String,
        cantidad: Int,
        userId: String,
        costoUnitario: Double,
        tipoDocumento: String,
        referenciaId: String,
        nota: String
    ): Resource<Unit> {
        try {
            val montura = monturaDao.getMonturaById(monturaId)
                ?: return Resource.Error("Montura no encontrada: $monturaId")
            if (montura.stockActual < cantidad) {
                return Resource.Error("Stock insuficiente: actual=${montura.stockActual}, salida=$cantidad")
            }
            val nuevoStock = montura.stockActual - cantidad
            adjustMonturaStock(monturaId, opticaId, -cantidad)
            val movimiento = MonturaMovimiento(
                id = UUID.randomUUID().toString(),
                monturaId = monturaId,
                fecha = LocalDate.now(),
                tipo = "SALIDA_VENTA",
                cantidad = cantidad,
                stockPrevio = montura.stockActual,
                stockNuevo = nuevoStock,
                referenciaId = referenciaId,
                nota = nota,
                opticaId = opticaId,
                userId = userId,
                costoUnitario = costoUnitario,
                tipoDocumento = tipoDocumento
            )
            monturaMovimientoDao.insertMovimiento(movimiento)
            postSaveSyncScheduler.get().scheduleInventarioSync(opticaId)
            return Resource.Success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error al registrar salida de inventario", e)
            return Resource.Error(e.message ?: "Error al registrar salida")
        }
    }

    suspend fun syncStockFromMovimientos(opticaId: String): Resource<Int> {
        try {
            val monturas = monturaDao.getMonturasListByOptica(opticaId)
            val movimientos = monturaMovimientoDao.getMovimientosListByOptica(opticaId)
            val stockByMontura = movimientos.groupBy { it.monturaId }
                .mapValues { (_, movs) ->
                    val sorted = movs.sortedBy { it.fecha }
                    sorted.lastOrNull()?.stockNuevo ?: 0
                }
            var updated = 0
            for (montura in monturas) {
                val calculatedStock = stockByMontura[montura.id] ?: montura.stockActual
                if (montura.stockActual != calculatedStock) {
                    val delta = calculatedStock - montura.stockActual
                    monturaDao.adjustStock(montura.id, opticaId, delta)
                    updated++
                }
            }
            if (updated > 0) postSaveSyncScheduler.get().scheduleInventarioSync(opticaId)
            return Resource.Success(updated)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error al reconstruir stock desde movimientos", e)
            return Resource.Error(e.message ?: "Error al sincronizar stock")
        }
    }
}
