package com.example.optoapp.ui.components.paciente

import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
    onSendMessage: (mensaje: String) -> Unit
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss
    ) {
        val nombre = paciente.nombreCompleto.split(" ").firstOrNull() ?: ""

        DropdownMenuItem(
            text = { Text("Mensaje Libre (Saludo)") },
            onClick = {
                onDismiss()
                onSendMessage("Hola $nombre,")
            }
        )

        DropdownMenuItem(
            text = { Text("Invitación Control Anual") },
            onClick = {
                onDismiss()
                val msg = "Hola $nombre, te saludamos de Óptica Sersa Visual y Preventiva. " +
                    "Nos preocupamos por tu salud visual; te recordamos que ya se cumplió un año " +
                    "desde tu última evaluación y te invitamos a visitarnos para tu control optométrico anual. " +
                    "¡Esperamos verte pronto!"
                onSendMessage(msg)
            }
        )

        val ultimaEval = evaluaciones.maxByOrNull { it.fecha }
        if (ultimaEval?.proximaCita != null) {
            val proxFecha = DateUtils.formatLocalized(ultimaEval.proximaCita)
            DropdownMenuItem(
                text = { Text("Recordar Próxima Cita ($proxFecha)") },
                onClick = {
                    onDismiss()
                    val msg = "Hola $nombre, te saludamos de Óptica Sersa Visual y Preventiva. " +
                        "Te recordamos cordialmente que tienes una cita de control optométrico " +
                        "programada con nosotros para el día $proxFecha. ¡Te esperamos!"
                    onSendMessage(msg)
                }
            )
        }
    }
}
