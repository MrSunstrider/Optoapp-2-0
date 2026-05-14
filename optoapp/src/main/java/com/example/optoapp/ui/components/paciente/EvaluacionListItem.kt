package com.example.optoapp.ui.components.paciente

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.optoapp.data.EvaluacionClinica
import com.example.optoapp.util.DateUtils

@Composable
fun EvaluacionListItem(
    eval: EvaluacionClinica,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val date = DateUtils.formatLocalized(eval.fecha)
    var showDeleteDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Event, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = date, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
                Row {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "Editar", tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
            val formulaStr = buildString {
                val hasOd = eval.recetaOdEsf.isNotBlank() || eval.recetaOdCil.isNotBlank()
                val hasOi = eval.recetaOiEsf.isNotBlank() || eval.recetaOiCil.isNotBlank()
                if (hasOd) {
                    append("OD: ${eval.recetaOdEsf}/${eval.recetaOdCil}x${eval.recetaOdEje}°")
                    if (eval.recetaOdAv.isNotBlank()) append(" (${eval.recetaOdAv}) ") else append(" ")
                }
                if (hasOi) {
                    append("OI: ${eval.recetaOiEsf}/${eval.recetaOiCil}x${eval.recetaOiEje}°")
                    if (eval.recetaOiAv.isNotBlank()) append(" (${eval.recetaOiAv})")
                }
            }
            val diagStr = buildString {
                val dOd = eval.diagnosticoOd.firstOrNull() ?: ""
                val dOi = eval.diagnosticoOi.firstOrNull() ?: ""
                if (dOd.isNotBlank()) append("OD: $dOd ")
                if (dOi.isNotBlank()) append("OI: $dOi")
            }.trim()

            if (formulaStr.isNotBlank()) {
                Text(text = "Fórmula $formulaStr", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                if (diagStr.isNotBlank()) {
                    Text(text = "Diag: $diagStr", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else if (diagStr.isNotBlank()) {
                Text("Diagnóstico: $diagStr", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
            } else {
                Text("Sin fórmula ni diagnóstico", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("¿Eliminar evaluación?") },
            text = { Text("Esta acción no se puede deshacer.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}
