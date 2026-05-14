package com.example.optoapp.ui.components.dispensacion

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.optoapp.ui.components.DropdownField
import com.example.optoapp.ui.components.OptoTextField
import com.example.optoapp.viewmodel.DispensacionUiState

@Composable
fun LenteForm(
    uiState: DispensacionUiState,
    onUpdate: (DispensacionUiState) -> Unit
) {
    Card {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Información del Lente", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            DropdownField(label = "Tipo de Lente", selected = uiState.tipoLente, options = listOf("Monofocal", "Bifocal", "Progresivo", "Ocupacional")) {
                val nextS = uiState.copy(tipoLente = it)
                var cleaned = if (it != "Bifocal") nextS.copy(subTipoBifocal = "") else nextS
                if (it != "Monofocal") cleaned = cleaned.copy(distanciaLente = "")
                if (it != "Bifocal" && it != "Progresivo" && it != "Ocupacional") cleaned = cleaned.copy(altura = "")
                onUpdate(cleaned)
            }

            if (uiState.tipoLente == "Bifocal") {
                DropdownField(label = "Sub-tipo Bifocal", selected = uiState.subTipoBifocal, options = listOf("Flaptop", "Invisible")) {
                    onUpdate(uiState.copy(subTipoBifocal = it))
                }
            }

            if (uiState.tipoLente == "Monofocal") {
                DropdownField(label = "Distancia", selected = uiState.distanciaLente, options = listOf("Lejos", "Intermedia", "Cerca")) {
                    onUpdate(uiState.copy(distanciaLente = it))
                }
            }

            if (uiState.tipoLente == "Bifocal" || uiState.tipoLente == "Progresivo" || uiState.tipoLente == "Ocupacional") {
                OptoTextField(
                    value = uiState.altura,
                    onValueChange = { onUpdate(uiState.copy(altura = it)) },
                    label = "Altura (mm)",
                    keyboardType = KeyboardType.Decimal
                )
            }

            DropdownField(label = "Material", selected = uiState.materialLente, options = listOf("Resina", "Policarbonato", "Cristal", "Trivex")) {
                onUpdate(uiState.copy(materialLente = it))
            }
            Text("Tratamientos", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            val opts = listOf("Ninguno", "Antireflejo", "Antirayas", "Filtro UV 400", "Fotocromático", "AR Blue Defense")

            val currentTrats = if (uiState.tratamientos.isEmpty()) listOf("Ninguno") else uiState.tratamientos + "Ninguno"

            currentTrats.distinct().forEachIndexed { index, selectedValue ->
                if (index == 0 || currentTrats[index - 1] != "Ninguno") {
                    DropdownField(
                        label = if (index == 0) "Tratamiento Principal" else "Tratamiento Adicional",
                        selected = selectedValue,
                        options = opts
                    ) { selected ->
                        val newList = uiState.tratamientos.toMutableList()
                        if (index < uiState.tratamientos.size) {
                            if (selected == "Ninguno") newList.removeAt(index)
                            else newList[index] = selected
                        } else if (selected != "Ninguno") {
                            newList.add(selected)
                        }
                        onUpdate(uiState.copy(tratamientos = newList.distinct().filter { t -> t != "Ninguno" }))
                    }
                }
            }
            OptoTextField(value = uiState.colorLente, onValueChange = { onUpdate(uiState.copy(colorLente = it)) }, label = "Color")
            OptoTextField(value = uiState.notasDiseno, onValueChange = { onUpdate(uiState.copy(notasDiseno = it)) }, label = "Notas de Diseño")
        }
    }
}
