package com.example.optoapp.util

import com.example.optoapp.data.OrdenCompra
import com.example.optoapp.data.OrdenCompraItem
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class PrintRecepcionSlipTest {

    private fun testOc(
        numero: String = "OC-001",
        fecha: LocalDate = LocalDate.of(2025, 1, 15),
        estado: String = "COMPLETADA",
        total: Double = 150.0,
    ) = OrdenCompra(
        id = "oc1",
        numero = numero,
        proveedorId = "prov1",
        fecha = fecha,
        estado = estado,
        total = total,
        opticaId = "o1",
    )

    private fun testItem(
        id: String = "item1",
        monturaId: String = "M-001",
        cantidad: Int = 2,
        costoUnitario: Double = 50.0,
    ) = OrdenCompraItem(
        id = id,
        ordenId = "oc1",
        monturaId = monturaId,
        cantidad = cantidad,
        costoUnitario = costoUnitario,
    )

    @Test
    fun `buildReceiptHtml contains OC number`() {
        val html = PrintRecepcionSlip.buildReceiptHtml(testOc(), emptyList(), "Proveedor X")
        assertTrue(html.contains("OC-001"))
    }

    @Test
    fun `buildReceiptHtml contains proveedor nombre`() {
        val html = PrintRecepcionSlip.buildReceiptHtml(testOc(), emptyList(), "Óptica del Sur")
        assertTrue(html.contains("Óptica del Sur"))
    }

    @Test
    fun `buildReceiptHtml contains empty proveedor gracefully`() {
        val html = PrintRecepcionSlip.buildReceiptHtml(testOc(), emptyList(), "")
        assertTrue(html.contains("Proveedor:"))
    }

    @Test
    fun `buildReceiptHtml contains fecha`() {
        val html = PrintRecepcionSlip.buildReceiptHtml(
            testOc(fecha = LocalDate.of(2025, 3, 20)),
            emptyList(),
            "Prov",
        )
        // Fecha formateada como dd/MM/yyyy
        assertTrue(html.contains("20/03/2025"))
    }

    @Test
    fun `buildReceiptHtml contains estado`() {
        val html = PrintRecepcionSlip.buildReceiptHtml(
            testOc(estado = "RECIBIDA"),
            emptyList(),
            "Prov",
        )
        assertTrue(html.contains("RECIBIDA"))
    }

    @Test
    fun `buildReceiptHtml with single item`() {
        val items = listOf(testItem(monturaId = "M-001", cantidad = 3, costoUnitario = 25.0))
        val html = PrintRecepcionSlip.buildReceiptHtml(testOc(), items, "Prov")
        assertTrue(html.contains("M-001"))
        assertTrue(html.contains("3"))
        assertTrue(html.contains("25.00"))
        assertTrue(html.contains("75.00")) // 3 * 25.0
    }

    @Test
    fun `buildReceiptHtml with multiple items`() {
        val items = listOf(
            testItem(id = "i1", monturaId = "M-001", cantidad = 1, costoUnitario = 100.0),
            testItem(id = "i2", monturaId = "M-002", cantidad = 2, costoUnitario = 50.0),
        )
        val html = PrintRecepcionSlip.buildReceiptHtml(testOc(), items, "Prov")
        assertTrue(html.contains("M-001"))
        assertTrue(html.contains("M-002"))
    }

    @Test
    fun `buildReceiptHtml contains total`() {
        val html = PrintRecepcionSlip.buildReceiptHtml(
            testOc(total = 299.99),
            emptyList(),
            "Prov",
        )
        assertTrue(html.contains("299.99"))
    }

    @Test
    fun `buildReceiptHtml with zero quantities`() {
        val items = listOf(testItem(cantidad = 0, costoUnitario = 50.0))
        val html = PrintRecepcionSlip.buildReceiptHtml(testOc(), items, "Prov")
        assertTrue(html.contains("0.00")) // subtotal 0 * 50
    }

    @Test
    fun `buildReceiptHtml is valid HTML structure`() {
        val html = PrintRecepcionSlip.buildReceiptHtml(testOc(), emptyList(), "Prov")
        assertTrue(html.contains("<!DOCTYPE html>"))
        assertTrue(html.contains("<html>"))
        assertTrue(html.contains("<head>"))
        assertTrue(html.contains("<body>"))
        assertTrue(html.contains("</html>"))
    }

    @Test
    fun `buildReceiptHtml with empty items produces no table rows`() {
        val html = PrintRecepcionSlip.buildReceiptHtml(testOc(), emptyList(), "Prov")
        assertTrue(html.contains("<tbody>"))
        // No <tr> inside tbody
        val tbodyStart = html.indexOf("<tbody>")
        val tbodyEnd = html.indexOf("</tbody>")
        val tbodyContent = html.substring(tbodyStart, tbodyEnd)
        assertTrue(!tbodyContent.contains("<tr>") || tbodyContent.trim() == "<tbody>")
    }

    @Test
    fun `buildReceiptHtml with fractional costo`() {
        val items = listOf(testItem(cantidad = 3, costoUnitario = 33.33))
        val html = PrintRecepcionSlip.buildReceiptHtml(testOc(), items, "Prov")
        assertTrue(html.contains("33.33"))
        assertTrue(html.contains("99.99")) // 3 * 33.33
    }
}
