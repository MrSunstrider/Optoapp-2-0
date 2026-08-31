package com.example.optoapp.ui.components.dispensacion

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.optoapp.data.Montura
import com.example.optoapp.ui.components.MonturaSearchField
import com.example.optoapp.ui.components.OptoDropdownMenuField
import com.example.optoapp.ui.components.OptoTextField
import com.example.optoapp.ui.theme.LocalOptoDensity
import com.example.optoapp.viewmodel.DispensacionUiState

@Composable
fun MonturaForm(
    uiState: DispensacionUiState,
    onUpdate: (DispensacionUiState) -> Unit,
    monturasActivas: List<Montura>,
) {
    val density = LocalOptoDensity.current
    Card {
        Column(modifier = Modifier.padding(density.cardPadding), verticalArrangement = Arrangement.spacedBy(density.blockGap)) {
            Text("Información de Montura", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            OptoDropdownMenuField(label = "Origen", selected = uiState.origenMontura, options = listOf("Tienda", "Paciente")) {
                if (it == "Tienda") {
                    onUpdate(uiState.copy(origenMontura = it))
                } else {
                    onUpdate(uiState.copy(origenMontura = it, monturaId = ""))
                }
            }
            if (uiState.origenMontura == "Tienda" || uiState.origenMontura == "Nueva de Tienda") {
                MonturaSearchField(
                    monturas = monturasActivas,
                    selectedMonturaId = uiState.monturaId.ifBlank { null },
                    onMonturaSelected = { montura ->
                        onUpdate(
                            uiState.copy(
                                monturaId = montura.id,
                                tipoAro = montura.tipoAro,
                                materialMontura = montura.materialMontura,
                            ),
                        )
                    },
                    onClear = {
                        onUpdate(
                            uiState.copy(
                                monturaId = "",
                                descripcionMontura = "",
                                tipoAro = "",
                                materialMontura = "",
                            ),
                        )
                    },
                    onlyInStock = false,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            OptoDropdownMenuField(label = "Tipo de Aro", selected = uiState.tipoAro, options = listOf("Aro Completo", "Semi al aire", "Al aire")) {
                onUpdate(uiState.copy(tipoAro = it))
            }
            OptoDropdownMenuField(label = "Material", selected = uiState.materialMontura, options = listOf("Acetato", "Metal", "Carey", "TR-90", "Econ")) {
                onUpdate(uiState.copy(materialMontura = it))
            }
            OptoTextField(value = uiState.descripcionMontura, onValueChange = { onUpdate(uiState.copy(descripcionMontura = it)) }, label = "Descripción (Marca, Modelo)")
        }
    }
}
