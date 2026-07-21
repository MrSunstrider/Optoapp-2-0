package com.example.optoapp.domain

import com.example.optoapp.data.EvaluacionClinica
import com.example.optoapp.data.Paciente
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

/**
 * Characterization tests for SyncHistorialUseCase.
 *
 * Tests only what is testable in pure JUnit without mocking infrastructure.
 *
 * ## Limitations
 * - Full use case integration tests require Room in-memory database + Supabase fakes
 * - `uploadEvaluaciones()` and `downloadEvaluaciones()` are private and testable only via integration
 * - Without a mocking framework, Supabase-dependent tests (4.2.3, 4.2.4, 4.2.5) cannot be automated
 * - `buildUploadRows()` IS extracted as a pure internal function and is fully tested here
 *
 * ## Testable surface
 * - `normalizedHistoriaKey` — pure function, extracted to package level for direct testability
 * - `buildUploadRows` — pure function, FK-map logic extracted for direct testability
 */
class SyncHistorialUseCaseCharacterizationTest {

    // ── normalizedHistoriaKey ──────────────────────────────────────────────

    @Test
    fun `normalizedHistoriaKey trims and uppercases`() {
        assertEquals("ABC123", normalizedHistoriaKey(" abc123 "))
    }

    @Test
    fun `normalizedHistoriaKey returns null for blank input`() {
        assertNull(normalizedHistoriaKey(""))
        assertNull(normalizedHistoriaKey("  "))
    }

    @Test
    fun `normalizedHistoriaKey returns null for null input`() {
        assertNull(normalizedHistoriaKey(null))
    }

    @Test
    fun `normalizedHistoriaKey handles already normalized`() {
        assertEquals("TEST", normalizedHistoriaKey("TEST"))
    }

    // ── buildUploadRows — FK-map logic ──────────────────────────────────

    private val testOptica = "optica-1"
    private val today = LocalDate.of(2025, 6, 15)

    private fun makeEvaluacion(
        id: String = "ev-1",
        pacienteId: String = "p-1",
        fecha: LocalDate = today,
        opticaId: String = testOptica,
    ): EvaluacionClinica = EvaluacionClinica(
        id = id,
        pacienteId = pacienteId,
        fecha = fecha,
        opticaId = opticaId,
    )

    private fun makePaciente(
        id: String = "p-1",
        historiaOptometrica: String? = "H-001",
        nombreCompleto: String = "Paciente $id",
        opticaId: String = testOptica,
    ): Paciente = Paciente(
        id = id,
        nombreCompleto = nombreCompleto,
        edad = 30,
        telefono = "555-0100",
        fechaCreacion = LocalDate.of(2024, 1, 1),
        historiaOptometrica = historiaOptometrica,
        opticaId = opticaId,
    )

    private fun makePacienteRemoto(
        id: String = "rp-1",
        historiaOptometrica: String? = "H-001",
        nombreCompleto: String = "Remote $id",
        opticaId: String = testOptica,
    ): PacienteRemoto = PacienteRemoto(
        id = id,
        nombreCompleto = nombreCompleto,
        edad = 30,
        telefono = "555-0100",
        fechaCreacion = "2024-01-01",
        historiaOptometrica = historiaOptometrica,
        opticaId = opticaId,
    )

    // 4.2.1 — uploadEvaluaciones() con Supabase mocked → success
    // Tested via buildUploadRows: evaluaciones mapped correctly when FK matches

    @Test
    fun `buildUploadRows maps evaluaciones when pacientes match by historia`() {
        val ev = makeEvaluacion(pacienteId = "p-1")
        val localPaciente = makePaciente(id = "p-1", historiaOptometrica = "H-001")
        val remotePaciente = makePacienteRemoto(id = "rp-1", historiaOptometrica = "H-001")

        val rows = buildUploadRows(
            evaluaciones = listOf(ev),
            localPacientes = listOf(localPaciente),
            remotePacientes = listOf(remotePaciente),
            opticaId = testOptica,
        )

        assertEquals(1, rows.size)
        assertEquals("ev-1", rows[0].id)
        // pacienteId must be remapped from local p-1 to remote rp-1
        assertEquals("rp-1", rows[0].pacienteId)
        assertEquals(testOptica, rows[0].opticaId)
    }

