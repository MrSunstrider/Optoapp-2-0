package com.example.optoapp.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.optoapp.data.AppRoles
import com.example.optoapp.data.Paciente
import com.example.optoapp.data.PacienteListFilters
import com.example.optoapp.data.Resource
import com.example.optoapp.testing.TestTags
import com.example.optoapp.ui.components.OptoCard
import com.example.optoapp.ui.components.OptoTopAppBar
import com.example.optoapp.ui.navigation.Route
import com.example.optoapp.ui.components.paciente.ResumenDispensacionDialog
import com.example.optoapp.ui.components.paciente.ResumenEvaluacionDialog
import com.example.optoapp.ui.components.paciente.SexoSymbol
import com.example.optoapp.ui.components.paciente.sexoAvatarColor
import com.example.optoapp.ui.components.paciente.sexoSymbolOf
import com.example.optoapp.ui.theme.alertRed
import com.example.optoapp.ui.theme.OptoTokens
import com.example.optoapp.ui.theme.positiveGreen
import com.example.optoapp.util.DateUtils
import com.example.optoapp.viewmodel.AuthViewModel
import com.example.optoapp.viewmodel.PacienteViewModel
import kotlinx.coroutines.launch

private enum class QuickSummaryDialog { NONE, EVAL, DISP }

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PacientesListScreen(
    navController: NavController,
    drawerState: DrawerState,
    viewModel: PacienteViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel(),
) {
    val pacientes by viewModel.pacientes.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val activeFilter by viewModel.activeFilter.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val opticaRol by authViewModel.opticaRol.collectAsState(initial = "")
    val canCreateEdit = opticaRol in setOf("admin", "gerente")
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var activeDialog by remember { mutableStateOf(QuickSummaryDialog.NONE) }
    val lastEvalState by viewModel.lastEvaluacion.collectAsState()
    val lastDispState by viewModel.lastDispensacion.collectAsState()
    val pagosSumByDispensacion by viewModel.pagosSumByDispensacion.collectAsState()
    val closeAndResetEval: () -> Unit = {
        activeDialog = QuickSummaryDialog.NONE
        viewModel.resetLastEvaluacion()
    }
    val closeAndResetDisp: () -> Unit = {
        activeDialog = QuickSummaryDialog.NONE
        viewModel.resetLastDispensacion()
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
                },
                actions = {
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Actualizar")
                    }
                    var showSortMenu by remember { mutableStateOf(false) }
                    IconButton(onClick = { showSortMenu = true }) {
                        Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = "Ordenar")
                    }
                    DropdownMenu(expanded = showSortMenu, onDismissRequest = { showSortMenu = false }) {
                        DropdownMenuItem(text = { Text("Nombre A-Z") }, onClick = {
                            viewModel.setSort("nombre")
                            showSortMenu = false
                        })
                        DropdownMenuItem(text = { Text("Más recientes") }, onClick = {
                            viewModel.setSort("reciente")
                            showSortMenu = false
                        })
                        DropdownMenuItem(text = { Text("Más antiguos") }, onClick = {
                            viewModel.setSort("antiguo")
                            showSortMenu = false
                        })
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (!canCreateEdit) {
                        Toast.makeText(context, "Tu rol no permite crear pacientes.", Toast.LENGTH_SHORT).show()
                    } else {
                        navController.navigate(Route.NuevoPaciente.route)
                    }
                },
                modifier = Modifier.navigationBarsPadding(),
            ) { Icon(Icons.Default.Add, contentDescription = "Añadir Paciente") }
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp).navigationBarsPadding(),
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.onSearchQueryChange(it) },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Buscar por nombre, ID, teléfono u OT...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Buscar") },
                shape = MaterialTheme.shapes.medium,
            )
            Spacer(modifier = Modifier.height(10.dp))

            val filters = listOf(
                PacienteListFilters.TODOS,
                PacienteListFilters.SALDO_PENDIENTE,
                PacienteListFilters.ESTADO_ENTREGA,
            )
            FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                filters.forEach { filter ->
                    val isSelected = when (filter) {
                        PacienteListFilters.TODOS -> activeFilter.isNullOrBlank()
                        PacienteListFilters.SALDO_PENDIENTE -> activeFilter == PacienteListFilters.SALDO_PENDIENTE
                        PacienteListFilters.ESTADO_ENTREGA -> activeFilter == PacienteListFilters.ESTADO_ENTREGA
                        else -> false
                    }
                    FilterChip(selected = isSelected, onClick = {
                        viewModel.setFilter(filter)
                    }, label = { Text(filter, fontSize = 13.sp, fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal) })
                }
            }
            Spacer(modifier = Modifier.height(14.dp))

            if (isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
            }

            error?.let { errMsg ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.alertRed.copy(alpha = 0.1f)),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(Icons.Default.Error, contentDescription = "Error", tint = MaterialTheme.colorScheme.alertRed, modifier = Modifier.size(32.dp))
                        Spacer(Modifier.height(8.dp))
                        Text(errMsg, color = MaterialTheme.colorScheme.alertRed, style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(onClick = { viewModel.refresh() }) {
                            Text("Reintentar")
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (pacientes.isEmpty() && !isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.PersonOff, contentDescription = "Sin pacientes", modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                        Spacer(Modifier.height(8.dp))
                        Text("No se encontraron pacientes", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else if (pacientes.isNotEmpty()) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().testTag(TestTags.PACIENTE_LISTA),
                    contentPadding = PaddingValues(bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(pacientes, key = { it.id }) { paciente ->
                        PacienteCard(
                            paciente = paciente,
                            onClick = { navController.navigate(Route.DetallePaciente(paciente.id).route) },
                            onShowLastEvaluacion = { id ->
                                activeDialog = QuickSummaryDialog.EVAL
                                viewModel.loadLastEvaluacion(id)
                            },
                            onShowLastDispensacion = { id ->
                                activeDialog = QuickSummaryDialog.DISP
                                viewModel.loadLastDispensacion(id)
                            },
                            onCall = {
                                val intent = android.content.Intent(android.content.Intent.ACTION_DIAL).apply { data = android.net.Uri.parse("tel:${paciente.telefono}") }
                                context.startActivity(intent)
                            },
                        )
                    }
                }
            }
        }

        when (activeDialog) {
            QuickSummaryDialog.EVAL -> {
                lastEvalState?.let { resource ->
                    when (resource) {
                        is Resource.Loading -> Unit
                        is Resource.Success -> {
                            resource.data?.let { eval ->
                                val paciente = pacientes.find { it.id == eval.pacienteId }
                                if (paciente != null) {
                                    ResumenEvaluacionDialog(
                                        eval = eval,
                                        paciente = paciente,
                                        onDismiss = closeAndResetEval,
                                        onEdit = {
                                            navController.navigate(Route.EditarEvaluacion(eval.pacienteId, eval.id).route)
                                        },
                                    )
                                } else {
                                    closeAndResetEval()
                                }
                            }
                        }
                        is Resource.Error<*> -> AlertDialog(onDismissRequest = closeAndResetEval, title = { Text("Sin Evaluaciones") }, text = { Text(resource.message ?: "No hay evaluaciones") }, confirmButton = { TextButton(onClick = closeAndResetEval) { Text("Cerrar") } })
                    }
                }
            }
            QuickSummaryDialog.DISP -> {
                lastDispState?.let { resource ->
                    when (resource) {
                        is Resource.Loading -> Unit
                        is Resource.Success -> {
                            resource.data?.let { disp ->
                                val paciente = pacientes.find { it.id == disp.pacienteId }
                                if (paciente != null) {
                                    ResumenDispensacionDialog(
                                        disp = disp,
                                        paciente = paciente,
                                        onDismiss = closeAndResetDisp,
                                        onEdit = {
                                            navController.navigate(Route.EditarDispensacion(disp.pacienteId, disp.id).route)
                                        },
                                        pagosSum = pagosSumByDispensacion[disp.id] ?: 0.0,
                                        onGoToFinanciero = { target ->
                                        closeAndResetDisp()
                                        navController.navigate(Route.InformacionFinanciera(target.id).route)
                                    })
                                } else {
                                    closeAndResetDisp()
                                }
                            }
                        }
                        is Resource.Error<*> -> AlertDialog(onDismissRequest = closeAndResetDisp, title = { Text("Sin Dispensaciones") }, text = { Text(resource.message ?: "No hay dispensaciones") }, confirmButton = { TextButton(onClick = closeAndResetDisp) { Text("Cerrar") } })
                    }
                }
            }
            QuickSummaryDialog.NONE -> Unit
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PacienteCard(
    paciente: Paciente,
    onClick: () -> Unit,
    onShowLastEvaluacion: (String) -> Unit,
    onShowLastDispensacion: (String) -> Unit,
    onCall: () -> Unit,
) {
    val symbol = sexoSymbolOf(paciente.sexo)
    val avatarColor = sexoAvatarColor(symbol, isSystemInDarkTheme()) ?: MaterialTheme.colorScheme.primary
    val (avatarIcon, avatarDescription) = when (symbol) {
        SexoSymbol.MARTE -> Icons.Default.Male to "Paciente masculino"
        SexoSymbol.VENUS -> Icons.Default.Female to "Paciente femenino"
        SexoSymbol.DESCONOCIDO -> Icons.Default.Person to "Sexo no registrado"
    }

    OptoCard(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = OptoTokens.shapes.large,
        elevation = 1.dp,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Surface(modifier = Modifier.size(44.dp), shape = RoundedCornerShape(12.dp), color = avatarColor.copy(alpha = 0.12f)) {
                Box(contentAlignment = Alignment.Center) { Icon(avatarIcon, contentDescription = avatarDescription, modifier = Modifier.size(24.dp), tint = avatarColor) }
            }

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(paciente.nombreCompleto, fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text("Edad: ${paciente.edad}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        DateUtils.formatLocalized(paciente.fechaCreacion),
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = { onShowLastEvaluacion(paciente.id) }, modifier = Modifier.size(36.dp).testTag(TestTags.PACIENTE_CARD_LAST_EVAL_BTN)) {
                        Icon(Icons.Default.Visibility, contentDescription = "Ver evaluación", modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = { onShowLastDispensacion(paciente.id) }, modifier = Modifier.size(36.dp).testTag(TestTags.PACIENTE_CARD_LAST_DISP_BTN)) {
                        Icon(Icons.Default.Inventory2, contentDescription = "Ver dispensación", modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.tertiary)
                    }
                    if (paciente.telefono.isNotBlank()) {
                        IconButton(onClick = onCall, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Default.Call, contentDescription = "Llamar", modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.positiveGreen)
                        }
                    }
                }
            }
        }
    }
}
