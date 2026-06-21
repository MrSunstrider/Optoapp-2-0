package com.example.optoapp.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Characterization tests for DTOs extracted from MembershipRepository.
 *
 * These verify that the DTO data classes remain constructable and hold values
 * correctly after extraction to MembershipRepositoryDtos.kt.
 */
class MembershipRepositoryDtosTest {

    // ── OpticaDto ─────────────────────────────────────────────────────────────

    @Test
    fun opticaDto_constructsWithMinimalParams() {
        val dto = OpticaDto(id = "opt_abc123")
        assertEquals("opt_abc123", dto.id)
        assertEquals("", dto.nombre)
        assertEquals("free", dto.plan)
        assertNull(dto.planCode)
        assertEquals("", dto.laboratorioNombre)
        assertEquals("", dto.laboratorioContacto)
        assertEquals("", dto.razonSocial)
    }

    @Test
    fun opticaDto_constructsWithAllParams() {
        val dto = OpticaDto(
            id = "opt_full", nombre = "Mi Óptica", plan = "premium",
            planCode = "premium-2024", laboratorioNombre = "Lab1",
            laboratorioContacto = "123", fiscalDocTipo = "RUC",
            fiscalDocNumero = "12345678", razonSocial = "RS",
            direccionFiscal = "Av. Siempre Viva"
        )
        assertEquals("Mi Óptica", dto.nombre)
        assertEquals("premium-2024", dto.planCode)
        assertEquals("Lab1", dto.laboratorioNombre)
        assertEquals("RUC", dto.fiscalDocTipo)
    }

    // ── OpticaMemberRow ───────────────────────────────────────────────────────

    @Test
    fun opticaMemberRow_constructsWithDefaults() {
        val row = OpticaMemberRow(opticaId = "o1", userId = "u1")
        assertEquals("o1", row.opticaId)
        assertEquals("u1", row.userId)
        assertEquals("", row.email)
        assertEquals("", row.rol)
        assertEquals("", row.createdAt)
    }

    @Test
    fun opticaMemberRow_constructsWithAllFields() {
        val row = OpticaMemberRow(
            opticaId = "o2", userId = "u2",
            email = "test@example.com", rol = "admin",
            createdAt = "2024-01-01"
        )
        assertEquals("test@example.com", row.email)
        assertEquals("admin", row.rol)
        assertEquals("2024-01-01", row.createdAt)
    }

    // ── PlanSettings ──────────────────────────────────────────────────────────

    @Test
    fun planSettings_freePlan() {
        val s = PlanSettings(
            planCode = "free", maxOpticas = 1,
            maxPacientesPorOptica = 20, maxUsuariosPorOptica = 2,
            planStatus = "active"
        )
        assertEquals("free", s.planCode)
        assertEquals(1, s.maxOpticas)
        assertEquals(20, s.maxPacientesPorOptica)
        assertEquals(2, s.maxUsuariosPorOptica)
        assertEquals("active", s.planStatus)
    }

    @Test
    fun planSettings_premiumPlan() {
        val s = PlanSettings(
            planCode = "premium", maxOpticas = 10,
            maxPacientesPorOptica = null, maxUsuariosPorOptica = null,
            planStatus = "active"
        )
        assertEquals("premium", s.planCode)
        assertNull(s.maxPacientesPorOptica)
        assertNull(s.maxUsuariosPorOptica)
    }

    // ── OpticaFiscalSettings ──────────────────────────────────────────────────

    @Test
    fun opticaFiscalSettings_constructsWithDefaults() {
        val s = OpticaFiscalSettings()
        assertEquals("", s.nombreComercial)
        assertEquals("", s.docTipo)
        assertEquals("", s.docNumero)
        assertEquals("", s.razonSocial)
    }

    @Test
    fun opticaFiscalSettings_constructsWithValues() {
        val s = OpticaFiscalSettings(
            nombreComercial = "Óptica Test", docTipo = "RUC",
            docNumero = "123", razonSocial = "RS Test",
            direccionFiscal = "Calle 123"
        )
        assertEquals("Óptica Test", s.nombreComercial)
        assertEquals("RUC", s.docTipo)
        assertEquals("123", s.docNumero)
    }

    // ── OpticaHeaderSummary ───────────────────────────────────────────────────

    @Test
    fun opticaHeaderSummary_holdsValues() {
        val s = OpticaHeaderSummary(
            nombreOptica = "Mi Óptica",
            fiscalEtiqueta = "RUC 12345678"
        )
        assertEquals("Mi Óptica", s.nombreOptica)
        assertEquals("RUC 12345678", s.fiscalEtiqueta)
    }

    // ── UsuarioOpticaDto (internal) ───────────────────────────────────────────

    @Test
    fun usuarioOpticaDto_constructs() {
        val dto = UsuarioOpticaDto(
            userId = "u1", opticaId = "o1", rol = "admin"
        )
        assertEquals("u1", dto.userId)
        assertEquals("o1", dto.opticaId)
        assertEquals("admin", dto.rol)
    }

    @Test
    fun usuarioOpticaDto_defaultRol() {
        val dto = UsuarioOpticaDto(userId = "u1", opticaId = "o1")
        assertEquals("admin", dto.rol)
    }

    // ── UserProfileRow (internal) ─────────────────────────────────────────────

    @Test
    fun userProfileRow_constructs() {
        val row = UserProfileRow(userId = "u1", email = "a@b.com")
        assertEquals("u1", row.userId)
        assertEquals("a@b.com", row.email)
    }

    // ── UsuarioOpticaUpsertDto (internal) ─────────────────────────────────────

    @Test
    fun usuarioOpticaUpsertDto_constructs() {
        val dto = UsuarioOpticaUpsertDto(
            userId = "u1", opticaId = "o1", rol = "admin"
        )
        assertEquals("u1", dto.userId)
        assertEquals("o1", dto.opticaId)
        assertEquals("admin", dto.rol)
    }

    // ── OpticaInsertDto (internal) ────────────────────────────────────────────

    @Test
    fun opticaInsertDto_freePlanDefaults() {
        val dto = OpticaInsertDto(id = "opt_new", nombre = "Nueva")
        assertEquals("opt_new", dto.id)
        assertEquals("Nueva", dto.nombre)
        assertEquals("free", dto.plan)
        assertEquals("", dto.fiscalDocTipo)
    }

    // ── OpticaLaboratorioPatch (internal) ─────────────────────────────────────

    @Test
    fun opticaLaboratorioPatch_constructs() {
        val patch = OpticaLaboratorioPatch(
            laboratorioNombre = "Lab X",
            laboratorioContacto = "contacto@labx.com"
        )
        assertEquals("Lab X", patch.laboratorioNombre)
        assertEquals("contacto@labx.com", patch.laboratorioContacto)
    }

    // ── OpticaFiscalPatch (internal) ──────────────────────────────────────────

    @Test
    fun opticaFiscalPatch_constructs() {
        val patch = OpticaFiscalPatch(
            nombre = "Óptica", fiscalDocTipo = "RUC",
            fiscalDocNumero = "123", razonSocial = "RS",
            direccionFiscal = "Calle 1"
        )
        assertEquals("Óptica", patch.nombre)
        assertEquals("RUC", patch.fiscalDocTipo)
        assertEquals("123", patch.fiscalDocNumero)
    }
}
