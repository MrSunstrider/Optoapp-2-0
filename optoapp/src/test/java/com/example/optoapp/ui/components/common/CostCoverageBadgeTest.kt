package com.example.optoapp.ui.components.common

import com.example.optoapp.domain.CoberturaCosto
import com.example.optoapp.domain.FuenteDatos
import com.example.optoapp.ui.MainSources
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CostCoverageBadgeTest {

    private val copy = CostCoverageCopy(
        title = "Cobertura de costos",
        sinDatos = "Sin datos",
        offlineNote = "cobertura no disponible sin conexión",
        realTemplate = "%1\$s real",
        estimadoTemplate = "%1\$s estimado",
        faltanteTemplate = "%1\$s faltante",
    )

    @Test
    fun sixRealTwoEstimatedTwoMissing_showsSixtyTwentyTwenty() {
        val model = CostCoverageBadgeModel.from(cobertura(total = 10, reales = 6, estimadas = 2, faltantes = 2), FuenteDatos.SERVIDOR)

        assertEquals(CoverageShares(real = "60 %", estimado = "20 %", faltante = "20 %"), model.shares)
        assertFalse(model.showsSinDatos)
        assertEquals(
            "Cobertura de costos: 60 % real, 20 % estimado, 20 % faltante",
            model.contentDescription(copy),
        )
    }

    @Test
    fun noLines_showsSinDatosWithoutPercentage() {
        val model = CostCoverageBadgeModel.from(CoberturaCosto(), FuenteDatos.SERVIDOR)

        assertTrue(model.showsSinDatos)
        assertNull(model.shares)
        assertEquals("Sin datos", model.visibleLabel(copy))
        assertEquals("Cobertura de costos: Sin datos", model.contentDescription(copy))
    }

    @Test
    fun offline_showsTheSameSinDatosLabelAndAddsTheOfflineNote() {
        val model = CostCoverageBadgeModel.from(cobertura(total = 10, reales = 6, estimadas = 2, faltantes = 2), FuenteDatos.OFFLINE)

        assertTrue(model.showsSinDatos)
        assertNull(model.shares)
        assertEquals("Sin datos", model.visibleLabel(copy))
        assertEquals(
            "Cobertura de costos: Sin datos, cobertura no disponible sin conexión",
            model.contentDescription(copy),
        )
    }

    @Test
    fun shares_roundHalfUpToWholePercent() {
        val model = CostCoverageBadgeModel.from(cobertura(total = 8, reales = 5, estimadas = 2, faltantes = 1), FuenteDatos.SERVIDOR)

        assertEquals(CoverageShares(real = "63 %", estimado = "25 %", faltante = "13 %"), model.shares)
    }

    @Test
    fun coverageCopy_isDefinedInStringsXml() {
        val strings = MainSources.read(MainSources.STRINGS_XML)

        assertTrue(strings.contains("""<string name="cobertura_sin_datos">Sin datos</string>"""))
        assertTrue(strings.contains("""<string name="cobertura_offline_nota">cobertura no disponible sin conexión</string>"""))
        assertTrue(strings.contains("""<string name="cobertura_titulo">Cobertura de costos</string>"""))
    }

    private fun cobertura(total: Int, reales: Int, estimadas: Int, faltantes: Int) = CoberturaCosto(
        lineasTotal = total,
        lineasReales = reales,
        lineasEstimadas = estimadas,
        lineasFaltantes = faltantes,
    )
}
