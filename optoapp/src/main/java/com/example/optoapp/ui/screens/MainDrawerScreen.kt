package com.example.optoapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import com.example.optoapp.R
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.optoapp.data.AppRoles
import com.example.optoapp.ui.components.OfflineBanner
import com.example.optoapp.ui.navigation.Route
import com.example.optoapp.ui.screens.ordenescompra.OrdenesCompraScreen
import com.example.optoapp.viewmodel.AuthViewModel
import com.example.optoapp.viewmodel.OpticaHeaderViewModel
import kotlinx.coroutines.launch

/** CompositionLocal para que cualquier pantalla pueda mostrar Snackbar sin acoplamiento. */
val LocalSnackbarHostState = staticCompositionLocalOf { SnackbarHostState() }

@Composable
fun MainDrawerScreen(
    parentNavController: NavController,
    /** Misma instancia que [LoginScreen] / [PinScreen] en [MainActivity]; si no, logout no resetea el estado que lee el login. */
    authViewModel: AuthViewModel,
) {
    val context = LocalContext.current
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val navController = rememberNavController()

    val syncViewModel: com.example.optoapp.viewmodel.SyncViewModel = hiltViewModel()
    val opticaHeaderViewModel: OpticaHeaderViewModel = hiltViewModel()
    val syncState by syncViewModel.syncState.collectAsState()
    val isSilentSyncing by syncViewModel.isSilentSyncing.collectAsState()
    val opticaHeader by opticaHeaderViewModel.uiState.collectAsState()
    val opticaRol by authViewModel.opticaRol.collectAsState(initial = "admin")
    val showCierreCaja = AppRoles.canViewCierreCaja(opticaRol)
    val showBiYReportes = AppRoles.canViewBiAndReports(opticaRol)
    val showOperacionHoy = AppRoles.canViewOperacionHoy(opticaRol)
    val showConfiguracion = AppRoles.canManageUsers(opticaRol)

    // Refresh cloud data so the user always sees the latest shared state on entry
    LaunchedEffect(Unit) {
        syncViewModel.performSilentSync()
    }

    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
    val snackbarHostState = remember { SnackbarHostState() }
    val isOnline by syncViewModel.isOnline.collectAsState()
    val snackbarScope = rememberCoroutineScope()

    CompositionLocalProvider(LocalSnackbarHostState provides snackbarHostState) {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet(modifier = Modifier.width(280.dp)) {
                    DrawerContent(
                        currentRoute = currentRoute,
                        drawerState = drawerState,
                        navController = navController,
                        opticaHeader = opticaHeader,
                        showCierreCaja = showCierreCaja,
                        showBiYReportes = showBiYReportes,
                        showOperacionHoy = showOperacionHoy,
                        showConfiguracion = showConfiguracion,
                        syncState = syncState,
                        syncViewModel = syncViewModel,
                        authViewModel = authViewModel,
                        parentNavController = parentNavController,
                    )
                }
            },
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background,
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    if (isSilentSyncing) {
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth().height(2.dp),
                            color = MaterialTheme.colorScheme.secondary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        )
                    }
                    Surface(
                        tonalElevation = 1.dp,
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding(),
                    ) {
                        TextButton(
                            onClick = {
                                scope.launch {
                                    val hasMultiple = authViewModel.prepareOpticaSelection()
                                    if (hasMultiple) {
                                        parentNavController.navigate(Route.SeleccionOptica.route)
                                    } else {
                                        android.widget.Toast.makeText(
                                            context,
                                            "Solo tienes una óptica asociada.",
                                            android.widget.Toast.LENGTH_SHORT,
                                        ).show()
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                        ) {
                            Text(
                                text = "Óptica activa: ${opticaHeader.nombreOptica} · ${opticaHeader.fiscalEtiqueta}",
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(vertical = 2.dp),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                    OfflineBanner(isOnline = isOnline)

                    Box(modifier = Modifier.weight(1f)) {
                        NavHost(navController = navController, startDestination = Route.OperacionHoy.route, modifier = Modifier.fillMaxSize()) {
                            composable(Route.Pacientes.route) { PacientesListScreen(navController, drawerState) }
                            composable(Route.Agenda.route) { AgendaScreen(navController, drawerState) }
                            composable(Route.NuevoPaciente.route) { NuevoPacienteScreen(navController) }
                            composable(Route.EditarPaciente("{id}").route) { backStackEntry ->
                                NuevoPacienteScreen(navController, pacienteId = backStackEntry.arguments?.getString("id"))
                            }
                            composable(Route.DetallePaciente("{id}").route) { backStackEntry ->
                                val pid = backStackEntry.arguments?.getString("id")
                                if (pid.isNullOrBlank()) {
                                    LaunchedEffect(Unit) { navController.popBackStack() }
                                    Box(Modifier.fillMaxSize())
                                } else {
                                    DetallePacienteScreen(navController, id = pid)
                                }
                            }
                            composable(Route.NuevaEvaluacion("{pacienteId}").route) { backStackEntry ->
                                val pacienteId = backStackEntry.arguments?.getString("pacienteId")
                                if (pacienteId.isNullOrBlank()) {
                                    LaunchedEffect(Unit) { navController.popBackStack() }
                                    Box(Modifier.fillMaxSize())
                                } else {
                                    NuevaEvaluacionScreen(navController, pacienteId = pacienteId)
                                }
                            }
                            composable(Route.EditarEvaluacion("{pacienteId}", "{evalId}").route) { backStackEntry ->
                                val pacienteId = backStackEntry.arguments?.getString("pacienteId")
                                if (pacienteId.isNullOrBlank()) {
                                    LaunchedEffect(Unit) { navController.popBackStack() }
                                    Box(Modifier.fillMaxSize())
                                } else {
                                    NuevaEvaluacionScreen(
                                        navController,
                                        pacienteId = pacienteId,
                                        evaluacionId = backStackEntry.arguments?.getString("evalId"),
                                    )
                                }
                            }
                            composable(Route.NuevaDispensacion("{pacienteId}").route) { backStackEntry ->
                                val pacienteId = backStackEntry.arguments?.getString("pacienteId")
                                if (pacienteId.isNullOrBlank()) {
                                    LaunchedEffect(Unit) { navController.popBackStack() }
                                    Box(Modifier.fillMaxSize())
                                } else {
                                    NuevaDispensacionScreen(navController, pacienteId = pacienteId)
                                }
                            }
                            composable(Route.EditarDispensacion("{pacienteId}", "{dispId}").route) { backStackEntry ->
                                val pacienteId = backStackEntry.arguments?.getString("pacienteId")
                                if (pacienteId.isNullOrBlank()) {
                                    LaunchedEffect(Unit) { navController.popBackStack() }
                                    Box(Modifier.fillMaxSize())
                                } else {
                                    NuevaDispensacionScreen(
                                        navController,
                                        pacienteId = pacienteId,
                                        dispensacionId = backStackEntry.arguments?.getString("dispId"),
                                    )
                                }
                            }
                            composable(Route.ServiciosExtra.route) {
                                ServiciosExtraScreen(navController, drawerState)
                            }
                            composable(Route.Monturas.route) { MonturasScreen(navController) }
                            composable(Route.Proveedores.route) { ProveedoresScreen(navController) }
                            composable(Route.OrdenesCompra.route) { OrdenesCompraScreen(navController) }
                            composable(Route.InventarioFisico.route) { com.example.optoapp.ui.screens.inventariofisico.InventarioFisicoScreen(navController) }
                            composable(Route.Gastos.route) { CostosYGastosScreen(navController, drawerState) }
                            composable(Route.OperacionHoy.route) { OperacionHoyScreen(navController, drawerState) }
                            composable(Route.NuevoServicio.route) {
                                NuevoServicioScreen(navController, pacienteId = null)
                            }
                            composable(Route.NuevoServicioPaciente("{pacienteId}").route) { backStackEntry ->
                                NuevoServicioScreen(navController, pacienteId = backStackEntry.arguments?.getString("pacienteId"))
                            }
                            composable(Route.EditarServicio("{id}").route) { backStackEntry ->
                                NuevoServicioScreen(navController, servicioId = backStackEntry.arguments?.getString("id"))
                            }
                            composable(Route.Reportes.route) { ReportesScreen(drawerState) }
                            composable(Route.CostosYGastos.route) { CostosYGastosScreen(navController, drawerState) }
                            composable(Route.CostosYGastosDisp("{dispensacionId}").route) { backStackEntry ->
                                val dispId = backStackEntry.arguments?.getString("dispensacionId")
                                CostosYGastosScreen(navController, drawerState, dispensacionId = dispId)
                            }
                            composable(Route.CierreCaja.route) { CierreCajaScreen(navController) }
                            composable(Route.EstadisticasBI.route) { AnalisisNegocioScreen(navController) }
                            composable(Route.AnalisisDetalle.route) { AnalisisDetalleScreen(navController) }
                            composable(Route.Configuracion.route) { ConfiguracionScreen(navController, drawerState, syncViewModel) }
                            composable(Route.Conflictos.route) { ConflictosScreen(navController, syncViewModel) }
                            composable(Route.InformacionFinanciera("{dispensacionId}").route) { backStackEntry ->
                                val dispId = backStackEntry.arguments?.getString("dispensacionId")
                                if (dispId.isNullOrBlank()) {
                                    LaunchedEffect(Unit) { navController.popBackStack() }
                                    Box(Modifier.fillMaxSize())
                                } else {
                                    InformacionFinancieraScreen(navController, dispensacionId = dispId)
                                }
                            }
                        }
                        SnackbarHost(
                            hostState = snackbarHostState,
                            modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
                        )
                    }

                    // Bottom navigation removed — drawer provides all navigation
                }
            }
        }
    }
}
