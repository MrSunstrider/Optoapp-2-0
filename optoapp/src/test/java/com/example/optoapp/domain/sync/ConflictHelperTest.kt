package com.example.optoapp.domain.sync

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Tests para [ConflictHelper.isLocalNewerOrEqual]. */
@RunWith(RobolectricTestRunner::class)
class ConflictHelperTest {

    // ── isLocalNewerOrEqual ───────────────────────────────────────────────

    @Test
    fun `same instant with Z and +0000 are equal`() {
        assertTrue(
            ConflictHelper.isLocalNewerOrEqual(
                "2026-06-15T04:33:25Z",
                "2026-06-15T04:33:25+00:00"
            )
        )
    }

    @Test
    fun `same instant with and without millis are equal`() {
        assertTrue(
            ConflictHelper.isLocalNewerOrEqual(
                "2026-06-15T04:33:25.000Z",
                "2026-06-15T04:33:25Z"
            )
        )
    }

    @Test
    fun `same instant with micros and Z are equal`() {
        assertTrue(
            ConflictHelper.isLocalNewerOrEqual(
                "2026-06-15T04:33:25.123456Z",
                "2026-06-15T04:33:25.123Z"
            )
        )
    }

    @Test
    fun `local newer returns true`() {
        assertTrue(
            ConflictHelper.isLocalNewerOrEqual(
                "2026-06-15T05:00:00Z",
                "2026-06-15T04:00:00Z"
            )
        )
    }

    @Test
    fun `local older returns false`() {
        assertFalse(
            ConflictHelper.isLocalNewerOrEqual(
                "2026-06-15T04:00:00Z",
                "2026-06-15T05:00:00Z"
            )
        )
    }

    @Test
    fun `exact same string returns true`() {
        assertTrue(
            ConflictHelper.isLocalNewerOrEqual(
                "2026-06-15T04:33:25Z",
                "2026-06-15T04:33:25Z"
            )
        )
    }

    @Test
    fun `different days local newer`() {
        assertTrue(
            ConflictHelper.isLocalNewerOrEqual(
                "2026-06-16T00:00:00Z",
                "2026-06-15T23:59:59Z"
            )
        )
    }

    @Test
    fun `local with +0500 vs remote Z same instant`() {
        // 2026-06-15T09:33:25+05:00 = 2026-06-15T04:33:25Z
        assertTrue(
            ConflictHelper.isLocalNewerOrEqual(
                "2026-06-15T09:33:25+05:00",
                "2026-06-15T04:33:25Z"
            )
        )
    }

    @Test
    fun `unparseable falls back to string comparison`() {
        // "invalid" > "2026-06-15T04:33:25Z" as strings (i > 2)
        assertTrue(
            ConflictHelper.isLocalNewerOrEqual(
                "invalid",
                "2026-06-15T04:33:25Z"
            )
        )
    }

    @Test
    fun `both unparseable uses string comparison`() {
        // "abc" >= "xyz" is false as strings (a < x)
        assertFalse(
            ConflictHelper.isLocalNewerOrEqual("abc", "xyz")
        )
    }

    @Test
    fun `local nullish string vs remote valid`() {
        // "null" > "2026-06-15T04:33:25Z" as strings (n > 2)
        assertTrue(
            ConflictHelper.isLocalNewerOrEqual(
                "null",
                "2026-06-15T04:33:25Z"
            )
        )
    }

    @Test
    fun `remote nullish string local valid`() {
        // remote unparseable → falls back to string
        // "2026-06-15T04:33:25Z" >= "null" → false as strings (2 < n)
        assertFalse(
            ConflictHelper.isLocalNewerOrEqual(
                "2026-06-15T04:33:25Z",
                "null"
            )
        )
    }

    @Test
    fun `local one day later with positive offset is earlier in UTC`() {
        // 2026-06-16T00:00:00+05:00 = 2026-06-15T19:00:00Z
        // remote = 2026-06-15T23:30:00Z
        // local is BEFORE remote → should be false
        assertFalse(
            ConflictHelper.isLocalNewerOrEqual(
                "2026-06-16T00:00:00+05:00",
                "2026-06-15T23:30:00Z"
            )
        )
    }
}
