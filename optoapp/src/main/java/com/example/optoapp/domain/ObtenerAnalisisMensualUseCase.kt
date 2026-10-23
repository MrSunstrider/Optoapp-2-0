package com.example.optoapp.domain

import com.example.optoapp.data.DispensacionItem
import com.example.optoapp.data.DispensacionItemDao
import com.example.optoapp.data.DispensacionDao
import com.example.optoapp.data.DispensacionOptica
import com.example.optoapp.data.Resource
import com.example.optoapp.data.categoriaproducto.CategoriaProductoDao
import com.example.optoapp.data.configuracionfinanciera.ConfiguracionFinancieraDao
import com.example.optoapp.data.gastooperativo.GastoOperativoDao
import com.example.optoapp.data.gastooperativo.GastoOperativoEntity
import com.example.optoapp.data.regalodispensacion.RegaloDispensacionDao
import com.example.optoapp.data.regaloservicio.RegaloServicioExtraDao
import com.example.optoapp.data.resumendiario.ResumenDiarioDao
import com.example.optoapp.data.resumendiario.ResumenDiarioEntity
import com.example.optoapp.data.servicio.ServicioExtraDao
import com.example.optoapp.data.servicio.ServicioExtraItemDao
import com.example.optoapp.util.AppLogger
import io.github.jan.supabase.postgrest.Postgrest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
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
    private val dispensacionDao: DispensacionDao,
    private val dispensacionItemDao: DispensacionItemDao,
    private val regaloDispensacionDao: RegaloDispensacionDao,
    private val servicioExtraDao: ServicioExtraDao,
    private val servicioExtraItemDao: ServicioExtraItemDao,
    private val regaloServicioExtraDao: RegaloServicioExtraDao,
    private val categoriaProductoDao: CategoriaProductoDao,
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
        // Otherwise linked OT bevel rows double-count COGS already rolled into costoMes.
        val gastos = gastoOperativoDao.getByOpticaBetween(opticaId, inicioSerie.atDay(1), mesActual.atEndOfMonth())
            .filter { it.origenDispensacionItemId == null }
        val rows = resumenes.filter { mesDe(it.fecha) == mesActual }
        val mesAnterior = mesActual.minusMonths(1)
        val rowsAnterior = resumenes.filter { mesDe(it.fecha) == mesAnterior }
        val gastosDelMes = gastos.filter { YearMonth.from(it.fecha) == mesActual }
        val gastosMes = gastosDelMes.sumOf { it.monto }.toDouble()

        val ventasMes = rows.sumOf { it.ventasMontoTotal }
        val costoMes = rows.sumOf { it.ventasCostoTotal }
        val cobrosMes = rows.sumOf { it.cobrosMontoTotal }
        val cantidadVentas = rows.sumOf { it.ventasCantidad }.toInt()
        val ticketPromedio = if (cantidadVentas > 0) ventasMes / cantidadVentas else 0.0
        val ventasMesAnterior = rowsAnterior.sumOf { it.ventasMontoTotal }
        val cantidadVentasMesAnterior = rowsAnterior.sumOf { it.ventasCantidad }.toInt()
        val costoMesAnterior = rowsAnterior.sumOf { it.ventasCostoTotal }
        val ticketPromedioMesAnterior =
            if (cantidadVentasMesAnterior > 0) ventasMesAnterior / cantidadVentasMesAnterior else 0.0
        val variacionVentasPct = if (ventasMesAnterior > 0.0) {
            BigDecimal.valueOf((ventasMes - ventasMesAnterior) / ventasMesAnterior * 100)
                .setScale(1, RoundingMode.HALF_UP)
                .toDouble()
        } else {
            null
        }
        val valorInventario = rows.lastOrNull()?.inventarioValor ?: 0.0
        val saldoPendiente = rows.lastOrNull()?.saldoPendienteTotal ?: 0.0
        val margenBruto = ventasMes - costoMes
        // Keep offline % identical to rpc so the owner never sees two “truths” for one month.
        val margenBrutoPct = porcentajeDeVentas(margenBruto, ventasMes, decimales = 1)
        val margenNetoPct = porcentajeDeVentas(margenBruto - gastosMes, ventasMes, decimales = 1)
        val gastosFacts = gastosFacts(gastosDelMes, ventasMes, costoMes)
        val objetivo = configuracionFinancieraDao.getByOpticaIdOnce(opticaId)?.margenNetoObjetivo
            ?: MarginFacts.DEFAULT_MARGEN_NETO_OBJETIVO
        // Offline MIX needs the same category priors the RPC returns online.
        val margenPorCategoria = margenPorCategoriaOffline(opticaId, mesActual)
        val margenPorCategoriaMesAnterior = margenPorCategoriaOffline(opticaId, mesAnterior)

        return Resource.Success(
            AnalisisMensual(
                ventasMes = ventasMes,
                cobrosMes = cobrosMes,
                margenNetoPct = margenNetoPct,
                margenPorCategoria = margenPorCategoria,
                margenPorCategoriaMesAnterior = margenPorCategoriaMesAnterior,
                deudores = DeudoresResumen(0, 0.0),
                proyeccionCaja = null,
                stockEstancado = emptyList(),
                valorInventario = valorInventario,
                ventasMesAnterior = ventasMesAnterior,
                variacionVentasPct = variacionVentasPct,
                gastosMes = gastosMes,
                saldoPendiente = saldoPendiente,
                ticketPromedio = ticketPromedio,
                cantidadVentas = cantidadVentas,
                cantidadVentasMesAnterior = cantidadVentasMesAnterior,
                costoMesAnterior = costoMesAnterior,
                ticketPromedioMesAnterior = ticketPromedioMesAnterior,
                esOffline = true,
                costoMes = costoMes,
                marginFacts = MarginFacts(
                    ventasMes = ventasMes,
                    costoMes = costoMes,
                    margenBruto = margenBruto,
                    margenBrutoPct = margenBrutoPct,
                    margenPorCategoria = margenPorCategoria,
                    gastosPorCategoria = gastosFacts.porCategoria,
                    gastosPorTipo = GastosPorTipo(fijo = gastosFacts.fijos, variable = gastosFacts.variables),
                    gastosFijos = gastosFacts.fijos,
                    margenContribucion = gastosFacts.margenContribucion,
                    margenContribucionPct = gastosFacts.margenContribucionPct,
                    puntoEquilibrio = gastosFacts.puntoEquilibrio,
                    margenNetoPct = margenNetoPct,
                    margenNetoObjetivo = redondear(objetivo),
                    brechaMargenNetoPp = redondear(margenNetoPct - objetivo),
                    serie6m = serie6m(inicioSerie, resumenes, gastos),
                    fuente = FuenteDatos.OFFLINE,
                ),
            ),
        )
    }

    /** Room-side category margins so offline drivers stay consistent with the RPC. */
    private suspend fun margenPorCategoriaOffline(opticaId: String, mes: YearMonth): List<MargenCategoria> {
        val desde = mes.atDay(1)
        val hasta = mes.atEndOfMonth()
        val ots = dispensacionDao.getDispensacionesListByOptica(opticaId)
            .filter { it.fecha in desde..hasta && it.estadoEntrega != "Anulado" && it.estadoEntrega != "Reclamada" }
        val itemsByOt = dispensacionItemDao.getItemsListByOptica(opticaId).groupBy { it.dispensacionId }
        val regalosByOt = regaloDispensacionDao.getByOpticaId(opticaId).groupBy { it.dispensacionId }
        val agg = mutableMapOf<String, Pair<Double, Double>>() // categoriaId -> ventas, costo

        for (ot in ots) {
            val catId = categoriaIdDeOt(ot)
            val items = itemsByOt[ot.id].orEmpty()
            val itemCosto = items.sumOf { itemCosto(it) }
            val regalo = regalosByOt[ot.id].orEmpty().sumOf { it.cantidad * it.costoUnitario }
            val prev = agg[catId] ?: (0.0 to 0.0)
            agg[catId] = (prev.first + ot.montoTotal) to (prev.second + itemCosto + regalo)
        }

        val servicios = servicioExtraDao.getServiciosListByOptica(opticaId)
            .filter { it.fecha in desde..hasta && it.estado != "Anulado" }
        val itemsByServicio = servicioExtraItemDao.getByOpticaId(opticaId).groupBy { it.servicioExtraId }
        val regalosByServicio = regaloServicioExtraDao.getByOpticaId(opticaId).groupBy { it.servicioExtraId }
        for (sv in servicios) {
            val itemCosto = itemsByServicio[sv.id].orEmpty().sumOf { it.costoReal }
            val regalo = regalosByServicio[sv.id].orEmpty().sumOf { it.cantidad * it.costoUnitario }
            val prev = agg["servicio_extra"] ?: (0.0 to 0.0)
            agg["servicio_extra"] = (prev.first + sv.montoTotal) to (prev.second + sv.costoReal + itemCosto + regalo)
        }

        val catalog = categoriaProductoDao.getAll().first()
        if (catalog.isEmpty()) return emptyList()
        return catalog.map { cat ->
            val (ventas, costos) = agg[cat.id] ?: (0.0 to 0.0)
            val ventasR = redondear(ventas)
            val costosR = redondear(costos)
            MargenCategoria(
                categoria = cat.nombre,
                ventas = ventasR,
                costos = costosR,
                margenPct = if (ventasR > 0.0) {
                    BigDecimal.valueOf((ventasR - costosR) / ventasR * 100)
                        .setScale(2, RoundingMode.HALF_UP)
                        .toDouble()
                } else {
                    null
                },
                margen = redondear(ventasR - costosR),
            )
        }
    }

    private fun categoriaIdDeOt(ot: DispensacionOptica): String = when {
        ot.tipoLente == "Progresivo" -> "lente_progresivo"
        ot.tipoLente == "Bifocal" -> "lente_bifocal"
        ot.tipoLente == "Monofocal" && ot.materialLente == "Resina" -> "lente_monofocal"
        else -> "lente_otro"
    }

    private fun itemCosto(item: DispensacionItem): Double =
        (item.costoRealOd ?: 0.0) + (item.costoRealOi ?: 0.0) +
            (item.costoRealMontura ?: 0.0) + (item.costoRealLc ?: 0.0) +
            (item.costoRealBiselado ?: 0.0)

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
            margenNetoPct = porcentajeDeVentas(margen, ventas, decimales = 1),
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
