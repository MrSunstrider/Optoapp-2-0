package com.example.optoapp.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.optoapp.R

const val MOTIVO_MAX_LENGTH = 500

fun limitMotivo(input: String): String = input.take(MOTIVO_MAX_LENGTH)

fun canConfirmMotivo(motivo: String, submitting: Boolean): Boolean = motivo.isNotBlank() && !submitting

fun motivoCounter(motivo: String): String = "${motivo.length}/$MOTIVO_MAX_LENGTH"

/** OptoDialog has no disabled confirm state, so an ineligible confirm tap is ignored instead. */
@Composable
fun MotivoDialog(
    title: String,
    confirmText: String,
    onConfirm: (motivo: String) -> Unit,
    onDismiss: () -> Unit,
    message: String? = null,
    submitting: Boolean = false,
    extraContent: @Composable () -> Unit = {},
) {
    var motivo by rememberSaveable { mutableStateOf("") }
    OptoDialog(
        title = title,
        confirmText = if (submitting) "Procesando..." else confirmText,
        dismissText = stringResource(R.string.common_cancel),
        onConfirm = { if (canConfirmMotivo(motivo, submitting)) onConfirm(motivo.trim()) },
        onDismissRequest = { if (!submitting) onDismiss() },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (message != null) {
                Text(message, style = MaterialTheme.typography.bodyMedium)
            }
            OutlinedTextField(
                value = motivo,
                onValueChange = { motivo = limitMotivo(it) },
                label = { Text("Motivo *") },
                supportingText = { Text(motivoCounter(motivo)) },
                isError = motivo.isBlank(),
                enabled = !submitting,
                minLines = 2,
                modifier = Modifier.fillMaxWidth(),
            )
            extraContent()
        }
    }
}
