package com.example.optoapp.domain

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * RED test: CostoProductoRemoto and CostoBiseladoRemoto DTO serialization,
 * download order, and upload vs download-only semantics.
 *
 * Plain JUnit test — no Robolectric needed since SyncFinanzasDto is pure domain.
 */
class SyncFinanzasCostosTest {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    // ── CostoProductoRemoto serialization ──────────────────────────────

    @Test
    fun costoProductoRemoto_serialization_roundTrip() {
        val original = CostoProductoRemoto(
            id = "cp-1",
            opticaId = "optica1",
            material = "Resina",
            tipoLente = "Monofocal",
            stockOFabricacion = "stock",
            tratamiento = "Antireflex",
            serie = 2,
            costoUnitario = 18.0,
            laboratorioId = "lab1",
            vigenteDesde = "2026-07-01",
            vigenteHasta = null,
        )

        val encoded = json.encodeToString(original)
        val decoded = json.decodeFromString<CostoProductoRemoto>(encoded)

        assertEquals(original.id, decoded.id)
        assertEquals(original.opticaId, decoded.opticaId)
        assertEquals(original.material, decoded.material)
        assertEquals(original.tipoLente, decoded.tipoLente)
        assertEquals(original.stockOFabricacion, decoded.stockOFabricacion)
        assertEquals(original.tratamiento, decoded.tratamiento)
        assertEquals(original.serie, decoded.serie)
        assertEquals(original.costoUnitario, decoded.costoUnitario, 0.001)
        assertEquals(original.laboratorioId, decoded.laboratorioId)
        assertEquals(original.vigenteDesde, decoded.vigenteDesde)
        assertNull(decoded.vigenteHasta)
    }

    @Test
    fun costoProductoRemoto_serialName_mapsSnakeCase() {
        val jsonStr = """
            {
                "id": "cp-2",
                "optica_id": "optica1",
                "material": "Cristal",
                "tipo_lente": "Bifocal",
                "stock_o_fabricacion": "fabricacion",
                "tratamiento": "Simple",
                "serie": null,
                "costo_unitario": 20.0,
                "laboratorio_id": null,
                "vigente_desde": "2026-07-01",
                "vigente_hasta": null
            }
        """.trimIndent()

        val decoded = json.decodeFromString<CostoProductoRemoto>(jsonStr)

        assertEquals("Cristal", decoded.material)
        assertEquals("Bifocal", decoded.tipoLente)
        assertEquals("fabricacion", decoded.stockOFabricacion)
        assertEquals(20.0, decoded.costoUnitario, 0.001)
    }

    @Test
    fun costoProductoRemoto_roundTrip_preservesNullSerie() {
        // Fabricacion items have serie=null
        val original = CostoProductoRemoto(
            id = "cp-fab",
            opticaId = "optica1",
            material = "Resina",
            tipoLente = "Bifocal FT",
            stockOFabricacion = "fabricacion",
            tratamiento = "Simple",
            serie = null,
            costoUnitario = 20.0,
            vigenteDesde = "2026-07-01",
        )

        val encoded = json.encodeToString(original)
        val decoded = json.decodeFromString<CostoProductoRemoto>(encoded)

        assertNull(decoded.serie)
    }

    @Test
    fun costoProductoRemoto_roundTrip_preservesNullVigenteHasta() {
        val original = CostoProductoRemoto(
            id = "cp-3", opticaId = "optica1",
            material = "Resina", tipoLente = "Monofocal",
            stockOFabricacion = "stock", tratamiento = "AR", serie = 1,
            costoUnitario = 5.0, vigenteDesde = "2026-01-01", vigenteHasta = null,
        )

        val encoded = json.encodeToString(original)
        val decoded = json.decodeFromString<CostoProductoRemoto>(encoded)

        assertNull(decoded.vigenteHasta)
    }

    // ── CostoBiseladoRemoto serialization ──────────────────────────────

    @Test
    fun costoBiseladoRemoto_serialization_roundTrip() {
        val original = CostoBiseladoRemoto(
            id = "cb-1",
            opticaId = "optica1",
            material = "Resina",
            tipoAro = "aro_completo",
            stockOFabricacion = "stock",
            serie = 2,
            altoIndice = "1.50",
            costoPorPar = 15.0,
            proveedor = "Lab1",
            vigenteDesde = "2026-07-01",
            vigenteHasta = null,
        )

        val encoded = json.encodeToString(original)
        val decoded = json.decodeFromString<CostoBiseladoRemoto>(encoded)

        assertEquals(original.id, decoded.id)
        assertEquals(original.material, decoded.material)
        assertEquals(original.tipoAro, decoded.tipoAro)
        assertEquals(original.stockOFabricacion, decoded.stockOFabricacion)
        assertEquals(original.serie, decoded.serie)
        assertEquals(original.altoIndice, decoded.altoIndice)
        assertEquals(original.costoPorPar, decoded.costoPorPar, 0.001)
        assertEquals(original.proveedor, decoded.proveedor)
    }

    @Test
    fun costoBiseladoRemoto_serialName_mapsSnakeCase() {
        val jsonStr = """
            {
                "id": "cb-2",
                "optica_id": "optica1",
                "material": "Cristal",
                "tipo_aro": "ranurado",
                "stock_o_fabricacion": "fabricacion",
                "serie": null,
                "alto_indice": "1.67",
                "costo_por_par": 35.0,
                "proveedor": null,
                "vigente_desde": "2026-07-01",
                "vigente_hasta": null
            }
        """.trimIndent()

        val decoded = json.decodeFromString<CostoBiseladoRemoto>(jsonStr)

        assertEquals("Cristal", decoded.material)
        assertEquals("ranurado", decoded.tipoAro)
        assertEquals("fabricacion", decoded.stockOFabricacion)
        assertEquals(35.0, decoded.costoPorPar, 0.001)
    }

    // ── FinanzasSyncResult new fields ─────────────────────────────────

    @Test
    fun finanzasSyncResult_includesCostosCounters() {
        val result = FinanzasSyncResult(
            uploadedDispensaciones = 5,
            uploadedServicios = 3,
            uploadedPagos = 10,
            downloadedDispensaciones = 2,
            downloadedServicios = 1,
            downloadedPagos = 4,
            uploadedCostosProductos = 7,
            downloadedCostosProductos = 12,
            downloadedCostosBiselado = 8,
        )

        assertEquals(7, result.uploadedCostosProductos)
        assertEquals(12, result.downloadedCostosProductos)
        assertEquals(8, result.downloadedCostosBiselado)
    }

    @Test
    fun finanzasSyncResult_costosCounters_defaultToZero() {
        val result = FinanzasSyncResult(
            uploadedDispensaciones = 0,
            uploadedServicios = 0,
            uploadedPagos = 0,
            downloadedDispensaciones = 0,
            downloadedServicios = 0,
            downloadedPagos = 0,
        )

        assertEquals(0, result.uploadedCostosProductos)
        assertEquals(0, result.downloadedCostosProductos)
        assertEquals(0, result.downloadedCostosBiselado)
    }
}
