package com.example.optoapp.domain

import android.util.Log
import com.example.optoapp.data.DispensacionDao
import com.example.optoapp.data.DispensacionItemDao
import com.example.optoapp.data.Resource
import com.example.optoapp.data.categoriaproducto.CategoriaProductoDao
import com.example.optoapp.data.configuracionfinanciera.ConfiguracionFinancieraDao
import com.example.optoapp.data.configuracionfinanciera.ConfiguracionFinancieraEntity
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
import io.mockk.unmockkStatic
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.io.IOException
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

class GastosFactsOfflineTest {

    private val julio = LocalDate.of(2026, 7, 1)

    @Before
    fun setUp() {
        mockkStatic(Log::class)
        every { Log.w(any(), any<String>(), any()) } returns 0
    }

    @After
    fun tearDown() {
        unmockkStatic(Log::class)
    }

    private fun resumen(fecha: String, ventas: Double, costo: Double) = ResumenDiarioEntity(
        id = "r-$fecha", opticaId = "optica1", fecha = fecha,
        ventasMontoTotal = ventas, ventasCostoTotal = costo,
    )

    private fun gasto(id: String, categoria: String, monto: String, fecha: LocalDate = julio.plusDays(4), tipo: String? = null) =
        GastoOperativoEntity(
            id = id, opticaId = "optica1", categoria = categoria,
            monto = BigDecimal(monto), fecha = fecha, tipoGasto = tipo,
        )

    private suspend fun offlineFacts(
        resumenes: List<ResumenDiarioEntity>,
        gastos: List<GastoOperativoEntity> = emptyList(),
        config: ConfiguracionFinancieraEntity? = null,
    ): MarginFacts {
        val resumenDao = mockk<ResumenDiarioDao>()
        coEvery { resumenDao.getByOpticaBetween("optica1", "2026-02-01", "2026-07-31") } returns resumenes
        val gastoDao = mockk<GastoOperativoDao>()
        coEvery { gastoDao.getByOpticaBetween("optica1", LocalDate.of(2026, 2, 1), LocalDate.of(2026, 7, 31)) } returns gastos
        val configDao = mockk<ConfiguracionFinancieraDao>()
        coEvery { configDao.getByOpticaIdOnce("optica1") } returns config
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
        val useCase = object : ObtenerAnalisisMensualUseCase(
            mockk<Postgrest>(),
            resumenDao,
            gastoDao,
            configDao,
            dispensacionDao,
            itemDao,
            regaloDao,
            servicioDao,
            servicioItemDao,
            regaloServicioDao,
            categoriaDao,
        ) {
            override suspend fun callRpc(function: String, params: JsonObject): JsonObject = throw IOException("No network")
        }
        return (useCase("optica1", julio) as Resource.Success).data!!.marginFacts
    }

    @Test
    fun `gastos without explicit type are split by category`() = runTest {
        val facts = offlineFacts(
            resumenes = listOf(resumen("2026-07-10", ventas = 10000.0, costo = 4000.0)),
            gastos = listOf(gasto("g1", "alquiler", "2000.00"), gasto("g2", "marketing", "500.00")),
        )

        assertEquals(GastosPorTipo(fijo = 2000.0, variable = 500.0), facts.gastosPorTipo)
        assertEquals(2000.0, facts.gastosFijos, 0.0)
    }

    @Test
    fun `gastos by category are ordered by amount with ties resolved as fijo`() = runTest {
        val facts = offlineFacts(
            resumenes = listOf(resumen("2026-07-10", ventas = 10000.0, costo = 4000.0)),
            gastos = listOf(
                gasto("g1", "otro", "100.00", tipo = "fijo"),
                gasto("g2", "otro", "100.00", tipo = "variable"),
                gasto("g3", "personal", "300.00"),
                gasto("g4", "marketing", "50.00"),
            ),
        )

        assertEquals(
            listOf(
                GastoCategoria("personal", 300.0, "fijo"),
                GastoCategoria("otro", 200.0, "fijo"),
                GastoCategoria("marketing", 50.0, "variable"),
            ),
            facts.gastosPorCategoria,
        )
    }

    @Test
    fun `gastos outside the selected month stay out of the month facts`() = runTest {
        val facts = offlineFacts(
            resumenes = listOf(resumen("2026-07-10", ventas = 1000.0, costo = 400.0)),
            gastos = listOf(gasto("g1", "alquiler", "100.00"), gasto("g2", "alquiler", "900.00", fecha = LocalDate.of(2026, 6, 15))),
        )

        assertEquals(100.0, facts.gastosFijos, 0.0)
        assertEquals(50.0, facts.margenNetoPct, 0.0)
    }

