package com.example.optoapp.domain

import com.example.optoapp.data.Resource
import com.example.optoapp.data.configuracionfinanciera.ConfiguracionFinancieraDao
import com.example.optoapp.data.gastooperativo.GastoOperativoDao
import com.example.optoapp.data.gastooperativo.GastoOperativoEntity
import com.example.optoapp.data.resumendiario.ResumenDiarioDao
import com.example.optoapp.data.resumendiario.ResumenDiarioEntity
import com.example.optoapp.util.AppLogger
import io.github.jan.supabase.postgrest.Postgrest
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.io.IOException
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

open class ObtenerAnalisisMensualUseCase @Inject constructor(
    private val postgrest: Postgrest,
    private val resumenDiarioDao: ResumenDiarioDao,
    private val gastoOperativoDao: GastoOperativoDao,
    private val configuracionFinancieraDao: ConfiguracionFinancieraDao,
) {
    companion object {
        private const val TAG = "ObtenerAnalisisMensual"
        private const val MESES_SERIE = 6
    }

    suspend operator fun invoke(opticaId: String, mes: LocalDate): Resource<AnalisisMensual> = try {
        val params = buildJsonObject {
            put("p_optica_id", opticaId)
            put("p_mes", mes.toString())
        }
        val json = callRpc("rpc_analisis_mensual", params)
        Resource.Success(AnalisisMensual.fromJson(json))
    } catch (e: CancellationException) {
        throw e
    } catch (e: IOException) {
        AppLogger.w(TAG, "Offline — falling back to Room", e)
        fallbackToRoom(opticaId, mes)
    } catch (e: Exception) {
        AppLogger.e(TAG, "Error obteniendo analisis mensual", e)
        Resource.Error("No se pudieron cargar los datos del mes")
    }

    internal open suspend fun callRpc(function: String, params: JsonObject): JsonObject = postgrest.rpc(function, params).decodeAs<JsonObject>()

    private suspend fun fallbackToRoom(opticaId: String, mes: LocalDate): Resource<AnalisisMensual> {
        val mesActual = YearMonth.from(mes)
        val inicioSerie = mesActual.minusMonths(MESES_SERIE - 1L)
        val resumenes = resumenDiarioDao.getByOpticaBetween(
            opticaId,
            inicioSerie.atDay(1).toString(),
            mesActual.atEndOfMonth().toString(),
        )
        // Linked OT bevel gastos already sit in costoMes; exclude them from neto gastos.
        val gastos = gastoOperativoDao.getByOpticaBetween(opticaId, inicioSerie.atDay(1), mesActual.atEndOfMonth())
            .filter { it.origenDispensacionItemId == null }
        val rows = resumenes.filter { mesDe(it.fecha) == mesActual }
        val gastosDelMes = gastos.filter { YearMonth.from(it.fecha) == mesActual }
        val gastosMes = gastosDelMes.sumOf { it.monto }.toDouble()

        val ventasMes = rows.sumOf { it.ventasMontoTotal }
        val costoMes = rows.sumOf { it.ventasCostoTotal }
        val cobrosMes = rows.sumOf { it.cobrosMontoTotal }
        val valorInventario = rows.lastOrNull()?.inventarioValor ?: 0.0
        val margenBruto = ventasMes - costoMes
        // Same rounding as rpc_analisis_mensual so a month reads identically online and offline.
        val margenBrutoPct = porcentajeDeVentas(margenBruto, ventasMes, decimales = 2)
        val margenNetoPct = porcentajeDeVentas(margenBruto - gastosMes, ventasMes, decimales = 1)
        val gastosFacts = gastosFacts(gastosDelMes, ventasMes, costoMes)
        val objetivo = configuracionFinancieraDao.getByOpticaIdOnce(opticaId)?.margenNetoObjetivo
            ?: MarginFacts.DEFAULT_MARGEN_NETO_OBJETIVO

        return Resource.Success(
            AnalisisMensual(
                ventasMes = ventasMes,
                cobrosMes = cobrosMes,
                margenNetoPct = margenNetoPct,
                margenPorCategoria = emptyList(),
                deudores = DeudoresResumen(0, 0.0),
                proyeccionCaja = null,
                stockEstancado = emptyList(),
                valorInventario = valorInventario,
                ventasMesAnterior = 0.0,
                variacionVentasPct = null,
                gastosMes = gastosMes,
                esOffline = true,
                costoMes = costoMes,
                marginFacts = MarginFacts(
                    ventasMes = ventasMes,
                    costoMes = costoMes,
                    margenBruto = margenBruto,
                    margenBrutoPct = margenBrutoPct,
                    margenNetoPct = margenNetoPct,
                    gastosPorCategoria = gastosFacts.porCategoria,
                    gastosPorTipo = GastosPorTipo(fijo = gastosFacts.fijos, variable = gastosFacts.variables),
                    gastosFijos = gastosFacts.fijos,
                    margenContribucion = gastosFacts.margenContribucion,
                    margenContribucionPct = gastosFacts.margenContribucionPct,
                    puntoEquilibrio = gastosFacts.puntoEquilibrio,
                    margenNetoObjetivo = redondear(objetivo),
                    brechaMargenNetoPp = redondear(margenNetoPct - objetivo),
                    serie6m = serie6m(inicioSerie, resumenes, gastos),
                    fuente = FuenteDatos.OFFLINE,
                ),
            ),
        )
    }

    private class GastosFacts(
        val porCategoria: List<GastoCategoria>,
        val fijos: Double,
        val variables: Double,
        val margenContribucion: Double,
        val margenContribucionPct: Double,
        val puntoEquilibrio: Double?,
    )

    private fun gastosFacts(gastos: List<GastoOperativoEntity>, ventasMes: Double, costoMes: Double): GastosFacts {
        fun List<GastoOperativoEntity>.total(tipo: String) =
            filter { GastoTipoResolver.resolver(it.categoria, it.tipoGasto) == tipo }.sumOf { it.monto }.toDouble()

        val porCategoria = gastos.groupBy { it.categoria }
            .map { (categoria, filas) ->
                val tipoPredominante = if (filas.total(GastoTipoResolver.FIJO) >= filas.total(GastoTipoResolver.VARIABLE)) {
                    GastoTipoResolver.FIJO
                } else {
                    GastoTipoResolver.VARIABLE
                }
                GastoCategoria(categoria, filas.sumOf { it.monto }.toDouble(), tipoPredominante)
            }
            .filter { it.monto > 0.0 }
            .sortedWith(compareByDescending<GastoCategoria> { it.monto }.thenBy { it.categoria })
            .map { it.copy(monto = redondear(it.monto)) }
        val fijos = gastos.total(GastoTipoResolver.FIJO)
        val variables = gastos.total(GastoTipoResolver.VARIABLE)
        val margenContribucion = ventasMes - costoMes - variables
        val margenContribucionPct = if (ventasMes > 0.0) margenContribucion / ventasMes * 100 else 0.0
        // Break-even uses the unrounded percentage, exactly like the server.
        val puntoEquilibrio = if (margenContribucionPct > 0.0) redondear(fijos / (margenContribucionPct / 100)) else null
        return GastosFacts(
            porCategoria = porCategoria,
            fijos = redondear(fijos),
            variables = redondear(variables),
            margenContribucion = redondear(margenContribucion),
            margenContribucionPct = redondear(margenContribucionPct),
            puntoEquilibrio = puntoEquilibrio,
        )
    }

    private fun serie6m(
        inicioSerie: YearMonth,
        resumenes: List<ResumenDiarioEntity>,
        gastos: List<GastoOperativoEntity>,
    ): List<PuntoSerieMensual> = (0 until MESES_SERIE).map { offset ->
        val mes = inicioSerie.plusMonths(offset.toLong())
        val delMes = resumenes.filter { mesDe(it.fecha) == mes }
        val ventas = delMes.sumOf { it.ventasMontoTotal }
        val costo = delMes.sumOf { it.ventasCostoTotal }
        val gastosMes = gastos.filter { YearMonth.from(it.fecha) == mes }.sumOf { it.monto }.toDouble()
        val margen = ventas - costo - gastosMes
        PuntoSerieMensual(
            mes = mes,
            ventas = redondear(ventas),
            costo = redondear(costo),
            gastos = redondear(gastosMes),
            margen = redondear(margen),
            margenNetoPct = porcentajeDeVentas(margen, ventas, decimales = 2),
        )
    }

    private fun mesDe(fecha: String): YearMonth = YearMonth.parse(fecha.take(7))

    private fun redondear(valor: Double): Double = BigDecimal.valueOf(valor).setScale(2, RoundingMode.HALF_UP).toDouble()

    private fun porcentajeDeVentas(monto: Double, ventas: Double, decimales: Int): Double =
        if (ventas > 0.0) {
            BigDecimal.valueOf(monto / ventas * 100).setScale(decimales, RoundingMode.HALF_UP).toDouble()
        } else {
            0.0
        }
}
