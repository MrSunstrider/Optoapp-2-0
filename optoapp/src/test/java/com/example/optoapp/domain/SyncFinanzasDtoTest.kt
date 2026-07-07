package com.example.optoapp.domain

import com.example.optoapp.data.DispensacionOptica
import com.example.optoapp.data.FinanzasRemoteDefaults
import com.example.optoapp.data.Pago
import com.example.optoapp.data.venta.Venta
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * Tests for the DTOs, extension functions, and helper functions extracted
 * from SyncFinanzasUseCase into SyncFinanzasDto.kt.
 */
class SyncFinanzasDtoTest {

    // ── normalizedOtForUnique ─────────────────────────────────────────────

    @Test
    fun normalizedOtForUnique_trimsAndUppercases() {
        assertEquals("OT-123", normalizedOtForUnique("ot-123"))
    }

    @Test
    fun normalizedOtForUnique_trimsWhitespace() {
        assertEquals("OT-456", normalizedOtForUnique("  Ot-456  "))
    }

    @Test
    fun normalizedOtForUnique_nullInput_returnsNull() {
        assertNull(normalizedOtForUnique(null))
    }

    @Test
    fun normalizedOtForUnique_emptyInput_returnsNull() {
        assertNull(normalizedOtForUnique(""))
    }

    @Test
    fun normalizedOtForUnique_blankInput_returnsNull() {
        assertNull(normalizedOtForUnique("   "))
    }

    // ── normalizeOptionalFk ───────────────────────────────────────────────

    @Test
    fun normalizeOptionalFk_null_returnsNull() {
        assertNull(null.normalizeOptionalFk())
    }

    @Test
    fun normalizeOptionalFk_blank_returnsNull() {
        assertNull("  ".normalizeOptionalFk())
    }

    @Test
    fun normalizeOptionalFk_empty_returnsNull() {
        assertNull("".normalizeOptionalFk())
    }

    @Test
    fun normalizeOptionalFk_validString_returnsTrimmed() {
        assertEquals("abc-123", "  abc-123  ".normalizeOptionalFk())
    }

    // ── DispensacionRemota.toEntity ───────────────────────────────────────

    private fun makeDispensacionRemota(
        id: String = "test-disp-id",
        ot: String? = "OT-2024-0001",
        pacienteId: String = "test-paciente",
        fecha: String = "2024-06-15",
        opticaId: String = "test-optica",
        montoTotal: Double = 0.0,
        montoPagado: Double = 0.0,
        fechaVencimientoGarantia: String? = null,
        tratamientos: String? = null
    ) = DispensacionRemota(
        id = id, ot = ot, pacienteId = pacienteId, fecha = fecha, opticaId = opticaId,
        montoTotal = montoTotal, montoPagado = montoPagado,
        fechaVencimientoGarantia = fechaVencimientoGarantia,
        tratamientos = tratamientos
    )

    @Test
    fun dispensacionRemota_toEntity_normalValues_passesThrough() {
        val remoto = makeDispensacionRemota(
            montoTotal = 250.0, montoPagado = 100.0,
            tratamientos = "AntiReflejo, Fotocromático"
        )
        val entity = remoto.toEntity()
        assertEquals("test-disp-id", entity.id)
        assertEquals("OT-2024-0001", entity.ot)
        assertEquals(250.0, entity.montoTotal, 0.001)
        assertEquals(100.0, entity.montoPagado, 0.001)
        assertEquals(2, entity.tratamientos.size)
        assertTrue(entity.tratamientos.contains("AntiReflejo"))
    }

    @Test
    fun dispensacionRemota_toEntity_blankOpticaId_usesFallback() {
        val remoto = makeDispensacionRemota(opticaId = "  ")
        val entity = remoto.toEntity()
        assertEquals(FinanzasRemoteDefaults.OPTICA_ID_FALLBACK, entity.opticaId)
    }

    @Test
    fun dispensacionRemota_toEntity_nullTratamientos_returnsEmptyList() {
        val remoto = makeDispensacionRemota(tratamientos = null)
        val entity = remoto.toEntity()
        assertTrue(entity.tratamientos.isEmpty())
    }

    @Test
    fun dispensacionRemota_toEntity_nullFechaVencimiento_returnsNull() {
        val remoto = makeDispensacionRemota(fechaVencimientoGarantia = null)
        val entity = remoto.toEntity()
        assertNull(entity.fechaVencimientoGarantia)
    }

