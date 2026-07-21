package com.example.optoapp.ui.screens.ordenescompra

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.optoapp.data.Proveedor
import com.example.optoapp.ui.components.OptoTextField
import com.example.optoapp.viewmodel.OrdenesCompraViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrdenCompraEditDialog(
    viewModel: OrdenesCompraViewModel,
    onDismiss: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    val form = uiState.form

    // Actually we need the repository. Let's get the sessionManager opticaId from viewModel
    // We'll load proveedores via LaunchedEffect with a direct repository reference
    var proveedores by remember { mutableStateOf<List<Proveedor>>(emptyList()) }

    // For simplicity, accept proveedores as parameter or load via DI
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (form.id == null) "Nueva Orden de Compra" else "Editar OC ${form.numero}") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().heightIn(max = 500.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OptoTextField(
                    value = form.proveedorId,
                    onValueChange = { v -> viewModel.updateForm { it.copy(proveedorId = v) } },
                    label = "Proveedor ID *",
                )
                OptoTextField(
                    value = form.numero,
                    onValueChange = { v -> viewModel.updateForm { it.copy(numero = v) } },
                    label = "Número OC",
                )
                if (uiState.error != null) {
                    Text(uiState.error ?: "", color = MaterialTheme.colorScheme.error)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Ítems:", style = MaterialTheme.typography.titleSmall)
                    IconButton(onClick = { viewModel.addItem() }) {
                        Icon(Icons.Default.Add, contentDescription = "Agregar ítem")
                    }
                }
                LazyColumn(
                    modifier = Modifier.heightIn(max = 300.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    itemsIndexed(form.items) { index, item ->
                        EditItemRow(
                            item = item,
                            index = index,
                            onUpdate = { idx, upd -> viewModel.updateItem(idx, upd) },
                            onRemove = { viewModel.removeItem(index) },
                        )
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { viewModel.save() }) { Text("Guardar") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}

@Composable
private fun EditItemRow(
    item: com.example.optoapp.viewmodel.OCItemFormState,
    index: Int,
    onUpdate: (Int, (com.example.optoapp.viewmodel.OCItemFormState) -> com.example.optoapp.viewmodel.OCItemFormState) -> Unit,
    onRemove: () -> Unit,
) {
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Ítem ${index + 1}", style = MaterialTheme.typography.labelSmall)
                IconButton(onClick = onRemove, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Default.Delete, "Quitar", modifier = Modifier.size(16.dp))
                }
            }
            OptoTextField(
                value = item.monturaId,
                onValueChange = { v -> onUpdate(index) { it.copy(monturaId = v) } },
                label = "Montura ID",
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OptoTextField(
                    value = item.cantidad,
                    onValueChange = { v -> onUpdate(index) { it.copy(cantidad = v) } },
                    label = "Cant.",
                    modifier = Modifier.weight(1f),
                )
                OptoTextField(
                    value = item.costoUnitario,
                    onValueChange = { v -> onUpdate(index) { it.copy(costoUnitario = v) } },
                    label = "Costo U.",
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}
