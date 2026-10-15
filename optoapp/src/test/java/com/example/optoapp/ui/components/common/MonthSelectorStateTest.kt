package com.example.optoapp.ui.components.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.YearMonth

class MonthSelectorStateTest {

    private val currentMonth = YearMonth.of(2026, 7)

    @Test
    fun `previous moves one month back`() {
        val state = MonthSelectorState(selected = YearMonth.of(2026, 7), currentMonth = currentMonth)

        assertEquals(YearMonth.of(2026, 6), state.previous().selected)
    }

    @Test
    fun `next moves one month forward before the current month`() {
        val state = MonthSelectorState(selected = YearMonth.of(2026, 5), currentMonth = currentMonth)

        assertTrue(state.canGoNext)
        assertEquals(YearMonth.of(2026, 6), state.next().selected)
    }

    @Test
    fun `next is disabled and a no-op at the current month`() {
        val state = MonthSelectorState(selected = currentMonth, currentMonth = currentMonth)

        assertFalse(state.canGoNext)
        assertEquals(currentMonth, state.next().selected)
    }

    @Test
    fun `a future selection is clamped to the current month`() {
        val state = MonthSelectorState(selected = YearMonth.of(2026, 9), currentMonth = currentMonth)

        assertEquals(currentMonth, state.selected)
        assertFalse(state.canGoNext)
    }

    @Test
    fun `repeated next never emits a month after the current month`() {
        var state = MonthSelectorState(selected = YearMonth.of(2026, 4), currentMonth = currentMonth)

        repeat(6) {
            state = state.next()
            assertFalse(state.selected.isAfter(currentMonth))
        }
        assertEquals(currentMonth, state.selected)
    }
}
