package com.example.optoapp.data

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * RED test: Migration 24→25 structural validation.
 *
 * Verifies MIGRATION_24_25 bridges version 24→25, OptoDatabase is at v25,
 * new DAO abstract methods exist, and the version chain is sequential.
 *
 * These tests FAIL until MIGRATION_23_24, MIGRATION_24_25, new entities,
 * new DAOs, and OptoDatabase v25 are implemented (tasks A-03 through A-08).
 */
@RunWith(RobolectricTestRunner::class)
class OptoDatabaseMigration24to25Test {

    @Test
    fun migrationChain_includes23to24_and_24to25() {
        assertEquals(23, MIGRATION_23_24.startVersion)
        assertEquals(24, MIGRATION_23_24.endVersion)
        assertEquals(24, MIGRATION_24_25.startVersion)
        assertEquals(25, MIGRATION_24_25.endVersion)
    }

    @Test
    fun reExports_include24to25() {
        assertEquals(MIGRATION_24_25, OptoDatabase.MIGRATION_24_25)
        assertEquals(MIGRATION_23_24, OptoDatabase.MIGRATION_23_24)
    }

    @Test
    fun fullMigrationChain_6to25_isSequential() {
        val migrations = listOf(
            MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11,
            MIGRATION_11_12, MIGRATION_12_13, MIGRATION_13_14, MIGRATION_14_15, MIGRATION_15_16,
            MIGRATION_16_17, MIGRATION_17_18, MIGRATION_18_19, MIGRATION_19_20, MIGRATION_20_21,
            MIGRATION_21_22, MIGRATION_22_23, MIGRATION_23_24, MIGRATION_24_25,
        )

        assertEquals(6, migrations.first().startVersion)
        assertEquals(25, migrations.last().endVersion)

        for (i in 0 until migrations.size - 1) {
            assertEquals(
                "Migration $i end=${migrations[i].endVersion} should match ${i + 1} start=${migrations[i + 1].startVersion}",
                migrations[i].endVersion,
                migrations[i + 1].startVersion,
            )
        }
    }

    @Test
    fun newEntityClasses_areAccessibleForKsp() {
        // Room entities are verified at compile time via KSP.
        // This test confirms the classes exist and can be instantiated.
        val proveedor = Proveedor(id = "p1", nombre = "Test", ruc = "123", opticaId = "o1")
        assertEquals("p1", proveedor.id)

        val mp = MonturaProveedor(id = "mp1", monturaId = "m1", proveedorId = "p1")
        assertEquals("mp1", mp.id)

        val cat = CategoriaMontura(id = "c1", nombre = "SOL", opticaId = "o1")
        assertEquals("c1", cat.id)

        val oc = OrdenCompra(
            id = "oc1",
            numero = "OC-001",
            proveedorId = "p1",
            fecha = java.time.LocalDate.now(),
            opticaId = "o1",
        )
        assertEquals("oc1", oc.id)

        val oci = OrdenCompraItem(id = "oci1", ordenId = "oc1", monturaId = "m1", cantidad = 5)
        assertEquals("oci1", oci.id)

        val inv = InventarioFisico(
            id = "if1",
            fecha = java.time.LocalDate.now(),
            opticaId = "o1",
            userId = "u1",
        )
        assertEquals("if1", inv.id)

        val invd = InventarioFisicoDetalle(
            id = "ifd1",
            inventarioId = "if1",
            monturaId = "m1",
            stockSistema = 10,
        )
        assertEquals("ifd1", invd.id)
    }

