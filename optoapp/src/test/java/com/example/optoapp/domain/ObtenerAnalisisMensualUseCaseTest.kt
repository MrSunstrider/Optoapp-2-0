package com.example.optoapp.domain

import android.util.Log
import com.example.optoapp.data.Resource
import com.example.optoapp.data.gastooperativo.GastoOperativoDao
import com.example.optoapp.data.gastooperativo.GastoOperativoEntity
import com.example.optoapp.data.resumendiario.ResumenDiarioDao
import com.example.optoapp.data.resumendiario.ResumenDiarioEntity
import io.github.jan.supabase.postgrest.Postgrest
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException
import java.math.BigDecimal
import java.time.LocalDate

class ObtenerAnalisisMensualUseCaseTest {

    private val gastoDao = mockk<GastoOperativoDao>(relaxed = true)

    @Before
    fun setUpLog() {
        mockkStatic("android.util.Log")
        every { Log.d(any(), any()) } returns 0
        every { Log.w(any(), any<String>()) } returns 0
        every { Log.w(any(), any<String>(), any()) } returns 0
        every { Log.e(any(), any<String>()) } returns 0
        every { Log.e(any(), any<String>(), any()) } returns 0
        coEvery { gastoDao.getByOpticaIdList(any()) } returns emptyList()
    }

    private fun rpcSuccessJson() = buildJsonObject {
        put("ventas_mes", 15000.0)
        put("cobros_mes", 12000.0)
        put("margen_neto_pct", 25.5)
        putJsonArray("margen_por_categoria") {
            add(
                buildJsonObject {
                    put("categoria", "Lentes")
                    put("ventas", 8000.0)
                    put("costos", 4000.0)
                    put("margen_pct", 50.0)
                },
            )
        }
        putJsonObject("deudores") {
            put("cantidad", 5)
            put("saldo_total", 3000.0)
        }
        putJsonObject("proyeccion_caja") {
            put("ingresos_esperados", 5000.0)
            put("egresos_programados", 2000.0)
            put("saldo_neto", 3000.0)
        }
        putJsonArray("stock_estancado") {}
        put("valor_inventario", 45000.0)
        put("ventas_mes_anterior", 12000.0)
        put("variacion_ventas_pct", 25.0)
        put("costo_mes", 5000.0)
    }

    @Test
    fun online_rpcSuccess_returnsAnalisisMensual() = runBlocking {
        val useCase = object : ObtenerAnalisisMensualUseCase(mockk<Postgrest>(), mockk(), gastoDao) {
            override suspend fun callRpc(function: String, params: JsonObject): JsonObject = rpcSuccessJson()
        }

        val result = useCase("optica1", LocalDate.of(2026, 7, 1))

        assertTrue(result is Resource.Success)
        val analisis = (result as Resource.Success).data!!
        assertEquals(15000.0, analisis.ventasMes, 0.001)
        assertEquals(12000.0, analisis.cobrosMes, 0.001)
        assertEquals(25.5, analisis.margenNetoPct, 0.001)
        assertEquals(5, analisis.deudores.cantidad)
        assertEquals(5000.0, analisis.proyeccionCaja!!.ingresosEsperados, 0.001)
        assertEquals(5000.0, analisis.costoMes, 0.001)
        assertEquals(false, analisis.esOffline)
    }

