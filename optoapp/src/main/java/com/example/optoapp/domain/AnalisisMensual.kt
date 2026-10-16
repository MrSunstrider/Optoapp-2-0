package com.example.optoapp.domain

import kotlinx.serialization.json.JsonObject

data class AnalisisMensual(
    val ventasMes: Double,
    val cobrosMes: Double,
    val margenNetoPct: Double,
    val margenPorCategoria: List<MargenCategoria>,
    val deudores: DeudoresResumen,
    val proyeccionCaja: ProyeccionCaja?,
    val stockEstancado: List<StockEstancadoItem>,
    val valorInventario: Double,
    val ventasMesAnterior: Double,
    val variacionVentasPct: Double?,
    val gastosMes: Double = 0.0,
    val saldoPendiente: Double = 0.0,
    val ticketPromedio: Double = 0.0,
    val cantidadVentas: Int = 0,
    val esOffline: Boolean = false,
    val costoMes: Double = 0.0,
    val marginFacts: MarginFacts = MarginFacts(),
) {
    companion object {
        fun fromJson(obj: JsonObject): AnalisisMensual {
            val facts = MarginFactsParser.parse(obj)

            return AnalisisMensual(
                ventasMes = obj.optDouble("ventas_mes"),
                cobrosMes = obj.optDouble("cobros_mes"),
                margenNetoPct = obj.optDouble("margen_neto_pct"),
                margenPorCategoria = facts.margenPorCategoria,
                deudores = obj.parseDeudoresResumen(),
                proyeccionCaja = obj.parseProyeccionCaja(),
                stockEstancado = obj.parseStockEstancado(),
                valorInventario = obj.optDouble("valor_inventario"),
                ventasMesAnterior = obj.optDouble("ventas_mes_anterior"),
                variacionVentasPct = obj.optDoubleNullable("variacion_ventas_pct"),
                gastosMes = obj.optDouble("gastos_mes"),
                saldoPendiente = obj.optDouble("saldo_pendiente"),
                ticketPromedio = obj.optDouble("ticket_promedio"),
                cantidadVentas = obj.optInt("cantidad_ventas"),
                costoMes = facts.costoMes,
                marginFacts = facts,
            )
        }

        private fun JsonObject.parseDeudoresResumen(): DeudoresResumen {
            val obj = optObject("deudores") ?: return DeudoresResumen(0, 0.0)
            return DeudoresResumen(
                cantidad = obj.optInt("cantidad"),
                saldoTotal = obj.optDouble("saldo_total"),
            )
        }

        private fun JsonObject.parseProyeccionCaja(): ProyeccionCaja? {
            val obj = optObject("proyeccion_caja")
            val meses = optInt("meses_historicos")

            // The RPC returns meses_historicos at the top level, so the history count alone is
            // enough to show a projection even when the nested object is absent.
            if (obj == null && meses == 0) return null

            return ProyeccionCaja(
                ingresosEsperados = obj?.optDouble("ingresos_esperados") ?: 0.0,
                egresosProgramados = obj?.optDouble("egresos_programados") ?: 0.0,
                saldoNeto = obj?.optDouble("saldo_neto") ?: 0.0,
                mesesHistoricos = meses,
            )
        }

        private fun JsonObject.parseStockEstancado(): List<StockEstancadoItem> =
            optArray("stock_estancado").orEmpty().mapNotNull { item ->
                val obj = item as? JsonObject ?: return@mapNotNull null
                StockEstancadoItem(
                    monturaId = obj.optString("montura_id"),
                    sku = obj.optString("sku"),
                    modelo = obj.optString("modelo"),
                    costo = obj.optDouble("costo"),
                    stockActual = obj.optInt("stock_actual"),
                    ultimaVenta = obj.optStringNullable("ultima_venta"),
                    diasSinVenta = obj.optInt("dias_sin_venta"),
                )
            }
    }

    // Summing category costs undercounts whenever a category has no mapped cost; costo_mes comes
    // from resumen_diario and is the figure every other screen already reports.
    fun costoDeVentas(): Double = costoMes
}

data class DeudoresResumen(
    val cantidad: Int,
    val saldoTotal: Double,
)

data class ProyeccionCaja(
    val ingresosEsperados: Double,
    val egresosProgramados: Double,
    val saldoNeto: Double,
    val mesesHistoricos: Int = 0,
)

data class StockEstancadoItem(
    val monturaId: String,
    val sku: String,
    val modelo: String,
    val costo: Double,
    val stockActual: Int,
    val ultimaVenta: String?,
    val diasSinVenta: Int,
)
