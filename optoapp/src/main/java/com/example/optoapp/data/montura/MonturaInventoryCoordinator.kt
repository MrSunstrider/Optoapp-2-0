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
}