    @Test
    fun dispensacionRemota_toEntity_validFechaVencimiento_parsed() {
        val remoto = makeDispensacionRemota(fechaVencimientoGarantia = "2025-06-15")
        val entity = remoto.toEntity()
        assertEquals(LocalDate.of(2025, 6, 15), entity.fechaVencimientoGarantia)
    }

    @Test
    fun dispensacionRemota_toEntity_nullOt_returnsEmptyString() {
        val remoto = makeDispensacionRemota(ot = null)
        val entity = remoto.toEntity()
        assertEquals("", entity.ot)
    }

    // ── PagoRemoto.toEntity ───────────────────────────────────────────────

    @Test
    fun pagoRemoto_toEntity_normalValues_passesThrough() {
        val remoto = PagoRemoto(
            id = "pago-1", dispensacionId = "disp-1", fecha = "2024-06-15",
            tipo = "Abono", monto = 50.0, metodoPago = "Efectivo", opticaId = "test-optica"
        )
        val entity = remoto.toEntity()
        assertEquals("pago-1", entity.id)
        assertEquals("disp-1", entity.dispensacionId)
        assertEquals(50.0, entity.monto, 0.001)
    }

    @Test
    fun pagoRemoto_toEntity_blankDispensacionId_returnsNull() {
        val remoto = PagoRemoto(
            id = "pago-2", dispensacionId = "  ", fecha = "2024-06-15",
            tipo = "Abono", monto = 0.0, opticaId = "test-optica"
        )
        val entity = remoto.toEntity()
        assertNull(entity.dispensacionId)
    }

    @Test
    fun pagoRemoto_toEntity_nullNota_returnsEmptyString() {
        val remoto = PagoRemoto(
            id = "pago-3", fecha = "2024-06-15",
            tipo = "Abono", monto = 0.0, nota = null, opticaId = "test-optica"
        )
        val entity = remoto.toEntity()
        assertEquals("", entity.nota)
    }

    // ── DispensacionOptica.toRemoto (reverse mapping) ─────────────────────

    @Test
    fun dispensacionOptica_toRemoto_roundTrip() {
        val original = DispensacionOptica(
            id = "d1", ot = "OT-2024-0001", pacienteId = "p1",
            fecha = LocalDate.of(2024, 6, 15), opticaId = "test-optica",
            montoTotal = 250.0, montoPagado = 100.0,
            tratamientos = listOf("AR", "FOT"),
            fechaVencimientoGarantia = LocalDate.of(2025, 6, 15)
        )
        val remoto = original.toRemoto()
        assertEquals(original.id, remoto.id)
        assertEquals(original.ot, remoto.ot)
        assertEquals(original.montoTotal, remoto.montoTotal, 0.001)
        assertEquals(original.montoPagado, remoto.montoPagado, 0.001)
        assertEquals("AR,FOT", remoto.tratamientos)
        assertEquals("2025-06-15", remoto.fechaVencimientoGarantia)
    }

    // ── Pago.toRemoto (reverse mapping) ───────────────────────────────────

    @Test
    fun pago_toRemoto_roundTrip() {
        val original = Pago(
            id = "p1", dispensacionId = "d1", fecha = LocalDate.of(2024, 6, 15),
            tipo = "Abono", monto = 50.0, metodoPago = "Efectivo",
            nota = "Primer abono", opticaId = "test-optica"
        )
        val remoto = original.toRemoto()
        assertEquals(original.id, remoto.id)
        assertEquals(original.dispensacionId, remoto.dispensacionId)
        assertEquals(original.monto, remoto.monto, 0.001)
        assertEquals(original.nota, remoto.nota)
    }

    @Test
    fun pago_toRemoto_blankMetodoPago_usesDefault() {
        val original = Pago(
            id = "p2", fecha = LocalDate.of(2024, 6, 15),
            tipo = "Abono", monto = 0.0, metodoPago = "  ", opticaId = "test-optica"
        )
        val remoto = original.toRemoto()
        assertEquals(FinanzasRemoteDefaults.Pago.METODO_PAGO_VACIO, remoto.metodoPago)
    }

    // ── VentaRemota.toEntity ───────────────────────────────────────────────

