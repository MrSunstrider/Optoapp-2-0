package com.example.optoapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.example.optoapp.data.AppRoles
import com.example.optoapp.util.SyncErrorSanitizer
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.optoapp.viewmodel.AuthViewModel
import kotlinx.coroutines.launch

@Composable
fun MainDrawerScreen(
    parentNavController: NavController,
    /** Misma instancia que [LoginScreen] / [PinScreen] en [MainActivity]; si no, logout no resetea el estado que lee el login. */
    authViewModel: AuthViewModel
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val navController = rememberNavController()
    
    // Obtenemos el ViewModel de sincronización a nivel de pantalla
    val syncViewModel: com.example.optoapp.viewmodel.SyncViewModel = hiltViewModel()
    val syncState by syncViewModel.syncState.collectAsState()
    val isSilentSyncing by syncViewModel.isSilentSyncing.collectAsState()
    val opticaRol by authViewModel.opticaRol.collectAsState(initial = "admin")
    val showCierreCaja = AppRoles.canViewCierreCaja(opticaRol)
    val showBiYReportes = AppRoles.canViewBiAndReports(opticaRol)

    // Sincronización automática al entrar al dashboard
    LaunchedEffect(Unit) {
        syncViewModel.performSilentSync()
    }
    
    var showErrorDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountCircle,
                        contentDescription = "Avatar",
                        modifier = Modifier.size(80.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(text = "OptoApp", style = MaterialTheme.typography.titleLarge)
                    Text(text = "Usuario Único", style = MaterialTheme.typography.bodyMedium)
                }
                HorizontalDivider()
                NavigationDrawerItem(
                    label = { Text("Pacientes") },
                    selected = currentRoute == "pacientes",
                    onClick = {
                        scope.launch { drawerState.close() }
                        navController.navigate("pacientes") {
                            popUpTo("pacientes") { inclusive = true }
                        }
                    },
                    icon = { Icon(Icons.Default.Person, contentDescription = null) },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                NavigationDrawerItem(
                    label = { Text("Servicios Varios") },
                    selected = currentRoute == "servicios_extra",
                    onClick = {
                        scope.launch { drawerState.close() }
                        navController.navigate("servicios_extra")
                    },
                    icon = { Icon(Icons.Default.AddShoppingCart, contentDescription = null) },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                NavigationDrawerItem(
                    label = { Text("Inventario Monturas") },
                    selected = currentRoute == "monturas",
                    onClick = {
                        scope.launch { drawerState.close() }
                        navController.navigate("monturas")
                    },
                    icon = { Icon(Icons.Default.Inventory2, contentDescription = null) },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                if (showCierreCaja) {
                    NavigationDrawerItem(
                        label = { Text("Cierre de Caja") },
                        selected = currentRoute == "cierre_caja",
                        onClick = {
                            scope.launch { drawerState.close() }
                            navController.navigate("cierre_caja")
                        },
                        icon = { Icon(Icons.Default.AccountBalanceWallet, contentDescription = null) },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                }
                if (showBiYReportes) {
                    NavigationDrawerItem(
                        label = { Text("Estadísticas (BI)") },
                        selected = currentRoute == "estadisticas_bi",
                        onClick = {
                            scope.launch { drawerState.close() }
                            navController.navigate("estadisticas_bi")
                        },
                        icon = { Icon(Icons.Default.BarChart, contentDescription = null) },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                    NavigationDrawerItem(
                        label = { Text("Reportes") },
                        selected = currentRoute == "reportes",
                        onClick = {
                            scope.launch { drawerState.close() }
                            navController.navigate("reportes")
                        },
                        icon = { Icon(Icons.Default.DateRange, contentDescription = null) },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                }
                NavigationDrawerItem(
                    label = { Text("Configuración") },
                    selected = currentRoute == "configuracion",
                    onClick = {
                        scope.launch { drawerState.close() }
                        navController.navigate("configuracion")
                    },
                    icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )

                // Paso 5.2: Botón de Sincronización Cloud
                val context = androidx.compose.ui.platform.LocalContext.current

                LaunchedEffect(syncState) {
                    when (syncState) {
                        is com.example.optoapp.viewmodel.SyncState.Success -> {
                            android.widget.Toast.makeText(context, (syncState as com.example.optoapp.viewmodel.SyncState.Success).message, android.widget.Toast.LENGTH_SHORT).show()
                        }
                        is com.example.optoapp.viewmodel.SyncState.Error -> {
                            errorMessage = SyncErrorSanitizer.forUserMessage(
                                (syncState as com.example.optoapp.viewmodel.SyncState.Error).message
                            )
                            showErrorDialog = true
                        }
                        else -> {}
                    }
                }

                if (showErrorDialog) {
                    AlertDialog(
                        onDismissRequest = { showErrorDialog = false },
                        confirmButton = { 
                            TextButton(onClick = { showErrorDialog = false }) { Text("Entendido") } 
                        },
                        title = { 
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Error, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Error de Sincronización")
                            }
                        },
                        text = { 
                            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                                Text(errorMessage)
                            }
                        }
                    )
                }

                NavigationDrawerItem(
                    label = { 
                        Text(if (syncState is com.example.optoapp.viewmodel.SyncState.Loading) "Sincronizando..." else "Sincronizar Cloud") 
                    },
                    selected = false,
                    onClick = {
                        if (syncState !is com.example.optoapp.viewmodel.SyncState.Loading) {
                            syncViewModel.performFullSync()
                        }
                    },
                    icon = { 
                        if (syncState is com.example.optoapp.viewmodel.SyncState.Loading) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.CloudSync, contentDescription = null)
                        }
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )

                Spacer(modifier = Modifier.weight(1f))
                NavigationDrawerItem(
                    label = { Text("Cerrar Sesión") },
                    selected = false,
                    onClick = {
                        scope.launch {
                            drawerState.close()
                            authViewModel.logout()
                            parentNavController.navigate("login") {
                                popUpTo(parentNavController.graph.id) { inclusive = true }
                            }
                        }
                    },
                    icon = { Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Salir") },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
            }
        }
    ) {
        Column {
            // Indicador discreto de sincronización de fondo
            if (isSilentSyncing) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth().height(2.dp),
                    color = MaterialTheme.colorScheme.secondary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }
            NavHost(navController = navController, startDestination = "pacientes", modifier = Modifier.weight(1f)) {
                composable("pacientes") { PacientesListScreen(navController, drawerState) }
                composable("nuevoPaciente") { NuevoPacienteScreen(navController) }
                composable("editarPaciente/{id}") { backStackEntry ->
                    NuevoPacienteScreen(navController, pacienteId = backStackEntry.arguments?.getString("id"))
                }
                composable("detallePaciente/{id}") { backStackEntry ->
                    DetallePacienteScreen(navController, id = backStackEntry.arguments?.getString("id")!!)
                }
                composable("nuevaEvaluacion/{pacienteId}") { backStackEntry ->
                    NuevaEvaluacionScreen(navController, pacienteId = backStackEntry.arguments?.getString("pacienteId")!!)
                }
                composable("editarEvaluacion/{pacienteId}/{evalId}") { backStackEntry ->
                    NuevaEvaluacionScreen(
                        navController, 
                        pacienteId = backStackEntry.arguments?.getString("pacienteId")!!,
                        evaluacionId = backStackEntry.arguments?.getString("evalId")
                    )
                }
                composable("nuevaDispensacion/{pacienteId}") { backStackEntry ->
                    NuevaDispensacionScreen(navController, pacienteId = backStackEntry.arguments?.getString("pacienteId")!!)
                }
                composable("editarDispensacion/{pacienteId}/{dispId}") { backStackEntry ->
                    NuevaDispensacionScreen(
                        navController, 
                        pacienteId = backStackEntry.arguments?.getString("pacienteId")!!,
                        dispensacionId = backStackEntry.arguments?.getString("dispId")
                    )
                }
                composable("servicios_extra") { 
                    ServiciosExtraScreen(navController, drawerState) 
                }
                composable("monturas") { MonturasScreen(navController) }
                composable("nuevo_servicio/{pacienteId}") { backStackEntry ->
                    NuevoServicioScreen(navController, pacienteId = backStackEntry.arguments?.getString("pacienteId"))
                }
                composable("editar_servicio/{id}") { backStackEntry ->
                    NuevoServicioScreen(navController, servicioId = backStackEntry.arguments?.getString("id"))
                }
                composable("reportes") { ReportesScreen(drawerState) }
                composable("cierre_caja") { CierreCajaScreen(navController) }
                composable("estadisticas_bi") { BIScreen(navController) }
                composable("configuracion") { ConfiguracionScreen(navController, drawerState) }
            }
        }
    }
}
