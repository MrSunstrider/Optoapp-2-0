package com.example.optoapp.domain.sync

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * RED tests for [ThreeWayMerge] — FR-09: Field-Level Three-Way Merge Logic.
 *
 * Pure-function tests: no mocks, no Room, no Supabase. The merge logic is
 * deterministic and side-effect free, so each scenario maps directly to
 * an input → expected-output assertion.
 *
 * These tests FAIL until [ThreeWayMerge] is created in `domain/sync/ThreeWayMerge.kt`.
 */
class ThreeWayMergeTest {

    private val json = Json { ignoreUnknownKeys = true }

    private fun obj(vararg pairs: Pair<String, JsonPrimitive>): JsonObject = buildJsonObject {
        pairs.forEach { (k, v) -> put(k, v) }
    }

    private fun parse(s: String): JsonObject = json.parseToJsonElement(s).jsonObject

    private fun intField(o: JsonObject, name: String): Int? = o[name]?.jsonPrimitive?.intOrNull

    private fun strField(o: JsonObject, name: String): String? = o[name]?.jsonPrimitive?.contentOrNull

    // ── FR-09 Scenario: "No changes from either side" ────────────────────────

    @Test
    fun `all_unchanged_returns_base_with_no_conflicts_and_no_auto_merge`() {
        val base = obj("a" to JsonPrimitive(1), "b" to JsonPrimitive(2))
        val local = obj("a" to JsonPrimitive(1), "b" to JsonPrimitive(2))
        val remote = obj("a" to JsonPrimitive(1), "b" to JsonPrimitive(2))

        val result = ThreeWayMerge.merge(MergeInput(base, local, remote))

        assertEquals(1, intField(result.mergedEntity, "a"))
        assertEquals(2, intField(result.mergedEntity, "b"))
        assertTrue("conflictedFields must be empty", result.conflictedFields.isEmpty())
        assertTrue("autoMergedFields must be empty", result.autoMergedFields.isEmpty())
        assertFalse("hasConflict must be false when no changes", result.hasConflict)
    }

    // ── FR-09 Scenario: "local != base AND remote == base → apply local" ─────

    @Test
    fun `local_only_change_auto_merges_local_value`() {
        val base = obj("a" to JsonPrimitive(1), "b" to JsonPrimitive(2))
        val local = obj("a" to JsonPrimitive(10), "b" to JsonPrimitive(2))
        val remote = obj("a" to JsonPrimitive(1), "b" to JsonPrimitive(2))

        val result = ThreeWayMerge.merge(MergeInput(base, local, remote))

        assertEquals(10, intField(result.mergedEntity, "a"))
        assertEquals(2, intField(result.mergedEntity, "b"))
        assertTrue("conflictedFields must be empty — local-only is auto-merge", result.conflictedFields.isEmpty())
        assertEquals("autoMergedFields should include 'a'", listOf("a"), result.autoMergedFields)
        assertFalse("hasConflict must be false for local-only change", result.hasConflict)
    }

    // ── FR-09 Scenario: "local == base AND remote != base → apply remote" ────

    @Test
    fun `remote_only_change_auto_merges_remote_value`() {
        val base = obj("a" to JsonPrimitive(1), "b" to JsonPrimitive(2))
        val local = obj("a" to JsonPrimitive(1), "b" to JsonPrimitive(2))
        val remote = obj("a" to JsonPrimitive(1), "b" to JsonPrimitive(20))

        val result = ThreeWayMerge.merge(MergeInput(base, local, remote))

        assertEquals(1, intField(result.mergedEntity, "a"))
        assertEquals(20, intField(result.mergedEntity, "b"))
        assertTrue("conflictedFields must be empty — remote-only is auto-merge", result.conflictedFields.isEmpty())
        assertEquals("autoMergedFields should include 'b'", listOf("b"), result.autoMergedFields)
        assertFalse("hasConflict must be false for remote-only change", result.hasConflict)
    }

    // ── FR-09 Scenario: "Non-overlapping changes auto-merge" ─────────────────

    @Test
    fun `non_overlapping_changes_auto_merge_both_fields`() {
        val base = obj("a" to JsonPrimitive(1), "b" to JsonPrimitive(2))
        val local = obj("a" to JsonPrimitive(10), "b" to JsonPrimitive(2))
        val remote = obj("a" to JsonPrimitive(1), "b" to JsonPrimitive(20))

        val result = ThreeWayMerge.merge(MergeInput(base, local, remote))

        assertEquals(10, intField(result.mergedEntity, "a"))
        assertEquals(20, intField(result.mergedEntity, "b"))
        assertTrue("conflictedFields must be empty — non-overlapping auto-merges", result.conflictedFields.isEmpty())
        assertEquals("autoMergedFields should include both 'a' and 'b'", listOf("a", "b"), result.autoMergedFields.sorted())
        assertFalse("hasConflict must be false when changes don't overlap", result.hasConflict)
    }

    // ── FR-09 Scenario: "Overlapping changes produce conflict" ───────────────

    @Test
    fun `overlapping_changes_produce_conflict_on_both_fields`() {
        val base = obj("a" to JsonPrimitive(1), "b" to JsonPrimitive(2))
        val local = obj("a" to JsonPrimitive(10), "b" to JsonPrimitive(20))
        val remote = obj("a" to JsonPrimitive(100), "b" to JsonPrimitive(200))

        val result = ThreeWayMerge.merge(MergeInput(base, local, remote))

        assertEquals(
            "conflictedFields should list both 'a' and 'b'",
            listOf("a", "b"),
            result.conflictedFields.sorted(),
        )
        assertTrue("autoMergedFields must be empty — nothing auto-merged", result.autoMergedFields.isEmpty())
        assertTrue("hasConflict must be true when both sides changed the same field", result.hasConflict)
    }

