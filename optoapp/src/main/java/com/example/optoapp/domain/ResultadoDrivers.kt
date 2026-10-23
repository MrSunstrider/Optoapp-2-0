package com.example.optoapp.domain

/**
 * MoM bridge explaining why operating result moved: volume, ticket, and COGS.
 * Volume + ticket reconstruct Δventas when prior qty > 0; cost effect is −ΔCOGS.
 */
enum class DriverTipo {
    VOLUMEN,
    TICKET,
    COSTO,
}

data class ResultadoDriver(
    val tipo: DriverTipo,
    val soles: Double,
)

data class ResultadoDriversMom(
    val drivers: List<ResultadoDriver>,
    val sinBaseMom: Boolean,
) {
    fun top(n: Int = 3): List<ResultadoDriver> = drivers.take(n)
}

object ResultadoDrivers {
    fun fromAnalisis(analisis: AnalisisMensual): ResultadoDriversMom = compute(
        cantidadVentas = analisis.cantidadVentas,
        ticketPromedio = analisis.ticketPromedio,
        costoMes = analisis.costoMes,
        cantidadVentasMesAnterior = analisis.cantidadVentasMesAnterior,
        ticketPromedioMesAnterior = analisis.ticketPromedioMesAnterior,
        costoMesAnterior = analisis.costoMesAnterior,
    )

    fun compute(
        cantidadVentas: Int,
        ticketPromedio: Double,
        costoMes: Double,
        cantidadVentasMesAnterior: Int,
        ticketPromedioMesAnterior: Double,
        costoMesAnterior: Double,
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
        val ranked = listOf(
            ResultadoDriver(DriverTipo.VOLUMEN, volumen),
            ResultadoDriver(DriverTipo.TICKET, ticket),
            ResultadoDriver(DriverTipo.COSTO, costo),
        ).sortedByDescending { kotlin.math.abs(it.soles) }
        return ResultadoDriversMom(drivers = ranked, sinBaseMom = false)
    }
}
