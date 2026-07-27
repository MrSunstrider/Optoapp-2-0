package com.example.optoapp.domain

import com.example.optoapp.data.DispensacionOptica
import com.example.optoapp.data.OptoRepository
import com.example.optoapp.data.SyncStateTracker
import com.example.optoapp.util.AppLogger
import javax.inject.Inject

class DispensacionMergeHandler @Inject constructor(
    private val repository: OptoRepository,
    private val syncStateTracker: SyncStateTracker,
) {
    companion object {
        private const val TAG = "SyncFinanzas"
    }

    suspend fun mergeLocalDispensacionConflict(
        opticaId: String,
        canonical: DispensacionOptica,
        duplicate: DispensacionOptica,
    ) {
        val merged = canonical.copy(
            ot = canonical.ot.ifBlank { duplicate.ot },
            monturaId = canonical.monturaId.ifBlank { duplicate.monturaId },
            pacienteId = canonical.pacienteId.ifBlank { duplicate.pacienteId },
            fecha = if (canonical.fecha >= duplicate.fecha) canonical.fecha else duplicate.fecha,
            tipoMontura = canonical.tipoMontura.ifBlank { duplicate.tipoMontura },
            materialMontura = canonical.materialMontura.ifBlank { duplicate.materialMontura },
            tipoLente = canonical.tipoLente.ifBlank { duplicate.tipoLente },
            materialLente = canonical.materialLente.ifBlank { duplicate.materialLente },
            tratamientos = (canonical.tratamientos + duplicate.tratamientos).distinct(),
            colorLente = canonical.colorLente.ifBlank { duplicate.colorLente },
            notasDiseno = canonical.notasDiseno.ifBlank { duplicate.notasDiseno },
            origenMontura = canonical.origenMontura.ifBlank { duplicate.origenMontura },
            tipoAro = canonical.tipoAro.ifBlank { duplicate.tipoAro },
            descripcionMontura = canonical.descripcionMontura.ifBlank { duplicate.descripcionMontura },
            montoTotal = maxOf(canonical.montoTotal, duplicate.montoTotal),
            metodoPago = canonical.metodoPago.ifBlank { duplicate.metodoPago },
            montoPagado = maxOf(canonical.montoPagado, duplicate.montoPagado),
            estadoEntrega = canonical.estadoEntrega.ifBlank { duplicate.estadoEntrega },
            fechaVencimientoGarantia = canonical.fechaVencimientoGarantia ?: duplicate.fechaVencimientoGarantia,
            distanciaLente = canonical.distanciaLente.ifBlank { duplicate.distanciaLente },
            altura = canonical.altura.ifBlank { duplicate.altura },
            subTipoBifocal = canonical.subTipoBifocal.ifBlank { duplicate.subTipoBifocal },
        )
        var movedPagos = 0
        var movedItems = 0
        var movedRegalos = 0
        repository.withTransaction {
            repository.updateDispensacion(merged)
            movedPagos = repository.reassignPagosDispensacion(duplicate.id, canonical.id, opticaId)
            movedItems = repository.reassignItemsDispensacion(duplicate.id, canonical.id, opticaId)
            movedRegalos = repository.reassignRegalosDispensacion(duplicate.id, canonical.id, opticaId)
            repository.deleteDispensacionById(duplicate.id, opticaId)
            syncStateTracker.markSynced(opticaId, "dispensacion", duplicate.id)
        }
        syncStateTracker.markError(
            opticaId,
            "dispensacion",
            canonical.id,
            "Conflicto de reconciliación resuelto: fusionada ${duplicate.id} en ${canonical.id}; " +
                "ot=${merged.ot.ifBlank { "(sin OT)" }}, paciente_id=${merged.pacienteId}, " +
                "pagos=$movedPagos, items=$movedItems, regalos=$movedRegalos.",
        )
        AppLogger.w(TAG, "Dispensacion fusionada por conflicto remoto ${duplicate.id} -> ${canonical.id} (pagos=$movedPagos, items=$movedItems, regalos=$movedRegalos)")
    }

    suspend fun resolveLocalDuplicateDispensaciones(opticaId: String) {
        val local = repository.getDispensacionesSnapshotForOptica(opticaId)
        if (local.isEmpty()) return
        val groups = local
            .mapNotNull { d ->
                val key = normalizedOtForUnique(d.ot) ?: return@mapNotNull null
                key to d
            }
            .groupBy({ it.first }, { it.second })
            .filterValues { it.size > 1 }
        if (groups.isEmpty()) return

        groups.forEach { (otKey, rows) ->
            val canonical = rows.maxByOrNull { it.fecha } ?: return@forEach
            rows.forEach { duplicate ->
                if (duplicate.id == canonical.id) return@forEach
                var movedPagos = 0
                var movedItems = 0
                var movedRegalos = 0
                repository.withTransaction {
                    movedPagos = repository.reassignPagosDispensacion(duplicate.id, canonical.id, opticaId)
                    movedItems = repository.reassignItemsDispensacion(duplicate.id, canonical.id, opticaId)
                    movedRegalos = repository.reassignRegalosDispensacion(duplicate.id, canonical.id, opticaId)
                    repository.deleteDispensacionById(duplicate.id, opticaId)
                }
                syncStateTracker.markError(
                    opticaId,
                    "dispensacion",
                    duplicate.id,
                    "OT duplicada local ($otKey) resuelta automáticamente. Fusionada en ${canonical.id}; " +
                        "pagos=$movedPagos, items=$movedItems, regalos=$movedRegalos.",
                )
                AppLogger.w(TAG, "Dispensacion duplicada OT=$otKey fusionada ${duplicate.id} -> ${canonical.id} (pagos=$movedPagos, items=$movedItems, regalos=$movedRegalos)")
            }
        }
    }
}
