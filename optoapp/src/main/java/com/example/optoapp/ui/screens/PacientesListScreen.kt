package com.example.optoapp.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonSearch
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
import com.example.optoapp.OptoApplication
import com.example.optoapp.data.AppRoles
import com.example.optoapp.data.Paciente
import com.example.optoapp.viewmodel.AuthViewModel
import com.example.optoapp.viewmodel.PacienteViewModel
import com.example.optoapp.viewmodel.SubscriptionViewModel
import com.example.optoapp.subscription.SubscriptionTier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.optoapp.testing.TestTags
import com.example.optoapp.util.DateUtils
import kotlinx.coroutines.launch
import com.example.optoapp.ui.components.OptoTopAppBar
import com.example.optoapp.ui.components.OptoCard
import com.example.optoapp.ui.components.OptoDialog
import com.example.optoapp.ui.components.OptoFilterChip
import com.example.optoapp.ui.components.OptoTextField
import com.example.optoapp.ui.theme.OptoTokens

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PacientesListScreen(navController: NavController, drawerState: DrawerState, viewModel: PacienteViewModel = hiltViewModel(), subscriptionVm: SubscriptionViewModel = hiltViewModel(), authViewModel: AuthViewModel = hiltViewModel()) {
    val pacientes by viewModel.pacientes.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val activeFilter by viewModel.activeFilter.collectAsState()
    val canAddPaciente by subscriptionVm.canAddPaciente.collectAsState()
    val tier by subscriptionVm.tier.collectAsState()
    val opticaRol by authViewModel.opticaRol.collectAsState(initial = "admin")
    val canCreateEdit = AppRoles.canCreateEditPacientes(opticaRol)
    val context = LocalContext.current
    var showPaywall by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        subscriptionVm.refreshPlanFromServer()
    }

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
                } else if (canAddPaciente) navController.navigate("nuevoPaciente")
                else showPaywall = true
            },
                modifier = Modifier.navigationBarsPadding()
            ) {
                Icon(Icons.Default.Add, contentDescription = "Añadir Paciente")
            }
        }
    ) { padding ->
        if (showPaywall) {
            OptoDialog(
                onDismissRequest = { showPaywall = false },
                title = "Límite del plan gratuito",
                confirmText = "Actualizar plan",
                onConfirm = {
                    subscriptionVm.launchProPurchase(
                        onSuccess = { android.widget.Toast.makeText(context, "PRO activado — pacientes ilimitados.", android.widget.Toast.LENGTH_LONG).show() },
                        onError = { android.widget.Toast.makeText(context, it, android.widget.Toast.LENGTH_LONG).show() }
                    )
                    showPaywall = false
                },
                dismissText = "Cerrar",
                content = {
                    Column(verticalArrangement = Arrangement.spacedBy(OptoTokens.spacing.sm)) {
                        Text("Has alcanzado el máximo de pacientes del plan gratuito. Pasa a PRO para registros ilimitados.")
                        Text("Plan actual: ${if (tier == SubscriptionTier.PRO) "PRO" else "Gratuito"}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            )
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            // Search bar
            OptoTextField(
                value = searchQuery,
                onValueChange = { viewModel.onSearchQueryChange(it) },
                label = "Buscar",
                placeholder = "Nombre, ID o teléfono",
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Filter chips
            val filters = listOf("Todos", "Saldo Pendiente", "Entrega")
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(OptoTokens.spacing.sm),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                filters.forEach { filter ->
                    val isSelected = when (filter) {
                        "Todos" -> activeFilter == "" || activeFilter == "Todos"
                        "Saldo Pendiente" -> activeFilter == "Saldo Pendiente"
                        "Entrega" -> activeFilter == "Estado de entrega" || activeFilter == "Entrega"
                        else -> false
                    }
                    OptoFilterChip(
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
                        label = filter
                    )
                }
            }

            Spacer(modifier = Modifier.height(OptoTokens.spacing.lg))

            if (pacientes.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(OptoTokens.spacing.sm),
                        modifier = Modifier.padding(OptoTokens.spacing.xl)
                    ) {
                        Icon(
                            Icons.Default.PersonSearch,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "No hay pacientes registrados",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(
                            onClick = {
                                if (!canCreateEdit) {
                                    Toast.makeText(context, "Tu rol no permite crear pacientes.", Toast.LENGTH_SHORT).show()
                                } else if (canAddPaciente) navController.navigate("nuevoPaciente")
                                else showPaywall = true
                            }
                        ) {
                            Text("Añadir primer paciente")
                        }
                    }
                }
            } else {
            // Patient list
            LazyColumn(
                modifier = Modifier.fillMaxSize().testTag(TestTags.PACIENTE_LISTA),
                contentPadding = PaddingValues(bottom = 88.dp),
                verticalArrangement = Arrangement.spacedBy(OptoTokens.spacing.md)
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
                .padding(OptoTokens.spacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(OptoTokens.spacing.md)
        ) {
            // Avatar with gradient background
            Surface(
                modifier = Modifier.size(48.dp),
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(26.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Patient info
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
                        color = MaterialTheme.colorScheme.onSurface,
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
                    horizontalArrangement = Arrangement.spacedBy(OptoTokens.spacing.lg),
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
                    horizontalArrangement = Arrangement.spacedBy(OptoTokens.spacing.sm)
                ) {
                    Text(
                        text = "Creado: ${DateUtils.formatLocalized(paciente.fechaCreacion)}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Tags/chips
                    paciente.ultimasEtiquetas.take(2).forEach { etiqueta ->
                        Surface(
                            shape = MaterialTheme.shapes.small,
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
