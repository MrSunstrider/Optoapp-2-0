package com.example.optoapp.data.serviciotipo

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.optoapp.data.OptoDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

// Real-Room DAO test (AGENTS.md exception): in-memory Room needs the Android Context Robolectric provides.
@RunWith(RobolectricTestRunner::class)
class ServicioTipoDaoTest {

    private lateinit var db: OptoDatabase
    private lateinit var dao: ServicioTipoDao

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            OptoDatabase::class.java,
        ).allowMainThreadQueries().build()
        dao = db.servicioTipoDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun tipo(
        id: String,
        nombre: String,
        opticaId: String = "o1",
        activo: Boolean = true,
    ) = ServicioTipoEntity(
        id = id,
        opticaId = opticaId,
        nombre = nombre,
        costoPropio = 5.0,
        costoTercero = 12.0,
        activo = activo,
        updatedAt = "2026-07-20T10:00:00Z",
        updatedBy = "u1",
    )

    @Test
    fun upsert_inserts_then_updates_the_same_row() = runTest {
        dao.upsert(tipo("t1", "Biselado"))
        dao.upsert(tipo("t1", "Biselado").copy(costoTercero = 15.0))

        val stored = dao.getById("t1", "o1")
        assertEquals(15.0, stored!!.costoTercero, 0.0)
        assertEquals(5.0, stored.costoPropio, 0.0)
        assertEquals("2026-07-20T10:00:00Z", stored.updatedAt)
        assertEquals(1, dao.getByOpticaIdList("o1").size)
    }

    @Test
    fun observeActivosByOptica_returns_only_active_types_of_that_optica_sorted_by_name() = runTest {
        dao.upsertAll(
            listOf(
                tipo("t1", "Reparacion"),
                tipo("t2", "Biselado"),
                tipo("t3", "Ajuste", activo = false),
                tipo("t4", "Montaje", opticaId = "o2"),
            ),
        )

        val activos = dao.observeActivosByOptica("o1").first()

        assertEquals(listOf("Biselado", "Reparacion"), activos.map { it.nombre })
    }

    @Test
    fun observeByOptica_includes_inactive_types() = runTest {
        dao.upsertAll(listOf(tipo("t1", "Biselado"), tipo("t2", "Ajuste", activo = false)))

        val todos = dao.observeByOptica("o1").first()

        assertEquals(listOf("Ajuste", "Biselado"), todos.map { it.nombre })
    }

    @Test
    fun getById_resolves_a_deactivated_type_for_past_servicios() = runTest {
        dao.upsert(tipo("t3", "Ajuste", activo = false))

        val stored = dao.getById("t3", "o1")

        assertEquals("Ajuste", stored!!.nombre)
        assertFalse(stored.activo)
        assertNull(dao.getById("missing", "o1"))
        assertNull("another optica must not resolve the type", dao.getById("t3", "o2"))
    }

    @Test
    fun dao_exposes_no_delete_operation() {
        val deleteMethods = ServicioTipoDao::class.java.declaredMethods
            .map { it.name }
            .filter { it.contains("delete", ignoreCase = true) }

        assertEquals(emptyList<String>(), deleteMethods)
    }
}
