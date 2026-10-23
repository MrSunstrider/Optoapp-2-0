package com.example.optoapp.domain

import android.util.Log
import com.example.optoapp.data.DispensacionDao
import com.example.optoapp.data.DispensacionItem
import com.example.optoapp.data.DispensacionItemDao
import com.example.optoapp.data.DispensacionOptica
import com.example.optoapp.data.Resource
import com.example.optoapp.data.categoriaproducto.CategoriaProductoDao
import com.example.optoapp.data.categoriaproducto.CategoriaProductoEntity
import com.example.optoapp.data.configuracionfinanciera.ConfiguracionFinancieraDao
import com.example.optoapp.data.gastooperativo.GastoOperativoDao
import com.example.optoapp.data.gastooperativo.GastoOperativoEntity
import com.example.optoapp.data.regalodispensacion.RegaloDispensacionDao
import com.example.optoapp.data.regaloservicio.RegaloServicioExtraDao
import com.example.optoapp.data.resumendiario.ResumenDiarioDao
import com.example.optoapp.data.resumendiario.ResumenDiarioEntity
import com.example.optoapp.data.servicio.ServicioExtraDao
import com.example.optoapp.data.servicio.ServicioExtraItemDao
import io.github.jan.supabase.postgrest.Postgrest
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException
import java.math.BigDecimal
import java.time.LocalDate

class ObtenerAnalisisMensualUseCaseTest {

    private val julio = LocalDate.of(2026, 7, 1)

    @Before
    fun setUpLog() {
        mockkStatic("android.util.Log")
        every { Log.d(any(), any()) } returns 0
        every { Log.w(any(), any<String>()) } returns 0
        every { Log.w(any(), any<String>(), any()) } returns 0
        every { Log.e(any(), any<String>()) } returns 0
        every { Log.e(any(), any<String>(), any()) } returns 0
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
        put("cantidad_ventas", 10)
        put("ticket_promedio", 1500.0)
        put("cantidad_ventas_mes_anterior", 8)
        put("costo_mes_anterior", 5000.0)
        put("ticket_promedio_mes_anterior", 1500.0)
        put("variacion_ventas_pct", 25.0)
    }

    private fun resumen(
        id: String,
        fecha: String,
        ventas: Double,
        costo: Double,
        cobros: Double = 0.0,
        inventario: Double = 0.0,
        cantidad: Int = 1,
    ) =
        ResumenDiarioEntity(
            id = id, opticaId = "optica1", fecha = fecha,
            ventasCantidad = cantidad, ventasMontoTotal = ventas, ventasCostoTotal = costo,
            cobrosCantidad = 1, cobrosMontoTotal = cobros,
            saldoPendienteTotal = 0.0, saldoPendienteCantidad = 0,
            inventarioValor = inventario,
        )

    private fun gasto(id: String, monto: String) = GastoOperativoEntity(
        id = id,
        opticaId = "optica1",
        categoria = "alquiler",
        monto = BigDecimal(monto),
        fecha = julio.plusDays(4),
    )

    private fun emptyOfflineDeps(): Array<Any> {
        val dispensacionDao = mockk<DispensacionDao>()
        coEvery { dispensacionDao.getDispensacionesListByOptica(any()) } returns emptyList()
        val itemDao = mockk<DispensacionItemDao>()
        coEvery { itemDao.getItemsListByOptica(any()) } returns emptyList()
        val regaloDao = mockk<RegaloDispensacionDao>()
        coEvery { regaloDao.getByOpticaId(any()) } returns emptyList()
        val servicioDao = mockk<ServicioExtraDao>()
        coEvery { servicioDao.getServiciosListByOptica(any()) } returns emptyList()
        val servicioItemDao = mockk<ServicioExtraItemDao>()
        coEvery { servicioItemDao.getByOpticaId(any()) } returns emptyList()
        val regaloServicioDao = mockk<RegaloServicioExtraDao>()
        coEvery { regaloServicioDao.getByOpticaId(any()) } returns emptyList()
        val categoriaDao = mockk<CategoriaProductoDao>()
        every { categoriaDao.getAll() } returns flowOf(emptyList())
        return arrayOf(
            dispensacionDao, itemDao, regaloDao, servicioDao, servicioItemDao, regaloServicioDao, categoriaDao,
        )
    }

