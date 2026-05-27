package com.example.optoapp.rules

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.optoapp.data.DispensacionDao
import com.example.optoapp.data.DispensacionItemDao
import com.example.optoapp.data.EvaluacionDao
import com.example.optoapp.data.OptoDatabase
import com.example.optoapp.data.PacienteDao
import com.example.optoapp.data.SyncEntityStateDao
import com.example.optoapp.data.montura.MonturaDao
import com.example.optoapp.data.montura.MonturaMovimientoDao
import com.example.optoapp.data.pago.PagoDao
import com.example.optoapp.data.servicio.ServicioExtraDao
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

/**
 * JUnit [TestRule] that creates an in-memory Room database before each test
 * and closes it after.
 *
 * Usage:
 * ```
 * @get:Rule
 * val dbRule = TestDatabaseRule()
 *
 * @Test
 * fun myTest() {
 *     dbRule.pacienteDao.insertPaciente(...)
 * }
 * ```
 *
 * All DAOs are accessible via properties. The database is built with
 * [Room.inMemoryDatabaseBuilder] and [allowMainThreadQueries] so Compose UI
 * tests can read/write without coroutine wrappers.
 */
class TestDatabaseRule : TestRule {

    private var _database: OptoDatabase? = null
    private var _pacienteDao: PacienteDao? = null
    private var _evaluacionDao: EvaluacionDao? = null
    private var _dispensacionDao: DispensacionDao? = null
    private var _dispensacionItemDao: DispensacionItemDao? = null
    private var _pagoDao: PagoDao? = null
    private var _servicioExtraDao: ServicioExtraDao? = null
    private var _monturaDao: MonturaDao? = null
    private var _monturaMovimientoDao: MonturaMovimientoDao? = null
    private var _syncEntityStateDao: SyncEntityStateDao? = null

    /** The in-memory [OptoDatabase] instance, valid only during test execution. */
    val database: OptoDatabase
        get() = _database ?: throw IllegalStateException(
            "TestDatabaseRule not initialized — did you forget @get:Rule?"
        )

    val pacienteDao: PacienteDao get() = checkNotNull(_pacienteDao)
    val evaluacionDao: EvaluacionDao get() = checkNotNull(_evaluacionDao)
    val dispensacionDao: DispensacionDao get() = checkNotNull(_dispensacionDao)
    val dispensacionItemDao: DispensacionItemDao get() = checkNotNull(_dispensacionItemDao)
    val pagoDao: PagoDao get() = checkNotNull(_pagoDao)
    val servicioExtraDao: ServicioExtraDao get() = checkNotNull(_servicioExtraDao)
    val monturaDao: MonturaDao get() = checkNotNull(_monturaDao)
    val monturaMovimientoDao: MonturaMovimientoDao get() = checkNotNull(_monturaMovimientoDao)
    val syncEntityStateDao: SyncEntityStateDao get() = checkNotNull(_syncEntityStateDao)

    override fun apply(base: Statement, description: Description): Statement {
        return object : Statement() {
            override fun evaluate() {
                createDatabase()
                try {
                    base.evaluate()
                } finally {
                    closeDatabase()
                }
            }
        }
    }

    private fun createDatabase() {
        val db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            OptoDatabase::class.java
        ).allowMainThreadQueries().build()

        _database = db
        _pacienteDao = db.pacienteDao()
        _evaluacionDao = db.evaluacionDao()
        _dispensacionDao = db.dispensacionDao()
        _dispensacionItemDao = db.dispensacionItemDao()
        _pagoDao = db.pagoDao()
        _servicioExtraDao = db.servicioExtraDao()
        _monturaDao = db.monturaDao()
        _monturaMovimientoDao = db.monturaMovimientoDao()
        _syncEntityStateDao = db.syncEntityStateDao()
    }

    private fun closeDatabase() {
        try {
            _database?.close()
        } finally {
            _database = null
            _pacienteDao = null
            _evaluacionDao = null
            _dispensacionDao = null
            _dispensacionItemDao = null
            _pagoDao = null
            _servicioExtraDao = null
            _monturaDao = null
            _monturaMovimientoDao = null
            _syncEntityStateDao = null
        }
    }
}
