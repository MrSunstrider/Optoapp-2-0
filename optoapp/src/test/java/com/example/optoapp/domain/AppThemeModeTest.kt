package com.example.optoapp.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppThemeModeTest {

    @Test
    fun parse_defaults_to_system() {
        assertEquals(AppThemeMode.SYSTEM, AppThemeMode.parse(null))
        assertEquals(AppThemeMode.SYSTEM, AppThemeMode.parse(""))
        assertEquals(AppThemeMode.SYSTEM, AppThemeMode.parse("nope"))
    }

    @Test
    fun parse_accepts_light_and_dark() {
        assertEquals(AppThemeMode.LIGHT, AppThemeMode.parse("light"))
        assertEquals(AppThemeMode.DARK, AppThemeMode.parse("DARK"))
    }

    @Test
    fun resolveDark_follows_mode() {
        assertTrue(AppThemeMode.resolveDark(AppThemeMode.SYSTEM, systemDark = true))
        assertFalse(AppThemeMode.resolveDark(AppThemeMode.SYSTEM, systemDark = false))
        assertFalse(AppThemeMode.resolveDark(AppThemeMode.LIGHT, systemDark = true))
        assertTrue(AppThemeMode.resolveDark(AppThemeMode.DARK, systemDark = false))
    }

    @Test
    fun afterToggle_picks_opposite_of_effective_appearance() {
        assertEquals(AppThemeMode.LIGHT, AppThemeMode.afterToggle(AppThemeMode.SYSTEM, systemDark = true))
        assertEquals(AppThemeMode.DARK, AppThemeMode.afterToggle(AppThemeMode.SYSTEM, systemDark = false))
        assertEquals(AppThemeMode.DARK, AppThemeMode.afterToggle(AppThemeMode.LIGHT, systemDark = true))
        assertEquals(AppThemeMode.LIGHT, AppThemeMode.afterToggle(AppThemeMode.DARK, systemDark = false))
    }
}
