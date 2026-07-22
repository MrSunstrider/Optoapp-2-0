package com.example.optoapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.optoapp.data.gastooperativo.GastoOperativoEntity
import com.example.optoapp.ui.components.OptoDatePickerDialog
import com.example.optoapp.ui.components.OptoTopAppBar
import com.example.optoapp.ui.theme.AlertRed
import com.example.optoapp.util.DateUtils
import com.example.optoapp.viewmodel.GastosViewModel
import kotlinx.coroutines.launch
import java.util.*

/**
 * @deprecated Replaced by [CostosYGastosScreen] Tab 2 "Gastos Operativos".
 *             Mantenida para retrocompatibilidad de rutas. No migrar nuevas funcionalidades aquí.
 */
@Deprecated("Reemplazada por CostosYGastosScreen Tab 2 (Gastos Operativos)")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GastosScreen(navController: NavController, drawerState: DrawerState, viewModel: GastosViewModel = hiltViewModel()) {
    val gastos by viewModel.allGastos.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()
    var showDatePicker by remember { mutableStateOf(false) }

    val totalMes = gastos.filter { it.fecha.month == java.time.LocalDate.now().month && it.fecha.year == java.time.LocalDate.now().year }.sumOf { it.monto }

    if (showDatePicker) {
        OptoDatePickerDialog(initialDate = uiState.fecha, onDateSelected = { viewModel.updateFecha(it) }, onDismiss = { showDatePicker = false })
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            OptoTopAppBar(
                title = "Gastos",
                navigationIcon = {
                    IconButton(onClick = { scope.launch { drawerState.open() } }) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu")
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { viewModel.showNewGasto() }, modifier = Modifier.navigationBarsPadding()) {
                Icon(Icons.Default.Add, contentDescription = "Añadir Gasto")
            }
        },
    ) { padding ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).navigationBarsPadding(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("Total del mes", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("s/. ${fmt(totalMes.toDouble())}", fontWeight = FontWeight.Bold, fontSize = 22.sp, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            if (gastos.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.MoneyOff, contentDescription = "Sin gastos", modifier = Modifier.size(40.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                            Spacer(Modifier.height(8.dp))
                            Text("Sin gastos registrados", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            items(gastos) { gasto ->
                GastoCard(
                    gasto = gasto,
                    onEdit = { viewModel.editGasto(gasto) },
                    onDelete = { viewModel.delete(gasto) },
                )
            }

            item { Spacer(Modifier.height(80.dp)) }
        }
    }

    if (uiState.isDialogVisible) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissDialog() },
            title = { Text(if (uiState.editingGasto != null) "Editar Gasto" else "Nuevo Gasto", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    var expanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                        OutlinedTextField(
                            value = uiState.categoria,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Categoría") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true),
                        )
                        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            viewModel.categorias.forEach { cat ->
                                DropdownMenuItem(text = { Text(cat) }, onClick = {
                                    viewModel.updateCategoria(cat)
                                    expanded = false
                                })
                            }
                        }
                    }

                    OutlinedTextField(value = uiState.monto, onValueChange = { viewModel.updateMonto(it) }, label = { Text("Monto") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())

                    OutlinedTextField(value = uiState.descripcion, onValueChange = { viewModel.updateDescripcion(it) }, label = { Text("Descripción (opcional)") }, modifier = Modifier.fillMaxWidth())

                    OutlinedButton(onClick = { showDatePicker = true }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.DateRange, contentDescription = "Seleccionar fecha", modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(DateUtils.formatLocalized(uiState.fecha))
                    }

                    OutlinedTextField(value = uiState.nota, onValueChange = { viewModel.updateNota(it) }, label = { Text("Nota (opcional)") }, modifier = Modifier.fillMaxWidth())

                    uiState.error?.let {
                        Text(it, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
                    }
                }
            },
            confirmButton = {
                Button(onClick = { viewModel.save() }) { Text("Guardar") }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissDialog() }) { Text("Cancelar") }
            },
        )
    }
}

@Composable
private fun GastoCard(gasto: GastoOperativoEntity, onEdit: () -> Unit, onDelete: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp)) {
        Row(
            modifier = Modifier.padding(14.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(gasto.categoria, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                if (!gasto.descripcion.isNullOrBlank()) {
                    Text(gasto.descripcion, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(DateUtils.formatLocalized(gasto.fecha), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
            }
            Text("s/. ${fmt(gasto.monto.toDouble())}", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = AlertRed, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.width(8.dp))
            IconButton(modifier = Modifier.size(48.dp), onClick = onEdit) {
                Icon(Icons.Default.Edit, contentDescription = "Editar", modifier = Modifier.size(18.dp))
            }
            IconButton(modifier = Modifier.size(48.dp), onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = AlertRed, modifier = Modifier.size(18.dp))
            }
        }
    }
}

private fun fmt(value: Double): String = if (value == value.toLong().toDouble()) {
    String.format(Locale.getDefault(), "%,.0f", value)
} else {
    String.format(Locale.getDefault(), "%,.2f", value)
}