    @Test
    fun offline_ioException_fallsBackToRoom() = runBlocking {
        val dao = mockk<ResumenDiarioDao>()
        coEvery { dao.getByOpticaAndMonth("optica1", "2026-07") } returns listOf(
            ResumenDiarioEntity(
                id = "r1", opticaId = "optica1", fecha = "2026-07-01",
                ventasCantidad = 2, ventasMontoTotal = 5000.0, ventasCostoTotal = 2000.0,
                cobrosCantidad = 1, cobrosMontoTotal = 4000.0,
                saldoPendienteTotal = 1000.0, saldoPendienteCantidad = 1,
                inventarioValor = 30000.0,
            ),
            ResumenDiarioEntity(
                id = "r2", opticaId = "optica1", fecha = "2026-07-02",
                ventasCantidad = 3, ventasMontoTotal = 7000.0, ventasCostoTotal = 3000.0,
                cobrosCantidad = 2, cobrosMontoTotal = 5000.0,
                saldoPendienteTotal = 2000.0, saldoPendienteCantidad = 1,
                inventarioValor = 35000.0,
            ),
        )

        val useCase = object : ObtenerAnalisisMensualUseCase(mockk<Postgrest>(), dao, gastoDao) {
            override suspend fun callRpc(function: String, params: JsonObject): JsonObject =
                throw IOException("No network")
        }

        val result = useCase("optica1", LocalDate.of(2026, 7, 1))

        assertTrue(result is Resource.Success)
        val analisis = (result as Resource.Success).data!!
        assertEquals(true, analisis.esOffline)
        assertEquals(12000.0, analisis.ventasMes, 0.001)
        assertEquals(9000.0, analisis.cobrosMes, 0.001)
        assertEquals(5000.0, analisis.costoMes, 0.001)
        assertEquals(0.0, analisis.gastosMes, 0.001)
        assertEquals(0.0, analisis.margenNetoPct, 0.001)
        assertEquals(0, analisis.deudores.cantidad)
        assertNull(analisis.proyeccionCaja)
        assertEquals(0, analisis.margenPorCategoria.size)
        assertEquals(0, analisis.stockEstancado.size)
        assertEquals(35000.0, analisis.valorInventario, 0.001)
        assertEquals(0.0, analisis.ventasMesAnterior, 0.001)
        assertNull(analisis.variacionVentasPct)
    }

    @Test
    fun offline_composes_cogs_and_gastos_when_present() = runBlocking {
        val dao = mockk<ResumenDiarioDao>()
        coEvery { dao.getByOpticaAndMonth("optica1", "2026-07") } returns listOf(
            ResumenDiarioEntity(
                id = "r1", opticaId = "optica1", fecha = "2026-07-01",
                ventasCantidad = 1, ventasMontoTotal = 1000.0, ventasCostoTotal = 300.0,
                cobrosCantidad = 1, cobrosMontoTotal = 1000.0,
                saldoPendienteTotal = 0.0, saldoPendienteCantidad = 0,
                inventarioValor = 0.0,
            ),
        )
        coEvery { gastoDao.getByOpticaIdList("optica1") } returns listOf(
            GastoOperativoEntity(
                id = "g1",
                opticaId = "optica1",
                categoria = "Alquiler",
                monto = BigDecimal("100.00"),
                fecha = LocalDate.of(2026, 7, 5),
            ),
            GastoOperativoEntity(
                id = "g2",
                opticaId = "optica1",
                categoria = "Alquiler",
                monto = BigDecimal("50.00"),
                fecha = LocalDate.of(2026, 6, 5),
            ),
        )

        val useCase = object : ObtenerAnalisisMensualUseCase(mockk<Postgrest>(), dao, gastoDao) {
            override suspend fun callRpc(function: String, params: JsonObject): JsonObject =
                throw IOException("offline")
        }

        val analisis = (useCase("optica1", LocalDate.of(2026, 7, 1)) as Resource.Success).data!!
        assertTrue(analisis.esOffline)
        assertEquals(300.0, analisis.costoMes, 0.001)
        assertEquals(100.0, analisis.gastosMes, 0.001)
    }

    @Test
    fun unexpectedError_returnsResourceError() = runBlocking {
        val useCase = object : ObtenerAnalisisMensualUseCase(mockk<Postgrest>(), mockk(), gastoDao) {
            override suspend fun callRpc(function: String, params: JsonObject): JsonObject =
                throw RuntimeException("Unexpected")
        }

        val result = useCase("optica1", LocalDate.of(2026, 7, 1))
        assertTrue(result is Resource.Error)
        assertEquals("No se pudieron cargar los datos del mes", (result as Resource.Error).message)
    }
}
