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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.optoapp.R
import com.example.optoapp.data.EvaluacionClinica
import com.example.optoapp.util.DateUtils

@Composable
fun EvaluacionListItem(
    eval: EvaluacionClinica,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onResumen: () -> Unit,
) {
    val date = DateUtils.formatLocalized(eval.fecha)
    var showDeleteDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        onClick = onEdit,
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = date,
                    modifier = Modifier.weight(1f),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                IconButton(modifier = Modifier.size(40.dp), onClick = onResumen) {
                    Icon(
                        Icons.Default.Visibility,
                        contentDescription = stringResource(R.string.paciente_eval_ver_resumen_cd),
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(20.dp),
                    )
                }
                IconButton(modifier = Modifier.size(40.dp), onClick = { showDeleteDialog = true }) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = stringResource(R.string.paciente_eval_eliminar_cd),
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }

            val formulaStr = buildString {
                val odEsf = eval.recetaOdEsf.orEmpty()
                val odCil = eval.recetaOdCil.orEmpty()
                val odEje = eval.recetaOdEje.orEmpty()
                val oiEsf = eval.recetaOiEsf.orEmpty()
                val oiCil = eval.recetaOiCil.orEmpty()
                val oiEje = eval.recetaOiEje.orEmpty()
                val hasOd = odEsf.isNotBlank() || odCil.isNotBlank()
                val hasOi = oiEsf.isNotBlank() || oiCil.isNotBlank()
                if (hasOd) {
                    append("OD: $odEsf/$odCil x ${odEje}°")
                    if (eval.recetaOdAv.orEmpty().isNotBlank()) append(" (${eval.recetaOdAv})")
                }
                if (hasOd && hasOi) append(" · ")
                if (hasOi) {
                    append("OI: $oiEsf/$oiCil x ${oiEje}°")
                    if (eval.recetaOiAv.orEmpty().isNotBlank()) append(" (${eval.recetaOiAv})")
                }
            }
            val diagStr = buildString {
                val dOd = eval.diagnosticoOd?.firstOrNull().orEmpty()
                val dOi = eval.diagnosticoOi?.firstOrNull().orEmpty()
                if (dOd.isNotBlank()) append("OD: $dOd")
                if (dOd.isNotBlank() && dOi.isNotBlank()) append(" · ")
                if (dOi.isNotBlank()) append("OI: $dOi")
            }.trim()

            if (formulaStr.isNotBlank()) {
                Text(
                    text = stringResource(R.string.paciente_eval_formula, formulaStr),
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (diagStr.isNotBlank()) {
                    Text(
                        text = stringResource(R.string.paciente_eval_diag, diagStr),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else if (diagStr.isNotBlank()) {
                Text(
                    text = stringResource(R.string.paciente_eval_diagnostico, diagStr),
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            } else {
                Text(
                    text = stringResource(R.string.paciente_eval_sin_datos),
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.paciente_eval_eliminar_titulo)) },
            text = { Text(stringResource(R.string.paciente_eval_eliminar_mensaje)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) {
                    Text(stringResource(R.string.common_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.common_cancel))
                }
            },
        )
    }
}
