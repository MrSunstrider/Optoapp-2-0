package com.example.optoapp.util.refraction

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SphereTest {

    @Test
    fun `sphere accepts positive integer`() {
        assertTrue(Sphere("+2").isValid)
        assertTrue(Sphere("2").isValid)
        assertTrue(Sphere("+0").isValid)
    }

    @Test
    fun `sphere accepts negative integer`() {
        assertTrue(Sphere("-3").isValid)
        assertTrue(Sphere("-0").isValid)
    }

    @Test
    fun `sphere accepts decimal with one or two digits`() {
        assertTrue(Sphere("+1.25").isValid)
        assertTrue(Sphere("-2.5").isValid)
        assertTrue(Sphere("0.75").isValid)
    }

    @Test
    fun `sphere rejects letters and symbols`() {
        assertFalse(Sphere("abc").isValid)
        assertFalse(Sphere("+1.5x").isValid)
        assertFalse(Sphere("2.5°").isValid)
        assertFalse(Sphere("x2.0").isValid)
    }

    @Test
    fun `sphere rejects three decimal digits`() {
        assertFalse(Sphere("+1.255").isValid)
        assertFalse(Sphere("-0.123").isValid)
    }

    @Test
    fun `sphere blank is valid`() {
        assertTrue(Sphere("").isValid)
        assertTrue(Sphere("   ").isValid)
    }

    @Test
    fun `sphere toDisplayString returns value or empty`() {
        assertEquals("+2.50", Sphere("+2.50").toDisplayString())
        assertEquals("", Sphere("").toDisplayString())
        assertEquals("", Sphere("   ").toDisplayString())
    }

    @Test
    fun `sphere fromString trims input`() {
        assertEquals("+2.00", Sphere.fromString("  +2.00  ").value)
    }

    @Test
    fun `cylinder accepts valid values same as sphere`() {
        assertTrue(Cylinder("-1.75").isValid)
        assertTrue(Cylinder("+0.50").isValid)
        assertTrue(Cylinder("3").isValid)
    }

    @Test
    fun `cylinder rejects invalid input`() {
        assertFalse(Cylinder("1.5°").isValid)
        assertFalse(Cylinder("x-2.00").isValid)
    }

    @Test
    fun `cylinder blank is valid`() {
        assertTrue(Cylinder("").isValid)
    }

    @Test
    fun `cylinder toDisplayString and fromString`() {
        assertEquals("-1.75", Cylinder("-1.75").toDisplayString())
        assertEquals("", Cylinder("").toDisplayString())
        assertEquals("-1.00", Cylinder.fromString(" -1.00 ").value)
    }

    @Test
    fun `axis accepts one to three digits`() {
        assertTrue(Axis("0").isValid)
        assertTrue(Axis("90").isValid)
        assertTrue(Axis("180").isValid)
    }

    @Test
    fun `axis rejects four or more digits`() {
        assertFalse(Axis("1000").isValid)
        assertFalse(Axis("1800").isValid)
    }

    @Test
    fun `axis rejects letters and symbols`() {
        assertFalse(Axis("90°").isValid)
        assertFalse(Axis("x90").isValid)
        assertFalse(Axis("abc").isValid)
    }

    @Test
    fun `axis blank is valid`() {
        assertTrue(Axis("").isValid)
    }

    @Test
    fun `axis toDisplayString and fromString`() {
        assertEquals("90", Axis("90").toDisplayString())
        assertEquals("", Axis("").toDisplayString())
        assertEquals("180", Axis.fromString(" 180 ").value)
    }

    @Test
    fun `visual acuity accepts 20-20 notation`() {
        assertTrue(VisualAcuity("20/20").isValid)
        assertTrue(VisualAcuity("20/40").isValid)
        assertTrue(VisualAcuity("20/30").isValid)
    }

    @Test
    fun `visual acuity accepts fractional with decimal numerator`() {
        assertTrue(VisualAcuity("20/20").isValid)
        assertTrue(VisualAcuity("6/6").isValid)
        assertTrue(VisualAcuity("0.5").isValid)
    }

    @Test
    fun `visual acuity accepts decimal notation`() {
        assertTrue(VisualAcuity("1.0").isValid)
        assertTrue(VisualAcuity("0.5").isValid)
        assertTrue(VisualAcuity("0.33").isValid)
    }

    @Test
    fun `visual acuity accepts raw number`() {
        assertTrue(VisualAcuity("100").isValid)
        assertTrue(VisualAcuity("50").isValid)
    }

    @Test
    fun `visual acuity rejects invalid formats`() {
        assertFalse(VisualAcuity("20/20/20").isValid)
        assertFalse(VisualAcuity("abc").isValid)
        assertFalse(VisualAcuity("20-20").isValid)
    }

    @Test
    fun `visual acuity blank is valid`() {
        assertTrue(VisualAcuity("").isValid)
    }

    @Test
    fun `visual acuity toDisplayString and fromString`() {
        assertEquals("20/20", VisualAcuity("20/20").toDisplayString())
        assertEquals("", VisualAcuity("").toDisplayString())
        assertEquals("0.5", VisualAcuity.fromString(" 0.5 ").value)
    }

    @Test
    fun `prism toDisplayString and fromString`() {
        assertEquals("2Δ", Prism("2Δ").toDisplayString())
        assertEquals("", Prism("").toDisplayString())
        assertEquals("1.5", Prism.fromString(" 1.5 ").value)
    }

    @Test
    fun `dip measurement toDisplayString and fromString`() {
        assertEquals("32", DipMeasurement("32").toDisplayString())
        assertEquals("", DipMeasurement("").toDisplayString())
        assertEquals("30.5", DipMeasurement.fromString(" 30.5 ").value)
    }
}
