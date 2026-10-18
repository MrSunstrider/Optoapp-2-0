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
class Migration56To57Test {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val dbName = "migration-56-to-57-test.db"

    @After
    fun tearDown() {
        context.deleteDatabase(dbName)
    }

    @Test
    fun migration_56_57_is_registered() {
        assertEquals(56, MIGRATION_56_57.startVersion)
        assertEquals(57, MIGRATION_56_57.endVersion)
        assertEquals(MIGRATION_56_57, OptoDatabase.MIGRATION_56_57)
    }

    @Test
    fun feedback_rows_gain_estado_and_periodo_with_backfill() {
        createV56Database()

        val room = Room.databaseBuilder(context, OptoDatabase::class.java, dbName)
            .addMigrations(MIGRATION_56_57, MIGRATION_57_58, MIGRATION_58_59, MIGRATION_59_60)
            .allowMainThreadQueries()
            .build()
        val db = room.openHelper.writableDatabase

        db.query(
            "SELECT recomendacionId, fueUtil, estado, periodo FROM feedback_recomendaciones ORDER BY recomendacionId",
        ).use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("rec-no", cursor.getString(0))
            assertEquals(0, cursor.getInt(1))
            assertEquals("NO_APLICA", cursor.getString(2))
            assertEquals("", cursor.getString(3))
            assertTrue(cursor.moveToNext())
            assertEquals("rec-si", cursor.getString(0))
            assertEquals(1, cursor.getInt(1))
            assertEquals("HECHO", cursor.getString(2))
            assertEquals("", cursor.getString(3))
        }
        room.close()
    }

    private fun createV56Database() {
        context.deleteDatabase(dbName)
        val schema = loadSchema(56)
        val config = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(dbName)
            .callback(object : SupportSQLiteOpenHelper.Callback(56) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    createSchema(db, schema)
                    db.execSQL(
                        "INSERT INTO feedback_recomendaciones (id, recomendacionId, opticaId, fueUtil, fecha) " +
                            "VALUES ('fb-1', 'rec-si', 'optica1', 1, 1000)",
                    )
                    db.execSQL(
                        "INSERT INTO feedback_recomendaciones (id, recomendacionId, opticaId, fueUtil, fecha) " +
                            "VALUES ('fb-2', 'rec-no', 'optica1', 0, 2000)",
                    )
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

    private fun createSchema(db: SupportSQLiteDatabase, schema: JsonObject) {
        schema.getValue("entities").jsonArray.forEach { element ->
            val entity = element.jsonObject
            val table = entity.getValue("tableName").jsonPrimitive.content
            db.execSQL(entity.getValue("createSql").jsonPrimitive.content.replace("\${TABLE_NAME}", table))
            entity["indices"]?.jsonArray?.forEach { indexElement ->
                val index = indexElement.jsonObject
                db.execSQL(index.getValue("createSql").jsonPrimitive.content.replace("\${TABLE_NAME}", table))
            }
        }
    }
}
