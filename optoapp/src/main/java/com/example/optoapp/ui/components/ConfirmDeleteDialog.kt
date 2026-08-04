package com.example.optoapp.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.example.optoapp.R

@Composable
fun ConfirmDeleteDialog(
    itemName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    deleting: Boolean = false,
    title: String = stringResource(R.string.delete_title),
) {
    OptoDialog(
        title = title,
        confirmText = if (deleting) stringResource(R.string.delete_deleting) else stringResource(R.string.common_delete),
        dismissText = stringResource(R.string.common_cancel),
        onConfirm = onConfirm,
        onDismissRequest = onDismiss,
    ) {
        Text(
            text = stringResource(R.string.delete_confirm_message, itemName),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Normal,
        )
    }
}
