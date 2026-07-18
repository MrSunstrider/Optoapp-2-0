package com.example.optoapp.domain.sync

import com.example.optoapp.util.AppLogger
import com.example.optoapp.data.OptoRepository
import com.example.optoapp.data.OrdenCompraRepository
import com.example.optoapp.data.ProveedorRepository
import com.example.optoapp.data.Resource
import com.example.optoapp.data.SessionManager
import kotlinx.coroutines.flow.first

/**
 * Strategy for bumping an entity's `updatedAt` timestamp to resolve "keep mine" conflicts.
 *
 * Each entity type has a handler that fetches the entity from Room, then calls the
 * corresponding repository update method (which auto-stamps `updatedAt = Instant.now()`).
 */
class BumpEntityStrategy(
    private val repository: OptoRepository,
    private val proveedorRepository: ProveedorRepository,
    private val ordenCompraRepository: OrdenCompraRepository,
    private val sessionManager: SessionManager
) {
    private val TAG = "BumpEntityStrategy"

    /**
     * Bumps the entity identified by [entityId] and [entityType], delegating to the
     * appropriate handler in [bumpHandlers].
     */
    suspend fun bump(entityId: String, entityType: String) {
        val opticaId = sessionManager.opticaId.first()
        val handler = bumpHandlers[entityType]
        if (handler != null) {
            handler(entityId, opticaId)
        } else {
            AppLogger.d(TAG, "bumpEntityUpdatedAt: tipo no aplica bump: $entityType")
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────

    private suspend fun <T> bumpWithResource(
        entityId: String, entityType: String,
        fetcher: suspend () -> Resource<T>,
        updater: suspend (T) -> Unit
    ) {
        val result = fetcher()
        if (result is Resource.Success && result.data != null) {
            updater(result.data)
        } else {
            AppLogger.w(TAG, "bumpEntityUpdatedAt: $entityType no encontrado id=$entityId")
        }
    }

    private suspend fun <T> bumpWithNullable(
        entityId: String, entityType: String,
        fetcher: suspend () -> T?,
        updater: suspend (T) -> Unit
    ) {
        val entity = fetcher()
        if (entity != null) {
            updater(entity)
        } else {
            AppLogger.w(TAG, "bumpEntityUpdatedAt: $entityType no encontrado id=$entityId")
        }
    }

    // ── Handler map (entity type → bump logic) ───────────────────────────

    private val bumpHandlers: Map<String, suspend (entityId: String, opticaId: String) -> Unit> = mapOf(
        "servicio_extra" to { id, _ ->
            bumpWithResource(id, "servicio", { repository.getServicioById(id) }, { repository.updateServicio(it) })
        },
        "dispensacion" to { id, _ ->
            bumpWithResource(id, "dispensacion", { repository.getDispensacionById(id) }, { repository.updateDispensacion(it) })
        },
        "pago" to { id, opticaId ->
            bumpWithNullable(id, "pago", { repository.getPagoById(id, opticaId) }, { repository.updatePago(it) })
        },
        "paciente" to { id, _ ->
            bumpWithResource(id, "paciente", { repository.getPacienteById(id) }, { repository.updatePaciente(it) })
        },
        "evaluacion" to { id, _ ->
            bumpWithResource(id, "evaluacion", { repository.getEvaluacionById(id) }, { repository.updateEvaluacion(it) })
        },
        "montura" to { id, opticaId ->
            bumpWithResource(id, "montura", { repository.getMonturaById(id, opticaId) }, { repository.updateMontura(it) })
        },
        "proveedor" to { id, _ ->
            bumpWithNullable(id, "proveedor", { proveedorRepository.getById(id) }, { proveedorRepository.update(it) })
        },
        "orden_compra" to { id, _ ->
            bumpWithNullable(id, "orden_compra", { ordenCompraRepository.getById(id) }, { ordenCompraRepository.update(it) })
        },
        "montura_movimiento" to { id, opticaId ->
            val mov = repository.getMovimientoMonturaById(id)
            if (mov != null) {
                bumpWithResource(
                    mov.monturaId, "parent montura",
                    { repository.getMonturaById(mov.monturaId, opticaId) },
                    { repository.updateMontura(it) }
                )
            } else {
                AppLogger.w(TAG, "bumpEntityUpdatedAt: montura_movimiento no encontrado id=$id")
            }
        },
        "orden_compra_item" to { id, _ ->
            val item = ordenCompraRepository.getOrdenItemById(id)
            if (item != null) {
                val oc = ordenCompraRepository.getById(item.ordenId)
                if (oc != null) {
                    ordenCompraRepository.update(oc)
                } else {
                    AppLogger.w(TAG, "bumpEntityUpdatedAt: parent orden_compra no encontrada id=${item.ordenId} for item=$id")
                }
            } else {
                AppLogger.w(TAG, "bumpEntityUpdatedAt: orden_compra_item no encontrado id=$id")
            }
        },
        "dispensacion_item" to { id, _ ->
            val item = repository.getDispensacionItemById(id)
            if (item != null) {
                bumpWithResource(
                    item.dispensacionId, "parent dispensacion",
                    { repository.getDispensacionById(item.dispensacionId) },
                    { repository.updateDispensacion(it) }
                )
                repository.insertDispensacionItem(item)
            } else {
                AppLogger.w(TAG, "bumpEntityUpdatedAt: dispensacion_item no encontrado id=$id")
            }
        },
        "categoria_montura" to { _, _ -> AppLogger.w(TAG, "categoria_montura has no parent, skipping bump") }
    )
}
