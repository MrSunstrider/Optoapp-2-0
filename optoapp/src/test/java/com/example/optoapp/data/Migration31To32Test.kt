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
 * GREEN test: MIGRATION_31_32 creates 4 new tables, seeds categorias_producto,
 * and adds categoriaProductoId column to ventas.
 *
 * Uses the real MIGRATION_31_32 from OptoDatabaseMigrations.kt.
 */

@RunWith(RobolectricTestRunner::class)
class Migration31To32Test {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext<Context>()
    }

    @After
    fun tearDown() {
        // Clean up any test databases
        context.deleteDatabase("migration-31to32-test.db")
        context.deleteDatabase("migration-31to32-col-test.db")
    }

    @Test
    fun migration31to32_createsNewTables() {
        val dbName = "migration-31to32-test.db"
        context.deleteDatabase(dbName)
        val factory = FrameworkSQLiteOpenHelperFactory()

        // ── Step 1: Create v31 database with ventas table ──
        val v31Config = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(dbName)
            .callback(object : SupportSQLiteOpenHelper.Callback(31) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS ventas (
                            id TEXT NOT NULL PRIMARY KEY,
                            opticaId TEXT NOT NULL,
                            origen TEXT NOT NULL,
                            origenId TEXT NOT NULL,
                            pacienteId TEXT NOT NULL DEFAULT '',
                            fecha TEXT NOT NULL,
                            fechaEntrega TEXT,
                            montoTotal REAL NOT NULL,
                            costoUnitarioSnapshot REAL,
                            estado TEXT NOT NULL,
                            createdAt TEXT,
                            updatedAt TEXT,
                            updatedBy TEXT,
                            ot TEXT NOT NULL DEFAULT ''
                        )
                        """.trimIndent()
                    )
                    db.execSQL("CREATE INDEX IF NOT EXISTS index_ventas_opticaId ON ventas(opticaId)")
                    db.execSQL("CREATE INDEX IF NOT EXISTS index_ventas_origen_origenId ON ventas(origen, origenId)")

                    // Insert a test venta row
                    db.execSQL(
                        "INSERT INTO ventas (id, opticaId, origen, origenId, pacienteId, fecha, montoTotal, estado) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                        arrayOf<Any>("v_test1", "optica1", "dispensacion", "disp1", "p1", "2026-07-01", 150.0, "Pendiente")
                    )
                }

                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
            })
            .build()

        val v31Helper = factory.create(v31Config)
        val v31Db = v31Helper.writableDatabase

        // Verify venta exists before migration
        val preCount = v31Db.query("SELECT COUNT(*) FROM ventas")
        assertTrue(preCount.moveToFirst())
        assertEquals(1, preCount.getInt(0))
        preCount.close()

        v31Helper.close()

        // ── Step 2: Run MIGRATION_31_32_STUB ──
        val v32Config = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(dbName)
            .callback(object : SupportSQLiteOpenHelper.Callback(32) {
                override fun onCreate(db: SupportSQLiteDatabase) {}
                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {
                    if (oldVersion == 31 && newVersion == 32) {
                        MIGRATION_31_32.migrate(db)
                    }
                }
            })
            .build()

        val v32Helper = factory.create(v32Config)
        val v32Db = v32Helper.writableDatabase

        // ── Step 3: Assert new tables exist ──
        val catCount = v32Db.query("SELECT COUNT(*) FROM categorias_producto")
        assertTrue("categorias_producto table should exist after migration", catCount.moveToFirst())
        assertEquals("categorias_producto should have 9 seed rows", 9, catCount.getInt(0))
        catCount.close()

        val gastoCount = v32Db.query("SELECT COUNT(*) FROM gastos_operativos")
        assertTrue("gastos_operativos table should exist after migration", gastoCount.moveToFirst())
        assertEquals(0, gastoCount.getInt(0))
        gastoCount.close()

        val resumenCount = v32Db.query("SELECT COUNT(*) FROM resumen_diario")
        assertTrue("resumen_diario table should exist after migration", resumenCount.moveToFirst())
        assertEquals(0, resumenCount.getInt(0))
        resumenCount.close()

        val configCount = v32Db.query("SELECT COUNT(*) FROM configuracion_financiera")
        assertTrue("configuracion_financiera table should exist", configCount.moveToFirst())
        assertEquals(0, configCount.getInt(0))
        configCount.close()

        // Existing venta still intact
        val postCount = v32Db.query("SELECT COUNT(*) FROM ventas")
        assertTrue(postCount.moveToFirst())
        assertEquals(1, postCount.getInt(0))
        postCount.close()

        v32Helper.close()
    }

    @Test
    fun migration31to32_addsCategoriaColumnToVentas() {
        val dbName = "migration-31to32-col-test.db"
        context.deleteDatabase(dbName)
        val factory = FrameworkSQLiteOpenHelperFactory()

        // ── Step 1: Create v31 with ventas table ──
        val v31Config = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(dbName)
            .callback(object : SupportSQLiteOpenHelper.Callback(31) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS ventas (
                            id TEXT NOT NULL PRIMARY KEY,
                            opticaId TEXT NOT NULL,
                            origen TEXT NOT NULL,
                            origenId TEXT NOT NULL,
                            pacienteId TEXT NOT NULL DEFAULT '',
                            fecha TEXT NOT NULL,
                            fechaEntrega TEXT,
                            montoTotal REAL NOT NULL,
                            costoUnitarioSnapshot REAL,
                            estado TEXT NOT NULL,
                            createdAt TEXT,
                            updatedAt TEXT,
                            updatedBy TEXT,
                            ot TEXT NOT NULL DEFAULT ''
                        )
                        """.trimIndent()
                    )
                    db.execSQL(
                        "INSERT INTO ventas (id, opticaId, origen, origenId, pacienteId, fecha, montoTotal, estado) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                        arrayOf<Any>("v_test1", "optica1", "dispensacion", "disp1", "p1", "2026-07-01", 150.0, "Pendiente")
                    )
                }

                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
            })
            .build()

        val v31Helper = factory.create(v31Config)
        v31Helper.writableDatabase
        v31Helper.close()

        // ── Step 2: Run MIGRATION_31_32_STUB ──
        val v32Config = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(dbName)
            .callback(object : SupportSQLiteOpenHelper.Callback(32) {
                override fun onCreate(db: SupportSQLiteDatabase) {}
                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {
                    if (oldVersion == 31 && newVersion == 32) {
                        MIGRATION_31_32.migrate(db)
                    }
                }
            })
            .build()

        val v32Helper = factory.create(v32Config)
        val v32Db = v32Helper.writableDatabase

        // ── Step 3: Assert categoriaProductoId column exists ──
        // This query will throw android.database.sqlite.SQLiteException.
        val cursor = v32Db.query("SELECT id, categoriaProductoId FROM ventas WHERE id = 'v_test1'")
        assertTrue(cursor.moveToFirst())
        assertEquals("v_test1", cursor.getString(cursor.getColumnIndexOrThrow("id")))
        // categoriaProductoId should be NULL for existing rows
        assertTrue(cursor.isNull(cursor.getColumnIndexOrThrow("categoriaProductoId")))
        cursor.close()

        v32Helper.close()
    }
}
