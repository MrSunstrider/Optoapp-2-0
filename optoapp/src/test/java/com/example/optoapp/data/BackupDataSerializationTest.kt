package com.example.optoapp.data

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class BackupDataSerializationTest {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    // ─── Roundtrip via default serializer ───────────────────────────────

    @Test
    fun roundtrip_emptyBackupData_preservesAllFields() {
        val data = BackupData(
            version = 3,
            dateExported = 123456789L,
            appIdentifier = "OptoApp-2.0",
            sourceOpticaId = "opt_abc123",
            pacientes = emptyList(),
            evaluaciones = emptyList(),
            dispensaciones = emptyList(),
            pagos = emptyList(),
            serviciosExtra = emptyList()
        )

        val encoded = json.encodeToString(data)
        val decoded = json.decodeFromString<BackupData>(encoded)

        assertEquals(data.version, decoded.version)
        assertEquals(data.dateExported, decoded.dateExported)
        assertEquals(data.appIdentifier, decoded.appIdentifier)
        assertEquals(data.sourceOpticaId, decoded.sourceOpticaId)
        assertEquals(data.pacientes, decoded.pacientes)
        assertEquals(data.evaluaciones, decoded.evaluaciones)
        assertEquals(data.dispensaciones, decoded.dispensaciones)
        assertEquals(data.pagos, decoded.pagos)
        assertEquals(data.serviciosExtra, decoded.serviciosExtra)
    }

    @Test
    fun roundtrip_withPacienteData_serializesAndDeserializesCorrectly() {
        val paciente = Paciente(
            id = "p1",
            nombreCompleto = "Juan Perez",
            edad = 30,
            telefono = "123456789",
            fechaCreacion = LocalDate.of(2024, 1, 15),
            opticaId = "opt_abc123"
        )

        val data = BackupData(
            pacientes = listOf(paciente)
        )

        val encoded = json.encodeToString(data)
        val decoded = json.decodeFromString<BackupData>(encoded)

        val result = decoded.pacientes?.first()
        assertNotNull(result)
        assertEquals("p1", result?.id)
        assertEquals("Juan Perez", result?.nombreCompleto)
        assertEquals(30, result?.edad)
        assertEquals(LocalDate.of(2024, 1, 15), result?.fechaCreacion)
    }

    @Test
    fun roundtrip_withFullBackupData_preservesAllCollections() {
        val dispensacion = DispensacionOptica(
            id = "d1",
            pacienteId = "p1",
            fecha = LocalDate.of(2024, 6, 1),
            opticaId = "opt_abc"
        )
        val pago = Pago(
            id = "pay1",
            fecha = LocalDate.of(2024, 6, 1),
            tipo = "efectivo",
            monto = 150.0,
            opticaId = "opt_abc"
        )
        val servicio = ServicioExtra(
            id = "s1",
            descripcion = "Examen",
            montoTotal = 50.0,
            aCuenta = 50.0,
            estado = "Pagado",
            fecha = LocalDate.of(2024, 6, 1),
            opticaId = "opt_abc"
        )

        val data = BackupData(
            sourceOpticaId = "opt_abc",
            pacientes = listOf(
                Paciente("p1", "Ana Lopez", 25, "999888777", LocalDate.of(2024, 1, 15), opticaId = "opt_abc")
            ),
            evaluaciones = emptyList(),
            dispensaciones = listOf(dispensacion),
            pagos = listOf(pago),
            serviciosExtra = listOf(servicio)
        )

        val encoded = json.encodeToString(data)
        val decoded = json.decodeFromString<BackupData>(encoded)

        assertEquals(1, decoded.pacientes?.size)
        assertEquals(1, decoded.dispensaciones?.size)
        assertEquals(1, decoded.pagos?.size)
        assertEquals(1, decoded.serviciosExtra?.size)
        assertEquals("Ana Lopez", decoded.pacientes?.first()?.nombreCompleto)
        assertEquals("d1", decoded.dispensaciones?.first()?.id)
        assertEquals(150.0, decoded.pagos!!.first().monto, 0.001)
    }

    // ─── BackupDataSerializer: backward compat with old keys ────────────

    @Test
    fun backwardCompat_ordenesKey_mapsToDispensaciones() {
        val oldJson = """{"dispensaciones":[]}"""
        val data = json.decodeFromString(BackupDataSerializer, oldJson)
        assertNotNull(data.dispensaciones)
        assertTrue(data.dispensaciones!!.isEmpty())
    }

    @Test
    fun backwardCompat_ventasKey_mapsToDispensaciones() {
        val oldJson = """{"ventas":[]}"""
        val data = json.decodeFromString(BackupDataSerializer, oldJson)
        assertNotNull(data.dispensaciones)
        assertTrue(data.dispensaciones!!.isEmpty())
    }

    @Test
    fun backwardCompat_serviciosKey_mapsToServiciosExtra() {
        val oldJson = """{"servicios":[]}"""
        val data = json.decodeFromString(BackupDataSerializer, oldJson)
        assertNotNull(data.serviciosExtra)
        assertTrue(data.serviciosExtra!!.isEmpty())
    }

    @Test
    fun backwardCompat_otrosServiciosKey_mapsToServiciosExtra() {
        val oldJson = """{"otrosServicios":[]}"""
        val data = json.decodeFromString(BackupDataSerializer, oldJson)
        assertNotNull(data.serviciosExtra)
        assertTrue(data.serviciosExtra!!.isEmpty())
    }

    @Test
    fun backwardCompat_primaryKey_takesPrecedenceOverAlternates() {
        val oldJson = """{"dispensaciones":[{"id":"d1","pacienteId":"p1","fecha":"2024-01-15"}],"ordenes":[]}"""
        val data = json.decodeFromString(BackupDataSerializer, oldJson)
        assertEquals(1, data.dispensaciones?.size)
        assertEquals("d1", data.dispensaciones?.first()?.id)
    }
}
