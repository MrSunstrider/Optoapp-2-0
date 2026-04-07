package com.example.optoapp.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

val osdiOptions = listOf(
    "Ninguna vez (0)" to 0,
    "Algunas veces (1)" to 1,
    "Mitad de las veces (2)" to 2,
    "La mayoría de las veces (3)" to 3,
    "Todo el tiempo (4)" to 4
)

val osdiBloque1 = listOf(
    "¿Sensibilidad a la luz?",
    "¿Sensación de arenilla o polvo?",
    "¿Dolor o ardor en los ojos?",
    "¿Visión borrosa o baja?"
)

val osdiBloque2 = listOf(
    "¿Leer o mirar pantallas?",
    "¿Conducir de noche?",
    "¿Usar cajeros automáticos o leer señales?",
    "¿Ver televisión?"
)

val osdiBloque3 = listOf(
    "¿Viento o aire acondicionado?",
    "¿Ambientes muy secos (calefacción, aire acondicionado)?",
    "¿Ambientes con humo o contaminación?",
    "¿Usar lentes de contacto (si aplica)?"
)

@Composable
fun OSDIDialog(
    onDismissRequest: () -> Unit,
    onSave: (puntuacion: Int, clasificacion: String) -> Unit
) {
    // Almacenamos respuestas como mapa: Index(0-11) -> Valor(0-4). Null = sin responder
    var answers by remember { mutableStateOf(mutableMapOf<Int, Int>()) }
    
    Dialog(onDismissRequest = onDismissRequest) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Cuestionario OSDI",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text("Bloque 1: Síntomas Visuales", fontWeight = FontWeight.Bold)
                    osdiBloque1.forEachIndexed { qIndex, question ->
                        QuestionItem(1 + qIndex, question, answers[qIndex]) { ans ->
                            val newMap = answers.toMutableMap()
                            newMap[qIndex] = ans
                            answers = newMap
                        }
                    }
                    
                    HorizontalDivider()
                    
                    Text("Bloque 2: Problemas con actividades diarias", fontWeight = FontWeight.Bold)
                    osdiBloque2.forEachIndexed { qIndex, question ->
                        val globalIndex = qIndex + 4
                        QuestionItem(1 + globalIndex, question, answers[globalIndex]) { ans ->
                            val newMap = answers.toMutableMap()
                            newMap[globalIndex] = ans
                            answers = newMap
                        }
                    }
                    
                    HorizontalDivider()
                    
                    Text("Bloque 3: Problemas con condiciones ambientales", fontWeight = FontWeight.Bold)
                    osdiBloque3.forEachIndexed { qIndex, question ->
                        val globalIndex = qIndex + 8
                        QuestionItem(1 + globalIndex, question, answers[globalIndex]) { ans ->
                            val newMap = answers.toMutableMap()
                            newMap[globalIndex] = ans
                            answers = newMap
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismissRequest) {
                        Text("Cancelar")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = {
                        if (answers.isNotEmpty()) {
                            val sum = answers.values.sum()
                            val answeredCount = answers.size
                            val score = Math.round((sum * 25.0) / answeredCount).toInt()
                            
                            val clasificacion = when {
                                score <= 12 -> "Normal"
                                score <= 22 -> "Leve"
                                score <= 32 -> "Moderado"
                                else -> "Severo"
                            }
                            
                            onSave(score, clasificacion)
                        } else {
                            onDismissRequest() // Si no se respondió nada, cierra sin guardar
                        }
                    }) {
                        Text("Guardar y Cerrar")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuestionItem(number: Int, question: String, selectedValue: Int?, onSelect: (Int) -> Unit) {
    Column {
        Text(text = "$number. $question", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 8.dp))
        var expanded by remember { mutableStateOf(false) }
        val selectedText = if (selectedValue != null) osdiOptions.find { it.second == selectedValue }?.first ?: "Seleccionar..." else "Seleccionar..."
        
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it }
        ) {
            OutlinedTextField(
                value = selectedText,
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier.fillMaxWidth().menuAnchor()
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                osdiOptions.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option.first) },
                        onClick = {
                            onSelect(option.second)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}
