package com.example.optoapp.domain

import com.example.optoapp.data.FinanzasRemoteDefaults
import org.junit.Assert.assertEquals
import org.junit.Test

class SyncFinanzasUseCaseKtTest {

    private fun makeServicioRemoto(
        id: String = "test-servicio-id",
        ot: String = "",
        descripcion: String = "",
        montoTotal: Double = 0.0,
        aCuenta: Double = 0.0,
        estado: String = "",
        fecha: String = "2024-06-15",
        pacienteId: String? = null,
        metodoPago: String = "",
        opticaId: String = "test-optica"
    ) = ServicioRemoto(
        id = id,
        ot = ot,
        descripcion = descripcion,
        montoTotal = montoTotal,
        aCuenta = aCuenta,
        estado = estado,
        fecha = fecha,
        pacienteId = pacienteId,
        metodoPago = metodoPago,
        opticaId = opticaId
    )

    @Test
    fun toEntity_negativeMontoTotal_coercesToZero() {
        val remoto = makeServicioRemoto(montoTotal = -5.0)
        val entity = remoto.toEntity()
        assertEquals(0.0, entity.montoTotal, 0.001)
    }

    @Test
    fun toEntity_aCuentaExceedsMontoTotal_clampedToMontoTotal() {
        val remoto = makeServicioRemoto(montoTotal = 100.0, aCuenta = 150.0)
        val entity = remoto.toEntity()
        assertEquals(100.0, entity.aCuenta, 0.001)
    }

    @Test
    fun toEntity_negativeACuenta_coercesToZero() {
        val remoto = makeServicioRemoto(montoTotal = 50.0, aCuenta = -10.0)
        val entity = remoto.toEntity()
        assertEquals(0.0, entity.aCuenta, 0.001)
    }

    @Test
    fun toEntity_normalValues_passesThrough() {
        val remoto = makeServicioRemoto(montoTotal = 50.0, aCuenta = 25.0)
        val entity = remoto.toEntity()
        assertEquals(50.0, entity.montoTotal, 0.001)
        assertEquals(25.0, entity.aCuenta, 0.001)
    }

    @Test
    fun toEntity_blankOpticaId_usesFallback() {
        val remoto = makeServicioRemoto(opticaId = "  ")
        val entity = remoto.toEntity()
        assertEquals(FinanzasRemoteDefaults.OPTICA_ID_FALLBACK, entity.opticaId)
    }

    @Test
    fun toEntity_zeroValues_passesThrough() {
        val remoto = makeServicioRemoto(montoTotal = 0.0, aCuenta = 0.0)
        val entity = remoto.toEntity()
        assertEquals(0.0, entity.montoTotal, 0.001)
        assertEquals(0.0, entity.aCuenta, 0.001)
    }
}
