package com.example.optoapp.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.optoapp.data.gastooperativo.GastoOperativoDao
import com.example.optoapp.data.gastooperativo.GastoOperativoEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.IOException
import java.math.BigDecimal
import java.time.LocalDate

/**
 * Verifies that DAO @Insert methods in download paths are idempotent —
 * inserting the same PK twice does NOT throw SQLiteConstraintException.
 *
 * Covers REQ-SYNC-002: all @Insert in download-related DAOs use
 * OnConflictStrategy.REPLACE.
 */
@RunWith(RobolectricTestRunner::class)
class DaoIdempotencyTest {

    private lateinit var db: OptoDatabase
    private lateinit var dao: GastoOperativoDao

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            OptoDatabase::class.java,
        ).allowMainThreadQueries().build()
        dao = db.gastoOperativoDao()
    }

    @After
    @Throws(IOException::class)
    fun tearDown() {
        db.close()
    }

    @Test
    fun insertSamePkTwice_doesNotThrow() = runBlocking {
        val gasto = GastoOperativoEntity(
            id = "gasto-dup-1",
            opticaId = "optica-test",
            descripcion = "Original",
            monto = BigDecimal.valueOf(100.0),
            fecha = LocalDate.now(),
            categoria = "General",
        )

        // First insert — must succeed
        dao.insert(gasto)

        // Second insert with same PK — must NOT throw (REPLACE)
        val gasto2 = gasto.copy(descripcion = "Reemplazado")
        try {
            dao.insert(gasto2)
        } catch (e: Exception) {
            fail("Duplicate PK insert must not throw with REPLACE: ${e.message}")
        }

        // Verify last value wins — there should be exactly 1 row
        val all = dao.getByOpticaIdList("optica-test")
        assertEquals("Only one row should exist after REPLACE", 1, all.size)
        assertEquals(
            "Second insert must have replaced the first (last value wins)",
            "Reemplazado",
            all.first().descripcion,
        )
    }
}
