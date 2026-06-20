package com.example.optoapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.optoapp.data.AppRoles
import com.example.optoapp.data.Proveedor
import com.example.optoapp.ui.components.OptoTextField
import com.example.optoapp.ui.components.OptoTopAppBar
import com.example.optoapp.viewmodel.AuthViewModel
import com.example.optoapp.viewmodel.ProveedoresViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProveedoresScreen(
    navController: NavController,
    viewModel: ProveedoresViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val proveedores by viewModel.proveedores.collectAsState()
    val opticaRol by authViewModel.opticaRol.collectAsState(initial = "admin")
    val canEdit = AppRoles.canEditInventory(opticaRol)

    val filtrados = proveedores.filter {
        if (uiState.query.isBlank()) true
        else {
            it.nombre.contains(uiState.query, ignoreCase = true) ||
                it.ruc.contains(uiState.query, ignoreCase = true) ||
                it.telefono.contains(uiState.query, ignoreCase = true)
        }
    }

    if (uiState.editing) {
        AlertDialog(
            onDismissRequest = { viewModel.cancelEdit() },
            title = { Text(if (uiState.form.id == null) "Nuevo Proveedor" else "Editar Proveedor") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OptoTextField(
                        value = uiState.form.nombre,
                        onValueChange = { v -> viewModel.updateForm { it.copy(nombre = v) } },
                        label = "Nombre *"
                    )
                    OptoTextField(
                        value = uiState.form.ruc,
                        onValueChange = { v -> viewModel.updateForm { it.copy(ruc = v) } },
                        label = "RUC *"
                    )
                    OptoTextField(
                        value = uiState.form.telefono,
                        onValueChange = { v -> viewModel.updateForm { it.copy(telefono = v) } },
                        label = "Teléfono"
                    )
                    OptoTextField(
                        value = uiState.form.email,
                        onValueChange = { v -> viewModel.updateForm { it.copy(email = v) } },
                        label = "Email"
                    )
                    OptoTextField(
                        value = uiState.form.direccion,
                        onValueChange = { v -> viewModel.updateForm { it.copy(direccion = v) } },
                        label = "Dirección"
                    )
                    OptoTextField(
                        value = uiState.form.contacto,
                        onValueChange = { v -> viewModel.updateForm { it.copy(contacto = v) } },
                        label = "Persona de contacto"
                    )
                    if (uiState.error != null) {
                        Text(uiState.error ?: "", color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            confirmButton = { TextButton(onClick = { viewModel.save() }) { Text("Guardar") } },
            dismissButton = { TextButton(onClick = { viewModel.cancelEdit() }) { Text("Cancelar") } }
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            OptoTopAppBar(
                title = "Proveedores",
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                },
                actions = {
                    if (canEdit) {
                        IconButton(onClick = { viewModel.startCreate() }) {
                            Icon(Icons.Default.Add, contentDescription = "Nuevo proveedor")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 14.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OptoTextField(
                value = uiState.query,
                onValueChange = viewModel::onQueryChange,
                label = "Buscar por nombre, RUC o teléfono"
            )

            if (!uiState.error.isNullOrBlank()) {
                Text(uiState.error ?: "", color = MaterialTheme.colorScheme.error)
            }
            if (!uiState.success.isNullOrBlank()) {
                Text(uiState.success ?: "", color = MaterialTheme.colorScheme.tertiary)
            }

            if (filtrados.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No hay proveedores registrados",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(filtrados, key = { it.id }) { proveedor ->
                        ProveedorCard(
                            proveedor = proveedor,
                            canEdit = canEdit,
                            onEdit = { viewModel.startEdit(it) },
                            onDelete = { viewModel.delete(it) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProveedorCard(
    proveedor: Proveedor,
    canEdit: Boolean,
    onEdit: (Proveedor) -> Unit,
    onDelete: (Proveedor) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = proveedor.nombre,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = if (proveedor.activo)
                        MaterialTheme.colorScheme.primaryContainer
                    else
                        MaterialTheme.colorScheme.errorContainer
                ) {
                    Text(
                        text = if (proveedor.activo) "Activo" else "Inactivo",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
            Text("RUC: ${proveedor.ruc}", style = MaterialTheme.typography.bodyMedium)
            if (proveedor.telefono.isNotBlank()) {
                Text("Tel: ${proveedor.telefono}", style = MaterialTheme.typography.bodySmall)
            }
            if (proveedor.email.isNotBlank()) {
                Text(proveedor.email, style = MaterialTheme.typography.bodySmall)
            }
            if (canEdit) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { onEdit(proveedor) }) { Text("Editar") }
                    TextButton(
                        onClick = { onDelete(proveedor) },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) { Text("Desactivar") }
                }
            }
        }
    }
}
