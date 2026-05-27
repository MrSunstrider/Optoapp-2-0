package com.example.optoapp.factories

import com.example.optoapp.data.DispensacionItem
import com.example.optoapp.data.DispensacionOptica
import com.example.optoapp.data.EvaluacionClinica
import com.example.optoapp.data.Paciente
import com.example.optoapp.data.Pago
import java.time.LocalDate
import java.util.UUID

/**
 * Inline factory functions for creating test entities with stable defaults.
 *
 * Each function generates unique IDs (via [generateId]) so callers can create
 * multiple entities without primary-key collisions. Override any field by
 * passing it explicitly — patterns match production entity signatures.
 */
object TestDataFactory {

    private fun generateId(): String = "test-${UUID.randomUUID().toString().take(8)}"

    private fun timestampSuffix(): String = System.currentTimeMillis().toString().takeLast(4)

    // ── User / Auth ──────────────────────────────────────────────────────────

    /**
     * Test credentials for login flows.
     * Each call generates a unique email to avoid auth collisions.
     */
    data class TestCredentials(
        val email: String,
        val password: String
    )

    fun createTestUser(): TestCredentials = TestCredentials(
        email = "test-user-${timestampSuffix()}@optoapp-test.com",
        password = "TestPass123!"
    )

    // ── Paciente ─────────────────────────────────────────────────────────────

    fun createTestPaciente(
        id: String = generateId(),
        nombreCompleto: String = "Paciente Test ${id.take(6)}",
        edad: Int = 30,
        telefono: String = "999${timestampSuffix()}",
        fechaCreacion: LocalDate = LocalDate.now(),
        opticaId: String = "test-optica",
        dni: String? = null,
        email: String? = null,
        direccion: String? = null,
        historiaOptometrica: String? = null
    ): Paciente = Paciente(
        id = id,
        nombreCompleto = nombreCompleto,
        edad = edad,
        telefono = telefono,
        fechaCreacion = fechaCreacion,
        opticaId = opticaId,
        dni = dni,
        email = email,
        direccion = direccion,
        historiaOptometrica = historiaOptometrica
    )

    // ── Evaluación ───────────────────────────────────────────────────────────

    fun createTestEvaluacion(
        id: String = generateId(),
        pacienteId: String,
        fecha: LocalDate = LocalDate.now(),
        opticaId: String = "test-optica",
        motivoConsulta: String = "Control general",
        diagnostico: String = "",
        citaEstado: String = "completada"
    ): EvaluacionClinica = EvaluacionClinica(
        id = id,
        pacienteId = pacienteId,
        fecha = fecha,
        opticaId = opticaId,
        motivoConsulta = motivoConsulta,
        diagnostico = diagnostico,
        citaEstado = citaEstado
    )

    // ── Dispensación ─────────────────────────────────────────────────────────

    fun createTestDispensacion(
        id: String = generateId(),
        pacienteId: String,
        fecha: LocalDate = LocalDate.now(),
        opticaId: String = "test-optica",
        montoTotal: Double = 150.0,
        montoPagado: Double = 100.0,
        estadoEntrega: String = "Pendiente",
        ot: String = "OT-TEST-${timestampSuffix()}",
        tratamientos: List<String> = listOf("Antireflex", "Endurecido")
    ): DispensacionOptica = DispensacionOptica(
        id = id,
        pacienteId = pacienteId,
        fecha = fecha,
        opticaId = opticaId,
        montoTotal = montoTotal,
        montoPagado = montoPagado,
        estadoEntrega = estadoEntrega,
        ot = ot,
        tratamientos = tratamientos
    )

    fun createTestDispensacionItem(
        id: String = generateId(),
        dispensacionId: String,
        tipoLente: String = "Lejos",
        materialLente: String = "Orgánico",
        opticaId: String = "test-optica"
    ): DispensacionItem = DispensacionItem(
        id = id,
        dispensacionId = dispensacionId,
        tipoLente = tipoLente,
        materialLente = materialLente,
        opticaId = opticaId
    )

    // ── Pago ─────────────────────────────────────────────────────────────────

    fun createTestPago(
        id: String = generateId(),
        dispensacionId: String,
        fecha: LocalDate = LocalDate.now(),
        tipo: String = "Contado",
        monto: Double = 100.0,
        metodoPago: String = "Efectivo",
        opticaId: String = "test-optica"
    ): Pago = Pago(
        id = id,
        dispensacionId = dispensacionId,
        fecha = fecha,
        tipo = tipo,
        monto = monto,
        metodoPago = metodoPago,
        opticaId = opticaId
    )
}
