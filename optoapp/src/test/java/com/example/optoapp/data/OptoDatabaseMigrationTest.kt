package com.example.optoapp.data

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
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
 * Characterization tests for Room migrations and OptoDatabase structure.
 *
 * Covers: re-export consistency, migration chain sequentiality,
 * version bounds, DAO method declarations, and a full data-preservation
 * migration run from v30 to the current version (v40).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = android.app.Application::class)
class OptoDatabaseMigrationTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @After
    fun tearDown() {
        context.deleteDatabase("migration-30-to-40-test.db")
    }

    @Test
    fun all_re_exports_match_migration_constants() {
        assertEquals(MIGRATION_7_8, OptoDatabase.MIGRATION_7_8)
        assertEquals(MIGRATION_6_7, OptoDatabase.MIGRATION_6_7)
        assertEquals(MIGRATION_8_9, OptoDatabase.MIGRATION_8_9)
        assertEquals(MIGRATION_9_10, OptoDatabase.MIGRATION_9_10)
        assertEquals(MIGRATION_10_11, OptoDatabase.MIGRATION_10_11)
        assertEquals(MIGRATION_11_12, OptoDatabase.MIGRATION_11_12)
        assertEquals(MIGRATION_12_13, OptoDatabase.MIGRATION_12_13)
        assertEquals(MIGRATION_13_14, OptoDatabase.MIGRATION_13_14)
        assertEquals(MIGRATION_14_15, OptoDatabase.MIGRATION_14_15)
        assertEquals(MIGRATION_15_16, OptoDatabase.MIGRATION_15_16)
        assertEquals(MIGRATION_16_17, OptoDatabase.MIGRATION_16_17)
        assertEquals(MIGRATION_17_18, OptoDatabase.MIGRATION_17_18)
        assertEquals(MIGRATION_18_19, OptoDatabase.MIGRATION_18_19)
        assertEquals(MIGRATION_19_20, OptoDatabase.MIGRATION_19_20)
        assertEquals(MIGRATION_20_21, OptoDatabase.MIGRATION_20_21)
        assertEquals(MIGRATION_21_22, OptoDatabase.MIGRATION_21_22)
        assertEquals(MIGRATION_22_23, OptoDatabase.MIGRATION_22_23)
        assertEquals(MIGRATION_23_24, OptoDatabase.MIGRATION_23_24)
        assertEquals(MIGRATION_24_25, OptoDatabase.MIGRATION_24_25)
        assertEquals(MIGRATION_25_26, OptoDatabase.MIGRATION_25_26)
        assertEquals(MIGRATION_26_27, OptoDatabase.MIGRATION_26_27)
        assertEquals(MIGRATION_27_28, OptoDatabase.MIGRATION_27_28)
        assertEquals(MIGRATION_28_29, OptoDatabase.MIGRATION_28_29)
        assertEquals(MIGRATION_29_30, OptoDatabase.MIGRATION_29_30)
        assertEquals(MIGRATION_30_31, OptoDatabase.MIGRATION_30_31)
        assertEquals(MIGRATION_31_32, OptoDatabase.MIGRATION_31_32)
        assertEquals(MIGRATION_32_33, OptoDatabase.MIGRATION_32_33)
        assertEquals(MIGRATION_33_34, OptoDatabase.MIGRATION_33_34)
        assertEquals(MIGRATION_34_35, OptoDatabase.MIGRATION_34_35)
        assertEquals(MIGRATION_35_36, OptoDatabase.MIGRATION_35_36)
        assertEquals(MIGRATION_36_37, OptoDatabase.MIGRATION_36_37)
        assertEquals(MIGRATION_37_38, OptoDatabase.MIGRATION_37_38)
        assertEquals(MIGRATION_38_39, OptoDatabase.MIGRATION_38_39)
        assertEquals(MIGRATION_39_40, OptoDatabase.MIGRATION_39_40)
    }

    @Test
    fun migration_version_ranges_are_sequential() {
        val migrations = listOf(
            MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11,
            MIGRATION_11_12, MIGRATION_12_13, MIGRATION_13_14, MIGRATION_14_15, MIGRATION_15_16,
            MIGRATION_16_17, MIGRATION_17_18, MIGRATION_18_19, MIGRATION_19_20, MIGRATION_20_21,
            MIGRATION_21_22, MIGRATION_22_23, MIGRATION_23_24, MIGRATION_24_25,
            MIGRATION_25_26, MIGRATION_26_27, MIGRATION_27_28, MIGRATION_28_29, MIGRATION_29_30,
            MIGRATION_30_31, MIGRATION_31_32, MIGRATION_32_33, MIGRATION_33_34, MIGRATION_34_35,
            MIGRATION_35_36, MIGRATION_36_37, MIGRATION_37_38, MIGRATION_38_39, MIGRATION_39_40
        )

        for (i in 0 until migrations.size - 1) {
            assertEquals(
                "Migration ${i} endVersion should match ${i + 1} startVersion",
                migrations[i].endVersion,
                migrations[i + 1].startVersion
            )
        }
    }

    @Test
    fun first_migration_starts_at_6_and_last_ends_at_40() {
        assertEquals(6, MIGRATION_6_7.startVersion)
        assertEquals(7, MIGRATION_6_7.endVersion)
        assertEquals(39, MIGRATION_39_40.startVersion)
        assertEquals(40, MIGRATION_39_40.endVersion)
    }

    // ─── Schema version characterization ────────────────────────────────
    //
    // Room @Database has CLASS retention (not RUNTIME), so annotation
    // is not visible via reflection at runtime. These tests verify the
    // expected version (40) as a documented constant matching the source:
    //   @Database(entities = [...], version = 40) in OptoDatabase.kt
    // Changing this version triggers a Room migration — it MUST be preserved.

    @Test
    fun databaseVersion_is40() {
        val chainVersion = MIGRATION_39_40.endVersion
        assertEquals(
            "Room schema version must remain 40 — changing it triggers migration. " +
                "Source: @Database(version = 40) in OptoDatabase.kt. " +
                "MIGRATION_39_40.endVersion ($chainVersion) must match.",
            40,
            chainVersion
        )
    }

    @Test
    fun databaseVersion_migration_chain_ends_at_current_version() {
        assertEquals(40, MIGRATION_39_40.endVersion)
        assertEquals(MIGRATION_39_40.endVersion, MIGRATION_39_40.startVersion + 1)
    }

    // ─── Full migration chain data-preservation test ────────────────────
    //
    // Creates a realistic v30 database (all tables that migrations 30→40
    // expect to exist, with columns that existed before those migrations),
    // inserts a paciente row, runs all 10 migrations, and verifies the
    // paciente data survived unchanged.

    /** Creates all tables that must exist at v30 for migrations 30→40. */
    private fun createV30Tables(db: SupportSQLiteDatabase) {
        // pacientes — the table we care about preserving
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS pacientes (
                id TEXT NOT NULL PRIMARY KEY,
                nombreCompleto TEXT NOT NULL,
                edad INTEGER NOT NULL,
                telefono TEXT NOT NULL,
                fechaCreacion TEXT NOT NULL,
                dni TEXT,
                fechaNacimiento TEXT,
                sexo TEXT,
                email TEXT,
                historiaOptometrica TEXT,
                direccion TEXT,
                distrito TEXT,
                ocupacion TEXT,
                acompanante TEXT,
                hobbies TEXT,
                ultimasEtiquetas TEXT NOT NULL,
                opticaId TEXT NOT NULL,
                updatedAt TEXT,
                updatedBy TEXT
            )
        """.trimIndent())

        // ventas — created in MIGRATION_29_30; will gain `ot` in v30→31
        // and `categoriaProductoId` in v31→32
        db.execSQL("""
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
                updatedBy TEXT
            )
        """.trimIndent())

        // pagos — exists since v6; will gain `ventaId` in v32→33
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS pagos (
                id TEXT NOT NULL PRIMARY KEY,
                dispensacionId TEXT,
                servicioExtraId TEXT,
                fecha TEXT NOT NULL,
                tipo TEXT NOT NULL,
                monto REAL NOT NULL,
                metodoPago TEXT NOT NULL,
                nota TEXT NOT NULL,
                opticaId TEXT NOT NULL,
                updatedAt TEXT,
                updatedBy TEXT
            )
        """.trimIndent())

        // dispensaciones — recreated in v9→10; will gain
        // filtro_discromatopsia_tipo (v34→35), reclamo_origen_id (v37→38),
        // evaluacion_id (v38→39)
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS dispensaciones (
                id TEXT NOT NULL PRIMARY KEY,
                ot TEXT NOT NULL,
                monturaId TEXT NOT NULL,
                pacienteId TEXT NOT NULL,
                fecha TEXT NOT NULL,
                opticaId TEXT NOT NULL,
                tipoMontura TEXT NOT NULL,
                materialMontura TEXT NOT NULL,
                tipoLente TEXT NOT NULL,
                materialLente TEXT NOT NULL,
                tratamientos TEXT NOT NULL,
                colorLente TEXT NOT NULL,
                notasDiseno TEXT NOT NULL,
                origenMontura TEXT NOT NULL,
                tipoAro TEXT NOT NULL,
                descripcionMontura TEXT NOT NULL,
                montoTotal REAL NOT NULL,
                metodoPago TEXT NOT NULL,
                montoPagado REAL NOT NULL,
                estadoEntrega TEXT NOT NULL,
                fechaEntrega TEXT,
                fechaVencimientoGarantia TEXT,
                distanciaLente TEXT NOT NULL,
                altura TEXT NOT NULL,
                subTipoBifocal TEXT NOT NULL,
                updatedAt TEXT,
                updatedBy TEXT,
                FOREIGN KEY(pacienteId) REFERENCES pacientes(id) ON DELETE CASCADE
            )
        """.trimIndent())

        // dispensacion_items — created in v20→21; will gain
        // alto_indice, reduccion_diametro, etc. in v38→39
        // Uses snake_case per @ColumnInfo(name = "...") in DispensacionItemEntity
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS dispensacion_items (
                id TEXT NOT NULL PRIMARY KEY,
                dispensacion_id TEXT NOT NULL,
                tipo_lente TEXT NOT NULL,
                material_lente TEXT NOT NULL,
                tratamientos TEXT NOT NULL,
                color_lente TEXT NOT NULL,
                distancia_lente TEXT NOT NULL,
                altura TEXT NOT NULL,
                sub_tipo_bifocal TEXT NOT NULL,
                filtro_discromatopsia_tipo TEXT NOT NULL,
                notas_diseno TEXT NOT NULL,
                montura_id TEXT NOT NULL,
                origen_montura TEXT NOT NULL,
                tipo_aro TEXT NOT NULL,
                material_montura TEXT NOT NULL,
                descripcion_montura TEXT NOT NULL,
                tipo_montura TEXT NOT NULL,
                optica_id TEXT NOT NULL,
                FOREIGN KEY(dispensacion_id) REFERENCES dispensaciones(id) ON DELETE CASCADE
            )
        """.trimIndent())

        // arqueo_caja — part of initial Room schema; will be DROPped in v35→36
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS arqueo_caja (
                id TEXT NOT NULL PRIMARY KEY,
                fecha TEXT NOT NULL,
                opticaId TEXT NOT NULL,
                fondoCaja REAL NOT NULL,
                efectivoContado REAL NOT NULL,
                tarjetaContado REAL NOT NULL,
                transferenciaContado REAL NOT NULL,
                movilContado REAL NOT NULL,
                efectivoCobrado REAL NOT NULL,
                tarjetaCobrado REAL NOT NULL,
                transferenciaCobrado REAL NOT NULL,
                movilCobrado REAL NOT NULL,
                diferenciaEfectivo REAL NOT NULL,
                diferenciaTarjeta REAL NOT NULL,
                diferenciaTransferencia REAL NOT NULL,
                diferenciaMovil REAL NOT NULL,
                diferenciaTotal REAL NOT NULL,
                cerradoPor TEXT NOT NULL,
                sellado INTEGER NOT NULL,
                createdAt TEXT NOT NULL,
                updatedAt TEXT NOT NULL,
                updatedBy TEXT NOT NULL
            )
        """.trimIndent())

        // conflict_records — part of initial Room schema; will be recreated
        // with composite PK in v39→40
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS conflict_records (
                entityId TEXT NOT NULL,
                opticaId TEXT NOT NULL,
                entityType TEXT NOT NULL,
                localSnapshot TEXT NOT NULL,
                remoteSnapshot TEXT NOT NULL,
                detectedAt INTEGER NOT NULL,
                baseSnapshot TEXT NOT NULL,
                localData TEXT NOT NULL,
                remoteData TEXT NOT NULL,
                PRIMARY KEY(entityId)
            )
        """.trimIndent())
    }

    @Test
    fun `migrate 30 to current preserves all data`() {
        val dbName = "migration-30-to-40-test.db"
        context.deleteDatabase(dbName)
        val factory = FrameworkSQLiteOpenHelperFactory()

        // ── Step 1: Create v30 database with all required tables ──
        val v30Config = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(dbName)
            .callback(object : SupportSQLiteOpenHelper.Callback(30) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    createV30Tables(db)
                    db.execSQL(
                        "INSERT INTO pacientes (id, opticaId, nombreCompleto, edad, telefono, fechaCreacion, ultimasEtiquetas) VALUES (?, ?, ?, ?, ?, ?, ?)",
                        arrayOf<Any>("p1", "opt1", "Juan", 30, "555-1234", "2024-01-01", "")
                    )
                }

                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
            })
            .build()

        val v30Helper = factory.create(v30Config)
        // Verify data exists before migration
        val preCursor = v30Helper.writableDatabase.query(
            "SELECT nombreCompleto FROM pacientes WHERE id = 'p1'"
        )
        assertTrue(preCursor.moveToFirst())
        assertEquals("Juan", preCursor.getString(0))
        preCursor.close()
        v30Helper.close()

        // ── Step 2: Run all migrations 30→31→...→40 ──
        val v40Config = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(dbName)
            .callback(object : SupportSQLiteOpenHelper.Callback(40) {
                override fun onCreate(db: SupportSQLiteDatabase) {}

                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {
                    MIGRATION_30_31.migrate(db)
                    MIGRATION_31_32.migrate(db)
                    MIGRATION_32_33.migrate(db)
                    MIGRATION_33_34.migrate(db)
                    MIGRATION_34_35.migrate(db)
                    MIGRATION_35_36.migrate(db)
                    MIGRATION_36_37.migrate(db)
                    MIGRATION_37_38.migrate(db)
                    MIGRATION_38_39.migrate(db)
                    MIGRATION_39_40.migrate(db)
                }
            })
            .build()

        val v40Helper = factory.create(v40Config)
        val db = v40Helper.writableDatabase

        // ── Step 3: Assert data survived all migrations ──
        val cursor = db.query("SELECT id, nombreCompleto FROM pacientes WHERE id = 'p1'")
        assertTrue("Data should survive migration 30→40", cursor.moveToFirst())
        assertEquals("p1", cursor.getString(cursor.getColumnIndexOrThrow("id")))
        assertEquals("Juan", cursor.getString(cursor.getColumnIndexOrThrow("nombreCompleto")))
        cursor.close()

        v40Helper.close()
    }

    // ─── DAO accessibility via OptoDatabase abstract methods ───
    //
    // These verify each DAO is accessible via its abstract method on OptoDatabase.
    // We use individual tests for clear failure reporting.

    @Test
    fun allDaos_haveCorrespondingAbstractMethods() {
        val expectedDaos = setOf(
            "pacienteDao" to "PacienteDao",
            "evaluacionDao" to "EvaluacionDao",
            "dispensacionDao" to "DispensacionDao",
            "dispensacionItemDao" to "DispensacionItemDao",
            "pagoDao" to "PagoDao",
            "servicioExtraDao" to "ServicioExtraDao",
            "monturaDao" to "MonturaDao",
            "monturaMovimientoDao" to "MonturaMovimientoDao",
            "syncEntityStateDao" to "SyncEntityStateDao",
            "conflictDao" to "ConflictDao",
            "proveedorDao" to "ProveedorDao",
            "monturaProveedorDao" to "MonturaProveedorDao",
            "categoriaMonturaDao" to "CategoriaMonturaDao",
            "ordenCompraDao" to "OrdenCompraDao",
            "ordenCompraItemDao" to "OrdenCompraItemDao",
            "inventarioFisicoDao" to "InventarioFisicoDao",
            "regaloDispensacionDao" to "RegaloDispensacionDao",
            "categoriaProductoDao" to "CategoriaProductoDao",
            "costoProductoDao" to "CostoProductoDao",
            "costoBiseladoDao" to "CostoBiseladoDao",
            "gastoOperativoDao" to "GastoOperativoDao",
            "resumenDiarioDao" to "ResumenDiarioDao",
            "configuracionFinancieraDao" to "ConfiguracionFinancieraDao",
            "feedbackRecomendacionDao" to "FeedbackRecomendacionDao"
        )

        val abstractMethods = OptoDatabase::class.java.declaredMethods
            .filter { java.lang.reflect.Modifier.isAbstract(it.modifiers) }
            .map { it.name to it.returnType.simpleName }
            .toMap()

        for ((methodName, expectedReturnType) in expectedDaos) {
            val actualReturnType = abstractMethods[methodName]
            assertNotNull(
                "OptoDatabase must declare abstract method '$methodName()' returning $expectedReturnType",
                actualReturnType
            )
            assertEquals(
                "Return type of $methodName() should be $expectedReturnType",
                expectedReturnType,
                actualReturnType
            )
        }
    }

    @Test
    fun pagoDao_isDeclaredInOptoDatabase() {
        val method = OptoDatabase::class.java.getDeclaredMethod("pagoDao")
        assertNotNull(method)
        assertEquals("PagoDao", method.returnType.simpleName)
    }

    @Test
    fun servicioExtraDao_isDeclaredInOptoDatabase() {
        val method = OptoDatabase::class.java.getDeclaredMethod("servicioExtraDao")
        assertNotNull(method)
        assertEquals("ServicioExtraDao", method.returnType.simpleName)
    }

    @Test
    fun monturaDao_isDeclaredInOptoDatabase() {
        val method = OptoDatabase::class.java.getDeclaredMethod("monturaDao")
        assertNotNull(method)
        assertEquals("MonturaDao", method.returnType.simpleName)
    }

    @Test
    fun monturaMovimientoDao_isDeclaredInOptoDatabase() {
        val method = OptoDatabase::class.java.getDeclaredMethod("monturaMovimientoDao")
        assertNotNull(method)
        assertEquals("MonturaMovimientoDao", method.returnType.simpleName)
    }

    @Test
    fun syncEntityStateDao_isDeclaredInOptoDatabase() {
        val method = OptoDatabase::class.java.getDeclaredMethod("syncEntityStateDao")
        assertNotNull(method)
        assertEquals("SyncEntityStateDao", method.returnType.simpleName)
    }
}
