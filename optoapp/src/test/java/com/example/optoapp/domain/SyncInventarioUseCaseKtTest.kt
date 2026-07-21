package com.example.optoapp.domain

import com.example.optoapp.data.MonturaMovimiento
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

/**
 * Bug 3: MonturaMovimientoRemoto must include user_id, costo_unitario, tipo_documento
 * that are NOT NULL in the remote DB but were missing from the sync DTO.
 */
class SyncInventarioUseCaseKtTest {

    @Test
    fun monturaMovimiento_toRemoto_includesUserId() {
        val mov = MonturaMovimiento(
            id = "mov1", monturaId = "m1",
            fecha = LocalDate.of(2026, 6, 20),
            tipo = "ENTRADA", cantidad = 5,
            stockPrevio = 10, stockNuevo = 15,
            opticaId = "o1", userId = "usr-99",
            costoUnitario = 45.0, tipoDocumento = "FACTURA",
        )
        val remoto = mov.toRemoto()
        assertEquals("usr-99", remoto.userId)
    }

    @Test
    fun monturaMovimiento_toRemoto_includesCostoUnitario() {
        val mov = MonturaMovimiento(
            id = "mov2", monturaId = "m2",
            fecha = LocalDate.of(2026, 6, 20),
            tipo = "SALIDA_VENTA", cantidad = 1,
            stockPrevio = 5, stockNuevo = 4,
            opticaId = "o1", userId = "usr-1",
            costoUnitario = 120.5, tipoDocumento = "BOLETA",
        )
        val remoto = mov.toRemoto()
        assertEquals(120.5, remoto.costoUnitario, 0.001)
    }

    @Test
    fun monturaMovimiento_toRemoto_includesTipoDocumento() {
        val mov = MonturaMovimiento(
            id = "mov3", monturaId = "m3",
            fecha = LocalDate.of(2026, 6, 20),
            tipo = "AJUSTE", cantidad = 2,
            stockPrevio = 20, stockNuevo = 18,
            opticaId = "o1", userId = "usr-2",
            costoUnitario = 0.0, tipoDocumento = "AJUSTE_INVENTARIO",
        )
        val remoto = mov.toRemoto()
        assertEquals("AJUSTE_INVENTARIO", remoto.tipoDocumento)
    }

    @Test
    fun monturaMovimiento_toRemoto_roundTripAllFields() {
        val mov = MonturaMovimiento(
            id = "mov4", monturaId = "m4",
            fecha = LocalDate.of(2026, 6, 20),
            tipo = "ENTRADA", cantidad = 3,
            stockPrevio = 0, stockNuevo = 3,
            referenciaId = "ref-1", nota = "Compra proveedor",
            opticaId = "optica-x", userId = "usr-x",
            costoUnitario = 75.0, tipoDocumento = "GUIA",
        )
        val remoto = mov.toRemoto()
        assertEquals("mov4", remoto.id)
        assertEquals("m4", remoto.monturaId)
        assertEquals("ENTRADA", remoto.tipo)
        assertEquals(3, remoto.cantidad)
        assertEquals(0, remoto.stockPrevio)
        assertEquals(3, remoto.stockNuevo)
        assertEquals("optica-x", remoto.opticaId)
        assertEquals("usr-x", remoto.userId)
        assertEquals(75.0, remoto.costoUnitario, 0.001)
        assertEquals("GUIA", remoto.tipoDocumento)
    }

    @Test
    fun monturaMovimiento_toRemoto_defaultsForEmptyFields() {
        val mov = MonturaMovimiento(
            id = "mov5",
            monturaId = "m5",
            fecha = LocalDate.of(2026, 6, 20),
            tipo = "AJUSTE",
            cantidad = 0,
            stockPrevio = 0,
            stockNuevo = 0,
            opticaId = "o1",
        )
        val remoto = mov.toRemoto()
        assertEquals("", remoto.userId)
        assertEquals(0.0, remoto.costoUnitario, 0.001)
        assertEquals("", remoto.tipoDocumento)
    }
}
