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
            val ventas = facts.ventasMes
            val cogs = facts.costoMes
            val bruto = ventas - cogs
            val opex = analisis.gastosMes
            val resultado = bruto - opex
            val roPct = facts.margenNetoPct

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
