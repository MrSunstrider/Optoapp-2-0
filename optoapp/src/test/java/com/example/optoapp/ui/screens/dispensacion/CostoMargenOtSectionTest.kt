package com.example.optoapp.ui.screens.dispensacion

import com.example.optoapp.domain.ComponenteCosto.BISELADO
import com.example.optoapp.domain.ComponenteCosto.MONTURA
import com.example.optoapp.domain.ComponenteCosto.OD
import com.example.optoapp.domain.ComponenteCosto.OI
import com.example.optoapp.domain.OrigenComponente
import com.example.optoapp.util.MoneyFormatter
import com.example.optoapp.viewmodel.DispensacionItemUi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CostoMargenOtSectionTest {

    private val capturado = DispensacionItemUi(
        id = "item-1",
        costoRealOd = 25.0,
        costoRealOi = 25.0,
        costoRealMontura = 80.0,
        costoRealBiselado = 10.0,
        costoOrigenes = mapOf(
            OD to OrigenComponente.MATRIZ,
            OI to OrigenComponente.MATRIZ,
            MONTURA to OrigenComponente.MATRIZ,
            BISELADO to OrigenComponente.MATRIZ,
        ),
    )

    @Test
    fun `shows the breakdown total margin and margin percentage`() {
        val modelo = CostoMargenOtModel.from(200.0, listOf(capturado))

        assertEquals(listOf(OD, OI, MONTURA, BISELADO), modelo.items.single().filas.map { it.componente })
        assertTrue(modelo.items.single().filas.all { it.origen == "matriz" })
        assertEquals("S/ 140.00", modelo.costoTotalTexto)
        assertEquals("S/ 60.00", modelo.margenTexto)
        assertEquals("30.0 %", modelo.margenPctTexto)
        assertFalse(modelo.incompleto)
    }

    @Test
    fun `a session override wins over the captured value and is labelled manual`() {
        val item = capturado.copy(costoOverrides = mapOf(OD to 20.0))

        val modelo = CostoMargenOtModel.from(200.0, listOf(item))

        val fila = modelo.items.single().filas.first { it.componente == OD }
        assertEquals(20.0, fila.valor, 0.001)
        assertEquals("manual", fila.origen)
        assertTrue(fila.editado)
        assertEquals("S/ 65.00", modelo.margenTexto)
    }

    @Test
    fun `a missing bevel price is labelled faltante and marks the margin incomplete`() {
        val item = capturado.copy(
            costoRealBiselado = 0.0,
            costoOrigenes = capturado.costoOrigenes + (BISELADO to OrigenComponente.FALTANTE),
        )

        val modelo = CostoMargenOtModel.from(200.0, listOf(item))

        assertEquals("faltante", modelo.items.single().filas.first { it.componente == BISELADO }.origen)
        assertTrue(modelo.items.single().incompleto)
        assertTrue(modelo.incompleto)
    }

    @Test
    fun `a zero total shows a dash instead of a percentage`() {
        val modelo = CostoMargenOtModel.from(0.0, listOf(capturado))

        assertNull(modelo.margenPct)
        assertEquals(MoneyFormatter.NOT_AVAILABLE, modelo.margenPctTexto)
        assertEquals("-S/ 140.00", modelo.margenTexto)
    }

    @Test
    fun `a reloaded item shows its saved origin on every component`() {
        val recargado = capturado.copy(costoOrigenes = emptyMap(), costoOrigen = "matriz")

        val modelo = CostoMargenOtModel.from(200.0, listOf(recargado))

        assertEquals("matriz", modelo.items.single().origenGuardado)
        assertTrue(modelo.items.single().filas.all { it.origen == "matriz" })
        assertFalse(modelo.incompleto)
    }

    @Test
    fun `a reloaded faltante item stays incomplete without guessing component origins`() {
        val recargado = capturado.copy(costoOrigenes = emptyMap(), costoOrigen = "faltante")

        val modelo = CostoMargenOtModel.from(200.0, listOf(recargado))

        assertTrue(modelo.incompleto)
        assertTrue(modelo.items.single().filas.all { it.origen == null })
    }

    @Test
    fun `an item without any captured cost is incomplete`() {
        val modelo = CostoMargenOtModel.from(150.0, listOf(DispensacionItemUi(id = "nuevo")))

        assertTrue(modelo.items.single().filas.isEmpty())
        assertTrue(modelo.incompleto)
        assertEquals("100.0 %", modelo.margenPctTexto)
    }

    @Test
    fun `costs of every item are added to the order total`() {
        val segundo = DispensacionItemUi(id = "item-2", costoRealOd = 15.0, costoOrigen = "manual")

        val modelo = CostoMargenOtModel.from(310.0, listOf(capturado, segundo))

        assertEquals(155.0, modelo.costoTotal, 0.001)
        assertEquals("S/ 155.00", modelo.margenTexto)
        assertEquals("50.0 %", modelo.margenPctTexto)
    }
}
