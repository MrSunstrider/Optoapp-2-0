package com.example.optoapp.domain

import com.example.optoapp.data.DispensacionItemDao
import com.example.optoapp.data.OptoRepository
import java.time.LocalDate
import javax.inject.Inject

class ObtenerMovimientosFinancierosUseCase @Inject constructor(
    private val repository: OptoRepository,
    private val dispensacionItemDao: DispensacionItemDao,
) {
    suspend operator fun invoke(
        opticaId: String,
        start: LocalDate,
        end: LocalDate,
    ): List<MovimientoFinanciero> {
        val dispensaciones = repository.getDispensacionesSnapshotForOptica(opticaId)
            .filter { it.fecha >= start && it.fecha <= end }
        val servicios = repository.getServiciosSnapshotForOptica(opticaId)
            .filter { it.fecha >= start && it.fecha <= end }
        val pagos = repository.getPagosSnapshotForOptica(opticaId)

        val pagosSumByDisp = pagos
            .filter { it.dispensacionId != null }
            .groupBy { it.dispensacionId!! }
            .mapValues { (_, pags) -> pags.sumOf { it.monto } }
        val aCuentaSumByServ = pagos
            .filter { it.servicioExtraId != null }
            .groupBy { it.servicioExtraId!! }
            .mapValues { (_, pags) -> pags.sumOf { it.monto } }

        val dispIds = dispensaciones.map { it.id }.toSet()
        val costosByDisp = if (dispIds.isNotEmpty()) {
            dispensacionItemDao.getCostosByDispensacionIds(dispIds, opticaId)
        } else {
            emptyMap()
        }

        val dispMovs = dispensaciones.map { d ->
            MovimientoFinanciero(
                id = d.id,
                fecha = d.fecha,
                tipo = TipoMovimiento.VENTA,
                origen = Origen.DISPENSACION,
                origenId = d.id,
                montoTotal = d.montoTotal,
                montoPagado = pagosSumByDisp[d.id] ?: 0.0,
                costo = costosByDisp[d.id] ?: 0.0,
                pacienteId = d.pacienteId,
                opticaId = d.opticaId,
                descripcion = "OT ${d.ot}",
                vinculadoA = d.ot.takeIf { it.isNotBlank() },
            )
        }

        val servMovs = servicios.map { s ->
            MovimientoFinanciero(
                id = s.id,
                fecha = s.fecha,
                tipo = TipoMovimiento.VENTA,
                origen = Origen.SERVICIO,
                origenId = s.id,
                montoTotal = s.montoTotal,
                montoPagado = aCuentaSumByServ[s.id] ?: 0.0,
                costo = 0.0,
                pacienteId = s.pacienteId ?: "",
                opticaId = s.opticaId,
                descripcion = s.descripcion.takeIf { it.isNotBlank() } ?: "Servicio OT ${s.ot}",
                vinculadoA = s.ot.takeIf { it.isNotBlank() },
            )
        }

        val regalos = repository.getRegalosSnapshotForOptica(opticaId)
            .filter { it.dispensacionId in dispIds }
        val regaloMovs = regalos.map { r ->
            MovimientoFinanciero(
                id = r.id,
                fecha = dispensaciones.firstOrNull { it.id == r.dispensacionId }?.fecha
                    ?: LocalDate.now(),
                tipo = TipoMovimiento.REGALO,
                origen = Origen.REGALO,
                origenId = r.id,
                montoTotal = 0.0,
                montoPagado = 0.0,
                costo = r.costoUnitario * r.cantidad,
                pacienteId = dispensaciones.firstOrNull { it.id == r.dispensacionId }?.pacienteId ?: "",
                opticaId = opticaId,
                descripcion = "Regalo: ${r.descripcion}",
                vinculadoA = r.dispensacionId,
            )
        }

        return dispMovs + servMovs + regaloMovs
    }
}
