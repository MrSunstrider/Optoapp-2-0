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
    private lateinit var deleteLinked: suspend (GastoOperativoEntity) -> Unit
    private lateinit var upsertLinked: suspend (GastoOperativoEntity) -> Unit
    private val deleted = mutableListOf<GastoOperativoEntity>()
    private val upserted = mutableListOf<GastoOperativoEntity>()

    @Before
    fun setUp() {
        dao = mockk(relaxed = true)
        coEvery { dao.getByOrigenDispensacionItemId(any(), any()) } returns null
        deleted.clear()
        upserted.clear()
        deleteLinked = { deleted += it }
        upsertLinked = { upserted += it }
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

    private suspend fun sync(
        items: List<DispensacionItem>,
        deletedItemIds: List<String> = emptyList(),
        ot: String = "OT-1",
    ) = SyncBiseladoGastosDerivados.sync(
        gastoOperativoDao = dao,
        opticaId = opticaId,
        ot = ot,
        fecha = fecha,
        items = items,
        deletedItemIds = deletedItemIds,
        deleteLinked = deleteLinked,
        upsertLinked = upsertLinked,
    )

    @Test
    fun terceroWithPositiveCost_upsertsLinkedVariableGasto() = runTest {
        sync(listOf(item()))

        assertEquals(1, upserted.size)
        assertEquals(0, deleted.size)
        val captured = upserted.single()
        assertEquals("gb_item-1", captured.id)
        assertEquals("Biselado", captured.categoria)
        assertEquals("Biselado OT OT-1", captured.descripcion)
        assertEquals(BigDecimal.valueOf(25.0), captured.monto)
        assertEquals(GastoTipoResolver.VARIABLE, captured.tipoGasto)
        assertEquals("item-1", captured.origenDispensacionItemId)
        coVerify(exactly = 0) { dao.delete(any(), any()) }
        coVerify(exactly = 0) { dao.upsert(any()) }
    }

    @Test
    fun propio_removesExistingLinkedGastoViaDeleteCallback() = runTest {
        val existing = GastoOperativoEntity(
            id = "gb_old",
            opticaId = opticaId,
            categoria = "Biselado",
            monto = BigDecimal("10"),
            fecha = fecha,
            origenDispensacionItemId = "item-1",
        )
        coEvery { dao.getByOrigenDispensacionItemId(opticaId, "item-1") } returns existing

        sync(listOf(item(origen = OrigenBiselado.PROPIO, costoBiselado = 12.0)))

        assertEquals(listOf("gb_old"), deleted.map { it.id })
        assertEquals(0, upserted.size)
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

        sync(listOf(item(costoBiselado = 0.0)))

        assertEquals(listOf("gb_old"), deleted.map { it.id })
        assertEquals(0, upserted.size)
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

        sync(emptyList(), deletedItemIds = listOf("gone"))

        assertEquals(listOf("gb_gone"), deleted.map { it.id })
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

        sync(listOf(item(costoBiselado = 30.0)))

        val captured = upserted.single()
        assertEquals("gb_keep", captured.id)
        assertEquals("2026-01-01T00:00:00Z", captured.createdAt)
        assertEquals(BigDecimal.valueOf(30.0), captured.monto)
    }
}
