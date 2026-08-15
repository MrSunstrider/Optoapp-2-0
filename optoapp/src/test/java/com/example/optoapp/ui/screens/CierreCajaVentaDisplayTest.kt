package com.example.optoapp.ui.screens

import com.example.optoapp.data.DispensacionOptica
import com.example.optoapp.data.Pago
import com.example.optoapp.data.ServicioExtra
import com.example.optoapp.viewmodel.PagoDisplayItem
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class CierreCajaVentaDisplayTest {

    private val today = LocalDate.of(2026, 8, 15)

    @Test
    fun `dispensacionVentaTitle uses OT when present`() {
        val disp = DispensacionOptica(
            id = "d1",
            ot = "2026-0050",
            pacienteId = "p1",
            fecha = today,
        )
        assertEquals("OT 2026-0050", dispensacionVentaTitle(disp))
    }

    @Test
    fun `dispensacionVentaTitle falls back to id prefix when OT blank`() {
        val disp = DispensacionOptica(
            id = "abcdefgh1234",
            pacienteId = "p1",
            fecha = today,
        )
        assertEquals("Dispensación abcdefgh", dispensacionVentaTitle(disp))
    }

    @Test
    fun `dispensacionVentaSubtitle prefers tipo and material lente`() {
        val disp = DispensacionOptica(
            id = "d1",
            pacienteId = "p1",
            fecha = today,
            tipoLente = "Monofocal",
            materialLente = "CR-39",
        )
        assertEquals("Monofocal · CR-39", dispensacionVentaSubtitle(disp))
    }

    @Test
    fun `dispensacionVentaSubtitle falls back to descripcion montura`() {
        val disp = DispensacionOptica(
            id = "d1",
            pacienteId = "p1",
            fecha = today,
            descripcionMontura = "Aro metálico",
        )
        assertEquals("Aro metálico", dispensacionVentaSubtitle(disp))
    }

    @Test
    fun `servicioVentaOtLine returns OT when present`() {
        val serv = ServicioExtra(
            id = "s1",
            ot = "2026-0042",
            descripcion = "Reparación bisel",
            montoTotal = 80.0,
            estado = "Pendiente",
            fecha = today,
        )
        assertEquals("OT 2026-0042", servicioVentaOtLine(serv))
    }

    @Test
    fun `servicioVentaOtLine returns null when OT blank`() {
        val serv = ServicioExtra(
            id = "s1",
            descripcion = "Limpieza",
            montoTotal = 30.0,
            estado = "Entregado",
            fecha = today,
        )
        assertEquals(null, servicioVentaOtLine(serv))
    }

    @Test
    fun `heroCobradoLabel returns COBRADO HOY for today`() {
        assertEquals("COBRADO HOY", heroCobradoLabel(today, today))
    }

    @Test
    fun `heroCobradoLabel returns COBRADO AYER for yesterday`() {
        assertEquals("COBRADO AYER", heroCobradoLabel(today.minusDays(1), today))
    }

    @Test
    fun `heroCobradoLabel returns TOTAL COBRADO for other dates`() {
        assertEquals("TOTAL COBRADO", heroCobradoLabel(today.minusDays(3), today))
    }

    @Test
    fun `filterPagoDisplayItems matches OT label`() {
        val item = PagoDisplayItem(
            pago = Pago(
                id = "p1",
                fecha = today,
                tipo = "Abono",
                monto = 100.0,
                metodoPago = "Efectivo",
            ),
            label = "OT 2026-0042",
            tipoEntidad = "Dispensación",
            esCobroAtrasado = false,
            dispensacionId = "d1",
            servicioExtraId = null,
            pacienteId = "pac1",
        )
        val filtered = filterPagoDisplayItems(listOf(item), "0042", emptyMap())
        assertEquals(1, filtered.size)
        assertEquals(0, filterPagoDisplayItems(listOf(item), "9999", emptyMap()).size)
    }

    @Test
    fun `filterDispensaciones matches paciente nombre`() {
        val disp = DispensacionOptica(
            id = "d1",
            ot = "2026-0010",
            pacienteId = "pac1",
            fecha = today,
            tipoLente = "Monofocal",
        )
        val nombres = mapOf("pac1" to "María García")
        assertEquals(1, filterDispensaciones(listOf(disp), "maría", nombres).size)
        assertEquals(0, filterDispensaciones(listOf(disp), "pedro", nombres).size)
    }

    @Test
    fun `filterServiciosExtra matches descripcion`() {
        val serv = ServicioExtra(
            id = "s1",
            descripcion = "Reparación bisel",
            montoTotal = 50.0,
            estado = "Pendiente",
            fecha = today,
        )
        assertEquals(1, filterServiciosExtra(listOf(serv), "bisel", emptyMap()).size)
    }
}
