package com.example.optoapp.ui.components.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.optoapp.R
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.example.optoapp.ui.components.OptoButton
import com.example.optoapp.ui.components.OptoButtonVariant
import com.example.optoapp.ui.theme.OptoTokens
import com.example.optoapp.ui.theme.OptoAppTheme

@Composable
fun ErrorState(
    message: String,
    onRetry: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(OptoTokens.spacing.xxl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Default.ErrorOutline,
            contentDescription = "Error",
            modifier = Modifier
                .size(48.dp)
                .semantics { testTag = "error_state_icon" },
            tint = MaterialTheme.colorScheme.error,
        )
        Spacer(modifier = Modifier.height(OptoTokens.spacing.md))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        if (onRetry != null) {
            Spacer(modifier = Modifier.height(OptoTokens.spacing.lg))
            OptoButton(
                text = stringResource(R.string.common_retry),
                onClick = onRetry,
                variant = OptoButtonVariant.Outlined,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ErrorStatePreview() {
    OptoAppTheme { ErrorState(message = "Error de conexión") }
}

@Preview(showBackground = true)
@Composable
private fun ErrorStateWithRetryPreview() {
    OptoAppTheme { ErrorState(message = "Error de conexión", onRetry = {}) }
}
