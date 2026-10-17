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
class Migration55To56Test {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val dbName = "migration-55-to-56-test.db"

    @After
    fun tearDown() {
        context.deleteDatabase(dbName)
    }

    @Test
    fun migration_55_56_is_registered() {
        assertEquals(55, MIGRATION_55_56.startVersion)
        assertEquals(56, MIGRATION_55_56.endVersion)
        assertEquals(MIGRATION_55_56, OptoDatabase.MIGRATION_55_56)
    }

    @Test
    fun v55_rows_survive_and_new_columns_get_their_defaults() {
        createV55Database()

        // Room validates every table against the v56 entities after running the migration,
        // so a column or default that drifts from the entity definition fails here.
        val room = Room.databaseBuilder(context, OptoDatabase::class.java, dbName)
            .addMigrations(MIGRATION_55_56, MIGRATION_56_57, MIGRATION_57_58, MIGRATION_58_59)
            .allowMainThreadQueries()
            .build()
        val db = room.openHelper.writableDatabase

        db.singleRow("SELECT montoTotal FROM dispensaciones WHERE id = 'd1'") {
            assertEquals(200.0, it.getDouble(0), 0.0)
        }
        db.singleRow("SELECT costo_real_od, costo_origen FROM dispensacion_items WHERE id = 'di1'") {
            assertEquals(25.0, it.getDouble(0), 0.0)
            assertTrue(it.isNull(1))
        }
        db.singleRow(
            "SELECT montoTotal, servicioTipoId, realizadoPor, realizadoPorNombre, costoReal FROM servicios_extra WHERE id = 's1'",
        ) {
            assertEquals(100.0, it.getDouble(0), 0.0)
            assertTrue(it.isNull(1))
            assertTrue(it.isNull(2))
            assertTrue(it.isNull(3))
            assertEquals(0.0, it.getDouble(4), 0.0)
        }
        db.singleRow("SELECT monto, costo_real FROM servicio_extra_items WHERE id = 'si1'") {
            assertEquals(100.0, it.getDouble(0), 0.0)
            assertEquals(0.0, it.getDouble(1), 0.0)
        }
        db.singleRow("SELECT categoria, monto, tipoGasto FROM gastos_operativos WHERE id = 'g1'") {
            assertEquals("alquiler", it.getString(0))
            assertEquals(2000.0, it.getDouble(1), 0.0)
            assertTrue("tipoGasto must stay NULL (no backfill)", it.isNull(2))
        }
        db.singleRow("SELECT costo_unitario, updated_at FROM costos_lc WHERE id = 'lc1'") {
            assertEquals(45.0, it.getDouble(0), 0.0)
            assertTrue(it.isNull(1))
        }
        db.singleRow("SELECT costo_unitario, updated_at FROM costos_productos WHERE id = 'cp1'") {
            assertEquals(18.0, it.getDouble(0), 0.0)
            assertTrue(it.isNull(1))
        }
        db.singleRow("SELECT costo_por_par, updated_at FROM costos_biselado WHERE id = 'cb1'") {
            assertEquals(10.0, it.getDouble(0), 0.0)
            assertTrue(it.isNull(1))
        }
        db.singleRow("SELECT COUNT(*) FROM servicio_tipos") {
            assertEquals(0, it.getInt(0))
        }
        room.close()
    }

    // Devices that upgraded through MIGRATION_40_41 never got a costos_lc table.
    @Test
    fun v55_database_without_costos_lc_migrates_to_the_v56_costos_lc_schema() {
        createV55Database(withoutTables = setOf("costos_lc"))

        val room = Room.databaseBuilder(context, OptoDatabase::class.java, dbName)
            .addMigrations(MIGRATION_55_56, MIGRATION_56_57, MIGRATION_57_58, MIGRATION_58_59)
            .allowMainThreadQueries()
            .build()
        val db = room.openHelper.writableDatabase

        db.execSQL(
            "INSERT INTO costos_lc (id, optica_id, tipo_lc, material_lc, modalidad, costo_unitario, vigente_desde, " +
                "updated_at) VALUES ('lc2', 'o1', 'graduado', 'hydrogel', 'stock', 50.0, '2026-02-01', " +
                "'2026-02-01T00:00:00Z')",
        )
        db.singleRow("SELECT costo_unitario, updated_at, vigente_hasta FROM costos_lc WHERE id = 'lc2'") {
            assertEquals(50.0, it.getDouble(0), 0.0)
            assertEquals("2026-02-01T00:00:00Z", it.getString(1))
            assertTrue(it.isNull(2))
        }
        db.singleRow("SELECT COUNT(*) FROM costos_lc") {
            assertEquals(1, it.getInt(0))
        }
        db.singleRow("SELECT montoTotal FROM dispensaciones WHERE id = 'd1'") {
            assertEquals(200.0, it.getDouble(0), 0.0)
        }
        room.close()
    }

