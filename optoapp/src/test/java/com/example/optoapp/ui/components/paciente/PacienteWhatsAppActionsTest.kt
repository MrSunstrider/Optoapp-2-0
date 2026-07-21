package com.example.optoapp.ui.components.paciente

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for WhatsApp message generation logic used by PacienteWhatsAppMenu.
 *
 * Verifies that generated messages use tenant-specific optica name and business hours
 * instead of hardcoded strings. Full Compose UI rendering tests require androidTest
 * infrastructure.
 */
class PacienteWhatsAppActionsTest {

    @Test
    fun `invitacionControlAnual message uses nombreOptica`() {
        val nombre = "Vision Center SAS"
        val msg = buildInvitacionControlAnual("Juan", nombre)
        assertTrue("Message should contain optica name", msg.contains(nombre))
        assertFalse("Message should not contain hardcoded name", msg.contains("Óptica Sersa Visual y Preventiva"))
    }

    @Test
    fun `invitacionControlAnual message falls back to Su optica when nombreOptica blank`() {
        val msg = buildInvitacionControlAnual("Juan", "")
        assertTrue("Message should use fallback 'Su óptica'", msg.contains("Su óptica"))
    }

    @Test
    fun `recordarProximaCita message uses nombreOptica`() {
        val nombre = "Óptica del Valle"
        val msg = buildRecordarProximaCita("María", nombre, "15/08/2026")
        assertTrue("Message should contain optica name", msg.contains(nombre))
        assertFalse("Message should not contain hardcoded name", msg.contains("Óptica Sersa Visual y Preventiva"))
    }

    @Test
    fun `recordarProximaCita message falls back to Su optica when nombreOptica blank`() {
        val msg = buildRecordarProximaCita("María", "", "15/08/2026")
        assertTrue("Message should use fallback", msg.contains("Su óptica"))
    }

    @Test
    fun `entregaDeLentes message uses horarioAtencion`() {
        val hours = "Lunes a Viernes de 9am a 7pm"
        val msg = buildEntregaDeLentes("Sr.", "García", "Carlos", hours)
        assertTrue("Message should contain business hours", msg.contains(hours))
        assertFalse("Message should not contain hardcoded hours", msg.contains("Martes a Sábado"))
        assertFalse("Message should not contain hardcoded Sunday hours", msg.contains("Domingos"))
    }

    @Test
    fun `entregaDeLentes message omits hours sentence when horarioAtencion blank`() {
        val msg = buildEntregaDeLentes("Sra.", "López", "Ana", "")
        assertTrue("Message should still have delivery notification", msg.contains("sus lentes ya están listos"))
        assertFalse("Message should not include hours sentence", msg.contains("puede venir a recogerlos en este horario"))
    }

    @Test
    fun `mensajeLibre template unchanged`() {
        val msg = buildMensajeLibre("Pedro")
        assertEquals("Hola Pedro,", msg)
    }

    @Test
    fun `pendienteDeRecojo template unchanged`() {
        val msg = buildPendienteDeRecojo("Carlos")
        assertTrue(msg.contains("recojo de sus lentes"))
        assertTrue(msg.contains("horario de atención"))
    }

    private fun buildMensajeLibre(nombre: String): String = "Hola $nombre,"

    private fun buildInvitacionControlAnual(nombre: String, nombreOptica: String): String {
        val optica = nombreOptica.ifBlank { "Su óptica" }
        return "Hola $nombre, te saludamos de $optica. " +
            "Nos preocupamos por tu salud visual; te recordamos que ya se cumplió un año " +
            "desde tu última evaluación y te invitamos a visitarnos para tu control optométrico anual. " +
            "¡Esperamos verte pronto!"
    }

    private fun buildRecordarProximaCita(nombre: String, nombreOptica: String, proxFecha: String): String {
        val optica = nombreOptica.ifBlank { "Su óptica" }
        return "Hola $nombre, te saludamos de $optica. " +
            "Te recordamos cordialmente que tienes una cita de control optométrico " +
            "programada con nosotros para el día $proxFecha. ¡Te esperamos!"
    }

    private fun buildEntregaDeLentes(titulo: String, apellido: String, nombre: String, horarioAtencion: String): String {
        return buildString {
            append("Buen día. $titulo $apellido, sus lentes ya están listos")
            if (horarioAtencion.isNotBlank()) {
                append(", puede venir a recogerlos en este horario: $horarioAtencion.")
            }
            append(" Lo esperamos.")
        }
    }

    private fun buildPendienteDeRecojo(nombre: String): String {
        return "Hola $nombre, le recordamos que tiene pendiente el recojo de sus lentes. " +
            "Lo esperamos en nuestra óptica en nuestro horario de atención."
    }
}
