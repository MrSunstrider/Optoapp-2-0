package com.example.optoapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.example.optoapp.data.AppRoles
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.optoapp.viewmodel.AuthViewModel
import com.example.optoapp.viewmodel.OpticaHeaderViewModel
import kotlinx.coroutines.launch

/** CompositionLocal para que cualquier pantalla pueda mostrar Snackbar sin acoplamiento. */
val LocalSnackbarHostState = staticCompositionLocalOf { SnackbarHostState() }

@Composable
fun MainDrawerScreen(
    parentNavController: NavController,
    /** Misma instancia que [LoginScreen] / [PinScreen] en [MainActivity]; si no, logout no resetea el estado que lee el login. */
    authViewModel: AuthViewModel
) {
    val context = LocalContext.current
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val navController = rememberNavController()
    
    // Obtenemos el ViewModel de sincronización a nivel de pantalla
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

    // Sincronización automática al entrar al dashboard
    LaunchedEffect(Unit) {
        syncViewModel.performSilentSync()
    }

    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
    val snackbarHostState = remember { SnackbarHostState() }
    val snackbarScope = rememberCoroutineScope()

    CompositionLocalProvider(LocalSnackbarHostState provides snackbarHostState) {
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
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
                    parentNavController = parentNavController
                )
            }
        }
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
            // Indicador discreto de sincronización de fondo
            if (isSilentSyncing) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth().height(2.dp),
                    color = MaterialTheme.colorScheme.secondary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }
            Surface(
                tonalElevation = 1.dp,
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
            ) {
                TextButton(
                    onClick = {
                        scope.launch {
                            val hasMultiple = authViewModel.prepareOpticaSelection()
                            if (hasMultiple) {
                                parentNavController.navigate("seleccion_optica")
                            } else {
                                android.widget.Toast.makeText(
                                    context,
                                    "Solo tienes una óptica asociada.",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp)
                ) {
                    Text(
                        text = "Óptica activa: ${opticaHeader.nombreOptica} · ${opticaHeader.fiscalEtiqueta}",
                        style = MaterialTheme.typography.labelSmall, // Letra un poco más pequeña y elegante
                        modifier = Modifier.padding(vertical = 2.dp), // Espacio mínimo
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Box(modifier = Modifier.weight(1f)) {
                NavHost(navController = navController, startDestination = "pacientes", modifier = Modifier.fillMaxSize()) {
                composable("pacientes") { PacientesListScreen(navController, drawerState) }
                composable("agenda") { AgendaScreen(navController, drawerState) }
                composable("nuevoPaciente") { NuevoPacienteScreen(navController) }
                composable("editarPaciente/{id}") { backStackEntry ->
                    NuevoPacienteScreen(navController, pacienteId = backStackEntry.arguments?.getString("id"))
                }
                composable("detallePaciente/{id}") { backStackEntry ->
                    val pid = backStackEntry.arguments?.getString("id")
                    if (pid.isNullOrBlank()) {
                        LaunchedEffect(Unit) { navController.popBackStack() }
                        Box(Modifier.fillMaxSize())
                    } else {
                        DetallePacienteScreen(navController, id = pid)
                    }
                }
                composable("nuevaEvaluacion/{pacienteId}") { backStackEntry ->
                    val pacienteId = backStackEntry.arguments?.getString("pacienteId")
                    if (pacienteId.isNullOrBlank()) {
                        LaunchedEffect(Unit) { navController.popBackStack() }
                        Box(Modifier.fillMaxSize())
                    } else {
                        NuevaEvaluacionScreen(navController, pacienteId = pacienteId)
                    }
                }
                composable("editarEvaluacion/{pacienteId}/{evalId}") { backStackEntry ->
                    val pacienteId = backStackEntry.arguments?.getString("pacienteId")
                    if (pacienteId.isNullOrBlank()) {
                        LaunchedEffect(Unit) { navController.popBackStack() }
                        Box(Modifier.fillMaxSize())
                    } else {
                        NuevaEvaluacionScreen(
                            navController,
                            pacienteId = pacienteId,
                            evaluacionId = backStackEntry.arguments?.getString("evalId")
                        )
                    }
                }
                composable("nuevaDispensacion/{pacienteId}") { backStackEntry ->
                    val pacienteId = backStackEntry.arguments?.getString("pacienteId")
                    if (pacienteId.isNullOrBlank()) {
                        LaunchedEffect(Unit) { navController.popBackStack() }
                        Box(Modifier.fillMaxSize())
                    } else {
                        NuevaDispensacionScreen(navController, pacienteId = pacienteId)
                    }
                }
                composable("editarDispensacion/{pacienteId}/{dispId}") { backStackEntry ->
                    val pacienteId = backStackEntry.arguments?.getString("pacienteId")
                    if (pacienteId.isNullOrBlank()) {
                        LaunchedEffect(Unit) { navController.popBackStack() }
                        Box(Modifier.fillMaxSize())
                    } else {
                        NuevaDispensacionScreen(
                            navController,
                            pacienteId = pacienteId,
                            dispensacionId = backStackEntry.arguments?.getString("dispId")
                        )
                    }
                }
                composable("servicios_extra") { 
                    ServiciosExtraScreen(navController, drawerState) 
                }
                composable("monturas") { MonturasScreen(navController) }
                composable("operacion_hoy") { OperacionHoyScreen(navController) }
                composable("nuevo_servicio") {
                    NuevoServicioScreen(navController, pacienteId = null)
                }
                composable("nuevo_servicio/{pacienteId}") { backStackEntry ->
                    NuevoServicioScreen(navController, pacienteId = backStackEntry.arguments?.getString("pacienteId"))
                }
                composable("editar_servicio/{id}") { backStackEntry ->
                    NuevoServicioScreen(navController, servicioId = backStackEntry.arguments?.getString("id"))
                }
                composable("reportes") { ReportesScreen(drawerState) }
                composable("cierre_caja") { CierreCajaScreen(navController) }
                composable("estadisticas_bi") { BIScreen(navController) }
                composable("configuracion") { ConfiguracionScreen(navController, drawerState, syncViewModel) }
                composable("conflictos") { ConflictosScreen(navController, syncViewModel) }
                    }
                    SnackbarHost(
                        hostState = snackbarHostState,
                        modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp)
                    )
                }
            }
        }
    }
}
}
