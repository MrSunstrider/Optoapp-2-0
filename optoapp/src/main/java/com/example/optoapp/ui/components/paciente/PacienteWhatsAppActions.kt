package com.example.optoapp.ui.components.paciente

import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.example.optoapp.data.EvaluacionClinica
import com.example.optoapp.data.Paciente
import com.example.optoapp.util.DateUtils

@Composable
fun PacienteWhatsAppMenu(
    expanded: Boolean,
    paciente: Paciente,
    evaluaciones: List<EvaluacionClinica>,
    onDismiss: () -> Unit,
    onSendMessage: (mensaje: String) -> Unit,
    nombreOptica: String = "",
    horarioAtencion: String = "",
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
    ) {
        val nombre = paciente.nombreCompleto.split(" ").firstOrNull() ?: ""
        val optica = nombreOptica.ifBlank { "Su óptica" }

        DropdownMenuItem(
            text = { Text("Mensaje Libre") },
            onClick = {
                onDismiss()
                onSendMessage("Hola $nombre,")
            },
        )

        DropdownMenuItem(
            text = { Text("Invitación Control Anual") },
            onClick = {
                onDismiss()
                val msg = "Hola $nombre, te saludamos de $optica. " +
                    "Nos preocupamos por tu salud visual; te recordamos que ya se cumplió un año " +
                    "desde tu última evaluación y te invitamos a visitarnos para tu control optométrico anual. " +
                    "¡Esperamos verte pronto!"
                onSendMessage(msg)
            },
        )

        val ultimaEval = evaluaciones.maxByOrNull { it.fecha }
        if (ultimaEval?.proximaCita != null) {
            val proxFecha = DateUtils.formatLocalized(ultimaEval.proximaCita)
            DropdownMenuItem(
                text = { Text("Recordar Próxima Cita ($proxFecha)") },
                onClick = {
                    onDismiss()
                    val msg = "Hola $nombre, te saludamos de $optica. " +
                        "Te recordamos cordialmente que tienes una cita de control optométrico " +
                        "programada con nosotros para el día $proxFecha. ¡Te esperamos!"
                    onSendMessage(msg)
                },
            )
        }

        HorizontalDivider()

        DropdownMenuItem(
            text = { Text("Pendiente de Recojo") },
            onClick = {
                onDismiss()
                val msg = "Hola $nombre, le recordamos que tiene pendiente el recojo de sus lentes. " +
                    "Lo esperamos en nuestra óptica en nuestro horario de atención."
                onSendMessage(msg)
            },
        )

        DropdownMenuItem(
            text = { Text("Entrega de Lentes") },
            onClick = {
                onDismiss()
                val titulo = when (paciente.sexo?.lowercase()) {
                    "masculino", "m" -> "Sr."
                    "femenino", "f" -> "Sra."
                    else -> "Sr./Sra."
                }
                val apellido = paciente.nombreCompleto.split(" ").let { partes ->
                    if (partes.size >= 2) partes[1] else nombre
                }
                val msg = buildString {
                    append("Buen día. $titulo $apellido, sus lentes ya están listos")
                    if (horarioAtencion.isNotBlank()) {
                        append(", puede venir a recogerlos en este horario: $horarioAtencion.")
                    }
                    append(" Lo esperamos.")
                }
                onSendMessage(msg)
            },
        )
    }
}
