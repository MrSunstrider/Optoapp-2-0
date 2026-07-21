package com.example.optoapp.data

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * RED test: MIGRATION_28_29 adds `fecha_entrega` column to `servicios_extra`.
 *
 * Verifies Servicio Extra Schema Migration.
 *
 * These tests FAIL until:
 * - [MIGRATION_28_29] is created in `OptoDatabaseMigrations.kt`
 * - [OptoDatabase] is bumped to version 29 and registers [MIGRATION_28_29]
 */
@RunWith(RobolectricTestRunner::class)
class ServicioExtraMigration28To29Test {

    @Test
    fun migration_28_29_exists_and_targets_correct_versions() {
        assertEquals(28, MIGRATION_28_29.startVersion)
        assertEquals(29, MIGRATION_28_29.endVersion)
    }

    @Test
    fun migration_28_29_is_re_exported_from_opto_database() {
        assertEquals(MIGRATION_28_29, OptoDatabase.MIGRATION_28_29)
    }

    @Test
    fun full_migration_chain_6_to_29_is_sequential() {
        val migrations = listOf(
            MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11,
            MIGRATION_11_12, MIGRATION_12_13, MIGRATION_13_14, MIGRATION_14_15, MIGRATION_15_16,
            MIGRATION_16_17, MIGRATION_17_18, MIGRATION_18_19, MIGRATION_19_20, MIGRATION_20_21,
            MIGRATION_21_22, MIGRATION_22_23, MIGRATION_23_24, MIGRATION_24_25, MIGRATION_25_26,
            MIGRATION_26_27, MIGRATION_27_28, MIGRATION_28_29,
        )

        assertEquals(6, migrations.first().startVersion)
        assertEquals(29, migrations.last().endVersion)

        for (i in 0 until migrations.size - 1) {
            assertEquals(
                "Migration $i end=${migrations[i].endVersion} should match ${i + 1} start=${migrations[i + 1].startVersion}",
                migrations[i].endVersion,
                migrations[i + 1].startVersion,
            )
        }
    }

    /**
     * Scenario: "Migration preserves existing rows".
     *
     * Creates a v28 database with 3 servicios_extra rows, runs MIGRATION_28_29,
     * and verifies all 3 rows survive with `fecha_entrega IS NULL`.
     */
    @Test
    fun migration_28_29_preserves_existing_rows() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dbName = "migration-28to29-preserve-test.db"
        context.deleteDatabase(dbName)

        val factory = FrameworkSQLiteOpenHelperFactory()

