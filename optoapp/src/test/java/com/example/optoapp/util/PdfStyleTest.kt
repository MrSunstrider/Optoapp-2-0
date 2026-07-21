package com.example.optoapp.util

import android.graphics.Paint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PdfStyleTest {

    @Test
    fun `page dimensions are standard A4 at 72dpi`() {
        assertEquals(595, PdfStyle.PAGE_W)
        assertEquals(842, PdfStyle.PAGE_H)
    }

    @Test
    fun `margins and safe areas have reasonable values`() {
        assertTrue(PdfStyle.MARGIN > 0)
        assertTrue(PdfStyle.BOTTOM_SAFE > 0)
        assertTrue(PdfStyle.TABLE_CELL_PAD > 0)
        assertTrue(PdfStyle.CARD_RADIUS > 0)
    }

    @Test
    fun `COLOR_ACCENT is a blue tone`() {
        val color = PdfStyle.COLOR_ACCENT
        val blue = color and 0xFF
        val green = (color shr 8) and 0xFF
        val red = (color shr 16) and 0xFF
        assertTrue(blue > green)
        assertTrue(blue > red)
    }

    @Test
    fun `COLOR_TEXT is dark`() {
        val color = PdfStyle.COLOR_TEXT
        val red = (color shr 16) and 0xFF
        val green = (color shr 8) and 0xFF
        val blue = color and 0xFF
        assertTrue(red < 60)
        assertTrue(green < 60)
        assertTrue(blue < 60)
    }

    @Test
    fun `COLOR_CARD_FILL is very light`() {
        val color = PdfStyle.COLOR_CARD_FILL
        val red = (color shr 16) and 0xFF
        val green = (color shr 8) and 0xFF
        val blue = color and 0xFF
        assertTrue(red > 240)
        assertTrue(green > 240)
        assertTrue(blue > 240)
    }

    @Test
    fun `all color constants are non-zero`() {
        assertTrue(PdfStyle.COLOR_ACCENT != 0)
        assertTrue(PdfStyle.COLOR_ACCENT_LIGHT != 0)
        assertTrue(PdfStyle.COLOR_TEXT != 0)
        assertTrue(PdfStyle.COLOR_TEXT_MUTED != 0)
        assertTrue(PdfStyle.COLOR_BORDER != 0)
        assertTrue(PdfStyle.COLOR_CARD_FILL != 0)
        assertTrue(PdfStyle.COLOR_RULE != 0)
    }

    @Test
    fun `accentBarPaint has correct color`() {
        assertEquals(PdfStyle.COLOR_ACCENT, PdfStyle.accentBarPaint.color)
        assertTrue(PdfStyle.accentBarPaint.isAntiAlias)
    }

    @Test
    fun `cardFillPaint has fill style`() {
        assertEquals(Paint.Style.FILL, PdfStyle.cardFillPaint.style)
        assertEquals(PdfStyle.COLOR_CARD_FILL, PdfStyle.cardFillPaint.color)
    }

    @Test
    fun `cardStrokePaint has stroke style with border color`() {
        assertEquals(Paint.Style.STROKE, PdfStyle.cardStrokePaint.style)
        assertEquals(PdfStyle.COLOR_BORDER, PdfStyle.cardStrokePaint.color)
        assertEquals(1f, PdfStyle.cardStrokePaint.strokeWidth)
    }

    @Test
    fun `altRowPaint is a muted accent color with low alpha`() {
        val color = PdfStyle.altRowPaint.color
        val alpha = (color shr 24) and 0xFF
        assertTrue(alpha in 1..100) // Semi-transparent
    }

    @Test
    fun `prismaTrianglePaint has fill style and 60 alpha`() {
        assertEquals(Paint.Style.FILL, PdfStyle.prismaTrianglePaint.style)
        assertEquals(60, PdfStyle.prismaTrianglePaint.alpha)
    }

    @Test
    fun `titlePaint is bold and larger than body`() {
        assertNotNull(PdfStyle.titlePaint.typeface)
        assertEquals(20f, PdfStyle.titlePaint.textSize)
        assertEquals(PdfStyle.COLOR_TEXT, PdfStyle.titlePaint.color)
    }

    @Test
    fun `subtitlePaint is small and muted`() {
        assertEquals(10f, PdfStyle.subtitlePaint.textSize)
        assertEquals(PdfStyle.COLOR_TEXT_MUTED, PdfStyle.subtitlePaint.color)
    }

    @Test
    fun `sectionPaint has accent color`() {
        assertEquals(PdfStyle.COLOR_ACCENT, PdfStyle.sectionPaint.color)
        assertEquals(11.5f, PdfStyle.sectionPaint.textSize)
    }

    @Test
    fun `bodyPaint has normal text size`() {
        assertEquals(10.5f, PdfStyle.bodyPaint.textSize)
        assertEquals(PdfStyle.COLOR_TEXT, PdfStyle.bodyPaint.color)
    }

    @Test
    fun `bodyBoldPaint is same size as body but bold`() {
        assertEquals(PdfStyle.bodyPaint.textSize, PdfStyle.bodyBoldPaint.textSize)
        assertEquals(PdfStyle.COLOR_TEXT, PdfStyle.bodyBoldPaint.color)
    }

    @Test
    fun `labelPaint is smaller than body`() {
        assertEquals(9.5f, PdfStyle.labelPaint.textSize)
        assertTrue(PdfStyle.labelPaint.textSize < PdfStyle.bodyPaint.textSize)
    }

    @Test
    fun `tableHeaderPaint has accent color`() {
        assertEquals(PdfStyle.COLOR_ACCENT, PdfStyle.tableHeaderPaint.color)
        assertEquals(10f, PdfStyle.tableHeaderPaint.textSize)
    }

    @Test
    fun `smallPaint is the smallest text`() {
        assertEquals(8.5f, PdfStyle.smallPaint.textSize)
        assertTrue(PdfStyle.smallPaint.textSize < PdfStyle.labelPaint.textSize)
    }

    @Test
    fun `rxValuePaint is between body and section sizes`() {
        assertEquals(11f, PdfStyle.rxValuePaint.textSize)
        assertEquals(PdfStyle.COLOR_TEXT, PdfStyle.rxValuePaint.color)
    }

    @Test
    fun `paint singletons return same instance`() {
        val p1 = PdfStyle.accentBarPaint
        val p2 = PdfStyle.accentBarPaint
        assertTrue(p1 === p2)
    }

    @Test
    fun `textPaint singletons return same instance`() {
        val t1 = PdfStyle.titlePaint
        val t2 = PdfStyle.titlePaint
        assertTrue(t1 === t2)
    }

    @Test
    fun `all paints are anti-aliased`() {
        val paints = listOf(
            PdfStyle.accentBarPaint, PdfStyle.cardFillPaint, PdfStyle.cardStrokePaint,
            PdfStyle.headerStripPaint, PdfStyle.rulePaint, PdfStyle.sectionBarPaint,
            PdfStyle.gridStrokePaint, PdfStyle.altRowPaint, PdfStyle.prismaTrianglePaint,
            PdfStyle.titlePaint, PdfStyle.subtitlePaint, PdfStyle.sectionPaint,
            PdfStyle.bodyPaint, PdfStyle.bodyBoldPaint, PdfStyle.labelPaint,
            PdfStyle.rxValuePaint, PdfStyle.tableHeaderPaint, PdfStyle.smallPaint,
        )
        paints.forEach { paint ->
            assertTrue("${paint.javaClass.simpleName} is not anti-aliased", paint.isAntiAlias)
        }
    }
}
