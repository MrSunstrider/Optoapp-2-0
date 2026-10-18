package com.example.optoapp.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReportPdfThemeTest {

    @Test
    fun pageIsA4() {
        assertEquals(595, ReportPdfTheme.PAGE_W)
        assertEquals(842, ReportPdfTheme.PAGE_H)
    }

    @Test
    fun headerBandIsDistinctFromClinicalAccentBar() {
        assertTrue(ReportPdfTheme.HEADER_BAND_H > PdfStyle.ACCENT_BAR_H * 5)
        assertTrue(ReportPdfTheme.COLOR_HEADER_BG != PdfStyle.COLOR_ACCENT)
    }

    @Test
    fun reportColorsAreOpaqueArgb() {
        fun opaque(c: Int) = ((c ushr 24) and 0xFF) == 0xFF
        assertTrue(opaque(ReportPdfTheme.COLOR_HEADER_BG))
        assertTrue(opaque(ReportPdfTheme.COLOR_ACCENT))
        assertTrue(opaque(ReportPdfTheme.COLOR_TEXT))
        assertTrue(opaque(ReportPdfTheme.COLOR_KPI_FILL))
    }

    @Test
    fun marginsLeaveReadableContentWidth() {
        assertTrue(ReportPdfTheme.MARGIN >= 24f)
        assertTrue(ReportPdfTheme.PAGE_W - ReportPdfTheme.MARGIN * 2 > 400f)
        assertTrue(ReportPdfTheme.BOTTOM_SAFE >= 36f)
    }
}
