package com.example.optoapp.domain

import com.example.optoapp.R
import com.example.optoapp.data.Resource
import com.example.optoapp.data.configuracionfinanciera.ConfiguracionFinancieraDao
import com.example.optoapp.data.configuracionfinanciera.ConfiguracionFinancieraEntity
import com.example.optoapp.ui.MainSources
import com.example.optoapp.ui.screens.labelRes
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class RecomendacionesLabelsTest {

    private val configDao: ConfiguracionFinancieraDao = mockk {
        coEvery { getByOpticaIdOnce(OPTICA_ID) } returns ConfiguracionFinancieraEntity(
            opticaId = OPTICA_ID,
            deudaTotalAlertaMonto = 3000.0,
            deudaViejaAlertaDias = 30,
            stockEstancadoAlertaDias = 180,
            caidaVentasAlertaPct = 10.0,
            minVentasParaRecomendar = 5,
        )
    }

    private val useCase = GenerarRecomendacionesUseCase(configDao)

    @Test
    fun prioridad_mapsToLabelResources() {
        assertEquals(R.string.analisis_prioridad_alta, Prioridad.ALTA.labelRes())
        assertEquals(R.string.analisis_prioridad_media, Prioridad.MEDIA.labelRes())
        assertEquals(R.string.analisis_prioridad_baja, Prioridad.BAJA.labelRes())
    }

    @Test
    fun prioridadLabels_readAsUrgenteImportanteSugerencia() {
        assertEquals("Urgente", MainSources.stringValue("analisis_prioridad_alta"))
        assertEquals("Importante", MainSources.stringValue("analisis_prioridad_media"))
        assertEquals("Sugerencia", MainSources.stringValue("analisis_prioridad_baja"))
    }

    @Test
    fun prioridad_keepsAltaMediaBajaOrder() = runBlocking {
        assertEquals(listOf("ALTA", "MEDIA", "BAJA"), Prioridad.entries.map { it.name })

        val stock = listOf(stockItem(dias = 210))
        val lista = listaDe(useCase(analisis(variacionVentasPct = -15.0, stock = stock), deudores(), OPTICA_ID))

        val ordinals = lista.map { it.prioridad.ordinal }
        assertEquals(ordinals.sorted(), ordinals)
        assertEquals(Prioridad.ALTA, lista.first().prioridad)
        assertEquals(Prioridad.MEDIA, lista.last().prioridad)
    }

    @Test
    fun cobrar_formatsAmountsWithMoneyFormatter() = runBlocking {
        val lista = listaDe(useCase(analisis(), deudores(), OPTICA_ID))
        val cobrar = lista.single { it.tipo == RecomendacionTipo.COBRAR }

        assertTrue(cobrar.detalle, cobrar.detalle.contains("S/ 4,200.00"))
        assertTrue(cobrar.impactoEstimado.orEmpty(), cobrar.impactoEstimado.orEmpty().contains("S/ 4,200.00"))
        assertEquals("Cobranza pendiente", cobrar.titulo)
    }

    @Test
    fun liquidarStock_formatsCostoTotalWithMoneyFormatter() = runBlocking {
        val lista = listaDe(useCase(analisis(stock = listOf(stockItem(dias = 210, costo = 1250.5))), emptyList(), OPTICA_ID))
        val liquidar = lista.single { it.tipo == RecomendacionTipo.LIQUIDAR_STOCK }

        assertTrue(liquidar.detalle, liquidar.detalle.contains("S/ 2,501.00"))
    }

    @Test
    fun reducirGasto_formatsAmountsWithMoneyFormatter() = runBlocking {
        val lista = listaDe(useCase(analisis(ventasMes = 9400.0, gastosMes = 3900.0), emptyList(), OPTICA_ID))
        val reducir = lista.single { it.tipo == RecomendacionTipo.REDUCIR_GASTO }

        assertTrue(reducir.detalle, reducir.detalle.contains("S/ 3,900.00"))
        assertTrue(reducir.detalle, reducir.detalle.contains("S/ 9,400.00"))
        assertTrue(reducir.detalle, reducir.detalle.contains("S/ 390.00"))
    }

    private fun listaDe(result: Resource<List<Recomendacion>>): List<Recomendacion> =
        (result as Resource.Success).data!!

    private fun deudores() = listOf(deudor("Juan", 2000.0), deudor("Maria", 2200.0))

    private fun deudor(nombre: String, saldo: Double) = Deudor(
        pacienteNombre = nombre,
        pacienteTelefono = "999888777",
        ventaId = "v-$nombre",
        ventaFecha = LocalDate.of(2026, 6, 1),
        montoTotal = saldo + 500.0,
        totalPagado = 500.0,
        saldo = saldo,
        diasDeuda = 10,
    )

    private fun stockItem(dias: Int, costo: Double = 100.0) = StockEstancadoItem(
        monturaId = "m-test",
        sku = "SKU-TEST",
        modelo = "Modelo Test",
        costo = costo,
        stockActual = 2,
        ultimaVenta = null,
        diasSinVenta = dias,
    )

    private fun analisis(
        ventasMes: Double = 10000.0,
        gastosMes: Double = 0.0,
        variacionVentasPct: Double? = 0.0,
        stock: List<StockEstancadoItem> = emptyList(),
    ) = AnalisisMensual(
        ventasMes = ventasMes, cobrosMes = 0.0, margenNetoPct = 0.0,
        margenPorCategoria = emptyList(), deudores = DeudoresResumen(0, 0.0),
        proyeccionCaja = null, stockEstancado = stock,
        valorInventario = 0.0, ventasMesAnterior = 0.0,
        variacionVentasPct = variacionVentasPct, gastosMes = gastosMes,
        esOffline = false,
    )

    private companion object {
        const val OPTICA_ID = "optica1"
    }
}
