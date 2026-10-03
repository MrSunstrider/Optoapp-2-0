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
import io.mockk.coVerify
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

    private val syncedCredits = listOf(Triple("a1", 120.0, "Efectivo"), Triple("a2", 80.0, "Tarjeta"))

    private suspend fun seedClaimedOriginal(
        frameStock: Int = 3,
        unsyncedCredit: Boolean = false,
        lateSyncedCredit: Boolean = false,
        legacyReembolso: Boolean = false,
        losing: Boolean = true,
    ): String {
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
        syncedCredits.forEach { (id, monto, metodo) ->
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
        if (lateSyncedCredit) {
            db.pagoDao().insertPago(
                Pago(id = "a4", dispensacionId = origId, fecha = today.minusDays(3), tipo = "Abono", monto = 30.0, metodoPago = "Tarjeta", opticaId = opticaId),
            )
            tracker.markSynced(opticaId, "pago", "a4")
        }
        if (legacyReembolso) {
            db.pagoDao().insertPago(
                Pago(id = "rb1", dispensacionId = origId, fecha = today.minusDays(33), tipo = "Reembolso", monto = 20.0, metodoPago = "Efectivo", opticaId = opticaId),
            )
            tracker.markSynced(opticaId, "pago", "rb1")
        }
        val created = claim(origId, "Lente rayado", 150.0, "Tarjeta")
        if (losing) tracker.markError(opticaId, "dispensacion", created, "quarantine:reclamo_duplicate:$origId")
        return created
    }

    private suspend fun claim(originalId: String, motivo: String, nuevoMontoTotal: Double, metodo: String): String =
        (
            ReclamarDispensacionUseCase(
                repository, db.pagoDao(), stockHelper, mockk(relaxed = true), CalcularMontoPagadoUseCase(db.pagoDao()),
            )(originalId, opticaId, motivo, nuevoMontoTotal, metodo) as ReclamoOutcome.Created
            ).replacementId

    private suspend fun seedSecondLosingClaim(): String {
        db.dispensacionDao().insertDispensacion(
            DispensacionOptica(
                id = "d2", ot = "2026-0043", pacienteId = "pac", fecha = today.minusDays(20), opticaId = opticaId,
                estadoEntrega = "Entregado", fechaEntrega = today.minusDays(10),
            ),
        )
        val replacementId = claim("d2", "Armazón roto", 0.0, "")
        tracker.markError(opticaId, "dispensacion", replacementId, "quarantine:reclamo_duplicate:d2")
        return replacementId
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

        val awaiting = db.syncEntityStateDao().getByStatus(opticaId, "awaiting_remote")
            .map { it.entityType to it.entityId }.toSet()
        assertEquals(setOf("dispensacion" to origId, "reclamo_credito" to origId), awaiting)
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

        assertEquals(0, discarder(failing)(opticaId))

        assertNotNull(db.dispensacionDao().getDispensacionById(replId, opticaId))
        assertTrue(tracker.awaitingRemoteIds(opticaId, "dispensacion").isEmpty())
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

    /**
     * What the download brings: the winner's replacement, its transfers and the Reversos of the credits
     * the winner knew. [remoteOriginalId] differs from the local id when the original adopted a remote id by OT.
     */
    private suspend fun downloadWinner(remoteOriginalId: String = origId, remoteOriginalEstado: String = "Reclamada") {
        if (remoteOriginalId != origId) {
            db.dispensacionDao().insertDispensacion(
                DispensacionOptica(
                    id = remoteOriginalId, ot = "2026-0042", pacienteId = "pac", fecha = today.minusDays(40), opticaId = opticaId,
                    montoTotal = 200.0, estadoEntrega = remoteOriginalEstado, fechaEntrega = today.minusDays(30),
                    motivoAnulacion = "Armazón roto", fechaAnulacion = today,
                ),
            )
            syncedCredits.forEach { (id, monto, metodo) ->
                db.pagoDao().insertPago(
                    Pago(id = id, dispensacionId = remoteOriginalId, fecha = today.minusDays(35), tipo = "Abono", monto = monto, metodoPago = metodo, opticaId = opticaId),
                )
            }
        }
        db.dispensacionDao().insertDispensacion(
            DispensacionOptica(
                id = winnerId, ot = "2026-0042-R1", pacienteId = "pac", fecha = today, opticaId = opticaId,
                montoTotal = 150.0, montoPagado = 200.0, estadoEntrega = "Pendiente", reclamoOrigenId = remoteOriginalId,
            ),
        )
        tracker.markSynced(opticaId, "dispensacion", winnerId)
        syncedCredits.forEach { (creditId, monto, metodo) ->
            db.pagoDao().insertPago(
                Pago(id = "wrv-$creditId", dispensacionId = remoteOriginalId, fecha = today, tipo = "Reverso", monto = monto, metodoPago = metodo, opticaId = opticaId, reversaPagoId = creditId),
            )
            db.pagoDao().insertPago(
                Pago(id = "wtr-$creditId", dispensacionId = winnerId, fecha = today, tipo = "Abono", monto = monto, metodoPago = metodo, opticaId = opticaId),
            )
            listOf("wrv-$creditId", "wtr-$creditId").forEach { tracker.markSynced(opticaId, "pago", it) }
        }
        tracker.markSynced(opticaId, "dispensacion", remoteOriginalId)
        tracker.markSynced(opticaId, "download_pago", "batch")
    }

    @Test
    fun remappedOriginal_isFoldedIntoTheDownloadedOriginalAndItsHoldReleased() = runTest {
        seedClaimedOriginal(unsyncedCredit = true)
        discarder()(opticaId)
        assertEquals(setOf(origId), tracker.awaitingRemoteIds(opticaId, "dispensacion"))
        downloadWinner(remoteOriginalId = "r1")

        val transferred = discarder().transferResidualCredit(opticaId)

        assertEquals(1, transferred)
        assertNull(db.dispensacionDao().getDispensacionById(origId, opticaId))
        assertTrue(tracker.awaitingRemoteIds(opticaId, "dispensacion").isEmpty())
        assertTrue(pagosOf(origId).isEmpty())
        assertEquals(1, pagosOf("r1").count { it.tipo == "Reverso" && it.reversaPagoId == "a3" })
        assertEquals(0.0, db.pagoDao().sumMontoByDispensacion("r1", opticaId), 0.001)
        assertEquals(250.0, db.pagoDao().sumMontoByDispensacion(winnerId, opticaId), 0.001)
        assertEquals(listOf("r1"), db.dispensacionItemDao().getItemsListByDispensacion("r1", opticaId).map { it.dispensacionId })
    }

    private suspend fun pendingCreditMarkers() = tracker.awaitingRemoteIds(opticaId, "reclamo_credito")

    @Test
    fun remappedOriginal_waitsUntilTheDownloadedTwinIsReclamada() = runTest {
        seedClaimedOriginal(unsyncedCredit = true)
        discarder()(opticaId)
        downloadWinner(remoteOriginalId = "r1", remoteOriginalEstado = "Entregado")

        assertEquals(0, discarder().transferResidualCredit(opticaId))

        assertNotNull(db.dispensacionDao().getDispensacionById(origId, opticaId))
        assertEquals(setOf(origId), pendingCreditMarkers())
        assertEquals(1, pagosOf(origId).count { it.id == "a3" })

        val twin = db.dispensacionDao().getDispensacionById("r1", opticaId)!!
        db.dispensacionDao().insertDispensacion(twin.copy(estadoEntrega = "Reclamada"))
        tracker.markSynced(opticaId, "download_pago", "batch")

        assertEquals(1, discarder().transferResidualCredit(opticaId))
        assertNull(db.dispensacionDao().getDispensacionById(origId, opticaId))
        assertTrue(pendingCreditMarkers().isEmpty())
        assertEquals(0.0, db.pagoDao().sumMontoByDispensacion("r1", opticaId), 0.001)
    }

    @Test
    fun failureAfterTheFold_rollsBackTheFoldAndKeepsTheMarker() = runTest {
        seedClaimedOriginal(unsyncedCredit = true)
        discarder()(opticaId)
        downloadWinner(remoteOriginalId = "r1")
        val failing = spyk(repository)
        coEvery { failing.insertPago(match { it.dispensacionId == winnerId }) } throws IllegalStateException("insert failed")

        val error = runCatching { discarder(failing).transferResidualCredit(opticaId) }.exceptionOrNull()

        assertEquals("insert failed", error?.message)
        assertNotNull(db.dispensacionDao().getDispensacionById(origId, opticaId))
        assertEquals(1, pagosOf(origId).count { it.id == "a3" })
        assertEquals(setOf(origId), pendingCreditMarkers())
        assertEquals(setOf(origId), tracker.awaitingRemoteIds(opticaId, "dispensacion"))
    }

    @Test
    fun residualTransfer_needsAPagosDownloadFromTheSameRun() = runTest {
        seedClaimedOriginal(unsyncedCredit = true)
        discarder()(opticaId)
        downloadWinner()
        tracker.markAwaitingRemote(opticaId, "dispensacion", origId)
        assertEquals(0, discarder().transferResidualCredit(opticaId))

        tracker.markSynced(opticaId, "dispensacion", origId)
        assertEquals(0, discarder().transferResidualCredit(opticaId))
        assertTrue(pagosOf(origId).none { it.reversaPagoId == "a3" })

        tracker.markSynced(opticaId, "download_pago", "batch")
        assertEquals(1, discarder().transferResidualCredit(opticaId))
    }

    @Test
    fun syncedCreditTheWinnerNeverReversed_isTransferredToTheWinner() = runTest {
        seedClaimedOriginal(lateSyncedCredit = true)
        discarder()(opticaId)
        downloadWinner()

        val transferred = discarder().transferResidualCredit(opticaId)

        assertEquals(1, transferred)
        assertEquals(1, pagosOf(origId).count { it.tipo == "Reverso" && it.reversaPagoId == "a4" })
        assertEquals(0.0, db.pagoDao().sumMontoByDispensacion(origId, opticaId), 0.001)
        assertEquals(230.0, db.pagoDao().sumMontoByDispensacion(winnerId, opticaId), 0.001)
        assertEquals(0, discarder().transferResidualCredit(opticaId))
    }

    @Test
    fun residualTransfer_waitsForASuccessfulPagosDownload() = runTest {
        seedClaimedOriginal(unsyncedCredit = true)
        discarder()(opticaId)
        downloadWinner()
        tracker.markError(opticaId, "download_pago", "batch", "timeout")

        assertEquals(0, discarder().transferResidualCredit(opticaId))
        assertTrue(pagosOf(origId).none { it.reversaPagoId == "a3" })

        tracker.markSynced(opticaId, "download_pago", "batch")
        assertEquals(1, discarder().transferResidualCredit(opticaId))
    }

    @Test
    fun failedPagosDownload_staysRecordedAfterTheResidualTransfer() = runTest {
        tracker.markError(opticaId, "download_pago", "batch", "timeout")

        assertEquals(0, discarder().transferResidualCredit(opticaId))

        assertEquals(
            listOf("batch"),
            db.syncEntityStateDao().getByStatus(opticaId, "error").filter { it.entityType == "download_pago" }.map { it.entityId },
        )
    }

    @Test
    fun ownConfirmedClaim_keepsItsUnsyncedCompensationAbonoOnTheOriginal() = runTest {
        val replId = seedClaimedOriginal(legacyReembolso = true, losing = false)
        listOf(replId, origId).forEach { tracker.markSynced(opticaId, "dispensacion", it) }
        tracker.markSynced(opticaId, "download_pago", "batch")
        val originalLedger = pagosOf(origId).toSet()
        val replacementLedger = pagosOf(replId).toSet()
        assertTrue(originalLedger.any { it.tipo == "Abono" && it.nota.startsWith(NOTA_COMPENSACION_PREFIX) })

        assertEquals(0, discarder().transferResidualCredit(opticaId))

        assertEquals(originalLedger, pagosOf(origId).toSet())
        assertEquals(replacementLedger, pagosOf(replId).toSet())
    }

    // ── Per-claim isolation ───────

    @Test
    fun failingClaim_doesNotBlockTheOthersAndIsNotRetriedEverySync() = runTest {
        val failingId = seedClaimedOriginal()
        val otherId = seedSecondLosingClaim()
        val failing = spyk(repository)
        coEvery { failing.deleteDispensacionById(failingId, opticaId) } throws IllegalStateException("delete failed")

        assertEquals(1, discarder(failing)(opticaId))

        assertNull(db.dispensacionDao().getDispensacionById(otherId, opticaId))
        assertNotNull(db.dispensacionDao().getDispensacionById(failingId, opticaId))
        val notice = tracker.quarantineReasons(opticaId, "reclamo_descartado")[failingId]
        assertEquals(
            "quarantine:reclamo_descarte_fallido: No se pudo descartar el reclamo local de la OT 2026-0042 (delete failed).",
            notice,
        )

        tracker.markError(opticaId, "dispensacion", failingId, "quarantine:reclamo_duplicate:$origId")
        assertEquals(0, discarder(failing)(opticaId))
        coVerify(exactly = 1) { failing.deleteDispensacionById(failingId, opticaId) }
    }

    @Test
    fun claimWhoseMonturaWasDeleted_isDiscardedWithNothingToRestock() = runTest {
        val replId = seedClaimedOriginal()
        db.monturaDao().deleteMontura("M1", opticaId)
        assertTrue(movimientos().isEmpty())

        assertEquals(1, discarder()(opticaId))

        assertNull(db.dispensacionDao().getDispensacionById(replId, opticaId))
        assertTrue(movimientos().isEmpty())
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

    private suspend fun heldPagosNotices() = db.syncEntityStateDao().getByStatus(opticaId, "error")
        .filter { it.entityType == "reclamo_descartado" && it.entityId == "$origId:credito_pendiente" }

    @Test
    fun pagosHeldByALocalQuarantine_tellTheUserTheCreditIsPendingOnce() = runTest {
        seedClaimedOriginal(unsyncedCredit = true)
        discarder()(opticaId)
        tracker.markSynced(opticaId, "download_pago_blocked", "batch")

        assertEquals(0, discarder().transferResidualCredit(opticaId))
        assertEquals(0, discarder().transferResidualCredit(opticaId))

        val notice = heldPagosNotices().single()
        assertEquals(
            "El crédito del reclamo de la OT 2026-0042 queda pendiente hasta resolver los pagos en espera.",
            notice.lastError,
        )
        assertFalse(notice.lastError.startsWith("quarantine:"))
        assertEquals(setOf(origId), pendingCreditMarkers())
    }

    @Test
    fun pagosDownloadFailedByNetwork_doesNotTellTheUserTheCreditIsPending() = runTest {
        seedClaimedOriginal(unsyncedCredit = true)
        discarder()(opticaId)
        tracker.markError(opticaId, "download_pago", "batch", "timeout")

        assertEquals(0, discarder().transferResidualCredit(opticaId))

        assertTrue(heldPagosNotices().isEmpty())
    }

    @Test
    fun pagosHeldLocallyWithoutAPendingCredit_tellTheUserNothing() = runTest {
        tracker.markSynced(opticaId, "download_pago_blocked", "batch")

        assertEquals(0, discarder().transferResidualCredit(opticaId))

        assertTrue(heldPagosNotices().isEmpty())
    }

    private suspend fun reverseEveryCreditOfTheOriginal() {
        val existing = pagosOf(origId).map { it.id }.toSet()
        (syncedCredits + Triple("a3", 50.0, "Efectivo")).filter { (creditId, _, _) -> creditId in existing }.forEach { (creditId, monto, metodo) ->
            db.pagoDao().insertPago(
                Pago(id = "wrv-$creditId", dispensacionId = origId, fecha = today, tipo = "Reverso", monto = monto, metodoPago = metodo, opticaId = opticaId, reversaPagoId = creditId),
            )
            tracker.markSynced(opticaId, "pago", "wrv-$creditId")
        }
    }

    @Test
    fun pagosHeldLocallyWithAMarkerButNoResidualCredit_tellTheUserNothing() = runTest {
        seedClaimedOriginal()
        discarder()(opticaId)
        reverseEveryCreditOfTheOriginal()
        tracker.markSynced(opticaId, "download_pago_blocked", "batch")

        assertEquals(0, discarder().transferResidualCredit(opticaId))

        assertTrue(heldPagosNotices().isEmpty())
        assertEquals(setOf(origId), pendingCreditMarkers())
    }

    @Test
    fun pendingCreditNotice_isDroppedWhenNoResidualCreditRemainsWhileThePagosStayBlocked() = runTest {
        seedClaimedOriginal(unsyncedCredit = true)
        discarder()(opticaId)
        tracker.markSynced(opticaId, "download_pago_blocked", "batch")
        discarder().transferResidualCredit(opticaId)
        assertEquals(1, heldPagosNotices().size)

        reverseEveryCreditOfTheOriginal()
        discarder().transferResidualCredit(opticaId)

        assertTrue(heldPagosNotices().isEmpty())
    }

    @Test
    fun pendingCreditNotice_isClearedWhenTheTransferRuns() = runTest {
        seedClaimedOriginal(unsyncedCredit = true)
        discarder()(opticaId)
        tracker.markSynced(opticaId, "download_pago_blocked", "batch")
        discarder().transferResidualCredit(opticaId)
        assertEquals(1, heldPagosNotices().size)

        tracker.clear(opticaId, "download_pago_blocked", "batch")
        downloadWinner()
        assertEquals(1, discarder().transferResidualCredit(opticaId))

        assertTrue(heldPagosNotices().isEmpty())
        assertEquals(1, db.syncEntityStateDao().getByStatus(opticaId, "error").count { it.entityId == "$origId:credito" })
    }

    @Test
    fun pendingCreditNotice_isClearedWhenTheOriginalIsGone() = runTest {
        seedClaimedOriginal(unsyncedCredit = true)
        discarder()(opticaId)
        tracker.markSynced(opticaId, "download_pago_blocked", "batch")
        discarder().transferResidualCredit(opticaId)
        db.dispensacionDao().deleteById(origId, opticaId)
        tracker.markSynced(opticaId, "download_pago", "batch")

        discarder().transferResidualCredit(opticaId)

        assertTrue(heldPagosNotices().isEmpty())
        assertTrue(pendingCreditMarkers().isEmpty())
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
