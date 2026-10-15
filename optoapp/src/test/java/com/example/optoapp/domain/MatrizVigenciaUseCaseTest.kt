package com.example.optoapp.domain

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.optoapp.data.OptoDatabase
import com.example.optoapp.data.RoomTransactionRunner
import com.example.optoapp.data.SyncStateTracker
import com.example.optoapp.data.costobiselado.CostoBiseladoEntity
import com.example.optoapp.data.costolc.CostoLcEntity
import com.example.optoapp.data.costoproducto.CostoProductoDao
import com.example.optoapp.data.costoproducto.CostoProductoEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.IOException
import java.time.LocalDate

// Real-Room DAO test: in-memory Room needs Robolectric for its Android context, the AGENTS.md exception.
@RunWith(RobolectricTestRunner::class)
class MatrizVigenciaUseCaseTest {

    private val opticaId = "o1"
    private val hoy = LocalDate.of(2026, 7, 20)
    private val pendiente = "pending_upload"
    private lateinit var db: OptoDatabase
    private lateinit var tracker: SyncStateTracker

    private val producto = CostoProductoEntity(
        id = "p1", opticaId = opticaId, material = "Resina", tipoLente = "Monofocal",
        stockOFabricacion = "stock", serie = 1, costoUnitario = 18.0, vigenteDesde = "2026-01-01",
    )

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), OptoDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        tracker = SyncStateTracker(db.syncEntityStateDao(), db)
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun useCase(costoProductoDao: CostoProductoDao = db.costoProductoDao()) = MatrizVigenciaUseCase(
        costoProductoDao = costoProductoDao,
        costoBiseladoDao = db.costoBiseladoDao(),
        costoLcDao = db.costoLcDao(),
        syncStateTracker = tracker,
        transactionRunner = RoomTransactionRunner(db),
    )

    private suspend fun estado(tipo: String, id: String) = db.syncEntityStateDao().getState(opticaId, tipo, id)

    @Test
    fun editClosesThePreviousRowTheDayBeforeAndOpensANewVersion() = runTest {
        db.costoProductoDao().upsertAll(listOf(producto))

        val resultado = useCase().editarProducto(producto, 20.0, LocalDate.of(2026, 8, 1), hoy)

        val nuevoId = (resultado as ResultadoVigencia.Aplicado).nuevoId
        val cerrado = db.costoProductoDao().getById("p1")!!
        val abierto = db.costoProductoDao().getById(nuevoId)!!
        assertEquals("2026-07-31", cerrado.vigenteHasta)
        assertEquals(18.0, cerrado.costoUnitario, 0.0)
        assertEquals(20.0, abierto.costoUnitario, 0.0)
        assertEquals("2026-08-01", abierto.vigenteDesde)
        assertNull(abierto.vigenteHasta)
        assertEquals(producto.copy(id = nuevoId, costoUnitario = 20.0, vigenteDesde = "2026-08-01", updatedAt = abierto.updatedAt), abierto)
        assertNotNull(cerrado.updatedAt)
        assertNotNull(abierto.updatedAt)
        assertEquals(pendiente, estado("costo_producto", "p1")?.status)
        assertEquals(pendiente, estado("costo_producto", nuevoId)?.status)
        assertEquals(2, db.syncEntityStateDao().getByStatus(opticaId, pendiente).size)
    }

    @Test
    fun defaultDateIsTodayOrTheDayAfterThePreviousStartWhicheverIsLater() {
        assertEquals(hoy, MatrizVigenciaUseCase.fechaMinima("2026-01-01", hoy))
        assertEquals(LocalDate.of(2026, 7, 21), MatrizVigenciaUseCase.fechaMinima("2026-07-20", hoy))
        assertEquals(LocalDate.of(2026, 8, 2), MatrizVigenciaUseCase.fechaMinima("2026-08-01", hoy))
    }

    @Test
    fun aDateInThePastIsRejectedWithoutAnyChange() = runTest {
        db.costoProductoDao().upsertAll(listOf(producto))

        val resultado = useCase().editarProducto(producto, 20.0, LocalDate.of(2026, 7, 19), hoy)

        assertEquals(ResultadoVigencia.Rechazado(hoy), resultado)
        assertEquals(listOf(producto), db.costoProductoDao().getAllByOpticaIdList(opticaId))
        assertNull(estado("costo_producto", "p1"))
    }

    @Test
    fun aSecondEditOnTheSameDayMustStartTheNextDay() = runTest {
        val deHoy = producto.copy(vigenteDesde = "2026-07-20")
        db.costoProductoDao().upsertAll(listOf(deHoy))

        val mismoDia = useCase().editarProducto(deHoy, 21.0, hoy, hoy)
        val diaSiguiente = useCase().editarProducto(deHoy, 21.0, LocalDate.of(2026, 7, 21), hoy)

        assertEquals(ResultadoVigencia.Rechazado(LocalDate.of(2026, 7, 21)), mismoDia)
        assertTrue(diaSiguiente is ResultadoVigencia.Aplicado)
        assertEquals("2026-07-20", db.costoProductoDao().getById("p1")!!.vigenteHasta)
    }

    @Test
    fun aScheduledOpenRowCanOnlyBeReplacedAfterItsStart() = runTest {
        val programado = producto.copy(vigenteDesde = "2026-08-01")
        db.costoProductoDao().upsertAll(listOf(programado))

        val mismoInicio = useCase().editarProducto(programado, 21.0, LocalDate.of(2026, 8, 1), hoy)
        val siguiente = useCase().editarProducto(programado, 21.0, LocalDate.of(2026, 8, 2), hoy)

        assertEquals(ResultadoVigencia.Rechazado(LocalDate.of(2026, 8, 2)), mismoInicio)
        assertTrue(siguiente is ResultadoVigencia.Aplicado)
        assertEquals("2026-08-01", db.costoProductoDao().getById("p1")!!.vigenteHasta)
    }

    @Test
    fun aRowAlreadyClosedOnTheDeviceIsNotVersionedAgain() = runTest {
        db.costoProductoDao().upsertAll(listOf(producto.copy(vigenteHasta = "2026-07-25")))

        val resultado = useCase().editarProducto(producto, 20.0, LocalDate.of(2026, 8, 1), hoy)

        assertEquals(ResultadoVigencia.YaNoVigente, resultado)
        assertEquals(1, db.costoProductoDao().getAllByOpticaIdList(opticaId).size)
    }

    @Test
    fun anInsertFailureRollsBackTheClosingAndTheMarks() = runTest {
        db.costoProductoDao().upsertAll(listOf(producto))
        val real = db.costoProductoDao()
        val fallaAlInsertar = object : CostoProductoDao by real {
            override suspend fun upsertAll(entities: List<CostoProductoEntity>) {
                real.upsertAll(entities)
                if (entities.any { it.id != producto.id }) throw IOException("disk full")
            }
        }

        val error = runCatching { useCase(fallaAlInsertar).editarProducto(producto, 20.0, LocalDate.of(2026, 8, 1), hoy) }

        assertTrue(error.exceptionOrNull() is IOException)
        assertEquals(listOf(producto), db.costoProductoDao().getAllByOpticaIdList(opticaId))
        assertTrue(db.syncEntityStateDao().getByStatus(opticaId, pendiente).isEmpty())
    }

    @Test
    fun biseladoEditVersionsCostoPorPar() = runTest {
        val biselado = CostoBiseladoEntity(
            id = "b1", opticaId = opticaId, material = "Resina", tipoAro = "aro_completo",
            stockOFabricacion = "stock", serie = 1, costoPorPar = 8.0, vigenteDesde = "2026-01-01",
        )
        db.costoBiseladoDao().upsertAll(listOf(biselado))

        val nuevoId = (useCase().editarBiselado(biselado, 9.5, LocalDate.of(2026, 8, 1), hoy) as ResultadoVigencia.Aplicado).nuevoId

        assertEquals("2026-07-31", db.costoBiseladoDao().getById("b1")!!.vigenteHasta)
        assertEquals(9.5, db.costoBiseladoDao().getById(nuevoId)!!.costoPorPar, 0.0)
        assertEquals(pendiente, estado("costo_biselado", "b1")?.status)
        assertEquals(pendiente, estado("costo_biselado", nuevoId)?.status)
    }

    @Test
    fun lcEditVersionsCostoUnitario() = runTest {
        val lc = CostoLcEntity(
            id = "l1", opticaId = opticaId, tipoLc = "graduado", materialLc = "HEMA",
            modalidad = "mensual", costoUnitario = 45.0, vigenteDesde = "2026-01-01",
        )
        db.costoLcDao().upsertAll(listOf(lc))

        val nuevoId = (useCase().editarLc(lc, 47.0, LocalDate.of(2026, 8, 1), hoy) as ResultadoVigencia.Aplicado).nuevoId

        assertEquals("2026-07-31", db.costoLcDao().getById("l1")!!.vigenteHasta)
        assertEquals(47.0, db.costoLcDao().getById(nuevoId)!!.costoUnitario, 0.0)
        assertEquals(pendiente, estado("costo_lc", "l1")?.status)
        assertEquals(pendiente, estado("costo_lc", nuevoId)?.status)
    }
}