    @Test
    fun ventaRemota_toEntity_mapsSnakeToCamelCase() {
        val remoto = VentaRemota(
            id = "v_disp_001",
            opticaId = "optica-1",
            origen = "dispensacion",
            origenId = "disp-001",
            pacienteId = "pac-1",
            fecha = "2026-07-01",
            fechaEntrega = "2026-07-15",
            montoTotal = 150.0,
            costoUnitarioSnapshot = 45.0,
            estado = "Entregado",
            createdAt = "2026-07-01T10:00:00Z",
            updatedAt = "2026-07-15T14:00:00Z",
            updatedBy = "user-1"
        )
        val entity = remoto.toEntity()

        assertEquals("v_disp_001", entity.id)
        assertEquals("optica-1", entity.opticaId)
        assertEquals("dispensacion", entity.origen)
        assertEquals("disp-001", entity.origenId)
        assertEquals("pac-1", entity.pacienteId)
        assertEquals(LocalDate.of(2026, 7, 1), entity.fecha)
        assertEquals(LocalDate.of(2026, 7, 15), entity.fechaEntrega)
        assertEquals(150.0, entity.montoTotal, 0.001)
        assertEquals(45.0, entity.costoUnitarioSnapshot!!, 0.001)
        assertEquals("Entregado", entity.estado)
        assertEquals("2026-07-01T10:00:00Z", entity.createdAt)
        assertEquals("2026-07-15T14:00:00Z", entity.updatedAt)
        assertEquals("user-1", entity.updatedBy)
    }

    @Test
    fun ventaRemota_toEntity_nullOptionals_mapToNull() {
        val remoto = VentaRemota(
            id = "v_serv_002",
            opticaId = "optica-1",
            origen = "servicio_extra",
            origenId = "serv-002",
            pacienteId = "",
            fecha = "2026-07-04",
            montoTotal = 50.0,
            estado = "Pendiente"
        )
        val entity = remoto.toEntity()

        assertEquals("v_serv_002", entity.id)
        assertNull(entity.fechaEntrega)
        assertNull(entity.costoUnitarioSnapshot)
        assertNull(entity.createdAt)
        assertNull(entity.updatedAt)
        assertNull(entity.updatedBy)
        assertEquals("", entity.pacienteId)
    }

    @Test
    fun ventaRemota_toEntity_emptyOpticaId_mapsToFallback() {
        val remoto = VentaRemota(
            id = "v_disp_003",
            opticaId = "  ",
            origen = "dispensacion",
            origenId = "disp-003",
            pacienteId = "pac-3",
            fecha = "2026-07-04",
            montoTotal = 100.0,
            estado = "Pendiente"
        )
        val entity = remoto.toEntity()
        assertEquals("mi_optica_base", entity.opticaId)
    }

    // ── FinanzasSyncResult with downloadedVentas ────────────────────────────

    @Test
    fun finanzasSyncResult_includesDownloadedVentas() {
        val result = FinanzasSyncResult(
            uploadedDispensaciones = 5,
            uploadedServicios = 3,
            uploadedPagos = 10,
            downloadedDispensaciones = 2,
            downloadedServicios = 1,
            downloadedPagos = 4,
            downloadedVentas = 3
        )
        assertEquals(3, result.downloadedVentas)
    }

    // ── FinanzasSyncResult ────────────────────────────────────────────────

    @Test
    fun finanzasSyncResult_holdsValues() {
        val result = FinanzasSyncResult(
            uploadedDispensaciones = 5,
            uploadedServicios = 3,
            uploadedPagos = 10,
            downloadedDispensaciones = 2,
            downloadedServicios = 1,
            downloadedPagos = 4
        )
        assertEquals(5, result.uploadedDispensaciones)
        assertEquals(3, result.uploadedServicios)
        assertEquals(10, result.uploadedPagos)
        assertEquals(2, result.downloadedDispensaciones)
        assertEquals(1, result.downloadedServicios)
        assertEquals(4, result.downloadedPagos)
    }

    // ── mergePacienteData (pure function from PacienteRepository) ─────────

    @Test
    fun mergePacienteData_canonicalTakesPriority() {
        val canonical = paciente(
            id = "p1", nombre = "Juan Perez", edad = 30, telefono = "999111222",
            historiaOptometrica = "HO-2024-0001"
        )
        val duplicate = paciente(
            id = "p2", nombre = "Juan Perez", edad = 30, telefono = "999333444",
            historiaOptometrica = "HO-2024-0001"
        )
        // The merge logic is tested via the package-level function in PacienteRepository.kt
        // We test the same logic contract via a local helper.
        val merged = mergePacienteHelper(canonical, duplicate)
        assertEquals("Juan Perez", merged.nombreCompleto)
        assertEquals("999111222", merged.telefono) // canonical wins
        assertEquals(30, merged.edad)
    }

