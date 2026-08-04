package com.example.optoapp.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.optoapp.ui.theme.OptoTokens

@Composable
fun FormActions(
    onSave: () -> Unit,
    onCancel: (() -> Unit)? = null,
    saveEnabled: Boolean = true,
    saveLoading: Boolean = false,
    saveText: String = "Guardar",
    cancelText: String = "Cancelar",
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onCancel != null) {
            OptoButton(
                text = cancelText,
                onClick = onCancel,
                variant = OptoButtonVariant.Outlined,
                modifier = Modifier.weight(1f),
            )
            Spacer(modifier = Modifier.width(OptoTokens.spacing.sm))
        }
        OptoButton(
            text = saveText,
            onClick = onSave,
            variant = OptoButtonVariant.Filled,
            enabled = saveEnabled,
            loading = saveLoading,
            modifier = Modifier.weight(1f),
        )
    }
}
