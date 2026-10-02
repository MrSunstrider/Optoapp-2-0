package com.example.optoapp.domain

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.optoapp.data.DispensacionItem
import com.example.optoapp.data.DispensacionOptica
import com.example.optoapp.data.DispensacionRepository
import com.example.optoapp.data.Montura
import com.example.optoapp.data.OptoDatabase
import com.example.optoapp.data.OptoRepository
import com.example.optoapp.data.Paciente
import com.example.optoapp.data.PacienteRepository
import com.example.optoapp.data.Pago
import com.example.optoapp.data.RoomTransactionRunner
import com.example.optoapp.data.SyncRepository
import com.example.optoapp.data.SyncStateTracker
import com.example.optoapp.data.backup.BackupRestoreCoordinator
import com.example.optoapp.data.montura.MonturaInventoryCoordinator
import com.example.optoapp.data.sync.SyncSnapshotCoordinator
import com.example.optoapp.sync.PostSaveSyncScheduler
import com.example.optoapp.util.DateUtils
import com.example.optoapp.util.DispensacionStockHelper
import dagger.Lazy
import io.github.jan.supabase.SupabaseClient
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DiscardLosingClaimUseCaseTest {
    private val opticaId = "o1"
    private val origId = "d1"
    private lateinit var db: OptoDatabase
    private lateinit var repository: OptoRepository
    private lateinit var tracker: SyncStateTracker
    private lateinit var stockHelper: DispensacionStockHelper
    private val today = DateUtils.today()

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), OptoDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val schedulerLazy = mockk<Lazy<PostSaveSyncScheduler>>()
        every { schedulerLazy.get() } returns mockk(relaxed = true)
        tracker = SyncStateTracker(db.syncEntityStateDao(), db)
        val snapshots = mockk<SyncSnapshotCoordinator>(relaxed = true)
        coEvery { snapshots.getDispensacionesSnapshotForOptica(any()) } coAnswers {
            db.dispensacionDao().getDispensacionesListByOptica(firstArg())
        }
        repository = OptoRepository(
            database = db,
            syncStateTracker = tracker,
            postSaveSyncScheduler = schedulerLazy,
            pacienteRepo = PacienteRepository(db.pacienteDao(), db.evaluacionDao()),
            dispensacionRepo = DispensacionRepository(db.dispensacionDao(), db.dispensacionItemDao(), db.pagoDao(), db.servicioExtraDao()),
            syncRepo = SyncRepository(mockk(relaxed = true), db.monturaDao(), db.monturaMovimientoDao()),
            snapshotCoordinator = snapshots,
            backupCoordinator = mockk<BackupRestoreCoordinator>(relaxed = true),
            monturaCoordinator = MonturaInventoryCoordinator(db.monturaDao(), db.monturaMovimientoDao(), schedulerLazy),
            gastoOperativoDao = db.gastoOperativoDao(),
            supabase = mockk<SupabaseClient>(relaxed = true),
        )
        stockHelper = DispensacionStockHelper(repository.monturaCoordinator, RoomTransactionRunner(db))
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun discarder(repo: OptoRepository = repository) = DiscardLosingClaimUseCase(
        repo, db.pagoDao(), db.monturaMovimientoDao(), repository.monturaCoordinator, stockHelper, tracker,
    )

    private suspend fun seedClaimedOriginal(frameStock: Int = 3, unsyncedCredit: Boolean = false): String {
        db.pacienteDao().insertPaciente(
            Paciente(id = "pac", nombreCompleto = "Paciente", edad = 30, telefono = "1", fechaCreacion = today, opticaId = opticaId),
        )
        db.dispensacionDao().insertDispensacion(
            DispensacionOptica(
                id = origId, ot = "2026-0042", pacienteId = "pac", fecha = today.minusDays(40), opticaId = opticaId,
                montoTotal = 200.0, estadoEntrega = "Entregado", fechaEntrega = today.minusDays(30),
            ),
        )
        db.monturaDao().insertMontura(Montura(id = "M1", sku = "sku-M1", stockActual = frameStock, opticaId = opticaId))
        db.dispensacionItemDao().insertItem(
            DispensacionItem(id = "i1", dispensacionId = origId, monturaId = "M1", origenMontura = "Tienda", opticaId = opticaId),
        )
        listOf(Triple("a1", 120.0, "Efectivo"), Triple("a2", 80.0, "Tarjeta")).forEach { (id, monto, metodo) ->
            db.pagoDao().insertPago(
                Pago(id = id, dispensacionId = origId, fecha = today.minusDays(35), tipo = "Abono", monto = monto, metodoPago = metodo, opticaId = opticaId),
            )
            tracker.markSynced(opticaId, "pago", id)
        }
        if (unsyncedCredit) {
            db.pagoDao().insertPago(
                Pago(id = "a3", dispensacionId = origId, fecha = today.minusDays(2), tipo = "Abono", monto = 50.0, metodoPago = "Efectivo", opticaId = opticaId),
            )
        }
        val created = ReclamarDispensacionUseCase(
            repository, db.pagoDao(), stockHelper, mockk(relaxed = true), CalcularMontoPagadoUseCase(db.pagoDao()),
        )(origId, opticaId, "Lente rayado", 150.0, "Tarjeta") as ReclamoOutcome.Created
        tracker.markError(opticaId, "dispensacion", created.replacementId, "quarantine:reclamo_duplicate:$origId")
        return created.replacementId
    }

    private suspend fun pagosOf(parentId: String) = db.pagoDao().getPagosByParent(parentId, opticaId)
    private suspend fun stock() = db.monturaDao().getMonturaByIdForOptica("M1", opticaId)!!.stockActual
    private suspend fun movimientos() = db.monturaMovimientoDao().getMovimientosListByOptica(opticaId)
    private suspend fun states() = db.syncEntityStateDao().getByStatus(opticaId, "error") +
        db.syncEntityStateDao().getByStatus(opticaId, "awaiting_remote")

    @Test
    fun discard_removesTheLosingClaimAndRestoresTheLedgerAndStock() = runTest {
        val replId = seedClaimedOriginal()
        assertEquals(2, stock())

        val discarded = discarder()(opticaId)

        assertEquals(1, discarded)
        assertNull(db.dispensacionDao().getDispensacionById(replId, opticaId))
        assertTrue(pagosOf(replId).isEmpty())
        assertTrue(db.dispensacionItemDao().getItemsListByDispensacion(replId, opticaId).isEmpty())
        assertEquals(setOf("a1", "a2"), pagosOf(origId).map { it.id }.toSet())
        assertEquals(200.0, db.pagoDao().sumMontoByDispensacion(origId, opticaId), 0.001)
        assertEquals(3, stock())
        assertTrue(movimientos().isEmpty())
        assertTrue(tracker.quarantineReasons(opticaId, "dispensacion").isEmpty())
    }

    @Test
    fun discard_holdsTheOriginalForTheWinnerAndTellsTheUserOnce() = runTest {
        val replId = seedClaimedOriginal()

        discarder()(opticaId)

        val awaiting = db.syncEntityStateDao().getByStatus(opticaId, "awaiting_remote").single()
        assertEquals("dispensacion" to origId, awaiting.entityType to awaiting.entityId)
        val notice = db.syncEntityStateDao().getByStatus(opticaId, "error").single()
        assertEquals("reclamo_descartado" to replId, notice.entityType to notice.entityId)
        assertEquals(
            "Otro dispositivo ya registró el reclamo de la OT 2026-0042; se descartó el reclamo local.",
            notice.lastError,
        )
        assertFalse(notice.lastError.startsWith("quarantine:"))
    }

    @Test
    fun discard_compensatesAnAlreadyUploadedSaleWithAnIdempotentRestock() = runTest {
        val replId = seedClaimedOriginal()
        val sale = movimientos().single()
        tracker.markSynced(opticaId, "montura_movimiento", sale.id)

        discarder()(opticaId)

        assertEquals(3, stock())
        val restock = movimientos().single { it.id != sale.id }
        assertEquals("AJUSTE" to "$replId:descarte:${sale.id}", restock.tipo to restock.referenciaId)
        assertEquals(1, restock.cantidad)
        assertNotNull(movimientos().singleOrNull { it.id == sale.id })
    }

    @Test
    fun discard_removesOnlyTheClaimReversosAndClearsTheirQuarantine() = runTest {
        seedClaimedOriginal()
        val reversos = pagosOf(origId).filter { it.tipo == "Reverso" }
        assertEquals(2, reversos.size)
        tracker.markError(opticaId, "pago", reversos.first().id, "quarantine:reverso_duplicate:a1")

        discarder()(opticaId)

        assertEquals(setOf("a1", "a2"), pagosOf(origId).map { it.id }.toSet())
        assertTrue(states().none { it.entityType == "pago" })
    }

    @Test
    fun failureMidDiscard_rollsBackEverything() = runTest {
        val replId = seedClaimedOriginal()
        val failing = spyk(repository)
        coEvery { failing.deleteDispensacionById(replId, opticaId) } throws IllegalStateException("delete failed")

        val error = runCatching { discarder(failing)(opticaId) }.exceptionOrNull()

        assertEquals("delete failed", error?.message)
        assertNotNull(db.dispensacionDao().getDispensacionById(replId, opticaId))
        assertEquals(2, pagosOf(origId).count { it.tipo == "Reverso" })
        assertEquals(2, stock())
        assertEquals(1, movimientos().size)
        assertEquals(
            mapOf(replId to "quarantine:reclamo_duplicate:$origId"),
            tracker.quarantineReasons(opticaId, "dispensacion"),
        )
    }

    @Test
    fun withoutALosingClaim_discardIsANoOp() = runTest {
        db.pacienteDao().insertPaciente(
            Paciente(id = "pac", nombreCompleto = "Paciente", edad = 30, telefono = "1", fechaCreacion = today, opticaId = opticaId),
        )
        db.dispensacionDao().insertDispensacion(
            DispensacionOptica(id = origId, ot = "2026-0042", pacienteId = "pac", fecha = today, opticaId = opticaId),
        )
        tracker.markError(opticaId, "dispensacion", origId, "quarantine:constraint:23514")

        assertEquals(0, discarder()(opticaId))
        assertNotNull(db.dispensacionDao().getDispensacionById(origId, opticaId))
    }

    // ── Residual local credit moves to the winner's replacement ───────

    private val winnerId = "w1"

    /** What the download brings: the winner's replacement, its transfers and the Reversos of synced credits. */
    private suspend fun downloadWinner() {
        db.dispensacionDao().insertDispensacion(
            DispensacionOptica(
                id = winnerId, ot = "2026-0042-R1", pacienteId = "pac", fecha = today, opticaId = opticaId,
                montoTotal = 150.0, montoPagado = 200.0, estadoEntrega = "Pendiente", reclamoOrigenId = origId,
            ),
        )
        tracker.markSynced(opticaId, "dispensacion", winnerId)
        listOf(Triple("a1", 120.0, "Efectivo"), Triple("a2", 80.0, "Tarjeta")).forEach { (creditId, monto, metodo) ->
            db.pagoDao().insertPago(
                Pago(id = "wrv-$creditId", dispensacionId = origId, fecha = today, tipo = "Reverso", monto = monto, metodoPago = metodo, opticaId = opticaId, reversaPagoId = creditId),
            )
            db.pagoDao().insertPago(
                Pago(id = "wtr-$creditId", dispensacionId = winnerId, fecha = today, tipo = "Abono", monto = monto, metodoPago = metodo, opticaId = opticaId),
            )
            listOf("wrv-$creditId", "wtr-$creditId").forEach { tracker.markSynced(opticaId, "pago", it) }
        }
        tracker.markSynced(opticaId, "dispensacion", origId)
    }

    @Test
    fun adoptedWinner_receivesTheResidualLocalCreditAndTheOriginalNetsZero() = runTest {
        seedClaimedOriginal(unsyncedCredit = true)
        discarder()(opticaId)
        downloadWinner()

        val transferred = discarder().transferResidualCredit(opticaId)

        assertEquals(1, transferred)
        assertEquals(0.0, db.pagoDao().sumMontoByDispensacion(origId, opticaId), 0.001)
        assertEquals(250.0, db.pagoDao().sumMontoByDispensacion(winnerId, opticaId), 0.001)
        val reverso = pagosOf(origId).single { it.tipo == "Reverso" && it.reversaPagoId == "a3" }
        assertEquals(Triple(50.0, "Efectivo", today), Triple(reverso.monto, reverso.metodoPago, reverso.fecha))
        val credit = pagosOf(winnerId).single { it.id !in setOf("wtr-a1", "wtr-a2") }
        assertEquals("Abono", credit.tipo)
        assertEquals(Triple(50.0, "Efectivo", today), Triple(credit.monto, credit.metodoPago, credit.fecha))
        assertEquals(0.0, db.dispensacionDao().getDispensacionById(origId, opticaId)!!.montoPagado, 0.001)
        assertEquals(250.0, db.dispensacionDao().getDispensacionById(winnerId, opticaId)!!.montoPagado, 0.001)
    }

    @Test
    fun residualTransfer_isIdempotentAndTellsTheUserOnce() = runTest {
        seedClaimedOriginal(unsyncedCredit = true)
        discarder()(opticaId)
        downloadWinner()

        discarder().transferResidualCredit(opticaId)
        val second = discarder().transferResidualCredit(opticaId)

        assertEquals(0, second)
        assertEquals(1, pagosOf(origId).count { it.reversaPagoId == "a3" })
        assertEquals(250.0, db.pagoDao().sumMontoByDispensacion(winnerId, opticaId), 0.001)
        val notice = db.syncEntityStateDao().getByStatus(opticaId, "error")
            .single { it.entityType == "reclamo_descartado" && it.entityId == "$origId:credito" }
        assertEquals(
            "El pago local de la OT 2026-0042 se transfirió a la orden 2026-0042-R1 del reclamo registrado en otro dispositivo.",
            notice.lastError,
        )
        assertFalse(notice.lastError.startsWith("quarantine:"))
    }

    @Test
    fun residualTransfer_waitsUntilTheDownloadReleasesTheOriginal() = runTest {
        seedClaimedOriginal(unsyncedCredit = true)
        discarder()(opticaId)

        assertEquals(0, discarder().transferResidualCredit(opticaId))

        downloadWinner()
        tracker.markAwaitingRemote(opticaId, "dispensacion", origId)
        assertEquals(0, discarder().transferResidualCredit(opticaId))
        assertTrue(pagosOf(origId).none { it.reversaPagoId == "a3" })
    }

    @Test
    fun adoptedWinnerWithoutLocalCredit_needsNoTransfer() = runTest {
        seedClaimedOriginal()
        discarder()(opticaId)
        downloadWinner()

        assertEquals(0, discarder().transferResidualCredit(opticaId))
        assertEquals(0.0, db.pagoDao().sumMontoByDispensacion(origId, opticaId), 0.001)
    }

    @Test
    fun failureMidTransfer_rollsBackEverything() = runTest {
        seedClaimedOriginal(unsyncedCredit = true)
        discarder()(opticaId)
        downloadWinner()
        val failing = spyk(repository)
        coEvery { failing.insertPago(match { it.dispensacionId == winnerId }) } throws IllegalStateException("insert failed")

        val error = runCatching { discarder(failing).transferResidualCredit(opticaId) }.exceptionOrNull()

        assertEquals("insert failed", error?.message)
        assertTrue(pagosOf(origId).none { it.reversaPagoId == "a3" })
        assertEquals(50.0, db.pagoDao().sumMontoByDispensacion(origId, opticaId), 0.001)
    }
}
