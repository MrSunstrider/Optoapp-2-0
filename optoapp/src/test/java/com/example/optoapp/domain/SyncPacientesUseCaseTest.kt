package com.example.optoapp.domain

import com.example.optoapp.data.Paciente
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class SyncPacientesUseCaseTest {

    @Test
    fun `toEntity decodes JSON array string`() {
        val remoto = PacienteRemoto(
            id = "p1",
            nombreCompleto = "Test",
            edad = 30,
            telefono = "111",
            fechaCreacion = "2026-01-15",
            ultimasEtiquetas = """["lentes","examen","graduacion"]""",
        )
        val entity = remoto.toEntity()
        assertEquals(listOf("lentes", "examen", "graduacion"), entity.ultimasEtiquetas)
    }

    @Test
    fun `toEntity falls back to CSV split when JSON parsing fails`() {
        val remoto = PacienteRemoto(
            id = "p2",
            nombreCompleto = "CSV Patient",
            edad = 25,
            telefono = "222",
            fechaCreacion = "2026-02-01",
            ultimasEtiquetas = "tag1,tag2,tag3",
        )
        val entity = remoto.toEntity()
        assertEquals(listOf("tag1", "tag2", "tag3"), entity.ultimasEtiquetas)
    }

    @Test
    fun `toEntity handles null ultimasEtiquetas as empty list`() {
        val remoto = PacienteRemoto(
            id = "p3",
            nombreCompleto = "Null Tags",
            edad = 35,
            telefono = "333",
            fechaCreacion = "2026-03-01",
            ultimasEtiquetas = null,
        )
        val entity = remoto.toEntity()
        assertEquals(emptyList<String>(), entity.ultimasEtiquetas)
    }

    @Test
    fun `toEntity handles single tag CSV`() {
        val remoto = PacienteRemoto(
            id = "p4",
            nombreCompleto = "Single",
            edad = 40,
            telefono = "444",
            fechaCreacion = "2026-04-01",
            ultimasEtiquetas = "urgente",
        )
        // "urgente" is not valid JSON, so CSV fallback kicks in
        val entity = remoto.toEntity()
        assertEquals(listOf("urgente"), entity.ultimasEtiquetas)
    }

    @Test
    fun `toEntity handles empty string CSV gracefully`() {
        val remoto = PacienteRemoto(
            id = "p5",
            nombreCompleto = "Empty",
            edad = 20,
            telefono = "555",
            fechaCreacion = "2026-05-01",
            ultimasEtiquetas = "",
        )
        val entity = remoto.toEntity()
        assertEquals(emptyList<String>(), entity.ultimasEtiquetas)
    }

    @Test
    fun `toRemoto produces JSON array string from tags`() {
        val paciente = Paciente(
            id = "p6",
            nombreCompleto = "JSON Out",
            edad = 28,
            telefono = "666",
            fechaCreacion = LocalDate.parse("2026-06-01"),
            ultimasEtiquetas = listOf("tag-a", "tag-b"),
        )
        val remoto = paciente.toRemoto()
        assertEquals("""["tag-a","tag-b"]""", remoto.ultimasEtiquetas)
    }

    @Test
    fun `toRemoto produces empty JSON array from empty tags`() {
        val paciente = Paciente(
            id = "p7",
            nombreCompleto = "No Tags",
            edad = 22,
            telefono = "777",
            fechaCreacion = LocalDate.parse("2026-07-01"),
            ultimasEtiquetas = emptyList(),
        )
        val remoto = paciente.toRemoto()
        assertEquals("""[]""", remoto.ultimasEtiquetas)
    }
}
