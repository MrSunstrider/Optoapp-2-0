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

@RunWith(RobolectricTestRunner::class)
class MIGRATION_33_34_Test {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext<Context>()
    }

    @After
    fun tearDown() {
        context.deleteDatabase("migration-33to34-test.db")
    }

    @Test
    fun migration33to34_preservesRowsAndCreatesTable() {
        val dbName = "migration-33to34-test.db"
        context.deleteDatabase(dbName)
        val factory = FrameworkSQLiteOpenHelperFactory()

        // Step 1: Create v33 database with existing tables
        val v33Config = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(dbName)
            .callback(object : SupportSQLiteOpenHelper.Callback(33) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS configuracion_financiera (
                            opticaId TEXT PRIMARY KEY NOT NULL,
                            margenNetoObjetivo REAL NOT NULL DEFAULT 15.0,
                            ticketPromedioObjetivo REAL,
                            caidaVentasAlertaPct REAL NOT NULL DEFAULT 10.0,
                            deudaViejaAlertaDias INTEGER NOT NULL DEFAULT 30,
                            deudaTotalAlertaMonto REAL NOT NULL DEFAULT 3000.0,
                            stockEstancadoAlertaDias INTEGER NOT NULL DEFAULT 180,
                            stockBajoAlertaUnidades INTEGER NOT NULL DEFAULT 2,
                            minVentasParaRecomendar INTEGER NOT NULL DEFAULT 5,
                            frecuenciaRecalculoDias INTEGER NOT NULL DEFAULT 1
                        )
                        """.trimIndent(),
                    )
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
                            ot TEXT NOT NULL DEFAULT '',
                            categoriaProductoId TEXT
                        )
                        """.trimIndent(),
                    )

                    // Insert test rows
                    db.execSQL(
                        "INSERT INTO configuracion_financiera (opticaId, margenNetoObjetivo, deudaViejaAlertaDias) VALUES (?, ?, ?)",
                        arrayOf<Any>("optica1", 15.0, 30),
                    )
                    db.execSQL(
                        "INSERT INTO configuracion_financiera (opticaId, margenNetoObjetivo, deudaViejaAlertaDias) VALUES (?, ?, ?)",
                        arrayOf<Any>("optica2", 20.0, 45),
                    )
                    db.execSQL(
                        "INSERT INTO ventas (id, opticaId, origen, origenId, fecha, montoTotal, estado) VALUES (?, ?, ?, ?, ?, ?, ?)",
                        arrayOf<Any>("v1", "optica1", "disp", "d1", "2026-07-01", 100.0, "completada"),
                    )
                }

                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
            })
            .build()

        val v33Helper = factory.create(v33Config)
        val v33Db = v33Helper.writableDatabase

        // Verify rows exist before migration
        val preConfig = v33Db.query("SELECT COUNT(*) FROM configuracion_financiera")
        assertTrue(preConfig.moveToFirst())
        assertEquals(2, preConfig.getInt(0))
        preConfig.close()

        val preVentas = v33Db.query("SELECT COUNT(*) FROM ventas")
        assertTrue(preVentas.moveToFirst())
        assertEquals(1, preVentas.getInt(0))
        preVentas.close()

        v33Helper.close()

        // Step 2: Run MIGRATION_33_34
        val v34Config = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(dbName)
            .callback(object : SupportSQLiteOpenHelper.Callback(34) {
                override fun onCreate(db: SupportSQLiteDatabase) {}
                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {
                    if (oldVersion == 33 && newVersion == 34) {
                        MIGRATION_33_34.migrate(db)
                    }
                }
            })
            .build()

        val v34Helper = factory.create(v34Config)
        val v34Db = v34Helper.writableDatabase

        // Step 3: Assert existing rows preserved
        val postConfig = v34Db.query("SELECT COUNT(*) FROM configuracion_financiera")
        assertTrue(postConfig.moveToFirst())
        assertEquals(2, postConfig.getInt(0))
        postConfig.close()

        val postVentas = v34Db.query("SELECT COUNT(*) FROM ventas")
        assertTrue(postVentas.moveToFirst())
        assertEquals(1, postVentas.getInt(0))
        postVentas.close()

        // Step 4: Assert feedback_recomendaciones table exists
        val fbTable = v34Db.query("SELECT name FROM sqlite_master WHERE type='table' AND name='feedback_recomendaciones'")
        assertTrue(fbTable.moveToFirst())
        assertEquals("feedback_recomendaciones", fbTable.getString(0))
        fbTable.close()

        // Step 5: Assert inserting into feedback_recomendaciones works
        v34Db.execSQL(
            "INSERT INTO feedback_recomendaciones (recomendacionId, opticaId, fueUtil, fecha) VALUES (?, ?, ?, ?)",
            arrayOf<Any>("rec-abc", "optica1", 1, System.currentTimeMillis()),
        )
        val fbRow = v34Db.query("SELECT * FROM feedback_recomendaciones WHERE recomendacionId = 'rec-abc'")
        assertTrue(fbRow.moveToFirst())
        assertTrue(fbRow.getInt(fbRow.getColumnIndexOrThrow("fueUtil")) == 1)
        fbRow.close()

        v34Helper.close()
    }
}