    @Test
    fun newDaos_areDeclaredInOptoDatabase() {
        val abstractMethods = OptoDatabase::class.java.declaredMethods
            .filter { java.lang.reflect.Modifier.isAbstract(it.modifiers) }
            .map { it.name to it.returnType.simpleName }
            .toMap()

        val newDaos = listOf(
            "proveedorDao" to "ProveedorDao",
            "monturaProveedorDao" to "MonturaProveedorDao",
            "categoriaMonturaDao" to "CategoriaMonturaDao",
            "ordenCompraDao" to "OrdenCompraDao",
            "ordenCompraItemDao" to "OrdenCompraItemDao",
            "inventarioFisicoDao" to "InventarioFisicoDao",
        )

        for ((methodName, expectedReturnType) in newDaos) {
            val actualReturnType = abstractMethods[methodName]
            assertNotNull(
                "OptoDatabase must declare abstract method '$methodName()' returning $expectedReturnType",
                actualReturnType,
            )
            assertEquals(expectedReturnType, actualReturnType)
        }
    }

    @Test
    fun monturaEntity_hasNewCatalogFields() {
        val fields = Montura::class.java.declaredFields
            .filter { it.name in setOf("categoria", "coleccion", "temporada", "estadoComercial", "genero") }
            .associate { it.name to it.type.simpleName }

        assertEquals(5, fields.size)
        for (name in listOf("categoria", "coleccion", "temporada", "estadoComercial", "genero")) {
            assertEquals("String", fields[name])
        }
    }

    @Test
    fun monturaMovimientoEntity_hasNewAuditFields() {
        val fields = MonturaMovimiento::class.java.declaredFields
            .filter { it.name in setOf("userId", "costoUnitario", "tipoDocumento") }
            .associate { it.name to it.type.simpleName }

        assertEquals(3, fields.size)
        assertEquals("String", fields["userId"])
        assertEquals("double", fields["costoUnitario"])
        assertEquals("String", fields["tipoDocumento"])
    }

    /**
     * Runtime test: creates a v24 database with data, runs MIGRATION_24_25,
     * and verifies data is preserved with correct default values on new columns.
     *
     * Uses FrameworkSQLiteOpenHelperFactory to create the v24 database manually
     * (bypassing Room's schema validation, which requires exportSchema=true).
     * Then opens a v25 helper whose onUpgrade calls the real MIGRATION_24_25.
     */
    @Test
    fun migration24to25_preservesData_and_setsDefaultValues() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dbName = "migration-data-24to25-test.db"

        // Delete any leftover test file from a previous run
        context.deleteDatabase(dbName)

