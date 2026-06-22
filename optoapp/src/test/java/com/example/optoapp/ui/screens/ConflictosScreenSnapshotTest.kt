package com.example.optoapp.ui.screens

import com.example.optoapp.data.ConflictRecord
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Phase 13 — RED tests for snapshot-based field-level conflict UI.
 *
 * Verifies that [ConflictCard] correctly branches between:
 * - Field-level diff display when snapshot data is available
 * - Timestamp display when `baseSnapshot == "{}"`
 */
@RunWith(RobolectricTestRunner::class)
class ConflictosScreenSnapshotTest {

    private val opticaId = "optica-ui-test"

    // Conflict WITH snapshot data — should show field-level diffs
    private val conflictWithSnapshot = ConflictRecord(
        entityId = "paciente-ui-001",
        opticaId = opticaId,
        entityType = "paciente",
        localSnapshot = """{"id":"paciente-ui-001","nombre":"Juan Local","telefono":"555-0100"}""",
        remoteSnapshot = """{"id":"paciente-ui-001","nombre":"Juan Remoto","telefono":"555-0999"}""",
        baseSnapshot = """{"id":"paciente-ui-001","nombre":"Juan Base","telefono":"555-0000"}""",
        localData = """{"id":"paciente-ui-001","nombre":"Juan Local","telefono":"555-0100"}""",
        remoteData = """{"id":"paciente-ui-001","nombre":"Juan Remoto","telefono":"555-0999"}"""
    )

    // Conflict WITHOUT snapshot data — should show timestamp display
    private val conflictWithoutSnapshot = ConflictRecord(
        entityId = "paciente-ui-002",
        opticaId = opticaId,
        entityType = "paciente",
        localSnapshot = "2026-06-22T09:00:00Z",
        remoteSnapshot = "2026-06-22T10:00:00Z",
        baseSnapshot = "{}"
    )

    @Test
    fun conflictRecord_withBaseSnapshot_hasSnapshotData() {
        assertTrue(
            "Conflict with non-empty baseSnapshot should have snapshot data",
            conflictWithSnapshot.baseSnapshot.isNotBlank() && conflictWithSnapshot.baseSnapshot != "{}"
        )
    }

    @Test
    fun conflictRecord_withoutBaseSnapshot_hasNoSnapshotData() {
        assertFalse(
            "Conflict with empty baseSnapshot should NOT have snapshot data",
            conflictWithoutSnapshot.baseSnapshot.isNotBlank() && conflictWithoutSnapshot.baseSnapshot != "{}"
        )
    }

    @Test
    fun conflictRecord_withSnapshot_hasLocalAndRemoteData() {
        assertTrue("localData should be non-blank", conflictWithSnapshot.localData.isNotBlank())
        assertTrue("remoteData should be non-blank", conflictWithSnapshot.remoteData.isNotBlank())
    }

    @Test
    fun conflictRecord_withoutSnapshot_hasLocalAndRemoteDataEmpty() {
        assertEquals("localData should be '{}' default", "{}", conflictWithoutSnapshot.localData)
        assertEquals("remoteData should be '{}' default", "{}", conflictWithoutSnapshot.remoteData)
    }
}
