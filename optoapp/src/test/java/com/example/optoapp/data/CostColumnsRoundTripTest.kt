package com.example.optoapp.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.optoapp.data.costobiselado.CostoBiseladoEntity
import com.example.optoapp.data.costolc.CostoLcEntity
import com.example.optoapp.data.costoproducto.CostoProductoEntity
import com.example.optoapp.data.gastooperativo.GastoOperativoEntity
import com.example.optoapp.data.servicio.ServicioExtraItem
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.math.BigDecimal
import java.time.LocalDate

// Real-Room test (AGENTS.md exception): in-memory Room needs the Android Context Robolectric provides.
@RunWith(RobolectricTestRunner::class)
class CostColumnsRoundTripTest {

    private lateinit var db: OptoDatabase

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            OptoDatabase::class.java,
        ).allowMainThreadQueries().build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    private suspend fun seedPaciente() {
        db.pacienteDao().insertPaciente(
            Paciente(
                id = "p1",
                nombreCompleto = "Ana",
                edad = 30,
                telefono = "999",
                fechaCreacion = LocalDate.parse("2026-07-01"),
                opticaId = "o1",
            ),
        )
    }

    private fun servicio(id: String) = ServicioExtra(
        id = id,
        descripcion = "Biselado",
        montoTotal = 100.0,
        estado = "Pendiente",
        fecha = LocalDate.parse("2026-07-10"),
        pacienteId = "p1",
        opticaId = "o1",
    )

    @Test
    fun servicio_executor_and_cost_snapshot_round_trip() = runTest {
        seedPaciente()
        db.servicioExtraDao().insertServicio(
            servicio("s1").copy(
                servicioTipoId = "t1",
                realizadoPor = "tercero",
                realizadoPorNombre = "Optica Lima",
                costoReal = 60.0,
            ),
        )
        db.servicioExtraDao().insertServicio(servicio("s2"))

        val stored = db.servicioExtraDao().getServicioById("s1", "o1")!!
        assertEquals("t1", stored.servicioTipoId)
        assertEquals("tercero", stored.realizadoPor)
        assertEquals("Optica Lima", stored.realizadoPorNombre)
        assertEquals(60.0, stored.costoReal, 0.0)

        val legacy = db.servicioExtraDao().getServicioById("s2", "o1")!!
        assertNull(legacy.realizadoPor)
        assertEquals(0.0, legacy.costoReal, 0.0)
    }

    @Test
    fun servicio_item_frame_cost_round_trips_and_defaults_to_zero() = runTest {
        seedPaciente()
        db.servicioExtraDao().insertServicio(servicio("s1"))
        db.servicioExtraItemDao().insert(
            ServicioExtraItem(id = "si1", servicioExtraId = "s1", monturaId = "m1", opticaId = "o1", costoReal = 30.0),
        )
        db.servicioExtraItemDao().insert(ServicioExtraItem(id = "si2", servicioExtraId = "s1", opticaId = "o1"))

        val costs = db.servicioExtraItemDao().getByServicioId("s1", "o1").associate { it.id to it.costoReal }

        assertEquals(mapOf("si1" to 30.0, "si2" to 0.0), costs)
    }

    @Test
    fun dispensacion_item_costo_origen_round_trips() = runTest {
        seedPaciente()
        db.dispensacionDao().insertDispensacion(
            DispensacionOptica(id = "d1", pacienteId = "p1", fecha = LocalDate.parse("2026-07-10"), opticaId = "o1"),
        )
        db.dispensacionItemDao().insertItem(
            DispensacionItem(id = "di1", dispensacionId = "d1", opticaId = "o1", costoOrigen = "mixto"),
        )
        db.dispensacionItemDao().insertItem(DispensacionItem(id = "di2", dispensacionId = "d1", opticaId = "o1"))

        assertEquals("mixto", db.dispensacionItemDao().getById("di1", "o1")!!.costoOrigen)
        assertNull(db.dispensacionItemDao().getById("di2", "o1")!!.costoOrigen)
    }

    @Test
    fun gasto_tipo_round_trips_and_stays_null_when_unset() = runTest {
        val base = GastoOperativoEntity(
            id = "g1",
            opticaId = "o1",
            categoria = "marketing",
            monto = BigDecimal("500.00"),
            fecha = LocalDate.parse("2026-07-01"),
        )
        db.gastoOperativoDao().insert(base.copy(tipoGasto = "fijo"))
        db.gastoOperativoDao().insert(base.copy(id = "g2"))

        val tipos = db.gastoOperativoDao().getByOpticaIdList("o1").associate { it.id to it.tipoGasto }

        assertEquals(mapOf("g1" to "fijo", "g2" to null), tipos)
    }

    @Test
    fun matrix_rows_round_trip_updated_at() = runTest {
        val stamp = "2026-07-20T10:00:00Z"
        db.costoLcDao().upsertAll(
            listOf(
                CostoLcEntity(
                    id = "lc1", opticaId = "o1", tipoLc = "graduado", materialLc = "hydrogel",
                    modalidad = "stock", costoUnitario = 45.0, vigenteDesde = "2026-01-01", updatedAt = stamp,
                ),
            ),
        )
        db.costoProductoDao().upsertAll(
            listOf(
                CostoProductoEntity(
                    id = "cp1", opticaId = "o1", material = "resina", tipoLente = "monofocal",
                    stockOFabricacion = "stock", costoUnitario = 18.0, vigenteDesde = "2026-01-01", updatedAt = stamp,
                ),
            ),
        )
        db.costoBiseladoDao().upsertAll(
            listOf(
                CostoBiseladoEntity(
                    id = "cb1", opticaId = "o1", material = "resina", tipoAro = "completo",
                    stockOFabricacion = "stock", costoPorPar = 10.0, vigenteDesde = "2026-01-01",
                ),
            ),
        )

        assertEquals(stamp, db.costoLcDao().getByOpticaIdList("o1").single().updatedAt)
        assertEquals(stamp, db.costoProductoDao().getByOpticaIdList("o1").single().updatedAt)
        assertNull(db.costoBiseladoDao().getByOpticaIdList("o1").single().updatedAt)
    }
}
