package com.example.optoapp.domain

import com.example.optoapp.data.DatabaseTransactionRunner
import com.example.optoapp.data.SyncStateTracker
import com.example.optoapp.data.serviciotipo.ServicioTipoDao
import com.example.optoapp.data.serviciotipo.ServicioTipoEntity
import java.time.Instant
import javax.inject.Inject

/**
 * Types are deactivated, never deleted: saved servicios keep pointing at them by id. Every write stamps
 * updatedAt and marks the row in the same transaction so upload LWW never sees an edit without its timestamp.
 */
class ServicioTipoCatalogUseCase @Inject constructor(
    private val servicioTipoDao: ServicioTipoDao,
    private val syncStateTracker: SyncStateTracker,
    private val transactionRunner: DatabaseTransactionRunner,
) {

    suspend fun guardar(tipo: ServicioTipoEntity) {
        transactionRunner.inTransaction { escribir(tipo) }
    }

    suspend fun desactivar(id: String, opticaId: String) = cambiarActivo(id, opticaId, activo = false)

    suspend fun reactivar(id: String, opticaId: String) = cambiarActivo(id, opticaId, activo = true)

    private suspend fun cambiarActivo(id: String, opticaId: String, activo: Boolean) {
        transactionRunner.inTransaction {
            val actual = servicioTipoDao.getById(id, opticaId) ?: return@inTransaction
            escribir(actual.copy(activo = activo))
        }
    }

    private suspend fun escribir(tipo: ServicioTipoEntity) {
        servicioTipoDao.upsert(tipo.copy(updatedAt = Instant.now().toString()))
        syncStateTracker.markLocallyModified(tipo.opticaId, ENTITY_TYPE, tipo.id)
    }

    companion object {
        const val ENTITY_TYPE = "servicio_tipo"
    }
}