    // ── FR-09 Scenario: "Missing snapshot fields treated as no-change" → empty base ──

    @Test
    fun `empty_base_treats_all_fields_as_conflicting_when_both_sides_have_data`() {
        // baseSnapshot = '{}' (empty object) — per FR-09, all fields in both local and remote
        // are treated as conflicting (no base to compare against).
        val base = JsonObject(emptyMap())
        val local = obj("a" to JsonPrimitive(10), "b" to JsonPrimitive(20))
        val remote = obj("a" to JsonPrimitive(100), "b" to JsonPrimitive(200))

        val result = ThreeWayMerge.merge(MergeInput(base, local, remote))

        assertEquals(
            "With empty base, all fields present in both sides are conflicting",
            listOf("a", "b"),
            result.conflictedFields.sorted(),
        )
        assertTrue("hasConflict must be true with empty base and differing fields", result.hasConflict)
    }

    // ── FR-09 Scenario: "Missing fields in one side" ─────────────────────────

    @Test
    fun `field_present_in_base_and_local_but_missing_in_remote_keeps_local`() {
        // remote omits field 'b' — base has it, local changed it.
        // Since remote == base for 'a' (both 1), 'a' is auto-merged to local.
        // For 'b', remote has no value — treat as "remote == base" (absence = no change).
        val base = obj("a" to JsonPrimitive(1), "b" to JsonPrimitive(2))
        val local = obj("a" to JsonPrimitive(1), "b" to JsonPrimitive(99))
        val remote = obj("a" to JsonPrimitive(1))

        val result = ThreeWayMerge.merge(MergeInput(base, local, remote))

        // 'b' is only changed in local → auto-merge to local value
        assertEquals(99, intField(result.mergedEntity, "b"))
        assertTrue("conflictedFields must be empty", result.conflictedFields.isEmpty())
        assertEquals("autoMergedFields should include 'b'", listOf("b"), result.autoMergedFields)
        assertFalse("hasConflict must be false", result.hasConflict)
    }

    @Test
    fun `field_present_in_base_and_remote_but_missing_in_local_keeps_remote`() {
        // local omits field 'b' — base has it, remote changed it.
        val base = obj("a" to JsonPrimitive(1), "b" to JsonPrimitive(2))
        val local = obj("a" to JsonPrimitive(1))
        val remote = obj("a" to JsonPrimitive(1), "b" to JsonPrimitive(88))

        val result = ThreeWayMerge.merge(MergeInput(base, local, remote))

        assertEquals(88, intField(result.mergedEntity, "b"))
        assertTrue("conflictedFields must be empty", result.conflictedFields.isEmpty())
        assertEquals("autoMergedFields should include 'b'", listOf("b"), result.autoMergedFields)
        assertFalse("hasConflict must be false", result.hasConflict)
    }

    // ── FR-09 Scenario: "no-changes" (redundant with first test but with different field shapes) ──

    @Test
    fun `no_changes_returns_base_identical_with_string_fields`() {
        val base = obj("nombre" to JsonPrimitive("Juan"), "telefono" to JsonPrimitive("555"))
        val local = obj("nombre" to JsonPrimitive("Juan"), "telefono" to JsonPrimitive("555"))
        val remote = obj("nombre" to JsonPrimitive("Juan"), "telefono" to JsonPrimitive("555"))

        val result = ThreeWayMerge.merge(MergeInput(base, local, remote))

        assertEquals("Juan", strField(result.mergedEntity, "nombre"))
        assertEquals("555", strField(result.mergedEntity, "telefono"))
        assertTrue(result.conflictedFields.isEmpty())
        assertTrue(result.autoMergedFields.isEmpty())
        assertFalse(result.hasConflict)
    }

    // ── Mixed scenario: one auto-merge + one conflict ────────────────────────

    @Test
    fun `mixed_auto_merge_and_conflict_reports_correct_lists`() {
        // 'a' is local-only change (auto-merge), 'b' is conflicting (both changed)
        val base = obj("a" to JsonPrimitive(1), "b" to JsonPrimitive(2))
        val local = obj("a" to JsonPrimitive(10), "b" to JsonPrimitive(20))
        val remote = obj("a" to JsonPrimitive(1), "b" to JsonPrimitive(200))

        val result = ThreeWayMerge.merge(MergeInput(base, local, remote))

        assertEquals("Only 'b' is conflicted (both sides changed it)", listOf("b"), result.conflictedFields)
        assertEquals("Only 'a' was auto-merged (local-only change)", listOf("a"), result.autoMergedFields)
        assertTrue("hasConflict must be true — 'b' conflicts", result.hasConflict)
        // Merged entity: 'a' takes local (10), 'b' — for the merged result, the conflict
        // keeps one side. The spec says mergedEntity is the auto-merged fields; conflicted
        // fields need a resolution policy (local-wins or remote-wins) applied by the caller.
        // ThreeWayMerge.merge returns the auto-merged entity; conflicted fields retain local
        // as the default starting point.
        assertEquals(10, intField(result.mergedEntity, "a"))
    }
}
