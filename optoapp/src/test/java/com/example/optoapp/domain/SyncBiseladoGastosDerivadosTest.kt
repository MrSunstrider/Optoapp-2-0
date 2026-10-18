package com.example.optoapp.domain

import com.example.optoapp.data.DispensacionItem
import com.example.optoapp.data.gastooperativo.GastoOperativoDao
import com.example.optoapp.data.gastooperativo.GastoOperativoEntity
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate

class SyncBiseladoGastosDerivadosTest {

    private val opticaId = "o1"
    private val fecha = LocalDate.of(2026, 10, 18)
    private lateinit var dao: GastoOperativoDao

    @Before
    fun setUp() {
        dao = mockk(relaxed = true)
        coEvery { dao.getByOrigenDispensacionItemId(any(), any()) } returns null
    }

    private fun item(
        id: String = "item-1",
        origen: String = OrigenBiselado.TERCERO,
        costoBiselado: Double? = 25.0,
    ) = DispensacionItem(
        id = id,
        dispensacionId = "d1",
        tipoLente = "Monofocal",
        materialLente = "Resina",
        tratamientos = emptyList(),
        origenMontura = "Paciente",
        opticaId = opticaId,
        origenBiselado = origen,
        costoRealBiselado = costoBiselado,
    )

    @Test
    fun terceroWithPositiveCost_upsertsLinkedVariableGasto() = runTest {
        val captured = slot<GastoOperativoEntity>()
        coEvery { dao.upsert(capture(captured)) } returns Unit

        SyncBiseladoGastosDerivados.sync(dao, opticaId, "OT-1", fecha, listOf(item()), emptyList())

        coVerify(exactly = 1) { dao.upsert(any()) }
        assertEquals("gb_item-1", captured.captured.id)
        assertEquals("Biselado", captured.captured.categoria)
        assertEquals("Biselado OT OT-1", captured.captured.descripcion)
        assertEquals(BigDecimal.valueOf(25.0), captured.captured.monto)
        assertEquals(GastoTipoResolver.VARIABLE, captured.captured.tipoGasto)
        assertEquals("item-1", captured.captured.origenDispensacionItemId)
    }

    @Test
    fun propio_removesExistingLinkedGasto() = runTest {
        val existing = GastoOperativoEntity(
            id = "gb_old",
            opticaId = opticaId,
            categoria = "Biselado",
            monto = BigDecimal("10"),
            fecha = fecha,
            origenDispensacionItemId = "item-1",
        )
        coEvery { dao.getByOrigenDispensacionItemId(opticaId, "item-1") } returns existing

        SyncBiseladoGastosDerivados.sync(
            dao, opticaId, "OT-1", fecha,
            listOf(item(origen = OrigenBiselado.PROPIO, costoBiselado = 12.0)),
            emptyList(),
        )

        coVerify(exactly = 1) { dao.delete("gb_old", opticaId) }
        coVerify(exactly = 0) { dao.upsert(any()) }
    }

    @Test
    fun terceroWithZeroCost_removesExistingLinkedGasto() = runTest {
        val existing = GastoOperativoEntity(
            id = "gb_old",
            opticaId = opticaId,
            categoria = "Biselado",
            monto = BigDecimal("10"),
            fecha = fecha,
            origenDispensacionItemId = "item-1",
        )
        coEvery { dao.getByOrigenDispensacionItemId(opticaId, "item-1") } returns existing

        SyncBiseladoGastosDerivados.sync(
            dao, opticaId, "OT-1", fecha,
            listOf(item(costoBiselado = 0.0)),
            emptyList(),
        )

        coVerify(exactly = 1) { dao.delete("gb_old", opticaId) }
        coVerify(exactly = 0) { dao.upsert(any()) }
    }

    @Test
    fun deletedItems_removeLinkedGastos() = runTest {
        val existing = GastoOperativoEntity(
            id = "gb_gone",
            opticaId = opticaId,
            categoria = "Biselado",
            monto = BigDecimal("8"),
            fecha = fecha,
            origenDispensacionItemId = "gone",
        )
        coEvery { dao.getByOrigenDispensacionItemId(opticaId, "gone") } returns existing

        SyncBiseladoGastosDerivados.sync(dao, opticaId, "OT-1", fecha, emptyList(), listOf("gone"))

        coVerify(exactly = 1) { dao.delete("gb_gone", opticaId) }
    }

    @Test
    fun terceroUpsert_reusesExistingId() = runTest {
        val existing = GastoOperativoEntity(
            id = "gb_keep",
            opticaId = opticaId,
            categoria = "Biselado",
            monto = BigDecimal("5"),
            fecha = fecha,
            origenDispensacionItemId = "item-1",
            createdAt = "2026-01-01T00:00:00Z",
        )
        coEvery { dao.getByOrigenDispensacionItemId(opticaId, "item-1") } returns existing
        val captured = slot<GastoOperativoEntity>()
        coEvery { dao.upsert(capture(captured)) } returns Unit

        SyncBiseladoGastosDerivados.sync(
            dao, opticaId, "OT-1", fecha,
            listOf(item(costoBiselado = 30.0)),
            emptyList(),
        )

        assertEquals("gb_keep", captured.captured.id)
        assertEquals("2026-01-01T00:00:00Z", captured.captured.createdAt)
        assertEquals(BigDecimal.valueOf(30.0), captured.captured.monto)
    }
}
