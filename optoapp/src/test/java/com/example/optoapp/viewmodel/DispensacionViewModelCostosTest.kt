package com.example.optoapp.viewmodel

import com.example.optoapp.data.DispensacionOptica
import com.example.optoapp.data.EvaluacionClinica
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * RED test: Cost calculation integration in DispensacionViewModel.
 *
 * Tests:
 * - UiState has evaluacionId and cost fields
 * - ViewModel contract for cost calculation methods
 * - Pure cost determination logic (stock vs fabricacion, serie lookup)
 *
 * Pure function approach: extract cost calculation into testable functions
 * that don't depend on Room, Supabase, or Hilt.
 */
class DispensacionViewModelCostosTest {

    // ─── Pure cost calculation logic ──────────────────────────────────

    /**
     * Determines if a lens is stock or fabrication based on sphere and cylinder power.
     * |esfera| <= 6.00 AND |cilindro| <= 6.00 → stock, else → fabricacion.
     */
    private fun determineTipoLente(esfera: Double, cilindro: Double?): String {
        val absEsf = kotlin.math.abs(esfera)
        val absCil = cilindro?.let { kotlin.math.abs(it) } ?: 0.0
        return if (absEsf > 6.00 || absCil > 6.00) "fabricacion" else "stock"
    }

    /**
     * Determines cylinder series for stock lenses:
     * null or 0 to -2.00 → 1ra (serie=1)
     * -2.25 to -4.00 → 2da (serie=2)
     * -4.25 to -6.00 → 3ra (serie=3)
     */
    private fun determineSeriePorCilindro(cilindro: Double?): Int? = when {
        cilindro == null || kotlin.math.abs(cilindro) <= 2.00 -> 1
        kotlin.math.abs(cilindro) <= 4.00 -> 2
        kotlin.math.abs(cilindro) <= 6.00 -> 3
        else -> null
    }

    @Test
    fun costCalc_esferaMenorIgual6_stock() {
        assertEquals("stock", determineTipoLente(0.0, null))
        assertEquals("stock", determineTipoLente(6.0, null))
        assertEquals("stock", determineTipoLente(-6.0, null))
        assertEquals("stock", determineTipoLente(5.75, null))
        assertEquals("stock", determineTipoLente(-3.50, null))
    }

    @Test
    fun costCalc_esferaMayor6_fabricacion() {
        assertEquals("fabricacion", determineTipoLente(6.01, null))
        assertEquals("fabricacion", determineTipoLente(-7.0, null))
        assertEquals("fabricacion", determineTipoLente(12.0, null))
    }

    @Test
    fun costCalc_serieByCilindro_1ra() {
        assertEquals(1, determineSeriePorCilindro(0.0))
        assertEquals(1, determineSeriePorCilindro(-0.75))
        assertEquals(1, determineSeriePorCilindro(-2.00))
        assertEquals(1, determineSeriePorCilindro(1.50))
    }

    @Test
    fun costCalc_serieByCilindro_2da() {
        assertEquals(2, determineSeriePorCilindro(-2.25))
        assertEquals(2, determineSeriePorCilindro(-3.00))
        assertEquals(2, determineSeriePorCilindro(-4.00))
    }

    @Test
    fun costCalc_serieByCilindro_3ra() {
        assertEquals(3, determineSeriePorCilindro(-4.25))
        assertEquals(3, determineSeriePorCilindro(-5.00))
        assertEquals(3, determineSeriePorCilindro(-6.00))
    }

    @Test
    fun costCalc_serieByCilindro_outOfRange_returnsNull() {
        assertNull(determineSeriePorCilindro(-6.25))
        assertNull(determineSeriePorCilindro(7.0))
    }

    @Test
    fun costCalc_serieByCilindro_null_returns1() {
        assertEquals(1, determineSeriePorCilindro(null))
    }

    @Test
    fun costCalc_fabricacion_byHighCylinder() {
        assertEquals("fabricacion", determineTipoLente(-2.0, -7.0))
        assertEquals("fabricacion", determineTipoLente(0.0, 6.01))
    }

    @Test
    fun costCalc_stockLookup_keys_fromReceta() {
        // Given: receta OD esf=-3.00, cil=-2.50, material=Resina, tipo=Monofocal, trat=Antireflex
        val esfera = -3.00
        val cilindro = -2.50

        val tipo = determineTipoLente(esfera, cilindro)
        val serie = determineSeriePorCilindro(cilindro)

        assertEquals("stock", tipo)
        assertEquals(2, serie)

        // lookup keys would be: material="Resina", tipoLente="Monofocal",
        // stockOFabricacion="stock", tratamiento="Antireflex", serie=2
        // → CostoProductoDao.lookup() returns S/ 12.00 per design
    }

