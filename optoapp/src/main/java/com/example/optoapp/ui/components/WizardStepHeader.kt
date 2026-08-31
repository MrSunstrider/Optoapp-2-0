package com.example.optoapp.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.optoapp.ui.theme.LocalOptoDensity
import com.example.optoapp.ui.theme.OptoAppTheme

/** Title of the current wizard step, or "" when the index is out of range. */
fun wizardStepTitle(labels: List<String>, currentStep: Int): String =
    labels.getOrElse(currentStep) { "" }

/** Human-readable 1-based progress, clamped inside 1..totalSteps. */
fun wizardStepProgress(totalSteps: Int, currentStep: Int): String {
    val safeTotal = totalSteps.coerceAtLeast(1)
    val paso = (currentStep + 1).coerceIn(1, safeTotal)
    return "Paso $paso de $safeTotal"
}

/**
 * Cabecera de paso para wizards: título del paso + progreso legible.
 * Reemplaza al StepIndicator de círculos numerados; sin recuadro ni números redondos.
 */
@Composable
fun WizardStepHeader(
    labels: List<String>,
    currentStep: Int,
    totalSteps: Int,
    modifier: Modifier = Modifier,
) {
    val density = LocalOptoDensity.current
    val isDense = density.isDense
    val titleStyle = if (isDense) {
        MaterialTheme.typography.titleMedium
    } else {
        MaterialTheme.typography.headlineSmall
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = wizardStepTitle(labels, currentStep),
            style = titleStyle,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = wizardStepProgress(totalSteps, currentStep),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun WizardStepHeaderPreview() {
    OptoAppTheme {
        WizardStepHeader(
            labels = listOf("Anamnesis", "Examen Visual", "Refracción", "Contactología", "Cierre"),
            currentStep = 2,
            totalSteps = 5,
            modifier = Modifier.width(300.dp),
        )
    }
}
