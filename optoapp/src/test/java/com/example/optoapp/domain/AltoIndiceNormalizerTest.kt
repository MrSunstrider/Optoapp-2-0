package com.example.optoapp.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AltoIndiceNormalizerTest {

    @Test
    fun numericValues_normalizeToTwoDecimalsWithDot() {
        assertEquals("1.50", AltoIndiceNormalizer.normalize("1.5"))
        assertEquals("1.50", AltoIndiceNormalizer.normalize("1,50"))
        assertEquals("1.50", AltoIndiceNormalizer.normalize(" 1.50 "))
        assertEquals("1.67", AltoIndiceNormalizer.normalize("1,67"))
    }

    @Test
    fun numericValues_roundHalfUpToTwoDecimals() {
        assertEquals("1.60", AltoIndiceNormalizer.normalize("1.595"))
    }

    @Test
    fun blankAndNull_normalizeToNull() {
        assertNull(AltoIndiceNormalizer.normalize(null))
        assertNull(AltoIndiceNormalizer.normalize(""))
        assertNull(AltoIndiceNormalizer.normalize("   "))
    }

    @Test
    fun nonNumericValues_areUppercasedWithoutAccents() {
        assertEquals("ALTO INDICE", AltoIndiceNormalizer.normalize("alto índice"))
        assertEquals("ALTO INDICE", AltoIndiceNormalizer.normalize("  Alto Indice "))
    }

    @Test
    fun matches_comparesNormalizedForms() {
        assertTrue(AltoIndiceNormalizer.matches("1.5", "1,50"))
        assertTrue(AltoIndiceNormalizer.matches("alto índice", "ALTO INDICE"))
        assertFalse(AltoIndiceNormalizer.matches("1.67", "1.6"))
    }

    @Test
    fun matches_nullMatchesOnlyNull() {
        assertTrue(AltoIndiceNormalizer.matches(null, null))
        assertTrue(AltoIndiceNormalizer.matches(null, " "))
        assertFalse(AltoIndiceNormalizer.matches(null, "1.50"))
        assertFalse(AltoIndiceNormalizer.matches("1.50", null))
    }
}