    private fun buildUseCase(
        postgrest: Postgrest = mockk(),
        resumenDao: ResumenDiarioDao = mockk(),
        gastoDao: GastoOperativoDao = mockk(),
        configDao: ConfiguracionFinancieraDao = mockk(),
        offlineDeps: Array<Any> = emptyOfflineDeps(),
        rpc: suspend (String, JsonObject) -> JsonObject,
    ): ObtenerAnalisisMensualUseCase {
        @Suppress("UNCHECKED_CAST")
        return object : ObtenerAnalisisMensualUseCase(
            postgrest,
            resumenDao,
            gastoDao,
            configDao,
            offlineDeps[0] as DispensacionDao,
            offlineDeps[1] as DispensacionItemDao,
            offlineDeps[2] as RegaloDispensacionDao,
            offlineDeps[3] as ServicioExtraDao,
            offlineDeps[4] as ServicioExtraItemDao,
            offlineDeps[5] as RegaloServicioExtraDao,
            offlineDeps[6] as CategoriaProductoDao,
        ) {
            override suspend fun callRpc(function: String, params: JsonObject): JsonObject = rpc(function, params)
        }
    }

    private fun offlineUseCase(
        resumenes: List<ResumenDiarioEntity>,
        gastos: List<GastoOperativoEntity> = emptyList(),
        offlineDeps: Array<Any> = emptyOfflineDeps(),
    ): ObtenerAnalisisMensualUseCase {
        val resumenDao = mockk<ResumenDiarioDao>()
        coEvery { resumenDao.getByOpticaBetween("optica1", "2026-02-01", "2026-07-31") } returns resumenes
        val gastoDao = mockk<GastoOperativoDao>()
        coEvery { gastoDao.getByOpticaBetween("optica1", LocalDate.of(2026, 2, 1), LocalDate.of(2026, 7, 31)) } returns gastos
        val configDao = mockk<ConfiguracionFinancieraDao>()
        coEvery { configDao.getByOpticaIdOnce("optica1") } returns null
        return buildUseCase(
            resumenDao = resumenDao,
            gastoDao = gastoDao,
            configDao = configDao,
            offlineDeps = offlineDeps,
        ) { _, _ -> throw IOException("No network") }
    }

    @Test
    fun online_rpcSuccess_returnsAnalisisMensual() = runTest {
        val useCase = buildUseCase { _, _ -> rpcSuccessJson() }

        val result = useCase("optica1", julio)

        assertTrue(result is Resource.Success)
        val analisis = (result as Resource.Success).data!!
        assertEquals(15000.0, analisis.ventasMes, 0.001)
        assertEquals(12000.0, analisis.cobrosMes, 0.001)
        assertEquals(25.5, analisis.margenNetoPct, 0.001)
        assertEquals(5, analisis.deudores.cantidad)
        assertEquals(5000.0, analisis.proyeccionCaja!!.ingresosEsperados, 0.001)
        assertEquals(false, analisis.esOffline)
        assertEquals(10, analisis.cantidadVentas)
        assertEquals(8, analisis.cantidadVentasMesAnterior)
        assertEquals(5000.0, analisis.costoMesAnterior, 0.001)
        assertEquals(1500.0, analisis.ticketPromedioMesAnterior, 0.001)
    }

    @Test
    fun offline_ioException_fallsBackToRoom() = runTest {
        val useCase = offlineUseCase(
            listOf(
                resumen("r1", "2026-07-01", ventas = 5000.0, costo = 2000.0, cobros = 4000.0, inventario = 30000.0),
                resumen("r2", "2026-07-02", ventas = 7000.0, costo = 3000.0, cobros = 5000.0, inventario = 35000.0),
            ),
        )

        val result = useCase("optica1", julio)

        assertTrue(result is Resource.Success)
        val analisis = (result as Resource.Success).data!!
        assertEquals(true, analisis.esOffline)
        assertEquals(12000.0, analisis.ventasMes, 0.001)
        assertEquals(9000.0, analisis.cobrosMes, 0.001)
        assertEquals(0, analisis.deudores.cantidad)
        assertNull(analisis.proyeccionCaja)
        assertEquals(0, analisis.margenPorCategoria.size)
        assertEquals(0, analisis.stockEstancado.size)
        assertEquals(35000.0, analisis.valorInventario, 0.001)
        assertEquals(0.0, analisis.ventasMesAnterior, 0.001)
        assertNull(analisis.variacionVentasPct)
        assertEquals(2, analisis.cantidadVentas)
        assertEquals(0, analisis.cantidadVentasMesAnterior)
    }

