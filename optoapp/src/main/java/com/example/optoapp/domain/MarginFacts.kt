package com.example.optoapp.domain

import java.time.YearMonth

enum class FuenteDatos { SERVIDOR, OFFLINE }

data class MarginFacts(
    val ventasMes: Double = 0.0,
    val costoMes: Double = 0.0,
    val margenBruto: Double = 0.0,
    val margenBrutoPct: Double = 0.0,
    val margenPorCategoria: List<MargenCategoria> = emptyList(),
    val costosPorLinea: CostosPorLinea = CostosPorLinea(),
    val margenServicios: MargenServicios = MargenServicios(),
    val cobertura: CoberturaCosto = CoberturaCosto(),
    val gastosPorCategoria: List<GastoCategoria> = emptyList(),
    val gastosPorTipo: GastosPorTipo = GastosPorTipo(),
    val gastosFijos: Double = 0.0,
    val margenContribucion: Double = 0.0,
    val margenContribucionPct: Double = 0.0,
    val puntoEquilibrio: Double? = null,
    val margenNetoPct: Double = 0.0,
    val margenNetoObjetivo: Double = DEFAULT_MARGEN_NETO_OBJETIVO,
    val brechaMargenNetoPp: Double = 0.0,
    val serie6m: List<PuntoSerieMensual> = emptyList(),
    val fuente: FuenteDatos = FuenteDatos.SERVIDOR,
) {
    // Offline facts leave the server-only detail at its defaults; the UI must not render those as zeros.
    val detalleServidorDisponible: Boolean get() = fuente == FuenteDatos.SERVIDOR

    companion object {
        const val DEFAULT_MARGEN_NETO_OBJETIVO = 15.0
    }
}

data class MargenCategoria(
    val categoria: String,
    val ventas: Double,
    val costos: Double,
    val margenPct: Double?,
    val margen: Double = ventas - costos,
)

data class CostosPorLinea(
    val lentes: Double = 0.0,
    val montura: Double = 0.0,
    val lc: Double = 0.0,
    val biselado: Double = 0.0,
    val serviciosPropios: Double = 0.0,
    val serviciosTercerizados: Double = 0.0,
    val serviciosSinClasificar: Double = 0.0,
    val regalos: Double = 0.0,
) {
    fun total(): Double = lentes + montura + lc + biselado +
        serviciosPropios + serviciosTercerizados + serviciosSinClasificar + regalos
}

data class MargenLinea(
    val ventas: Double = 0.0,
    val costos: Double = 0.0,
    val margen: Double = 0.0,
    val margenPct: Double? = null,
    val cantidad: Int = 0,
)

data class MargenServicios(
    val propios: MargenLinea = MargenLinea(),
    val tercerizados: MargenLinea = MargenLinea(),
    val sinClasificar: MargenLinea = MargenLinea(),
)

data class CoberturaCosto(
    val lineasTotal: Int = 0,
    val lineasReales: Int = 0,
    val lineasEstimadas: Int = 0,
    val lineasFaltantes: Int = 0,
    val pctReal: Double = 0.0,
    val pctEstimado: Double = 0.0,
    val pctFaltante: Double = 0.0,
) {
    val sinDatos: Boolean get() = lineasTotal == 0
}

data class GastoCategoria(
    val categoria: String,
    val monto: Double,
    val tipoPredominante: String,
)

data class GastosPorTipo(
    val fijo: Double = 0.0,
    val variable: Double = 0.0,
)

data class PuntoSerieMensual(
    val mes: YearMonth,
    val ventas: Double,
    val costo: Double,
    val gastos: Double,
    val margen: Double,
    val margenNetoPct: Double,
)