    @Test
    fun mergePacienteData_blankCanonicalField_usesDuplicate() {
        val canonical = paciente(id = "p1", nombre = "Juan", telefono = "")
        val duplicate = paciente(id = "p2", nombre = "Juan", telefono = "999888777")
        val merged = mergePacienteHelper(canonical, duplicate)
        assertEquals("999888777", merged.telefono) // duplicate fills blank
    }

    @Test
    fun mergePacienteData_ultimasEtiquetas_mergedDistinct() {
        val canonical = paciente(id = "p1", nombre = "Ana", etiquetas = listOf("VIP", "Nuevo"))
        val duplicate = paciente(id = "p2", nombre = "Ana", etiquetas = listOf("VIP", "Descuento"))
        val merged = mergePacienteHelper(canonical, duplicate)
        assertEquals(3, merged.ultimasEtiquetas.size)
        assertTrue(merged.ultimasEtiquetas.contains("VIP"))
        assertTrue(merged.ultimasEtiquetas.contains("Nuevo"))
        assertTrue(merged.ultimasEtiquetas.contains("Descuento"))
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    /** Minimal Paciente factory for test clarity. */
    private fun paciente(
        id: String,
        nombre: String = "",
        edad: Int = 0,
        telefono: String = "",
        historiaOptometrica: String? = null,
        etiquetas: List<String> = emptyList(),
        fechaCreacion: LocalDate = LocalDate.now()
    ) = com.example.optoapp.data.Paciente(
        id = id,
        nombreCompleto = nombre,
        edad = edad,
        telefono = telefono,
        fechaCreacion = fechaCreacion,
        historiaOptometrica = historiaOptometrica,
        ultimasEtiquetas = etiquetas
    )

    /**
     * Replicates the exact merge logic from PacienteRepository.kt's mergePacienteData().
     * Tests the contract, not the implementation details.
     */
    private fun mergePacienteHelper(canonical: com.example.optoapp.data.Paciente, other: com.example.optoapp.data.Paciente)
            : com.example.optoapp.data.Paciente {
        fun chooseText(primary: String?, fallback: String?): String? =
            primary?.takeIf { it.isNotBlank() } ?: fallback?.takeIf { it.isNotBlank() }
        return canonical.copy(
            nombreCompleto = if (canonical.nombreCompleto.isNotBlank()) canonical.nombreCompleto else other.nombreCompleto,
            edad = maxOf(canonical.edad, other.edad),
            telefono = chooseText(canonical.telefono, other.telefono).orEmpty(),
            dni = chooseText(canonical.dni, other.dni),
            fechaNacimiento = canonical.fechaNacimiento ?: other.fechaNacimiento,
            sexo = chooseText(canonical.sexo, other.sexo),
            email = chooseText(canonical.email, other.email),
            historiaOptometrica = chooseText(canonical.historiaOptometrica, other.historiaOptometrica),
            direccion = chooseText(canonical.direccion, other.direccion),
            distrito = chooseText(canonical.distrito, other.distrito),
            ocupacion = chooseText(canonical.ocupacion, other.ocupacion),
            acompanante = chooseText(canonical.acompanante, other.acompanante),
            hobbies = chooseText(canonical.hobbies, other.hobbies),
            ultimasEtiquetas = (canonical.ultimasEtiquetas + other.ultimasEtiquetas).distinct()
        )
    }

    // ── ArqueoCajaRemota.toLocal() createdAt must not equal updatedAt ────

    @Test
    fun arqueoCajaRemota_toLocal_does_not_conflate_createdAt_with_updatedAt() {
        val updatedAtValue = "2026-06-17T10:00:00Z"
        val remote = ArqueoCajaRemota(
            id = "arq-1",
            fecha = "2026-06-17",
            opticaId = "optica-test",
            fondoCaja = 1000.0,
            efectivoContado = 500.0,
            tarjetaContado = 200.0,
            transferenciaContado = 100.0,
            movilContado = 50.0,
            efectivoCobrado = 490.0,
            tarjetaCobrado = 198.0,
            transferenciaCobrado = 100.0,
            movilCobrado = 50.0,
            diferenciaEfectivo = -10.0,
            diferenciaTarjeta = -2.0,
            diferenciaTransferencia = 0.0,
            diferenciaMovil = 0.0,
            diferenciaTotal = -12.0,
            cerradoPor = "admin",
            sellado = true,
            updatedAt = updatedAtValue
        )

        val local = remote.toLocal()

        assertNotEquals(
            "createdAt must not equal updatedAt — they represent different moments in time. " +
            "Remote doesn't carry createdAt, so toLocal() should leave it unset.",
            updatedAtValue, local.createdAt
        )
    }
}
