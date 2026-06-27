package com.example.optoapp.ui.screens

import com.example.optoapp.data.esMasculino
import com.example.optoapp.data.esFemenino
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Male
import androidx.compose.material.icons.filled.Female
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.graphics.Color
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.optoapp.data.AppRoles
import com.example.optoapp.data.Paciente
import com.example.optoapp.viewmodel.AuthViewModel
import com.example.optoapp.viewmodel.PacienteViewModel
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.optoapp.testing.TestTags
import com.example.optoapp.util.DateUtils
import kotlinx.coroutines.launch
import com.example.optoapp.ui.components.OptoTopAppBar
import com.example.optoapp.ui.components.OptoCard
import com.example.optoapp.ui.theme.OptoTokens

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PacientesListScreen(navController: NavController, drawerState: DrawerState, viewModel: PacienteViewModel = hiltViewModel(), authViewModel: AuthViewModel = hiltViewModel()) {
    val pacientes by viewModel.pacientes.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val activeFilter by viewModel.activeFilter.collectAsState()
    val opticaRol by authViewModel.opticaRol.collectAsState(initial = "admin")
    val canCreateEdit = AppRoles.canCreateEditPacientes(opticaRol)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            OptoTopAppBar(
                title = "Pacientes",
                navigationIcon = {
                    IconButton(onClick = { scope.launch { drawerState.open() } }) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                if (!canCreateEdit) {
                    Toast.makeText(context, "Tu rol no permite crear pacientes.", Toast.LENGTH_SHORT).show()
                } else navController.navigate("nuevoPaciente")
            },
                modifier = Modifier.navigationBarsPadding()
            ) {
                Icon(Icons.Default.Add, contentDescription = "Añadir Paciente")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.onSearchQueryChange(it) },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Buscar por nombre, ID o teléfono...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                shape = MaterialTheme.shapes.medium
            )

            Spacer(modifier = Modifier.height(12.dp))

            val filters = listOf("Todos", "Saldo Pendiente", "Entrega")
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                filters.forEach { filter ->
                    val isSelected = when (filter) {
                        "Todos" -> activeFilter == "" || activeFilter == "Todos"
                        "Saldo Pendiente" -> activeFilter == "Saldo Pendiente"
                        "Entrega" -> activeFilter == "Estado de entrega" || activeFilter == "Entrega"
                        else -> false
                    }
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            viewModel.setFilter(
                                when (filter) {
                                    "Todos" -> ""
                                    "Saldo Pendiente" -> "Saldo Pendiente"
                                    "Entrega" -> "Estado de entrega"
                                    else -> ""
                                }
                            )
                        },
                        label = { Text(filter, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(
                modifier = Modifier.fillMaxSize().testTag(TestTags.PACIENTE_LISTA),
                contentPadding = PaddingValues(bottom = 88.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(pacientes, key = { it.id }) { paciente ->
                    PacienteCard(paciente) {
                        navController.navigate("detallePaciente/${paciente.id}")
                    }
                }
            }
        }
    }
}

@Composable
private fun PacienteCard(paciente: Paciente, onClick: () -> Unit) {
    OptoCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = OptoTokens.shapes.large,
        elevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            val avatarColor = when {
                paciente.esMasculino() -> Color(0xFF2196F3)
                paciente.esFemenino() -> Color(0xFFE91E63)
                else -> MaterialTheme.colorScheme.primary
            }
            val avatarIcon = when {
                paciente.esMasculino() -> Icons.Default.Male
                paciente.esFemenino() -> Icons.Default.Female
                else -> Icons.Default.Person
            }
            Surface(
                modifier = Modifier.size(48.dp),
                shape = RoundedCornerShape(14.dp),
                color = avatarColor.copy(alpha = 0.12f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = avatarIcon,
                        contentDescription = null,
                        modifier = Modifier.size(26.dp),
                        tint = avatarColor
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = paciente.nombreCompleto,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = when {
                            paciente.esMasculino() -> Color(0xFF1976D2)
                            paciente.esFemenino() -> Color(0xFFC2185B)
                            else -> MaterialTheme.colorScheme.onSurface
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "ID: ${paciente.id.take(8)}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Edad: ${paciente.edad}",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Tel: ${paciente.telefono}",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Creado: ${DateUtils.formatLocalized(paciente.fechaCreacion)}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    paciente.ultimasEtiquetas.take(2).forEach { etiqueta ->
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                        ) {
                            Text(
                                text = etiqueta,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
