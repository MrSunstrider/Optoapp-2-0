package com.example.optoapp.domain

import android.util.Log
import com.example.optoapp.data.Resource
import com.example.optoapp.data.pago.PagoDao
import com.example.optoapp.data.venta.VentaDao
import io.github.jan.supabase.postgrest.Postgrest
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

class ObtenerDeudoresUseCaseTest {

    @Before
    fun setUpLog() {
        mockkStatic("android.util.Log")
        every { Log.d(any(), any()) } returns 0
        every { Log.w(any(), any<String>()) } returns 0
        every { Log.w(any(), any<String>(), any()) } returns 0
        every { Log.e(any(), any<String>()) } returns 0
        every { Log.e(any(), any<String>(), any()) } returns 0
    }

    private fun rpcDeudoresJson() = buildJsonArray {
        add(buildJsonObject {
            put("paciente_nombre", "Carlos Diaz")
            put("paciente_telefono", "555-0003")
            put("venta_id", "v3")
            put("venta_fecha", "2026-05-10")
            put("monto_total", 500.0)
            put("total_pagado", 0.0)
            put("saldo", 500.0)
            put("dias_deuda", 56)
        })
        add(buildJsonObject {
            put("paciente_nombre", "Juan Perez")
            put("paciente_telefono", "555-0001")
            put("venta_id", "v1")
            put("venta_fecha", "2026-06-01")
            put("monto_total", 1000.0)
            put("total_pagado", 400.0)
            put("saldo", 600.0)
            put("dias_deuda", 34)
        })
        add(buildJsonObject {
            put("paciente_nombre", "Maria Lopez")
            put("paciente_telefono", "555-0002")
            put("venta_id", "v2")
            put("venta_fecha", "2026-06-15")
            put("monto_total", 2000.0)
            put("total_pagado", 1500.0)
            put("saldo", 500.0)
            put("dias_deuda", 20)
        })
    }

    @Test
    fun online_rpcSuccess_returnsDeudoresList() = runBlocking {
        val useCase = object : ObtenerDeudoresUseCase(mockk<Postgrest>(), mockk(), mockk()) {
            override suspend fun callRpcDeudores(opticaId: String): JsonArray = rpcDeudoresJson()
        }

        val result = useCase("optica1")

        assertTrue(result is Resource.Success)
        val deudores = (result as Resource.Success).data!!
        assertEquals(3, deudores.size)
        // Server-sorted by dias_deuda DESC
        assertEquals("Carlos Diaz", deudores[0].pacienteNombre)
        assertEquals(56, deudores[0].diasDeuda)
        assertEquals(500.0, deudores[0].saldo, 0.001)
        assertEquals("Juan Perez", deudores[1].pacienteNombre)
        assertEquals(34, deudores[1].diasDeuda)
        assertEquals("Maria Lopez", deudores[2].pacienteNombre)
        assertEquals(20, deudores[2].diasDeuda)
        assertEquals("555-0002", deudores[2].pacienteTelefono)
    }

    @Test
    fun online_emptyResult_returnsEmptyList() = runBlocking {
        val useCase = object : ObtenerDeudoresUseCase(mockk<Postgrest>(), mockk(), mockk()) {
            override suspend fun callRpcDeudores(opticaId: String): JsonArray = buildJsonArray {}
        }

        val result = useCase("optica1")

        assertTrue(result is Resource.Success)
        val deudores = (result as Resource.Success).data!!
        assertTrue(deudores.isEmpty())
    }

    @Test
    fun offline_ioException_returnsError() = runBlocking {
        val ventaDao = mockk<VentaDao>()
        val pagoDao = mockk<PagoDao>()

        coEvery { ventaDao.getAllVentasByOptica("optica1") } throws IOException("DB error")

        val useCase = object : ObtenerDeudoresUseCase(mockk<Postgrest>(), ventaDao, pagoDao) {
            override suspend fun callRpcDeudores(opticaId: String): JsonArray {
                throw IOException("No network")
            }
        }

        val result = useCase("optica1")
        assertTrue(result is Resource.Error)
    }

    @Test
    fun unexpectedError_returnsResourceError() = runBlocking {
        val useCase = object : ObtenerDeudoresUseCase(mockk<Postgrest>(), mockk(), mockk()) {
            override suspend fun callRpcDeudores(opticaId: String): JsonArray {
                throw RuntimeException("Unexpected")
            }
        }

        val result = useCase("optica1")
        assertTrue(result is Resource.Error)
    }
}
