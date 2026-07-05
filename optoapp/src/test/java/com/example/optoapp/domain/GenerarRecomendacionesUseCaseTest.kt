package com.example.optoapp.domain

import com.example.optoapp.data.Resource
import com.example.optoapp.data.configuracionfinanciera.ConfiguracionFinancieraDao
import com.example.optoapp.data.configuracionfinanciera.ConfiguracionFinancieraEntity
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
class GenerarRecomendacionesUseCaseTest {

    private val analisisUseCase: ObtenerAnalisisMensualUseCase = mockk(relaxed = true)
    private val deudoresUseCase: ObtenerDeudoresUseCase = mockk(relaxed = true)
    private val configDao: ConfiguracionFinancieraDao = mockk(relaxed = true)

    private lateinit var useCase: GenerarRecomendacionesUseCase

    private val testMes = LocalDate.of(2026, 7, 1)

    private val defaultConfig = ConfiguracionFinancieraEntity(
        opticaId = "optica1",
        deudaTotalAlertaMonto = 3000.0,
        deudaViejaAlertaDias = 30,
        stockEstancadoAlertaDias = 180,
        caidaVentasAlertaPct = 10.0,
        minVentasParaRecomendar = 5
    )

    @Before
    fun setUp() {
        useCase = GenerarRecomendacionesUseCase(analisisUseCase, deudoresUseCase, configDao)
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    private fun deudor(
        nombre: String = "Cliente Test",
        saldo: Double = 1000.0,
        dias: Int = 10,
        telefono: String = "999888777"
    ) = Deudor(
        pacienteNombre = nombre,
        pacienteTelefono = telefono,
        ventaId = "v-test",
        ventaFecha = LocalDate.of(2026, 6, 1),
        montoTotal = saldo + 500.0,
        totalPagado = 500.0,
        saldo = saldo,
        diasDeuda = dias
    )

    private fun categoria(
        nombre: String = "Test Cat",
        ventas: Double = 100.0,
        costos: Double = 50.0,
        margenPct: Double? = null
    ) = MargenCategoria(categoria = nombre, ventas = ventas, costos = costos, margenPct = margenPct)

    private fun stockItem(
        modelo: String = "Modelo Test",
        dias: Int = 200,
        costo: Double = 100.0
    ) = StockEstancadoItem(
        monturaId = "m-test", sku = "SKU-TEST", modelo = modelo,
        costo = costo, stockActual = 2, ultimaVenta = null, diasSinVenta = dias
    )

    private fun cleanAnalisis(
        ventasMes: Double = 10000.0,
        gastosMes: Double = 0.0,
        variacionVentasPct: Double? = 0.0,
        categorias: List<MargenCategoria> = emptyList(),
        stockEstancado: List<StockEstancadoItem> = emptyList(),
        deudoresResumen: DeudoresResumen = DeudoresResumen(0, 0.0)
    ) = AnalisisMensual(
        ventasMes = ventasMes, cobrosMes = 0.0, margenNetoPct = 0.0,
        margenPorCategoria = categorias, deudores = deudoresResumen,
        proyeccionCaja = null, stockEstancado = stockEstancado,
        valorInventario = 0.0, ventasMesAnterior = 0.0,
        variacionVentasPct = variacionVentasPct, gastosMes = gastosMes,
        esOffline = false
    )

    private fun mockDeps(
        analisis: AnalisisMensual = cleanAnalisis(),
        deudores: List<Deudor> = emptyList(),
        config: ConfiguracionFinancieraEntity = defaultConfig
    ) {
        coEvery { analisisUseCase.invoke("optica1", testMes) } returns Resource.Success(analisis)
        coEvery { deudoresUseCase.invoke("optica1") } returns Resource.Success(deudores)
        coEvery { configDao.getByOpticaIdOnce("optica1") } returns config
    }

    private fun listaDe(result: Resource<List<Recomendacion>>): List<Recomendacion> =
        (result as Resource.Success).data!!

    // ── R1: COBRAR ─────────────────────────────────────────────────────────

    @Test
    fun cobrar_whenDeudaTotalExceedsThreshold_returnsRecomendacion() = runBlocking {
        val deudores = listOf(
            deudor("Juan", saldo = 2000.0, dias = 15),
            deudor("Maria", saldo = 2200.0, dias = 10)
        )
        mockDeps(deudores = deudores)

        val lista = listaDe(useCase.invoke("optica1", testMes))
        val cobrar = lista.find { it.tipo == RecomendacionTipo.COBRAR }
        assertNotNull("COBRAR recommendation should exist", cobrar)
        assertEquals(Prioridad.ALTA, cobrar!!.prioridad)
        assertTrue(cobrar.detalle.contains("S/"))
        assertTrue(cobrar.detalle.contains("4,200") || cobrar.detalle.contains("4200"))
    }

    @Test
    fun cobrar_whenOldDebtorExists_returnsRecomendacion() = runBlocking {
        val deudores = listOf(
            deudor("Juan", saldo = 500.0, dias = 45),
            deudor("Maria", saldo = 1000.0, dias = 10)
        )
        mockDeps(deudores = deudores)

        val lista = listaDe(useCase.invoke("optica1", testMes))
        val cobrar = lista.find { it.tipo == RecomendacionTipo.COBRAR }
        assertNotNull("COBRAR should fire on old debtor", cobrar)
        assertEquals(Prioridad.ALTA, cobrar!!.prioridad)
    }

    @Test
    fun cobrar_whenNoDebtors_returnsNull() = runBlocking {
        mockDeps(deudores = emptyList())
        val lista = listaDe(useCase.invoke("optica1", testMes))
        assertNull("COBRAR should not fire with empty deudores", lista.find { it.tipo == RecomendacionTipo.COBRAR })
    }

    // ── R2: MEJORAR_PRECIO ─────────────────────────────────────────────────

    @Test
    fun mejorarPrecio_whenLowMarginAboveThreshold_returnsRecomendacion() = runBlocking {
        val categorias = listOf(
            categoria("Monturas Economicas", ventas = 960.0, costos = 880.0, margenPct = 8.3)
        )
        mockDeps(analisis = cleanAnalisis(categorias = categorias))

        val lista = listaDe(useCase.invoke("optica1", testMes))
        val mejorar = lista.find { it.tipo == RecomendacionTipo.MEJORAR_PRECIO }
        assertNotNull("MEJORAR_PRECIO should fire", mejorar)
        assertEquals(Prioridad.ALTA, mejorar!!.prioridad)
        assertTrue(mejorar.detalle.contains("Monturas Economicas"))
    }

    @Test
    fun mejorarPrecio_whenBelowMonetaryThreshold_returnsNull() = runBlocking {
        val categorias = listOf(
            categoria("Accesorios", ventas = 2.0, costos = 1.9, margenPct = 5.0)
        )
        mockDeps(analisis = cleanAnalisis(categorias = categorias))

        val lista = listaDe(useCase.invoke("optica1", testMes))
        assertNull(lista.find { it.tipo == RecomendacionTipo.MEJORAR_PRECIO })
    }

    @Test
    fun mejorarPrecio_whenHealthyMargin_returnsNull() = runBlocking {
        val categorias = listOf(
            categoria("Lentes", ventas = 1000.0, costos = 700.0, margenPct = 30.0)
        )
        mockDeps(analisis = cleanAnalisis(categorias = categorias))

        val lista = listaDe(useCase.invoke("optica1", testMes))
        assertNull(lista.find { it.tipo == RecomendacionTipo.MEJORAR_PRECIO })
    }

    // ── R3: LIQUIDAR_STOCK ─────────────────────────────────────────────────

    @Test
    fun liquidarStock_whenItemsExceedDiasThreshold_returnsRecomendacion() = runBlocking {
        val stock = listOf(
            stockItem("Modelo A", dias = 210, costo = 120.0),
            stockItem("Modelo B", dias = 210, costo = 80.0),
            stockItem("Modelo C", dias = 45, costo = 100.0)
        )
        mockDeps(analisis = cleanAnalisis(stockEstancado = stock))

        val lista = listaDe(useCase.invoke("optica1", testMes))
        val liquidar = lista.find { it.tipo == RecomendacionTipo.LIQUIDAR_STOCK }
        assertNotNull("LIQUIDAR_STOCK should fire", liquidar)
        assertEquals(Prioridad.MEDIA, liquidar!!.prioridad)
        assertTrue(liquidar.detalle.contains("Modelo A"))
        assertTrue(liquidar.detalle.contains("Modelo B"))
        assertTrue(!liquidar.detalle.contains("Modelo C"))
    }

    @Test
    fun liquidarStock_whenAllItemsBelowThreshold_returnsNull() = runBlocking {
        val stock = listOf(stockItem("Modelo A", dias = 30), stockItem("Modelo B", dias = 90))
        mockDeps(analisis = cleanAnalisis(stockEstancado = stock))

        val lista = listaDe(useCase.invoke("optica1", testMes))
        assertNull(lista.find { it.tipo == RecomendacionTipo.LIQUIDAR_STOCK })
    }

    @Test
    fun liquidarStock_whenEmptyList_returnsNull() = runBlocking {
        mockDeps(analisis = cleanAnalisis(stockEstancado = emptyList()))
        val lista = listaDe(useCase.invoke("optica1", testMes))
        assertNull(lista.find { it.tipo == RecomendacionTipo.LIQUIDAR_STOCK })
    }

    // ── R4: VENDER_MAS_DE ──────────────────────────────────────────────────

    @Test
    fun venderMasDe_whenHighMarginHighContribution_returnsRecomendacion() = runBlocking {
        val categorias = listOf(
            categoria("Lentes Progresivos", ventas = 4800.0, costos = 2640.0, margenPct = 45.0),
            categoria("Monturas Estandar", ventas = 1000.0, costos = 600.0, margenPct = 40.0)
        )
        mockDeps(analisis = cleanAnalisis(categorias = categorias))

        val lista = listaDe(useCase.invoke("optica1", testMes))
        val vender = lista.find { it.tipo == RecomendacionTipo.VENDER_MAS_DE }
        assertNotNull("VENDER_MAS_DE should fire", vender)
        assertEquals(Prioridad.MEDIA, vender!!.prioridad)
        assertTrue(vender.detalle.contains("45%"))
        assertTrue(vender.detalle.contains("Lentes Progresivos"))
    }

    @Test
    fun venderMasDe_whenHighMarginButLowContribution_returnsNull() = runBlocking {
        val categorias = listOf(
            categoria("Nicho", ventas = 100.0, costos = 50.0, margenPct = 50.0),
            categoria("Volumen", ventas = 10000.0, costos = 7000.0, margenPct = 30.0)
        )
        mockDeps(analisis = cleanAnalisis(categorias = categorias))

        val lista = listaDe(useCase.invoke("optica1", testMes))
        assertNull(lista.find { it.tipo == RecomendacionTipo.VENDER_MAS_DE })
    }

    @Test
    fun venderMasDe_whenBelowMonetaryThreshold_returnsNull() = runBlocking {
        val categorias = listOf(
            categoria("Baratija", ventas = 3.0, costos = 1.65, margenPct = 45.0)
        )
        mockDeps(analisis = cleanAnalisis(categorias = categorias))

        val lista = listaDe(useCase.invoke("optica1", testMes))
        assertNull(lista.find { it.tipo == RecomendacionTipo.VENDER_MAS_DE })
    }

    // ── R5: ALERTA_CAIDA ───────────────────────────────────────────────────

    @Test
    fun alertaCaida_whenDropExceedsThreshold_returnsRecomendacion() = runBlocking {
        mockDeps(analisis = cleanAnalisis(variacionVentasPct = -15.0))

        val lista = listaDe(useCase.invoke("optica1", testMes))
        val alerta = lista.find { it.tipo == RecomendacionTipo.ALERTA_CAIDA }
        assertNotNull("ALERTA_CAIDA should fire", alerta)
        assertEquals(Prioridad.ALTA, alerta!!.prioridad)
        assertTrue(alerta.detalle.contains("15%"))
    }

    @Test
    fun alertaCaida_whenDropBelowThreshold_returnsNull() = runBlocking {
        mockDeps(analisis = cleanAnalisis(variacionVentasPct = -3.0))
        val lista = listaDe(useCase.invoke("optica1", testMes))
        assertNull(lista.find { it.tipo == RecomendacionTipo.ALERTA_CAIDA })
    }

    @Test
    fun alertaCaida_whenNullVariation_returnsNull() = runBlocking {
        mockDeps(analisis = cleanAnalisis(variacionVentasPct = null))
        val lista = listaDe(useCase.invoke("optica1", testMes))
        assertNull(lista.find { it.tipo == RecomendacionTipo.ALERTA_CAIDA })
    }

    // ── R6: REDUCIR_GASTO ──────────────────────────────────────────────────

    @Test
    fun reducirGasto_whenRatioExceeds40Percent_returnsRecomendacion() = runBlocking {
        mockDeps(analisis = cleanAnalisis(ventasMes = 9400.0, gastosMes = 3900.0))

        val lista = listaDe(useCase.invoke("optica1", testMes))
        val reducir = lista.find { it.tipo == RecomendacionTipo.REDUCIR_GASTO }
        assertNotNull("REDUCIR_GASTO should fire", reducir)
        assertEquals(Prioridad.MEDIA, reducir!!.prioridad)
        assertTrue(reducir.detalle.contains("41"))
    }

    @Test
    fun reducirGasto_whenRatioBelow40Percent_returnsNull() = runBlocking {
        mockDeps(analisis = cleanAnalisis(ventasMes = 10000.0, gastosMes = 1000.0))
        val lista = listaDe(useCase.invoke("optica1", testMes))
        assertNull(lista.find { it.tipo == RecomendacionTipo.REDUCIR_GASTO })
    }

    @Test
    fun reducirGasto_whenVentasMesIsZero_returnsNull() = runBlocking {
        mockDeps(analisis = cleanAnalisis(ventasMes = 0.0, gastosMes = 5000.0))
        val lista = listaDe(useCase.invoke("optica1", testMes))
        assertNull(lista.find { it.tipo == RecomendacionTipo.REDUCIR_GASTO })
    }

    // ── Orchestrator ───────────────────────────────────────────────────────

    @Test
    fun invoke_whenAllRulesFire_returnsCappedList() = runBlocking {
        val deudores = listOf(deudor("Juan", saldo = 4000.0, dias = 15))
        val categorias = listOf(
            categoria("Mont Econ", ventas = 960.0, costos = 880.0, margenPct = 8.3),
            categoria("Lentes Prog", ventas = 4800.0, costos = 2640.0, margenPct = 45.0),
            categoria("Volumen", ventas = 10000.0, costos = 7000.0, margenPct = 30.0)
        )
        val stock = listOf(stockItem("Mod A", dias = 210, costo = 120.0))
        val analisis = cleanAnalisis(
            ventasMes = 12000.0, gastosMes = 6000.0, variacionVentasPct = -15.0,
            categorias = categorias, stockEstancado = stock
        )
        mockDeps(analisis = analisis, deudores = deudores)

        val lista = listaDe(useCase.invoke("optica1", testMes))
        assertEquals(5, lista.size)
        assertNull(lista.find { it.tipo == RecomendacionTipo.REDUCIR_GASTO })
    }

    @Test
    fun invoke_whenNoRulesFire_returnsEmptyList() = runBlocking {
        mockDeps()
        val lista = listaDe(useCase.invoke("optica1", testMes))
        assertTrue(lista.isEmpty())
    }

    @Test
    fun invoke_whenAnalisisError_returnsError() = runBlocking {
        coEvery { analisisUseCase.invoke("optica1", testMes) } returns Resource.Error("sin conexion")
        val result = useCase.invoke("optica1", testMes)
        assertTrue(result is Resource.Error)
        assertEquals("sin conexion", (result as Resource.Error).message)
    }

    @Test
    fun invoke_prioridadOrderAltaBeforeMedia() = runBlocking {
        val deudores = listOf(deudor("Juan", saldo = 4000.0, dias = 15))
        val categorias = listOf(
            categoria("Mont Econ", ventas = 960.0, costos = 880.0, margenPct = 8.3)
        )
        val stock = listOf(stockItem("Mod A", dias = 210))
        val analisis = cleanAnalisis(
            ventasMes = 10000.0, variacionVentasPct = -15.0,
            categorias = categorias, stockEstancado = stock
        )
        mockDeps(analisis = analisis, deudores = deudores)

        val lista = listaDe(useCase.invoke("optica1", testMes))
        assertEquals(4, lista.size)
        assertEquals(Prioridad.ALTA, lista[0].prioridad)
        assertEquals(Prioridad.ALTA, lista[1].prioridad)
        assertEquals(Prioridad.ALTA, lista[2].prioridad)
        assertEquals(Prioridad.MEDIA, lista[3].prioridad)
    }

    @Test
    fun invoke_cappingKeepsHighestPriority() = runBlocking {
        val deudores = listOf(deudor("Juan", saldo = 4000.0, dias = 15))
        val categorias = listOf(
            categoria("Mont Econ", ventas = 960.0, costos = 880.0, margenPct = 8.3),
            categoria("Lentes Prog", ventas = 4800.0, costos = 2640.0, margenPct = 45.0),
            categoria("Volumen", ventas = 10000.0, costos = 7000.0, margenPct = 30.0)
        )
        val stock = listOf(stockItem("Mod A", dias = 210))
        val analisis = cleanAnalisis(
            ventasMes = 12000.0, gastosMes = 6000.0, variacionVentasPct = -15.0,
            categorias = categorias, stockEstancado = stock
        )
        mockDeps(analisis = analisis, deudores = deudores)

        val lista = listaDe(useCase.invoke("optica1", testMes))
        assertEquals(5, lista.size)
        val tiposPresentes = lista.map { it.tipo }.toSet()
        assertEquals(5, tiposPresentes.size)
        assertTrue(!tiposPresentes.contains(RecomendacionTipo.REDUCIR_GASTO))
    }

    @Test
    fun invoke_readsConfigFromDao() = runBlocking {
        mockDeps()
        useCase.invoke("optica1", testMes)
        coVerify(exactly = 1) { configDao.getByOpticaIdOnce("optica1") }
    }
}
