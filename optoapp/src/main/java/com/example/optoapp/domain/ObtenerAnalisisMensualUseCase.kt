package com.example.optoapp.domain

import com.example.optoapp.data.Resource
import com.example.optoapp.data.gastooperativo.GastoOperativoDao
import com.example.optoapp.data.resumendiario.ResumenDiarioDao
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
import javax.inject.Inject

open class ObtenerAnalisisMensualUseCase @Inject constructor(
    private val postgrest: Postgrest,
    private val resumenDiarioDao: ResumenDiarioDao,
    private val gastoOperativoDao: GastoOperativoDao,
) {
    companion object {
        private const val TAG = "ObtenerAnalisisMensual"
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
        val yearMonth = String.format("%04d-%02d", mes.year, mes.monthValue)
        val rows = resumenDiarioDao.getByOpticaAndMonth(opticaId, yearMonth)
        val inicioMes = mes.withDayOfMonth(1)
        val gastosMes = gastoOperativoDao.getByOpticaIdAndDateRange(opticaId, inicioMes, mes.withDayOfMonth(mes.lengthOfMonth()))
            .first()
            .sumOf { it.monto }
            .toDouble()

        val ventasMes = rows.sumOf { it.ventasMontoTotal }
        val costoMes = rows.sumOf { it.ventasCostoTotal }
        val cobrosMes = rows.sumOf { it.cobrosMontoTotal }
        val valorInventario = rows.lastOrNull()?.inventarioValor ?: 0.0
        val margenBruto = ventasMes - costoMes
        // Same rounding as rpc_analisis_mensual so a month reads identically online and offline.
        val margenBrutoPct = porcentajeDeVentas(margenBruto, ventasMes, decimales = 2)
        val margenNetoPct = porcentajeDeVentas(margenBruto - gastosMes, ventasMes, decimales = 1)

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
                    fuente = FuenteDatos.OFFLINE,
                ),
            ),
        )
    }

    private fun porcentajeDeVentas(monto: Double, ventas: Double, decimales: Int): Double =
        if (ventas > 0.0) {
            BigDecimal.valueOf(monto / ventas * 100).setScale(decimales, RoundingMode.HALF_UP).toDouble()
        } else {
            0.0
        }
}
