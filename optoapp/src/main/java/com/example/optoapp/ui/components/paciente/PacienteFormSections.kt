package com.example.optoapp.ui.components.paciente

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.OffsetMapping
import com.example.optoapp.testing.TestTags
import com.example.optoapp.util.DateUtils
import java.time.LocalDate

/** Shows dd/mm/yyyy formatting without changing the underlying digits-only value */
private object DateSlashTransformation : VisualTransformation {
    override fun filter(text: androidx.compose.ui.text.AnnotatedString): TransformedText {
        val digits = text.text
        val formatted = buildString {
            for (i in digits.indices) {
                if (i == 2 || i == 4) append('/')
                append(digits[i])
            }
        }
        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                return when {
                    offset <= 2 -> offset
                    offset <= 4 -> offset + 1
                    else -> offset + 2
                }
            }
            override fun transformedToOriginal(offset: Int): Int {
                return when {
                    offset <= 2 -> offset
                    offset <= 5 -> offset - 1
                    else -> offset - 2
                }
            }
        }
        return TransformedText(androidx.compose.ui.text.AnnotatedString(formatted), offsetMapping)
    }
}

/** Shows "XXX XXX XXX" formatting without changing the underlying digits-only value */
private object PhoneSpaceTransformation : VisualTransformation {
    override fun filter(text: androidx.compose.ui.text.AnnotatedString): TransformedText {
        val digits = text.text
        val formatted = buildString {
            for (i in digits.indices) {
                if (i == 3 || i == 6) append(' ')
                append(digits[i])
            }
        }
        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                return when {
                    offset <= 3 -> offset
                    offset <= 6 -> offset + 1
                    else -> offset + 2
                }
            }
            override fun transformedToOriginal(offset: Int): Int {
                return when {
                    offset <= 3 -> offset
                    offset <= 7 -> offset - 1
                    else -> offset - 2
                }
            }
        }
        return TransformedText(androidx.compose.ui.text.AnnotatedString(formatted), offsetMapping)
    }
}

@Composable
fun PacienteFormSections(
    nombreCompleto: String,
    onNombreCompletoChange: (String) -> Unit,
    edad: String,
    onEdadChange: (String) -> Unit,
    telefono: String,
    onTelefonoChange: (String) -> Unit,
    dni: String,
    onDniChange: (String) -> Unit,
    historiaOptometrica: String,
    onHistoriaOptometricaChange: (String) -> Unit,
    fechaNacimiento: String,
    onFechaNacimientoChange: (String) -> Unit,
    sexo: String,
    onSexoChange: (String) -> Unit,
    email: String,
    onEmailChange: (String) -> Unit,
    direccion: String,
    onDireccionChange: (String) -> Unit,
    distrito: String,
    onDistritoChange: (String) -> Unit,
    ocupacion: String,
    onOcupacionChange: (String) -> Unit,
    acompanante: String,
    onAcompananteChange: (String) -> Unit,
    hobbies: String,
    onHobbiesChange: (String) -> Unit,
    fechaCreacion: LocalDate,
    onShowDatePicker: () -> Unit,
    onSuggestHo: () -> Unit
) {
    var showSexoDialog by remember { mutableStateOf(false) }
    val sexos = listOf("Masculino", "Femenino")

    OutlinedButton(
        onClick = onShowDatePicker,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("Fecha de Registro: ${DateUtils.formatLocalized(fechaCreacion)}")
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = historiaOptometrica,
            onValueChange = onHistoriaOptometricaChange,
            label = { Text("N° Historia Optométrica") },
            modifier = Modifier.weight(1f)
        )
        TextButton(
            onClick = onSuggestHo,
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
        ) {
            Text("Sugerir HO", maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
        }
    }

    OutlinedTextField(
        value = nombreCompleto,
        onValueChange = onNombreCompletoChange,
        label = { Text("Nombre Completo *") },
        modifier = Modifier.fillMaxWidth().testTag(TestTags.PACIENTE_NOMBRE_FIELD)
    )
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = fechaNacimiento,
            onValueChange = { onFechaNacimientoChange(it.filter { c -> c.isDigit() }.take(8)) },
            label = { Text("Fecha Nac. (dd/mm/aaaa)") },
            modifier = Modifier.weight(2f).testTag(TestTags.PACIENTE_FECHA_NAC_FIELD),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            visualTransformation = DateSlashTransformation,
            singleLine = true
        )
        OutlinedTextField(
            value = edad,
            onValueChange = { if (it.all { char -> char.isDigit() }) onEdadChange(it) },
            label = { Text("Edad *") },
            modifier = Modifier.weight(1f).testTag(TestTags.PACIENTE_EDAD_FIELD),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )
    }
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = telefono,
            onValueChange = { onTelefonoChange(it.filter { c -> c.isDigit() }.take(9)) },
            label = { Text("Teléfono *") },
            modifier = Modifier.weight(1f).testTag(TestTags.PACIENTE_TELEFONO_FIELD),
            visualTransformation = PhoneSpaceTransformation,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )
        OutlinedTextField(
            value = dni,
            onValueChange = onDniChange,
            label = { Text("DNI / Cédula") },
            modifier = Modifier.weight(1f)
        )
    }

    OutlinedTextField(
        value = sexo,
        onValueChange = {},
        readOnly = true,
        label = { Text("Sexo") },
        trailingIcon = {
            IconButton(onClick = { showSexoDialog = true }) {
                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
            }
        },
        modifier = Modifier.fillMaxWidth()
    )

    if (showSexoDialog) {
        AlertDialog(
            onDismissRequest = { showSexoDialog = false },
            title = {
                Text("Sexo", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    sexos.forEach { opt ->
                        val isSelected = opt == sexo
                        Surface(
                            onClick = {
                                onSexoChange(opt)
                                showSexoDialog = false
                            },
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surface,
                            shape = MaterialTheme.shapes.small,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = null
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = opt,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSexoDialog = false }) { Text("Cancelar") }
            }
        )
    }

    OutlinedTextField(
        value = email,
        onValueChange = onEmailChange,
        label = { Text("Correo Electrónico") },
        modifier = Modifier.fillMaxWidth().testTag(TestTags.PACIENTE_EMAIL_FIELD),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
    )
    OutlinedTextField(
        value = direccion,
        onValueChange = onDireccionChange,
        label = { Text("Dirección") },
        modifier = Modifier.fillMaxWidth().testTag(TestTags.PACIENTE_DIRECCION_FIELD)
    )
    OutlinedTextField(
        value = distrito,
        onValueChange = onDistritoChange,
        label = { Text("Distrito") },
        modifier = Modifier.fillMaxWidth()
    )
    OutlinedTextField(
        value = ocupacion,
        onValueChange = onOcupacionChange,
        label = { Text("Ocupación") },
        modifier = Modifier.fillMaxWidth()
    )
    OutlinedTextField(
        value = acompanante,
        onValueChange = onAcompananteChange,
        label = { Text("Acompañante") },
        modifier = Modifier.fillMaxWidth()
    )
    OutlinedTextField(
        value = hobbies,
        onValueChange = onHobbiesChange,
        label = { Text("Hobbies / Hábitos") },
        modifier = Modifier.fillMaxWidth(),
        minLines = 2
    )
}