    @Test
    fun offline_computesMomPriorsFromPriorMonth() = runTest {
        val useCase = offlineUseCase(
            listOf(
                resumen("jun", "2026-06-15", ventas = 8000.0, costo = 3000.0, cantidad = 4),
                resumen("jul", "2026-07-10", ventas = 10000.0, costo = 4000.0, cantidad = 5),
            ),
        )

        val analisis = (useCase("optica1", julio) as Resource.Success).data!!

        assertEquals(8000.0, analisis.ventasMesAnterior, 0.001)
        assertEquals(4, analisis.cantidadVentasMesAnterior)
        assertEquals(3000.0, analisis.costoMesAnterior, 0.001)
        assertEquals(2000.0, analisis.ticketPromedioMesAnterior, 0.001)
        assertEquals(5, analisis.cantidadVentas)
        assertEquals(2000.0, analisis.ticketPromedio, 0.001)
        assertEquals(25.0, analisis.variacionVentasPct!!, 0.001)
    }

    @Test
    fun offline_momPriors_matchUnroundedDivision() = runTest {
        // Values that ROUND(..., 2) would alter; offline Room must keep exact V0/Q0 and COGS sum.
        val useCase = offlineUseCase(
            listOf(
                resumen("jun", "2026-06-15", ventas = 10_000.0, costo = 3_000.123, cantidad = 3),
                resumen("jul", "2026-07-10", ventas = 12_000.0, costo = 4_000.0, cantidad = 4),
            ),
        )

        val analisis = (useCase("optica1", julio) as Resource.Success).data!!

        assertEquals(10_000.0 / 3.0, analisis.ticketPromedioMesAnterior, 0.000_000_1)
        assertEquals(3_000.123, analisis.costoMesAnterior, 0.000_000_1)
        assertFalse(kotlin.math.abs(analisis.ticketPromedioMesAnterior - 3_333.33) < 0.000_000_1)
    }

    @Test
    fun offline_computesMarginFactsFromResumenAndGastos() = runTest {
        val useCase = offlineUseCase(
            resumenes = listOf(
                resumen("r1", "2026-07-01", ventas = 6000.0, costo = 2500.0),
                resumen("r2", "2026-07-02", ventas = 4000.0, costo = 1500.0),
            ),
            gastos = listOf(gasto("g1", "3000.00"), gasto("g2", "1000.00")),
        )

        val analisis = (useCase("optica1", julio) as Resource.Success).data!!

        assertEquals(20.0, analisis.margenNetoPct, 0.001)
        assertEquals(4000.0, analisis.costoMes, 0.001)
        assertEquals(4000.0, analisis.costoDeVentas(), 0.001)
        assertEquals(4000.0, analisis.gastosMes, 0.001)
        val facts = analisis.marginFacts
        assertEquals(FuenteDatos.OFFLINE, facts.fuente)
        assertFalse(facts.detalleServidorDisponible)
        assertEquals(10000.0, facts.ventasMes, 0.001)
        assertEquals(4000.0, facts.costoMes, 0.001)
        assertEquals(6000.0, facts.margenBruto, 0.001)
        assertEquals(60.0, facts.margenBrutoPct, 0.001)
        assertEquals(20.0, facts.margenNetoPct, 0.001)
    }

    @Test
    fun offline_excludesLinkedOtBiseladoGastosFromNeto() = runTest {
        val linked = gasto("gb1", "2000.00").copy(origenDispensacionItemId = "item-1")
        val useCase = offlineUseCase(
            resumenes = listOf(resumen("r1", "2026-07-01", ventas = 10000.0, costo = 4000.0)),
            gastos = listOf(gasto("g1", "1000.00"), linked),
        )

        val analisis = (useCase("optica1", julio) as Resource.Success).data!!

        assertEquals(1000.0, analisis.gastosMes, 0.001)
        assertEquals(50.0, analisis.margenNetoPct, 0.001)
    }

    @Test
    fun offline_withoutSales_returnsZeroPercentagesWithoutDividing() = runTest {
        val useCase = offlineUseCase(resumenes = emptyList(), gastos = listOf(gasto("g1", "500.00")))

        val analisis = (useCase("optica1", julio) as Resource.Success).data!!

        assertEquals(0.0, analisis.margenNetoPct, 0.001)
        assertEquals(0.0, analisis.marginFacts.margenNetoPct, 0.001)
        assertEquals(0.0, analisis.marginFacts.margenBrutoPct, 0.001)
        assertEquals(500.0, analisis.gastosMes, 0.001)
    }

