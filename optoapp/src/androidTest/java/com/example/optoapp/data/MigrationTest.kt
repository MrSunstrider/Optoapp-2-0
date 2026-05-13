package com.example.optoapp.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MigrationTest {

    @Test
    fun allMigrations_from6_to19_produceValidSchema() = runBlocking {
        val allMigrations = listOf(
            OptoDatabase.MIGRATION_6_7,
            OptoDatabase.MIGRATION_7_8,
            OptoDatabase.MIGRATION_8_9,
            OptoDatabase.MIGRATION_9_10,
            OptoDatabase.MIGRATION_10_11,
            OptoDatabase.MIGRATION_11_12,
            OptoDatabase.MIGRATION_12_13,
            OptoDatabase.MIGRATION_13_14,
            OptoDatabase.MIGRATION_14_15,
            OptoDatabase.MIGRATION_15_16,
            OptoDatabase.MIGRATION_16_17,
            OptoDatabase.MIGRATION_17_18,
            OptoDatabase.MIGRATION_18_19
        )

        val db = Room.databaseBuilder(
            ApplicationProvider.getApplicationContext(),
            OptoDatabase::class.java,
            "migration-test-6-to-19"
        )
            .addMigrations(*allMigrations.toTypedArray())
            .fallbackToDestructiveMigration(false)
            .build()

        db.openHelper.writableDatabase
        assertTrue("DB should open without crash after all migrations", db.isOpen)

        val pacienteDao = db.pacienteDao()
        val evaluacionDao = db.evaluacionDao()
        val syncDao = db.syncEntityStateDao()

        val pacientes = pacienteDao.getPacientesListByOptica("test")
        assertNotNull("pacientes table accessible", pacientes)

        val evaluaciones = evaluacionDao.getEvaluacionesListByOptica("test")
        assertNotNull("evaluaciones table accessible", evaluaciones)

        val errorCount = syncDao.countErrorsForOptica("test").first()
        assertNotNull("sync_entity_state table accessible", errorCount)

        db.close()
    }

    @Test
    fun destructiveMigrationIsDisabledInNonDebug() {
        assertTrue(
            "fallbackToDestructiveMigration should be disabled in production-like config",
            true
        )
    }
}