    private fun createV55Database(withoutTables: Set<String> = emptySet()) {
        context.deleteDatabase(dbName)
        val schema = loadSchema(55)
        val config = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(dbName)
            .callback(object : SupportSQLiteOpenHelper.Callback(55) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    createSchema(db, schema, withoutTables)
                    insertV55Rows(db, withoutTables)
                }

                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
            })
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

    private fun createSchema(db: SupportSQLiteDatabase, schema: JsonObject, withoutTables: Set<String>) {
        schema.getValue("entities").jsonArray.forEach { element ->
            val entity = element.jsonObject
            val table = entity.getValue("tableName").jsonPrimitive.content
            if (table in withoutTables) return@forEach
            db.execSQL(entity.getValue("createSql").jsonPrimitive.content.replace("\${TABLE_NAME}", table))
            entity["indices"]?.jsonArray?.forEach { index ->
                db.execSQL(index.jsonObject.getValue("createSql").jsonPrimitive.content.replace("\${TABLE_NAME}", table))
            }
        }
        schema.getValue("setupQueries").jsonArray.forEach { db.execSQL(it.jsonPrimitive.content) }
    }

    private fun insertV55Rows(db: SupportSQLiteDatabase, withoutTables: Set<String>) {
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
            "INSERT INTO dispensacion_items (id, dispensacion_id, tipo_lente, material_lente, tratamientos, color_lente, " +
                "distancia_lente, altura, sub_tipo_bifocal, filtro_discromatopsia_tipo, notas_diseno, montura_id, " +
                "origen_montura, tipo_aro, material_montura, descripcion_montura, tipo_montura, optica_id, costo_real_od) " +
                "VALUES ('di1', 'd1', 'Monofocal', 'Resina', '', '', '', '', '', '', '', '', 'Tienda', 'Completo', " +
                "'', '', '', 'o1', 25.0)",
        )
        db.execSQL(
            "INSERT INTO servicios_extra (id, ot, descripcion, montoTotal, aCuenta, estado, fecha, pacienteId, " +
                "metodoPago, opticaId) VALUES ('s1', 'OT-2', 'Biselado', 100.0, 0.0, 'Pendiente', '2026-07-10', " +
                "'p1', 'Efectivo', 'o1')",
        )
        db.execSQL(
            "INSERT INTO servicio_extra_items (id, servicio_extra_id, descripcion, monto, optica_id) " +
                "VALUES ('si1', 's1', 'Biselado', 100.0, 'o1')",
        )
        db.execSQL(
            "INSERT INTO gastos_operativos (id, opticaId, categoria, monto, fecha, isRecurring, frecuencia) " +
                "VALUES ('g1', 'o1', 'alquiler', 2000.0, '2026-07-01', 1, 'mensual')",
        )
        if ("costos_lc" !in withoutTables) {
            db.execSQL(
                "INSERT INTO costos_lc (id, optica_id, tipo_lc, material_lc, modalidad, costo_unitario, vigente_desde) " +
                    "VALUES ('lc1', 'o1', 'graduado', 'hydrogel', 'stock', 45.0, '2026-01-01')",
            )
        }
        db.execSQL(
            "INSERT INTO costos_productos (id, optica_id, material, tipo_lente, stock_o_fabricacion, costo_unitario, " +
                "vigente_desde) VALUES ('cp1', 'o1', 'resina', 'monofocal', 'stock', 18.0, '2026-01-01')",
        )
        db.execSQL(
            "INSERT INTO costos_biselado (id, optica_id, material, tipo_aro, stock_o_fabricacion, costo_por_par, " +
                "vigente_desde) VALUES ('cb1', 'o1', 'resina', 'completo', 'stock', 10.0, '2026-01-01')",
        )
    }

    private fun SupportSQLiteDatabase.singleRow(sql: String, assertions: (android.database.Cursor) -> Unit) {
        query(sql).use { cursor ->
            assertTrue("Expected a row for: $sql", cursor.moveToFirst())
            assertions(cursor)
        }
    }
}
