package com.example.optoapp.ui.screens.inventariofisico

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
import com.example.optoapp.data.InventarioFisico
import com.example.optoapp.viewmodel.InventarioFisicoUiState
import com.example.optoapp.viewmodel.InventarioFisicoViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventarioFisicoScreen(
    navController: NavController,
    viewModel: InventarioFisicoViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadSessions()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Inventario Físico") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver")
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { viewModel.createSession() }) {
                Icon(Icons.Default.Add, "Nuevo conteo")
            }
        },
    ) { padding ->
        when (val state = uiState) {
            is InventarioFisicoUiState.Loading -> {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is InventarioFisicoUiState.Error -> {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(state.message, color = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = { viewModel.loadSessions() }) {
                            Text("Reintentar")
                        }
                    }
                }
            }
            is InventarioFisicoUiState.SessionList -> {
                if (state.sessions.isEmpty()) {
                    Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Sin conteos registrados", style = MaterialTheme.typography.bodyLarge)
                            Spacer(Modifier.height(8.dp))
                            OutlinedButton(onClick = { viewModel.createSession() }) {
                                Text("Iniciar primer conteo")
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(padding),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(state.sessions) { session ->
                            SessionCard(session) {
                                viewModel.loadSessionDetail(session.id)
                            }
                        }
                    }
                }
            }
            is InventarioFisicoUiState.Detail -> {
                MonturaScanScreen(
                    session = state.session,
                    detalles = state.detalles,
                    progressMessage = state.progressMessage,
                    onUpdateStock = { id, stock -> viewModel.updateStockContado(id, stock) },
                    onClose = { viewModel.closeSession() },
                    onBack = { viewModel.loadSessions() },
                )
            }
        }
    }
}

@Composable
private fun SessionCard(session: InventarioFisico, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = session.fecha.toString(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "Usuario: ${session.userId}",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            AssistChip(
                onClick = {},
                label = {
                    Text(
                        session.estado,
                        style = MaterialTheme.typography.labelSmall,
                    )
                },
            )
        }
    }
}
