package com.example.optoapp.domain

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.contentOrNull

private fun JsonObject.optDouble(key: String): Double =
    this[key]?.jsonPrimitive?.doubleOrNull ?: 0.0

private fun JsonObject.optInt(key: String): Int =
    this[key]?.jsonPrimitive?.intOrNull ?: 0

private fun JsonObject.optDoubleNullable(key: String): Double? =
    this[key]?.jsonPrimitive?.doubleOrNull

private fun JsonObject.optString(key: String): String =
    this[key]?.jsonPrimitive?.contentOrNull ?: ""

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
    val esOffline: Boolean = false
) {
    companion object {
        fun fromJson(json: JsonElement): AnalisisMensual {
            val obj = json.jsonObject

            return AnalisisMensual(
                ventasMes = obj.optDouble("ventas_mes"),
                cobrosMes = obj.optDouble("cobros_mes"),
                margenNetoPct = obj.optDouble("margen_neto_pct"),
                margenPorCategoria = obj.parseMargenPorCategoria(),
                deudores = obj.parseDeudoresResumen(),
                proyeccionCaja = obj.parseProyeccionCaja(),
                stockEstancado = obj.parseStockEstancado(),
                valorInventario = obj.optDouble("valor_inventario"),
                ventasMesAnterior = obj.optDouble("ventas_mes_anterior"),
                variacionVentasPct = obj.optDoubleNullable("variacion_ventas_pct"),
                gastosMes = obj.optDouble("gastos_mes"),
                saldoPendiente = obj.optDouble("saldo_pendiente"),
                ticketPromedio = obj.optDouble("ticket_promedio"),
                cantidadVentas = obj.optInt("cantidad_ventas")
            )
        }

        private fun JsonObject.parseMargenPorCategoria(): List<MargenCategoria> {
            val arr = this["margen_por_categoria"]?.jsonArray ?: return emptyList()
            return arr.map { item ->
                val obj = item.jsonObject
                MargenCategoria(
                    categoria = obj.optString("categoria"),
                    ventas = obj.optDouble("ventas"),
                    costos = obj.optDouble("costos"),
                    margenPct = obj.optDoubleNullable("margen_pct")
                )
            }
        }

        private fun JsonObject.parseDeudoresResumen(): DeudoresResumen {
            val obj = this["deudores"]?.jsonObject ?: return DeudoresResumen(0, 0.0)
            return DeudoresResumen(
                cantidad = obj.optInt("cantidad"),
                saldoTotal = obj.optDouble("saldo_total")
            )
        }

        private fun JsonObject.parseProyeccionCaja(): ProyeccionCaja? {
            val obj = this["proyeccion_caja"]?.jsonObject ?: return null
            return ProyeccionCaja(
                ingresosEsperados = obj.optDouble("ingresos_esperados"),
                egresosProgramados = obj.optDouble("egresos_programados"),
                saldoNeto = obj.optDouble("saldo_neto")
            )
        }

        private fun JsonObject.parseStockEstancado(): List<StockEstancadoItem> {
            val arr = this["stock_estancado"]?.jsonArray ?: return emptyList()
            return arr.map { item ->
                val obj = item.jsonObject
                StockEstancadoItem(
                    monturaId = obj.optString("montura_id"),
                    sku = obj.optString("sku"),
                    modelo = obj.optString("modelo"),
                    costo = obj.optDouble("costo"),
                    stockActual = obj.optInt("stock_actual"),
                    ultimaVenta = obj["ultima_venta"]?.jsonPrimitive?.contentOrNull,
                    diasSinVenta = obj.optInt("dias_sin_venta")
                )
            }
        }
    }

    fun costoDeVentas(): Double = margenPorCategoria.sumOf { it.costos }
}

data class MargenCategoria(
    val categoria: String,
    val ventas: Double,
    val costos: Double,
    val margenPct: Double?
)

data class DeudoresResumen(
    val cantidad: Int,
    val saldoTotal: Double
)

data class ProyeccionCaja(
    val ingresosEsperados: Double,
    val egresosProgramados: Double,
    val saldoNeto: Double
)

data class StockEstancadoItem(
    val monturaId: String,
    val sku: String,
    val modelo: String,
    val costo: Double,
    val stockActual: Int,
    val ultimaVenta: String?,
    val diasSinVenta: Int
)
