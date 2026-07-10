package com.example.optoapp.domain

import com.example.optoapp.data.OptoRepository
import java.time.LocalDate
import javax.inject.Inject

/**
 * Builds a [MovimientoFinanciero] list from dispensaciones + servicios_extra + pagos.
 * 3-query approach: loads all dispensaciones, servicios, and pagos for an optica,
 * then computes montoPagado/aCuenta dynamically from pagos.
 */
class ObtenerMovimientosFinancierosUseCase @Inject constructor(
    private val repository: OptoRepository
) {
    suspend operator fun invoke(
        opticaId: String,
        start: LocalDate,
        end: LocalDate
    ): List<MovimientoFinanciero> {
        val dispensaciones = repository.getDispensacionesSnapshotForOptica(opticaId)
            .filter { it.fecha >= start && it.fecha <= end }
        val servicios = repository.getServiciosSnapshotForOptica(opticaId)
            .filter { it.fecha >= start && it.fecha <= end }
        val pagos = repository.getPagosSnapshotForOptica(opticaId)

        val pagosSumByDisp = pagos
            .filter { it.tipo != "Anulación" && it.dispensacionId != null }
            .groupBy { it.dispensacionId!! }
            .mapValues { (_, pags) -> pags.sumOf { it.monto } }
        val aCuentaSumByServ = pagos
            .filter { it.tipo != "Anulación" && it.servicioExtraId != null }
            .groupBy { it.servicioExtraId!! }
            .mapValues { (_, pags) -> pags.sumOf { it.monto } }

        val dispMovs = dispensaciones.map { d ->
            MovimientoFinanciero(
                id = d.id,
                fecha = d.fecha,
                tipo = TipoMovimiento.VENTA,
                origen = Origen.DISPENSACION,
                origenId = d.id,
                montoTotal = d.montoTotal,
                montoPagado = pagosSumByDisp[d.id] ?: 0.0,
                costo = 0.0,
                pacienteId = d.pacienteId,
                opticaId = d.opticaId,
                descripcion = "OT ${d.ot}",
                vinculadoA = d.ot.takeIf { it.isNotBlank() }
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
                vinculadoA = s.ot.takeIf { it.isNotBlank() }
            )
        }

        return dispMovs + servMovs
    }
}
