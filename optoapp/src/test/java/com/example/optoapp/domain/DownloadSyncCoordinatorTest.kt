package com.example.optoapp.domain

import com.example.optoapp.data.DispensacionOptica
import com.example.optoapp.data.OptoRepository
import com.example.optoapp.data.Resource
import com.example.optoapp.data.ServicioExtra
import com.example.optoapp.data.SyncStateTracker
import com.example.optoapp.data.configuracionfinanciera.ConfiguracionFinancieraDao
import com.example.optoapp.data.costobiselado.CostoBiseladoDao
import com.example.optoapp.data.costoproducto.CostoProductoDao
import com.example.optoapp.data.resumendiario.ResumenDiarioDao
import io.github.jan.supabase.SupabaseClient
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class DownloadSyncCoordinatorTest {

    @Test
    fun constructor_takesNineDependencies() {
        val constructors = DownloadSyncCoordinator::class.java.declaredConstructors
        assertEquals(1, constructors.size)
        assertEquals(9, constructors[0].parameterTypes.size)
    }

    @Test
    fun injectAnnotation_isPresent() {
        val classHasInject = DownloadSyncCoordinator::class.java.annotations.any {
            it.annotationClass.qualifiedName?.contains("Inject") == true
        }
        val constructorHasInject = DownloadSyncCoordinator::class.java.declaredConstructors
            .flatMap { it.annotations.toList() }
            .any { it.annotationClass.qualifiedName?.contains("Inject") == true }
        assertTrue("DownloadSyncCoordinator debe tener @Inject", classHasInject || constructorHasInject)
    }

    @Test
    fun publicMethods_haveCorrectNames() {
        val methodNames = DownloadSyncCoordinator::class.java.declaredMethods.map { it.name }
        assertTrue("Debe tener downloadDispensacionItems", "downloadDispensacionItems" in methodNames)
        assertTrue("Debe tener downloadDispensaciones", "downloadDispensaciones" in methodNames)
        assertTrue("Debe tener downloadServicios", "downloadServicios" in methodNames)
        assertTrue("Debe tener downloadPagos", "downloadPagos" in methodNames)
    }

    @Test
    fun downloadMethods_existWithOpticaIdParam() {
        val downloadMethods = DownloadSyncCoordinator::class.java.declaredMethods
            .filter { it.name.startsWith("download") }
        assertTrue("Debe haber al menos un método download", downloadMethods.isNotEmpty())
        for (m in downloadMethods) {
            assertTrue(
                "Método ${m.name} debe aceptar String (opticaId)",
                m.parameterTypes.any { it == String::class.java },
            )
        }
    }

    @Test
    fun companion_hasTableConstants() {
        val allFields = DownloadSyncCoordinator::class.java.declaredFields.map { it.name }
        val expected = listOf("TABLE_DISPENSACIONES", "TABLE_DISPENSACION_ITEMS", "TABLE_SERVICIOS", "TABLE_PAGOS")
        for (expectedName in expected) {
            assertTrue(
                "Debe existir $expectedName (found: $allFields)",
                allFields.any { it == expectedName || it.contains(expectedName) },
            )
        }
    }

    // ── Local terminal estado wins over remote non-terminal ───────────

    private val opticaId = "o1"
    private val repository = mockk<OptoRepository>(relaxed = true)
    private val syncStateTracker = mockk<SyncStateTracker>(relaxed = true)
    private val deletionSyncHelper = mockk<DeletionSyncHelper>(relaxed = true)
    private val coordinator = DownloadSyncCoordinator(
        repository = repository,
        supabase = mockk<SupabaseClient>(relaxed = true),
        syncStateTracker = syncStateTracker,
        deletionSyncHelper = deletionSyncHelper,
        networkRetryHelper = mockk<NetworkRetryHelper>(relaxed = true),
        resumenDiarioDao = mockk<ResumenDiarioDao>(relaxed = true),
        configuracionFinancieraDao = mockk<ConfiguracionFinancieraDao>(relaxed = true),
        costoProductoDao = mockk<CostoProductoDao>(relaxed = true),
        costoBiseladoDao = mockk<CostoBiseladoDao>(relaxed = true),
    )

    private fun stubSyncInfra() {
        coEvery { deletionSyncHelper.deletedIds(opticaId) } returns emptySet()
        coEvery { syncStateTracker.quarantinedEntityIds(opticaId, any()) } returns emptySet()
        coEvery { repository.withTransaction(any<suspend () -> Any?>()) } coAnswers {
            firstArg<suspend () -> Any?>().invoke()
        }
    }

    private fun localDisp(estado: String, motivo: String? = null, fecha: LocalDate? = null) = DispensacionOptica(
        id = "d1", ot = "2026-0042", pacienteId = "p1", fecha = LocalDate.of(2026, 9, 1), opticaId = opticaId,
        estadoEntrega = estado, motivoAnulacion = motivo, fechaAnulacion = fecha,
    )

    private fun remoteDisp(estado: String?, motivo: String? = null, fecha: String? = null) = DispensacionRemota(
        id = "d1", ot = "2026-0042", pacienteId = "p1", fecha = "2026-09-01", opticaId = opticaId,
        estadoEntrega = estado, motivoAnulacion = motivo, fechaAnulacion = fecha,
    )

    private fun localServicio(estado: String) = ServicioExtra(
        id = "s1", ot = "S-001", descripcion = "Biselado", montoTotal = 50.0, aCuenta = 0.0, estado = estado,
        fecha = LocalDate.of(2026, 9, 1), opticaId = opticaId,
        motivoAnulacion = "Cliente desistió", fechaAnulacion = LocalDate.of(2026, 9, 30),
    )

    private fun remoteServicio(estado: String) = ServicioRemoto(
        id = "s1", ot = "S-001", descripcion = "Biselado", montoTotal = 50.0, estado = estado,
        fecha = "2026-09-01", opticaId = opticaId,
    )

    @Test
    fun remotePendiente_doesNotRevertLocalAnuladoDispensacion() = runTest {
        stubSyncInfra()
        coEvery { repository.getDispensacionById("d1", opticaId) } returns
            Resource.Success(localDisp("Anulado", "Cliente desistió", LocalDate.of(2026, 9, 30)))

        val persisted = coordinator.persistDispensaciones(opticaId, listOf(remoteDisp("Pendiente")))

        assertEquals(0, persisted)
        coVerify(exactly = 0) { repository.upsertDispensacionFromRemote(any()) }
        coVerify(exactly = 0) { syncStateTracker.markSynced(opticaId, "dispensacion", "d1") }
    }

    @Test
    fun remoteEntregado_doesNotRevertLocalReclamadaDispensacion() = runTest {
        stubSyncInfra()
        coEvery { repository.getDispensacionById("d1", opticaId) } returns
            Resource.Success(localDisp("Reclamada", "Lente rayado", LocalDate.of(2026, 9, 25)))

        val persisted = coordinator.persistDispensaciones(opticaId, listOf(remoteDisp("Entregado")))

        assertEquals(0, persisted)
        coVerify(exactly = 0) { repository.upsertDispensacionFromRemote(any()) }
        coVerify(exactly = 0) { syncStateTracker.markSynced(opticaId, "dispensacion", "d1") }
    }

    @Test
    fun remoteAnulado_appliesOverLocalNonTerminalWithItsMetadata() = runTest {
        stubSyncInfra()
        coEvery { repository.getDispensacionById("d1", opticaId) } returns Resource.Success(localDisp("Entregado"))
        val upserted = slot<DispensacionOptica>()
        coEvery { repository.upsertDispensacionFromRemote(capture(upserted)) } returns Unit

        val persisted = coordinator.persistDispensaciones(
            opticaId,
            listOf(remoteDisp("Anulado", "Error de registro", "2026-09-29")),
        )

        assertEquals(1, persisted)
        assertEquals("Anulado", upserted.captured.estadoEntrega)
        assertEquals("Error de registro", upserted.captured.motivoAnulacion)
        assertEquals(LocalDate.of(2026, 9, 29), upserted.captured.fechaAnulacion)
        coVerify { syncStateTracker.markSynced(opticaId, "dispensacion", "d1") }
    }

    @Test
    fun missingLocalDispensacion_isUpserted() = runTest {
        stubSyncInfra()
        coEvery { repository.getDispensacionById("d1", opticaId) } returns Resource.Error("Dispensación no encontrada")

        val persisted = coordinator.persistDispensaciones(opticaId, listOf(remoteDisp("Pendiente")))

        assertEquals(1, persisted)
        coVerify { repository.upsertDispensacionFromRemote(match { it.id == "d1" && it.estadoEntrega == "Pendiente" }) }
    }

    @Test
    fun remoteEntregado_doesNotRevertLocalAnuladoServicio() = runTest {
        stubSyncInfra()
        coEvery { repository.getServicioById("s1", opticaId) } returns Resource.Success(localServicio("Anulado"))

        val persisted = coordinator.persistServicios(opticaId, listOf(remoteServicio("Entregado")))

        assertEquals(0, persisted)
        coVerify(exactly = 0) { repository.upsertServicioFromRemote(any()) }
        coVerify(exactly = 0) { syncStateTracker.markSynced(opticaId, "servicio_extra", "s1") }
    }

    @Test
    fun remoteServicioSameTerminalEstado_isAppliedNormally() = runTest {
        stubSyncInfra()
        coEvery { repository.getServicioById("s1", opticaId) } returns Resource.Success(localServicio("Anulado"))

        val persisted = coordinator.persistServicios(opticaId, listOf(remoteServicio("Anulado")))

        assertEquals(1, persisted)
        coVerify { repository.upsertServicioFromRemote(match { it.id == "s1" && it.estado == "Anulado" }) }
    }

    @Test
    fun pendingLocalDeletion_skipsRemoteDispensacion() = runTest {
        stubSyncInfra()
        coEvery { deletionSyncHelper.deletedIds(opticaId) } returns setOf("d1")

        val persisted = coordinator.persistDispensaciones(opticaId, listOf(remoteDisp("Pendiente")))

        assertEquals(0, persisted)
        coVerify(exactly = 0) { repository.upsertDispensacionFromRemote(any()) }
    }
}
