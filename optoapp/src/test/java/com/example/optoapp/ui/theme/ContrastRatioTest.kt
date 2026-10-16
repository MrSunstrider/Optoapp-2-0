package com.example.optoapp.ui.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ContrastRatioTest {

    @Test
    fun black_on_white_is_the_maximum_ratio() {
        assertEquals(21.0, ContrastRatio.of(Color.Black, Color.White), 0.01)
    }

    @Test
    fun same_color_is_the_minimum_ratio() {
        assertEquals(1.0, ContrastRatio.of(Color(0xFF006D6F), Color(0xFF006D6F)), 0.001)
    }

    @Test
    fun ratio_matches_the_wcag_reference_for_mid_gray() {
        assertEquals(4.48, ContrastRatio.of(Color(0xFF777777), Color.White), 0.01)
    }

    @Test
    fun ratio_is_symmetric() {
        val fg = Color(0xFF1E7E45)
        val bg = Color(0xFFF8FCFB)
        assertEquals(ContrastRatio.of(fg, bg), ContrastRatio.of(bg, fg), 0.0)
    }

    @Test
    fun a_text_token_at_2_9_to_1_is_reported_as_a_violation() {
        val weak = ThemedToken("weak", light = Color(0xFF979797), dark = Color.White, role = ContrastRole.TEXT)
        val ratio = ContrastRatio.of(weak.light, Color.White)
        assertTrue("fixture must sit at ~2.9:1, was $ratio", ratio in 2.85..2.95)

        val violations = ContrastRatio.violations(
            tokens = listOf(weak),
            lightBackgrounds = listOf(Color.White),
            darkBackgrounds = listOf(Color.Black),
        )

        assertEquals(1, violations.size)
        assertTrue(violations.single().startsWith("weak light"))
    }

    @Test
    fun a_graphic_token_needs_only_3_to_1() {
        val chart = ThemedToken("chart", light = Color(0xFF888888), dark = Color(0xFF888888), role = ContrastRole.GRAPHIC)

        val violations = ContrastRatio.violations(
            tokens = listOf(chart),
            lightBackgrounds = listOf(Color.White),
            darkBackgrounds = listOf(Color.Black),
        )

        assertEquals(emptyList<String>(), violations)
        assertTrue(ContrastRatio.of(chart.light, Color.White) < ContrastRole.TEXT.minimum)
    }
}
