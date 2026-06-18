package com.example.optoapp.util

import com.example.optoapp.data.Montura
import com.example.optoapp.data.montura.MonturaDao
import com.example.optoapp.data.MonturaMovimiento
import com.example.optoapp.data.montura.MonturaMovimientoDao
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

class DispensacionStockHelperTest {

    private lateinit var monturaDao: FakeMonturaDao
    private lateinit var movimientoDao: FakeMonturaMovimientoDao
    private lateinit var helper: DispensacionStockHelper

    @Before
    fun setUp() {
        monturaDao = FakeMonturaDao()
        movimientoDao = FakeMonturaMovimientoDao()
        helper = DispensacionStockHelper(monturaDao, movimientoDao)
    }

    @Test
    fun adjustStock_reduceStock_returnsAffectedRows() = runTest {
        monturaDao.addMontura(Montura(id = "m1", opticaId = "o1", stockActual = 5))

        val result = helper.adjustStock("m1", "o1", -1)

        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrNull())
        assertEquals(4, monturaDao.getMonturaById("m1")?.stockActual)
    }

    @Test
    fun adjustStock_insufficientStock_returnsFailure() = runTest {
        monturaDao.addMontura(Montura(id = "m1", opticaId = "o1", stockActual = 0))

        val result = helper.adjustStock("m1", "o1", -1)

        assertTrue(result.isFailure)
        assertEquals(0, monturaDao.getMonturaById("m1")?.stockActual)
    }

    @Test
    fun adjustStock_increaseStock_returnsAffectedRows() = runTest {
        monturaDao.addMontura(Montura(id = "m1", opticaId = "o1", stockActual = 3))

        val result = helper.adjustStock("m1", "o1", 5)

        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrNull())
        assertEquals(8, monturaDao.getMonturaById("m1")?.stockActual)
    }

    @Test
    fun adjustStock_nonExistentMontura_returnsFailure() = runTest {
        val result = helper.adjustStock("nonexistent", "o1", -1)

        assertTrue(result.isFailure)
    }

    @Test
    fun adjustStock_wrongOpticaId_returnsFailure() = runTest {
        monturaDao.addMontura(Montura(id = "m1", opticaId = "o1", stockActual = 5))

        val result = helper.adjustStock("m1", "o2", -1)

        assertTrue(result.isFailure)
        assertEquals(5, monturaDao.getMonturaById("m1")?.stockActual)
    }

    @Test
    fun registrarMovimiento_insertsMovementRecord() = runTest {
        helper.registrarMovimiento(
            monturaId = "m1",
            opticaId = "o1",
            tipo = "SALIDA_VENTA",
            cantidad = 1,
            stockPrevio = 5,
            referenciaId = "disp-1",
            nota = "Salida por venta"
        )

        val movimientos = movimientoDao.getAllMovimientos()
        assertEquals(1, movimientos.size)
        val mov = movimientos[0]
        assertEquals("m1", mov.monturaId)
        assertEquals("SALIDA_VENTA", mov.tipo)
        assertEquals(1, mov.cantidad)
        assertEquals(5, mov.stockPrevio)
        assertEquals(6, mov.stockNuevo)  // stockPrevio + cantidad
        assertEquals("disp-1", mov.referenciaId)
        assertEquals("Salida por venta", mov.nota)
        assertEquals("o1", mov.opticaId)
        assertNotNull(mov.id)
        assertTrue(mov.id.isNotEmpty())
    }

    @Test
    fun registrarMovimiento_multipleCalls_storesAll() = runTest {
        helper.registrarMovimiento("m1", "o1", "ENTRADA", 10, 0, "ref1", "Entrada inicial")
        helper.registrarMovimiento("m1", "o1", "SALIDA_VENTA", 1, 10, "ref2", "Venta")

        val movimientos = movimientoDao.getAllMovimientos()
        assertEquals(2, movimientos.size)
        // First call: stockPrevio=0, cantidad=10, stockNuevo=10
        assertEquals(10, movimientos[0].cantidad)
        assertEquals(0, movimientos[0].stockPrevio)
        assertEquals(10, movimientos[0].stockNuevo)
        // Second call: stockPrevio=10, cantidad=1, stockNuevo=11
        assertEquals(10, movimientos[1].stockPrevio)
        assertEquals(11, movimientos[1].stockNuevo)
    }

    @Test
    fun adjustStockAndRegistrarMovimiento_combinesBoth() = runTest {
        monturaDao.addMontura(Montura(id = "m1", opticaId = "o1", stockActual = 10))

        val result = helper.adjustStockAndRegistrarMovimiento(
            monturaId = "m1",
            opticaId = "o1",
            delta = -1,
            tipo = "SALIDA_VENTA",
            referenciaId = "disp-1",
            nota = "Venta"
        )

        assertTrue("result.isSuccess debería ser true", result.isSuccess)
        // Verificar stock se ajustó
        val m = monturaDao.getMonturaById("m1")
        assertNotNull("montura debe existir", m)
        assertEquals(9, m!!.stockActual)
        // Verificar que el DAO de movimientos recibió el insert
        val daoSize = movimientoDao.getAllMovimientos().size
        assertEquals("dao size after adjustAndRegistrar", 1, daoSize)
    }

    @Test
    fun adjustStockAndRegistrarMovimiento_insufficientStock_noMovimiento() = runTest {
        monturaDao.addMontura(Montura(id = "m1", opticaId = "o1", stockActual = 0))

        val result = helper.adjustStockAndRegistrarMovimiento(
            monturaId = "m1",
            opticaId = "o1",
            delta = -1,
            tipo = "SALIDA_VENTA",
            referenciaId = "disp-1",
            nota = "Venta"
        )

        assertTrue(result.isFailure)
        assertEquals(0, monturaDao.getMonturaById("m1")?.stockActual)
        assertEquals(0, movimientoDao.getAllMovimientos().size)
    }

    @Test
    fun adjustStockAndRegistrarMovimiento_positiveDelta_succeeds() = runTest {
        monturaDao.addMontura(Montura(id = "m1", opticaId = "o1", stockActual = 5))

        val result = helper.adjustStockAndRegistrarMovimiento(
            monturaId = "m1",
            opticaId = "o1",
            delta = 3,
            tipo = "AJUSTE",
            referenciaId = "ref-ajuste",
            nota = "Ajuste de inventario"
        )

        assertTrue(result.isSuccess)
        assertEquals(8, monturaDao.getMonturaById("m1")?.stockActual)
        assertEquals(1, movimientoDao.getAllMovimientos().size)
        assertEquals(5, movimientoDao.getAllMovimientos()[0].stockPrevio)
        assertEquals(8, movimientoDao.getAllMovimientos()[0].stockNuevo)
    }
}

