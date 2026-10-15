package com.example.optoapp.ui.components.common

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Warning
import com.example.optoapp.R
import com.example.optoapp.ui.MainSources
import com.example.optoapp.ui.theme.OptoTokens
import com.example.optoapp.ui.theme.SemanticPalette
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MetricTileTest {

    @Test
    fun delta_isShownAndReadInTheContentDescription() {
        val content = MetricTileContent(label = "Margen neto", value = "20.0 %", delta = "+2.0 pp", status = MetricStatus.BUENO)

        assertEquals("+2.0 pp", content.visibleDelta)
        assertEquals("Margen neto, 20.0 %, +2.0 pp, Bueno", content.contentDescription(statusLabel = "Bueno"))
    }

    @Test
    fun noDelta_hidesTheDeltaTextAndOmitsItFromTheDescription() {
        val content = MetricTileContent(label = "Ventas", value = "S/ 1,200.00", delta = null, status = null)

        assertNull(content.visibleDelta)
        assertEquals("Ventas, S/ 1,200.00", content.contentDescription(statusLabel = null))
    }

    @Test
    fun blankDelta_isTreatedAsNoDelta() {
        val content = MetricTileContent(label = "Ventas", value = "S/ 0.00", delta = "  ", status = null)

        assertNull(content.visibleDelta)
        assertFalse(content.contentDescription(statusLabel = null).contains(", ,"))
    }

    @Test
    fun critico_showsIconAndWord_inLightAndDarkThemes() {
        listOf(false to OptoTokens.semantic.alertRedLight, true to OptoTokens.semantic.alertRedDark).forEach { (dark, expectedColor) ->
            val style = MetricStatusStyle.of(MetricStatus.CRITICO, SemanticPalette.forDarkTheme(dark))

            assertEquals(Icons.Filled.Error, style.icon)
            assertEquals(R.string.metric_estado_critico, style.labelRes)
            assertEquals(expectedColor, style.color)
        }
    }

    @Test
    fun everyStatus_hasItsOwnIconAndWord() {
        val palette = SemanticPalette.forDarkTheme(false)

        assertEquals(Icons.Filled.CheckCircle, MetricStatusStyle.of(MetricStatus.BUENO, palette).icon)
        assertEquals(Icons.Filled.Warning, MetricStatusStyle.of(MetricStatus.ATENCION, palette).icon)
        assertEquals(R.string.metric_estado_bueno, MetricStatusStyle.of(MetricStatus.BUENO, palette).labelRes)
        assertEquals(R.string.metric_estado_atencion, MetricStatusStyle.of(MetricStatus.ATENCION, palette).labelRes)
    }

    @Test
    fun statusWords_areDefinedInStringsXml() {
        val strings = MainSources.read(MainSources.STRINGS_XML)

        assertTrue(strings.contains("""<string name="metric_estado_bueno">Bueno</string>"""))
        assertTrue(strings.contains("""<string name="metric_estado_atencion">Atención</string>"""))
        assertTrue(strings.contains("""<string name="metric_estado_critico">Crítico</string>"""))
    }
}
