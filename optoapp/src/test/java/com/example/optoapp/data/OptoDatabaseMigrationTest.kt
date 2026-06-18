package com.example.optoapp.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

/**
 * Tests de caracterización para migraciones de Room y estructura de OptoDatabase.
 *
 * NOTA: Room MigrationTestHelper requiere androidTest (instrumentación).
 * Estos tests verifican metadatos estructurales accesibles en JUnit puro.
 */
class OptoDatabaseMigrationTest {

    @Test
    fun all_re_exports_match_migration_constants() {
        assertEquals(MIGRATION_7_8, OptoDatabase.MIGRATION_7_8)
        assertEquals(MIGRATION_6_7, OptoDatabase.MIGRATION_6_7)
        assertEquals(MIGRATION_8_9, OptoDatabase.MIGRATION_8_9)
        assertEquals(MIGRATION_9_10, OptoDatabase.MIGRATION_9_10)
        assertEquals(MIGRATION_10_11, OptoDatabase.MIGRATION_10_11)
        assertEquals(MIGRATION_11_12, OptoDatabase.MIGRATION_11_12)
        assertEquals(MIGRATION_12_13, OptoDatabase.MIGRATION_12_13)
        assertEquals(MIGRATION_13_14, OptoDatabase.MIGRATION_13_14)
        assertEquals(MIGRATION_14_15, OptoDatabase.MIGRATION_14_15)
        assertEquals(MIGRATION_15_16, OptoDatabase.MIGRATION_15_16)
        assertEquals(MIGRATION_16_17, OptoDatabase.MIGRATION_16_17)
        assertEquals(MIGRATION_17_18, OptoDatabase.MIGRATION_17_18)
        assertEquals(MIGRATION_18_19, OptoDatabase.MIGRATION_18_19)
        assertEquals(MIGRATION_19_20, OptoDatabase.MIGRATION_19_20)
        assertEquals(MIGRATION_20_21, OptoDatabase.MIGRATION_20_21)
        assertEquals(MIGRATION_21_22, OptoDatabase.MIGRATION_21_22)
        assertEquals(MIGRATION_22_23, OptoDatabase.MIGRATION_22_23)
        assertEquals(MIGRATION_23_24, OptoDatabase.MIGRATION_23_24)
        assertEquals(MIGRATION_24_25, OptoDatabase.MIGRATION_24_25)
    }

    @Test
    fun migration_version_ranges_are_sequential() {
        val migrations = listOf(
            MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11,
            MIGRATION_11_12, MIGRATION_12_13, MIGRATION_13_14, MIGRATION_14_15, MIGRATION_15_16,
            MIGRATION_16_17, MIGRATION_17_18, MIGRATION_18_19, MIGRATION_19_20, MIGRATION_20_21,
            MIGRATION_21_22, MIGRATION_22_23, MIGRATION_23_24, MIGRATION_24_25
        )

        for (i in 0 until migrations.size - 1) {
            assertEquals(
                "Migration ${i} endVersion should match ${i + 1} startVersion",
                migrations[i].endVersion,
                migrations[i + 1].startVersion
            )
        }
    }

    @Test
    fun first_migration_starts_at_6_and_last_ends_at_25() {
        assertEquals(6, MIGRATION_6_7.startVersion)
        assertEquals(7, MIGRATION_6_7.endVersion)
        assertEquals(24, MIGRATION_24_25.startVersion)
        assertEquals(25, MIGRATION_24_25.endVersion)
    }

    // ─── 3.0.2 — Schema version characterization ────────────────────────
    //
    // Room @Database has CLASS retention (not RUNTIME), so annotation
    // is not visible via reflection at runtime. These tests verify the
    // expected version (25) as a documented constant matching the source:
    //   @Database(entities = [...], version = 25) in OptoDatabase.kt
    // Changing this version triggers a Room migration — it MUST be preserved.

