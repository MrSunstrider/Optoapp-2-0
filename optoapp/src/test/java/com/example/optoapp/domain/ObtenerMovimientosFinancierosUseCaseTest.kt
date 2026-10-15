package com.example.optoapp.domain

import com.example.optoapp.data.DispensacionItem
import com.example.optoapp.data.DispensacionOptica
import com.example.optoapp.data.OptoRepository
import com.example.optoapp.data.ServicioExtra
import com.example.optoapp.data.regalodispensacion.RegaloDispensacionEntity
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

class ObtenerMovimientosFinancierosUseCaseTest {

    private val opticaId = "o1"
    private val fecha = LocalDate.of(2026, 7, 10)
    private lateinit var repository: OptoRepository
    private lateinit var useCase: ObtenerMovimientosFinancierosUseCase

    @Before
    fun setUp() {
        repository = mockk(relaxed = true)
        coEvery { repository.getDispensacionesSnapshotForOptica(opticaId) } returns emptyList()
        coEvery { repository.getServiciosSnapshotForOptica(opticaId) } returns emptyList()
        coEvery { repository.getPagosSnapshotForOptica(opticaId) } returns emptyList()
        coEvery { repository.getDispensacionItemsSnapshotForOptica(opticaId) } returns emptyList()
        coEvery { repository.getRegalosSnapshotForOptica(opticaId) } returns emptyList()
        coEvery { repository.getServicioExtraItemsSnapshotForOptica(opticaId) } returns emptyList()
        coEvery { repository.getRegalosServicioExtraSnapshotForOptica(opticaId) } returns emptyList()
        useCase = ObtenerMovimientosFinancierosUseCase(repository)
    }

    private fun dispensacion() = DispensacionOptica(id = "d1", ot = "100", pacienteId = "p1", fecha = fecha, montoTotal = 200.0, opticaId = opticaId)

    private fun item(od: Double?, oi: Double?, origen: String?) = DispensacionItem(
        id = "i-$origen",
        dispensacionId = "d1",
        opticaId = opticaId,
        costoRealOd = od,
        costoRealOi = oi,
        costoOrigen = origen,
    )

    private fun servicio(realizadoPor: String?, costoReal: Double) = ServicioExtra(
        id = "s1",
        descripcion = "Reparacion",
        montoTotal = 100.0,
        estado = "Entregado",
        fecha = fecha,
        opticaId = opticaId,
        realizadoPor = realizadoPor,
        costoReal = costoReal,
    )

    private suspend fun venta(origen: Origen) =
        useCase(opticaId, fecha.withDayOfMonth(1), fecha.withDayOfMonth(31)).single { it.origen == origen }

    @Test
    fun dispensacion_withItemCosts_reportsRealCostAndCompleteCoverage() = runTest {
        coEvery { repository.getDispensacionesSnapshotForOptica(opticaId) } returns listOf(dispensacion())
        coEvery { repository.getDispensacionItemsSnapshotForOptica(opticaId) } returns listOf(item(70.0, 70.0, "matriz"))

        val mov = venta(Origen.DISPENSACION)

        assertEquals(140.0, mov.costo, 0.001)
        assertFalse(mov.costoIncompleto)
    }

    @Test
    fun dispensacion_giftCostStaysOnTheRegaloMovement() = runTest {
        coEvery { repository.getDispensacionesSnapshotForOptica(opticaId) } returns listOf(dispensacion())
        coEvery { repository.getDispensacionItemsSnapshotForOptica(opticaId) } returns listOf(item(70.0, 70.0, "matriz"))
        coEvery { repository.getRegalosSnapshotForOptica(opticaId) } returns listOf(
            RegaloDispensacionEntity(
                id = "r1",
                dispensacionId = "d1",
                productoId = "pr1",
                cantidad = 2,
                costoUnitario = 5.0,
                descripcion = "Estuche",
                opticaId = opticaId,
            ),
        )

        val movs = useCase(opticaId, fecha.withDayOfMonth(1), fecha.withDayOfMonth(31))

        assertEquals(140.0, movs.single { it.origen == Origen.DISPENSACION }.costo, 0.001)
        assertEquals(10.0, movs.single { it.origen == Origen.REGALO }.costo, 0.001)
    }

    @Test
    fun dispensacion_faltanteItem_flagsIncompleteCost() = runTest {
        coEvery { repository.getDispensacionesSnapshotForOptica(opticaId) } returns listOf(dispensacion())
        coEvery { repository.getDispensacionItemsSnapshotForOptica(opticaId) } returns listOf(item(70.0, null, "faltante"))

        assertTrue(venta(Origen.DISPENSACION).costoIncompleto)
    }

    @Test
    fun dispensacion_legacyZeroTotal_flagsIncompleteCost() = runTest {
        coEvery { repository.getDispensacionesSnapshotForOptica(opticaId) } returns listOf(dispensacion())

        val mov = venta(Origen.DISPENSACION)

        assertEquals(0.0, mov.costo, 0.001)
        assertTrue(mov.costoIncompleto)
    }

    @Test
    fun servicio_withExecutorCost_reportsRealCost() = runTest {
        coEvery { repository.getServiciosSnapshotForOptica(opticaId) } returns listOf(servicio("tercero", 60.0))

        val mov = venta(Origen.SERVICIO)

        assertEquals(60.0, mov.costo, 0.001)
        assertFalse(mov.costoIncompleto)
    }

    @Test
    fun servicio_withoutExecutorAndZeroCost_flagsIncompleteCost() = runTest {
        coEvery { repository.getServiciosSnapshotForOptica(opticaId) } returns listOf(servicio(null, 0.0))

        assertTrue(venta(Origen.SERVICIO).costoIncompleto)
    }
}
