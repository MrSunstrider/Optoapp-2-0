package com.example.optoapp.domain

import com.example.optoapp.data.DispensacionItem
import com.example.optoapp.domain.ComponenteCosto.BISELADO
import com.example.optoapp.domain.ComponenteCosto.LC
import com.example.optoapp.domain.ComponenteCosto.MONTURA
import com.example.optoapp.domain.ComponenteCosto.OD
import com.example.optoapp.domain.ComponenteCosto.OI
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CapturaCostoItemTest {

    private val receta = RecetaCosto(odEsf = -1.0, odCil = -0.5, oiEsf = -1.25, oiCil = null)

    private val item = DispensacionItem(
        id = "item-1",
        dispensacionId = "disp-1",
        tipoLente = "Monofocal",
        materialLente = "Resina",
        tratamientos = listOf("Antireflex"),
        monturaId = "mont-1",
        origenMontura = "Tienda",
        tipoAro = "Aro completo",
    )

    private val matriz = mapOf(OD to 22.0, OI to 21.0, MONTURA to 99.0, BISELADO to 9.0)

    private var lookups = 0
    private var itemConsultado: DispensacionItem? = null

    private suspend fun capturar(
        nuevo: DispensacionItem = item,
        previo: DispensacionItem? = null,
        overrides: Map<ComponenteCosto, Double> = emptyMap(),
        recetaActual: RecetaCosto? = receta,
        recetaPrevia: RecetaCosto? = receta,
        resultado: Map<ComponenteCosto, Double?> = matriz,
    ): DispensacionItem = CapturaCostoItem.capturar(nuevo, previo, overrides, recetaActual, recetaPrevia) { consultado ->
        lookups++
        itemConsultado = consultado
        CostoItemCalculado(resultado)
    }

    private fun persistido(origen: String?, od: Double?, oi: Double?, montura: Double? = 30.0, biselado: Double? = 8.0) =
        item.copy(
            costoRealOd = od,
            costoRealOi = oi,
            costoRealMontura = montura,
            costoRealBiselado = biselado,
            costoOrigen = origen,
        )

    @Test
    fun `a new item captures every applicable component from the matrix`() = runTest {
        val guardado = capturar()

        assertEquals(22.0, guardado.costoRealOd!!, 0.001)
        assertEquals(21.0, guardado.costoRealOi!!, 0.001)
        assertEquals(99.0, guardado.costoRealMontura!!, 0.001)
        assertEquals(9.0, guardado.costoRealBiselado!!, 0.001)
        assertNull(guardado.costoRealLc)
        assertEquals("matriz", guardado.costoOrigen)
    }

    @Test
    fun `a missing lookup persists zero with mixto origin when other components hit`() = runTest {
        val guardado = capturar(resultado = mapOf(OD to 22.0, OI to null))

        assertEquals(0.0, guardado.costoRealOi!!, 0.001)
        assertEquals(22.0, guardado.costoRealOd!!, 0.001)
        assertEquals("mixto", guardado.costoOrigen)
    }

    @Test
    fun `a session override is never replaced even when the lens material changed`() = runTest {
        val guardado = capturar(
            nuevo = item.copy(materialLente = "Policarbonato"),
            previo = persistido("matriz", od = 18.0, oi = 18.0),
            overrides = mapOf(OD to 25.0),
        )

        assertEquals(25.0, guardado.costoRealOd!!, 0.001)
        assertEquals(21.0, guardado.costoRealOi!!, 0.001)
        assertEquals("mixto", guardado.costoOrigen)
    }

    @Test
    fun `an edit with unchanged inputs keeps the snapshot without any lookup`() = runTest {
        val guardado = capturar(previo = persistido("matriz", od = 18.0, oi = 18.0))

        assertEquals(0, lookups)
        assertEquals(18.0, guardado.costoRealOd!!, 0.001)
        assertEquals(18.0, guardado.costoRealOi!!, 0.001)
        assertEquals(8.0, guardado.costoRealBiselado!!, 0.001)
        assertEquals("matriz", guardado.costoOrigen)
    }

    @Test
    fun `a changed lens material re-looks-up the lenses and the bevel only`() = runTest {
        val guardado = capturar(
            nuevo = item.copy(materialLente = "Policarbonato"),
            previo = persistido("matriz", od = 18.0, oi = 18.0),
        )

        assertEquals(22.0, guardado.costoRealOd!!, 0.001)
        assertEquals(21.0, guardado.costoRealOi!!, 0.001)
        assertEquals(9.0, guardado.costoRealBiselado!!, 0.001)
        assertEquals(30.0, guardado.costoRealMontura!!, 0.001)
        assertEquals("matriz", guardado.costoOrigen)
    }

    @Test
    fun `a changed contact lens modality or material re-looks-up the lc cost`() = runTest {
        val lc = item.copy(
            tipoLente = "Lentes de Contacto", materialLente = "Graduado",
            colorLente = "HEMA", notasDiseno = "Mensual",
            monturaId = "", origenMontura = "", tipoAro = "",
        )
        val previo = lc.copy(costoRealLc = 40.0, costoOrigen = "matriz")
        val nuevoPrecio = mapOf(LC to 55.0)

        val otraModalidad = capturar(nuevo = lc.copy(notasDiseno = "Diario"), previo = previo, resultado = nuevoPrecio)
        val otroMaterial = capturar(nuevo = lc.copy(colorLente = "RGP"), previo = previo, resultado = nuevoPrecio)

        assertEquals(55.0, otraModalidad.costoRealLc!!, 0.001)
        assertEquals(55.0, otroMaterial.costoRealLc!!, 0.001)
        assertEquals(2, lookups)
    }

    @Test
    fun `a prescription change on one eye re-looks-up only that eye`() = runTest {
        val guardado = capturar(
            previo = persistido("matriz", od = 18.0, oi = 18.0),
            recetaPrevia = receta.copy(oiEsf = -2.0),
        )

        assertEquals(18.0, guardado.costoRealOd!!, 0.001)
        assertEquals(21.0, guardado.costoRealOi!!, 0.001)
        assertEquals(8.0, guardado.costoRealBiselado!!, 0.001)
    }

    @Test
    fun `a cylinder series change re-looks-up the bevel`() = runTest {
        val guardado = capturar(
            previo = persistido("matriz", od = 18.0, oi = 18.0),
            recetaPrevia = receta.copy(odCil = -3.0),
        )

        assertEquals(9.0, guardado.costoRealBiselado!!, 0.001)
        assertEquals(18.0, guardado.costoRealOi!!, 0.001)
    }

    @Test
    fun `a faltante item re-looks-up only its null or zero components`() = runTest {
        val guardado = capturar(previo = persistido("faltante", od = 18.0, oi = 0.0))

        assertEquals(18.0, guardado.costoRealOd!!, 0.001)
        assertEquals(21.0, guardado.costoRealOi!!, 0.001)
        assertEquals(30.0, guardado.costoRealMontura!!, 0.001)
        assertEquals("mixto", guardado.costoOrigen)
    }

    @Test
    fun `a legacy item without origin re-looks-up only its empty components`() = runTest {
        val guardado = capturar(previo = persistido(origen = null, od = 18.0, oi = null))

        assertEquals(18.0, guardado.costoRealOd!!, 0.001)
        assertEquals(21.0, guardado.costoRealOi!!, 0.001)
        assertEquals("mixto", guardado.costoOrigen)
    }

    @Test
    fun `a persisted manual value is re-looked-up when its inputs changed`() = runTest {
        val guardado = capturar(
            nuevo = item.copy(tratamientos = listOf("Blue cut")),
            previo = persistido("manual", od = 40.0, oi = 40.0, montura = 35.0, biselado = 8.0),
        )

        assertEquals(22.0, guardado.costoRealOd!!, 0.001)
        assertEquals(35.0, guardado.costoRealMontura!!, 0.001)
        assertEquals("mixto", guardado.costoOrigen)
    }

    @Test
    fun `a component that stops applying after a re-lookup is cleared`() = runTest {
        val guardado = capturar(
            nuevo = item.copy(origenMontura = "Paciente", monturaId = ""),
            previo = persistido("matriz", od = 18.0, oi = 18.0),
            resultado = mapOf(OD to 22.0, OI to 21.0, BISELADO to 9.0),
        )

        assertNull(guardado.costoRealMontura)
        assertEquals(18.0, guardado.costoRealOd!!, 0.001)
    }

    @Test
    fun `a high-index treatment is saved as the normalized alto indice`() = runTest {
        val guardado = capturar(nuevo = item.copy(tratamientos = listOf("Alto Índice Blanco 1.7")))

        assertEquals("1.70", guardado.altoIndice)
        assertEquals("1.70", itemConsultado!!.altoIndice)
    }

    @Test
    fun `an item without a high-index treatment keeps alto indice null`() = runTest {
        assertNull(capturar().altoIndice)
    }

    @Test
    fun `a persisted alto indice not derived from treatments is kept and does not trigger a bevel lookup`() = runTest {
        val guardado = capturar(previo = persistido("matriz", od = 18.0, oi = 18.0).copy(altoIndice = "1.67"))

        assertEquals("1.67", guardado.altoIndice)
        assertEquals(0, lookups)
    }

    @Test
    fun `adding a high-index treatment re-looks-up the bevel`() = runTest {
        val guardado = capturar(
            nuevo = item.copy(tratamientos = listOf("Antireflex", "Alto Índice Blanco 1.7")),
            previo = persistido("matriz", od = 18.0, oi = 18.0),
        )

        assertEquals(9.0, guardado.costoRealBiselado!!, 0.001)
        assertEquals(30.0, guardado.costoRealMontura!!, 0.001)
    }

    @Test
    fun `removing the high-index treatment clears the derived alto indice and re-looks-up the bevel`() = runTest {
        val previo = persistido("matriz", od = 18.0, oi = 18.0)
            .copy(tratamientos = listOf("Antireflex", "Alto Índice Blanco 1.7"), altoIndice = "1.70")

        val guardado = capturar(previo = previo)

        assertNull(guardado.altoIndice)
        assertEquals(9.0, guardado.costoRealBiselado!!, 0.001)
    }

    @Test
    fun `the detailed capture reports the origin of each captured component`() = runTest {
        val detalle = CapturaCostoItem.capturarDetalle(
            item,
            persistido(null, od = 18.0, oi = 18.0, biselado = null),
            mapOf(OD to 25.0),
            receta,
            receta,
        ) { CostoItemCalculado(mapOf(BISELADO to null)) }

        assertEquals(OrigenComponente.MANUAL, detalle.origenes[OD])
        assertEquals(OrigenComponente.PRESERVADA, detalle.origenes[OI])
        assertEquals(OrigenComponente.PRESERVADA, detalle.origenes[MONTURA])
        assertEquals(OrigenComponente.FALTANTE, detalle.origenes[BISELADO])
        assertEquals(25.0, detalle.item.costoRealOd!!, 0.001)
        assertEquals("mixto", detalle.item.costoOrigen)
    }
}
