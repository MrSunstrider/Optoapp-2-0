package com.example.optoapp.ui.components.paciente

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.input.KeyboardType
import com.example.optoapp.testing.TestTags
import com.example.optoapp.util.DateUtils
import java.time.LocalDate

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
    var expandedSexo by remember { mutableStateOf(false) }
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
            value = edad,
            onValueChange = { if (it.all { char -> char.isDigit() }) onEdadChange(it) },
            label = { Text("Edad *") },
            modifier = Modifier.weight(1f).testTag(TestTags.PACIENTE_EDAD_FIELD),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )
        OutlinedTextField(
            value = telefono,
            onValueChange = onTelefonoChange,
            label = { Text("Teléfono *") },
            modifier = Modifier.weight(2f).testTag(TestTags.PACIENTE_TELEFONO_FIELD),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
        )
    }
    OutlinedTextField(
        value = dni,
        onValueChange = onDniChange,
        label = { Text("DNI / Cédula") },
        modifier = Modifier.fillMaxWidth()
    )
    OutlinedTextField(
        value = fechaNacimiento,
        onValueChange = onFechaNacimientoChange,
        label = { Text("Fecha de Nacimiento (dd/mm/aaaa)") },
        modifier = Modifier.fillMaxWidth()
    )

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val anchorWidth = maxWidth
        OutlinedTextField(
            value = sexo,
            onValueChange = {},
            readOnly = true,
            label = { Text("Sexo") },
            trailingIcon = {
                IconButton(onClick = { expandedSexo = !expandedSexo }) {
                    Icon(
                        if (expandedSexo) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                        contentDescription = null
                    )
                }
            },
            modifier = Modifier.fillMaxWidth()
        )
        DropdownMenu(
            expanded = expandedSexo,
            onDismissRequest = { expandedSexo = false },
            modifier = Modifier.width(anchorWidth)
        ) {
            sexos.forEach { selectionOption ->
                DropdownMenuItem(
                    text = { Text(selectionOption) },
                    onClick = {
                        onSexoChange(selectionOption)
                        expandedSexo = false
                    }
                )
            }
        }
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
