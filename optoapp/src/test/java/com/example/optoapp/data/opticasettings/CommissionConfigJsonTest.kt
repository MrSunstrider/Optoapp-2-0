package com.example.optoapp.data.opticasettings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CommissionConfigJsonTest {

    @Test
    fun extract_defaultsToZero() {
        assertEquals(0.0, CommissionConfigJson.extractPct(""), 0.0)
        assertEquals(0.0, CommissionConfigJson.extractPct("{}"), 0.0)
    }

    @Test
    fun merge_preservesSiblingKeys() {
        val merged = CommissionConfigJson.mergePct("""{"business_hours":"9-18"}""", 7.5)
        assertTrue(merged.contains("business_hours"))
        assertEquals(7.5, CommissionConfigJson.extractPct(merged), 0.0)
    }
}