    @Test
    fun `contribution margin and break even match the server formula`() = runTest {
        val facts = offlineFacts(
            resumenes = listOf(resumen("2026-07-10", ventas = 10000.0, costo = 4000.0)),
            gastos = listOf(gasto("g1", "alquiler", "2000.00"), gasto("g2", "marketing", "500.00")),
        )

        assertEquals(5500.0, facts.margenContribucion, 0.0)
        assertEquals(55.0, facts.margenContribucionPct, 0.0)
        assertEquals(3636.36, facts.puntoEquilibrio!!, 0.0)
    }

    @Test
    fun `break even is unavailable without sales`() = runTest {
        val facts = offlineFacts(resumenes = emptyList(), gastos = listOf(gasto("g1", "alquiler", "2000.00")))

        assertEquals(0.0, facts.margenContribucionPct, 0.0)
        assertNull(facts.puntoEquilibrio)
    }

    @Test
    fun `break even is unavailable when the contribution margin is not positive`() = runTest {
        val facts = offlineFacts(
            resumenes = listOf(resumen("2026-07-10", ventas = 1000.0, costo = 1200.0)),
            gastos = listOf(gasto("g1", "alquiler", "300.00")),
        )

        assertNull(facts.puntoEquilibrio)
    }

    @Test
    fun `serie 6m has six months oldest first with gaps at zero and negative margins kept`() = runTest {
        val facts = offlineFacts(
            resumenes = listOf(
                resumen("2026-02-03", ventas = 1000.0, costo = 400.0),
                resumen("2026-04-10", ventas = 100.0, costo = 150.0),
                resumen("2026-07-01", ventas = 600.0, costo = 200.0),
                resumen("2026-07-20", ventas = 400.0, costo = 100.0),
            ),
            gastos = listOf(
                gasto("g1", "alquiler", "200.00", fecha = LocalDate.of(2026, 3, 5)),
                gasto("g2", "alquiler", "100.00"),
            ),
        )

        val serie = facts.serie6m
        assertEquals((2..7).map { YearMonth.of(2026, it) }, serie.map { it.mes })
        assertEquals(PuntoSerieMensual(YearMonth.of(2026, 2), 1000.0, 400.0, 0.0, 600.0, 60.0), serie[0])
        assertEquals(PuntoSerieMensual(YearMonth.of(2026, 3), 0.0, 0.0, 200.0, -200.0, 0.0), serie[1])
        assertEquals(PuntoSerieMensual(YearMonth.of(2026, 4), 100.0, 150.0, 0.0, -50.0, -50.0), serie[2])
        assertEquals(PuntoSerieMensual(YearMonth.of(2026, 5), 0.0, 0.0, 0.0, 0.0, 0.0), serie[3])
        assertEquals(PuntoSerieMensual(YearMonth.of(2026, 7), 1000.0, 300.0, 100.0, 600.0, 60.0), serie[5])
    }

    @Test
    fun `month totals come only from the selected month of the six month range`() = runTest {
        val facts = offlineFacts(
            resumenes = listOf(resumen("2026-02-03", ventas = 5000.0, costo = 1000.0), resumen("2026-07-01", ventas = 1000.0, costo = 400.0)),
        )

        assertEquals(1000.0, facts.ventasMes, 0.0)
        assertEquals(400.0, facts.costoMes, 0.0)
    }

    @Test
    fun `target comes from the local financial configuration`() = runTest {
        val facts = offlineFacts(
            resumenes = listOf(resumen("2026-07-10", ventas = 1000.0, costo = 600.0)),
            gastos = listOf(gasto("g1", "alquiler", "150.00")),
            config = ConfiguracionFinancieraEntity(opticaId = "optica1", margenNetoObjetivo = 20.0),
        )

        assertEquals(25.0, facts.margenNetoPct, 0.0)
        assertEquals(20.0, facts.margenNetoObjetivo, 0.0)
        assertEquals(5.0, facts.brechaMargenNetoPp, 0.0)
    }

    @Test
    fun `target defaults to 15 without local configuration`() = runTest {
        val facts = offlineFacts(resumenes = listOf(resumen("2026-07-10", ventas = 1000.0, costo = 900.0)))

        assertEquals(15.0, facts.margenNetoObjetivo, 0.0)
        assertEquals(-5.0, facts.brechaMargenNetoPp, 0.0)
    }
}
