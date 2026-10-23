package com.example.optoapp.domain

data class WaterfallStep(
    val key: String,
    val monto: Double,
)

data class AnalisisKpis(
    val utilidadBruta: Double,
    val resultadoOperativoPct: Double,
    val ticket: Double,
    val cxc: Double,
    val puntoEquilibrio: Double?,
    val resultadoIncompleto: Boolean,
)

data class AnalisisCajaResumen(
    val cobrado: Double,
    val saldoPendiente: Double,
    val proyeccionSaldoNeto: Double?,
)

data class AnalisisDecisionLayout(
    val waterfall: List<WaterfallStep>,
    val kpis: AnalisisKpis,
    val caja: AnalisisCajaResumen,
) {
    companion object {
        fun from(analisis: AnalisisMensual): AnalisisDecisionLayout {
            val facts = analisis.marginFacts
            val ventas = if (facts.ventasMes != 0.0) facts.ventasMes else analisis.ventasMes
            val cogs = if (facts.costoMes != 0.0) facts.costoMes else analisis.costoMes
            val bruto = if (facts.margenBruto != 0.0) facts.margenBruto else (ventas - cogs)
            val opex = analisis.gastosMes
            val resultado = ventas - cogs - opex
            val roPct = if (facts.margenNetoPct != 0.0) facts.margenNetoPct else analisis.margenNetoPct

            return AnalisisDecisionLayout(
                waterfall = listOf(
                    WaterfallStep("ventas", ventas),
                    WaterfallStep("cogs", cogs),
                    WaterfallStep("bruto", bruto),
                    WaterfallStep("opex", opex),
                    WaterfallStep("resultado", resultado),
                ),
                kpis = AnalisisKpis(
                    utilidadBruta = bruto,
                    resultadoOperativoPct = roPct,
                    ticket = analisis.ticketPromedio,
                    cxc = analisis.saldoPendiente,
                    puntoEquilibrio = facts.puntoEquilibrio,
                    resultadoIncompleto = ResultadoOperativoConfiabilidad.esIncompleto(facts.cobertura),
                ),
                caja = AnalisisCajaResumen(
                    cobrado = analisis.cobrosMes,
                    saldoPendiente = analisis.saldoPendiente,
                    proyeccionSaldoNeto = analisis.proyeccionCaja?.saldoNeto,
                ),
            )
        }
    }
}