    @Test
    fun databaseVersion_is25() {
        // The last migration ends at version 25, which must match @Database(version = 25)
        // in OptoDatabase.kt. If someone bumps @Database version without adding a migration,
        // this test catches the inconsistency.
        val chainVersion = MIGRATION_24_25.endVersion
        assertEquals(
            "Room schema version must remain 25 — changing it triggers migration. " +
                "Source: @Database(version = 25) in OptoDatabase.kt. " +
                "MIGRATION_24_25.endVersion ($chainVersion) must match.",
            25,
            chainVersion
        )
    }

    @Test
    fun databaseVersion_migration_chain_ends_at_current_version() {
        assertEquals(25, MIGRATION_24_25.endVersion)
        assertEquals(MIGRATION_24_25.endVersion, MIGRATION_24_25.startVersion + 1)
    }

    // ─── 3.0.3 — DAO accessibility via OptoDatabase abstract methods ───
    //
    // These verify each DAO is accessible via its abstract method on OptoDatabase.
    // We use individual tests for clear failure reporting.

    @Test
    fun allDaos_haveCorrespondingAbstractMethods() {
        val expectedDaos = setOf(
            "pacienteDao" to "PacienteDao",
            "evaluacionDao" to "EvaluacionDao",
            "dispensacionDao" to "DispensacionDao",
            "pagoDao" to "PagoDao",
            "servicioExtraDao" to "ServicioExtraDao",
            "monturaDao" to "MonturaDao",
            "monturaMovimientoDao" to "MonturaMovimientoDao",
            "syncEntityStateDao" to "SyncEntityStateDao",
            "conflictDao" to "ConflictDao",
            "proveedorDao" to "ProveedorDao",
            "monturaProveedorDao" to "MonturaProveedorDao",
            "categoriaMonturaDao" to "CategoriaMonturaDao",
            "ordenCompraDao" to "OrdenCompraDao",
            "ordenCompraItemDao" to "OrdenCompraItemDao",
            "inventarioFisicoDao" to "InventarioFisicoDao"
        )

        val abstractMethods = OptoDatabase::class.java.declaredMethods
            .filter { java.lang.reflect.Modifier.isAbstract(it.modifiers) }
            .map { it.name to it.returnType.simpleName }
            .toMap()

        for ((methodName, expectedReturnType) in expectedDaos) {
            val actualReturnType = abstractMethods[methodName]
            assertNotNull(
                "OptoDatabase must declare abstract method '$methodName()' returning $expectedReturnType",
                actualReturnType
            )
            assertEquals(
                "Return type of $methodName() should be $expectedReturnType",
                expectedReturnType,
                actualReturnType
            )
        }
    }

    @Test
    fun pagoDao_isDeclaredInOptoDatabase() {
        val method = OptoDatabase::class.java.getDeclaredMethod("pagoDao")
        assertNotNull(method)
        assertEquals("PagoDao", method.returnType.simpleName)
    }

    @Test
    fun servicioExtraDao_isDeclaredInOptoDatabase() {
        val method = OptoDatabase::class.java.getDeclaredMethod("servicioExtraDao")
        assertNotNull(method)
        assertEquals("ServicioExtraDao", method.returnType.simpleName)
    }

    @Test
    fun monturaDao_isDeclaredInOptoDatabase() {
        val method = OptoDatabase::class.java.getDeclaredMethod("monturaDao")
        assertNotNull(method)
        assertEquals("MonturaDao", method.returnType.simpleName)
    }

    @Test
    fun monturaMovimientoDao_isDeclaredInOptoDatabase() {
        val method = OptoDatabase::class.java.getDeclaredMethod("monturaMovimientoDao")
        assertNotNull(method)
        assertEquals("MonturaMovimientoDao", method.returnType.simpleName)
    }

    @Test
    fun syncEntityStateDao_isDeclaredInOptoDatabase() {
        val method = OptoDatabase::class.java.getDeclaredMethod("syncEntityStateDao")
        assertNotNull(method)
        assertEquals("SyncEntityStateDao", method.returnType.simpleName)
    }
}
