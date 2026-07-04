package com.example.optoapp.data

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * RED test: MIGRATION_27_28 adds three snapshot columns to `conflict_records`.
 *
 * Verifies FR-07: Conflict Record Schema Migration.
 *
 * These tests FAIL until:
 * - [MIGRATION_27_28] is created in `OptoDatabaseMigrations.kt`
 * - [OptoDatabase] is bumped to version 28 and registers [MIGRATION_27_28]
 */
@RunWith(RobolectricTestRunner::class)
class Migration27To28Test {

    @Test
    fun migration_27_28_exists_and_targets_correct_versions() {
        assertEquals(27, MIGRATION_27_28.startVersion)
        assertEquals(28, MIGRATION_27_28.endVersion)
    }

    @Test
    fun migration_27_28_is_re_exported_from_opto_database() {
        assertEquals(MIGRATION_27_28, OptoDatabase.MIGRATION_27_28)
    }

    @Test
    fun full_migration_chain_6_to_28_is_sequential() {
        val migrations = listOf(
            MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11,
            MIGRATION_11_12, MIGRATION_12_13, MIGRATION_13_14, MIGRATION_14_15, MIGRATION_15_16,
            MIGRATION_16_17, MIGRATION_17_18, MIGRATION_18_19, MIGRATION_19_20, MIGRATION_20_21,
            MIGRATION_21_22, MIGRATION_22_23, MIGRATION_23_24, MIGRATION_24_25, MIGRATION_25_26,
            MIGRATION_26_27, MIGRATION_27_28
        )

        assertEquals(6, migrations.first().startVersion)
        assertEquals(28, migrations.last().endVersion)

        for (i in 0 until migrations.size - 1) {
            assertEquals(
                "Migration ${i} end=${migrations[i].endVersion} should match ${i + 1} start=${migrations[i + 1].startVersion}",
                migrations[i].endVersion,
                migrations[i + 1].startVersion
            )
        }
    }

    /**
     * FR-07 Scenario: "Migration preserves existing rows".
     *
     * Creates a v27 database with 5 conflict_records rows, runs MIGRATION_27_28,
     * and verifies all 5 rows survive with the 3 new columns defaulting to '{}'
     * while the original column values are preserved.
     */
    @Test
    fun migration_27_28_preserves_existing_rows_and_adds_snapshot_columns_with_defaults() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dbName = "migration-27to28-snapshot-test.db"
        context.deleteDatabase(dbName)

        val factory = FrameworkSQLiteOpenHelperFactory()

