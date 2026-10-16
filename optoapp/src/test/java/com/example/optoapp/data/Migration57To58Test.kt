package com.example.optoapp.data

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

// Real-Room migration test (AGENTS.md exception): the migration runs on Android SQLite and Room's
// schema validation, both of which need the Context Robolectric provides.
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = android.app.Application::class)
class Migration57To58Test {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val dbName = "migration-57-to-58-test.db"

    @After
    fun tearDown() {
        context.deleteDatabase(dbName)
    }

    @Test
    fun migration_57_58_is_registered() {
        assertEquals(57, MIGRATION_57_58.startVersion)
        assertEquals(58, MIGRATION_57_58.endVersion)
        assertEquals(MIGRATION_57_58, OptoDatabase.MIGRATION_57_58)
    }

    @Test
    fun v57_rows_survive_and_vendedor_user_id_is_nullable() {
        createV57Database()

        val room = Room.databaseBuilder(context, OptoDatabase::class.java, dbName)
            .addMigrations(MIGRATION_57_58)
            .allowMainThreadQueries()
            .build()
        val db = room.openHelper.writableDatabase

        db.singleRow("SELECT montoTotal, vendedorUserId FROM dispensaciones WHERE id = 'd1'") {
            assertEquals(200.0, it.getDouble(0), 0.0)
            assertTrue("legacy rows stay unattributed", it.isNull(1))
        }
        db.singleRow("SELECT montoTotal, vendedorUserId FROM servicios_extra WHERE id = 's1'") {
            assertEquals(100.0, it.getDouble(0), 0.0)
            assertTrue(it.isNull(1))
        }
        room.close()
    }

    private fun createV57Database() {
        context.deleteDatabase(dbName)
        val schema = loadSchema(57)
        val config = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(dbName)
            .callback(
                object : SupportSQLiteOpenHelper.Callback(57) {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        createSchema(db, schema)
                        insertV57Rows(db)
                    }

                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
                },
            )
            .build()
        FrameworkSQLiteOpenHelperFactory().create(config).apply {
            writableDatabase
            close()
        }
    }

    private fun loadSchema(version: Int): JsonObject {
        val relative = "schemas/com.example.optoapp.data.OptoDatabase/$version.json"
        val file = listOf(File(relative), File("optoapp/$relative")).first { it.exists() }
        return Json.parseToJsonElement(file.readText()).jsonObject.getValue("database").jsonObject
    }

    private fun createSchema(db: SupportSQLiteDatabase, schema: JsonObject) {
        schema.getValue("entities").jsonArray.forEach { element ->
            val entity = element.jsonObject
            val table = entity.getValue("tableName").jsonPrimitive.content
            db.execSQL(entity.getValue("createSql").jsonPrimitive.content.replace("\${TABLE_NAME}", table))
            entity["indices"]?.jsonArray?.forEach { index ->
                db.execSQL(index.jsonObject.getValue("createSql").jsonPrimitive.content.replace("\${TABLE_NAME}", table))
            }
        }
        schema.getValue("setupQueries").jsonArray.forEach { db.execSQL(it.jsonPrimitive.content) }
    }

    private fun insertV57Rows(db: SupportSQLiteDatabase) {
        db.execSQL(
            "INSERT INTO pacientes (id, nombreCompleto, edad, telefono, fechaCreacion, ultimasEtiquetas, opticaId) " +
                "VALUES ('p1', 'Ana', 30, '999', '2026-01-01', '', 'o1')",
        )
        db.execSQL(
            "INSERT INTO dispensaciones (id, ot, monturaId, pacienteId, fecha, opticaId, tipoMontura, materialMontura, " +
                "tipoLente, materialLente, tratamientos, colorLente, notasDiseno, origenMontura, tipoAro, " +
                "descripcionMontura, montoTotal, metodoPago, montoPagado, estadoEntrega, distanciaLente, altura, " +
                "subTipoBifocal, filtro_discromatopsia_tipo) VALUES ('d1', 'OT-1', '', 'p1', '2026-07-10', 'o1', " +
                "'', '', 'Monofocal', 'Resina', '', '', '', 'Tienda', 'Completo', '', 200.0, 'Efectivo', 200.0, " +
                "'Entregado', '', '', '', '')",
        )
        db.execSQL(
            "INSERT INTO servicios_extra (id, ot, descripcion, montoTotal, aCuenta, estado, fecha, pacienteId, " +
                "metodoPago, opticaId, costoReal) VALUES ('s1', 'OT-2', 'Biselado', 100.0, 0.0, 'Pendiente', " +
                "'2026-07-10', 'p1', 'Efectivo', 'o1', 0.0)",
        )
    }

    private fun SupportSQLiteDatabase.singleRow(sql: String, assertions: (android.database.Cursor) -> Unit) {
        query(sql).use { cursor ->
            assertTrue("Expected a row for: $sql", cursor.moveToFirst())
            assertions(cursor)
        }
    }
}
