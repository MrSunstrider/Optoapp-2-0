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
 * Tests for MIGRATION_38_39 which:
 * - Creates costos_productos matrix table
 * - Creates costos_biselado table
 * - ALTERs dispensaciones (adds evaluacion_id)
 * - ALTERs dispensacion_items (adds 9 spec/cost columns)
 *
 * CRITICAL: Every v38 schema MUST include dispensaciones AND dispensacion_items
 * because the migration ALTERs both tables. In production v38, both tables always exist.
 */
@RunWith(RobolectricTestRunner::class)
class Migration38_39Test {

    private lateinit var context: Context

    private companion object {
        /** Minimal tables that must exist in v38 for the migration's ALTER TABLE to succeed. */
        const val V38_TABLES_SQL = """
            CREATE TABLE IF NOT EXISTS dispensaciones (
                id TEXT NOT NULL PRIMARY KEY,
                pacienteId TEXT NOT NULL,
                fecha TEXT NOT NULL,
                opticaId TEXT NOT NULL DEFAULT 'mi_optica_base',
                montoTotal REAL NOT NULL DEFAULT 0.0,
                updatedAt TEXT,
                updatedBy TEXT
            );
            CREATE TABLE IF NOT EXISTS dispensacion_items (
                id TEXT NOT NULL PRIMARY KEY,
                dispensacion_id TEXT NOT NULL,
                tipo_lente TEXT NOT NULL DEFAULT '',
                material_lente TEXT NOT NULL DEFAULT '',
                optica_id TEXT NOT NULL DEFAULT 'mi_optica_base'
            );
        """
    }

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext<Context>()
    }

    @After
    fun tearDown() {
        context.deleteDatabase("migration-38to39-test.db")
        context.deleteDatabase("migration-38to39-data-test.db")
        context.deleteDatabase("migration-38to39-items-test.db")
    }
    private fun openV39(dbName: String, v38OnCreate: (SupportSQLiteDatabase) -> Unit): SupportSQLiteDatabase {
        context.deleteDatabase(dbName)
        val factory = FrameworkSQLiteOpenHelperFactory()

        val v38Config = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(dbName)
            .callback(object : SupportSQLiteOpenHelper.Callback(38) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    v38OnCreate(db)
                }
                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
            })
            .build()
        val v38Helper = factory.create(v38Config)
        v38Helper.writableDatabase // force creation
        v38Helper.close()

        val v39Config = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(dbName)
            .callback(object : SupportSQLiteOpenHelper.Callback(39) {
                override fun onCreate(db: SupportSQLiteDatabase) {}
                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {
                    if (oldVersion == 38 && newVersion == 39) {
                        MIGRATION_38_39.migrate(db)
                    }
                }
            })
            .build()
        return factory.create(v39Config).writableDatabase
    }
    // Test 1: costos_productos matrix table
    @Test
    fun migration38to39_createsCostosProductosTable() {
        val db = openV39("migration-38to39-test.db") { d ->
            // In real v38 Room DB, costos_productos does NOT exist as a Room entity.
            // Only create the tables the migration will ALTER.
            V38_TABLES_SQL.split(";").filter { it.isNotBlank() }.forEach { d.execSQL(it.trim()) }
        }

        // costos_productos table was created by the migration
        val tc = db.query("SELECT name FROM sqlite_master WHERE type='table' AND name='costos_productos'")
        assertTrue(tc.moveToFirst())
        assertEquals("costos_productos", tc.getString(0))
        tc.close()

        // Verify matrix columns
        val pc = db.query("PRAGMA table_info(costos_productos)")
        val cols = mutableSetOf<String>()
        while (pc.moveToNext()) cols.add(pc.getString(pc.getColumnIndexOrThrow("name")))
        pc.close()

        listOf(
            "material", "tipo_lente", "stock_o_fabricacion", "tratamiento",
            "serie", "costo_unitario", "laboratorio_id", "vigente_desde", "vigente_hasta",
        ).forEach { assertTrue("$it column", cols.contains(it)) }

        db.close()
    }
    // Test 2: costos_biselado table
    @Test
    fun migration38to39_createsCostosBiseladoTable() {
        val db = openV39("migration-38to39-test.db") { d ->
            V38_TABLES_SQL.split(";").filter { it.isNotBlank() }.forEach { d.execSQL(it.trim()) }
        }

        val tc = db.query("SELECT name FROM sqlite_master WHERE type='table' AND name='costos_biselado'")
        assertTrue(tc.moveToFirst())
        assertEquals("costos_biselado", tc.getString(0))
        tc.close()

        val pc = db.query("PRAGMA table_info(costos_biselado)")
        val cols = mutableSetOf<String>()
        while (pc.moveToNext()) cols.add(pc.getString(pc.getColumnIndexOrThrow("name")))
        pc.close()

        listOf(
            "material", "tipo_aro", "stock_o_fabricacion", "serie",
            "alto_indice", "costo_por_par", "proveedor", "vigente_desde", "vigente_hasta",
        ).forEach { assertTrue("$it column", cols.contains(it)) }

        db.close()
    }
    // Test 3: evaluacion_id on dispensaciones + data preserved
    @Test
    fun migration38to39_addsEvaluacionIdToDispensacionesAndPreservesData() {
        val dbName = "migration-38to39-data-test.db"
        context.deleteDatabase(dbName)
        val factory = FrameworkSQLiteOpenHelperFactory()

        val v38Config = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(dbName)
            .callback(object : SupportSQLiteOpenHelper.Callback(38) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    // Full dispensaciones table with data
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS dispensaciones (
                            id TEXT NOT NULL PRIMARY KEY,
                            ot TEXT NOT NULL DEFAULT '',
                            pacienteId TEXT NOT NULL,
                            fecha TEXT NOT NULL,
                            opticaId TEXT NOT NULL DEFAULT 'mi_optica_base',
                            montoTotal REAL NOT NULL DEFAULT 0.0,
                            montoPagado REAL NOT NULL DEFAULT 0.0,
                            estadoEntrega TEXT NOT NULL DEFAULT 'Pendiente',
                            updatedAt TEXT,
                            updatedBy TEXT
                        )
                        """.trimIndent(),
                    )
                    db.execSQL(
                        """
                        INSERT INTO dispensaciones (id, pacienteId, fecha, opticaId, montoTotal)
                        VALUES ('disp1', 'p1', '2026-07-01', 'optica1', 350.0)
                    """,
                    )
                    // Also need dispensacion_items for the ALTER TABLE in migration
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS dispensacion_items (
                            id TEXT NOT NULL PRIMARY KEY,
                            dispensacion_id TEXT NOT NULL,
                            tipo_lente TEXT NOT NULL DEFAULT '',
                            material_lente TEXT NOT NULL DEFAULT '',
                            optica_id TEXT NOT NULL DEFAULT 'mi_optica_base'
                        )
                        """.trimIndent(),
                    )
                }
                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
            })
            .build()
        val v38Helper = factory.create(v38Config)

        // Verify pre-migration state
        val v38Db = v38Helper.writableDatabase
        val preCount = v38Db.query("SELECT COUNT(*) FROM dispensaciones")
        assertTrue(preCount.moveToFirst())
        assertEquals(1, preCount.getInt(0))
        preCount.close()
        val preCols = v38Db.query("PRAGMA table_info(dispensaciones)")
        var hasEvalBefore = false
        while (preCols.moveToNext()) {
            if (preCols.getString(preCols.getColumnIndexOrThrow("name")) == "evaluacion_id") hasEvalBefore = true
        }
        preCols.close()
        assertTrue("evaluacion_id should NOT exist before migration", !hasEvalBefore)
        v38Helper.close()

        // Run migration
        val v39Config = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(dbName)
            .callback(object : SupportSQLiteOpenHelper.Callback(39) {
                override fun onCreate(db: SupportSQLiteDatabase) {}
                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {
                    if (oldVersion == 38 && newVersion == 39) MIGRATION_38_39.migrate(db)
                }
            })
            .build()
        val db = factory.create(v39Config).writableDatabase

        // Assert evaluacion_id exists
        val postCols = db.query("PRAGMA table_info(dispensaciones)")
        val names = mutableSetOf<String>()
        while (postCols.moveToNext()) names.add(postCols.getString(postCols.getColumnIndexOrThrow("name")))
        postCols.close()
        assertTrue("evaluacion_id should exist after migration", names.contains("evaluacion_id"))

        // Assert data preserved
        val c = db.query("SELECT id, montoTotal, evaluacion_id FROM dispensaciones WHERE id = 'disp1'")
        assertTrue(c.moveToFirst())
        assertEquals("disp1", c.getString(c.getColumnIndexOrThrow("id")))
        assertEquals(350.0, c.getDouble(c.getColumnIndexOrThrow("montoTotal")), 0.001)
        assertTrue(c.isNull(c.getColumnIndexOrThrow("evaluacion_id")))
        c.close()
        db.close()
    }
    // Test 4: 9 new columns on dispensacion_items
    @Test
    fun migration38to39_addsColumnsToDispensacionItems() {
        val dbName = "migration-38to39-items-test.db"
        context.deleteDatabase(dbName)
        val factory = FrameworkSQLiteOpenHelperFactory()

        val v38Config = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(dbName)
            .callback(object : SupportSQLiteOpenHelper.Callback(38) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    // dispensaciones must exist (migration ALTERs it)
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS dispensaciones (
                            id TEXT NOT NULL PRIMARY KEY,
                            pacienteId TEXT NOT NULL,
                            fecha TEXT NOT NULL,
                            opticaId TEXT NOT NULL DEFAULT 'mi_optica_base',
                            montoTotal REAL NOT NULL DEFAULT 0.0,
                            updatedAt TEXT,
                            updatedBy TEXT
                        )
                        """.trimIndent(),
                    )
                    // Full dispensacion_items table with data
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS dispensacion_items (
                            id TEXT NOT NULL PRIMARY KEY,
                            dispensacion_id TEXT NOT NULL,
                            tipo_lente TEXT NOT NULL DEFAULT '',
                            material_lente TEXT NOT NULL DEFAULT '',
                            optica_id TEXT NOT NULL DEFAULT 'mi_optica_base'
                        )
                        """.trimIndent(),
                    )
                    db.execSQL(
                        """
                        INSERT INTO dispensacion_items (id, dispensacion_id, tipo_lente, material_lente, optica_id)
                        VALUES ('item1', 'disp1', 'Monofocal', 'Resina', 'optica1')
                    """,
                    )
                }
                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
            })
            .build()
        factory.create(v38Config).writableDatabase.close()

        // Run migration
        val v39Config = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(dbName)
            .callback(object : SupportSQLiteOpenHelper.Callback(39) {
                override fun onCreate(db: SupportSQLiteDatabase) {}
                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {
                    if (oldVersion == 38 && newVersion == 39) MIGRATION_38_39.migrate(db)
                }
            })
            .build()
        val db = factory.create(v39Config).writableDatabase

        // Assert new columns exist
        val pc = db.query("PRAGMA table_info(dispensacion_items)")
        val cols = mutableSetOf<String>()
        while (pc.moveToNext()) cols.add(pc.getString(pc.getColumnIndexOrThrow("name")))
        pc.close()
        listOf(
            "alto_indice", "reduccion_diametro", "lenticular", "curva_base",
            "costo_real_od", "costo_real_oi", "costo_real_montura", "costo_real_biselado", "costo_real_lc",
        ).forEach { assertTrue("$it column", cols.contains(it)) }

        // Assert data preserved
        val dc = db.query("SELECT id, tipo_lente FROM dispensacion_items WHERE id = 'item1'")
        assertTrue(dc.moveToFirst())
        assertEquals("item1", dc.getString(dc.getColumnIndexOrThrow("id")))
        assertEquals("Monofocal", dc.getString(dc.getColumnIndexOrThrow("tipo_lente")))
        dc.close()

        // Assert new columns nullable
        val nc = db.query("SELECT costo_real_od, costo_real_oi FROM dispensacion_items WHERE id = 'item1'")
        assertTrue(nc.moveToFirst())
        assertTrue(nc.isNull(nc.getColumnIndexOrThrow("costo_real_od")))
        assertTrue(nc.isNull(nc.getColumnIndexOrThrow("costo_real_oi")))
        nc.close()
        db.close()
    }
}