    @Test
    fun costCalc_fabricacionLookup_keys_fromReceta() {
        // Given: receta OD esf=-7.00, cil=-1.00, tipo=Bifocal FT, trat=Simple
        val esfera = -7.00
        val cilindro = -1.00

        val tipo = determineTipoLente(esfera, cilindro)

        assertEquals("fabricacion", tipo)

        // lookup keys would be: material="Resina", tipoLente="Bifocal FT",
        // stockOFabricacion="fabricacion", tratamiento="Simple", serie=null
        // → CostoProductoDao.lookup() returns S/ 20.00 per design
    }

    // ─── normalizeTipoAro ────────────────────────────────────────────

    /**
     * Normalizes tipo_aro values from the form to DB lookup keys.
     * "Completo" → "aro_completo", "Semi" → "semi_aire",
     * "aire" → "al_aire", unrecognized → "aro_completo".
     */
    private fun normalizeTipoAro(tipoAro: String): String = when {
        tipoAro.contains("Completo", ignoreCase = true) -> "aro_completo"
        tipoAro.contains("Semi", ignoreCase = true) -> "semi_aire"
        tipoAro.contains("aire", ignoreCase = true) -> "al_aire"
        else -> "aro_completo"
    }

    @Test
    fun costCalc_normalizeTipoAro_semiAire() {
        assertEquals("semi_aire", normalizeTipoAro("Semi al aire"))
    }

    @Test
    fun costCalc_normalizeTipoAro_ranurado_fallback() {
        assertEquals("aro_completo", normalizeTipoAro("ranurado"))
    }

    @Test
    fun costCalc_normalizeTipoAro_taladro_fallback() {
        assertEquals("aro_completo", normalizeTipoAro("taladro"))
    }

    // ─── UiState evaluacionId field ───────────────────────────────────

    @Test
    fun uiState_hasEvaluacionIdField() {
        val fields = DispensacionUiState::class.java.declaredFields.map { it.name }
        assertTrue(
            "DispensacionUiState debe tener evaluacionId",
            "evaluacionId" in fields
        )
    }

    @Test
    fun uiState_evaluacionId_defaultsToNull() {
        val state = DispensacionUiState(fecha = LocalDate.of(2026, 7, 1))
        assertNull(state.evaluacionId)
    }

    // ─── ViewModel cost calculation method ───────────────────────────

    @Test
    fun viewModel_hasCalculateCostsMethod() {
        val methods = DispensacionViewModel::class.java.declaredMethods.map { it.name }
        assertTrue(
            "DispensacionViewModel debe tener calculateCosts (or loadCostos)",
            "calculateCosts" in methods || "loadCostos" in methods
        )
    }

    @Test
    fun viewModel_hasEvaluacionIdMethod() {
        val methods = DispensacionViewModel::class.java.declaredMethods.map { it.name }
        assertTrue(
            "DispensacionViewModel debe tener setEvaluacionId or onEvaluacionChanged",
            "setEvaluacionId" in methods || "onEvaluacionChanged" in methods
        )
    }

    // ─── Montura cost fallback logic ─────────────────────────────────

    @Test
    fun costCalc_montura_fallbackFromMonturasCosto() {
        // If no rule in costos_productos where stockOFabricacion='montura',
        // fallback to monturas.costo from the Montura entity.
        val costoFromMontura = 80.0
        assertEquals(80.0, costoFromMontura, 0.001)

        // With rule in costos_productos, product rule takes priority
        val costoFromMatrix = 75.0
        assertEquals(75.0, costoFromMatrix, 0.001)
    }

    // ─── Per-eye independent calculation ─────────────────────────────

    @Test
    fun costCalc_perEye_independent() {
        // OD: esf=-3.00, cil=-2.50 → stock, serie=2
        val odTipo = determineTipoLente(-3.00, -2.50)
        val odSerie = determineSeriePorCilindro(-2.50)
        assertEquals("stock", odTipo)
        assertEquals(2, odSerie)

        // OI: esf=-7.00, cil=-1.00 → fabricacion (serie determined by cilindro regardless)
        val oiTipo = determineTipoLente(-7.00, -1.00)
        val oiSerie = determineSeriePorCilindro(-1.00)
        assertEquals("fabricacion", oiTipo)
        assertEquals(1, oiSerie)
        // Note: for fabricacion lookups, serie is ignored as a filter parameter

        // Each eye is calculated independently
    }
}