        // Step 1: Create v28 database with servicios_extra table
        val v28Config = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(dbName)
            .callback(object : SupportSQLiteOpenHelper.Callback(28) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    // v28 schema for servicios_extra (from MIGRATION_9_10 + MIGRATION_22_23)
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS servicios_extra (
                            id TEXT NOT NULL PRIMARY KEY,
                            ot TEXT NOT NULL,
                            descripcion TEXT NOT NULL,
                            montoTotal REAL NOT NULL,
                            aCuenta REAL NOT NULL,
                            estado TEXT NOT NULL,
                            fecha TEXT NOT NULL,
                            pacienteId TEXT,
                            metodoPago TEXT NOT NULL,
                            opticaId TEXT NOT NULL,
                            updatedAt TEXT,
                            updatedBy TEXT,
                            FOREIGN KEY(pacienteId) REFERENCES pacientes(id) ON DELETE SET NULL
                        )
                        """.trimIndent(),
                    )
                }

                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {
                    // No upgrade — this helper stays at v28
                }
            })
            .build()

        val v28Helper = factory.create(v28Config)
        val v28Db = v28Helper.writableDatabase

        // Step 2: Insert 3 servicios_extra rows
        val rows = listOf(
            Triple("serv-1", "Reparación armazón", 50.0),
            Triple("serv-2", "Lente de sol", 120.0),
            Triple("serv-3", "Accesorio", 25.0),
        )
        rows.forEach { (id, desc, monto) ->
            v28Db.execSQL(
                "INSERT INTO servicios_extra (id, ot, descripcion, montoTotal, aCuenta, estado, fecha, pacienteId, metodoPago, opticaId) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                arrayOf<Any>(id, "ot-$id", desc, monto, monto / 2.0, "Pendiente", "2026-06-15", "paciente-1", "Efectivo", "optica-test"),
            )
        }

        val countCursor = v28Db.query("SELECT COUNT(*) FROM servicios_extra")
        assertTrue(countCursor.moveToFirst())
        assertEquals(3, countCursor.getInt(0))
        countCursor.close()

        v28Helper.close()

        // Step 3: Open at v29 and run MIGRATION_28_29
        val v29Config = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(dbName)
            .callback(object : SupportSQLiteOpenHelper.Callback(29) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    // Should not be called — DB already exists at v28
                }

                override fun onUpgrade(
                    db: SupportSQLiteDatabase,
                    oldVersion: Int,
                    newVersion: Int,
                ) {
                    if (oldVersion == 28 && newVersion == 29) {
                        MIGRATION_28_29.migrate(db)
                    }
                }
            })
            .build()

        val v29Helper = factory.create(v29Config)
        val v29Db = v29Helper.writableDatabase

        // Step 4: Verify all 3 rows preserved with fecha_entrega NULL
        val allRowsCursor = v29Db.query("SELECT id, descripcion, montoTotal, fecha_entrega FROM servicios_extra ORDER BY id")
        val surviving = mutableListOf<Array<Any?>>()
        while (allRowsCursor.moveToNext()) {
            surviving.add(
                arrayOf(
                    allRowsCursor.getString(allRowsCursor.getColumnIndexOrThrow("id")),
                    allRowsCursor.getString(allRowsCursor.getColumnIndexOrThrow("descripcion")),
                    allRowsCursor.getDouble(allRowsCursor.getColumnIndexOrThrow("montoTotal")),
                    allRowsCursor.getString(allRowsCursor.getColumnIndexOrThrow("fecha_entrega")),
                ),
            )
        }
        allRowsCursor.close()

        assertEquals("All 3 rows must survive migration", 3, surviving.size)

        val firstRow = surviving[0]
        assertEquals("serv-1", firstRow[0])
        assertEquals("Reparación armazón", firstRow[1])
        assertEquals(50.0, firstRow[2])
        assertNull("fecha_entrega must be NULL for existing rows", firstRow[3])

        val lastRow = surviving[2]
        assertEquals("serv-3", lastRow[0])
        assertEquals("Accesorio", lastRow[1])
        assertEquals(25.0, lastRow[2])
        assertNull("fecha_entrega must be NULL for existing rows", lastRow[3])

        v29Helper.close()
        context.deleteDatabase(dbName)
    }

    /**
     * Scenario: "Fresh insert includes fecha_entrega".
     *
     * After migration, a fresh INSERT that supplies `fecha_entrega` must persist
     * the provided date.
     */
    @Test
    fun migration_28_29_allows_fresh_insert_with_fecha_entrega() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dbName = "migration-28to29-fresh-insert-test.db"
        context.deleteDatabase(dbName)

        val factory = FrameworkSQLiteOpenHelperFactory()

        // Create v28 with empty servicios_extra
        val v28Config = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(dbName)
            .callback(object : SupportSQLiteOpenHelper.Callback(28) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS servicios_extra (
                            id TEXT NOT NULL PRIMARY KEY,
                            ot TEXT NOT NULL,
                            descripcion TEXT NOT NULL,
                            montoTotal REAL NOT NULL,
                            aCuenta REAL NOT NULL,
                            estado TEXT NOT NULL,
                            fecha TEXT NOT NULL,
                            pacienteId TEXT,
                            metodoPago TEXT NOT NULL,
                            opticaId TEXT NOT NULL,
                            updatedAt TEXT,
                            updatedBy TEXT
                        )
                        """.trimIndent(),
                    )
                }

                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
            })
            .build()
        val v28Helper = factory.create(v28Config)
        v28Helper.writableDatabase.execSQL(
            "INSERT INTO servicios_extra (id, ot, descripcion, montoTotal, aCuenta, estado, fecha, pacienteId, metodoPago, opticaId) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
            arrayOf<Any>("fresh-1", "ot-1", "Servicio v28", 80.0, 40.0, "Entregado", "2026-06-20", "paciente-1", "Tarjeta", "optica-x"),
        )
        v28Helper.close()

        // Migrate to v29
        val v29Config = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(dbName)
            .callback(object : SupportSQLiteOpenHelper.Callback(29) {
                override fun onCreate(db: SupportSQLiteDatabase) {}
                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {
                    if (oldVersion == 28 && newVersion == 29) MIGRATION_28_29.migrate(db)
                }
            })
            .build()
        val v29Helper = factory.create(v29Config)
        val v29Db = v29Helper.writableDatabase

        // Insert a fresh row supplying fecha_entrega
        v29Db.execSQL(
            "INSERT INTO servicios_extra (id, ot, descripcion, montoTotal, aCuenta, estado, fecha, pacienteId, metodoPago, opticaId, fecha_entrega) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
            arrayOf<Any>("fresh-2", "ot-2", "Servicio v29", 99.0, 99.0, "Entregado", "2026-07-01", "paciente-2", "Efectivo", "optica-x", "2026-07-01"),
        )

        // Verify the fresh row has the provided fecha_entrega
        val cursor = v29Db.query("SELECT fecha_entrega FROM servicios_extra WHERE id = 'fresh-2'")
        assertTrue(cursor.moveToFirst())
        assertEquals("2026-07-01", cursor.getString(0))
        cursor.close()

        v29Helper.close()
        context.deleteDatabase(dbName)
    }

    /**
     * Scenario: "Migration on empty database".
     *
     * Migration on a v28 DB with zero servicios_extra rows must succeed —
     * the new column must be present and the table empty.
     */
    @Test
    fun migration_28_29_on_empty_table_succeeds() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dbName = "migration-28to29-empty-test.db"
        context.deleteDatabase(dbName)

        val factory = FrameworkSQLiteOpenHelperFactory()

        val v28Config = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(dbName)
            .callback(object : SupportSQLiteOpenHelper.Callback(28) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS servicios_extra (
                            id TEXT NOT NULL PRIMARY KEY,
                            ot TEXT NOT NULL,
                            descripcion TEXT NOT NULL,
                            montoTotal REAL NOT NULL,
                            aCuenta REAL NOT NULL,
                            estado TEXT NOT NULL,
                            fecha TEXT NOT NULL,
                            pacienteId TEXT,
                            metodoPago TEXT NOT NULL,
                            opticaId TEXT NOT NULL,
                            updatedAt TEXT,
                            updatedBy TEXT
                        )
                        """.trimIndent(),
                    )
                }

                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
            })
            .build()
        val v28Helper = factory.create(v28Config)
        v28Helper.writableDatabase
        v28Helper.close()

        val v29Config = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(dbName)
            .callback(object : SupportSQLiteOpenHelper.Callback(29) {
                override fun onCreate(db: SupportSQLiteDatabase) {}
                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {
                    if (oldVersion == 28 && newVersion == 29) MIGRATION_28_29.migrate(db)
                }
            })
            .build()
        val v29Helper = factory.create(v29Config)
        val v29Db = v29Helper.writableDatabase

        // After migration: table exists, 0 rows, fecha_entrega queryable
        val countCursor = v29Db.query("SELECT COUNT(*) FROM servicios_extra")
        assertTrue(countCursor.moveToFirst())
        assertEquals(0, countCursor.getInt(0))
        countCursor.close()

        // Inserting a row without fecha_entrega must work (column is nullable)
        v29Db.execSQL(
            "INSERT INTO servicios_extra (id, ot, descripcion, montoTotal, aCuenta, estado, fecha, pacienteId, metodoPago, opticaId) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
            arrayOf<Any>("empty-1", "ot-1", "Servicio vacío", 10.0, 5.0, "Pendiente", "2026-06-30", "paciente-1", "Efectivo", "optica-y"),
        )
        val rowCursor = v29Db.query("SELECT fecha_entrega FROM servicios_extra WHERE id = 'empty-1'")
        assertTrue(rowCursor.moveToFirst())
        assertNull(rowCursor.getString(0))
        rowCursor.close()

        v29Helper.close()
        context.deleteDatabase(dbName)
    }
}
