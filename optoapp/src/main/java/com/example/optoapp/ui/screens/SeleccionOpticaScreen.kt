package com.example.optoapp.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.optoapp.viewmodel.AuthViewModel
import kotlinx.coroutines.launch
import com.example.optoapp.ui.components.OptoTopAppBar
import com.example.optoapp.ui.components.OptoCard

/**
 * Elección de óptica cuando el usuario tiene varias membresías ([usuario_optica]).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SeleccionOpticaScreen(
    navController: NavController,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val memberships by viewModel.pendingMemberships.collectAsState()
    val isPinRequired by viewModel.isPinRequired.collectAsState(initial = false)
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(memberships) {
        if (memberships.isEmpty()) {
            navController.navigate(if (isPinRequired) "pin" else "main") {
                popUpTo("seleccion_optica") { inclusive = true }
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            OptoTopAppBar(title = "Seleccionar óptica")
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Text(
                    "Tienes acceso a varias ópticas. Elige con cuál trabajar ahora.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(16.dp))
            }
            items(memberships, key = { it.opticaId }) { m ->
                OptoCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = !busy) {
                            scope.launch {
                                busy = true
                                viewModel.selectOptica(m)
                                val dest = if (isPinRequired) "pin" else "main"
                                navController.navigate(dest) {
                                    popUpTo("seleccion_optica") { inclusive = true }
                                }
                                busy = false
                            }
                        }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(m.nombre, style = MaterialTheme.typography.titleMedium)
                            Text(
                                "Rol: ${m.rol}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                    }
                }
            }
        }
    }
}