    @Test
    fun offline_keepsServerOnlyFactsAtDefaults() = runTest {
        val useCase = offlineUseCase(listOf(resumen("r1", "2026-07-01", ventas = 1000.0, costo = 400.0)))

        val facts = (useCase("optica1", julio) as Resource.Success).data!!.marginFacts
        val defaults = MarginFacts()

        assertEquals(defaults.margenPorCategoria, facts.margenPorCategoria)
        assertEquals(defaults.costosPorLinea, facts.costosPorLinea)
        assertEquals(defaults.margenServicios, facts.margenServicios)
        assertEquals(defaults.cobertura, facts.cobertura)
        assertFalse(facts.detalleServidorDisponible)
    }

    @Test
    fun unexpectedError_returnsResourceError() = runTest {
        val useCase = buildUseCase { _, _ -> throw RuntimeException("Unexpected") }

        val result = useCase("optica1", julio)
        assertTrue(result is Resource.Error)
        assertEquals("No se pudieron cargar los datos del mes", (result as Resource.Error).message)
    }

    @Test
    fun offline_computesMargenPorCategoriaFromLocalDispensaciones() = runTest {
        val dispensacionDao = mockk<DispensacionDao>()
        coEvery { dispensacionDao.getDispensacionesListByOptica("optica1") } returns listOf(
            DispensacionOptica(
                id = "d1",
                pacienteId = "p1",
                fecha = LocalDate.of(2026, 7, 10),
                opticaId = "optica1",
                tipoLente = "Progresivo",
                materialLente = "Resina",
                montoTotal = 1000.0,
                estadoEntrega = "Entregado",
            ),
            DispensacionOptica(
                id = "d0",
                pacienteId = "p1",
                fecha = LocalDate.of(2026, 6, 10),
                opticaId = "optica1",
                tipoLente = "Progresivo",
                materialLente = "Resina",
                montoTotal = 800.0,
                estadoEntrega = "Entregado",
            ),
        )
        val itemDao = mockk<DispensacionItemDao>()
        coEvery { itemDao.getItemsListByOptica("optica1") } returns listOf(
            DispensacionItem(
                id = "i1",
                dispensacionId = "d1",
                opticaId = "optica1",
                costoRealOd = 200.0,
                costoRealOi = 200.0,
            ),
            DispensacionItem(
                id = "i0",
                dispensacionId = "d0",
                opticaId = "optica1",
                costoRealOd = 150.0,
                costoRealOi = 150.0,
            ),
        )
        val regaloDao = mockk<RegaloDispensacionDao>()
        coEvery { regaloDao.getByOpticaId("optica1") } returns emptyList()
        val servicioDao = mockk<ServicioExtraDao>()
        coEvery { servicioDao.getServiciosListByOptica("optica1") } returns emptyList()
        val servicioItemDao = mockk<ServicioExtraItemDao>()
        coEvery { servicioItemDao.getByOpticaId("optica1") } returns emptyList()
        val regaloServicioDao = mockk<RegaloServicioExtraDao>()
        coEvery { regaloServicioDao.getByOpticaId("optica1") } returns emptyList()
        val categoriaDao = mockk<CategoriaProductoDao>()
        every { categoriaDao.getAll() } returns flowOf(
            listOf(
                CategoriaProductoEntity("lente_progresivo", "Lentes Progresivos", "lente", 1),
                CategoriaProductoEntity("servicio_extra", "Servicios Extra", "servicio", 7),
            ),
        )
        val deps = arrayOf(
            dispensacionDao, itemDao, regaloDao, servicioDao, servicioItemDao, regaloServicioDao, categoriaDao,
        )
        val useCase = offlineUseCase(
            resumenes = listOf(
                resumen("jun", "2026-06-15", ventas = 800.0, costo = 300.0, cantidad = 1),
                resumen("jul", "2026-07-10", ventas = 1000.0, costo = 400.0, cantidad = 1),
            ),
            offlineDeps = deps,
        )

        val analisis = (useCase("optica1", julio) as Resource.Success).data!!
        val actual = analisis.margenPorCategoria.first { it.categoria == "Lentes Progresivos" }
        val prior = analisis.margenPorCategoriaMesAnterior.first { it.categoria == "Lentes Progresivos" }
        assertEquals(1000.0, actual.ventas, 0.001)
        assertEquals(400.0, actual.costos, 0.001)
        assertEquals(800.0, prior.ventas, 0.001)
        assertEquals(300.0, prior.costos, 0.001)
        val mix = ResultadoDrivers.fromAnalisis(analisis).drivers.find { it.tipo == DriverTipo.MIX }
        assertTrue(mix != null)
    }
}
