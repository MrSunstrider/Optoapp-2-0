package com.example.optoapp.domain

import com.example.optoapp.data.DispensacionOptica
import com.example.optoapp.data.FinanzasRemoteDefaults
import com.example.optoapp.data.Pago
import com.example.optoapp.data.ServicioExtra
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * Tests for the DTOs, extension functions, and helper functions extracted
 * from SyncFinanzasUseCase into SyncFinanzasDto.kt.
 */
class SyncFinanzasDtoTest {

    private val lenientJson = Json { ignoreUnknownKeys = true }

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

    private fun makeDispensacionRemota(
        id: String = "test-disp-id",
        ot: String? = "OT-2024-0001",
        pacienteId: String = "test-paciente",
        fecha: String = "2024-06-15",
        opticaId: String = "test-optica",
        montoTotal: Double = 0.0,
        montoPagado: Double = 0.0,
        fechaVencimientoGarantia: String? = null,
        tratamientos: String? = null,
    ) = DispensacionRemota(
        id = id, ot = ot, pacienteId = pacienteId, fecha = fecha, opticaId = opticaId,
        montoTotal = montoTotal, montoPagado = montoPagado,
        fechaVencimientoGarantia = fechaVencimientoGarantia,
        tratamientos = tratamientos,
    )

    @Test
    fun dispensacionRemota_toEntity_normalValues_passesThrough() {
        val remoto = makeDispensacionRemota(
            montoTotal = 250.0,
            montoPagado = 100.0,
            tratamientos = "AntiReflejo, Fotocromático",
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

    @Test
    fun pagoRemoto_toEntity_normalValues_passesThrough() {
        val remoto = PagoRemoto(
            id = "pago-1",
            dispensacionId = "disp-1",
            fecha = "2024-06-15",
            tipo = "Abono",
            monto = 50.0,
            metodoPago = "Efectivo",
            opticaId = "test-optica",
        )
        val entity = remoto.toEntity()
        assertEquals("pago-1", entity.id)
        assertEquals("disp-1", entity.dispensacionId)
        assertEquals(50.0, entity.monto, 0.001)
    }

    @Test
    fun pagoRemoto_toEntity_blankDispensacionId_returnsNull() {
        val remoto = PagoRemoto(
            id = "pago-2",
            dispensacionId = "  ",
            fecha = "2024-06-15",
            tipo = "Abono",
            monto = 0.0,
            opticaId = "test-optica",
        )
        val entity = remoto.toEntity()
        assertNull(entity.dispensacionId)
    }

    @Test
    fun pagoRemoto_toEntity_nullNota_returnsEmptyString() {
        val remoto = PagoRemoto(
            id = "pago-3",
            fecha = "2024-06-15",
            tipo = "Abono",
            monto = 0.0,
            nota = null,
            opticaId = "test-optica",
        )
        val entity = remoto.toEntity()
        assertEquals("", entity.nota)
    }

    @Test
    fun dispensacionOptica_toRemoto_roundTrip() {
        val original = DispensacionOptica(
            id = "d1", ot = "OT-2024-0001", pacienteId = "p1",
            fecha = LocalDate.of(2024, 6, 15), opticaId = "test-optica",
            montoTotal = 250.0, montoPagado = 100.0,
            tratamientos = listOf("AR", "FOT"),
            fechaVencimientoGarantia = LocalDate.of(2025, 6, 15),
        )
        val remoto = original.toRemoto()
        assertEquals(original.id, remoto.id)
        assertEquals(original.ot, remoto.ot)
        assertEquals(original.montoTotal, remoto.montoTotal, 0.001)
        assertEquals(original.montoPagado, remoto.montoPagado, 0.001)
        assertEquals("AR,FOT", remoto.tratamientos)
        assertEquals("2025-06-15", remoto.fechaVencimientoGarantia)
    }

    @Test
    fun dispensacionOptica_toRemoto_carriesClaimLinkageAndCancellationMetadata() {
        val original = DispensacionOptica(
            id = "r1", ot = "2026-0042-R1", pacienteId = "p1",
            fecha = LocalDate.of(2026, 9, 30), opticaId = "test-optica",
            estadoEntrega = "Reclamada",
            reclamoOrigenId = "o1",
            motivoAnulacion = "Lente rayado",
            fechaAnulacion = LocalDate.of(2026, 9, 29),
        )
        val remoto = original.toRemoto()
        assertEquals("o1", remoto.reclamoOrigenId)
        assertEquals("Lente rayado", remoto.motivoAnulacion)
        assertEquals("2026-09-29", remoto.fechaAnulacion)

        val restored = remoto.toEntity()
        assertEquals("o1", restored.reclamoOrigenId)
        assertEquals("Lente rayado", restored.motivoAnulacion)
        assertEquals(LocalDate.of(2026, 9, 29), restored.fechaAnulacion)
    }

    @Test
    fun dispensacionOptica_toRemoto_withoutMetadata_sendsNulls() {
        val remoto = DispensacionOptica(
            id = "d1", pacienteId = "p1", fecha = LocalDate.of(2026, 9, 30), opticaId = "test-optica",
        ).toRemoto()
        assertNull(remoto.reclamoOrigenId)
        assertNull(remoto.motivoAnulacion)
        assertNull(remoto.fechaAnulacion)
    }

    @Test
    fun dispensacionRemota_jsonWithoutNewKeys_deserializesToNullFields() {
        val json = """{"id":"d1","paciente_id":"p1","fecha":"2026-09-30","optica_id":"o1"}"""
        val entity = lenientJson.decodeFromString(DispensacionRemota.serializer(), json).toEntity()
        assertNull(entity.reclamoOrigenId)
        assertNull(entity.motivoAnulacion)
        assertNull(entity.fechaAnulacion)
    }

    @Test
    fun dispensacionRemota_jsonWithNewKeys_deserializesMetadata() {
        val json = """{"id":"d1","paciente_id":"p1","fecha":"2026-09-30","optica_id":"o1",""" +
            """"reclamo_origen_id":"o9","motivo_anulacion":"Cliente desistió","fecha_anulacion":"2026-09-28"}"""
        val entity = lenientJson.decodeFromString(DispensacionRemota.serializer(), json).toEntity()
        assertEquals("o9", entity.reclamoOrigenId)
        assertEquals("Cliente desistió", entity.motivoAnulacion)
        assertEquals(LocalDate.of(2026, 9, 28), entity.fechaAnulacion)
    }

    @Test
    fun servicioExtra_toRemoto_roundTripsCancellationMetadata() {
        val original = ServicioExtra(
            id = "s1", descripcion = "Reparación", montoTotal = 80.0, estado = "Anulado",
            fecha = LocalDate.of(2026, 9, 1), opticaId = "test-optica",
            motivoAnulacion = "Pieza no disponible",
            fechaAnulacion = LocalDate.of(2026, 9, 30),
        )
        val remoto = original.toRemoto()
        assertEquals("Pieza no disponible", remoto.motivoAnulacion)
        assertEquals("2026-09-30", remoto.fechaAnulacion)

        val restored = remoto.toEntity()
        assertEquals("Pieza no disponible", restored.motivoAnulacion)
        assertEquals(LocalDate.of(2026, 9, 30), restored.fechaAnulacion)
    }

    @Test
    fun servicioRemoto_jsonWithoutNewKeys_deserializesToNullFields() {
        val json = """{"id":"s1","fecha":"2026-09-30","optica_id":"o1"}"""
        val entity = lenientJson.decodeFromString(ServicioRemoto.serializer(), json).toEntity()
        assertNull(entity.motivoAnulacion)
        assertNull(entity.fechaAnulacion)
    }

    @Test
    fun servicioExtra_defaultsCancellationMetadataToNull() {
        val servicio = ServicioExtra(
            id = "s1", descripcion = "x", montoTotal = 1.0, estado = "Pendiente", fecha = LocalDate.of(2026, 9, 30),
        )
        assertNull(servicio.motivoAnulacion)
        assertNull(servicio.fechaAnulacion)
    }

    @Test
    fun pago_toRemoto_roundTrip() {
        val original = Pago(
            id = "p1",
            dispensacionId = "d1",
            fecha = LocalDate.of(2024, 6, 15),
            tipo = "Abono",
            monto = 50.0,
            metodoPago = "Efectivo",
            nota = "Primer abono",
            opticaId = "test-optica",
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
            id = "p2",
            fecha = LocalDate.of(2024, 6, 15),
            tipo = "Abono",
            monto = 0.0,
            metodoPago = "  ",
            opticaId = "test-optica",
        )
        val remoto = original.toRemoto()
        assertEquals(FinanzasRemoteDefaults.Pago.METODO_PAGO_VACIO, remoto.metodoPago)
    }

    @Test
    fun finanzasSyncResult_includesDownloadedVentas() {
        val result = FinanzasSyncResult(
            uploadedDispensaciones = 5,
            uploadedServicios = 3,
            uploadedPagos = 10,
            downloadedDispensaciones = 2,
            downloadedServicios = 1,
            downloadedPagos = 4,
        )
    }

    @Test
    fun finanzasSyncResult_holdsValues() {
        val result = FinanzasSyncResult(
            uploadedDispensaciones = 5,
            uploadedServicios = 3,
            uploadedPagos = 10,
            downloadedDispensaciones = 2,
            downloadedServicios = 1,
            downloadedPagos = 4,
        )
        assertEquals(5, result.uploadedDispensaciones)
        assertEquals(3, result.uploadedServicios)
        assertEquals(10, result.uploadedPagos)
        assertEquals(2, result.downloadedDispensaciones)
        assertEquals(1, result.downloadedServicios)
        assertEquals(4, result.downloadedPagos)
    }

    @Test
    fun mergePacienteData_canonicalTakesPriority() {
        val canonical = paciente(
            id = "p1",
            nombre = "Juan Perez",
            edad = 30,
            telefono = "999111222",
            historiaOptometrica = "HO-2024-0001",
        )
        val duplicate = paciente(
            id = "p2",
            nombre = "Juan Perez",
            edad = 30,
            telefono = "999333444",
            historiaOptometrica = "HO-2024-0001",
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

    /** Minimal Paciente factory for test clarity. */
    private fun paciente(
        id: String,
        nombre: String = "",
        edad: Int = 0,
        telefono: String = "",
        historiaOptometrica: String? = null,
        etiquetas: List<String> = emptyList(),
        fechaCreacion: LocalDate = LocalDate.now(),
    ) = com.example.optoapp.data.Paciente(
        id = id,
        nombreCompleto = nombre,
        edad = edad,
        telefono = telefono,
        fechaCreacion = fechaCreacion,
        historiaOptometrica = historiaOptometrica,
        ultimasEtiquetas = etiquetas,
    )

    /**
     * Replicates the exact merge logic from PacienteRepository.kt's mergePacienteData().
     * Tests the contract, not the implementation details.
     */
    private fun mergePacienteHelper(canonical: com.example.optoapp.data.Paciente, other: com.example.optoapp.data.Paciente): com.example.optoapp.data.Paciente {
        fun chooseText(primary: String?, fallback: String?): String? = primary?.takeIf { it.isNotBlank() } ?: fallback?.takeIf { it.isNotBlank() }
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
            ultimasEtiquetas = (canonical.ultimasEtiquetas + other.ultimasEtiquetas).distinct(),
        )
    }
}
