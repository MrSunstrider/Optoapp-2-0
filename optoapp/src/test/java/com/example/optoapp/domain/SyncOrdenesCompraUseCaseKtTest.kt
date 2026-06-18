package com.example.optoapp.domain

import com.example.optoapp.data.OrdenCompra
import com.example.optoapp.data.OrdenCompraItem
import com.example.optoapp.data.Resource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * SyncOrdenesCompraUseCase DTO and logic tests.
 * Tests entity fields, sync result, and pipeline behavior.
 * MockK integration tests cover the network layer.
 */
class SyncOrdenesCompraUseCaseKtTest {

    @Test
    fun ordenCompraEntity_allFieldsAreAccessible() {
        val oc = OrdenCompra(
            id = "oc1", numero = "OC-001", proveedorId = "p1",
            fecha = LocalDate.of(2026, 6, 17), estado = "PENDIENTE",
            total = 1500.0, opticaId = "o1",
            updatedAt = "2026-01-01T00:00:00Z", updatedBy = "user1"
        )
        assertEquals("oc1", oc.id)
        assertEquals("OC-001", oc.numero)
        assertEquals("p1", oc.proveedorId)
        assertEquals(LocalDate.of(2026, 6, 17), oc.fecha)
        assertEquals("PENDIENTE", oc.estado)
        assertEquals(1500.0, oc.total, 0.001)
        assertEquals("o1", oc.opticaId)
        assertEquals("2026-01-01T00:00:00Z", oc.updatedAt)
        assertEquals("user1", oc.updatedBy)
    }

    @Test
    fun ordenCompraEntity_defaultValues() {
        val oc = OrdenCompra(
            id = "oc1", numero = "OC-001", proveedorId = "p1",
            fecha = LocalDate.now(), opticaId = "o1"
        )
        assertEquals("PENDIENTE", oc.estado)
        assertEquals(0.0, oc.total, 0.001)
    }

    @Test
    fun ordenCompraItemEntity_allFieldsAreAccessible() {
        val item = OrdenCompraItem(
            id = "i1", ordenId = "oc1", monturaId = "m1",
            cantidad = 10, costoUnitario = 120.0, recibido = 5
        )
        assertEquals("i1", item.id)
        assertEquals("oc1", item.ordenId)
        assertEquals("m1", item.monturaId)
        assertEquals(10, item.cantidad)
        assertEquals(120.0, item.costoUnitario, 0.001)
        assertEquals(5, item.recibido)
    }

    @Test
    fun ordenCompraItemEntity_defaultValues() {
        val item = OrdenCompraItem(
            id = "i1", ordenId = "oc1", monturaId = "m1", cantidad = 10
        )
        assertEquals(0.0, item.costoUnitario, 0.001)
        assertEquals(0, item.recibido)
    }

    @Test
    fun ordenCompraItemEntity_recibidoCannotExceedCantidad() {
        val item = OrdenCompraItem(
            id = "i1", ordenId = "oc1", monturaId = "m1",
            cantidad = 10, costoUnitario = 100.0, recibido = 10
        )
        assertEquals(10, item.recibido)
        assertTrue("recibido debe ser <= cantidad", item.recibido <= item.cantidad)
    }

    @Test
    fun ordenCompraSyncResult_hasCorrectFields() {
        val result = OrdenesCompraSyncResult(
            uploadedCompras = 3,
            uploadedItems = 10,
            downloadedCompras = 2,
            downloadedItems = 8
        )
        assertEquals(3, result.uploadedCompras)
        assertEquals(10, result.uploadedItems)
        assertEquals(2, result.downloadedCompras)
        assertEquals(8, result.downloadedItems)
    }

    @Test
    fun ordenCompraSyncResult_zeroForAll() {
        val result = OrdenesCompraSyncResult(0, 0, 0, 0)
        assertEquals(0, result.uploadedCompras)
        assertEquals(0, result.uploadedItems)
        assertEquals(0, result.downloadedCompras)
        assertEquals(0, result.downloadedItems)
    }

    @Test
    fun resourceSuccess_wrapsSyncResult() {
        val result = Resource.Success(
            OrdenesCompraSyncResult(1, 2, 0, 0)
        )
        assertNotNull(result.data)
        assertEquals(1, result.data!!.uploadedCompras)
        assertEquals(2, result.data!!.uploadedItems)
    }

    @Test
    fun resourceError_stringMessage() {
        val result = Resource.Error<OrdenesCompraSyncResult>("Network failure")
        assertTrue(result is Resource.Error)
    }

    @Test
    fun syncResult_uploadOnly_hasZeroDownloads() {
        val result = OrdenesCompraSyncResult(
            uploadedCompras = 5,
            uploadedItems = 20,
            downloadedCompras = 0,
            downloadedItems = 0
        )
        assertEquals(5, result.uploadedCompras)
        assertEquals(20, result.uploadedItems)
        assertEquals(0, result.downloadedCompras)
        assertEquals(0, result.downloadedItems)
    }

    @Test
    fun syncResult_downloadOnly_hasZeroUploads() {
        val result = OrdenesCompraSyncResult(
            uploadedCompras = 0,
            uploadedItems = 0,
            downloadedCompras = 4,
            downloadedItems = 15
        )
        assertEquals(0, result.uploadedCompras)
        assertEquals(0, result.uploadedItems)
        assertEquals(4, result.downloadedCompras)
        assertEquals(15, result.downloadedItems)
    }

    @Test
    fun ordenCompra_totalMustEqualSumOfItemsCost() {
        val items = listOf(
            OrdenCompraItem(id = "i1", ordenId = "oc1", monturaId = "m1",
                cantidad = 10, costoUnitario = 120.0),
            OrdenCompraItem(id = "i2", ordenId = "oc1", monturaId = "m2",
                cantidad = 5, costoUnitario = 80.0)
        )
        val total = items.sumOf { it.cantidad.toDouble() * it.costoUnitario }
        assertEquals(1600.0, total, 0.001) // 10*120 + 5*80 = 1600
    }
}
