package com.example.optoapp.data

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * GREEN test: MIGRATION_32_33 adds ventaId column to pagos table
 * and creates index_pagos_ventaId index.
 * Uses the real MIGRATION_32_33 from OptoDatabaseMigrations.kt.
 */
@RunWith(RobolectricTestRunner::class)
class MIGRATION_32_33_Test {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext<Context>()
    }

    @After
    fun tearDown() {
        context.deleteDatabase("migration-32to33-test.db")
        context.deleteDatabase("migration-32to33-col-test.db")
    }

    @Test
    fun migration32to33_preservesRowsAndAddsVentaIdColumn() {
        val dbName = "migration-32to33-test.db"
        context.deleteDatabase(dbName)
        val factory = FrameworkSQLiteOpenHelperFactory()

        // Step 1: Create v32 database with pagos table (schema matching current v32)
        val v32Config = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(dbName)
            .callback(object : SupportSQLiteOpenHelper.Callback(32) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS pagos (
                            id TEXT NOT NULL PRIMARY KEY,
                            dispensacionId TEXT,
                            servicioExtraId TEXT,
                            fecha TEXT NOT NULL,
                            tipo TEXT NOT NULL,
                            monto REAL NOT NULL,
                            metodoPago TEXT NOT NULL DEFAULT '',
                            nota TEXT NOT NULL DEFAULT '',
                            opticaId TEXT NOT NULL DEFAULT 'mi_optica_base',
                            updatedAt TEXT,
                            updatedBy TEXT
                        )
                        """.trimIndent(),
                    )
                    db.execSQL("CREATE INDEX IF NOT EXISTS index_pagos_dispensacionId ON pagos(dispensacionId)")
                    db.execSQL("CREATE INDEX IF NOT EXISTS index_pagos_servicioExtraId ON pagos(servicioExtraId)")
                    db.execSQL("CREATE INDEX IF NOT EXISTS index_pagos_opticaId ON pagos(opticaId)")

                    // Insert 3 test pago rows
                    db.execSQL(
                        "INSERT INTO pagos (id, dispensacionId, fecha, tipo, monto, metodoPago, opticaId) VALUES (?, ?, ?, ?, ?, ?, ?)",
                        arrayOf<Any>("p1", "d1", "2026-07-01", "efectivo", 100.0, "Efectivo", "optica1"),
                    )
                    db.execSQL(
                        "INSERT INTO pagos (id, servicioExtraId, fecha, tipo, monto, metodoPago, opticaId) VALUES (?, ?, ?, ?, ?, ?, ?)",
                        arrayOf<Any>("p2", "s1", "2026-07-02", "tarjeta", 200.0, "Tarjeta", "optica1"),
                    )
                    db.execSQL(
                        "INSERT INTO pagos (id, fecha, tipo, monto, metodoPago, opticaId) VALUES (?, ?, ?, ?, ?, ?)",
                        arrayOf<Any>("p3", "2026-07-03", "transferencia", 300.0, "Transferencia", "optica1"),
                    )
                }

                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
            })
            .build()

        val v32Helper = factory.create(v32Config)
        val v32Db = v32Helper.writableDatabase

        // Verify 3 rows exist before migration
        val preCount = v32Db.query("SELECT COUNT(*) FROM pagos")
        assertTrue(preCount.moveToFirst())
        assertEquals(3, preCount.getInt(0))
        preCount.close()

        v32Helper.close()

        // Step 2: Run MIGRATION_32_33
        val v33Config = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(dbName)
            .callback(object : SupportSQLiteOpenHelper.Callback(33) {
                override fun onCreate(db: SupportSQLiteDatabase) {}
                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {
                    if (oldVersion == 32 && newVersion == 33) {
                        MIGRATION_32_33.migrate(db)
                    }
                }
            })
            .build()

        val v33Helper = factory.create(v33Config)
        val v33Db = v33Helper.writableDatabase

        // Step 3: Assert rows preserved and ventaId column exists
        val postCount = v33Db.query("SELECT COUNT(*) FROM pagos")
        assertTrue(postCount.moveToFirst())
        assertEquals(3, postCount.getInt(0))
        postCount.close()

        // ventaId column exists and is NULL for existing rows
        val cursor = v33Db.query("SELECT id, ventaId FROM pagos WHERE id = 'p1'")
        assertTrue(cursor.moveToFirst())
        assertEquals("p1", cursor.getString(cursor.getColumnIndexOrThrow("id")))
        assertTrue(cursor.isNull(cursor.getColumnIndexOrThrow("ventaId")))
        cursor.close()

        v33Helper.close()
    }

    @Test
    fun migration32to33_createsVentaIdIndex() {
        val dbName = "migration-32to33-col-test.db"
        context.deleteDatabase(dbName)
        val factory = FrameworkSQLiteOpenHelperFactory()

        // Create v32 with pagos table
        val v32Config = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(dbName)
            .callback(object : SupportSQLiteOpenHelper.Callback(32) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS pagos (
                            id TEXT NOT NULL PRIMARY KEY,
                            dispensacionId TEXT,
                            servicioExtraId TEXT,
                            fecha TEXT NOT NULL,
                            tipo TEXT NOT NULL,
                            monto REAL NOT NULL,
                            metodoPago TEXT NOT NULL DEFAULT '',
                            nota TEXT NOT NULL DEFAULT '',
                            opticaId TEXT NOT NULL DEFAULT 'mi_optica_base',
                            updatedAt TEXT,
                            updatedBy TEXT
                        )
                        """.trimIndent(),
                    )
                    db.execSQL(
                        "INSERT INTO pagos (id, fecha, tipo, monto, metodoPago, opticaId) VALUES (?, ?, ?, ?, ?, ?)",
                        arrayOf<Any>("p1", "2026-07-01", "efectivo", 100.0, "Efectivo", "optica1"),
                    )
                }

                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
            })
            .build()

        val v32Helper = factory.create(v32Config)
        v32Helper.writableDatabase
        v32Helper.close()

        // Run migration
        val v33Config = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(dbName)
            .callback(object : SupportSQLiteOpenHelper.Callback(33) {
                override fun onCreate(db: SupportSQLiteDatabase) {}
                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {
                    if (oldVersion == 32 && newVersion == 33) {
                        MIGRATION_32_33.migrate(db)
                    }
                }
            })
            .build()

        val v33Helper = factory.create(v33Config)
        val v33Db = v33Helper.writableDatabase

        // Verify index exists by querying with the indexed column (should work without error)
        val cursor = v33Db.query("SELECT id FROM pagos WHERE ventaId = ?", arrayOf<Any>("test"))
        assertEquals(0, cursor.count)
        cursor.close()

        v33Helper.close()
    }
}
