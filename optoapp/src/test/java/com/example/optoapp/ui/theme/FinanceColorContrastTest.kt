package com.example.optoapp.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FinanceColorContrastTest {

    @Test
    fun every_finance_token_meets_wcag_aa_against_its_backgrounds_in_light_and_dark() {
        val violations = ContrastRatio.violations(
            tokens = FinanceColorTokens.all,
            lightBackgrounds = FinanceColorTokens.lightBackgrounds,
            darkBackgrounds = FinanceColorTokens.darkBackgrounds,
        )

        assertEquals(emptyList<String>(), violations)
    }

    @Test
    fun finance_tokens_cover_text_and_chart_roles() {
        val names = FinanceColorTokens.all.map { it.name }.toSet()

        assertTrue(names.containsAll(setOf("positiveGreen", "alertRed", "warningGold", "warningAmber", "textDark")))
        assertTrue(FinanceColorTokens.all.any { it.role == ContrastRole.GRAPHIC })
        assertTrue(FinanceColorTokens.all.any { it.role == ContrastRole.TEXT })
    }
}
