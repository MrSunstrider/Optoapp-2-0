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
 * RED test: MIGRATION_37_38 creates regalos_dispensacion table,
 * adds reclamoOrigenId column to dispensaciones, and preserves existing data.
 *
 * Uses the real MIGRATION_37_38 from OptoDatabaseMigrations.kt.
 */
@RunWith(RobolectricTestRunner::class)
class Migration37_38Test {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext<Context>()
    }

    @After
    fun tearDown() {
        context.deleteDatabase("migration-37to38-test.db")
        context.deleteDatabase("migration-37to38-data-test.db")
    }

    @Test
    fun migration37to38_createsRegalosDispensacionTable() {
        val dbName = "migration-37to38-test.db"
        context.deleteDatabase(dbName)
        val factory = FrameworkSQLiteOpenHelperFactory()
        val v37Config = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(dbName)
            .callback(object : SupportSQLiteOpenHelper.Callback(37) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS dispensaciones (
                            id TEXT NOT NULL PRIMARY KEY,
                            ot TEXT NOT NULL DEFAULT '',
                            monturaId TEXT NOT NULL DEFAULT '',
                            pacienteId TEXT NOT NULL,
                            fecha TEXT NOT NULL,
                            opticaId TEXT NOT NULL DEFAULT 'mi_optica_base',
                            tipoMontura TEXT NOT NULL DEFAULT '',
                            materialMontura TEXT NOT NULL DEFAULT '',
                            tipoLente TEXT NOT NULL DEFAULT '',
                            materialLente TEXT NOT NULL DEFAULT '',
                            tratamientos TEXT NOT NULL DEFAULT '[]',
                            colorLente TEXT NOT NULL DEFAULT '',
                            notasDiseno TEXT NOT NULL DEFAULT '',
                            origenMontura TEXT NOT NULL DEFAULT '',
                            tipoAro TEXT NOT NULL DEFAULT '',
                            descripcionMontura TEXT NOT NULL DEFAULT '',
                            montoTotal REAL NOT NULL DEFAULT 0.0,
                            metodoPago TEXT NOT NULL DEFAULT '',
                            montoPagado REAL NOT NULL DEFAULT 0.0,
                            estadoEntrega TEXT NOT NULL DEFAULT 'Pendiente',
                            fechaEntrega TEXT,
                            fechaVencimientoGarantia TEXT,
                            distanciaLente TEXT NOT NULL DEFAULT '',
                            altura TEXT NOT NULL DEFAULT '',
                            subTipoBifocal TEXT NOT NULL DEFAULT '',
                            filtro_discromatopsia_tipo TEXT NOT NULL DEFAULT '',
                            updatedAt TEXT,
                            updatedBy TEXT
                        )
                        """.trimIndent(),
                    )
                    db.execSQL("CREATE INDEX IF NOT EXISTS index_dispensaciones_pacienteId ON dispensaciones(pacienteId)")
                    db.execSQL("CREATE INDEX IF NOT EXISTS index_dispensaciones_opticaId ON dispensaciones(opticaId)")
                }

                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
            })
            .build()

        val v37Helper = factory.create(v37Config)
        v37Helper.writableDatabase
        v37Helper.close()
        val v38Config = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(dbName)
            .callback(object : SupportSQLiteOpenHelper.Callback(38) {
                override fun onCreate(db: SupportSQLiteDatabase) {}
                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {
                    if (oldVersion == 37 && newVersion == 38) {
                        MIGRATION_37_38.migrate(db)
                    }
                }
            })
            .build()

        val v38Helper = factory.create(v38Config)
        val v38Db = v38Helper.writableDatabase
        val tableCursor = v38Db.query(
            "SELECT name FROM sqlite_master WHERE type='table' AND name='regalos_dispensacion'",
        )
        assertTrue(tableCursor.moveToFirst())
        assertEquals("regalos_dispensacion", tableCursor.getString(0))
        tableCursor.close()

        // Verify regalos_dispensacion has expected columns
        val pragmaCursor = v38Db.query("PRAGMA table_info(regalos_dispensacion)")
        val columns = mutableSetOf<String>()
        while (pragmaCursor.moveToNext()) {
            columns.add(pragmaCursor.getString(pragmaCursor.getColumnIndexOrThrow("name")))
        }
        pragmaCursor.close()
        assertTrue("dispensacion_id column", columns.contains("dispensacion_id"))
        assertTrue("producto_id column", columns.contains("producto_id"))
        assertTrue("cantidad column", columns.contains("cantidad"))
        assertTrue("costo_unitario column", columns.contains("costo_unitario"))
        assertTrue("descripcion column", columns.contains("descripcion"))
        assertTrue("motivo column", columns.contains("motivo"))
        assertTrue("optica_id column", columns.contains("optica_id"))

        v38Helper.close()
    }

    @Test
    fun migration37to38_addsReclamoOrigenIdAndPreservesData() {
        val dbName = "migration-37to38-data-test.db"
        context.deleteDatabase(dbName)
        val factory = FrameworkSQLiteOpenHelperFactory()
        val v37Config = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(dbName)
            .callback(object : SupportSQLiteOpenHelper.Callback(37) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS dispensaciones (
                            id TEXT NOT NULL PRIMARY KEY,
                            ot TEXT NOT NULL DEFAULT '',
                            monturaId TEXT NOT NULL DEFAULT '',
                            pacienteId TEXT NOT NULL,
                            fecha TEXT NOT NULL,
                            opticaId TEXT NOT NULL DEFAULT 'mi_optica_base',
                            tipoMontura TEXT NOT NULL DEFAULT '',
                            materialMontura TEXT NOT NULL DEFAULT '',
                            tipoLente TEXT NOT NULL DEFAULT '',
                            materialLente TEXT NOT NULL DEFAULT '',
                            tratamientos TEXT NOT NULL DEFAULT '[]',
                            colorLente TEXT NOT NULL DEFAULT '',
                            notasDiseno TEXT NOT NULL DEFAULT '',
                            origenMontura TEXT NOT NULL DEFAULT '',
                            tipoAro TEXT NOT NULL DEFAULT '',
                            descripcionMontura TEXT NOT NULL DEFAULT '',
                            montoTotal REAL NOT NULL DEFAULT 0.0,
                            metodoPago TEXT NOT NULL DEFAULT '',
                            montoPagado REAL NOT NULL DEFAULT 0.0,
                            estadoEntrega TEXT NOT NULL DEFAULT 'Pendiente',
                            fechaEntrega TEXT,
                            fechaVencimientoGarantia TEXT,
                            distanciaLente TEXT NOT NULL DEFAULT '',
                            altura TEXT NOT NULL DEFAULT '',
                            subTipoBifocal TEXT NOT NULL DEFAULT '',
                            filtro_discromatopsia_tipo TEXT NOT NULL DEFAULT '',
                            updatedAt TEXT,
                            updatedBy TEXT
                        )
                        """.trimIndent(),
                    )

                    // Insert a test dispensacion
                    db.execSQL(
                        """INSERT INTO dispensaciones (id, pacienteId, fecha, opticaId, montoTotal, estadoEntrega)
                           VALUES (?, ?, ?, ?, ?, ?)""",
                        arrayOf<Any>("disp1", "p1", "2026-07-01", "optica1", 250.0, "Pendiente"),
                    )
                }

                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
            })
            .build()

        val v37Helper = factory.create(v37Config)
        val v37Db = v37Helper.writableDatabase

        // Verify data exists before migration
        val preCount = v37Db.query("SELECT COUNT(*) FROM dispensaciones")
        assertTrue(preCount.moveToFirst())
        assertEquals(1, preCount.getInt(0))
        preCount.close()

        // reclamo_origen_id should NOT exist before migration
        val preColumns = v37Db.query("PRAGMA table_info(dispensaciones)")
        var hasReclamoBefore = false
        while (preColumns.moveToNext()) {
            if (preColumns.getString(preColumns.getColumnIndexOrThrow("name")) == "reclamo_origen_id") {
                hasReclamoBefore = true
            }
        }
        preColumns.close()
        assertTrue("reclamo_origen_id should NOT exist before migration", !hasReclamoBefore)

        v37Helper.close()
        val v38Config = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(dbName)
            .callback(object : SupportSQLiteOpenHelper.Callback(38) {
                override fun onCreate(db: SupportSQLiteDatabase) {}
                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {
                    if (oldVersion == 37 && newVersion == 38) {
                        MIGRATION_37_38.migrate(db)
                    }
                }
            })
            .build()

        val v38Helper = factory.create(v38Config)
        val v38Db = v38Helper.writableDatabase
        val postColumns = v38Db.query("PRAGMA table_info(dispensaciones)")
        val columnNames = mutableSetOf<String>()
        while (postColumns.moveToNext()) {
            columnNames.add(postColumns.getString(postColumns.getColumnIndexOrThrow("name")))
        }
        postColumns.close()
        assertTrue("reclamo_origen_id should exist after migration", columnNames.contains("reclamo_origen_id"))
        val cursor = v38Db.query("SELECT id, montoTotal, reclamo_origen_id FROM dispensaciones WHERE id = 'disp1'")
        assertTrue(cursor.moveToFirst())
        assertEquals("disp1", cursor.getString(cursor.getColumnIndexOrThrow("id")))
        assertEquals(250.0, cursor.getDouble(cursor.getColumnIndexOrThrow("montoTotal")), 0.001)
        assertTrue(cursor.isNull(cursor.getColumnIndexOrThrow("reclamo_origen_id")))
        cursor.close()
        v38Db.execSQL(
            """INSERT INTO regalos_dispensacion (id, dispensacion_id, producto_id, cantidad, costo_unitario, descripcion, optica_id)
               VALUES (?, ?, ?, ?, ?, ?, ?)""",
            arrayOf<Any>("r1", "disp1", "prod1", 2, 50.0, "Estuche de regalo", "optica1"),
        )
        val regaloCount = v38Db.query("SELECT COUNT(*) FROM regalos_dispensacion")
        assertTrue(regaloCount.moveToFirst())
        assertEquals(1, regaloCount.getInt(0))
        regaloCount.close()

        // PRAGMA must be ON for FK enforcement (default OFF in raw SQLite)
        v38Db.execSQL("PRAGMA foreign_keys = ON")
        // Verify FK constraint works - delete dispensacion should cascade
        v38Db.execSQL("DELETE FROM dispensaciones WHERE id = 'disp1'")
        val afterCascade = v38Db.query("SELECT COUNT(*) FROM regalos_dispensacion")
        assertTrue(afterCascade.moveToFirst())
        assertEquals(0, afterCascade.getInt(0))
        afterCascade.close()

        v38Helper.close()
    }
}