        // ── Step 1: Create v27 database with conflict_records table ──
        val v27Config = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(dbName)
            .callback(object : SupportSQLiteOpenHelper.Callback(27) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    // v27 schema for conflict_records (from MIGRATION_22_23)
                    db.execSQL("""
                        CREATE TABLE IF NOT EXISTS conflict_records (
                            entityId TEXT NOT NULL PRIMARY KEY,
                            opticaId TEXT NOT NULL,
                            entityType TEXT NOT NULL,
                            localSnapshot TEXT NOT NULL,
                            remoteSnapshot TEXT NOT NULL,
                            detectedAt INTEGER NOT NULL DEFAULT 0
                        )
                    """)
                }

                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {
                    // No upgrade — this helper stays at v27
                }
            })
            .build()

        val v27Helper = factory.create(v27Config)
        val v27Db = v27Helper.writableDatabase

        // ── Step 2: Insert 5 conflict_records rows ──
        val rows = listOf(
            Triple("id-1", "paciente", "2026-06-15T04:00:00Z"),
            Triple("id-2", "evaluacion", "2026-06-15T05:00:00Z"),
            Triple("id-3", "montura", "2026-06-15T06:00:00Z"),
            Triple("id-4", "proveedor", "2026-06-15T07:00:00Z"),
            Triple("id-5", "orden_compra", "2026-06-15T08:00:00Z")
        )
        rows.forEachIndexed { index, (id, type, ts) ->
            v27Db.execSQL(
                "INSERT INTO conflict_records (entityId, opticaId, entityType, localSnapshot, remoteSnapshot, detectedAt) VALUES (?, ?, ?, ?, ?, ?)",
                arrayOf<Any>(id, "optica-test", type, ts, ts, (index + 1).toLong())
            )
        }

        // Verify 5 rows inserted before migration
        val countCursor = v27Db.query("SELECT COUNT(*) FROM conflict_records")
        assertTrue(countCursor.moveToFirst())
        assertEquals(5, countCursor.getInt(0))
        countCursor.close()

        v27Helper.close()

        // ── Step 3: Open at v28 and run MIGRATION_27_28 ──
        val v28Config = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(dbName)
            .callback(object : SupportSQLiteOpenHelper.Callback(28) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    // Should not be called — DB already exists at v27
                }

                override fun onUpgrade(
                    db: SupportSQLiteDatabase,
                    oldVersion: Int,
                    newVersion: Int
                ) {
                    if (oldVersion == 27 && newVersion == 28) {
                        MIGRATION_27_28.migrate(db)
                    }
                }
            })
            .build()

        val v28Helper = factory.create(v28Config)
        val v28Db = v28Helper.writableDatabase

        // ── Step 4: Verify all 5 rows preserved with original column values ──
        val allRowsCursor = v28Db.query("SELECT entityId, opticaId, entityType, localSnapshot, remoteSnapshot, detectedAt, baseSnapshot, localData, remoteData FROM conflict_records ORDER BY entityId")
        val surviving = mutableListOf<Array<Any?>>()
        while (allRowsCursor.moveToNext()) {
            surviving.add(arrayOf(
                allRowsCursor.getString(allRowsCursor.getColumnIndexOrThrow("entityId")),
                allRowsCursor.getString(allRowsCursor.getColumnIndexOrThrow("opticaId")),
                allRowsCursor.getString(allRowsCursor.getColumnIndexOrThrow("entityType")),
                allRowsCursor.getString(allRowsCursor.getColumnIndexOrThrow("localSnapshot")),
                allRowsCursor.getString(allRowsCursor.getColumnIndexOrThrow("remoteSnapshot")),
                allRowsCursor.getLong(allRowsCursor.getColumnIndexOrThrow("detectedAt")),
                allRowsCursor.getString(allRowsCursor.getColumnIndexOrThrow("baseSnapshot")),
                allRowsCursor.getString(allRowsCursor.getColumnIndexOrThrow("localData")),
                allRowsCursor.getString(allRowsCursor.getColumnIndexOrThrow("remoteData"))
            ))
        }
        allRowsCursor.close()

        assertEquals("All 5 rows must survive migration", 5, surviving.size)

        // Verify first row: original columns preserved + new columns default to '{}'
        val firstRow = surviving[0]
        assertEquals("id-1", firstRow[0])
        assertEquals("optica-test", firstRow[1])
        assertEquals("paciente", firstRow[2])
        assertEquals("2026-06-15T04:00:00Z", firstRow[3])
        assertEquals("2026-06-15T04:00:00Z", firstRow[4])
        assertEquals(1L, firstRow[5])
        assertEquals("baseSnapshot must default to '{}' for existing rows", "{}", firstRow[6])
        assertEquals("localData must default to '{}' for existing rows", "{}", firstRow[7])
        assertEquals("remoteData must default to '{}' for existing rows", "{}", firstRow[8])

        // Verify last row similarly
        val lastRow = surviving[4]
        assertEquals("id-5", lastRow[0])
        assertEquals("orden_compra", lastRow[2])
        assertEquals(5L, lastRow[5])
        assertEquals("{}", lastRow[6])
        assertEquals("{}", lastRow[7])
        assertEquals("{}", lastRow[8])

        v28Helper.close()
        context.deleteDatabase(dbName)
    }

    /**
     * FR-07 Scenario: "Fresh insert includes snapshot columns".
     *
     * After migration, a fresh INSERT that supplies the 3 new columns must persist
     * the provided JSON values (not the defaults).
     */
    @Test
    fun migration_27_28_allows_fresh_insert_with_snapshot_values() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dbName = "migration-27to28-fresh-insert-test.db"
        context.deleteDatabase(dbName)

        val factory = FrameworkSQLiteOpenHelperFactory()

        // Create v27 with empty conflict_records
        val v27Config = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(dbName)
            .callback(object : SupportSQLiteOpenHelper.Callback(27) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    db.execSQL("""
                        CREATE TABLE IF NOT EXISTS conflict_records (
                            entityId TEXT NOT NULL PRIMARY KEY,
                            opticaId TEXT NOT NULL,
                            entityType TEXT NOT NULL,
                            localSnapshot TEXT NOT NULL,
                            remoteSnapshot TEXT NOT NULL,
                            detectedAt INTEGER NOT NULL DEFAULT 0
                        )
                    """)
                }
                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
            })
            .build()
        val v27Helper = factory.create(v27Config)
        v27Helper.writableDatabase.execSQL(
            "INSERT INTO conflict_records (entityId, opticaId, entityType, localSnapshot, remoteSnapshot, detectedAt) VALUES (?, ?, ?, ?, ?, ?)",
            arrayOf<Any>("fresh-1", "optica-x", "paciente", "T1", "T2", 100L)
        )
        v27Helper.close()

        // Migrate to v28
        val v28Config = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(dbName)
            .callback(object : SupportSQLiteOpenHelper.Callback(28) {
                override fun onCreate(db: SupportSQLiteDatabase) {}
                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {
                    if (oldVersion == 27 && newVersion == 28) MIGRATION_27_28.migrate(db)
                }
            })
            .build()
        val v28Helper = factory.create(v28Config)
        val v28Db = v28Helper.writableDatabase

        // Insert a fresh row supplying all 3 new snapshot columns
        v28Db.execSQL(
            """
            INSERT OR REPLACE INTO conflict_records
                (entityId, opticaId, entityType, localSnapshot, remoteSnapshot, detectedAt, baseSnapshot, localData, remoteData)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """,
            arrayOf<Any>(
                "fresh-2", "optica-x", "paciente", "T3", "T4", 200L,
                """{"id":"fresh-2","nombre":"base"}""",
                """{"id":"fresh-2","nombre":"local"}""",
                """{"id":"fresh-2","nombre":"remote"}"""
            )
        )

        // Verify the fresh row has the provided JSON values (not the defaults)
        val cursor = v28Db.query("SELECT baseSnapshot, localData, remoteData FROM conflict_records WHERE entityId = 'fresh-2'")
        assertTrue(cursor.moveToFirst())
        assertEquals("""{"id":"fresh-2","nombre":"base"}""", cursor.getString(0))
        assertEquals("""{"id":"fresh-2","nombre":"local"}""", cursor.getString(1))
        assertEquals("""{"id":"fresh-2","nombre":"remote"}""", cursor.getString(2))
        cursor.close()

        v28Helper.close()
        context.deleteDatabase(dbName)
    }

    /**
     * FR-07 Scenario: "Migration on empty database".
     *
     * Migration on a v27 DB with zero conflict_records rows must succeed —
     * the 3 new columns must be present and the table empty.
     */
    @Test
    fun migration_27_28_on_empty_table_succeeds() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dbName = "migration-27to28-empty-test.db"
        context.deleteDatabase(dbName)

        val factory = FrameworkSQLiteOpenHelperFactory()

        val v27Config = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(dbName)
            .callback(object : SupportSQLiteOpenHelper.Callback(27) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    db.execSQL("""
                        CREATE TABLE IF NOT EXISTS conflict_records (
                            entityId TEXT NOT NULL PRIMARY KEY,
                            opticaId TEXT NOT NULL,
                            entityType TEXT NOT NULL,
                            localSnapshot TEXT NOT NULL,
                            remoteSnapshot TEXT NOT NULL,
                            detectedAt INTEGER NOT NULL DEFAULT 0
                        )
                    """)
                }
                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
            })
            .build()
        val v27Helper = factory.create(v27Config)
        // Access writableDatabase to trigger onCreate (SQLite creates the DB lazily)
        v27Helper.writableDatabase
        v27Helper.close()

        val v28Config = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(dbName)
            .callback(object : SupportSQLiteOpenHelper.Callback(28) {
                override fun onCreate(db: SupportSQLiteDatabase) {}
                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {
                    if (oldVersion == 27 && newVersion == 28) MIGRATION_27_28.migrate(db)
                }
            })
            .build()
        val v28Helper = factory.create(v28Config)
        val v28Db = v28Helper.writableDatabase

        // After migration: table exists, 0 rows, 3 new columns queryable
        val countCursor = v28Db.query("SELECT COUNT(*) FROM conflict_records")
        assertTrue(countCursor.moveToFirst())
        assertEquals(0, countCursor.getInt(0))
        countCursor.close()

        // Inserting a row using only defaults for new columns must work (DEFAULT '{}' applies)
        v28Db.execSQL(
            "INSERT INTO conflict_records (entityId, opticaId, entityType, localSnapshot, remoteSnapshot, detectedAt) VALUES (?, ?, ?, ?, ?, ?)",
            arrayOf<Any>("empty-1", "optica-y", "paciente", "T", "T", 1L)
        )
        val rowCursor = v28Db.query("SELECT baseSnapshot, localData, remoteData FROM conflict_records WHERE entityId = 'empty-1'")
        assertTrue(rowCursor.moveToFirst())
        assertEquals("{}", rowCursor.getString(0))
        assertEquals("{}", rowCursor.getString(1))
        assertEquals("{}", rowCursor.getString(2))
        rowCursor.close()

        v28Helper.close()
        context.deleteDatabase(dbName)
    }
}
