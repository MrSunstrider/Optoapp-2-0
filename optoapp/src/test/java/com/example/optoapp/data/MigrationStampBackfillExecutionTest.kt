package com.example.optoapp.data

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Executes stamp backfill migrations against real SQLite (not SQL-string mocks).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = android.app.Application::class)
class MigrationStampBackfillExecutionTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val dbName = "stamp-backfill-execution-test.db"

    @After
    fun tearDown() {
        context.deleteDatabase(dbName)
    }

    @Test
    fun migration_49_50_stamps_null_and_blank_proveedores_preserves_existing() {
        val helper = FrameworkSQLiteOpenHelperFactory().create(
            androidx.sqlite.db.SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(dbName)
                .callback(object : androidx.sqlite.db.SupportSQLiteOpenHelper.Callback(49) {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        db.execSQL(
                            """
                            CREATE TABLE proveedores (
                                id TEXT NOT NULL PRIMARY KEY,
                                nombre TEXT NOT NULL,
                                ruc TEXT NOT NULL,
                                telefono TEXT NOT NULL,
                                email TEXT NOT NULL,
                                direccion TEXT NOT NULL,
                                contacto TEXT NOT NULL,
                                activo INTEGER NOT NULL,
                                tipo TEXT NOT NULL,
                                opticaId TEXT NOT NULL,
                                updatedAt TEXT,
                                updatedBy TEXT
                            )
                            """.trimIndent(),
                        )
                    }

                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
                })
                .build(),
        )

        helper.writableDatabase.use { db ->
            db.execSQL(
                "INSERT INTO proveedores (id, nombre, ruc, telefono, email, direccion, contacto, activo, tipo, opticaId, updatedAt, updatedBy) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                arrayOf("p-null", "A", "1", "", "", "", "", 1, "monturas", "o1", null, null),
            )
            db.execSQL(
                "INSERT INTO proveedores (id, nombre, ruc, telefono, email, direccion, contacto, activo, tipo, opticaId, updatedAt, updatedBy) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                arrayOf("p-blank", "B", "2", "", "", "", "", 1, "monturas", "o1", "", null),
            )
            db.execSQL(
                "INSERT INTO proveedores (id, nombre, ruc, telefono, email, direccion, contacto, activo, tipo, opticaId, updatedAt, updatedBy) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                arrayOf("p-keep", "C", "3", "", "", "", "", 1, "monturas", "o1", "2026-01-01T00:00:00Z", null),
            )

            MIGRATION_49_50.migrate(db)

            db.query("SELECT id, updatedAt FROM proveedores ORDER BY id").use { cursor ->
                val byId = mutableMapOf<String, String?>()
                while (cursor.moveToNext()) {
                    byId[cursor.getString(0)] = cursor.getString(1)
                }
                assertNotNull(byId["p-null"])
                assertTrue(byId["p-null"]!!.isNotBlank())
                assertNotNull(byId["p-blank"])
                assertTrue(byId["p-blank"]!!.isNotBlank())
                assertEquals("2026-01-01T00:00:00Z", byId["p-keep"])
            }
        }
    }

    @Test
    fun migration_47_48_stamps_null_montura_movimientos() {
        val helper = FrameworkSQLiteOpenHelperFactory().create(
            androidx.sqlite.db.SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(dbName + "-mov")
                .callback(object : androidx.sqlite.db.SupportSQLiteOpenHelper.Callback(47) {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        db.execSQL(
                            """
                            CREATE TABLE montura_movimientos (
                                id TEXT NOT NULL PRIMARY KEY,
                                monturaId TEXT NOT NULL,
                                fecha TEXT NOT NULL,
                                tipo TEXT NOT NULL,
                                cantidad INTEGER NOT NULL,
                                stockPrevio INTEGER NOT NULL,
                                stockNuevo INTEGER NOT NULL,
                                referenciaId TEXT NOT NULL,
                                nota TEXT NOT NULL,
                                opticaId TEXT NOT NULL,
                                userId TEXT,
                                costoUnitario REAL,
                                tipoDocumento TEXT,
                                updatedAt TEXT,
                                updatedBy TEXT
                            )
                            """.trimIndent(),
                        )
                    }

                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
                })
                .build(),
        )

        helper.writableDatabase.use { db ->
            db.execSQL(
                "INSERT INTO montura_movimientos (id, monturaId, fecha, tipo, cantidad, stockPrevio, stockNuevo, referenciaId, nota, opticaId, updatedAt) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                arrayOf("m1", "mont1", "2026-08-29", "ENTRADA", 1, 0, 1, "", "", "o1", null),
            )
            MIGRATION_47_48.migrate(db)
            db.query("SELECT updatedAt FROM montura_movimientos WHERE id = 'm1'").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertNotNull(cursor.getString(0))
                assertTrue(cursor.getString(0).isNotBlank())
            }
        }
        context.deleteDatabase(dbName + "-mov")
    }
}