internal class FakeMonturaDao : MonturaDao {
    private val monturas = mutableMapOf<String, Montura>()

    fun addMontura(montura: Montura) {
        monturas[montura.id] = montura
    }

    override suspend fun getMonturaById(id: String): Montura? = monturas[id]

    override suspend fun adjustStock(monturaId: String, opticaId: String, delta: Int): Int {
        val m = monturas[monturaId] ?: return 0
        if (m.opticaId != opticaId) return 0
        val newStock = m.stockActual + delta
        if (newStock < 0) return 0
        monturas[monturaId] = m.copy(stockActual = newStock)
        return 1
    }

    override fun getMonturasByOptica(opticaId: String) =
        kotlinx.coroutines.flow.flowOf(monturas.values.filter { it.opticaId == opticaId })

    override suspend fun insertMontura(montura: Montura) { monturas[montura.id] = montura }
    override suspend fun updateMontura(montura: Montura) { monturas[montura.id] = montura }
    override suspend fun deleteMontura(montura: Montura) { monturas.remove(montura.id) }
    override suspend fun getMonturasListByOptica(opticaId: String): List<Montura> =
        monturas.values.filter { it.opticaId == opticaId }

    override suspend fun searchMonturas(
        opticaId: String,
        marca: String?,
        material: String?,
        categoria: String?,
        precioMin: Double?,
        precioMax: Double?,
        stockBajo: Int
    ): List<Montura> = monturas.values.filter { m ->
        m.opticaId == opticaId && m.activo &&
            (marca == null || m.marca.contains(marca, ignoreCase = true)) &&
            (material == null || m.materialMontura.contains(material, ignoreCase = true)) &&
            (categoria == null || m.categoria == categoria) &&
            (precioMin == null || m.precio >= precioMin) &&
            (precioMax == null || m.precio <= precioMax) &&
            (stockBajo == 0 || m.stockActual <= m.stockMinimo)
    }
}

internal class FakeMonturaMovimientoDao : MonturaMovimientoDao {
    private val movimientos = mutableListOf<MonturaMovimiento>()

    fun getAllMovimientos(): List<MonturaMovimiento> = movimientos.toList()

    override suspend fun insertMovimiento(movimiento: MonturaMovimiento) {
        movimientos.add(movimiento)
    }

    override fun getMovimientosByOptica(opticaId: String) =
        kotlinx.coroutines.flow.flowOf(movimientos.filter { it.opticaId == opticaId })

    override fun getMovimientosByMontura(monturaId: String) =
        kotlinx.coroutines.flow.flowOf(movimientos.filter { it.monturaId == monturaId })

    override suspend fun getMovimientosListByOptica(opticaId: String): List<MonturaMovimiento> =
        movimientos.filter { it.opticaId == opticaId }
}
