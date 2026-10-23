package com.example.optoapp.domain

/**
 * MoM bridge: volume, ticket, COGS, and category mix (share shift × prior GM × current ventas).
 * Volume + ticket reconstruct Δventas when prior qty > 0; cost effect is −ΔCOGS.
 */
enum class DriverTipo {
    VOLUMEN,
    TICKET,
    COSTO,
    MIX,
}

data class ResultadoDriver(
    val tipo: DriverTipo,
    val soles: Double,
)

data class ResultadoDriversMom(
    val drivers: List<ResultadoDriver>,
    val sinBaseMom: Boolean,
) {
    fun top(n: Int = 4): List<ResultadoDriver> = drivers.take(n)
}

object ResultadoDrivers {
    fun fromAnalisis(analisis: AnalisisMensual): ResultadoDriversMom = compute(
        cantidadVentas = analisis.cantidadVentas,
        ticketPromedio = analisis.ticketPromedio,
        costoMes = analisis.costoMes,
        cantidadVentasMesAnterior = analisis.cantidadVentasMesAnterior,
        ticketPromedioMesAnterior = analisis.ticketPromedioMesAnterior,
        costoMesAnterior = analisis.costoMesAnterior,
        ventasMes = analisis.ventasMes,
        ventasMesAnterior = analisis.ventasMesAnterior,
        categorias = analisis.margenPorCategoria,
        categoriasAnterior = analisis.margenPorCategoriaMesAnterior,
    )

    fun compute(
        cantidadVentas: Int,
        ticketPromedio: Double,
        costoMes: Double,
        cantidadVentasMesAnterior: Int,
        ticketPromedioMesAnterior: Double,
        costoMesAnterior: Double,
        ventasMes: Double = 0.0,
        ventasMesAnterior: Double = 0.0,
        categorias: List<MargenCategoria> = emptyList(),
        categoriasAnterior: List<MargenCategoria> = emptyList(),
    ): ResultadoDriversMom {
        val sinBase = cantidadVentasMesAnterior <= 0
        if (sinBase) {
            return ResultadoDriversMom(drivers = emptyList(), sinBaseMom = true)
        }
        val t0 = ticketPromedioMesAnterior
        val t1 = ticketPromedio
        val volumen = (cantidadVentas - cantidadVentasMesAnterior) * t0
        val ticket = cantidadVentas * (t1 - t0)
        val costo = -(costoMes - costoMesAnterior)
        val mix = mixEffect(ventasMes, ventasMesAnterior, categorias, categoriasAnterior)
        val ranked = buildList {
            add(ResultadoDriver(DriverTipo.VOLUMEN, volumen))
            add(ResultadoDriver(DriverTipo.TICKET, ticket))
            add(ResultadoDriver(DriverTipo.COSTO, costo))
            if (mix != null) add(ResultadoDriver(DriverTipo.MIX, mix))
        }.sortedByDescending { kotlin.math.abs(it.soles) }
        return ResultadoDriversMom(drivers = ranked, sinBaseMom = false)
    }

    /** Σ (share1 − share0) × gm0 × V1 when prior ventas > 0. */
    fun mixEffect(
        ventasMes: Double,
        ventasMesAnterior: Double,
        categorias: List<MargenCategoria>,
        categoriasAnterior: List<MargenCategoria>,
    ): Double? {
        if (ventasMesAnterior <= 0.0 || ventasMes <= 0.0) return null
        if (categoriasAnterior.isEmpty()) return null
        val priorByName = categoriasAnterior.associateBy { it.categoria }
        val currentByName = categorias.associateBy { it.categoria }
        val names = priorByName.keys + currentByName.keys
        var effect = 0.0
        for (name in names) {
            val prior = priorByName[name]
            val curr = currentByName[name]
            val share0 = (prior?.ventas ?: 0.0) / ventasMesAnterior
            val share1 = (curr?.ventas ?: 0.0) / ventasMes
            val gm0 = if (prior != null && prior.ventas > 0.0) {
                (prior.ventas - prior.costos) / prior.ventas
            } else {
                0.0
            }
            effect += (share1 - share0) * gm0 * ventasMes
        }
        return effect
    }
}
