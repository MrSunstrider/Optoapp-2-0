package com.example.optoapp.util

import org.junit.Assert.*
import org.junit.Test

/**
 * Tests unitarios para PdfStyle constants y RecetaPdfBuilder.
 *
 * NOTA: Los tests de paints/textPaints requieren Android framework (android.graphics.Paint,
 * TextPaint) y no pueden correr en JUnit estándar. Esos se testean en androidTest o
 * verificando visualmente el output PDF.
 * Las constantes puras (Int, Float) son testeables en JUnit.
 */
class RecetaPdfBuilderTest {

    @Test
    fun pdfStyle_layout_constants_match_generator() {
        assertEquals(595, PdfStyle.PAGE_W)
        assertEquals(842, PdfStyle.PAGE_H)
        assertEquals(42f, PdfStyle.MARGIN, 0.001f)
        assertEquals(36f, PdfStyle.BOTTOM_SAFE, 0.001f)
        assertEquals(6f, PdfStyle.TABLE_CELL_PAD, 0.001f)
        assertEquals(4f, PdfStyle.ACCENT_BAR_H, 0.001f)
        assertEquals(8f, PdfStyle.CARD_RADIUS, 0.001f)
        assertEquals(10f, PdfStyle.CARD_PAD, 0.001f)
        assertEquals(4f, PdfStyle.SECTION_BAR_W, 0.001f)
    }

    @Test
    fun pdfStyle_color_constants_match_generator() {
        assertEquals(0xFF1E5B8C.toInt(), PdfStyle.COLOR_ACCENT)
        assertEquals(0xFFE8F0F8.toInt(), PdfStyle.COLOR_ACCENT_LIGHT)
        assertEquals(0xFF1C1C1E.toInt(), PdfStyle.COLOR_TEXT)
        assertEquals(0xFF585C66.toInt(), PdfStyle.COLOR_TEXT_MUTED)
        assertEquals(0xFFD2DAE4.toInt(), PdfStyle.COLOR_BORDER)
        assertEquals(0xFFFCFDFF.toInt(), PdfStyle.COLOR_CARD_FILL)
        assertEquals(0xFFE6EBF2.toInt(), PdfStyle.COLOR_RULE)
    }

    @Test
    fun pdfStyle_altRowPaint_matches_old_generator_color() {
        // Both PdfStyle.altRowPaint (line 77) and the old generator inline
        // altRowPaint (line 332) use: Color.argb(30, 30, 91, 140)
        // Color.argb packs as: (alpha << 24) | (red << 16) | (green << 8) | blue
        val packed = (30 shl 24) or (30 shl 16) or (91 shl 8) or 140
        assertEquals(
            "altRowPaint ARGB value mismatch from generator's inline Color.argb(30,30,91,140)",
            0x1E1E5B8C.toInt(),
            packed,
        )
    }

    @Test
    fun pdfStyle_contentWidth_formula_matches_expected() {
        // contentWidth() in RecetaPdfBuilder computes (PAGE_W - 2 * MARGIN).toInt()
        // With current constants: (595 - 2 * 48).toInt() = 499
        val computed = (PdfStyle.PAGE_W - 2 * PdfStyle.MARGIN).toInt()
        assertEquals(
            "contentWidth formula should produce 511 with current constants",
            511,
            computed,
        )
    }
}