    @Test
    fun `buildUploadRows preserves pacienteId when local and remote IDs match`() {
        val ev = makeEvaluacion(pacienteId = "p-1")
        val localPaciente = makePaciente(id = "p-1", historiaOptometrica = "H-001")
        val remotePaciente = makePacienteRemoto(id = "p-1", historiaOptometrica = "H-001")

        val rows = buildUploadRows(
            evaluaciones = listOf(ev),
            localPacientes = listOf(localPaciente),
            remotePacientes = listOf(remotePaciente),
            opticaId = testOptica,
        )

        assertEquals(1, rows.size)
        assertEquals("p-1", rows[0].pacienteId)
    }

    // 4.2.2 — uploadEvaluaciones() con conflictos → resolución correcta

    @Test
    fun `buildUploadRows skips evaluacion when remote paciente not found`() {
        val ev = makeEvaluacion(pacienteId = "p-no-match")
        val localPaciente = makePaciente(id = "p-no-match", historiaOptometrica = "NO-MATCH")
        val remotePaciente = makePacienteRemoto(id = "rp-1", historiaOptometrica = "H-001")

        val rows = buildUploadRows(
            evaluaciones = listOf(ev),
            localPacientes = listOf(localPaciente),
            remotePacientes = listOf(remotePaciente),
            opticaId = testOptica,
        )

        assertEquals(0, rows.size)
    }

    @Test
    fun `buildUploadRows skips evaluacion when local paciente historia is null`() {
        val ev = makeEvaluacion(pacienteId = "p-null")
        val localPaciente = makePaciente(id = "p-null", historiaOptometrica = null)
        val remotePaciente = makePacienteRemoto(id = "rp-1", historiaOptometrica = "H-001")

        val rows = buildUploadRows(
            evaluaciones = listOf(ev),
            localPacientes = listOf(localPaciente),
            remotePacientes = listOf(remotePaciente),
            opticaId = testOptica,
        )

        assertEquals(0, rows.size)
    }

    @Test
    fun `buildUploadRows handles multiple evaluaciones with mixed match results`() {
        val evMatch = makeEvaluacion(id = "ev-match", pacienteId = "p-match")
        val evSkip = makeEvaluacion(id = "ev-skip", pacienteId = "p-skip")
        val localMatch = makePaciente(id = "p-match", historiaOptometrica = "H-001")
        val localSkip = makePaciente(id = "p-skip", historiaOptometrica = "H-NONEXISTENT")
        val remotePaciente = makePacienteRemoto(id = "rp-1", historiaOptometrica = "H-001")

        val rows = buildUploadRows(
            evaluaciones = listOf(evMatch, evSkip),
            localPacientes = listOf(localMatch, localSkip),
            remotePacientes = listOf(remotePaciente),
            opticaId = testOptica,
        )

        assertEquals(1, rows.size)
        assertEquals("ev-match", rows[0].id)
    }

    @Test
    fun `buildUploadRows returns empty list when no evaluaciones`() {
        val rows = buildUploadRows(
            evaluaciones = emptyList(),
            localPacientes = emptyList(),
            remotePacientes = emptyList(),
            opticaId = testOptica,
        )
        assertEquals(0, rows.size)
    }

    @Test
    fun `buildUploadRows deduplicates rows by id via distinctBy`() {
        val ev1 = makeEvaluacion(id = "ev-dup", pacienteId = "p-1")
        val ev2 = makeEvaluacion(id = "ev-dup", pacienteId = "p-1")
        val localPaciente = makePaciente(id = "p-1", historiaOptometrica = "H-001")
        val remotePaciente = makePacienteRemoto(id = "p-1", historiaOptometrica = "H-001")

        val rows = buildUploadRows(
            evaluaciones = listOf(ev1, ev2),
            localPacientes = listOf(localPaciente),
            remotePacientes = listOf(remotePaciente),
            opticaId = testOptica,
        )

        assertEquals(1, rows.size)
    }

    // 4.2.3 — downloadEvaluaciones() con Supabase mocked → success
    // Cannot test in pure JUnit — requires Supabase fakes
    // See class-level KDoc for limitations

    // 4.2.4 — downloadEvaluaciones() con datos nuevos → insert correcto
    // Cannot test in pure JUnit — requires Room in-memory + Supabase fakes
    // See class-level KDoc for limitations

    // 4.2.5 — error de red → retry behavior correcto
    // Cannot test in pure JUnit — requires Supabase fakes to simulate network errors
    // See class-level KDoc for limitations
}
