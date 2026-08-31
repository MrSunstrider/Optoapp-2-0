package com.example.optoapp.data

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.example.optoapp.domain.sync.LEGACY_NULL_UPDATED_AT
import org.junit.After
import org.junit.Assert.assertEquals
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
    private val openedHelpers = mutableListOf<SupportSQLiteOpenHelper>()

    @After
    fun tearDown() {
        openedHelpers.forEach { runCatching { it.close() } }
        openedHelpers.clear()
        context.deleteDatabase("stamp-backfill-49-50.db")
        context.deleteDatabase("stamp-backfill-47-48.db")
        context.deleteDatabase("stamp-backfill-48-49.db")
    }

    private fun openHelper(name: String, version: Int, onCreate: (SupportSQLiteDatabase) -> Unit): SupportSQLiteOpenHelper {
        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(name)
                .callback(object : SupportSQLiteOpenHelper.Callback(version) {
                    override fun onCreate(db: SupportSQLiteDatabase) = onCreate(db)
                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
                })
                .build(),
        )
        openedHelpers.add(helper)
        return helper
    }

    private fun assertStampedNullBlankPreserve(
        db: SupportSQLiteDatabase,
        table: String,
        migrate: (SupportSQLiteDatabase) -> Unit,
        insertRow: (SupportSQLiteDatabase, String, String?) -> Unit,
    ) {
        insertRow(db, "row-null", null)
        insertRow(db, "row-blank", "")
        insertRow(db, "row-ws", "   ")
        insertRow(db, "row-keep", "2026-01-01T00:00:00Z")
        migrate(db)
        db.query("SELECT id, updatedAt FROM $table ORDER BY id").use { cursor ->
            val byId = mutableMapOf<String, String?>()
            while (cursor.moveToNext()) {
                byId[cursor.getString(0)] = cursor.getString(1)
            }
            assertEquals(LEGACY_NULL_UPDATED_AT, byId["row-null"])
            assertEquals(LEGACY_NULL_UPDATED_AT, byId["row-blank"])
            assertEquals(LEGACY_NULL_UPDATED_AT, byId["row-ws"])
            assertEquals("2026-01-01T00:00:00Z", byId["row-keep"])
        }
    }

    @Test
    fun migration_49_50_stamps_null_blank_whitespace_proveedores_preserves_existing() {
        openHelper("stamp-backfill-49-50.db", 49) { db ->
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
        }.writableDatabase.use { db ->
            assertStampedNullBlankPreserve(db, "proveedores", { MIGRATION_49_50.migrate(it) }) { d, id, stamp ->
                d.execSQL(
                    "INSERT INTO proveedores (id, nombre, ruc, telefono, email, direccion, contacto, activo, tipo, opticaId, updatedAt, updatedBy) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    arrayOf(id, "N", "1", "", "", "", "", 1, "monturas", "o1", stamp, null),
                )
            }
        }
    }

    @Test
    fun migration_47_48_stamps_null_blank_whitespace_movimientos_preserves_existing() {
        openHelper("stamp-backfill-47-48.db", 47) { db ->
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
        }.writableDatabase.use { db ->
            assertStampedNullBlankPreserve(db, "montura_movimientos", { MIGRATION_47_48.migrate(it) }) { d, id, stamp ->
                d.execSQL(
                    "INSERT INTO montura_movimientos (id, monturaId, fecha, tipo, cantidad, stockPrevio, stockNuevo, referenciaId, nota, opticaId, updatedAt) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    arrayOf(id, "mont1", "2026-08-29", "ENTRADA", 1, 0, 1, "", "", "o1", stamp),
                )
            }
        }
    }

    @Test
    fun migration_48_49_stamps_null_blank_whitespace_monturas_preserves_existing() {
        openHelper("stamp-backfill-48-49.db", 48) { db ->
            db.execSQL(
                """
                CREATE TABLE monturas (
                    id TEXT NOT NULL PRIMARY KEY,
                    sku TEXT NOT NULL,
                    marca TEXT NOT NULL,
                    modelo TEXT NOT NULL,
                    color TEXT NOT NULL,
                    talla TEXT NOT NULL,
                    costo REAL NOT NULL,
                    precio REAL NOT NULL,
                    stockActual INTEGER NOT NULL,
                    stockMinimo INTEGER NOT NULL,
                    activo INTEGER NOT NULL,
                    opticaId TEXT NOT NULL,
                    updatedAt TEXT,
                    updatedBy TEXT
                )
                """.trimIndent(),
            )
        }.writableDatabase.use { db ->
            assertStampedNullBlankPreserve(db, "monturas", { MIGRATION_48_49.migrate(it) }) { d, id, stamp ->
                d.execSQL(
                    "INSERT INTO monturas (id, sku, marca, modelo, color, talla, costo, precio, stockActual, stockMinimo, activo, opticaId, updatedAt) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    arrayOf(id, "S1", "A", "X", "N", "M", 1.0, 2.0, 1, 0, 1, "o1", stamp),
                )
            }
        }
    }
}
