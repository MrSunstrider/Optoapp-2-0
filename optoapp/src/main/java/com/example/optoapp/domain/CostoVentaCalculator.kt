package com.example.optoapp.domain

import com.example.optoapp.data.DispensacionItem
import com.example.optoapp.data.ServicioExtra
import com.example.optoapp.data.regalodispensacion.RegaloDispensacionEntity
import com.example.optoapp.data.regaloservicio.RegaloServicioExtraEntity
import com.example.optoapp.data.servicio.ServicioExtraItem

data class CostoVenta(val costo: Double, val costoIncompleto: Boolean)

/**
 * Client mirror of the cost that recalcular_resumen_diario books per sale, so movement rows and
 * report totals agree with resumen_diario.ventas_costo_total.
 */
object CostoVentaCalculator {

    fun dispensacion(
        items: List<DispensacionItem>,
        costoUnitarioSnapshot: Double?,
        regalos: List<RegaloDispensacionEntity>,
    ): CostoVenta {
        // Legacy OTs saved before item capture only carry the venta snapshot.
        val costoBase = if (items.isEmpty()) costoUnitarioSnapshot ?: 0.0 else items.sumOf { it.costoTotal() }
        val incompleto = if (items.isEmpty()) {
            CoberturaCostoClassifier.claseItem(null, costoBase) == ClaseCobertura.FALTANTE
        } else {
            items.any { CoberturaCostoClassifier.claseItem(it.costoOrigen, it.costoTotal()) == ClaseCobertura.FALTANTE }
        }
        return CostoVenta(costoBase + regalos.sumOf { it.cantidad * it.costoUnitario }, incompleto)
    }

    fun servicio(
        servicio: ServicioExtra,
        items: List<ServicioExtraItem>,
        regalos: List<RegaloServicioExtraEntity>,
    ): CostoVenta = CostoVenta(
        costo = servicio.costoReal + items.sumOf { it.costoReal } + regalos.sumOf { it.cantidad * it.costoUnitario },
        costoIncompleto = CoberturaCostoClassifier.claseServicio(servicio.realizadoPor, servicio.costoReal, servicio.servicioTipoId) ==
            ClaseCobertura.FALTANTE,
    )

    private fun DispensacionItem.costoTotal(): Double =
        (costoRealOd ?: 0.0) + (costoRealOi ?: 0.0) + (costoRealMontura ?: 0.0) + (costoRealBiselado ?: 0.0) + (costoRealLc ?: 0.0)
}