        val factory = FrameworkSQLiteOpenHelperFactory()
        val v24Config = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(dbName)
            .callback(object : SupportSQLiteOpenHelper.Callback(24) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    // monturas v24 schema (before Sprint A catalog fields)
                    db.execSQL(
                        """
                        CREATE TABLE monturas (
                            id TEXT NOT NULL PRIMARY KEY,
                            sku TEXT NOT NULL DEFAULT '',
                            marca TEXT NOT NULL DEFAULT '',
                            modelo TEXT NOT NULL DEFAULT '',
                            color TEXT NOT NULL DEFAULT '',
                            talla TEXT NOT NULL DEFAULT '',
                            costo REAL NOT NULL DEFAULT 0.0,
                            precio REAL NOT NULL DEFAULT 0.0,
                            stockActual INTEGER NOT NULL DEFAULT 0,
                            stockMinimo INTEGER NOT NULL DEFAULT 0,
                            activo INTEGER NOT NULL DEFAULT 1,
                            tipoAro TEXT NOT NULL DEFAULT '',
                            materialMontura TEXT NOT NULL DEFAULT '',
                            anchoMm REAL,
                            puenteMm REAL,
                            alturaMm REAL,
                            imagenUri TEXT,
                            opticaId TEXT NOT NULL DEFAULT 'mi_optica_base',
                            updatedAt TEXT,
                            updatedBy TEXT
                        )
                    """,
                    )
                    // montura_movimientos v24 schema (before Sprint A audit fields)
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
                            referenciaId TEXT NOT NULL DEFAULT '',
                            nota TEXT NOT NULL DEFAULT '',
                            opticaId TEXT NOT NULL DEFAULT 'mi_optica_base',
                            updatedAt TEXT,
                            updatedBy TEXT
                        )
                    """,
                    )
                }

                override fun onUpgrade(
                    db: SupportSQLiteDatabase,
                    oldVersion: Int,
                    newVersion: Int,
                ) {
                    // No upgrade needed — this helper stays at v24
                }
            })
            .build()

        val v24Helper = factory.create(v24Config)
        val v24Db = v24Helper.writableDatabase
        // Montura: full row with specific values
        v24Db.execSQL(
            """
            INSERT INTO monturas (id, sku, marca, modelo, color, talla, costo, precio,
                stockActual, stockMinimo, activo, tipoAro, materialMontura,
                anchoMm, puenteMm, alturaMm, imagenUri, opticaId, updatedAt, updatedBy)
            VALUES ('m1', 'SKU-001', 'RayBan', 'Aviator', 'Negro', 'M',
                50.0, 120.0, 10, 2, 1, 'COMPLETO', 'ACETATO',
                140.0, 20.0, 45.0, 'uri://m1.jpg', 'o1', '2025-01-01', 'user1')
        """,
        )
        // Montura: minimal row with defaults
        v24Db.execSQL(
            """
            INSERT INTO monturas (id, sku, marca, modelo, color, opticaId)
            VALUES ('m2', 'SKU-002', 'Oakley', 'Radar', 'Rojo', 'o1')
        """,
        )
        // Movimiento for m1
        v24Db.execSQL(
            """
            INSERT INTO montura_movimientos (id, monturaId, fecha, tipo, cantidad,
                stockPrevio, stockNuevo, referenciaId, nota, opticaId, updatedAt, updatedBy)
            VALUES ('mov1', 'm1', '2025-06-01', 'ENTRADA', 10, 0, 10, 'ref-001',
                'Ingreso inicial', 'o1', '2025-06-01', 'user1')
        """,
        )
        // Movimiento for m2
        v24Db.execSQL(
            """
            INSERT INTO montura_movimientos (id, monturaId, fecha, tipo, cantidad,
                stockPrevio, stockNuevo, opticaId)
            VALUES ('mov2', 'm2', '2025-06-02', 'ENTRADA', 5, 0, 5, 'o1')
        """,
        )

        v24Helper.close()
        val v25Config = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(dbName)
            .callback(object : SupportSQLiteOpenHelper.Callback(25) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    // Should not be called — DB already exists at v24
                }

                override fun onUpgrade(
                    db: SupportSQLiteDatabase,
                    oldVersion: Int,
                    newVersion: Int,
                ) {
                    if (oldVersion == 24 && newVersion == 25) {
                        MIGRATION_24_25.migrate(db)
                    }
                }
            })
            .build()

        val v25Helper = factory.create(v25Config)
        val v25Db = v25Helper.writableDatabase
        val m1Cursor = v25Db.query("SELECT * FROM monturas WHERE id = 'm1'")
        assertTrue("Montura m1 should exist after migration", m1Cursor.moveToFirst())
        assertEquals("m1", m1Cursor.getString(m1Cursor.getColumnIndexOrThrow("id")))
        assertEquals("SKU-001", m1Cursor.getString(m1Cursor.getColumnIndexOrThrow("sku")))
        assertEquals("RayBan", m1Cursor.getString(m1Cursor.getColumnIndexOrThrow("marca")))
        assertEquals("Aviator", m1Cursor.getString(m1Cursor.getColumnIndexOrThrow("modelo")))
        assertEquals("Negro", m1Cursor.getString(m1Cursor.getColumnIndexOrThrow("color")))
        assertEquals("M", m1Cursor.getString(m1Cursor.getColumnIndexOrThrow("talla")))
        assertEquals(50.0, m1Cursor.getDouble(m1Cursor.getColumnIndexOrThrow("costo")), 0.001)
        assertEquals(120.0, m1Cursor.getDouble(m1Cursor.getColumnIndexOrThrow("precio")), 0.001)
        assertEquals(10, m1Cursor.getInt(m1Cursor.getColumnIndexOrThrow("stockActual")))
        assertEquals(2, m1Cursor.getInt(m1Cursor.getColumnIndexOrThrow("stockMinimo")))
        assertEquals(1, m1Cursor.getInt(m1Cursor.getColumnIndexOrThrow("activo")))
        assertEquals("COMPLETO", m1Cursor.getString(m1Cursor.getColumnIndexOrThrow("tipoAro")))
        assertEquals("ACETATO", m1Cursor.getString(m1Cursor.getColumnIndexOrThrow("materialMontura")))
        assertEquals(140.0, m1Cursor.getDouble(m1Cursor.getColumnIndexOrThrow("anchoMm")), 0.001)
        assertEquals(20.0, m1Cursor.getDouble(m1Cursor.getColumnIndexOrThrow("puenteMm")), 0.001)
        assertEquals(45.0, m1Cursor.getDouble(m1Cursor.getColumnIndexOrThrow("alturaMm")), 0.001)
        assertEquals("uri://m1.jpg", m1Cursor.getString(m1Cursor.getColumnIndexOrThrow("imagenUri")))
        assertEquals("o1", m1Cursor.getString(m1Cursor.getColumnIndexOrThrow("opticaId")))
        assertEquals("2025-01-01", m1Cursor.getString(m1Cursor.getColumnIndexOrThrow("updatedAt")))
        assertEquals("user1", m1Cursor.getString(m1Cursor.getColumnIndexOrThrow("updatedBy")))
        assertEquals("", m1Cursor.getString(m1Cursor.getColumnIndexOrThrow("categoria")))
        assertEquals("", m1Cursor.getString(m1Cursor.getColumnIndexOrThrow("coleccion")))
        assertEquals("", m1Cursor.getString(m1Cursor.getColumnIndexOrThrow("temporada")))
        assertEquals("", m1Cursor.getString(m1Cursor.getColumnIndexOrThrow("estadoComercial")))
        assertEquals("", m1Cursor.getString(m1Cursor.getColumnIndexOrThrow("genero")))
        m1Cursor.close()
        val m2Cursor = v25Db.query("SELECT * FROM monturas WHERE id = 'm2'")
        assertTrue("Montura m2 should exist after migration", m2Cursor.moveToFirst())
        assertEquals("m2", m2Cursor.getString(m2Cursor.getColumnIndexOrThrow("id")))
        assertEquals("SKU-002", m2Cursor.getString(m2Cursor.getColumnIndexOrThrow("sku")))
        assertEquals("Oakley", m2Cursor.getString(m2Cursor.getColumnIndexOrThrow("marca")))
        // Default values from v24 schema preserved
        assertEquals("", m2Cursor.getString(m2Cursor.getColumnIndexOrThrow("talla")))
        assertEquals(0.0, m2Cursor.getDouble(m2Cursor.getColumnIndexOrThrow("costo")), 0.001)
        assertEquals(0, m2Cursor.getInt(m2Cursor.getColumnIndexOrThrow("stockActual")))
        assertEquals(1, m2Cursor.getInt(m2Cursor.getColumnIndexOrThrow("activo")))
        // New catalog columns should also default to ''
        assertEquals("", m2Cursor.getString(m2Cursor.getColumnIndexOrThrow("categoria")))
        assertEquals("", m2Cursor.getString(m2Cursor.getColumnIndexOrThrow("coleccion")))
        assertEquals("", m2Cursor.getString(m2Cursor.getColumnIndexOrThrow("temporada")))
        assertEquals("", m2Cursor.getString(m2Cursor.getColumnIndexOrThrow("estadoComercial")))
        assertEquals("", m2Cursor.getString(m2Cursor.getColumnIndexOrThrow("genero")))
        m2Cursor.close()
        val mov1Cursor = v25Db.query("SELECT * FROM montura_movimientos WHERE id = 'mov1'")
        assertTrue("Movimiento mov1 should exist after migration", mov1Cursor.moveToFirst())
        assertEquals("mov1", mov1Cursor.getString(mov1Cursor.getColumnIndexOrThrow("id")))
        assertEquals("m1", mov1Cursor.getString(mov1Cursor.getColumnIndexOrThrow("monturaId")))
        assertEquals("2025-06-01", mov1Cursor.getString(mov1Cursor.getColumnIndexOrThrow("fecha")))
        assertEquals("ENTRADA", mov1Cursor.getString(mov1Cursor.getColumnIndexOrThrow("tipo")))
        assertEquals(10, mov1Cursor.getInt(mov1Cursor.getColumnIndexOrThrow("cantidad")))
        assertEquals(0, mov1Cursor.getInt(mov1Cursor.getColumnIndexOrThrow("stockPrevio")))
        assertEquals(10, mov1Cursor.getInt(mov1Cursor.getColumnIndexOrThrow("stockNuevo")))
        assertEquals("ref-001", mov1Cursor.getString(mov1Cursor.getColumnIndexOrThrow("referenciaId")))
        assertEquals("Ingreso inicial", mov1Cursor.getString(mov1Cursor.getColumnIndexOrThrow("nota")))
        assertEquals("o1", mov1Cursor.getString(mov1Cursor.getColumnIndexOrThrow("opticaId")))
        assertEquals("", mov1Cursor.getString(mov1Cursor.getColumnIndexOrThrow("userId")))
        assertEquals(0.0, mov1Cursor.getDouble(mov1Cursor.getColumnIndexOrThrow("costoUnitario")), 0.001)
        assertEquals("", mov1Cursor.getString(mov1Cursor.getColumnIndexOrThrow("tipoDocumento")))
        mov1Cursor.close()
        val mov2Cursor = v25Db.query("SELECT * FROM montura_movimientos WHERE id = 'mov2'")
        assertTrue("Movimiento mov2 should exist after migration", mov2Cursor.moveToFirst())
        assertEquals("mov2", mov2Cursor.getString(mov2Cursor.getColumnIndexOrThrow("id")))
        assertEquals("m2", mov2Cursor.getString(mov2Cursor.getColumnIndexOrThrow("monturaId")))
        assertEquals("ENTRADA", mov2Cursor.getString(mov2Cursor.getColumnIndexOrThrow("tipo")))
        assertEquals(5, mov2Cursor.getInt(mov2Cursor.getColumnIndexOrThrow("cantidad")))
        // New audit columns should also default correctly
        assertEquals("", mov2Cursor.getString(mov2Cursor.getColumnIndexOrThrow("userId")))
        assertEquals(0.0, mov2Cursor.getDouble(mov2Cursor.getColumnIndexOrThrow("costoUnitario")), 0.001)
        assertEquals("", mov2Cursor.getString(mov2Cursor.getColumnIndexOrThrow("tipoDocumento")))
        mov2Cursor.close()

        v25Helper.close()

        // Clean up
        context.deleteDatabase(dbName)
    }

    /**
     * Regression: MIGRATION_24_25 crashed at login with UNIQUE constraint failed when
     * montura_movimientos had pre-existing duplicate (referenciaId, tipo, monturaId) rows.
     * The CREATE UNIQUE INDEX would fail, blocking DB open on every app start.
     * Fix: DELETE duplicates before CREATE UNIQUE INDEX.
     */
    @Test
    fun migration24to25_withDuplicateMovimientos_deduplicatesAndSucceeds() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dbName = "migration-dedup-24to25-test.db"
        context.deleteDatabase(dbName)

        val factory = FrameworkSQLiteOpenHelperFactory()

        val v24Config = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(dbName)
            .callback(object : SupportSQLiteOpenHelper.Callback(24) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        """
                        CREATE TABLE monturas (
                            id TEXT NOT NULL PRIMARY KEY,
                            sku TEXT NOT NULL DEFAULT '', marca TEXT NOT NULL DEFAULT '',
                            modelo TEXT NOT NULL DEFAULT '', color TEXT NOT NULL DEFAULT '',
                            talla TEXT NOT NULL DEFAULT '', costo REAL NOT NULL DEFAULT 0.0,
                            precio REAL NOT NULL DEFAULT 0.0, stockActual INTEGER NOT NULL DEFAULT 0,
                            stockMinimo INTEGER NOT NULL DEFAULT 0, activo INTEGER NOT NULL DEFAULT 1,
                            tipoAro TEXT NOT NULL DEFAULT '', materialMontura TEXT NOT NULL DEFAULT '',
                            anchoMm REAL, puenteMm REAL, alturaMm REAL, imagenUri TEXT,
                            opticaId TEXT NOT NULL DEFAULT 'mi_optica_base', updatedAt TEXT, updatedBy TEXT
                        )
                    """,
                    )
                    db.execSQL(
                        """
                        CREATE TABLE montura_movimientos (
                            id TEXT NOT NULL PRIMARY KEY, monturaId TEXT NOT NULL,
                            fecha TEXT NOT NULL, tipo TEXT NOT NULL,
                            cantidad INTEGER NOT NULL, stockPrevio INTEGER NOT NULL,
                            stockNuevo INTEGER NOT NULL, referenciaId TEXT NOT NULL DEFAULT '',
                            nota TEXT NOT NULL DEFAULT '',
                            opticaId TEXT NOT NULL DEFAULT 'mi_optica_base',
                            updatedAt TEXT, updatedBy TEXT
                        )
                    """,
                    )
                }
                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
            })
            .build()

        val v24Helper = factory.create(v24Config)
        val v24Db = v24Helper.writableDatabase

        v24Db.execSQL("INSERT INTO monturas (id, opticaId) VALUES ('m1', 'o1')")
        // Two duplicates: same (referenciaId='', tipo='AJUSTE', monturaId='m1'), different id
        v24Db.execSQL("INSERT INTO montura_movimientos (id, monturaId, fecha, tipo, cantidad, stockPrevio, stockNuevo, referenciaId, opticaId) VALUES ('dup-a', 'm1', '2025-01-01', 'AJUSTE', 1, 10, 9, '', 'o1')")
        v24Db.execSQL("INSERT INTO montura_movimientos (id, monturaId, fecha, tipo, cantidad, stockPrevio, stockNuevo, referenciaId, opticaId) VALUES ('dup-b', 'm1', '2025-01-02', 'AJUSTE', 2, 9, 7, '', 'o1')")
        // Unique row that must be preserved
        v24Db.execSQL("INSERT INTO montura_movimientos (id, monturaId, fecha, tipo, cantidad, stockPrevio, stockNuevo, referenciaId, opticaId) VALUES ('unique-1', 'm1', '2025-01-03', 'SALIDA_VENTA', 1, 7, 6, 'disp-001', 'o1')")
        v24Helper.close()

        val v25Config = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(dbName)
            .callback(object : SupportSQLiteOpenHelper.Callback(25) {
                override fun onCreate(db: SupportSQLiteDatabase) {}
                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {
                    if (oldVersion == 24 && newVersion == 25) MIGRATION_24_25.migrate(db)
                }
            })
            .build()

        val v25Helper = factory.create(v25Config)
        val v25Db = v25Helper.writableDatabase

        val cursor = v25Db.query("SELECT id FROM montura_movimientos ORDER BY id")
        val surviving = mutableListOf<String>()
        while (cursor.moveToNext()) surviving.add(cursor.getString(0))
        cursor.close()

        // One of the two duplicates must have been removed — exactly 2 rows total
        assertEquals("Expected 2 rows after dedup (one duplicate removed, unique-1 preserved)", 2, surviving.size)
        assertTrue("unique-1 must be preserved", "unique-1" in surviving)
        // One of dup-a or dup-b survives (MIN(id) = dup-a alphabetically)
        assertTrue("One of the AJUSTE duplicates must survive", "dup-a" in surviving || "dup-b" in surviving)

        v25Helper.close()
        context.deleteDatabase(dbName)
    }
}
